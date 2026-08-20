-- V2__crear_tabla_sugerencias_rifas.sql
CREATE TABLE sugerencias_rifas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    premio_sugerido VARCHAR(255) NOT NULL,
    contacto_usuario VARCHAR(255),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);