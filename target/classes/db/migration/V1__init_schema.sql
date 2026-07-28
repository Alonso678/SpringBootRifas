-- Crear tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    rol VARCHAR(30) NOT NULL
);

-- Crear tabla de rifas
CREATE TABLE IF NOT EXISTS rifas (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio_boleto NUMERIC(10, 2) NOT NULL,
    total_boletos INT NOT NULL,
    fecha_sorteo TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL,
    imagen_url VARCHAR(255),
    video_url VARCHAR(500)
);

-- Crear tabla de boletos
CREATE TABLE IF NOT EXISTS boletos (
    id BIGSERIAL PRIMARY KEY,
    numero_boleto INT NOT NULL,
    rifa_id BIGINT NOT NULL,
    usuario_id BIGINT,
    estado VARCHAR(30) NOT NULL,
    CONSTRAINT fk_boletos_rifa FOREIGN KEY (rifa_id) REFERENCES rifas(id),
    CONSTRAINT fk_boletos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Crear tabla de compras
CREATE TABLE IF NOT EXISTS compras (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    rifa_id BIGINT NOT NULL,
    monto_total NUMERIC(10, 2) NOT NULL,
    estado_pago VARCHAR(30) NOT NULL,
    fecha_compra TIMESTAMP NOT NULL,
    CONSTRAINT fk_compras_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_compras_rifa FOREIGN KEY (rifa_id) REFERENCES rifas(id)
);

-- Tabla intermedia compra_boletos
CREATE TABLE IF NOT EXISTS compra_boletos (
    compra_id BIGINT NOT NULL,
    boleto_id BIGINT NOT NULL,
    PRIMARY KEY (compra_id, boleto_id),
    CONSTRAINT fk_cb_compra FOREIGN KEY (compra_id) REFERENCES compras(id),
    CONSTRAINT fk_cb_boleto FOREIGN KEY (boleto_id) REFERENCES boletos(id)
);

-- Insertar un usuario Administrador por admin123defecto (Contraseña: admin123 codificada en BCrypt)
-- Email: admin@rifas.com / Password: 
INSERT INTO usuarios (email, password, nombre, telefono, rol)
VALUES ('admin@rifas.com', '$2a$10$pz9R/5hvIBCg3f6FNlu/3eZTDkV6Tyd7MzM1i/neNpWVoa6P430CO', 'Administrador General', '5555555555', 'ROLE_ADMIN')
ON CONFLICT (email) DO NOTHING;
