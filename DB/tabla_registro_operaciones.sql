USE retocacao_db;

CREATE TABLE IF NOT EXISTS registro_operaciones (
    id_registro BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_operacion VARCHAR(36) NOT NULL UNIQUE,
    lote_codigo VARCHAR(20) NOT NULL,
    etapa VARCHAR(100) NOT NULL,
    responsable VARCHAR(120) NOT NULL,
    fecha_operacion DATE NOT NULL,
    cantidad_entrada_kg DECIMAL(10,2) NOT NULL,
    cantidad_salida_kg DECIMAL(10,2) NOT NULL,
    observacion TEXT,
    costo_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    precio_venta_kg DECIMAL(12,2),
    costos_incluidos VARCHAR(500),
    justificacion_balance VARCHAR(500),
    fecha_creacion_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_operacion_lote
        FOREIGN KEY (lote_codigo) REFERENCES lote(codigo),

    CONSTRAINT chk_operacion_entrada
        CHECK (cantidad_entrada_kg > 0),

    CONSTRAINT chk_operacion_salida
        CHECK (cantidad_salida_kg >= 0),

    CONSTRAINT chk_operacion_costo
        CHECK (costo_total >= 0),

    CONSTRAINT chk_operacion_precio
        CHECK (precio_venta_kg IS NULL OR precio_venta_kg >= 0)
);
