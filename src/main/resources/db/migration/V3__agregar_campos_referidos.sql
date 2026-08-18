-- Agrega las columnas de código de referido y puntos a la tabla usuarios
ALTER TABLE usuarios ADD COLUMN codigo_referido VARCHAR(20) UNIQUE;
ALTER TABLE usuarios ADD COLUMN puntos INT NOT NULL DEFAULT 0;