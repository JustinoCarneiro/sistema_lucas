package com.sistema.lucas.security.controller;

import com.sistema.lucas.model.User;
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
import org.springframework.security.crypto.password.PasswordEncoder; // Use o Bean injetado
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Injetado para manter o padrão Argon2/BCrypt

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid LoginRequestDTO data) {
        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
            var auth = this.authenticationManager.authenticate(usernamePassword);

            var user = (User) auth.getPrincipal();
            var token = tokenService.generateToken(user.getEmail());

            return ResponseEntity.ok(new LoginResponseDTO(token));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            System.out.println("❌ SENHA INCORRETA PARA: " + data.email());
            throw e;
        } catch (Exception e) {
            System.out.println("❌ ERRO INESPERADO: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterDTO data) {
        if (this.userRepository.findByEmail(data.email()) != null) {
            return ResponseEntity.badRequest().body("Email já cadastrado");
        }

        // Use o passwordEncoder injetado para garantir que usa o Argon2 configurado
        String encryptedPassword = passwordEncoder.encode(data.password());
        
        User newUser = new User(data.email(), encryptedPassword, data.role());
        this.userRepository.save(newUser);

        return ResponseEntity.status(201).body("Utilizador criado com sucesso!");
    }
}