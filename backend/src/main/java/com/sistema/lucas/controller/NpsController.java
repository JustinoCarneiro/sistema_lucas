// backend/src/main/java/com/sistema/lucas/controller/NpsController.java
package com.sistema.lucas.controller;

import com.sistema.lucas.model.dto.NpsResponderDTO;
import com.sistema.lucas.model.dto.NpsStatusDTO;
import com.sistema.lucas.service.NpsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Rota pública (token na URL/body faz o papel da autenticação) — ver
// SecurityConfigurations e RateLimitingFilter para o tratamento de acesso.
@RestController
@RequestMapping("/nps")
public class NpsController {

    @Autowired private NpsService service;

    @GetMapping("/{token}")
    public ResponseEntity<NpsStatusDTO> status(@PathVariable String token) {
        return ResponseEntity.ok(service.consultarStatus(token));
    }

    @PostMapping("/responder")
    public ResponseEntity<String> responder(@Valid @RequestBody NpsResponderDTO dto) {
        service.responder(dto.token(), dto.score(), dto.comentario());
        return ResponseEntity.ok("Avaliação registrada. Obrigado pelo retorno!");
    }
}
