// backend/src/main/java/com/sistema/lucas/service/DocumentoService.java
package com.sistema.lucas.service;

import com.sistema.lucas.model.Documento;
import com.sistema.lucas.model.enums.TipoDocumento;
import com.sistema.lucas.model.dto.DocumentoResponseDTO;
import com.sistema.lucas.repository.DocumentoRepository;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.ProfessionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DocumentoService {

    @Autowired private DocumentoRepository documentoRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private AuditLogService auditLogService;

    // Paciente — só vê documentos disponibilizados
    public List<DocumentoResponseDTO> buscarDosPaciente(String emailPaciente) {
        auditLogService.log(emailPaciente, "VISUALIZACAO_LISTA", "Documento", null, "Paciente visualizou sua lista de documentos");
        return documentoRepository
            .findByPacienteEmailAndDisponivelTrueOrderByCriadoEmDesc(emailPaciente)
            .stream().map(DocumentoResponseDTO::new).toList();
    }

    // Profissional — vê todos os seus documentos
    public List<DocumentoResponseDTO> buscarDoProfissional(String emailProfissional) {
        auditLogService.log(emailProfissional, "VISUALIZACAO_LISTA", "Documento", null, "Profissional visualizou sua lista de documentos criados");
        return documentoRepository
            .findByProfissionalEmailOrderByCriadoEmDesc(emailProfissional)
            .stream().map(DocumentoResponseDTO::new).toList();
    }

    // Profissional — vê documentos de um paciente específico
    public List<DocumentoResponseDTO> buscarPorPaciente(Long pacienteId, String emailProfissional) {
        auditLogService.log(emailProfissional, "VISUALIZACAO_LISTA_PACIENTE", "Documento", pacienteId, "Profissional visualizou documentos do paciente ID: " + pacienteId);
        return documentoRepository
            .findByPacienteIdAndProfissionalEmailOrderByCriadoEmDesc(pacienteId, emailProfissional)
            .stream().map(DocumentoResponseDTO::new).toList();
    }

    // Criar documento (texto ou PDF)
    @Transactional
    public DocumentoResponseDTO criar(
            Long pacienteId,
            TipoDocumento tipo,
            String titulo,
            String conteudoTexto,
            String nomeArquivo,
            String arquivoBase64,
            boolean disponivel,
            String emailProfissional) {

        var paciente = patientRepository.findById(pacienteId)
            .orElseThrow(() -> new RuntimeException("Paciente não encontrado"));
        var profissional = professionalRepository.findByEmail(emailProfissional)
            .orElseThrow(() -> new RuntimeException("Profissional não encontrado"));

        // 🛡️ Segurança: Validação de Malware de PDF via Magic Bytes e Sizing Lock
        if (arquivoBase64 != null && !arquivoBase64.trim().isEmpty()) {
            String cleanBase64 = arquivoBase64.replaceAll("\\s+", ""); // sanitiza
            if (!cleanBase64.startsWith("JVBERi0") && !cleanBase64.startsWith("JVBERiA")) {
                throw new RuntimeException("Operação de Segurança: Arquivo rejeitado. A carga base64 não corresponde à assinatura física de um arquivo PDF.");
            }
            // Math: 5MB ~= 6.6MB in Base64 characters length
            if (cleanBase64.length() > 7_000_000) {
                throw new RuntimeException("Operação de Segurança: Arquivo excede o limite drástico permitido de 5MB.");
            }
            arquivoBase64 = cleanBase64;
        }

        var doc = new Documento();
        doc.setTipo(tipo);
        doc.setTitulo(titulo);
        doc.setConteudoTexto(conteudoTexto);
        doc.setNomeArquivo(nomeArquivo);
        doc.setArquivoBase64(arquivoBase64);
        doc.setPaciente(paciente);
        doc.setProfissional(profissional);
        doc.setDisponivel(disponivel);

        Documento saved = documentoRepository.save(doc);
        auditLogService.log(emailProfissional, "CRIACAO", "Documento", saved.getId(), "Criou documento: " + titulo);
        return new DocumentoResponseDTO(saved);
    }

    // Disponibilizar ou retirar acesso do paciente
    @Transactional
    public void alterarDisponibilidade(Long id, boolean disponivel, String emailProfissional) {
        var doc = documentoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        if (!doc.getProfissional().getEmail().equals(emailProfissional)) {
            throw new RuntimeException("Sem permissão para alterar este documento");
        }

        doc.setDisponivel(disponivel);
        documentoRepository.save(doc);
        auditLogService.log(emailProfissional, "ALTERAR_VISIBILIDADE", "Documento", id, "Alterou disponibilidade para: " + disponivel);
    }

    // Excluir
    @Transactional
    public void excluir(Long id, String emailProfissional) {
        var doc = documentoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Documento não encontrado"));

        if (!doc.getProfissional().getEmail().equals(emailProfissional)) {
            throw new RuntimeException("Sem permissão para excluir este documento");
        }

        documentoRepository.delete(doc);
        auditLogService.log(emailProfissional, "EXCLUSAO", "Documento", id, "Excluiu documento ID: " + id);
    }
}