package com.sistema.lucas.security.service;

import org.bouncycastle.util.encoders.Base32;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

// Relógio SEMPRE fixado via Clock injetado — nunca Instant.now() direto no teste. O projeto já
// foi mordido por um "time-bomb test" desse tipo antes (ver
// memoria-tecnica/bugs/teste-time-bomb-lembrete-scheduler.md).
class TotpServiceTest {

    // Secret dos vetores de teste oficiais do RFC 6238 (Apêndice B) — ASCII "12345678901234567890",
    // codificado em Base32 (é o que TotpService espera receber).
    private static final String SECRET_BASE32 =
        new String(Base32.encode("12345678901234567890".getBytes(StandardCharsets.US_ASCII)), StandardCharsets.US_ASCII);

    private TotpService serviceAt(long epochSecond) {
        Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
        return new TotpService(fixedClock);
    }

    @Test
    @DisplayName("Bate com o vetor de teste oficial do RFC 6238 em T=59s (contador=1)")
    void bateComVetorOficialRfc6238_T59() {
        var service = serviceAt(59);
        // RFC 6238 Apêndice B: código de 8 dígitos esperado é 94287082 — os 6 últimos dígitos
        // (que é o que geramos, DIGITS=6) são 287082.
        assertTrue(service.verifyCode(SECRET_BASE32, "287082"));
    }

    @Test
    @DisplayName("Bate com o vetor de teste oficial do RFC 6238 em T=1111111109s (contador=37037036)")
    void bateComVetorOficialRfc6238_T1111111109() {
        var service = serviceAt(1111111109L);
        // RFC 6238: código de 8 dígitos esperado é 07081804 — 6 últimos dígitos: 081804.
        assertTrue(service.verifyCode(SECRET_BASE32, "081804"));
    }

    @Test
    @DisplayName("Rejeita código incorreto")
    void rejeitaCodigoIncorreto() {
        var service = serviceAt(59);
        assertFalse(service.verifyCode(SECRET_BASE32, "000000"));
    }

    @Test
    @DisplayName("Tolera clock drift de ±1 passo (30s)")
    void toleraClockDriftDeUmPasso() {
        // Código gerado no instante 59 (passo 1) ainda deve validar 30s depois (passo 2, dentro
        // da janela de tolerância ±1) e 30s antes (passo 0).
        var serviceNoInstante59 = serviceAt(59);
        assertTrue(serviceNoInstante59.verifyCode(SECRET_BASE32, "287082"));

        var servicoUmPassoDepois = serviceAt(59 + 30);
        assertTrue(servicoUmPassoDepois.verifyCode(SECRET_BASE32, "287082"));

        var servicoUmPassoAntes = serviceAt(59 - 30 >= 0 ? 59 - 30 : 0);
        assertTrue(servicoUmPassoAntes.verifyCode(SECRET_BASE32, "287082"));
    }

    @Test
    @DisplayName("Rejeita código fora da janela de tolerância (2+ passos de diferença)")
    void rejeitaCodigoForaDaJanela() {
        var servicoDoisPassosDepois = serviceAt(59 + 60);
        assertFalse(servicoDoisPassosDepois.verifyCode(SECRET_BASE32, "287082"));
    }

    @Test
    @DisplayName("Rejeita código nulo, vazio ou com formato inválido")
    void rejeitaFormatoInvalido() {
        var service = serviceAt(59);
        assertFalse(service.verifyCode(SECRET_BASE32, null));
        assertFalse(service.verifyCode(SECRET_BASE32, ""));
        assertFalse(service.verifyCode(SECRET_BASE32, "12345")); // 5 dígitos, não 6
        assertFalse(service.verifyCode(SECRET_BASE32, "abcdef")); // não é numérico
    }

    @Test
    @DisplayName("generateSecretBase32 gera secrets distintos e decodificáveis")
    void generateSecretBase32GeraSecretValido() {
        var service = serviceAt(0);
        String s1 = service.generateSecretBase32();
        String s2 = service.generateSecretBase32();
        assertNotEquals(s1, s2);
        assertEquals(20, Base32.decode(s1).length); // 160 bits
    }

    @Test
    @DisplayName("buildOtpAuthUri inclui issuer, conta e parâmetros TOTP padrão")
    void buildOtpAuthUriTemFormatoEsperado() {
        var service = serviceAt(0);
        String uri = service.buildOtpAuthUri("ABCD1234", "paciente@test.com");
        assertTrue(uri.startsWith("otpauth://totp/InstitutoLucas:paciente@test.com"));
        assertTrue(uri.contains("secret=ABCD1234"));
        assertTrue(uri.contains("digits=6"));
        assertTrue(uri.contains("period=30"));
    }
}
