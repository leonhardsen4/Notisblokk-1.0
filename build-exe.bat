@echo off
REM ============================================================
REM NOTISBLOKK 1.0 - BUILD INSTALADOR WINDOWS (.exe)
REM ============================================================
echo.
echo ========================================
echo   NOTISBLOKK 1.0 - BUILD INSTALADOR
echo ========================================
echo.

REM Verificar se JAR existe
if not exist "target\notisblokk-1.0.0.jar" (
    echo [ERRO] JAR nao encontrado!
    echo Execute primeiro: mvn clean package
    pause
    exit /b 1
)

echo [1/3] JAR encontrado: target\notisblokk-1.0.0.jar
echo.

REM Criar diretório de saída
if not exist "dist" mkdir dist
echo [2/3] Diretorio dist criado
echo.

REM Executar jpackage
echo [3/3] Gerando instalador Windows...
echo.
echo Isso pode levar alguns minutos...
echo.

jpackage ^
  --input target ^
  --name "Notisblokk" ^
  --main-jar notisblokk-1.0.0.jar ^
  --main-class com.notisblokk.Main ^
  --type exe ^
  --dest dist ^
  --app-version 1.0 ^
  --description "Notisblokk - Sistema de Gerenciamento" ^
  --vendor "Notisblokk Team" ^
  --win-console ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   INSTALADOR GERADO COM SUCESSO!
    echo ========================================
    echo.
    echo Arquivo: dist\Notisblokk-1.0.exe
    echo Tamanho aproximado: 60-80 MB
    echo.
    echo O instalador inclui:
    echo  - Aplicacao completa
    echo  - JRE embutida ^(nao precisa Java instalado^)
    echo  - Atalho no Menu Iniciar
    echo  - Opcao de desinstalacao
    echo.
) else (
    echo.
    echo [ERRO] Falha ao gerar instalador!
    echo.
    echo Verifique se o JDK 14+ esta instalado
    echo Execute: java -version
    echo.
)

pause
