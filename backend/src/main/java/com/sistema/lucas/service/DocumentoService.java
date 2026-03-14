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

    // Paciente — só vê documentos disponibilizados
    public List<DocumentoResponseDTO> buscarDosPaciente(String emailPaciente) {
        return documentoRepository
            .findByPacienteEmailAndDisponivelTrueOrderByCriadoEmDesc(emailPaciente)
            .stream().map(DocumentoResponseDTO::new).toList();
    }

    // Profissional — vê todos os seus documentos
    public List<DocumentoResponseDTO> buscarDoProfissional(String emailProfissional) {
        return documentoRepository
            .findByProfissionalEmailOrderByCriadoEmDesc(emailProfissional)
            .stream().map(DocumentoResponseDTO::new).toList();
    }

    // Profissional — vê documentos de um paciente específico
    public List<DocumentoResponseDTO> buscarPorPaciente(Long pacienteId, String emailProfissional) {
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

        var doc = new Documento();
        doc.setTipo(tipo);
        doc.setTitulo(titulo);
        doc.setConteudoTexto(conteudoTexto);
        doc.setNomeArquivo(nomeArquivo);
        doc.setArquivoBase64(arquivoBase64);
        doc.setPaciente(paciente);
        doc.setProfissional(profissional);
        doc.setDisponivel(disponivel);

        return new DocumentoResponseDTO(documentoRepository.save(doc));
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
    }
}