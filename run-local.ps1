$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

$maven = 'C:\Users\sharu\maven\bin\mvn.cmd'
if (-not (Test-Path $maven)) {
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) { $maven = $mvnCmd.Source } else { $maven = 'mvn' }
}

Write-Host "Starting COET CampusNest on http://localhost:8081/campusnest/ ..." -ForegroundColor Cyan
& $maven compile exec:java -Dexec.mainClass="edu.coet.campusnest.LocalServer"
