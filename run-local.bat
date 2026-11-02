@echo off
echo Starting COET CampusNest on localhost:8081...
"C:\Users\sharu\maven\bin\mvn.cmd" compile exec:java -Dexec.mainClass=edu.coet.campusnest.LocalServer
pause
