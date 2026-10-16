# Integración THEOBRA — 16 de octubre de 2026

## Mejora del flujo de prueba

Se eliminaron los diálogos nativos `prompt` y `confirm`. Fecha, costos incluidos y cambio de base de masa se completan en el formulario de transformación, con justificación condicional. El detalle de lote permite seleccionar estado, escribir motivo y confirmar o cancelar dentro de la página. Se mantienen las clases visuales existentes y no se modifica CSS.

Las cantidades, etapa, costo y precio ya no se precargan con datos de ejemplo. El lote debe seleccionarse explícitamente. Cambiarlo conserva los datos escritos y muestra un aviso para revisarlos; el responsable se completa con el correo de la sesión y la fecha con hoy. Los errores y el resultado del guardado de operaciones permanecen visibles. La vista previa no presenta indicadores ficticios con campos vacíos ni calcula merma simple cuando hay cambio de base de masa.

Verificación de esta mejora: 14 pruebas JavaScript aprobadas, comprobación de sintaxis y de diferencias sin errores. Se actualizaron los recursos servidos en el puerto 8085; recargar con Ctrl+F5 antes de repetir la prueba. No se registraron nuevas operaciones reales durante estas comprobaciones. No se hicieron commits ni push. La prueba visual autenticada queda a cargo del usuario con su sesión privada.

## Acceso sin credenciales de demostración

Se retiraron las credenciales de ejemplo. Se mantienen visibles los perfiles Administrador y Operador en el formulario integrado, pero la selección es solo informativa: las credenciales se verifican en MySQL y los permisos efectivos siempre los determina el servidor. El prototipo de la raíz no simula el inicio de sesión y enlaza a la aplicación Spring Boot. No se modificó la base de datos. Para entrar, usa únicamente las credenciales que configuraste con el asistente de terminal.

## Actualización: asistente local autorizado

**Resultado confirmado:** el usuario completó el asistente con EXITO. Una consulta posterior de solo lectura (`scripts/VerificarUsuariosLocal.java`) encontró una cuenta ADMINISTRADOR y una OPERADOR, ambas activas y con formato BCrypt de costo 12, sin imprimir correos ni hashes. Se volvieron a ejecutar `IntegracionWebTests` y `SeguridadTests`: 9 pruebas aprobadas, cero errores y fallos (`target/verificacion-login.log`). Estas pruebas usan cuentas temporales propias; no verifican las contraseñas privadas del usuario mediante el navegador. El asistente sí comprobó ambas contraseñas contra sus hashes antes del commit. Queda la comprobación visual del usuario iniciando sesión.

Por decisión posterior del usuario se agregó `scripts/CrearUsuariosLocal.ps1` y `scripts/CrearUsuariosLocal.java`. La alternativa Workbench descrita abajo sigue disponible, pero ya no es la única opción.

Ejecutar desde una terminal interactiva en la raíz: `powershell -ExecutionPolicy Bypass -File .\scripts\CrearUsuariosLocal.ps1`.

El asistente pide nombre, correo y contraseña con confirmación para ADMINISTRADOR y OPERADOR. Las contraseñas se leen sin eco mediante `Console.readPassword`, se calculan hashes BCrypt con costo 12 y no se imprimen ni guardan en archivos. Usa la conexión local existente sin mostrar sus credenciales. No ejecuta DDL ni sobrescribe usuarios: si el correo existe, cancela. Ambas altas se confirman en una transacción después de leer y verificar sus hashes, roles y estados; un error previo al commit revierte las dos altas. El mensaje EXITO confirma el commit. Si se pierde la conexión al confirmar, comprobar los correos en Workbench antes de repetir.

El login real ya utiliza la tabla `usuario`; no necesita cambios adicionales para estas cuentas. Tras el mensaje de éxito, probar ambos correos en el formulario. La creación efectiva requiere completar los datos en la ventana local; preparar o abrir el asistente no significa que las cuentas ya existan. No compartir las contraseñas por chat.

Trabajo realizado en `integración`, sin commits, push ni cambios de rama. Se conservaron el diseño, los respaldos y los datos existentes. Esta actualización sustituye los datos simulados del frontend por solicitudes a la API existente.

## Funciones implementadas

