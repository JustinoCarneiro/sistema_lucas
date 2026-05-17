// backend/src/main/java/com/sistema/lucas/controller/DocumentoController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.DocumentoResponseDTO;
import com.sistema.lucas.model.enums.TipoDocumento;
import com.sistema.lucas.service.DocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documentos")
public class DocumentoController {

    @Autowired private DocumentoService service;

    // Paciente — seus documentos disponíveis
    @GetMapping("/meus")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<DocumentoResponseDTO>> meusDocs(Principal principal) {
        return ResponseEntity.ok(service.buscarDosPaciente(principal.getName()));
    }

    // Profissional — todos os documentos que criou
    @GetMapping("/profissional")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<DocumentoResponseDTO>> dosProfissional(Principal principal) {
        return ResponseEntity.ok(service.buscarDoProfissional(principal.getName()));
    }

    // Profissional — documentos de um paciente específico
    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<List<DocumentoResponseDTO>> porPaciente(
            @PathVariable Long pacienteId,
            Principal principal) {
        return ResponseEntity.ok(service.buscarPorPaciente(pacienteId, principal.getName()));
    }

    // Profissional — criar documento
    @PostMapping
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<DocumentoResponseDTO> criar(
            @RequestBody Map<String, Object> body,
            Principal principal) {

        Long pacienteId    = Long.valueOf(body.get("pacienteId").toString());
        TipoDocumento tipo = TipoDocumento.valueOf(body.get("tipo").toString());
        String titulo      = body.get("titulo").toString();
        String texto       = body.getOrDefault("conteudoTexto", "").toString();
        String nomeArq     = body.getOrDefault("nomeArquivo", "").toString();
        String base64      = body.getOrDefault("arquivoBase64", "").toString();
        boolean disponivel = Boolean.parseBoolean(body.getOrDefault("disponivel", "false").toString());

        var criado = service.criar(java.util.Objects.requireNonNull(pacienteId), tipo, titulo, texto, nomeArq, base64, disponivel, principal.getName());
        return ResponseEntity.status(201).body(criado);
    }

    // Profissional — disponibilizar ou retirar
    @PatchMapping("/{id}/disponibilidade")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<String> alterarDisponibilidade(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            Principal principal) {

        service.alterarDisponibilidade(java.util.Objects.requireNonNull(id), java.util.Objects.requireNonNull(body.get("disponivel")), principal.getName());
        return ResponseEntity.ok("Disponibilidade atualizada.");
    }

    // Profissional — excluir
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Principal principal) {
        service.excluir(java.util.Objects.requireNonNull(id), principal.getName());
        return ResponseEntity.noContent().build();
    }
}