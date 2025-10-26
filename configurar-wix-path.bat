@echo off
echo ========================================
echo   CONFIGURAR WIX NO PATH
echo ========================================
echo.

REM Tentar localizar WiX
set WIX_PATH_86=C:\Program Files (x86)\WiX Toolset v3.14\bin
set WIX_PATH_64=C:\Program Files\WiX Toolset v3.14\bin

if exist "%WIX_PATH_86%\candle.exe" (
    set WIX_BIN=%WIX_PATH_86%
    echo [OK] WiX encontrado em: %WIX_PATH_86%
) else if exist "%WIX_PATH_64%\candle.exe" (
    set WIX_BIN=%WIX_PATH_64%
    echo [OK] WiX encontrado em: %WIX_PATH_64%
) else (
    echo [ERRO] WiX nao encontrado!
    echo.
    echo Verifique se o WiX foi instalado corretamente.
    echo Procure por: candle.exe
    pause
    exit /b 1
)

echo.
echo Adicionando ao PATH do sistema...
echo.
echo ATENCAO: Sera solicitado permissao de Administrador
echo.

REM Adicionar ao PATH (requer administrador)
powershell -Command "Start-Process cmd -ArgumentList '/c setx PATH \"%%PATH%%;%WIX_BIN%\" /M' -Verb RunAs -Wait"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   PATH CONFIGURADO COM SUCESSO!
    echo ========================================
    echo.
    echo WiX adicionado ao PATH: %WIX_BIN%
    echo.
    echo IMPORTANTE:
    echo  1. FECHE este terminal
    echo  2. ABRA um NOVO terminal
    echo  3. Execute: candle.exe -?
    echo  4. Se funcionar, execute: build-exe.bat
    echo.
) else (
    echo.
    echo [ERRO] Falha ao configurar PATH
    echo.
    echo Tente adicionar manualmente:
    echo  1. Tecla Windows ^> "variaveis de ambiente"
    echo  2. Variaveis do sistema ^> Path ^> Editar
    echo  3. Adicionar: %WIX_BIN%
    echo.
)

pause
