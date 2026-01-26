-- Insertion des rôles si ils n'existent pas déjà
INSERT INTO roles (name)
SELECT 'USER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'USER');

INSERT INTO roles (name)
SELECT 'OWNER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'OWNER');

INSERT INTO roles (name)
SELECT 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');