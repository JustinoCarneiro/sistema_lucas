package com.sistema.lucas.service;

import com.sistema.lucas.model.AuditLog;
import com.sistema.lucas.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String email, String acao, String tipoEntidade, Long entidadeId, String detalhes) {
        AuditLog log = AuditLog.builder()
                .usuarioEmail(email)
                .dataHora(LocalDateTime.now())
                .acao(acao)
                .tipoEntidade(tipoEntidade)
                .entidadeId(entidadeId)
                .detalhes(detalhes)
                .build();
        auditLogRepository.save(log);
    }
}
