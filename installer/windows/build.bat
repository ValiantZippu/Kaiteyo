@echo off
REM ============================================================================
REM Kaiteyo — Windows installer build (cmd wrapper for CI)
REM Usage: build.bat <version> [cert-thumbprint]
REM   cert-thumbprint optional; when given, the installer is signed.
REM ============================================================================
setlocal

if "%~1"=="" (
  echo Usage: build.bat ^<version^> [cert-thumbprint]
  exit /b 1
)

set "VERSION=%~1"
set "THUMB=%~2"

if "%THUMB%"=="" (
  powershell -ExecutionPolicy Bypass -File "%~dp0build.ps1" -Version "%VERSION%"
) else (
  powershell -ExecutionPolicy Bypass -File "%~dp0build.ps1" -Version "%VERSION%" -Sign -CertThumbprint "%THUMB%"
)

exit /b %ERRORLEVEL%
