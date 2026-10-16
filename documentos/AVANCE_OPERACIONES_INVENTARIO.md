# Avance: operaciones, inventario y trazabilidad

Fecha: 15 de octubre de 2026.

## Alcance implementado

Registro de transformaciones asociadas a un lote, consumo de materia prima con control de concurrencia, reenvíos idempotentes y consulta interna del recorrido con merma y costos. Complementa RF02, RF03, RF04, RF05, RF08 y RF12. No declara completo el reto ni sustituye la revisión humana del equipo.

Se agregaron `RegistroOperacion`, `RegistroOperacionRepository`, `RegistroOperacionService`, `RegistroOperacionController` y los DTO `OperacionSolicitud`, `OperacionRespuesta` y `TrazabilidadLote`. Se amplió `LoteRepository` con bloqueo pesimista y `ApiExceptionHandler` con validación de campos y conflictos de bloqueo. Los paquetes son `com.cyan.retocacao`.

## Modelo de inventario y transacción

- `lote.cantidad` conserva la cantidad recibida original.
- Consumido = suma de entradas de las operaciones confirmadas del lote.
- Disponible = recibido - consumido.
- Salida producida = suma de salidas de esas operaciones. No se devuelve automáticamente al inventario de materia prima.
- Cada operación representa consumo directo de materia prima del lote y su transformación en salida vendible. Este avance NO permite encadenar una salida como entrada de otra etapa ni mezclar lotes. No registrar etapas consecutivas del mismo material como nuevos consumos del lote original.

El servicio registra dentro de una transacción `READ_COMMITTED`, bloquea primero la fila del lote con `PESSIMISTIC_WRITE`, comprueba el código de operación, valida existencias y guarda. La segunda solicitud sobre ese lote espera y después ve los consumos confirmados por la primera. Una excepción revierte la inserción; el disponible no se mantiene en una columna independiente que pueda quedar desincronizada.

La consulta de trazabilidad también bloquea brevemente el lote para obtener un balance coherente con sus operaciones. La garantía aplica a escrituras a través de este servicio; modificaciones SQL externas no adquieren necesariamente el mismo bloqueo. Futuros endpoints que modifiquen cantidades deben respetarlo.

## Idempotencia y errores

| Situación | HTTP y comportamiento |
|---|---|
| Operación nueva válida | 201, operación y cálculos |
| Mismo código, mismo lote y mismos datos | 200, operación existente; sin nuevo consumo |
| Mismo código con datos diferentes | 409, sin modificar el registro anterior |
| Inventario insuficiente o lote bloqueado | 409 |
| Lote inexistente | 404 |
| Campos inválidos, unidad distinta de kg o fecha anterior a recepción | 400 |
| Salida mayor que entrada sin justificación | 400 |
| Conflicto de integridad o bloqueo | 409 con mensaje; reintentar conservando el código |

Se recortan espacios externos y se equiparan escalas decimales (`100` y `100.00`) al comparar reintentos. Los textos deben conservar el mismo contenido y uso de mayúsculas. El código de operación es globalmente único gracias a la restricción SQL existente. Para un reenvío se debe conservar también el lote original.

Los números se validan antes de escribir: entradas positivas, salidas y costo no negativos, precio opcional estrictamente positivo, cantidades con ocho enteros y dos decimales como máximo, importes con diez enteros y dos decimales. Se exigen etapa, responsable, fecha y descripción de costos incluidos.

## Cálculos

