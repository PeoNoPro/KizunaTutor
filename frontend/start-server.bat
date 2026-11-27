@echo off
title KizunaTutor Frontend Server (Port 3000)
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -NoProfile -File ".\start-server.ps1" %*
if errorlevel 1 pause
