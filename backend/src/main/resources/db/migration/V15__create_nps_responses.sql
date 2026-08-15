-- M11 (E11/US-11.1) — NPS pós-consulta, aprovado pelo cliente em 10/08/2026.
-- Uma linha por consulta concluída: nasce junto com o pedido de avaliação
-- (ProntuarioService.create()) e é preenchida quando/se o paciente responder.
CREATE TABLE nps_responses (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    score SMALLINT,
    comentario TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    respondido_em TIMESTAMP,
    expira_em TIMESTAMP NOT NULL
);

CREATE INDEX idx_nps_responses_token ON nps_responses(token);
