# Aplicativo web "Cacao con historia, calidad y valor"

Para cumplir con los parámetros y requisitos del reto se desarrola un MVP de software para apoyar la trazabilidad de lotes de cacao de una asociación ficticia del Huila. El proyecto se desarrolla dentro del tiempo acordado para el desarrollo del reto, como parte de la II Hackathon de Programación de la Universidad Surcolombiana.

## Descripción general del aplicativo

Su finalidad principal es registrar lotes de cacao, documentar sus operaciones de
transformación y consultar el recorrido de cada lote. También se está
construyendo para apoyar el control de cantidades, estados, costos alertas y demas párametros
definidos en el reto.
Se aclara que el caso y los datos incluidos son simulados. No representan productores ni
resultados de campo reales.

## Estado del proyecto

Proyecto en desarrollo durante la hackathon.

Funciones implementadas y verificadas: [completar después de probarlas].

Funciones parciales o pendientes: [indicar con honestidad].

## Tecnologías implementadas para el desarrollo

- Java: ---.
- Spring Boot: ---.
- MySQL: ----.
- MySQL Workbench: utilizado para crear y revisar la base de datos.
- Agregar demas.

## Requisitos previos

-----



## Base de datos

La base de datos se llama `retocacao_db`.

El esquema contiene las tablas:

- `lote`: Registra la recepción y el estado de los lotes.
- `registro_operaciones`: Registra las operaciones asociadas a un lote.
- `auditoria_cacao`: Permite conservar registros de cambios realizados.

La estructura y los datos de demostración deben cargarse usando los archivos
SQL incluidos en `[ruta de los scripts por confirmar]`.

Datos sintéticos de demostración según el reto:

- CAC001: Productor A, Rivera, 100 kg.
- CAC002: Productor B, Campoalegre, 150 kg.
- CAC003: Productor C, Algeciras, 80 kg.

Estos se usan únicamente para las pruebas y la demostración del reto.

## Configuración

La aplicación debe conectarse a MySQL. La URL, el usuario y la contraseña se
configurarán en el entorno local de cada integrante.

Variables de configuración previstas:

- `DB_URL`: dirección de conexión a la base de datos.
- `DB_USERNAME`: usuario de MySQL.
- `DB_PASSWORD`: contraseña local de MySQL.

No se deben subir contraseñas, claves ni archivos con credenciales al
repositorio. El equipo debe confirmar los nombres de variables que realmente
usa Spring Boot antes de completar esta sección.

## Instalación y ejecución

Pasos pendientes de verificar con el proyecto Spring Boot:

1. Clonar el repositorio.
2. Crear o cargar la base de datos `retocacao_db` con los scripts SQL.
3. Configurar las variables de conexión a MySQL.
4. Instalar o verificar las dependencias del proyecto.
5. Ejecutar la aplicación.

Comandos de instalación y ejecución: [agregar cuando el equipo los haya
probado].

## Pruebas

Las pruebas automatizadas se encuentran en `[ruta por confirmar]`.

Comando para ejecutarlas: [agregar cuando esté confirmado y probado].

Los casos manuales, sus resultados y evidencias se documentarán en
`documentos/E05_pruebas.pdf`.

## Usuarios de demostración

Usuarios y roles de prueba: [completar cuando estén creados y verificados].

No incluir cuentas personales ni contraseñas reales.

## Respaldo y recuperación

Archivo de respaldo: [ruta por confirmar].

Pasos para restaurarlo: [documentar después de probar la recuperación].

Después de restaurar, verificar que se recuperen los identificadores y las
cantidades de los lotes de demostración.

## Limitaciones conocidas

[Registrar aquí únicamente limitaciones que el equipo haya comprobado.]

## Documentación adicional

Los entregables y evidencias del reto se encuentran en:

- `ENTREGA.md`: índice de entrega, enlaces y versión final.
- `documentos/`: requisitos, diseño, pruebas, usabilidad, operación y declaración
  de herramientas y componentes.
- `datos/`: datos sintéticos y respaldo, cuando estén disponibles.
- `pitch/`: presentación comercial, cuando esté disponible.