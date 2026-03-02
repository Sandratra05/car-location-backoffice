-- Recrée la table hotel avec les colonnes demandées (PostgreSQL)
DROP TABLE IF EXISTS hotel;

CREATE TABLE hotel (
  id_hotel SERIAL PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  libelle VARCHAR(100) NOT NULL,
  aeroport BOOLEAN NOT NULL DEFAULT FALSE
);

-- Vous pouvez ajouter des inserts si nécessaire
