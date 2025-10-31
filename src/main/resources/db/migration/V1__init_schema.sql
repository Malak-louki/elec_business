-- =============================================================
--  V1__init_schema.sql
--  Schéma initial de la plateforme Electricity Business
--  Auteur : Malak Louki
--  Date   : 2025-10
-- =============================================================

-- =====================
-- TABLE: role
-- =====================
CREATE TABLE role (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(50) NOT NULL UNIQUE
);

-- =====================
-- TABLE: user_eb
-- =====================
CREATE TABLE user_eb (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         firstname VARCHAR(100) NOT NULL,
                         lastname VARCHAR(100) NOT NULL,
                         email VARCHAR(150) NOT NULL UNIQUE,
                         password VARCHAR(255) NOT NULL,
                         phone VARCHAR(20),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         role_id BIGINT,
                         CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(id)
);

-- =====================
-- TABLE: address
-- =====================
CREATE TABLE address (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         street VARCHAR(255) NOT NULL,
                         city VARCHAR(100) NOT NULL,
                         zipcode VARCHAR(20) NOT NULL,
                         country VARCHAR(100) DEFAULT 'France'
);

-- =====================
-- TABLE: charging_location
-- =====================
CREATE TABLE charging_location (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   name VARCHAR(100) NOT NULL,
                                   description TEXT,
                                   latitude DECIMAL(9,6) NOT NULL,
                                   longitude DECIMAL(9,6) NOT NULL,
                                   address_id BIGINT NOT NULL,
                                   owner_id BIGINT NOT NULL,
                                   CONSTRAINT fk_location_address FOREIGN KEY (address_id) REFERENCES address(id),
                                   CONSTRAINT fk_location_owner FOREIGN KEY (owner_id) REFERENCES user_eb(id)
);

-- =====================
-- TABLE: charging_station
-- =====================
CREATE TABLE charging_station (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  name VARCHAR(100) NOT NULL,
                                  connector_type VARCHAR(50) NOT NULL,       -- Type de connecteur (Type2, CCS, CHAdeMO, etc.)
                                  power_kw DECIMAL(5,2) NOT NULL,            -- Puissance (kW)
                                  price_per_hour DECIMAL(6,2) NOT NULL,      -- Tarif horaire (€)
                                  is_available BOOLEAN DEFAULT TRUE,
                                  location_id BIGINT NOT NULL,
                                  CONSTRAINT fk_station_location FOREIGN KEY (location_id) REFERENCES charging_location(id)
);

-- =====================
-- TABLE: booking
-- =====================
CREATE TABLE booking (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         start_date DATE NOT NULL,
                         start_time TIME NOT NULL,
                         end_date DATE NOT NULL,
                         end_time TIME NOT NULL,
                         total_price DECIMAL(8,2) NOT NULL,
                         status VARCHAR(20) DEFAULT 'PENDING',
                         user_id BIGINT NOT NULL,
                         station_id BIGINT NOT NULL,
                         CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES user_eb(id),
                         CONSTRAINT fk_booking_station FOREIGN KEY (station_id) REFERENCES charging_station(id),
                         CONSTRAINT chk_booking_dates CHECK (
                             end_date > start_date OR
                             (end_date = start_date AND end_time > start_time)
                             )
);

-- =====================
-- Indexes & optimisations de base
-- =====================
CREATE INDEX idx_station_location ON charging_station(location_id);
CREATE INDEX idx_booking_user ON booking(user_id);
CREATE INDEX idx_booking_station ON booking(station_id);

-- =====================
-- Données initiales minimales
-- =====================
INSERT INTO role (name) VALUES ('USER'), ('OWNER'), ('ADMIN');