- Login real con correo y contraseña: Spring Security verifica BCrypt contra `usuario`. Sesión con cookie HttpOnly, SameSite Lax, caducidad de 30 minutos, protección CSRF y cierre de sesión en servidor. El rol lo determina el servidor. No se guardan contraseñas en el navegador.
- Registro y consulta de lotes persistidos, incluidas observaciones de hasta 2000 caracteres.
- Registro de operaciones con inventario, merma, costos y validaciones del backend. Se conserva el código de operación durante reintentos del mismo formulario para evitar duplicados. Un rechazo de validación permite corregir los datos de los diálogos.
- Trazabilidad real para ambos roles. Las consultas y respuestas al registrar o reintentar operaciones como OPERADOR excluyen costos históricos; ADMINISTRADOR dispone de costos, auditoría y cambios de estado con motivo obligatorio.
- Ficha pública RF06 sin sesión mediante un DTO dedicado: muestra código, productor y origen guardados en MySQL, estado, disponibilidad y etapas registradas, sin exponer observaciones, responsables, costos ni datos administrativos. El comprador consulta por código o mediante enlaces `?lote=CODIGO`.
- Filtros y exportación CSV del frontend para administrador, con escape de fórmulas de hojas de cálculo.
- Registro público de compradores en tabla separada, contraseña BCrypt y sin otorgar acceso de operador o administrador. No existe un portal autenticado de comprador en este alcance.
- Simulador local de merma, rendimiento, precio y rentabilidad basado en el saldo disponible de un lote. Sus entradas son independientes del formulario de operación y no se envían a la API ni se guardan.

## Archivos modificados y nuevos

| Área | Archivos |
| --- | --- |
| Seguridad | `src/main/java/com/cyan/retocacao/security/SeguridadConfig.java`, `src/main/resources/application.properties` |
| Lotes y operaciones | `model/Lote.java`, `service/LoteService.java`, `service/RegistroOperacionService.java` bajo `src/main/java/com/cyan/retocacao/` |
| Compradores | `src/main/java/com/cyan/retocacao/controller/CompradorController.java` |
| Frontend | `src/main/resources/static/index.html`, `static/js/app.js`, `static/js/api.js` (los dos últimos bajo `src/main/resources/`) |
| Base de datos | `DB/script_sql.sql`, `DB/tabla_comprador.sql`, `DB/usuarios_workbench_ejemplo.sql`, `scripts/PrepararIntegracion.java` |
| Hashes | `scripts/GenerarHashBCrypt.java` |
| Pruebas | `src/test/java/com/cyan/retocacao/SeguridadTests.java`, `IntegracionWebTests.java` en esa misma carpeta, `src/test/js/frontend.test.cjs` |
| Documentación | Este documento y `README.md` |

Se aplicaron únicamente cambios aditivos a MySQL: columna nullable `lote.observacion` y tabla `comprador` si no existían. No se crearon cuentas internas permanentes. La última comprobación encontró cero usuarios internos. `application-local.properties` continúa ignorado por Git.

Para una base existente, ejecutar `DB/migracion_observacion_lote.sql` después de confirmar que existe `retocacao_db.lote` y antes de iniciar la aplicación. El script agrega la columna nullable solo si falta y conserva los datos; `DB/script_sql.sql` crea la columna únicamente en instalaciones nuevas porque `CREATE TABLE IF NOT EXISTS` no modifica tablas ya creadas.

## Crear ADMINISTRADOR y OPERADOR desde Workbench

El generador solo calcula hashes localmente: no conecta a MySQL ni crea usuarios. Abre una terminal PowerShell interactiva en la raíz del proyecto. Con las dependencias de Maven ya descargadas, ejecuta:

```powershell
$cryptoJar = "$env:USERPROFILE\.m2\repository\org\springframework\security\spring-security-crypto\7.1.1\spring-security-crypto-7.1.1.jar"
java --class-path "$cryptoJar" scripts/GenerarHashBCrypt.java
```

Introduce una contraseña propia dos veces cuando se solicite: no aparece en pantalla ni como argumento del comando. Usa al menos 12 caracteres y como máximo 72 bytes UTF-8. Copia el hash mostrado. Repite el procedimiento con una contraseña diferente para la segunda cuenta. No redirijas la salida ni escribas las contraseñas en comandos, archivos, chat o SQL. El costo BCrypt es 12.

