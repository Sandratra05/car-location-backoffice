-- Réinitialisation de la base `carlocation`
-- Ce script recrée les tables conformément aux définitions fournies via `\d`.
-- Ne crée PAS le type `type_carburant` (il doit exister séparément si nécessaire).

BEGIN;

-- Supprimer les tables (ordre adapté aux contraintes FK)
DROP TABLE IF EXISTS distance CASCADE;
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

COMMIT;

-- DONNEES DE TEST
--BEGIN;

-- HOTELS (1 = aéroport)
INSERT INTO hotel (code, libelle, aeroport) VALUES
  ('AIR-MAD', 'Aéroport de Madagascar', TRUE),
  ('IBIS-01', 'Hotel Ibis', FALSE),
  ('LOUVRE-01', 'Hotel du Louvre', FALSE),
  ('CARLTON-01', 'Hotel Carlton', FALSE),
  ('COLBERT-01', 'Hotel Colbert', FALSE);
-- PARAMETRE
INSERT INTO parametre (vitesse_moyenne_kmh, temps_attente_min) VALUES
  (30.00, 30);

-- VEHICULES
INSERT INTO vehicule (reference, nb_place, type_carburant) VALUES
  -- ('VH-001', 10, 'DIESEL'),
  -- ('VH-002', 10, 'ESSENCE'),
  -- ('VH-003', 5,  'HYBRIDE'),
  ('VH-004', 18, 'DIESEL'),
  ('VH-005', 10,  'DIESEL');

-- RESERVATIONS (4 sur la même date 2026-03-15, 1 sur 2026-03-16)
-- INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client) VALUES
--   (8,  TIMESTAMP '2026-03-15 08:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C001'),
--   (10, TIMESTAMP '2026-03-15 08:15:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002'),
--   (11, TIMESTAMP '2026-03-15 08:30:00', (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 'C004'),
--   (4,  TIMESTAMP '2026-03-15 09:00:00', (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 'C003'),
--   (3,  TIMESTAMP '2026-03-16 10:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C005');

-- POUR TESTER LA SUPERPOSITION DES RESERVATIONS (exemple : 8 passagers à 8h puis 2 passagers à 8h15, alors que le véhicule a une capacité de 10, on doit pouvoir affecter les 2 passagers de la 2ème réservation au même véhicule que les 8 passagers de la 1ère réservation)
INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client) VALUES
 (13,  TIMESTAMP '2026-03-15 08:15:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C001'),
 (6, TIMESTAMP '2026-03-15 08:20:00', (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 'C002'),
 (3, TIMESTAMP '2026-03-15 08:40:00', (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 'C004'),
 (1,  TIMESTAMP '2026-03-15 09:30:00', (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 'C003');
--  (3,  TIMESTSAMP '2026-03-16 10:00:00', (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 'C005');

-- DISTANCES (aller: aéroport -> hotels)
-- INSERT INTO distance (from_hotel_id, to_hotel_id, kilometre) VALUES
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 15.00),
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 20.00),
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 25.00),
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 20.00),
--   ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), 0.00);



-- DISTANCES (aéroport → hôtels)
INSERT INTO distance (from_hotel_id, to_hotel_id, kilometre) VALUES
  ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), 15.00),
  ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 20.00),
  ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 25.00),
  ((SELECT id_hotel FROM hotel WHERE code='AIR-MAD' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 20.00),

  ((SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), 8.00),
  ((SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 12.00),
  ((SELECT id_hotel FROM hotel WHERE code='IBIS-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 15.00),
  ((SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), 7.00),
  ((SELECT id_hotel FROM hotel WHERE code='LOUVRE-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 10.00),
  ((SELECT id_hotel FROM hotel WHERE code='COLBERT-01' LIMIT 1), (SELECT id_hotel FROM hotel WHERE code='CARLTON-01' LIMIT 1), 6.00);
--COMMIT;
--```