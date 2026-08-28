package com.sistema.lucas.security.dto;

// GET /auth/me — dado mínimo do usuário logado que faz sentido pra QUALQUER role (ADMIN/TECNICO
// inclusos, que não são Patient nem Professional e por isso não têm endpoint /me próprio como os
// outros dois). Serve pra tela de Segurança (MFA) e pro cabeçalho do painel (nome + role) saberem
// o estado sem precisar de um perfil completo — não expande além disso de propósito.
public record MeResponseDTO(String name, String role, boolean mfaEnabled) {}
