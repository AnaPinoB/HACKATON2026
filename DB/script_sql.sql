CREATE DATABASE IF NOT EXISTS retocacao_db;
USE retocacao_db;

CREATE TABLE IF NOT EXISTS lote (
    codigo VARCHAR(20) PRIMARY KEY,  /* sera el identificador unico segun RF01 que esta en el reto*/
    productor VARCHAR(120) NOT NULL,
    origen VARCHAR(150) NOT NULL,
    fecha DATE NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    unidad VARCHAR(10) NOT NULL DEFAULT 'kg',
    estado_lote VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo_estado VARCHAR(255),
    fecha_creacion_registro_lote TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_lote_cantidad
        CHECK (cantidad > 0),
    CONSTRAINT chk_lote_estado
        CHECK (estado_lote IN ('PENDIENTE', 'LIBERADO', 'BLOQUEADO'))
);
