// backend/src/main/java/com/sistema/lucas/repository/NpsResponseRepository.java
package com.sistema.lucas.repository;

import com.sistema.lucas.model.NpsResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NpsResponseRepository extends JpaRepository<NpsResponse, Long> {
    Optional<NpsResponse> findByToken(String token);
    Optional<NpsResponse> findByAppointmentId(Long appointmentId);

    // UPDATE condicional atômico: só grava se AINDA não havia resposta (respondidoEm IS NULL).
    // Fecha a corrida entre duas submissões concorrentes com o mesmo token — sem isso, um
    // read-then-write em dois passos (isRespondido() e depois save()) permitia que a segunda
    // requisição sobrescrevesse a nota da primeira silenciosamente.
    @Modifying
    @Query("UPDATE NpsResponse n SET n.score = :score, n.comentario = :comentario, n.respondidoEm = :respondidoEm " +
           "WHERE n.token = :token AND n.respondidoEm IS NULL")
    int responderSeAindaNaoRespondido(
        @Param("token") String token,
        @Param("score") Integer score,
        @Param("comentario") String comentario,
        @Param("respondidoEm") LocalDateTime respondidoEm);
}
