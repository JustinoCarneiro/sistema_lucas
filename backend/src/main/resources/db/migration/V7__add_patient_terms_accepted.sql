-- LGPD: registro do consentimento expresso do paciente aos Termos de Uso
-- e à Política de Privacidade (Art. 7º, I e Art. 8º da Lei 13.709/2018).
-- Pacientes anteriores a esta versão recebem FALSE — o consentimento passa a
-- ser obrigatório a partir do novo fluxo de cadastro.

ALTER TABLE patient ADD COLUMN terms_accepted BOOLEAN NOT NULL DEFAULT FALSE;
