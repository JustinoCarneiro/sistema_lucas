-- M13 (E4/US-4.8) — lock otimista pra fechar a corrida entre o paciente confirmar a oferta
-- (WaitlistService.confirmarOferta) e o scheduler expirá-la no mesmo instante
-- (WaitlistService.expirarOfertasVencidas), que antes podia se sobrescrever silenciosamente.
ALTER TABLE waitlist_entries ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
