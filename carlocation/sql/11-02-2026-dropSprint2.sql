-- Supprimer les index
DROP INDEX IF EXISTS idx_token_value;
DROP INDEX IF EXISTS idx_token_expiration;

-- Supprimer les tables
DROP TABLE IF EXISTS token CASCADE;
DROP TABLE IF EXISTS vehicule CASCADE;

-- Supprimer l'enum
DROP TYPE IF EXISTS type_carburant CASCADE;