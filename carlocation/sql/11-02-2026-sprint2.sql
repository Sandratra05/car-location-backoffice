
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'type_carburant') THEN
        CREATE TYPE type_carburant AS ENUM ('DIESEL', 'ESSENCE', 'HYBRIDE', 'ELECTRIQUE');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS vehicule (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(100) NOT NULL UNIQUE,
    nb_place INT NOT NULL CHECK (nb_place > 0),
    type_carburant type_carburant NOT NULL
);


CREATE TABLE IF NOT EXISTS token (
    id SERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    date_expiration TIMESTAMP NOT NULL
);

-- ========================================
--  Index pour améliorer les performances
-- ========================================
CREATE INDEX IF NOT EXISTS idx_token_value ON token(token);
CREATE INDEX IF NOT EXISTS idx_token_expiration ON token(date_expiration);


INSERT INTO vehicule (reference, nb_place, type_carburant) VALUES
    ('VH-001-DIESEL', 5, 'DIESEL'),
    ('VH-002-ESSENCE', 4, 'ESSENCE'),
    ('VH-003-HYBRIDE', 5, 'HYBRIDE'),
    ('VH-004-ELECTRIQUE', 2, 'ELECTRIQUE'),
    ('VH-005-DIESEL', 7, 'DIESEL'),
    ('VH-006-ESSENCE', 5, 'ESSENCE')
ON CONFLICT (reference) DO NOTHING;

-- ========================================
--  Données de test - Tokens
-- ========================================
-- Token valide jusqu'au 28-02-2026
INSERT INTO token (token, date_expiration) VALUES
    ('test-token-123456789', '2026-02-28 23:59:59')
ON CONFLICT (token) DO NOTHING;

-- Token valide jusqu'au 31-12-2026
INSERT INTO token (token, date_expiration) VALUES
    ('test-token-999999999', '2026-12-31 23:59:59')
ON CONFLICT (token) DO NOTHING;

-- Token expiré (pour tests)
INSERT INTO token (token, date_expiration) VALUES
    ('expired-token-000', '2024-01-01 00:00:00')
ON CONFLICT (token) DO NOTHING;