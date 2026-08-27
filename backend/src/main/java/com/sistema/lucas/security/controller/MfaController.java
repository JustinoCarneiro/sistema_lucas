// backend/src/main/java/com/sistema/lucas/security/controller/MfaController.java
package com.sistema.lucas.security.controller;

import com.sistema.lucas.model.User;
import com.sistema.lucas.security.dto.*;
import com.sistema.lucas.security.service.AuthCookieService;
import com.sistema.lucas.security.service.MfaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/mfa")
public class MfaController {

    @Autowired private MfaService mfaService;
    @Autowired private AuthCookieService authCookieService;

    @Value("${app.security.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponseDTO> setup(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mfaService.iniciarSetup(user));
    }

    @PostMapping("/enable")
    public ResponseEntity<MfaEnableResponseDTO> enable(@AuthenticationPrincipal User user, @RequestBody @Valid MfaCodeDTO data) {
        var backupCodes = mfaService.ativar(user, data.code());
        return ResponseEntity.ok(new MfaEnableResponseDTO(backupCodes));
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal User user, @RequestBody @Valid MfaDisableDTO data) {
        mfaService.desativar(user, data.password(), data.code());
        return ResponseEntity.ok().build();
    }

    // Público (ver SecurityConfigurations) — o usuário ainda não tem sessão nesse ponto, só o
    // cookie mfa_pending_token emitido por AuthController.login().
    @PostMapping("/verify")
    public ResponseEntity<LoginResponseDTO> verify(
            @CookieValue(name = "mfa_pending_token", required = false) String pendingToken,
            @RequestBody @Valid MfaCodeDTO data) {
        if (pendingToken == null || pendingToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        User user = mfaService.verificarLogin(pendingToken, data.code());
        var cookies = authCookieService.gerarCookiesDeSessao(user);

        // Limpa o cookie de pendência — a sessão real já foi emitida, não precisa mais dele.
        ResponseCookie clearPending = ResponseCookie.from("mfa_pending_token", "")
            .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("Strict").build();

        var builder = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clearPending.toString());
        for (var cookie : cookies) {
            builder.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return builder.body(new LoginResponseDTO(user.getRole().name(), user.isVerified()));
    }
}
