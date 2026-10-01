-- Asignar el rol de SUPER_ADMIN al primer usuario registrado (id = 1)
UPDATE usuarios 
SET rol = 'ROLE_SUPER_ADMIN' 
WHERE id = 1;