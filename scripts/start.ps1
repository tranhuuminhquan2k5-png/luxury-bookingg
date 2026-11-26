param([switch]$Demo, [int]$Port=8080)
$ErrorActionPreference='Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not $env:JAVA_HOME -and (Test-Path '.tools\jdk21')) {
    $hotelJdk=Get-ChildItem '.tools\jdk21' -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
    if ($hotelJdk) { $env:JAVA_HOME=$hotelJdk.FullName }
}
if (-not $env:JAVA_HOME) { throw 'Cần JDK 21. Hãy thiết lập JAVA_HOME theo README.' }
if (-not $env:DB_USERNAME) { throw 'Thiết lập DB_USERNAME và DB_PASSWORD trước khi chạy. Xem README.' }
if (-not $env:DB_URL) {
    if ($Demo) { & (Join-Path $PSScriptRoot 'start-xampp-db.ps1') }
    else { throw 'Set DB_URL to the correct database server before starting.' }
}
# Windows can remove empty environment values; the explicit default is local-only and never sets a production password.
if ($null -eq $env:DB_PASSWORD) { $env:DB_PASSWORD='' }
if ($Demo) { $env:SPRING_PROFILES_ACTIVE='demo' }
$env:SERVER_PORT="$Port"
$env:MAVEN_USER_HOME=Join-Path (Get-Location) '.tools\maven-home'
& .\mvnw.cmd -ntp '-Dmaven.repo.local=.tools/m2' spring-boot:run
exit $LASTEXITCODE
