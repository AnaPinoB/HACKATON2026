USE retocacao_db;

CREATE TABLE IF NOT EXISTS auditoria_cacao (
    id_auditoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_encargado VARCHAR(150) NOT NULL,
    accion VARCHAR(40) NOT NULL,
    tipo_registro VARCHAR(40) NOT NULL,
    registro_id VARCHAR(36) NOT NULL,
    campo_modificado VARCHAR(80) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo TEXT,
    motivo TEXT,
    fecha_cambio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
