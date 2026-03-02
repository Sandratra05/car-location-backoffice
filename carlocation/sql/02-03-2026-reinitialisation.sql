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
