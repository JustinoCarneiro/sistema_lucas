// backend/src/main/java/com/sistema/lucas/service/WaitlistExpirationScheduler.java
package com.sistema.lucas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WaitlistExpirationScheduler {

    @Autowired private WaitlistService waitlistService;

    // A cada 15min, expira ofertas de vaga (lista de espera) não confirmadas a tempo e
    // cascateia pro próximo da fila.
    @Scheduled(cron = "0 */15 * * * *", zone = "America/Sao_Paulo")
    public void expirarOfertas() {
        waitlistService.expirarOfertasVencidas();
    }
}
