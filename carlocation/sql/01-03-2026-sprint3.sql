-- Création de la table parametre (PostgreSQL)
CREATE TABLE parametre (
  id_parametre SERIAL PRIMARY KEY,
  vitesse_moyenne_kmh NUMERIC(5,2) NOT NULL,
  temps_attente_min INTEGER NOT NULL
);

CREATE TABLE distance (
  id_distance SERIAL PRIMARY KEY,
  "from" INTEGER NOT NULL REFERENCES hotel(id_hotel),
  "to" INTEGER NOT NULL REFERENCES hotel(id_hotel),
  kilometre NUMERIC(8,2) NOT NULL
);
