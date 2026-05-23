-- V5 Migration: Create Admin Credentials Table
-- Structural prerequisite for database-backed Basic Authentication.
-- All users in this table are administrators. The role is injected statically
-- in the UserDetailsService to keep the table schema minimal and aligned with
-- the project scope (no RBAC).

CREATE TABLE tb_usuarios_admin (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_admin_username UNIQUE (username)
);

CREATE INDEX idx_admin_username ON tb_usuarios_admin(username);

-- Seed data: default administrator account
-- Password hash generated via BCrypt for plaintext 'admin123'
INSERT INTO tb_usuarios_admin (username, password, created_at, updated_at)
VALUES (
    'admin',
    '$2a$10$aGOmmFeOomV7qbzj2ojIeezOz.6.sd8s6As5JI6QfGsadMtXrrJdy',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
