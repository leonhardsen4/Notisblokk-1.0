@echo off
echo ========================================
echo   INSTALAR WIX TOOLSET
echo ========================================
echo.
echo O instalador do WiX sera aberto.
echo.
echo Instrucoes:
echo  1. Clique em "Install" (ou "Instalar")
echo  2. Aceite os termos
echo  3. Aguarde a instalacao (1-2 minutos)
echo  4. Clique em "Finish" (ou "Concluir")
echo.
echo Apos a instalacao, volte aqui e pressione qualquer tecla...
echo.
pause

REM Abrir instalador
start /wait wix314-installer.exe

echo.
echo ========================================
echo   CONFIGURANDO VARIAVEL DE AMBIENTE
echo ========================================
echo.

REM Adicionar ao PATH (requer administrador)
powershell -Command "Start-Process cmd -ArgumentList '/c setx PATH \"%PATH%;C:\Program Files (x86)\WiX Toolset v3.14\bin\" /M' -Verb RunAs"

echo.
echo Pronto! WiX Toolset instalado.
echo.
echo IMPORTANTE: Feche este terminal e abra um NOVO para o PATH funcionar.
echo.
pause
