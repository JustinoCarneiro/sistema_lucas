-- V9: Garante que professional_availability existe com o schema baseado em datas.
-- Caso 1 (banco novo): tabela não existe → cria com o schema correto.
-- Caso 2 (migração): tabela existe com day_of_week → migra para available_date.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'professional_availability'
    ) THEN
        -- Banco novo: cria a tabela direto com o schema definitivo
        CREATE TABLE professional_availability (
            id          BIGSERIAL PRIMARY KEY,
            professional_id BIGINT NOT NULL REFERENCES professional(id),
            available_date  DATE   NOT NULL,
            start_time      TIME   NOT NULL,
            end_time        TIME   NOT NULL
        );
    ELSE
        -- Banco existente: remove dados incompatíveis e troca a coluna
        DELETE FROM professional_availability;
        ALTER TABLE professional_availability DROP COLUMN IF EXISTS day_of_week;

        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'professional_availability' AND column_name = 'available_date'
        ) THEN
            ALTER TABLE professional_availability ADD COLUMN available_date DATE NOT NULL;
        END IF;
    END IF;
END $$;

-- Remove constraints únicas obsoletas (criadas pelo ddl-auto=update em versões antigas)
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'professional_availability'::regclass
          AND contype = 'u'
          AND conname <> 'uq_prof_avail_date_time'
    LOOP
        EXECUTE 'ALTER TABLE professional_availability DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

-- Garante a constraint canônica (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'professional_availability'::regclass
          AND conname = 'uq_prof_avail_date_time'
    ) THEN
        ALTER TABLE professional_availability
            ADD CONSTRAINT uq_prof_avail_date_time
            UNIQUE (professional_id, available_date, start_time);
    END IF;
END $$;
