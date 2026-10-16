# THEOBRA

Aplicación web de trazabilidad para una asociación cacaotera ficticia del Huila. Los datos de demostración son sintéticos y no representan productores ni resultados reales.

El frontend integrado que sirve Spring Boot está en `src/main/resources/static/`. La copia `index.html` de la raíz es un prototipo estático anterior; no autentica usuarios ni contiene credenciales de acceso. Usa la aplicación integrada en `http://localhost:8085/`.

## Funcionalidades

- Autenticación local con correo, BCrypt, sesión HttpOnly y protección CSRF. El rol y los permisos se obtienen del servidor.
- Registro y consulta de lotes persistidos en MySQL, inventario con bloqueo transaccional, operaciones idempotentes, merma, costos, estados y auditoría.
- Trazabilidad autenticada y ficha pública RF06 que omite los datos privados y los costos internos.
- Registro público de compradores en tabla separada, sin asignar permisos internos.
- Simulador de merma, rendimiento, precio y rentabilidad en el frontend. Es una proyección local basada en el saldo disponible; no escribe en la API ni cambia el inventario.

## Tecnologías

Java 21, Spring Boot, Spring Security, Spring Data JPA, MySQL, HTML, CSS y JavaScript.

Consulta [la guía de integración y verificación](documentos/INTEGRACION_CACAO_VIVO.md) y las notas de avances [RF01/RF07](documentos/AVANCE_RF01_RF07.md) y [operaciones e inventario](documentos/AVANCE_OPERACIONES_INVENTARIO.md).

## Requisitos

- JDK 21 o compatible con Java 21.
- MySQL accesible desde la máquina local.
- Node.js para ejecutar las pruebas del frontend.

## Preparar MySQL

La aplicación espera una base llamada `retocacao_db`. En una instalación nueva, ejecutar una vez en MySQL Workbench, en este orden:

1. `DB/script_sql.sql`
2. `DB/tabla_registro_operaciones.sql`
3. `DB/tabla_de_auditoriacacao.sql`
4. `DB/tabla_usuario.sql`
5. `DB/tabla_comprador.sql`
6. Opcional para registros sintéticos: `DB/datos_demo_reto.sql`

Para una base ya creada, no vuelvas a cargar los datos demo a ciegas. Revisa sus columnas; aplica solo las migraciones aditivas que falten: `DB/creacion_motivo_estado.sql`, `DB/migracion_operaciones_balance.sql` y `DB/migracion_observacion_lote.sql`. Son seguras para volver a ejecutar respecto a esas columnas y no eliminan datos.

Configura `src/main/resources/application-local.properties` con `spring.datasource.url`, `spring.datasource.username` y `spring.datasource.password`. Ese archivo es local e ignorado por Git; no compartas ni publiques credenciales. El perfil `local` se activa desde `application.properties`.

El asistente `scripts/CrearUsuariosLocal.ps1` crea un usuario ADMINISTRADOR y uno OPERADOR de forma interactiva. Requiere una terminal PowerShell real y que la tabla `usuario` ya exista. Cada persona elige sus correos y contraseñas; no hay contraseñas demo por defecto.

## Ejecutar

Desde PowerShell, en la raíz del repositorio:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8085"
```

Abre `http://localhost:8085/`. Para iniciar sesión, usa el correo y contraseña que elegiste en el asistente; las cuentas no se crean automáticamente. Si el puerto 8085 está ocupado, detén primero la instancia existente o cambia el puerto en el argumento.

## Pruebas

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd test
node --test src/test/js/frontend.test.cjs
node --check src/main/resources/static/js/app.js
node --check src/main/resources/static/js/api.js
```
