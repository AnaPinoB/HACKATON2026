-- Registro publico separado: nunca concede roles internos.
CREATE TABLE IF NOT EXISTS comprador (
    id_comprador BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(30) NOT NULL,
    tipo_comprador VARCHAR(40) NOT NULL,
    ciudad VARCHAR(150) NOT NULL,
    contrasena VARCHAR(255) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
