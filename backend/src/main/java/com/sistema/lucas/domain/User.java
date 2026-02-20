package com.sistema.lucas.domain;

import com.sistema.lucas.domain.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class User extends BaseEntity implements UserDetails { // <--- AQUI: Implementa UserDetails

    @NotBlank
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
    
    @Column(nullable = false)
    private Boolean active = true;

    // ==========================================================================
    // MÉTODOS OBRIGATÓRIOS DO SPRING SECURITY (CONTRATO USERDETAILS)
    // ==========================================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Define as permissões baseadas no nosso Enum Role
        if (this.role == Role.ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_DOCTOR"), new SimpleGrantedAuthority("ROLE_PATIENT"));
        } else if (this.role == Role.DOCTOR) {
            return List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"));
        } else {
            return List.of(new SimpleGrantedAuthority("ROLE_PATIENT"));
        }
    }

    @Override
    public String getUsername() {
        return this.email; // O Spring pergunta o "username", nós respondemos com o "email"
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.active; // Se active for false, o Spring bloqueia o login
    }
}