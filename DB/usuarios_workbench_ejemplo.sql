-- Copiar a una pestaña privada de Workbench; completar ahi, no en el repositorio.
-- Generar DOS hashes independientes con scripts/GenerarHashBCrypt.java.
-- Los marcadores NO son credenciales. La clausula WHERE impide insertar sin reemplazarlos.
USE retocacao_db;
START TRANSACTION;

INSERT INTO usuario (nombre, correo, contrasena, rol, activo)
SELECT 'NOMBRE_ADMIN', 'CORREO_ADMIN', 'HASH_BCRYPT_ADMIN', 'ADMINISTRADOR', TRUE
WHERE 'HASH_BCRYPT_ADMIN' REGEXP '^[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}$'
  AND 'CORREO_ADMIN' LIKE '%@%'
  AND NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'CORREO_ADMIN');

INSERT INTO usuario (nombre, correo, contrasena, rol, activo)
SELECT 'NOMBRE_OPERADOR', 'CORREO_OPERADOR', 'HASH_BCRYPT_OPERADOR', 'OPERADOR', TRUE
WHERE 'HASH_BCRYPT_OPERADOR' REGEXP '^[$]2[aby][$][0-9]{2}[$][./A-Za-z0-9]{53}$'
  AND 'CORREO_OPERADOR' LIKE '%@%'
  AND NOT EXISTS (SELECT 1 FROM usuario WHERE correo = 'CORREO_OPERADOR');

COMMIT;
SELECT id_usuario, nombre, correo, rol, activo FROM usuario;
