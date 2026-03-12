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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid LoginRequestDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        // CORREÇÃO AQUI: Passar apenas o email (String) e não o objeto User
        var user = (User) auth.getPrincipal();
        var token = tokenService.generateToken(user.getEmail());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data) {
        if (this.userRepository.findByEmail(data.email()) != null) {
            return ResponseEntity.badRequest().body("Email já cadastrado");
        }

        // Encriptar a password antes de guardar
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        
        User newUser = new User(data.email(), encryptedPassword, data.role());
        this.userRepository.save(newUser);

        return ResponseEntity.status(201).body("Utilizador criado com sucesso!");
    }
}