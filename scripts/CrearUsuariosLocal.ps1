$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
$cryptoRoot = Join-Path $env:USERPROFILE '.m2\repository\org\springframework\security\spring-security-crypto'
$mysqlRoot = Join-Path $env:USERPROFILE '.m2\repository\com\mysql\mysql-connector-j'
$crypto = Get-ChildItem -LiteralPath $cryptoRoot -Recurse -Filter '*.jar' | Where-Object Name -NotMatch '(sources|javadoc)' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$mysql = Get-ChildItem -LiteralPath $mysqlRoot -Recurse -Filter '*.jar' | Where-Object Name -NotMatch '(sources|javadoc)' | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (!$crypto -or !$mysql) { throw 'Faltan dependencias. Ejecuta primero .\mvnw.cmd -DskipTests compile.' }
Write-Host 'Asistente local: no muestra ni guarda contrasenas. No modifica tablas ni reemplaza cuentas existentes.'
& java --class-path ($crypto.FullName + ';' + $mysql.FullName) scripts/CrearUsuariosLocal.java
if ($LASTEXITCODE -ne 0) { throw 'El asistente no termino correctamente. Revisa el mensaje anterior.' }