- Merma: `(entrada - salida) / entrada * 100`, para el mismo material y base de masa sin insumos agregados.
- Alerta: pérdida estrictamente mayor al 15 %, comparada antes de redondear. El porcentaje presentado tiene dos decimales; puede mostrar 15.00 % y activar alerta cuando el valor real es ligeramente mayor.
- Costo unitario: costo total de la operación en COP / kg de salida vendible.
- Margen sobre venta: `(precio - costo unitario) / precio * 100`. Se utiliza el costo unitario redondeado a dos decimales.
- Salida cero: costo unitario y margen devuelven `null`, con explicación; no se divide por cero.
- Precio omitido: margen `null`.
- `costosIncluidos`: descripción de qué compone el costo total declarado. No se infieren ni acumulan costos de otras etapas.
- `justificacionBalance`: usar cuando las masas no sean comparables, por ejemplo por insumos añadidos o cambio de base. Si se informa, merma devuelve `null` y no se genera una alerta automática de merma; se devuelve la justificación. Es obligatoria si salida > entrada. No constituye un modelo detallado de insumos.

Ejemplo base: 100 kg de entrada, 80 kg de salida, 1.600.000 COP de costo y 25.000 COP/kg de precio → merma 20 %, alerta activa, costo unitario 20.000 COP/kg y margen 20 %. El lote queda con 0 kg de materia prima disponibles y 80 kg producidos.

## Preparación de MySQL

Se actualizó `DB/tabla_registro_operaciones.sql` para instalaciones nuevas. Se añadieron dos columnas opcionales al esquema existente: `costos_incluidos VARCHAR(500)` y `justificacion_balance VARCHAR(500)`. Son opcionales en SQL para conservar registros históricos; la API exige costos incluidos en nuevas operaciones. No se inventaron descripciones para registros anteriores.

En este entorno se ejecutó correctamente `scripts/PrepararOperaciones.java`, que creó la tabla si faltaba y agregó solo las columnas ausentes. No se borraron ni reemplazaron registros existentes. La aplicación mantiene `ddl-auto=none`.

Para otro equipo con la base y la tabla lote preparadas, desde la raíz y después de resolver las dependencias con Maven:

```powershell
$driverMysql = Get-ChildItem "$HOME/.m2/repository/com/mysql/mysql-connector-j" -Recurse -Filter '*.jar' | Select-Object -First 1 -ExpandProperty FullName
java --class-path $driverMysql scripts/PrepararOperaciones.java
.\mvnw.cmd -B test
.\mvnw.cmd spring-boot:run
```

El preparador lee los valores literales de conexión de `application-local.properties`, no los imprime ni interpreta placeholders de variables de entorno. Si se usa otra configuración, ejecutar los scripts con la conexión apropiada en MySQL Workbench. `DB/migracion_operaciones_balance.sql` es una alternativa manual **solo cuando faltan ambas columnas**; no repetirla si ya existen. El preparador Java sí comprueba cada columna antes de añadirla.

## Endpoints internos

| Método | Ruta | Resultado |
|---|---|---|
| POST | `/api/operaciones` | Registrar o recuperar un reenvío |
| GET | `/api/operaciones` | Todas las operaciones con cálculos |
| GET | `/api/lotes/{codigo}/operaciones` | Operaciones del lote, ordenadas por fecha e ID |
| GET | `/api/lotes/{codigo}/trazabilidad` | Lote, recibido, consumido, disponible, salida producida y operaciones |

Se mantienen POST y GET `/api/lotes`.

Ejemplo PowerShell con un lote nuevo para no alterar CAC001:

```powershell
$base = 'http://localhost:8080/api'
$codigoLote = 'MAN-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$lote = @{
    codigo = $codigoLote; productor = 'Productor sintetico'; origen = 'Rivera'
    fecha = '2026-10-15'; cantidad = 100; unidad = 'kg'
} | ConvertTo-Json
Invoke-RestMethod "$base/lotes" -Method Post -ContentType 'application/json' -Body $lote

$operacion = @{
    codigoOperacion = [Guid]::NewGuid().ToString()
    loteCodigo = $codigoLote
    etapa = 'Transformacion final'
    responsable = 'Operador sintetico'
    fechaOperacion = '2026-10-15'
    cantidadEntradaKg = 100
    cantidadSalidaKg = 80
    observacion = 'Ejemplo del reto sin ingredientes agregados'
    costoTotal = 1600000
    precioVentaKg = 25000
    costosIncluidos = 'Materia prima, mano de obra y energia'
} | ConvertTo-Json

Invoke-RestMethod "$base/operaciones" -Method Post -ContentType 'application/json' -Body $operacion
# Reenvio: usar exactamente el mismo cuerpo y codigo, no generar otro GUID.
Invoke-RestMethod "$base/operaciones" -Method Post -ContentType 'application/json' -Body $operacion
Invoke-RestMethod "$base/lotes/$codigoLote/trazabilidad"
```

