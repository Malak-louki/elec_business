-- =============================================================
--  V2__normalize_user_role_join_table.sql
--  Objectif : aligner la table de jointure ManyToMany User<->Role
--             sur les conventions du code (user_id / role_id)
-- =============================================================

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS user_role;

CREATE TABLE user_role (
                           user_id VARCHAR(36) NOT NULL,
                           role_id VARCHAR(36) NOT NULL,
                           PRIMARY KEY (user_id, role_id),
                           CONSTRAINT fk_user_role_user
                               FOREIGN KEY (user_id) REFERENCES user_table(id)
                                   ON UPDATE CASCADE ON DELETE CASCADE,
                           CONSTRAINT fk_user_role_role
                               FOREIGN KEY (role_id) REFERENCES role(id)
                                   ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET = utf8mb4;  -- pas de COLLATE explicite

CREATE INDEX idx_user_role_user ON user_role(user_id);
CREATE INDEX idx_user_role_role ON user_role(role_id);

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
