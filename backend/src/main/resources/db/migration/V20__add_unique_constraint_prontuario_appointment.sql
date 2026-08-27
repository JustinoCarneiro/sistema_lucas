-- E5 (Prontuário Eletrônico) — trava no banco contra prontuário duplicado pra mesma consulta
-- (duplo clique/retry em "Salvar e finalizar atendimento"). A checagem em ProntuarioService já
-- existe na aplicação, mas isso fecha a corrida (check-then-act) no nível do banco também.
ALTER TABLE prontuarios ADD CONSTRAINT uk_prontuario_appointment UNIQUE (appointment_id);
