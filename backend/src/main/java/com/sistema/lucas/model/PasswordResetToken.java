// backend/src/main/java/com/sistema/lucas/model/PasswordResetToken.java
package com.sistema.lucas.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiracao;

    private boolean usado = false;

    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(expiracao);
    }
}