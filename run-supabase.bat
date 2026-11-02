@echo off
setlocal
echo ==============================================================
echo  Connecting CampusNest to Supabase Backend
echo ==============================================================

:: If you haven't set these environment variables, edit them below:
if "%CAMPUSNEST_DB_URL%"=="" (
    set /p CAMPUSNEST_DB_URL="Enter Supabase JDBC URL: "
)
if "%CAMPUSNEST_DB_USER%"=="" (
    set /p CAMPUSNEST_DB_USER="Enter Database User (e.g. postgres): "
)
if "%CAMPUSNEST_DB_PASSWORD%"=="" (
    set /p CAMPUSNEST_DB_PASSWORD="Enter Database Password: "
)

echo Starting CampusNest with Supabase backend on http://localhost:8081/campusnest/ ...
"C:\Users\sharu\maven\bin\mvn.cmd" compile exec:java -Dexec.mainClass=edu.coet.campusnest.LocalServer
pause

