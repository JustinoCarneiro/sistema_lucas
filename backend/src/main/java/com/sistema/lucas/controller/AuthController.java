package com.sistema.lucas.controller;

import com.sistema.lucas.domain.Patient;
import com.sistema.lucas.domain.User;
import com.sistema.lucas.domain.enums.Role;
import com.sistema.lucas.dto.LoginDTO;
import com.sistema.lucas.dto.PatientCreateDTO;
import com.sistema.lucas.dto.TokenDTO;
import com.sistema.lucas.repository.PatientRepository;
import com.sistema.lucas.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@RequestBody @Valid LoginDTO dto) {
        // 1. Cria o "envelope" com email e senha
        var authenticationToken = new UsernamePasswordAuthenticationToken(dto.email(), dto.password());
        
        // 2. O Spring Security vai no banco (via AuthService) e verifica se a senha bate
        var authentication = manager.authenticate(authenticationToken);
        
        // 3. Se bater, pega o usuário autenticado e gera o Token JWT
        var tokenJWT = tokenService.generateToken((User) authentication.getPrincipal());
        
        // 4. Devolve o token
        return ResponseEntity.ok(new TokenDTO(tokenJWT));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> registerPatient(@RequestBody @Valid PatientCreateDTO dto) {
        var patient = new Patient();
        
        // Dados do Usuário
        patient.setName(dto.name());
        patient.setEmail(dto.email());
        patient.setPassword(passwordEncoder.encode(dto.password())); // Criptografa a senha!
        
        // O SEGREDO: Forçamos a role PATIENT e já deixamos a conta ativa!
        patient.setRole(Role.PATIENT); 
        patient.setActive(true);
        
        // Dados específicos do Paciente
        patient.setCpf(dto.cpf());
        patient.setHealthInsurance(dto.healthInsurance());
        patient.setWhatsapp(dto.whatsapp()); // Salva o novo campo

        patientRepository.save(patient); // Salva no banco

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}