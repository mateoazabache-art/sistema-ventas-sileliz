@echo off
rem =====================================================================
rem  recrear_bd.cmd  -  Gestion de Ventas Sileliz.mas  (tarea 14 de Rafael)
rem
rem  Borra ventas_db y la vuelve a crear DESDE CERO (plan 4.5, RNF-09, CP-17).
rem  ATENCION: se pierden los datos que tenga la base. Es para desarrollo.
rem
rem  USO (doble clic, o desde PowerShell en la carpeta del repo):
rem      bd\recrear_bd.cmd          base limpia con los datos de prueba
rem      bd\recrear_bd.cmd demo     ademas, 14 ventas de ejemplo (Inicio y Reportes)
rem
rem  Antes: tener SistemaVentas\src\java\config.properties (copia de
rem  config.example.properties) con su clave en db.clave. Esa misma clave
rem  se le pone a ventas_app, asi la aplicacion siempre conecta.
rem  Pide UNA vez la clave del usuario postgres (la de pgAdmin).
rem  Se conecta primero a template1 (existe siempre) para poder borrar ventas_db.
rem =====================================================================
setlocal
cd /d "%~dp0"
rem La ventana muestra UTF-8 (tildes y n bien escritas en los mensajes)
chcp 65001 >nul

rem --- 1. psql: el de PostgreSQL 18, 17 o 16, o el que este en el PATH ---
set "PSQL="
for %%v in (18 17 16) do (
    if not defined PSQL if exist "C:\Program Files\PostgreSQL\%%v\bin\psql.exe" set "PSQL=C:\Program Files\PostgreSQL\%%v\bin\psql.exe"
)
if not defined PSQL (
    where psql >nul 2>nul && set "PSQL=psql"
)
if not defined PSQL (
    echo [ERROR] No se encontro psql.exe. Instale PostgreSQL 18 o agregue su carpeta bin al PATH.
    goto :fin_error
)

rem --- 2. La clave de ventas_app sale de config.properties (db.clave) ---
set "CONFIG=..\SistemaVentas\src\java\config.properties"
if not exist "%CONFIG%" (
    echo [ERROR] Falta %CONFIG%
    echo         Copie config.example.properties con el nombre config.properties
    echo         y ponga su clave en db.clave.
    goto :fin_error
)
set "CLAVE_APP="
for /f "usebackq tokens=1,* delims==" %%a in ("%CONFIG%") do (
    if "%%a"=="db.clave" set "CLAVE_APP=%%b"
)
if not defined CLAVE_APP (
    echo [ERROR] config.properties no tiene la linea db.clave=...
    goto :fin_error
)
if "%CLAVE_APP%"=="CAMBIAR_POR_SU_CLAVE" (
    echo [ERROR] En config.properties cambie CAMBIAR_POR_SU_CLAVE por su clave real.
    goto :fin_error
)

rem --- 3. Con o sin ventas de ejemplo ---
set "DEMO=false"
if /i "%~1"=="demo" set "DEMO=true"

rem --- 4. UTF-8: sin esto, en Windows fallan las tildes y la n de 02 (hallazgo de Luz) ---
set "PGCLIENTENCODING=UTF8"

echo.
echo Se BORRARA ventas_db y se creara de nuevo (ventas de ejemplo: %DEMO%).
echo psql: %PSQL%
echo Escriba la clave del usuario postgres cuando la pida.
echo.
"%PSQL%" -X -h localhost -U postgres -d template1 -v clave="%CLAVE_APP%" -v demo=%DEMO% -f recrear_bd.sql
if errorlevel 1 (
    echo.
    echo [ERROR] La recreacion se detuvo. Lea el mensaje de arriba.
    echo         Si dice "being accessed by other users": detenga Tomcat y cierre pgAdmin.
    goto :fin_error
)

echo.
echo [OK] ventas_db recreada. Ya puede hacer Run en NetBeans.
endlocal
pause
exit /b 0

:fin_error
endlocal
pause
exit /b 1
