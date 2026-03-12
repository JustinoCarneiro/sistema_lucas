package com.sistema.lucas.model.enums;

public enum Role {
    ADMIN("admin"),
    PROFESSIONAL("professional"),
    PATIENT("patient");

    private String role;

    Role(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}