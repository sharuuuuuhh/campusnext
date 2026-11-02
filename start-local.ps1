$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path

$maven = 'C:\Users\sharu\maven\bin\mvn.cmd'
if (-not (Test-Path $maven)) {
    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCmd) { $maven = $mvnCmd.Source } else { $maven = 'mvn' }
}

function Test-LocalPort([int]$port) {
    return [bool](Test-NetConnection 127.0.0.1 -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue)
}

if (-not (Test-LocalPort 8081)) {
    Write-Host "Building and starting CampusNest pure Java server on localhost:8081..." -ForegroundColor Cyan
    & $maven compile exec:java
} else {
    Write-Host "CampusNest is already running on http://localhost:8081/campusnest/" -ForegroundColor Green
    Write-Host "Admin email:    admin@campusnest.local"
    Write-Host "Admin password: admin12345678"
    Start-Process "http://localhost:8081/campusnest/"
}
