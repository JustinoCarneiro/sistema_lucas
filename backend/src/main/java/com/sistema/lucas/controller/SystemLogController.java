// backend/src/main/java/com/sistema/lucas/controller/SystemLogController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.SystemLogResponseDTO;
import com.sistema.lucas.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// /panel/logs (ADMIN/TECNICO) — captura automática de WARN/ERROR via InMemoryLogAppender +
// SystemLogPersistenceService. Achado que motivou isso: SMTP quebrado silenciosamente por quem
// sabe quanto tempo, sem nenhum log visível (27/08/2026) — ver memoria-tecnica/bugs/
// smtp-gmail-autenticacao-falhando.md.
@RestController
@RequestMapping("/system-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'TECNICO')")
public class SystemLogController {

    @Autowired private SystemLogRepository repository;

    @GetMapping
    public ResponseEntity<Page<SystemLogResponseDTO>> listar(
            @RequestParam(required = false) String level,
            Pageable pageable) {
        Page<com.sistema.lucas.model.SystemLog> page = (level != null && !level.isBlank())
            ? repository.findByLevelOrderByCriadoEmDesc(level.toUpperCase(), pageable)
            : repository.findAllByOrderByCriadoEmDesc(pageable);

        return ResponseEntity.ok(page.map(SystemLogResponseDTO::new));
    }
}
