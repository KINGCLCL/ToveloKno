$ErrorActionPreference = "Stop"

$mysqlBase = Get-ChildItem "C:\Program Files\MySQL" -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName "bin\mysqld.exe") } |
    Sort-Object Name -Descending |
    Select-Object -First 1 -ExpandProperty FullName

if (!$mysqlBase) {
    throw "No MySQL Server installation was found under C:\Program Files\MySQL"
}

$mysqlBin = Join-Path $mysqlBase "bin"
$mysqlName = Split-Path $mysqlBase -Leaf
$configRoot = Join-Path "C:\ProgramData\MySQL" $mysqlName
$configFile = Join-Path $configRoot "my.ini"
$dataDir = Join-Path $configRoot "Data"

if (!(Test-Path (Join-Path $mysqlBin "mysqld.exe"))) {
    throw "MySQL Server was not found at $mysqlBase"
}

New-Item -ItemType Directory -Force -Path $configRoot | Out-Null

if (!(Test-Path $dataDir)) {
    New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
    & (Join-Path $mysqlBin "mysqld.exe") --initialize-insecure --basedir="$mysqlBase" --datadir="$dataDir"
}

if (!(Test-Path $configFile)) {
    @"
[mysqld]
basedir=$mysqlBase
datadir=$dataDir
port=3306
character-set-server=utf8mb4
sql_mode=STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION

[client]
port=3306
default-character-set=utf8mb4
"@ | Set-Content -Path $configFile -Encoding ASCII
}

$portInUse = Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "MySQL is already listening on port 3306."
    exit 0
}

Write-Host "Starting MySQL on port 3306. Keep this window open while developing."
& (Join-Path $mysqlBin "mysqld.exe") --defaults-file="$configFile" --console
