-- Réinitialisation de la base `carlocation`
-- Ce script recrée les tables conformément aux définitions fournies via `\d`.
-- Ne crée PAS le type `type_carburant` (il doit exister séparément si nécessaire).

BEGIN;

-- Supprimer les tables (ordre adapté aux contraintes FK)
DROP TABLE IF EXISTS disponibilite CASCADE;
DROP TABLE IF EXISTS assignation CASCADE;
DROP TABLE IF EXISTS distance CASCADE;
DROP TABLE IF EXISTS assignation CASCADE;
DROP TABLE IF EXISTS reservation CASCADE;
DROP TABLE IF EXISTS token CASCADE;
DROP TABLE IF EXISTS vehicule CASCADE;
DROP TABLE IF EXISTS parametre CASCADE;
DROP TABLE IF EXISTS hotel CASCADE;

-- Recréation de la table hotel (selon \d fourni)
CREATE TABLE hotel (
  id_hotel SERIAL PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  libelle VARCHAR(100) NOT NULL,
  aeroport BOOLEAN NOT NULL DEFAULT FALSE
);


-- Recréation de la table parametre (sprint 3)
CREATE TABLE parametre (
  id_parametre SERIAL PRIMARY KEY,
  vitesse_moyenne_kmh NUMERIC(5,2) NOT NULL,
  temps_attente_min INTEGER NOT NULL
);

-- Recréation de la table reservation
CREATE TABLE reservation (
  id_reservation SERIAL PRIMARY KEY,
  nb_passager INTEGER NOT NULL,
  date_heure_arrivee TIMESTAMP NOT NULL,
  id_hotel INTEGER NOT NULL REFERENCES hotel(id_hotel),
  id_client VARCHAR(4) NOT NULL
);

-- Recréation de la table token
CREATE TABLE token (
  id SERIAL PRIMARY KEY,
  token VARCHAR(255) NOT NULL UNIQUE,
  date_expiration TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_token_expiration ON token(date_expiration);
CREATE INDEX IF NOT EXISTS idx_token_value ON token(token);

-- Recréation de la table vehicule 
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'type_carburant') THEN
    CREATE TYPE type_carburant AS ENUM ('DIESEL','ESSENCE','HYBRIDE','ELECTRIQUE');
  END IF;
END $$;

CREATE TABLE vehicule (
  id SERIAL PRIMARY KEY,
  reference VARCHAR(100) NOT NULL UNIQUE,
  nb_place INTEGER NOT NULL CHECK (nb_place > 0),
  type_carburant type_carburant NOT NULL
);

