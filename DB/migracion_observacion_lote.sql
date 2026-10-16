-- Ejecutar en retocacao_db despues de crear la tabla lote.
-- Agrega solo la columna nullable cuando aun no existe.
USE retocacao_db;

SET @observacion_existe = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lote'
      AND COLUMN_NAME = 'observacion'
);

SET @sql_migracion = IF(
    @observacion_existe = 0,
    'ALTER TABLE lote ADD COLUMN observacion VARCHAR(2000) NULL',
    'SELECT ''La columna lote.observacion ya existe'' AS resultado'
);

PREPARE migracion_observacion FROM @sql_migracion;
EXECUTE migracion_observacion;
DEALLOCATE PREPARE migracion_observacion;
