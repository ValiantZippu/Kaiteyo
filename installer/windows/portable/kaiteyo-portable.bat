@echo off
REM ============================================================================
REM Kaiteyo Portable — zero-install launcher
REM All user data (settings, databases, logs) is kept inside the data\ folder
REM next to this launcher, so the whole directory is self-contained.
REM ============================================================================
setlocal

set "PORTABLE_ROOT=%~dp0"
set "KAITEYO_DATA_DIR=%PORTABLE_ROOT%data"

powershell -ExecutionPolicy Bypass -File "%PORTABLE_ROOT%launcher.ps1" "%PORTABLE_ROOT%Kaiteyo.exe" %*

exit /b %ERRORLEVEL%
