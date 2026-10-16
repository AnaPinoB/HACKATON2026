-- Solo para instalaciones anteriores que todavia no tienen estas columnas.
-- Consultar SHOW COLUMNS FROM registro_operaciones antes de ejecutar.
USE retocacao_db;
ALTER TABLE registro_operaciones
    ADD COLUMN costos_incluidos VARCHAR(500) NULL,
    ADD COLUMN justificacion_balance VARCHAR(500) NULL;
