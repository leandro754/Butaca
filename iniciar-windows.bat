@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\demo.ps1" %*
set "butacaExitCode=%ERRORLEVEL%"
if not "%butacaExitCode%"=="0" pause
exit /b %butacaExitCode%
