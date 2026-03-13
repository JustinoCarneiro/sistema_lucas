package com.sistema.lucas.controller;

import com.sistema.lucas.model.Exam;
import com.sistema.lucas.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/exams")
public class ExamController {

    @Autowired
    private ExamService service;

    @GetMapping("/me")
    public ResponseEntity<List<Exam>> getMyExams(Principal principal) {
        // O principal.getName() traz o e-mail do paciente logado
        return ResponseEntity.ok(service.getMyExams(principal.getName()));
    }
}