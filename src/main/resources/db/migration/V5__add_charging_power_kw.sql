-- =============================================================
--  V5__add_charging_power_kw.sql
--  Objectif : Ajouter un champ numérique pour la puissance
--             et migrer les données existantes
--  Auteur : Assistant
--  Date : 2026-01-01
-- =============================================================

-- 1. Ajouter la nouvelle colonne numérique
ALTER TABLE charging_station
    ADD COLUMN charging_power_kw DECIMAL(6,2) NULL COMMENT 'Puissance en kW (numérique pour les calculs)';

-- 2. Migrer les données existantes (extraire le nombre de la chaîne)
-- MySQL : REGEXP_REPLACE disponible depuis 8.0
-- Exemples : "7.4 kW" → 7.4, "22 kW" → 22, "150 kW" → 150
UPDATE charging_station
SET charging_power_kw = CAST(
        REGEXP_REPLACE(charging_power, '[^0-9.]', '') AS DECIMAL(6,2)
                        )
WHERE charging_power IS NOT NULL;

-- 3. Pour les bornes sans puissance définie (si il y en a), mettre une valeur par défaut
UPDATE charging_station
SET charging_power_kw = 7.4
WHERE charging_power_kw IS NULL;

-- 4. Rendre la colonne NOT NULL maintenant que les données sont migrées
ALTER TABLE charging_station
    MODIFY COLUMN charging_power_kw DECIMAL(6,2) NOT NULL;

-- 5. Ajouter un index pour les recherches par puissance
CREATE INDEX idx_charging_station_power_kw ON charging_station(charging_power_kw);

-- 6. Ajouter une contrainte CHECK pour valider la plage (0 à 350 kW)
ALTER TABLE charging_station
    ADD CONSTRAINT chk_power_range CHECK (charging_power_kw >= 0 AND charging_power_kw <= 350);