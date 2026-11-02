@echo off
set USE_LOCAL_DB=true
set CAMPUSNEST_DB_URL=
set CAMPUSNEST_DB_USER=
set CAMPUSNEST_DB_PASSWORD=
echo ==============================================================
echo  Starting COET CampusNest on localhost:8081 with LOCAL Database
echo ==============================================================
"C:\Users\sharu\maven\bin\mvn.cmd" compile exec:java -Dexec.mainClass=edu.coet.campusnest.LocalServer -DUSE_LOCAL_DB=true
pause
