-- =============================================================
--  V4__recreate_user_role_join_table.sql
--  Objectif : s'assurer que la table user_role existe
--             avec la bonne structure (user_id / role_id).
-- =============================================================

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS user_role (
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
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
