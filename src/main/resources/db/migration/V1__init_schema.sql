-- =============================================================
--  V1__init_uuid_schema.sql
--  Schéma initial (propre) en UUID pour Electricity Business
--  Auteur : Malak
--  Date   : 2025-11
--  Remarques :
--   - Toutes les PK sont en VARCHAR(36) (UUID généré par l'app / JPA)
--   - Timestamps audit : created_at / updated_at (TIMESTAMP(6))
--   - Noms de tables/colonnes alignés avec tes entités actuelles :
--       User            -> table user_table
--       Address         -> table address
--       ChargingLocation-> table charging_location
--       ChargingStation -> table charging_station
--       Availability    -> table availability
--       Booking         -> table booking
--       Payment         -> table payment
--       UserValidation  -> table user_validation
--       Role            -> table role
--       user_role (JoinTable) avec colonnes Id_User / Id_Role (conforme au code)
-- =============================================================

-- Sécurité
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================
-- TABLE: role
-- =============================================================
CREATE TABLE role (
                      id           VARCHAR(36)     NOT NULL,
                      name         VARCHAR(100) NOT NULL UNIQUE,
                      created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                      updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                      PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================
-- TABLE: address
-- =============================================================
CREATE TABLE address (
                         id           VARCHAR(36)     NOT NULL,
                         street       VARCHAR(255) NOT NULL,
                         number       VARCHAR(50)  NULL,
                         city         VARCHAR(100) NOT NULL,
                         postal_code  VARCHAR(20)  NOT NULL,
                         country      VARCHAR(100) NOT NULL DEFAULT 'France',
                         created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                         PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================
-- TABLE: user_table (User)
-- =============================================================
CREATE TABLE user_table (
                            id            VARCHAR(36)     NOT NULL,
                            first_name    VARCHAR(50)  NOT NULL,
                            last_name     VARCHAR(50)  NOT NULL,
                            email         VARCHAR(255) NOT NULL,
                            password      VARCHAR(100) NOT NULL,
                            phone         VARCHAR(20)  NULL,
                            user_status   VARCHAR(20)  NULL,   -- Enum mappé en STRING côté Java recommandé
                            date_of_birth DATE         NULL,
                            address_id    VARCHAR(36)     NULL,
                            created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                            updated_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                            PRIMARY KEY (id),
                            CONSTRAINT uk_user_email UNIQUE (email),
                            CONSTRAINT uk_user_phone UNIQUE (phone),
                            CONSTRAINT fk_user_address
                                FOREIGN KEY (address_id) REFERENCES address(id)
                                    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_address ON user_table(address_id);

-- =============================================================
-- TABLE: charging_location
-- =============================================================
CREATE TABLE charging_location (
                                   id           VARCHAR(36)     NOT NULL,
                                   available    TINYINT(1)   NULL,
                                   latitude     DECIMAL(10,8) NOT NULL,
                                   longitude    DECIMAL(11,8) NOT NULL,
                                   address_id   VARCHAR(36)     NOT NULL,
                                   created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                   PRIMARY KEY (id),
                                   CONSTRAINT fk_charging_location_address
                                       FOREIGN KEY (address_id) REFERENCES address(id)
                                           ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_charging_location_address ON charging_location(address_id);

-- =============================================================
-- TABLE: availability
-- =============================================================
CREATE TABLE availability (
                              id           VARCHAR(36)    NOT NULL,
                              day          DATE        NOT NULL,
                              start_time   TIME        NOT NULL,
                              end_time     TIME        NOT NULL,
                              created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              updated_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                              PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_availability_day ON availability(day);

-- =============================================================
-- TABLE: charging_station
-- =============================================================
CREATE TABLE charging_station (
                                  id                     VARCHAR(36)     NOT NULL,
                                  name                   VARCHAR(100) NOT NULL,
                                  hourly_price           DECIMAL(5,2) NOT NULL,
                                  charging_power         VARCHAR(15)  NULL,       -- Ex: "7.4 kW", "22 kW"
                                  instruction            TEXT         NULL,
                                  has_stand              TINYINT(1)   NULL,
                                  media                  VARCHAR(512) NULL,       -- URL image ou média
                                  available              TINYINT(1)   NULL,
                                  user_id                VARCHAR(36)     NOT NULL,   -- propriétaire (User)
                                  charging_location_id   VARCHAR(36)     NOT NULL,   -- FK vers ChargingLocation
                                  created_at             TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  updated_at             TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_station_user
                                      FOREIGN KEY (user_id) REFERENCES user_table(id)
                                          ON UPDATE CASCADE ON DELETE RESTRICT,
                                  CONSTRAINT fk_station_location
                                      FOREIGN KEY (charging_location_id) REFERENCES charging_location(id)
                                          ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cs_user      ON charging_station(user_id);
CREATE INDEX idx_cs_location  ON charging_station(charging_location_id);

-- =============================================================
-- TABLE: charging_station_availability (JoinTable)
-- =============================================================
CREATE TABLE charging_station_availability (
                                               charging_station_id VARCHAR(36) NOT NULL,
                                               availability_id     VARCHAR(36) NOT NULL,
                                               PRIMARY KEY (charging_station_id, availability_id),
                                               CONSTRAINT fk_csa_station
                                                   FOREIGN KEY (charging_station_id) REFERENCES charging_station(id)
                                                       ON UPDATE CASCADE ON DELETE CASCADE,
                                               CONSTRAINT fk_csa_availability
                                                   FOREIGN KEY (availability_id) REFERENCES availability(id)
                                                       ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_csa_station ON charging_station_availability (charging_station_id);
CREATE INDEX idx_csa_avail   ON charging_station_availability (availability_id);

-- =============================================================
-- TABLE: payment
-- =============================================================
CREATE TABLE payment (
                         id                        VARCHAR(36)     NOT NULL,
                         stripe_payment_intent_id  VARCHAR(255) NOT NULL,
                         payment_status            VARCHAR(30)  NOT NULL,
                         created_at                TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at                TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                         PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payment_status ON payment(payment_status);

-- =============================================================
-- TABLE: booking
-- =============================================================
CREATE TABLE booking (
                         id                   VARCHAR(36)    NOT NULL,
                         start_date           DATE        NOT NULL,
                         end_date             DATE        NOT NULL,
                         start_hour           TIME        NOT NULL,
                         end_hour             TIME        NOT NULL,
                         paid_amount          DECIMAL(5,2) NOT NULL,
                         booking_status       VARCHAR(20)  NULL,
                         invoice_path         VARCHAR(512) NULL,
                         user_id              VARCHAR(36)     NOT NULL,
                         payment_id           VARCHAR(36)     NULL,
                         charging_station_id  VARCHAR(36)     NOT NULL,
                         created_at           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                         PRIMARY KEY (id),
                         CONSTRAINT chk_booking_dates CHECK (
                             end_date > start_date OR (end_date = start_date AND end_hour > start_hour)
                             ),
                         CONSTRAINT fk_booking_user
                             FOREIGN KEY (user_id) REFERENCES user_table(id)
                                 ON UPDATE CASCADE ON DELETE RESTRICT,
                         CONSTRAINT fk_booking_payment
                             FOREIGN KEY (payment_id) REFERENCES payment(id)
                                 ON UPDATE CASCADE ON DELETE SET NULL,
                         CONSTRAINT fk_booking_station
                             FOREIGN KEY (charging_station_id) REFERENCES charging_station(id)
                                 ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_booking_user     ON booking(user_id);
CREATE INDEX idx_booking_station  ON booking(charging_station_id);
CREATE INDEX idx_booking_payment  ON booking(payment_id);

-- =============================================================
-- TABLE: user_validation
-- =============================================================
CREATE TABLE user_validation (
                                 id                 VARCHAR(36)     NOT NULL,
                                 confirmation_code  VARCHAR(100) NOT NULL,
                                 validated_at       DATETIME(6)  NULL,
                                 user_id            VARCHAR(36)     NOT NULL,
                                 created_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 updated_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                 PRIMARY KEY (id),
                                 CONSTRAINT fk_user_validation_user
                                     FOREIGN KEY (user_id) REFERENCES user_table(id)
                                         ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_validation_user ON user_validation(user_id);

-- =============================================================
-- TABLE: user_role (JoinTable pour User<->Role)
--  IMPORTANT : tes entités utilisent Id_User / Id_Role comme noms de colonnes
-- =============================================================
CREATE TABLE user_role (
                           Id_User VARCHAR(36) NOT NULL,
                           Id_Role VARCHAR(36) NOT NULL,
                           PRIMARY KEY (Id_User, Id_Role),
                           CONSTRAINT fk_user_role_user FOREIGN KEY (Id_User) REFERENCES user_table(id)
                               ON UPDATE CASCADE ON DELETE CASCADE,
                           CONSTRAINT fk_user_role_role FOREIGN KEY (Id_Role) REFERENCES role(id)
                               ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_user_role_user ON user_role (Id_User);
CREATE INDEX idx_user_role_role ON user_role (Id_Role);

-- Données de base (roles)
INSERT INTO role (id, name) VALUES
                                (REPLACE(UUID(),'-',''), 'USER'),
                                (REPLACE(UUID(),'-',''), 'OWNER'),
                                (REPLACE(UUID(),'-',''), 'ADMIN');

SET FOREIGN_KEY_CHECKS = 1;
