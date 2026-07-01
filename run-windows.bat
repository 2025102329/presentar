@echo off
setlocal
cd /d "%~dp0"

echo =====================================================
echo   DE LA CRUZ FRAGANCIAS IA - GEMINI
echo =====================================================
echo.

if not exist ".env" (
    echo No se encontro el archivo .env.
    echo Creando una plantilla para Gemini...
    > .env echo GEMINI_API_KEY=PEGA_AQUI_TU_API_KEY
    >> .env echo GEMINI_MODEL=gemini-3.5-flash
    >> .env echo GEMINI_ENABLED=true
    >> .env echo PORT=8081
    echo.
    echo Abre .env, pega tu clave de Gemini y vuelve a ejecutar.
    pause
    exit /b 1
)

findstr /C:"PEGA_AQUI_TU_API_KEY" ".env" >nul
if %errorlevel%==0 (
    echo AVISO: Falta colocar GEMINI_API_KEY en .env.
    echo El sistema iniciara, pero trabajara en modo local.
    echo.
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo ERROR: Maven no esta disponible en el PATH.
    echo Verifica con: mvn -version
    pause
    exit /b 1
)

start "" powershell -NoProfile -WindowStyle Hidden -Command "Start-Sleep -Seconds 7; Start-Process 'http://localhost:8081'"
mvn spring-boot:run
pause
