-- =============================================================
--  V3__seed_initial_roles.sql
--  Objectif : injecter les rôles de base pour l’authentification
--             (USER / OWNER / ADMIN) dès l’init du schéma.
--  Auteur : Malak
--  Date   : 2026-06
-- =============================================================

INSERT IGNORE INTO role (id, name)
VALUES
    (REPLACE(UUID(),'-',''), 'USER'),
    (REPLACE(UUID(),'-',''), 'OWNER'),
    (REPLACE(UUID(),'-',''), 'ADMIN');