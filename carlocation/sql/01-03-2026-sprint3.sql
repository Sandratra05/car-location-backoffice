-- Création de la table parametre (PostgreSQL)
CREATE TABLE parametre (
  id_parametre SERIAL PRIMARY KEY,
  vitesse_moyenne_kmh NUMERIC(5,2) NOT NULL,
  temps_attente_min INTEGER NOT NULL
);
