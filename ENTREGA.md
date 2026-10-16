# Entrega — THEOBRA

Fecha: 16 de octubre de 2026. Equipo: **CYAN**.

## Integrantes

- Nicol Sofia Covaleda Gutierrez
- Yurleidy Sanchez Canacue
- Ana Isabella Pino Bernal

## Enlaces y versión entregada

- [Repositorio](https://github.com/AnaPinoB/HACKATON2026).
- [Rama de entrega: main](https://github.com/AnaPinoB/HACKATON2026/tree/main).
- **Commit final de entrega:** identificado por la etiqueta Git `entrega-final-2026-10-16`, que apunta al commit que incorpora este archivo. [Versión fija de la entrega](https://github.com/AnaPinoB/HACKATON2026/tree/entrega-final-2026-10-16). Obtener su SHA completo con `git rev-list -n 1 entrega-final-2026-10-16` después de `git fetch --tags`.
- Commit del código validado antes de incorporar esta guía: [`d69890760b92569e4de369bff69450a94f248f20`](https://github.com/AnaPinoB/HACKATON2026/commit/d69890760b92569e4de369bff69450a94f248f20). La incorporación de esta guía no cambia el aplicativo.
- [Presentación del pitch](pitch/THEOBRA%20%281%29.pdf) y [declaración de asistencia de Claude](pitch/USO_DE_IA.txt).
- [README e instalación](README.md), [detalle de integración](documentos/INTEGRACION_CACAO_VIVO.md).
- Acceso local: <http://localhost:8085/>. **No se entrega una URL de despliegue público**; localhost funciona únicamente en el equipo que ejecuta el servidor.

## Instrucciones de instalación y acceso

1. Instalar JDK compatible con Java 21 y MySQL. Node.js es necesario para las pruebas JavaScript.
2. Clonar el repositorio y seleccionar `main` o la etiqueta de entrega.
3. En una base nueva, ejecutar desde Workbench, en orden: `DB/script_sql.sql`, `DB/tabla_registro_operaciones.sql`, `DB/tabla_de_auditoriacacao.sql`, `DB/tabla_usuario.sql` y `DB/tabla_comprador.sql`. La base se llama `retocacao_db`. Para una base existente seguir las migraciones del README; no recrear tablas ni recargar datos de demostración indiscriminadamente.
4. Crear localmente `src/main/resources/application-local.properties` con `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` y `spring.jpa.hibernate.ddl-auto=none`. Utilizar credenciales propias. El archivo está ignorado por Git y no forma parte de la entrega.
5. Descargar dependencias y compilar desde PowerShell en la raíz:

```powershell
.\mvnw.cmd -DskipTests compile
powershell -ExecutionPolicy Bypass -File .\scripts\CrearUsuariosLocal.ps1
```

El asistente pide nombre, correo y contraseña oculta para ADMINISTRADOR y OPERADOR; guarda hashes BCrypt. No cambia tablas ni reemplaza cuentas existentes. Si ya hay cuentas configuradas, usar sus credenciales sin repetir el alta. No hay contraseñas predeterminadas ni credenciales incluidas en este documento.

6. Iniciar el servidor:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8085"
```

7. Abrir <http://localhost:8085/>. La consulta pública de lotes no necesita sesión. Para funciones internas, pulsar **Entrar a plataforma** y usar el correo y contraseña creados localmente. El servidor determina el rol. ADMINISTRADOR gestiona estados, costos y auditoría; OPERADOR registra lotes y operaciones y consulta trazabilidad con restricciones de costos.

El registro público de compradores no concede roles internos ni implementa un portal autenticado de comprador. Para la ficha pública basta conocer el código de un lote existente.

## Recorrido de demostración

1. Registrar un lote nuevo con código único, productor, origen, fecha de hoy y 100 kg.
2. Registrar secado con entrada 100 kg, salida 80 kg y costo 200000 COP; indicar fecha, responsable y costos incluidos. Mantener la opción sin cambio de base de masa. Resultado esperado: merma 20 %, alerta por superar 15 % y costo unitario 2500 COP/kg.
3. Consultar trazabilidad y recargar: la operación debe persistir. Consultar el saldo antes de intentar nuevas operaciones; los 100 kg de entrada ya se consumieron del lote de origen.
4. Como ADMINISTRADOR, liberar el lote con motivo y consultar su código sin sesión. Bloquearlo y comprobar que su disponibilidad comercial cambia. OPERADOR no debe poder cambiar estados.
5. Cerrar sesión. Los datos privados deben desaparecer y la consulta pública debe continuar disponible.

## Lista de entregables E01–E09

La presencia de un archivo no certifica por sí sola todos sus criterios de evaluación. Se conservan los PDF proporcionados por el equipo sin alterar su contenido.

| Código | Entregable o ubicación | Estado |
| --- | --- | --- |
| E01 | [Problema](documentos/E01_problema.pdf) | Archivo incluido |
| E02 | [Requisitos](documentos/E02_requisitos.pdf) | Archivo incluido; matriz complementaria en esta guía |
| E03 | [Diseño](documentos/E03_diseno.pdf) | Archivo incluido |
| E04 | [Código del aplicativo](src/), [SQL](DB/), [README](README.md) | Implementación disponible; no existe un PDF E04 separado. Verificar esta correspondencia con la denominación oficial de E04 |
| E05 | [Pruebas](documentos/E05_pruebas.pdf), [evidencias](documentos/evidencias/E05/) | PDF y reportes incluidos |
| E06 | [Usabilidad](documentos/E06_usabilidad.pdf) | Archivo incluido |
| E07 | [Operación](documentos/E07_operacion.pdf) | Archivo incluido |
| E08 | [IA y fuentes](documentos/E08_IA_y_fuentes.pdf), [IA del pitch](pitch/USO_DE_IA.txt) | Archivos incluidos |
| E09 | `documentos/E09_piloto.pdf` | **Pendiente: no se encontró el archivo; no se acredita piloto de campo** |

## Matriz requisito → componente → prueba → resultado → evidencia

Los requisitos se identifican por su descripción funcional para no inventar numeración distinta de E02; RF06 es la consulta pública. Los resultados corresponden a las pruebas ejecutadas sobre el código indicado arriba. Las rutas de componentes Java parten de `src/main/java/com/cyan/retocacao/`.

| Requisito | Componente | Prueba | Resultado y alcance | Archivo de evidencia |
| --- | --- | --- | --- | --- |
| Registrar y consultar lotes válidos, con código único | `controller/LoteController.java`, `service/LoteService.java` | `LoteApiTests`: registro, duplicados, cantidad y fecha inválidas | Aprobado, 5 pruebas | [Reporte LoteApiTests](documentos/evidencias/E05/com.Cyan.retoCacao.LoteApiTests.txt) |
| Persistir el lote tras reiniciar la aplicación | Persistencia JPA y MySQL | `PersistenciaLoteTests.conservaLoteTrasCerrarYReabrirAplicacion` | Aprobado; no equivale a restaurar un respaldo | [Reporte PersistenciaLoteTests](documentos/evidencias/E05/com.Cyan.retoCacao.PersistenciaLoteTests.txt) |
| Registrar operaciones y evitar sobreconsumo y duplicados | `service/RegistroOperacionService.java`, repositorios | `RegistroOperacionTests`: sobreconsumo, reenvío, concurrencia y rollback | Aprobado dentro de la suite de 12 pruebas de operaciones | [Reporte RegistroOperacionTests](documentos/evidencias/E05/com.cyan.retocacao.RegistroOperacionTests.txt) |
| Calcular merma, costos y tratar balances no comparables | Servicios de operaciones, merma y costos | `ejemploBaseYReenvioHttp`, `salidaCeroNoDivide`, `salidaMayorRequiereJustificacion`, `alertaUsaPorcentajeSinRedondear` | Aprobado en pruebas de operaciones; datos sintéticos | [Reporte RegistroOperacionTests](documentos/evidencias/E05/com.cyan.retocacao.RegistroOperacionTests.txt) |
| Autenticar con BCrypt y restringir permisos y escrituras | `security/SeguridadConfig.java` | `SeguridadTests`: autenticación, cuenta inactiva, permisos y CSRF | Aprobado, 5 pruebas | [Reporte SeguridadTests](documentos/evidencias/E05/com.cyan.retocacao.SeguridadTests.txt) |
| Bloquear/liberar y auditar cambios y consumos | Servicios de lotes y auditoría | `EstadoAuditoriaTests`: autorización, motivo, historial y concurrencia | Aprobado, 8 pruebas | [Reporte EstadoAuditoriaTests](documentos/evidencias/E05/com.cyan.retocacao.EstadoAuditoriaTests.txt) |
| RF06: ficha y trazabilidad públicas, sin costos administrativos | `controller/FichaPublicaController.java`, `service/FichaPublicaService.java`, `dto/FichaPublicaDTO.java` | `IntegracionWebTests.flujoRealConSesionRolesMySQLYFichaPublica` | Aprobado: productor/origen públicos, estados y disponibilidad; no expone costos, responsables ni motivos internos | [Reporte IntegracionWebTests](documentos/evidencias/E05/com.cyan.retocacao.IntegracionWebTests.txt) |
| Sesión real y registro de comprador sin privilegios internos | Seguridad, `controller/CompradorController.java`, frontend | `IntegracionWebTests`: credenciales incorrectas, CSRF, comprador y recursos públicos | Aprobado, 4 pruebas de integración en total | [Reporte IntegracionWebTests](documentos/evidencias/E05/com.cyan.retocacao.IntegracionWebTests.txt) |
| Formularios, sesión, consulta pública y simulación sin persistencia | `src/main/resources/static/js/app.js`, `api.js` | Suite Node con DOM simulado y API controlada | 16 aprobadas; no sustituye una prueba visual completa ni un piloto con usuarios | [Pruebas ejecutables](src/test/js/frontend.test.cjs), [registro del resultado](documentos/INTEGRACION_CACAO_VIVO.md) |
| Filtros y exportación CSV | Frontend de administración | Comprobación manual específica pendiente | **Parcialmente verificado**: implementado; sin evidencia automatizada específica de exportación en esta matriz | [Implementación](src/main/resources/static/js/app.js) |
| QR de ficha pública | Frontend de ficha pública | Enlace por código probado; QR escaneable no implementado | **Parcial**: enlace real, patrón QR ilustrativo | [Limitaciones documentadas](documentos/INTEGRACION_CACAO_VIVO.md) |
| Respaldo/restauración y piloto de campo | Operación del entorno | No hay prueba de restauración ni E09 disponibles | **Pendiente**, no se declara aprobado | [E07](documentos/E07_operacion.pdf); E09 ausente |

Resultado agregado de la última validación del código: **36 pruebas Java aprobadas, sin fallos ni errores, y 16 pruebas JavaScript aprobadas**. [Resumen de evidencias Java](documentos/evidencias/E05/resumen_pruebas.txt). Los reportes versionados son evidencias de ejecuciones previas; la validación final de `main` volvió a aprobar las suites. El log final local `target/verificacion-main.log` no se publica porque `target/` está ignorado.

Para reproducir, con MySQL configurado y preferiblemente una base de evaluación separada:

```powershell
.\mvnw.cmd -B test
node --test src/test/js/frontend.test.cjs
```

Las pruebas Java usan MySQL y crean datos temporales con limpieza; las pruebas JavaScript usan un DOM simulado. No son pruebas de carga ni de producción.

## Funciones parciales y pendientes de entrega

- E09 y la validación de un piloto real están pendientes; los datos de demostración son sintéticos.
- QR ilustrativo: se entrega un enlace público funcional, no un QR escaneable.
- Reintentos de operación conservan su identificador mientras la página permanece abierta; después de recargar ante un resultado incierto, consultar trazabilidad antes de repetir.
- El simulador es una proyección local, no registra operaciones ni altera inventario.
- CSV generado en el navegador; no hay endpoint específico de exportación ni prueba automatizada específica acreditada aquí.
- El comprador puede registrarse y consultar públicamente; no se entrega una sesión privada de comprador.
- No se acredita restauración de respaldos, carga, despliegue público ni endurecimiento productivo con HTTPS.
- Seguridad previa a publicación pública: un respaldo con una credencial local estuvo en el historial de la rama `integración`. Se excluyó de la entrega en `main`, conservándolo localmente. **Queda pendiente rotar esa credencial y tratar su exposición en el historial remoto**; esta guía no declara ese punto resuelto.
