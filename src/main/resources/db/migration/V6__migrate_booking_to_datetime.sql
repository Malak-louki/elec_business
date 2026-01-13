-- ═════════════════════════════════════════════════════════════════
-- V6__migrate_booking_to_datetime.sql (VERSION FINALE - MySQL)
-- OBJECTIF : Migrer la table booking de LocalDate+LocalTime vers LocalDateTime
--
-- CORRECTION : Syntaxe MySQL pour supprimer une contrainte CHECK
-- ═════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────
-- ÉTAPE 0 : Supprimer la contrainte CHECK (syntaxe MySQL)
-- ─────────────────────────────────────────────────────────────────

-- Note : DROP CONSTRAINT IF EXISTS n'existe pas en MySQL
-- On utilise DROP CHECK à la place (MySQL 8.0+)
ALTER TABLE booking DROP CHECK chk_booking_dates;

-- ─────────────────────────────────────────────────────────────────
-- ÉTAPE 1 : Ajouter les nouvelles colonnes DATETIME
-- ─────────────────────────────────────────────────────────────────

ALTER TABLE booking
    ADD COLUMN start_date_time DATETIME(6) NULL COMMENT 'Date et heure de début (nouveau format)',
    ADD COLUMN end_date_time DATETIME(6) NULL COMMENT 'Date et heure de fin (nouveau format)';

-- ─────────────────────────────────────────────────────────────────
-- ÉTAPE 2 : Migrer les données existantes
-- ─────────────────────────────────────────────────────────────────

-- Combiner start_date + start_hour → start_date_time
UPDATE booking
SET start_date_time = TIMESTAMP(start_date, start_hour)
WHERE start_date IS NOT NULL AND start_hour IS NOT NULL;

-- Combiner end_date + end_hour → end_date_time
UPDATE booking
SET end_date_time = TIMESTAMP(end_date, end_hour)
WHERE end_date IS NOT NULL AND end_hour IS NOT NULL;

-- ─────────────────────────────────────────────────────────────────
-- ÉTAPE 3 : Rendre les nouvelles colonnes NOT NULL
-- ─────────────────────────────────────────────────────────────────

ALTER TABLE booking
    MODIFY COLUMN start_date_time DATETIME(6) NOT NULL,
    MODIFY COLUMN end_date_time DATETIME(6) NOT NULL;

-- ─────────────────────────────────────────────────────────────────
-- ÉTAPE 4 : Supprimer les anciennes colonnes
-- ─────────────────────────────────────────────────────────────────

ALTER TABLE booking
    DROP COLUMN start_date,
    DROP COLUMN start_hour,
    DROP COLUMN end_date,
    DROP COLUMN end_hour;

-- ─────────────────────────────────────────────────────────────────
-- ÉTAPE 5 : Ajouter la NOUVELLE contrainte CHECK (sur les nouvelles colonnes)
-- ─────────────────────────────────────────────────────────────────

ALTER TABLE booking
    ADD CONSTRAINT chk_booking_datetimes
        CHECK (end_date_time > start_date_time);

-- ─────────────────────────────────────────────────────────────────
-- ÉTAPE 6 : Renommer paid_amount → total_amount (si nécessaire)
-- ─────────────────────────────────────────────────────────────────

-- ⚠️ Si vous avez DÉJÀ 'total_amount', commentez cette ligne
ALTER TABLE booking
    CHANGE COLUMN paid_amount total_amount DECIMAL(7,2) NOT NULL
        COMMENT 'Montant total calculé et dû';

-- ─────────────────────────────────────────────────────────────────
-- ✅ MIGRATION TERMINÉE
-- ─────────────────────────────────────────────────────────────────