$ErrorActionPreference = "Stop"

$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
$schema = Join-Path $PSScriptRoot "..\backend\src\main\resources\db\schema.sql"
$password = if ($env:TOVELOKNO_DB_PASSWORD) { $env:TOVELOKNO_DB_PASSWORD } else { "000000" }

if (!(Test-Path $mysql)) {
    throw "mysql.exe was not found. Install MySQL Server 8.4 first."
}

if (!(Test-Path $schema)) {
    throw "Schema file was not found at $schema"
}

$command = "`"$mysql`" -u root -p`"$password`" --protocol=tcp -P 3306 --default-character-set=utf8mb4 < `"$schema`""
cmd.exe /c $command
Write-Host "Database schema is ready."
