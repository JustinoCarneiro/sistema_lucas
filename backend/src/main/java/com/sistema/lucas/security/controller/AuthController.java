// backend/src/main/java/com/sistema/lucas/security/controller/AuthController.java
package com.sistema.lucas.security.controller;

import com.sistema.lucas.model.Patient;
import com.sistema.lucas.model.User;
import com.sistema.lucas.model.enums.Role;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.repository.UserRepository;
import com.sistema.lucas.security.dto.LoginRequestDTO;
import com.sistema.lucas.security.dto.LoginResponseDTO;
import com.sistema.lucas.security.dto.RegisterDTO;
import com.sistema.lucas.security.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
// ✅ CORREÇÃO: removido @CrossOrigin("*") — o SecurityConfigurations já cuida disso
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private TokenService tokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private com.sistema.lucas.security.service.EmailVerificationService emailVerificationService;
    @Autowired private com.sistema.lucas.security.service.RefreshTokenService refreshTokenService;
    @Autowired private com.sistema.lucas.security.service.TokenDenylistService tokenDenylistService;
    @Autowired private com.sistema.lucas.service.CpfHashService cpfHashService;

    // SEC-01: cookie Secure ativo só em produção (HTTPS). Em dev (HTTP) é desativado
    // via application-dev.properties — senão o navegador descarta o cookie de sessão.
    @org.springframework.beans.factory.annotation.Value("${app.security.cookie.secure:true}")
    private boolean cookieSecure;

    // LGPD — versão vigente dos termos, registrada junto ao consentimento.
    @org.springframework.beans.factory.annotation.Value("${app.lgpd.terms-version}")
    private String termsVersion;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var user = (User) auth.getPrincipal();
        var token = tokenService.generateToken(user);
        
        // SEC-01: Token é enviado exclusivamente via cookie HttpOnly
        ResponseCookie cookie = ResponseCookie.from("token", java.util.Objects.requireNonNull(token))
            .httpOnly(true)
            .secure(cookieSecure) // SEC-01: true em produção (HTTPS); false em dev (HTTP)
            .path("/")
            .maxAge(15 * 60) // SEC-03: 15 minutos de validade
            .sameSite("Strict")
            .build();

        // SEC-03: Gerar Refresh Token de 7 dias
        String refreshTokenStr = refreshTokenService.createRefreshToken(user);
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", java.util.Objects.requireNonNull(refreshTokenStr))
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(7 * 24 * 60 * 60) // 7 dias
            .sameSite("Strict")
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(new LoginResponseDTO(user.getRole().name(), user.isVerified()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "token", required = false) String token,
                                       @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        // SEC-03: Revogar token primário (Denylist)
        if (token != null && !token.isBlank()) {
            tokenDenylistService.revokeToken(token, java.util.Objects.requireNonNull(java.time.LocalDateTime.now().plusMinutes(15)));
        }

        // SEC-03: Revogar refresh token
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.findByToken(refreshToken).ifPresent(rt -> refreshTokenService.revoke(rt));
        }

        // SEC-01: Limpar os cookies marcando a validade para 0
        ResponseCookie cookie = ResponseCookie.from("token", "")
            .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("Strict").build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("Strict").build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        var rtOpt = refreshTokenService.findByToken(refreshToken);
        if (rtOpt.isEmpty() || !refreshTokenService.isValid(rtOpt.get())) {
            return ResponseEntity.status(401).build();
        }

        var rt = rtOpt.get();
        refreshTokenService.markAsUsed(rt); // Rotacionar: o antigo já era

        User user = rt.getUser();
        String newToken = tokenService.generateToken(user);
        String newRefreshTokenStr = refreshTokenService.createRefreshToken(user);

        ResponseCookie cookie = ResponseCookie.from("token", java.util.Objects.requireNonNull(newToken))
            .httpOnly(true).secure(cookieSecure).path("/").maxAge(15 * 60).sameSite("Strict").build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", java.util.Objects.requireNonNull(newRefreshTokenStr))
            .httpOnly(true).secure(cookieSecure).path("/").maxAge(7 * 24 * 60 * 60).sameSite("Strict").build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .build();
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterDTO data) {
        if (this.userRepository.findByEmail(data.email()) != null) {
            return ResponseEntity.badRequest().body("Email já cadastrado");
        }

        String cpfHash = cpfHashService.hash(data.cpf());
        if (patientRepository.existsByCpfHash(cpfHash)) {
            return ResponseEntity.badRequest().body("CPF já cadastrado");
        }

        // LGPD — o cadastro só prossegue mediante consentimento expresso.
        if (!data.termsAccepted()) {
            return ResponseEntity.badRequest()
                .body("É necessário aceitar os Termos de Uso e a Política de Privacidade.");
        }

        String encryptedPassword = passwordEncoder.encode(data.password());

        Patient newPatient = new Patient();
        newPatient.setName(data.name());
        newPatient.setEmail(data.email());
        newPatient.setPassword(encryptedPassword);
        newPatient.setRole(Role.PATIENT);
        newPatient.setCpf(data.cpf());     // ✅ novo
        // AUD-03: cpf_hash é NOT NULL e o hashing saiu da entidade (foi para o CpfHashService).
        // Precisa ser gerado aqui explicitamente, senão o cadastro público quebra.
        newPatient.setCpfHash(cpfHash);
        newPatient.setPhone(data.phone()); // ✅ novo
        newPatient.setVerified(false);
        // LGPD — consentimento registrado com prova demonstrável (Art. 8º §1)
        newPatient.setTermsAccepted(true);
        newPatient.setTermsAcceptedAt(java.time.LocalDateTime.now());
        newPatient.setTermsVersion(termsVersion);
        try {
            patientRepository.save(newPatient);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Rede de segurança contra corrida entre cadastros simultâneos
            return ResponseEntity.badRequest().body("E-mail ou CPF já está em uso por outra conta.");
        }

        // ✅ Envia e-mail de verificação
        emailVerificationService.createAndSendVerificationEmail(newPatient);

        return ResponseEntity.status(201).body("Paciente registrado com sucesso! Verifique seu e-mail para confirmar a conta.");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam("token") String token) {
        String result = emailVerificationService.verifyToken(token);
        if (result.contains("sucesso")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }
}