package com.sistema.lucas.model;

import com.sistema.lucas.config.jpa.EncryptionConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String usuarioEmail;
    private LocalDateTime dataHora;
    private String acao; // ex: VISUALIZACAO, EXPORTACAO, EXCLUSAO
    private String tipoEntidade; // ex: Prontuario, Documento
    private Long entidadeId;
    
    // Pode carregar cópia de campo sensível (ex.: justificativa de cancelamento, que é cifrada
    // na origem em Appointment.cancelReason) — cifrado pelo mesmo motivo. Coluna já era TEXT,
    // não precisa alargar.
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionConverter.class)
    private String detalhes;

    @PrePersist
    protected void onCreate() {
        this.dataHora = LocalDateTime.now();
    }
}
