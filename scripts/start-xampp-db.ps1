param([int]$DatabasePort=3309)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$mysqlRoot='C:\xampp\mysql'
$dataPath=Join-Path $mysqlRoot 'data'
$serverPath=Join-Path $mysqlRoot 'bin\mysqld.exe'
$clientPath=Join-Path $mysqlRoot 'bin\mysql.exe'
$configPath=Join-Path $mysqlRoot 'bin\my.ini'
foreach($required in @($serverPath,$clientPath,$configPath,$dataPath)) {
    if(-not(Test-Path -LiteralPath $required)){throw "Missing XAMPP path: $required. Set DB_URL explicitly for your database."}
}
$dataPath=(Resolve-Path -LiteralPath $dataPath).Path
if(-not(Test-Path -LiteralPath (Join-Path $dataPath 'luxury_hotel'))){
    throw 'Existing luxury_hotel data was not found in C:\xampp\mysql\data. No new database was created.'
}
function Test-HotelPort([int]$Number) {
    $client=New-Object System.Net.Sockets.TcpClient
    try { $connection=$client.ConnectAsync('127.0.0.1',$Number); return ($connection.Wait(500) -and $client.Connected) }
    catch { return $false }
    finally { $client.Dispose() }
}
if(-not(Test-HotelPort $DatabasePort)) {
    $localPath=Join-Path $projectRoot '.local'
    New-Item -ItemType Directory -Force -Path $localPath | Out-Null
    # Use the existing XAMPP data; do not initialize, copy, reset, or edit my.ini.
    # Disable scheduled events when starting this local development instance.
    $arguments=@(('"--defaults-file='+$configPath+'"'),('--port='+$DatabasePort),'--bind-address=127.0.0.1','--event-scheduler=OFF','--console')
    Write-Host "Starting XAMPP MariaDB with existing data on port $DatabasePort..."
    $server=Start-Process -FilePath $serverPath -ArgumentList $arguments -WorkingDirectory $mysqlRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $localPath 'xampp-stdout.log') -RedirectStandardError (Join-Path $localPath 'xampp-stderr.log')
    for($attempt=0;$attempt -lt 40;$attempt++) {
        if(Test-HotelPort $DatabasePort){break}
        $server.Refresh()
        if($server.HasExited){throw 'XAMPP MariaDB could not start. See .local/xampp-stderr.log. No other server was stopped.'}
        Start-Sleep -Milliseconds 250
    }
    if(-not(Test-HotelPort $DatabasePort)){throw 'XAMPP MariaDB is not ready. See .local/xampp-stderr.log.'}
}
# Verify identity before permitting Spring Boot/Flyway to access this port.
$dbUser=$env:DB_USERNAME
if(-not $dbUser){$dbUser='root'}
$previousMysqlPassword=$env:MYSQL_PWD
try {
    $env:MYSQL_PWD=$env:DB_PASSWORD
    $identity=& $clientPath --protocol=TCP --host=127.0.0.1 "--port=$DatabasePort" "--user=$dbUser" --connect-timeout=5 --batch --skip-column-names -e 'SELECT @@datadir;'
    if($LASTEXITCODE -ne 0){throw 'Database login failed. Set DB_USERNAME and DB_PASSWORD; do not reset the database password.'}
    $actual=([string]($identity | Select-Object -First 1)).Trim().Replace('\\','\').Replace('/','\').TrimEnd('\')
    if(-not $actual.Equals($dataPath.TrimEnd('\'),[StringComparison]::OrdinalIgnoreCase)){
        throw "Port $DatabasePort belongs to a different database directory. Refusing to use it."
    }
    $schema=& $clientPath --protocol=TCP --host=127.0.0.1 "--port=$DatabasePort" "--user=$dbUser" --connect-timeout=5 --batch --skip-column-names -e "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='luxury_hotel';"
    if($LASTEXITCODE -ne 0 -or $schema -notcontains 'luxury_hotel'){throw 'The existing luxury_hotel database is not accessible.'}
} finally { $env:MYSQL_PWD=$previousMysqlPassword }
$env:DB_URL="jdbc:mariadb://127.0.0.1:$DatabasePort/luxury_hotel"
Write-Host "Verified existing Luxury Hotel database on port $DatabasePort."
