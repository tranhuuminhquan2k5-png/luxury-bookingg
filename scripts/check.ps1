$ErrorActionPreference='Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not $env:JAVA_HOME -and (Test-Path '.tools\jdk21')) {
    $hotelJdk=Get-ChildItem '.tools\jdk21' -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
    if ($hotelJdk) { $env:JAVA_HOME=$hotelJdk.FullName }
}
if (-not $env:JAVA_HOME) { throw 'Thiết lập JAVA_HOME tới JDK 21.' }
if (-not $env:TEST_DB_URL) { $env:TEST_DB_URL='jdbc:mariadb://127.0.0.1:3309/luxury_hotel_test' }
if ($env:TEST_DB_URL -notmatch '/luxury_hotel_test(?:\?|$)') { throw 'Chỉ chạy kiểm tra trên database luxury_hotel_test riêng.' }
$env:HOTEL_IT='true'
$env:MAVEN_USER_HOME=Join-Path (Get-Location) '.tools\maven-home'
& .\mvnw.cmd -B -ntp '-Dmaven.repo.local=.tools/m2' verify
exit $LASTEXITCODE
