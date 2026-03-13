package com.sistema.lucas.repository;

import com.sistema.lucas.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    // Busca exames de um paciente específico pelo e-mail (para o Dashboard do Paciente)
    List<Exam> findByPatientEmail(String email);
    
    // Busca exames solicitados por um médico específico
    List<Exam> findByProfessionalEmail(String email);
}