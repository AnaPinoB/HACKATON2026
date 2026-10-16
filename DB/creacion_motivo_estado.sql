USE retocacao_db;

SET @motivo_existe = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lote'
      AND COLUMN_NAME = 'motivo_estado'
);

SET @sql_migracion = IF(
    @motivo_existe = 0,
    'ALTER TABLE lote ADD COLUMN motivo_estado VARCHAR(255) NULL',
    'SELECT ''La columna lote.motivo_estado ya existe'' AS resultado'
);

PREPARE migracion_motivo_estado FROM @sql_migracion;
EXECUTE migracion_motivo_estado;
DEALLOCATE PREPARE migracion_motivo_estado;