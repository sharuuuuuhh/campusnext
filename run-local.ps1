<#
.SYNOPSIS
  Run CampusNest locally with LOCAL Database (No Supabase)
#>

$env:USE_LOCAL_DB = "true"
$env:CAMPUSNEST_DB_URL = ""
$env:CAMPUSNEST_DB_USER = ""
$env:CAMPUSNEST_DB_PASSWORD = ""

Write-Host "=============================================================="
Write-Host " Starting CampusNest on localhost:8081 with LOCAL Database"
Write-Host "=============================================================="

& "C:\Users\sharu\maven\bin\mvn.cmd" compile exec:java "-Dexec.mainClass=edu.coet.campusnest.LocalServer" "-DUSE_LOCAL_DB=true"
