SHOW COLUMNS FROM lote LIKE 'motivo_estado';

ALTER TABLE lote
ADD COLUMN motivo_estado VARCHAR(255) NULL;

SHOW COLUMNS FROM lote LIKE 'motivo_estado';