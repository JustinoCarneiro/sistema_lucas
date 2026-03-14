// backend/src/main/java/com/sistema/lucas/repository/DocumentoRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    // Paciente vê apenas documentos disponibilizados
    List<Documento> findByPacienteEmailAndDisponivelTrueOrderByCriadoEmDesc(String email);

    // Profissional vê todos os seus documentos (incluindo rascunhos)
    List<Documento> findByProfissionalEmailOrderByCriadoEmDesc(String email);

    // Profissional vê documentos de um paciente específico
    List<Documento> findByPacienteIdAndProfissionalEmailOrderByCriadoEmDesc(Long pacienteId, String email);
}