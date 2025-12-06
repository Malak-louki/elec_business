-- =============================================================
--  V3__seed_initial_roles.sql
--  Objectif : injecter les rôles de base pour l’authentification
--             (USER / OWNER / ADMIN) dès l’init du schéma.
--  Auteur : Malak
--  Date   : 2025-11
-- =============================================================

INSERT INTO role (id, name)
VALUES
    (UUID(), 'USER'),
    (UUID(), 'OWNER'),
    (UUID(), 'ADMIN');
