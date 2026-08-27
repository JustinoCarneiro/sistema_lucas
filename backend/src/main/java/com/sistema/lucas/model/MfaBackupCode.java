// backend/src/main/java/com/sistema/lucas/model/MfaBackupCode.java
package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Código de backup de MFA — o valor em texto plano nunca é persistido (só existe na resposta
// HTTP do /auth/mfa/enable, uma vez); aqui fica só o hash HMAC-SHA256 (MfaBackupCodeService).
@Entity
@Table(name = "mfa_backup_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MfaBackupCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
