<#
.SYNOPSIS
  Run CampusNest locally connected to Supabase PostgreSQL Backend
#>

$supabaseHost = $env:SUPABASE_HOST
if (-not $supabaseHost) {
    Write-Host "Tip: You can set `$env:CAMPUSNEST_DB_URL, `$env:CAMPUSNEST_DB_USER, and `$env:CAMPUSNEST_DB_PASSWORD in your environment."
}

if (-not $env:CAMPUSNEST_DB_URL) {
    $env:CAMPUSNEST_DB_URL = Read-Host "Enter Supabase JDBC URL (e.g., jdbc:postgresql://db.xxxx.supabase.co:5432/postgres?sslmode=require&stringtype=unspecified)"
}
if (-not $env:CAMPUSNEST_DB_USER) {
    $env:CAMPUSNEST_DB_USER = Read-Host "Enter Database User (default: postgres)"
    if (-not $env:CAMPUSNEST_DB_USER) { $env:CAMPUSNEST_DB_USER = "postgres" }
}
if (-not $env:CAMPUSNEST_DB_PASSWORD) {
    $env:CAMPUSNEST_DB_PASSWORD = Read-Host "Enter Supabase Database Password"
}

Write-Host "Starting CampusNest with Supabase backend on http://localhost:8081/campusnest/ ..."
& "C:\Users\sharu\maven\bin\mvn.cmd" compile exec:java "-Dexec.mainClass=edu.coet.campusnest.LocalServer"
