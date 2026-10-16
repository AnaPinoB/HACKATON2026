-- Solo para instalaciones anteriores que todavia no tienen estas columnas.
USE retocacao_db;

SET @costos_existen = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'registro_operaciones'
      AND COLUMN_NAME = 'costos_incluidos'
);
SET @sql_costos = IF(
    @costos_existen = 0,
    'ALTER TABLE registro_operaciones ADD COLUMN costos_incluidos VARCHAR(500) NULL',
    'SELECT ''La columna registro_operaciones.costos_incluidos ya existe'' AS resultado'
);
PREPARE migracion_costos FROM @sql_costos;
EXECUTE migracion_costos;
DEALLOCATE PREPARE migracion_costos;

SET @justificacion_existe = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'registro_operaciones'
      AND COLUMN_NAME = 'justificacion_balance'
);
SET @sql_justificacion = IF(
    @justificacion_existe = 0,
    'ALTER TABLE registro_operaciones ADD COLUMN justificacion_balance VARCHAR(500) NULL',
    'SELECT ''La columna registro_operaciones.justificacion_balance ya existe'' AS resultado'
);
PREPARE migracion_justificacion FROM @sql_justificacion;
EXECUTE migracion_justificacion;
DEALLOCATE PREPARE migracion_justificacion;
