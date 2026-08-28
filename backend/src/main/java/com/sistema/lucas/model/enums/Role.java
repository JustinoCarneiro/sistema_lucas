package com.sistema.lucas.model.enums;

public enum Role {
    ADMIN("admin"),
    PROFESSIONAL("professional"),
    PATIENT("patient"),
    // Acesso técnico/operacional, mesmo nível de permissão do ADMIN em todo o sistema — conta
    // separada da administração real da clínica (não é um perfil restrito/read-only). Criado só
    // via POST /auth/registrar-tecnico, por um ADMIN já autenticado — nunca por autocadastro.
    TECNICO("tecnico");

    private String role;

    Role(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}