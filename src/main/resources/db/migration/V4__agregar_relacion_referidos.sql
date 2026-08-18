ALTER TABLE usuarios ADD COLUMN referido_por_id BIGINT;
ALTER TABLE usuarios ADD CONSTRAINT fk_referido_por FOREIGN KEY (referido_por_id) REFERENCES usuarios(id);