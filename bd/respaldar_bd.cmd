@echo off
rem =====================================================================
rem  respaldar_bd.cmd  -  Gestion de Ventas Sileliz.mas  (tareas 14 y 17 de Rafael)
rem
rem  Guarda una copia completa de ventas_db (estructura + datos) con pg_dump.
rem  El respaldo queda FUERA del repositorio, en:
rem      %USERPROFILE%\respaldos-sileliz\ventas_db_AAAA-MM-DD_HHMM.sql
rem  (nunca se sube a GitHub). Pide la clave del usuario postgres.
rem
rem  RESTAURAR un respaldo (borra lo que tenga ventas_db):
rem      1. bd\recrear_bd.cmd           (deja la base y ventas_app listas)
rem      2. psql -U postgres -d ventas_db -f "RUTA\DEL\RESPALDO.sql"
rem  El archivo es SQL normal: tambien se puede abrir en pgAdmin (Query Tool) y ejecutar.
rem =====================================================================
setlocal
cd /d "%~dp0"
rem La ventana muestra UTF-8 (tildes y n bien escritas en los mensajes)
chcp 65001 >nul

set "PGDUMP="
for %%v in (18 17 16) do (
    if not defined PGDUMP if exist "C:\Program Files\PostgreSQL\%%v\bin\pg_dump.exe" set "PGDUMP=C:\Program Files\PostgreSQL\%%v\bin\pg_dump.exe"
)
if not defined PGDUMP (
    where pg_dump >nul 2>nul && set "PGDUMP=pg_dump"
)
if not defined PGDUMP (
    echo [ERROR] No se encontro pg_dump.exe. Instale PostgreSQL 18 o agregue su carpeta bin al PATH.
    goto :fin_error
)

set "CARPETA=%USERPROFILE%\respaldos-sileliz"
if not exist "%CARPETA%" mkdir "%CARPETA%"
for /f %%f in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HHmm"') do set "FECHA=%%f"
set "ARCHIVO=%CARPETA%\ventas_db_%FECHA%.sql"
set "PGCLIENTENCODING=UTF8"

echo.
echo Respaldando ventas_db en:
echo     %ARCHIVO%
echo Escriba la clave del usuario postgres cuando la pida.
echo.
rem --clean --if-exists: al restaurar, primero borra lo que haya (tablas, vistas...)
rem --no-owner: se restaura con el usuario que ejecuta (postgres)
"%PGDUMP%" -h localhost -U postgres -d ventas_db --clean --if-exists --no-owner -f "%ARCHIVO%"
if errorlevel 1 (
    echo.
    echo [ERROR] No se pudo respaldar. Lea el mensaje de arriba.
    goto :fin_error
)

echo.
echo [OK] Respaldo listo:
dir "%ARCHIVO%" | findstr /i "ventas_db_"
endlocal
pause
exit /b 0

:fin_error
endlocal
pause
exit /b 1
