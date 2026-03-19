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
    @Autowired private PatientRepository patientRepository; // ✅ NOVO
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private com.sistema.lucas.security.service.EmailVerificationService emailVerificationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var user = (User) auth.getPrincipal();
        var token = tokenService.generateToken(user);
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterDTO data) {
        if (this.userRepository.findByEmail(data.email()) != null) {
            return ResponseEntity.badRequest().body("Email já cadastrado");
        }

        String encryptedPassword = passwordEncoder.encode(data.password());

        Patient newPatient = new Patient();
        newPatient.setName(data.name());
        newPatient.setEmail(data.email());
        newPatient.setPassword(encryptedPassword);
        newPatient.setRole(Role.PATIENT);
        newPatient.setCpf(data.cpf());     // ✅ novo
        newPatient.setPhone(data.phone()); // ✅ novo
        newPatient.setVerified(false);
        patientRepository.save(newPatient);

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