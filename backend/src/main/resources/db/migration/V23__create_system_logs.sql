-- backend/src/main/resources/db/migration/V23__create_system_logs.sql
-- Captura automática de WARN/ERROR do sistema inteiro (via Logback appender + fila em memória +
-- persistência assíncrona, ver InMemoryLogAppender/SystemLogPersistenceService). Visível pra
-- ADMIN/TECNICO em /panel/logs — sem isso, uma falha como a de SMTP encontrada em 27/08/2026
-- (senha de app com espaço, e-mail nunca saía, sem log nenhum antes da correção do dia) fica
-- invisível até alguém reclamar diretamente.
CREATE TABLE system_logs (
    id BIGSERIAL PRIMARY KEY,
    level VARCHAR(10) NOT NULL,
    logger_name VARCHAR(255),
    message TEXT,
    stack_trace TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_system_logs_criado_em ON system_logs (criado_em DESC);
CREATE INDEX idx_system_logs_level ON system_logs (level);
