-- 1. Agregar la columna administrador_id permitiendo nulos temporalmente
ALTER TABLE rifas ADD COLUMN administrador_id BIGINT;

-- 2. Migrar datos históricos: asignar las rifas existentes al Administrador General (ej. ID 1)
UPDATE rifas SET administrador_id = 1 WHERE administrador_id IS NULL;

-- 3. Crear la llave foránea que vincula la rifa con el usuario administrador de la tabla usuario
ALTER TABLE rifas 
ADD CONSTRAINT fk_rifa_administrador 
FOREIGN KEY (administrador_id) REFERENCES usuario(id);