1. Abre una pestaña de consulta local de Workbench y copia allí `DB/usuarios_workbench_ejemplo.sql`.
2. Reemplaza **todas** las apariciones de `NOMBRE_ADMIN`, `CORREO_ADMIN`, `HASH_BCRYPT_ADMIN` y los tres marcadores equivalentes de OPERADOR. Usa correos propios, preferiblemente en minúsculas. Escapa apóstrofos de los nombres como `''` si los contienen.
3. Revisa el esquema `retocacao_db` antes de ejecutar. El script corresponde a `usuario(id_usuario,nombre,correo,contrasena,rol,activo)`. Inserta solamente si el correo no existe y el hash tiene formato BCrypt; no actualiza ni elimina cuentas existentes.
4. Ejecuta el script y verifica la consulta final de usuarios y roles. Si no inserta filas, revisa los marcadores y si el correo ya existe. No cambies el hash a texto plano.
5. Cierra la pestaña sin guardar los valores en el repositorio. Trata también los hashes como información sensible. Limpia el portapapeles tras usarlos y evita sesiones compartidas o grabadas.

Además del asistente PowerShell descrito antes, la ruta manual desde Workbench sigue disponible como alternativa. Las contraseñas solamente se introducen en el asistente o generador local y luego en el formulario de acceso.

## Verificación realizada

- `./mvnw.cmd -B test`: **36 pruebas aprobadas**, cero fallos y errores; compilación correcta. Incluye pruebas contra MySQL con datos temporales de prueba y limpieza de sus fixtures, login y sesión reales en MockMvc, CSRF, permisos, inventario, reintentos, estados, auditoría y ficha pública.
- `node --test src/test/js/frontend.test.cjs`: **16 pruebas aprobadas**. Usa DOM simulado y API controlada; cubre sesión, cierre de sesión, ausencia de credenciales demo, rol del servidor, payloads, reintentos, trazabilidad pública, estado bloqueado, enlace `?lote=CODIGO`, error de lote inexistente, expiración, CSRF y simulación sin persistencia. No sustituye una prueba completa del navegador autenticado.
- `node --check` para `app.js` y `api.js`: sintaxis válida.
- Generador BCrypt compilado con la dependencia local de Spring Security.
- Navegador conectado a la aplicación en `http://localhost:8085`: consulta pública real de CAC001 verificada, sin solicitar sesión ni exponer datos privados.
- `git diff --check`: sin errores de espacios. No se modificó CSS.

Los logs locales de Maven están en `target/integracion-test.log`; `target` no se debe subir. Para iniciar la aplicación manualmente:

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8085"
```

Si el servidor de verificación sigue activo en ese puerto, utiliza el existente o detén esa instancia antes de iniciarlo de nuevo.

## Prueba de demostración después de crear las cuentas

1. Accede como OPERADOR, registra un lote en kg y registra una operación cuya entrada no supere el inventario. Consulta su trazabilidad y la merma calculada.
2. Cierra sesión y accede como ADMINISTRADOR. Revisa costos y auditoría, libera el lote con un motivo y consulta la ficha desde una ventana sin sesión.
3. Bloquea el lote y verifica que la ficha pública refleja que ya no está disponible. Comprueba que OPERADOR no puede cambiar estados.
4. Exporta el listado filtrado como administrador, cierra sesión y comprueba que desaparecen los datos internos.

## Pendientes y límites

- Crear las cuentas permanentes en Workbench y hacer la prueba visual privada con ellas. La revisión automática rechazó crear cuentas temporales para esa prueba visual; esas cuentas no se crearon. Las pruebas automatizadas de autenticación sí se ejecutaron.
- El patrón QR existente sigue siendo ilustrativo; el enlace público generado sí consulta datos reales. No se añadió un generador QR escaneable.
- El código de reintento se conserva en memoria mientras la página permanece abierta. Tras recargarla y ante un resultado incierto, consultar las operaciones antes de registrar otra vez.
- La exportación CSV se realiza desde el frontend para administrador; no se agregó un endpoint nuevo de exportación.
- No se realizaron pruebas de carga ni un despliegue productivo. Antes de publicar, configurar HTTPS y secretos del entorno; no distribuir archivos locales ni artefactos que contengan `application-local.properties`.
