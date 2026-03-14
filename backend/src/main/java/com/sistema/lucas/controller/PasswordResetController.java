// backend/src/main/java/com/sistema/lucas/controller/PasswordResetController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {

    @Autowired private PasswordResetService service;

    // Passo 1 — usuário informa o e-mail
    @PostMapping("/esqueci-senha")
    public ResponseEntity<String> esqueciSenha(@RequestBody Map<String, String> body) {
        service.solicitarRecuperacao(body.get("email"));
        // Sempre retorna sucesso — não revela se o e-mail existe
        return ResponseEntity.ok("Se este e-mail estiver cadastrado, você receberá as instruções em breve.");
    }

    // Passo 2 — usuário define a nova senha com o token do link
    @PostMapping("/redefinir-senha")
    public ResponseEntity<String> redefinirSenha(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String novaSenha = body.get("novaSenha");

        if (novaSenha == null || novaSenha.length() < 6) {
            return ResponseEntity.badRequest().body("A senha deve ter pelo menos 6 caracteres.");
        }

        service.redefinirSenha(token, novaSenha);
        return ResponseEntity.ok("Senha redefinida com sucesso!");
    }
}