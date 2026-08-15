-- M13 (E4/US-4.8) — Lista de espera para cancelamentos, versão robusta (confirmação ativa
-- do paciente), aprovada pelo cliente em 15/08/2026. Repriced de Médio para Grande porque
-- exige reserva ativa do horário (não é "quem chega primeiro leva").
CREATE TABLE waitlist_entries (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL REFERENCES professional(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    date_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO',
    appointment_id BIGINT REFERENCES appointments(id),
    token VARCHAR(255) UNIQUE,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    oferta_expira_em TIMESTAMP
);

CREATE INDEX idx_waitlist_entries_slot ON waitlist_entries(professional_id, date_time, status);
CREATE INDEX idx_waitlist_entries_token ON waitlist_entries(token);
