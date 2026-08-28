// backend/src/main/java/com/sistema/lucas/repository/SystemLogRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    Page<SystemLog> findAllByOrderByCriadoEmDesc(Pageable pageable);
    Page<SystemLog> findByLevelOrderByCriadoEmDesc(String level, Pageable pageable);
}
