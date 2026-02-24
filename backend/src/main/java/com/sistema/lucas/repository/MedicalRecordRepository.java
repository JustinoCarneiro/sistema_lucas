package com.sistema.lucas.repository;

import com.sistema.lucas.domain.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    // Busca todo o histórico do paciente, do mais recente para o mais antigo
    List<MedicalRecord> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}