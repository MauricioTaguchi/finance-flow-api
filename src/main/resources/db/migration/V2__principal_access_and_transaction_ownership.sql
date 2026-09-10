CREATE TABLE auth_principals (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    active BOOLEAN NOT NULL,
    token_hash VARCHAR(64) UNIQUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE auth_administration_lock (id INTEGER PRIMARY KEY CHECK (id = 1));
INSERT INTO auth_administration_lock (id) VALUES (1);

ALTER TABLE transactions ADD COLUMN owner_id UUID;
ALTER TABLE transactions ADD CONSTRAINT fk_transaction_owner
    FOREIGN KEY (owner_id) REFERENCES auth_principals(id);
CREATE INDEX ix_transaction_owner_id ON transactions(owner_id, id);

-- Existing rows deliberately remain NULL and cannot be read through the API.
-- Assign legacy ownership only after an operator verifies its attribution.
