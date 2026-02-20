package com.sistema.lucas.controller;

import com.sistema.lucas.domain.User;
import com.sistema.lucas.dto.LoginDTO;
import com.sistema.lucas.dto.TokenDTO;
import com.sistema.lucas.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private final AuthenticationManager manager;
    private final TokenService tokenService;

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
}