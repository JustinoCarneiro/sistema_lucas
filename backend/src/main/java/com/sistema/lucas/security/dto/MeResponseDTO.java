package com.sistema.lucas.security.dto;

// GET /auth/me — dado mínimo do usuário logado que faz sentido pra QUALQUER role (ADMIN incluso,
// que não é Patient nem Professional e por isso não tem endpoint /me próprio como os outros
// dois). Hoje só serve pra tela de Segurança (MFA) saber o estado atual sem precisar de um perfil
// completo — não expande pra dado de perfil geral de propósito, é usar o mínimo necessário.
public record MeResponseDTO(String role, boolean mfaEnabled) {}
