-- V11__agregar_porcentaje_minimo_y_fecha_limite_rifas.sql
-- Adición del umbral mínimo de ventas requerido para el sorteo

ALTER TABLE rifas 
ADD COLUMN porcentaje_minimo_ventas NUMERIC(5,2) NOT NULL DEFAULT 70.00;