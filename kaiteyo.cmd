@echo off
rem Kaiteyo CLI — repository-root launcher (Windows).
setlocal
set "KCLI_DIR=%~dp0tools\cli"
where py >nul 2>nul
if %errorlevel%==0 (
    py -3 "%KCLI_DIR%\kaiteyo_cli\__main__.py" %*
) else (
    python "%KCLI_DIR%\kaiteyo_cli\__main__.py" %*
)
exit /b %errorlevel%
