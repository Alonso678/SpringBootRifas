-- Eliminar los campos antiguos de la tabla usuarios si ya existían
ALTER TABLE usuarios DROP COLUMN IF EXISTS banco;
ALTER TABLE usuarios DROP COLUMN IF EXISTS clabe_interbancaria;
ALTER TABLE usuarios DROP COLUMN IF EXISTS titular_cuenta;

-- Crear la nueva tabla datos_bancarios relacionada con el usuario administrador
CREATE TABLE datos_bancarios (
    id BIGSERIAL PRIMARY KEY,
    banco VARCHAR(100) NOT NULL,
    clabe_interbancaria VARCHAR(18) NOT NULL UNIQUE,
    titular_cuenta VARCHAR(150) NOT NULL,
    usuario_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_usuario_banco FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);