-- Recréation de la table distance (colonnes `from` et `to`)
CREATE TABLE distance (
  id_distance SERIAL PRIMARY KEY,
  from_hotel_id INTEGER NOT NULL REFERENCES hotel(id_hotel),
  to_hotel_id INTEGER NOT NULL REFERENCES hotel(id_hotel),
  kilometre NUMERIC(8,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_distance_from_to ON distance(from_hotel_id, to_hotel_id);

-- Recréation de la table assignation (liaison véhicule ↔ réservation)
CREATE TABLE assignation (
  id SERIAL PRIMARY KEY,
  id_vehicule INTEGER NOT NULL REFERENCES vehicule(id),
  id_reservation INTEGER NOT NULL REFERENCES reservation(id_reservation),
  date_depart TIMESTAMP,
  date_retour TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_assignation_vehicule ON assignation(id_vehicule);
CREATE INDEX IF NOT EXISTS idx_assignation_reservation ON assignation(id_reservation);

CREATE TABLE IF NOT EXISTS disponibilite (
    id SERIAL PRIMARY KEY,
    id_vehicule INTEGER NOT NULL REFERENCES vehicule(id) ON DELETE CASCADE,
    date_disponible DATE NOT NULL,
    heure_disponible TIME NOT NULL,
    UNIQUE(id_vehicule, date_disponible)
);

CREATE INDEX IF NOT EXISTS idx_disponibilite_vehicule_date ON disponibilite(id_vehicule, date_disponible);

COMMIT;

-- DONNEES DE TEST
--BEGIN;

-- HOTELS (1 = aéroport)
INSERT INTO hotel (code, libelle, aeroport) VALUES
  ('AIR-MAD', 'Aéroport de Madagascar', TRUE),
  ('HOTEL1', 'Hotel 1', FALSE),
  ('HOTEL2', 'Hotel 2', FALSE);
  -- ('CARLTON-01', 'Hotel Carlton', FALSE),
  -- ('COLBERT-01', 'Hotel Colbert', FALSE);
-- PARAMETRE
INSERT INTO parametre (vitesse_moyenne_kmh, temps_attente_min) VALUES
  (50.00, 30);

-- VEHICULES
INSERT INTO vehicule (reference, nb_place, type_carburant) VALUES
  ('VEHICULE1', 5, 'DIESEL'),
  ('VEHICULE2', 5, 'ESSENCE'),
  ('VEHICULE3', 12,  'DIESEL'),
   ('VEHICULE4', 9, 'DIESEL'),
   ('VEHICULE5', 12,  'ESSENCE');

-- RESERVATIONS (4 sur la même date 2026-03-15, 1 sur 2026-03-16)
-- INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client) VALUES
--   (8,  TIMESTAMP '2026-03-15 08:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C001'),
--   (10, TIMESTAMP '2026-03-15 08:15:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002'),
--   (11, TIMESTAMP '2026-03-15 08:30:00', (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 'C004'),
--   (4,  TIMESTAMP '2026-03-15 09:00:00', (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 'C003'),
--   (3,  TIMESTAMP '2026-03-16 10:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C005');

-- POUR TESTER LA SUPERPOSITION DES RESERVATIONS (exemple : 8 passagers à 8h puis 2 passagers à 8h15, alors que le véhicule a une capacité de 10, on doit pouvoir affecter les 2 passagers de la 2ème réservation au même véhicule que les 8 passagers de la 1ère réservation)
--INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client) VALUES
-- (9,  TIMESTAMP '2026-03-15 08:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C001'),
-- (3, TIMESTAMP '2026-03-15 08:15:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002');
--  (3, TIMESTAMP '2026-03-15 08:30:00', (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 'C004'),
--  (5,  TIMESTAMP '2026-03-15 08:25:00', (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 'C003'),
 INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client) VALUES
  (7,  TIMESTAMP '2026-03-19 09:00:00', (SELECT id_hotel FROM hotel WHERE code='HOTEL1' LIMIT 1), 'C001'),
  (20, TIMESTAMP '2026-03-19 08:00:00', (SELECT id_hotel FROM hotel WHERE code='HOTEL2' LIMIT 1), 'C002'),
  (3, TIMESTAMP '2026-03-19 09:10:00', (SELECT id_hotel FROM hotel WHERE code='HOTEL1' LIMIT 1), 'C003'),
  (10,  TIMESTAMP '2026-03-19 09:15:00', (SELECT id_hotel FROM hotel WHERE code='HOTEL1' LIMIT 1), 'C004'),
  (5,  TIMESTAMP '2026-03-19 09:20:00', (SELECT id_hotel FROM hotel WHERE code='HOTEL1' LIMIT 1), 'C005'),
 (12,  TIMESTAMP '2026-03-19 13:30:00', (SELECT id_hotel FROM hotel WHERE code='HOTEL1' LIMIT 1), 'C006');
--  (5,  TIMESTAMP '2026-03-15 12:10:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C001'),
--   (16, TIMESTAMP '2026-03-15 15:10:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002'),
--   (8, TIMESTAMP '2026-03-15 15:20:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002'),
  -- (6, TIMESTAMP '2026-03-15 12:15:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002'),
  -- (3, TIMESTAMP '2026-03-15 12:20:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002'),
--INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client) VALUES
-- (13,  TIMESTAMP '2026-03-15 08:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C001'),
-- (6, TIMESTAMP '2026-03-15 08:15:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002'),
-- (3, TIMESTAMP '2026-03-15 08:30:00', (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 'C004'),
-- (1,  TIMESTAMP '2026-03-15 09:35:00', (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 'C003');
--  (1,  TIMESTAMP '2026-03-15 08:25:00', (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 'C003'),
-- (15,  TIMESTAMP '2026-03-15 12:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C001'),
-- (5,  TIMESTAMP '2026-03-15 12:10:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C001'),
--  (8, TIMESTAMP '2026-03-15 12:20:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002');
--  (4, TIMESTAMP '2026-03-15 12:20:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002');

--  (3,  TIMESTAMP '2026-03-16 10:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C005');

-- DISTANCES (aller: aéroport -> hotels)  
-- INSERT INTO distance (from_hotel_id, to_hotel_id, kilometre) VALUES
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 15.00),
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 20.00),
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 25.00),
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 20.00),
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), 0.00);



-- DISTANCES (aéroport → hôtels)
INSERT INTO distance (from_hotel_id, to_hotel_id, kilometre) VALUES
  ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='HOTEL1' LIMIT 1), 90.00),
  ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='HOTEL2' LIMIT 1), 35.00),
  -- ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 25.00),
  -- ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 20.00),

  ((SELECT id_hotel FROM hotel WHERE code='HOTEL1' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='HOTEL2' LIMIT 1), 60.00);
  -- ((SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 12.00),
  -- ((SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 15.00),
  -- ((SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 7.00),
  -- ((SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 10.00),
  -- ((SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 6.00);
--COMMIT;
--```

INSERT INTO disponibilite (id_vehicule, date_disponible, heure_disponible) VALUES (5, '2026-03-19', '13:00:00');
INSERT INTO disponibilite (id_vehicule, date_disponible, heure_disponible) VALUES (1, '2026-03-19', '09:00:00');
INSERT INTO disponibilite (id_vehicule, date_disponible, heure_disponible) VALUES (2, '2026-03-19', '09:00:00');
INSERT INTO disponibilite (id_vehicule, date_disponible, heure_disponible) VALUES (3, '2026-03-19', '08:00:00');
INSERT INTO disponibilite (id_vehicule, date_disponible, heure_disponible) VALUES (4, '2026-03-19', '09:00:00');
