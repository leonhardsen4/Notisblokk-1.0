@echo off
REM ============================================================
REM NOTISBLOKK 1.0 - BUILD VERSAO PORTATIL (SEM INSTALADOR)
REM ============================================================
echo.
echo ========================================
echo   NOTISBLOKK 1.0 - BUILD PORTATIL
echo ========================================
echo.

REM Verificar se JAR existe
if not exist "target\notisblokk.jar" (
    echo [ERRO] JAR nao encontrado!
    echo Execute primeiro: mvn clean package
    pause
    exit /b 1
)

echo [1/3] JAR encontrado: target\notisblokk.jar
echo.

REM Criar diretório de saída
if not exist "dist-portable" mkdir dist-portable
echo [2/3] Diretorio dist-portable criado
echo.

REM Executar jpackage (tipo app-image = pasta portatil)
echo [3/3] Gerando versao portatil...
echo.
echo Isso pode levar alguns minutos...
echo.

jpackage ^
  --input target ^
  --name "Notisblokk" ^
  --main-jar notisblokk.jar ^
  --main-class com.notisblokk.Main ^
  --type app-image ^
  --dest dist-portable ^
  --app-version 1.0 ^
  --description "Notisblokk - Sistema de Gerenciamento" ^
  --vendor "Notisblokk Team" ^
  --win-console

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   VERSAO PORTATIL GERADA COM SUCESSO!
    echo ========================================
    echo.
    echo Pasta: dist-portable\Notisblokk\
    echo Executavel: dist-portable\Notisblokk\Notisblokk.exe
    echo Tamanho aproximado: 60-80 MB
    echo.
    echo A versao portatil inclui:
    echo  - Aplicacao completa
    echo  - JRE embutida ^(nao precisa Java instalado^)
    echo  - Executavel direto ^(sem instalador^)
    echo  - Pode copiar pasta inteira para qualquer lugar
    echo.
    echo Para usar:
    echo  1. Copie a pasta dist-portable\Notisblokk para onde quiser
    echo  2. Execute Notisblokk.exe
    echo.
) else (
    echo.
    echo [ERRO] Falha ao gerar versao portatil!
    echo.
    echo Verifique se o JDK 14+ esta instalado
    echo Execute: java -version
    echo.
)

pause
