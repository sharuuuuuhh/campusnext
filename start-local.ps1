$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$mysqlHome = Join-Path $root 'work\mysql\mysql-8.4.11-winx64'
$mysqlData = Join-Path $root 'work\mysql-data'
$tomcatHome = Join-Path $root 'work\apache-tomcat-10.1.61'
$mysql = Join-Path $mysqlHome 'bin\mysql.exe'
$mysqld = Join-Path $mysqlHome 'bin\mysqld.exe'
$maven = Join-Path $root 'work\apache-maven-3.10.0\bin\mvn.cmd'

function Test-LocalPort([int]$port) {
    return [bool](Test-NetConnection 127.0.0.1 -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue)
}

if (-not (Test-LocalPort 3306)) {
    $mysqlLog = Join-Path $root 'work\mysql.log'
    $mysqlError = Join-Path $root 'work\mysql-error.log'
    Start-Process -FilePath $mysqld -ArgumentList @("--basedir=$mysqlHome", "--datadir=$mysqlData", '--port=3306', '--bind-address=127.0.0.1', '--console') -RedirectStandardOutput $mysqlLog -RedirectStandardError $mysqlError -WindowStyle Hidden
    $deadline = (Get-Date).AddSeconds(30)
    while (-not (Test-LocalPort 3306) -and (Get-Date) -lt $deadline) { Start-Sleep -Milliseconds 500 }
    if (-not (Test-LocalPort 3306)) { throw "MySQL did not start. Check $mysqlError" }
}

$schemaExists = & $mysql --protocol=tcp -h 127.0.0.1 -uroot -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='campusnest'"
if ($LASTEXITCODE -ne 0) { throw 'Could not connect to the local MySQL server.' }
if ($schemaExists.Trim() -eq '0') {
    & $mysql --protocol=tcp -h 127.0.0.1 -uroot -e 'source database/schema.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the CampusNest database schema.' }
    & $mysql --protocol=tcp -h 127.0.0.1 -uroot -e 'source database/seed.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Could not load the simulated PG listings.' }
}

if (-not (Test-Path $maven)) { throw 'Local Maven is missing. Run mvn package after installing Maven.' }
& $maven -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'CampusNest did not build successfully.' }
Copy-Item -LiteralPath (Join-Path $root 'target\campusnest.war') -Destination (Join-Path $tomcatHome 'webapps\campusnest.war') -Force

if (-not (Test-LocalPort 8081)) {
    $tomcatLog = Join-Path $root 'work\tomcat-console.log'
    $tomcatError = Join-Path $root 'work\tomcat-error.log'
    Start-Process -FilePath (Join-Path $tomcatHome 'bin\catalina.bat') -ArgumentList 'run' -WorkingDirectory $tomcatHome -RedirectStandardOutput $tomcatLog -RedirectStandardError $tomcatError -WindowStyle Hidden
}

$healthUrl = 'http://127.0.0.1:8081/campusnest/api/health'
$deadline = (Get-Date).AddSeconds(45)
do {
    Start-Sleep -Milliseconds 750
    try { $health = Invoke-RestMethod $healthUrl -TimeoutSec 3 } catch { $health = $null }
} while ((-not $health -or -not $health.database) -and (Get-Date) -lt $deadline)
if (-not $health -or -not $health.database) { throw 'CampusNest is not database-ready. Check work\tomcat-console.log and the Tomcat logs folder.' }

Write-Host 'CampusNest is ready at http://127.0.0.1:8081/campusnest/'
Write-Host 'Admin login: admin@campusnest.local'
Write-Host "Admin password is saved locally in $root\work\admin-credentials.txt"
