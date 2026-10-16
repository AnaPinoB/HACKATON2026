# Avance: recepción y persistencia de lotes

Fecha: 15 de octubre de 2026 (America/Bogota).

## Alcance realizado

Se corrigió Maven en Windows y se implementó el flujo de API para registrar y consultar lotes en MySQL. Corresponde a la base de RF01 y RF07; no acredita por sí solo el cumplimiento integral del reto.

| Archivo | Cambio y propósito |
|---|---|
| `mvnw.cmd` | Comprobar si `Target` existe antes de acceder a su primer elemento. Evita el fallo con una carpeta `.m2` normal en PowerShell. |
| `LoteController.java` | Inyección de `LoteService`; GET devuelve los lotes reales y POST registra con respuesta HTTP 201. |
| `LoteService.java` | Rechazar lote nulo y cantidades fuera de `DECIMAL(10,2)`. Mantener las validaciones existentes y el estado inicial PENDIENTE. |
| `LoteRegistroRepository.java` | Insertar mediante `persist`, ejecutar `flush` y recuperar la fecha generada por MySQL. La transacción revierte ante fallos. No usar `merge` para registrar, evitando actualizar accidentalmente un código existente en una carrera concurrente. |
| `ApiExceptionHandler.java` | Respuestas JSON con `status` y `detail` para validación, JSON inválido y conflictos de integridad, sin devolver mensajes SQL internos. |
| `LoteApiTests.java` | Cinco pruebas de comportamiento HTTP con MockMvc y MySQL real, revertidas mediante transacciones de prueba. |
| `PersistenciaLoteTests.java` | Prueba que cierra y vuelve a abrir el contexto completo de la aplicación y comprueba que el lote persiste en MySQL. |

No se modificó `application-local.properties`, no se cambiaron contraseñas ni se ejecutaron migraciones SQL. Los cambios preexistentes en `.gitignore` y `application.properties` se conservaron.

## Preparación y ejecución

Requisitos: JDK compatible con Java 21 o posterior y MySQL activo. La verificación de este avance se realizó con JDK 27, Maven Wrapper 3.10.0, Spring Boot 4.1.1 y MySQL 8.0.45. El compilador genera código con `release 21`.

En una instalación nueva, ejecutar `DB/script_sql.sql` desde MySQL Workbench para crear la base y la tabla `lote`. No es necesario recrearlas si ya existen. El perfil local usa `ddl-auto=none`: la aplicación no crea ni modifica las tablas.

Crear localmente `src/main/resources/application-local.properties` con los valores propios, sin publicarlo:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/retocacao_db
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_CLAVE_LOCAL
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
```

`application.properties` activa el perfil `local`. Desde la raíz del proyecto:

```powershell
.\mvnw.cmd -B test
.\mvnw.cmd spring-boot:run
```

La primera ejecución puede descargar dependencias. Las pruebas actuales necesitan la base MySQL configurada y la tabla `lote`; no utilizan una base en memoria.

## API implementada

| Método y ruta | Respuesta |
|---|---|
| `GET /api/lotes` | HTTP 200 y arreglo JSON de lotes; `[]` si no hay registros. |
| `POST /api/lotes` | HTTP 201 y lote guardado, incluido el estado PENDIENTE y la fecha de creación. |

Ejemplo reproducible en otra terminal PowerShell, con la aplicación iniciada:

```powershell
$codigoPrueba = 'MAN-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$datosLote = @{
    codigo = $codigoPrueba
    productor = 'Productor de prueba'
    origen = 'Rivera'
    fecha = '2026-10-15'
    cantidad = 100.00
    unidad = 'kg'
} | ConvertTo-Json

Invoke-RestMethod -Uri 'http://localhost:8080/api/lotes' -Method Post -ContentType 'application/json' -Body $datosLote
Invoke-RestMethod -Uri 'http://localhost:8080/api/lotes' -Method Get
```

Este ejemplo manual crea un registro persistente. Para comprobar un reinicio de proceso, detener con Ctrl+C, ejecutar otra vez `spring-boot:run` y consultar GET buscando el mismo código. Esta comprobación manual se proporciona como procedimiento; la verificación automatizada realizada reinicia contextos dentro de la misma JVM.

Reglas aplicadas:

- Código obligatorio, máximo 20 caracteres y único; duplicado devuelve 409.
- Productor y origen obligatorios, con máximos de 120 y 150 caracteres.
- Fecha obligatoria con formato ISO `AAAA-MM-DD`.
- Cantidad mayor que cero, máximo `99999999.99` y hasta dos decimales significativos. `100.000` se acepta porque equivale exactamente a `100`.
- Unidad no vacía, máximo 10 caracteres; si se omite, el modelo usa `kg`.
- El cliente no puede registrar directamente un lote liberado: el servidor fija `PENDIENTE`.
- Datos inválidos devuelven 400 con explicación en `detail`.

## Resultados realmente obtenidos

Comando: `.\mvnw.cmd -B test`.

| Prueba | Resultado |
|---|---|
| Registrar y listar desde MySQL; validar estado inicial y fecha generada | Pasó |
| Rechazar código duplicado sin cambiar el productor original | Pasó |
| Rechazar cantidades -1, 0, 0.001, 100000000 y null sin guardar | Pasó |
| Rechazar productor vacío sin guardar | Pasó |
| Rechazar fecha inválida y JSON malformado | Pasó |
| Cerrar aplicación, abrir otro contexto y recuperar cantidad, productor y estado | Pasó |
| Prueba preexistente `contextLoads` | Pasó |

Resultado total: **7 pruebas, 0 fallos, 0 errores, 0 omitidas**. Seis pruebas nuevas de comportamiento, además de la prueba de arranque existente. Los reportes generados están en `target/surefire-reports/` y no se versionan.

Datos utilizados: códigos aleatorios con prefijo `TST-` para pruebas HTTP y `RST-` para reinicio. Las primeras revierten sus inserciones; la prueba de reinicio guarda 12.50 kg de un productor sintético de Rivera y elimina únicamente su propio registro en un bloque `finally` después de la consulta. Si el proceso se interrumpe o la segunda aplicación no logra arrancar, ese registro de prueba podría quedar y debe revisarse por su código. No se modificaron CAC001, CAC002 ni CAC003.

Durante la verificación se descargaron dependencias faltantes de Surefire. No fue necesario cambiar versiones del proyecto. Se observaron advertencias de Mockito sobre la carga dinámica de agentes y de JPA sobre `open-in-view`; no impidieron completar las pruebas.

## Límites y siguiente paso

Este avance entrega una API; no incluye formulario ni interfaz gráfica. Tampoco implementa autenticación, roles, transformación, inventario, auditoría, ficha pública, CSV ni respaldo/restauración. GET es actualmente una consulta interna sin protección y no debe presentarse como la ficha pública de RF06. El reinicio probado no sustituye la prueba de restauración de un respaldo.

No se ha ejecutado una prueba de concurrencia ni de rendimiento sobre 100 registros. Las pruebas documentadas no sustituyen los 15 casos del flujo completo exigidos por el reto.

El siguiente paso es añadir una interfaz de recepción y consulta, y usuarios/permisos antes de exponer datos internos. Después se integra transformación con inventario.

## Registro de apoyo de IA

Herramienta: Codex. Aporte: corrección del wrapper, endpoints, validaciones, inserción transaccional, manejo de errores, pruebas y este documento. Validación automática: compilación y suite descrita. Revisión humana del equipo: pendiente; comprobar comprensión del flujo y registrar cualquier corrección en E08. Este registro es un insumo para E08, no una declaración completa de bibliotecas y licencias.
