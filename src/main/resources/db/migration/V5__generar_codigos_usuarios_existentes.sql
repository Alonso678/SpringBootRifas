-- Genera un código único para usuarios que aún no lo tienen, usando su nombre e ID
-- El formato resultante será algo como: ALO-12345
UPDATE usuarios 
SET codigo_referido = UPPER(LEFT(nombre, 3)) || '-' || LPAD(CAST(id AS VARCHAR), 5, '0')
WHERE codigo_referido IS NULL;