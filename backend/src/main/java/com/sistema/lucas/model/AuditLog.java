package com.sistema.lucas.model;

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
    
    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @PrePersist
    protected void onCreate() {
        this.dataHora = LocalDateTime.now();
    }
}
