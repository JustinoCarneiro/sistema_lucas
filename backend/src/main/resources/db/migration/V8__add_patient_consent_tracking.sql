-- LGPD: prova demonstrável do consentimento (Art. 8º §1 da Lei 13.709/2018).
-- Um booleano não comprova QUANDO o titular consentiu nem a QUAL versão dos
-- termos — estas colunas registram o momento e a versão vigente do aceite.

ALTER TABLE patient ADD COLUMN terms_accepted_at TIMESTAMP;
ALTER TABLE patient ADD COLUMN terms_version VARCHAR(20);
