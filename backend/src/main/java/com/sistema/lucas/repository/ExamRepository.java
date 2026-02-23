package com.sistema.lucas.repository;

import com.sistema.lucas.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    
    // Adaptado para usar o seu campo 'releaseDate'!
    List<Exam> findAllByPatientIdOrderByReleaseDateDesc(Long patientId);
}