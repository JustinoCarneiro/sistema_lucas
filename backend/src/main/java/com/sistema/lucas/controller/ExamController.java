package com.sistema.lucas.controller;

import com.sistema.lucas.domain.User;
import com.sistema.lucas.domain.Exam; // Certifique-se de importar a entidade
import com.sistema.lucas.repository.ExamRepository;
import com.sistema.lucas.service.NotificationService; // Importar o serviço de notificação
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exams")
public class ExamController {

    private final ExamRepository repository;
    private final NotificationService notificationService; // 1. Injetar o serviço

    public ExamController(ExamRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public record ExamResponseDTO(Long id, String title, String fileUrl, LocalDateTime releaseDate) {}

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

    // 👇 2. GATILHO PARA NOTIFICAÇÃO DE EXAME LIBERADO 👇
    // Este método seria chamado quando o Admin faz o upload do resultado
    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseExam(@PathVariable Long id) {
        Exam exam = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exame não encontrado"));

        // Lógica para liberar o exame (ex: mudar status ou setar data)
        exam.setReleaseDate(LocalDateTime.now());
        repository.save(exam);

        // DISPARO ASSÍNCRONO: Avisa o paciente
        String message = "Olá " + exam.getPatient().getName() + 
                         ", seu exame '" + exam.getTitle() + "' já está disponível no portal!";
        
        notificationService.sendGenericEmail(
            exam.getPatient().getEmail(), 
            "Resultado de Exame Disponível - Sistema Lucas", 
            message
        );

        return ResponseEntity.ok().build();
    }
}