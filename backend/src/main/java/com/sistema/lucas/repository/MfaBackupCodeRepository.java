// backend/src/main/java/com/sistema/lucas/repository/MfaBackupCodeRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.MfaBackupCode;
import com.sistema.lucas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, Long> {
    List<MfaBackupCode> findByUserAndUsedAtIsNull(User user);
    void deleteByUser(User user);

    // UPDATE condicional atômico (mesmo padrão de RefreshTokenRepository.marcarUsadoSeAindaNaoUsado
    // e NpsResponseRepository.responderSeAindaNaoRespondido): fecha a corrida de duas requisições
    // concorrentes tentando consumir o mesmo backup code.
    @Modifying
    @Query("UPDATE MfaBackupCode b SET b.usedAt = :usedAt WHERE b.id = :id AND b.usedAt IS NULL")
    int marcarUsadoSeAindaNaoUsado(Long id, LocalDateTime usedAt);
}
