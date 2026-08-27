-- E6 (Documentos Clínicos) — titulo e nome_arquivo passam a ser cifrados
-- (EncryptionConverter, mesmo padrão de conteudo_texto/arquivo_base64) porque o título de um
-- documento clínico pode conter diagnóstico (ex.: "Laudo — Transtorno Depressivo Maior").
-- Alarga pra TEXT antes: o valor cifrado (AES-256-GCM + IV, em base64) é bem maior que o
-- texto original e não cabe garantidamente em VARCHAR(255).
ALTER TABLE documentos ALTER COLUMN titulo TYPE TEXT;
ALTER TABLE documentos ALTER COLUMN nome_arquivo TYPE TEXT;
