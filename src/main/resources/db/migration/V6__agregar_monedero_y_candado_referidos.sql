-- Agregar columnas para el monedero y reglas de candado del programa de referidos
ALTER TABLE usuarios ADD COLUMN saldo_monedero NUMERIC(10, 2) DEFAULT 0.00;
ALTER TABLE usuarios ADD COLUMN fecha_meta_completada TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE usuarios ADD COLUMN reclamo_bloqueado BOOLEAN DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN boletos_vendidos_tras_bloqueo INT DEFAULT 0;