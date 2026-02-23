package com.sistema.lucas.controller;

import com.sistema.lucas.domain.User;
import com.sistema.lucas.repository.ExamRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exams")
public class ExamController {

    private final ExamRepository repository;

    public ExamController(ExamRepository repository) {
        this.repository = repository;
    }

    // ✨ TRUQUE SÊNIOR: Criamos um DTO interno rápido apenas para devolver os dados 
    // com segurança para o Angular, evitando o erro do FetchType.LAZY do Paciente!
    public record ExamResponseDTO(Long id, String title, String fileUrl, LocalDateTime releaseDate) {}

    // Rota blindada: O paciente só vê os PRÓPRIOS exames
    @GetMapping("/me")
    public ResponseEntity<List<ExamResponseDTO>> getMyExams(@AuthenticationPrincipal User loggedUser) {
        
        List<ExamResponseDTO> myExams = repository.findAllByPatientIdOrderByReleaseDateDesc(loggedUser.getId())
                .stream()
                .map(exam -> new ExamResponseDTO(
                        exam.getId(),
                        exam.getTitle(),
                        exam.getFileUrl(),
                        exam.getReleaseDate()
                ))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(myExams);
    }
}