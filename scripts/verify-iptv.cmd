@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0verify-iptv.ps1"
if errorlevel 1 exit /b 1
