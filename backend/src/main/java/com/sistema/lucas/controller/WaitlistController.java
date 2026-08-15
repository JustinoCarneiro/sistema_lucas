// backend/src/main/java/com/sistema/lucas/controller/WaitlistController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.WaitlistEntradaDTO;
import com.sistema.lucas.model.dto.WaitlistEntryResponseDTO;
import com.sistema.lucas.model.dto.WaitlistOfertaStatusDTO;
import com.sistema.lucas.model.dto.WaitlistTokenDTO;
import com.sistema.lucas.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/waitlist")
public class WaitlistController {

    @Autowired private WaitlistService service;

    // Paciente — entrar na fila de um horário ocupado
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<String> entrar(@RequestBody @Valid WaitlistEntradaDTO dto, Principal principal) {
        service.entrarNaFila(dto.professionalId(), dto.dateTime(), principal.getName());
        return ResponseEntity.status(201).body("Você entrou na lista de espera. Avisaremos por e-mail se a vaga abrir.");
    }

    // Paciente — suas entradas na lista de espera
    @GetMapping("/minhas")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<WaitlistEntryResponseDTO>> minhas(Principal principal) {
        return ResponseEntity.ok(service.minhasEntradas(principal.getName()));
    }

    // Paciente — sair da fila
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> sair(@PathVariable Long id, Principal principal) {
        service.sairDaFila(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    // Rota pública — token da oferta (link do e-mail, sem exigir login)
    @GetMapping("/oferta/{token}")
    public ResponseEntity<WaitlistOfertaStatusDTO> statusOferta(@PathVariable String token) {
        return ResponseEntity.ok(service.consultarOferta(token));
    }

    @PostMapping("/oferta/confirmar")
    public ResponseEntity<String> confirmarOferta(@RequestBody @Valid WaitlistTokenDTO dto) {
        service.confirmarOferta(dto.token());
        return ResponseEntity.ok("Vaga confirmada! Agora é só aguardar a aprovação do profissional, como em qualquer agendamento.");
    }
}
