package com.sistema.lucas.repository;

import com.sistema.lucas.model.RefreshToken;
import com.sistema.lucas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    long deleteByExpiresAtBeforeOrUsedTrueOrRevokedAtNotNull(LocalDateTime now);

    // UPDATE condicional atômico: marca usado só se AINDA não estava usado. O retorno (linhas
    // afetadas) diz se esta chamada "venceu" a corrida — evita o TOCTOU de ler isValid() e só
    // depois marcar usado como dois passos separados, que permitia reaproveitar o mesmo refresh
    // token em requisições concorrentes.
    @Modifying
    @Query("UPDATE RefreshToken r SET r.used = true WHERE r.token = :token AND r.used = false")
    int marcarUsadoSeAindaNaoUsado(String token);
}