Los datos originales de la operación aparecen dentro de `datos` en la respuesta; los cálculos, fórmulas, supuestos, ID y fecha de creación están en el nivel superior. El ejemplo manual conserva sus registros en la base.

## Pruebas y resultados

Verificación con MySQL local real, JDK 27 compilando para Java 21, Spring Boot 4.1.1 y Maven 3.10.0. Comando: `.\mvnw.cmd -B test`.

`RegistroOperacionTests` aporta estos doce casos automatizados:

1. POST del ejemplo base, GET de trazabilidad y operaciones, reenvío HTTP sin duplicar.
2. Rechazo de sobreconsumo sin guardar ni cambiar el disponible.
3. Rechazo de lote bloqueado, unidad incompatible y fecha anterior.
4. Código repetido con datos distintos produce conflicto y conserva la operación original.
5. Reintento con escalas decimales distintas devuelve el mismo ID.
6. Salida cero evita división y devuelve indicadores no calculables.
7. Salida mayor que entrada exige justificación y desactiva comparación automática de merma.
8. Merma real 15.001 % activa alerta aunque la presentación sea 15.00 %.
9. Datos inválidos HTTP no crean registros.
10. Fallo simulado posterior al registro revierte la transacción y conserva inventario.
11. Dos solicitudes simultáneas de 60 kg sobre 100 kg permiten solo una operación.
12. Dos reenvíos simultáneos consumen una sola vez.

Se amplió `PersistenciaLoteTests`: cierra un contexto completo de aplicación, abre otro en la misma JVM, recupera lote y operación, verifica 2.50 kg disponibles de 12.50 recibidos y comprueba idempotencia después del reinicio. No es un reinicio de máquina ni una restauración de respaldo.

Resultado final: **19 pruebas, 0 fallos, 0 errores y 0 omitidas**, incluyendo las pruebas anteriores. Evidencias regenerables: `target/surefire-reports/` y salida de Maven. No se debe presentar este número como cumplimiento de todos los casos del reto: faltan pruebas de seguridad, ficha pública, CSV y restauración.

Datos de pruebas: lotes aleatorios `OP-...` y `RST-...`, responsables sintéticos, operaciones con UUID. Se limpian primero las operaciones y después el lote creado por cada prueba. Las pruebas de lotes conservan su rollback. Una interrupción abrupta puede dejar registros sintéticos; no se eliminan otros registros ni se modifica CAC001/CAC002/CAC003.

## Pendientes

- Seguridad y separación de permisos: el responsable recibido es texto declarado, todavía no identidad autenticada. Estos endpoints no son una ficha pública y exponen costos internos.
- Cambio autorizado de estados, motivo y auditoría. Se respeta un lote ya bloqueado, pero no se implementó el endpoint de bloqueo.
- Interfaz de usuario, ficha comercial pública, filtros y CSV administrativo.
- Cadena de múltiples etapas/productos y mezcla de lotes si se decide ampliar el alcance actual.
- Prueba de rendimiento con 100 registros, respaldo y restauración.
- Revisión humana de costos, supuestos y datos heredados antes de usarlos en una demostración.

## Aporte de IA

Codex generó los componentes, scripts, pruebas y documentación de este avance y ejecutó las verificaciones descritas. Revisión humana del equipo pendiente. Registrar este aporte y las correcciones posteriores en E08; no atribuirle validación con productores reales.
