package com.sistema.lucas.model;

import com.sistema.lucas.model.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class User implements UserDetails { // <-- 1. Adicionado implements UserDetails
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    @PrePersist
    @PreUpdate
    public void normalizeUser() {
        if (this.name != null) {
            this.name = this.name.trim().replaceAll("\\s+", " ");
            String[] words = this.name.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (!w.isEmpty()) {
                    sb.append(Character.toUpperCase(w.charAt(0)))
                      .append(w.substring(1).toLowerCase()).append(" ");
                }
            }
            this.name = sb.toString().trim();
        }
        if (this.email != null) {
            this.email = this.email.trim().toLowerCase();
        }
    }

    // --- 2. MÉTODOS OBRIGATÓRIOS DO SPRING SECURITY ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Retorna a Role com o prefixo que o Spring exige
        if (this.role != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
        }
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email; // O "username" do nosso sistema é o e-mail
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // A conta nunca expira
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // A conta não bloqueia
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // As credenciais não expiram
    }

    @Override
    public boolean isEnabled() {
        return true; // O usuário está sempre ativo
    }
}