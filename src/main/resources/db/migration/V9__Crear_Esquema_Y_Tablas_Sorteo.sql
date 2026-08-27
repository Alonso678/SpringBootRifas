-- Bloque anónimo en PL/pgSQL para verificar y crear el esquema de forma segura
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.schemata 
        WHERE schema_name = 'sorteos'
    ) THEN
        -- Si el usuario tiene permisos, esto lo creará dinámicamente.
        -- Si no los tiene, fallará aquí informando la falta de privilegios en lugar de avanzar a medias.
        EXECUTE 'CREATE SCHEMA sorteos';
    END IF;
END
$$;

-- Tabla de configuración del sorteo e histórico (regla de descarte y ganador)
CREATE TABLE IF NOT EXISTS sorteos.config_sorteo (
    id BIGSERIAL PRIMARY KEY,
    rifa_id BIGINT NOT NULL UNIQUE,
    boletos_a_descartar INT NOT NULL,
    sorteo_realizado BOOLEAN NOT NULL DEFAULT FALSE,
    boleto_ganador_id BIGINT,
    fecha_sorteo TIMESTAMP WITHOUT TIME ZONE
);

-- Tabla para persistir la identidad digital inalterable de los boletos
CREATE TABLE IF NOT EXISTS sorteos.boleto_digital (
    id BIGSERIAL PRIMARY KEY,
    boleto_id BIGINT NOT NULL,
    random_state VARCHAR(255) NOT NULL UNIQUE,
    sello_digital TEXT NOT NULL,
    fecha_emision TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar las búsquedas frecuentes por claves únicas y relaciones
CREATE INDEX IF NOT EXISTS idx_config_sorteo_rifa ON sorteos.config_sorteo(rifa_id);
CREATE INDEX IF NOT EXISTS idx_boleto_digital_boleto ON sorteos.boleto_digital(boleto_id);
CREATE INDEX IF NOT EXISTS idx_boleto_digital_random_state ON sorteos.boleto_digital(random_state);