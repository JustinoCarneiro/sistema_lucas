package com.sistema.lucas.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfHashServiceTest {

    // Usa o mesmo pepper definido em application.properties de teste
    private final CpfHashService cpfHashService = new CpfHashService("test-pepper-1234567890");

    @Test @DisplayName("Hash do mesmo CPF deve ser idempotente")
    void hashDoMesmoCpfDeveSerIgual() {
        String hash1 = cpfHashService.hash("11111111111");
        String hash2 = cpfHashService.hash("11111111111");
        assertEquals(hash1, hash2);
    }

    @Test @DisplayName("CPF com pontuação deve gerar o mesmo hash que CPF limpo")
    void cpfComPontuacaoDeveGerarMesmoHash() {
        String hashLimpo     = cpfHashService.hash("11111111111");
        String hashFormatado = cpfHashService.hash("111.111.111-11");
        assertEquals(hashLimpo, hashFormatado);
    }

    @Test @DisplayName("CPFs diferentes devem gerar hashes diferentes")
    void cpfsDiferentesDevemGerarHashesDiferentes() {
        String hash1 = cpfHashService.hash("11111111111");
        String hash2 = cpfHashService.hash("22222222222");
        assertNotEquals(hash1, hash2);
    }

    @Test @DisplayName("Hash deve retornar null para CPF nulo")
    void hashNullParaCpfNulo() {
        assertNull(cpfHashService.hash(null));
    }

    @Test @DisplayName("Hash deve ser uma string hexadecimal de 64 caracteres (SHA-256)")
    void hashDeveSerHexDe64Chars() {
        String hash = cpfHashService.hash("12345678901");
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }
}
