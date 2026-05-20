-- Migrates professional_availability from day_of_week (weekly recurrence) to available_date (specific dates).
-- Old weekly data is semantically incompatible with the LocalDate-based model and is discarded.

-- Clear old rows so we can safely add a NOT NULL column without a default
DELETE FROM professional_availability;

-- Remove the old weekly-recurrence column
ALTER TABLE professional_availability DROP COLUMN IF EXISTS day_of_week;

-- Add the new column (table is empty so NOT NULL without a default is safe)
ALTER TABLE professional_availability ADD COLUMN available_date DATE NOT NULL;

-- Drop any stale unique constraints created by Hibernate ddl-auto=update
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'professional_availability'::regclass AND contype = 'u'
    LOOP
        EXECUTE 'ALTER TABLE professional_availability DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

-- Add the constraint that matches the @UniqueConstraint in the entity
ALTER TABLE professional_availability
    ADD CONSTRAINT uq_prof_avail_date_time UNIQUE (professional_id, available_date, start_time);
