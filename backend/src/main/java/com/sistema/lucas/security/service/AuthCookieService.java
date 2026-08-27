// backend/src/main/java/com/sistema/lucas/security/service/AuthCookieService.java
package com.sistema.lucas.security.service;

import com.sistema.lucas.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

// Extraído de AuthController.login()/refresh() pra ser reaproveitado por MfaController.verify()
// — emitir os cookies de sessão real (token + refresh_token) é a mesma cauda nos três lugares,
// só o momento em que ela roda muda (direto no login sem MFA, ou só depois do segundo fator).
@Service
public class AuthCookieService {

    @Autowired private TokenService tokenService;
    @Autowired private RefreshTokenService refreshTokenService;

    @Value("${app.security.cookie.secure:true}")
    private boolean cookieSecure;

    public List<ResponseCookie> gerarCookiesDeSessao(User user) {
        String token = tokenService.generateToken(user);
        ResponseCookie cookie = ResponseCookie.from("token", Objects.requireNonNull(token))
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(15 * 60)
            .sameSite("Strict")
            .build();

        String refreshTokenStr = refreshTokenService.createRefreshToken(user);
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", Objects.requireNonNull(refreshTokenStr))
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .sameSite("Strict")
            .build();

        return List.of(cookie, refreshCookie);
    }
}
