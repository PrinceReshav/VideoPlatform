@echo off
setlocal
cd /d "%~dp0..\.."
echo Starting Live TV Service with the self-contained dev profile...
echo PostgreSQL, Redis and Kafka are NOT required for this local smoke test.
call mvn -pl live-tv-service -am spring-boot:run -Dspring-boot.run.profiles=dev
endlocal
