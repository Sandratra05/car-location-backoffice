-- Table pour définir les heures de disponibilité des véhicules
-- Un véhicule ne sera disponible qu'à partir de l'heure spécifiée (pour une date donnée)

CREATE TABLE IF NOT EXISTS disponibilite (
    id SERIAL PRIMARY KEY,
    id_vehicule INTEGER NOT NULL REFERENCES vehicule(id) ON DELETE CASCADE,
    date_disponible DATE NOT NULL,
    heure_disponible TIME NOT NULL,
    UNIQUE(id_vehicule, date_disponible)
);

-- Exemple : VH-005 (id=2) ne sera disponible qu'à partir de 15h le 19/03/2026
-- INSERT INTO disponibilite (id_vehicule, date_disponible, heure_disponible) VALUES (2, '2026-03-19', '15:00:00');

-- Index pour accélérer les recherches
CREATE INDEX IF NOT EXISTS idx_disponibilite_vehicule_date ON disponibilite(id_vehicule, date_disponible);
