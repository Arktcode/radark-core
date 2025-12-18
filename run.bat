@echo off
REM Ejecuta el script para el bot de Discord Mindustry (Windows)
echo =================================================
echo Mindustry Discord Bot - Build Made by Arksource.
echo =================================================
echo.

REM Comprueba si existe el archivo .env
if not exist .env (
    echo [WARNING] El archivo .env no existe
    if exist .env.example (
        echo Copiando .env.example a .env...
        copy .env.example .env
        echo Por favor edita .env y añade tu DISCORD_TOKEN
    ) else (
        echo Por favor crea el archivo .env con el DISCORD_TOKEN
    )
)

REM Comprueba si existe gradlew.bat
if not exist gradlew.bat (
    echo [WARNING] gradlew.bat Falta. Intentando ejecutar el wrapper directamente...
    java -cp gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain build
) else (
    echo [BUILD] Construyendo el proyecto...
    call gradlew.bat shadowJar
)

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build fallida :C.
    exit /b 1
)

echo [SUCCESS] Build exitosa :D.
echo.

REM Comprueba si existe la carpeta de assets
set ASSETS_PATH=.\assets
if not exist "%ASSETS_PATH%" (
    echo [WARNING] Assets no encontrados en %ASSETS_PATH%
    echo Por favor ejecuta download_assets.ps1 primero.
    exit /b 1
)

echo [START] Iniciando el bot :P...
echo.

REM Ejecuta el bot
java -Dassets.path="%ASSETS_PATH%" -jar build\libs\mindustry-discord-bot-1.0.0.jar
