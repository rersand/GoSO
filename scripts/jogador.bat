@echo off
rem Inicia um jogador. Abra duas janelas. Uso: jogador.bat [host] [porta]   (padrao localhost 5000)
call "%~dp0ambiente.bat"
if not exist "%~dp0..\dist\go9x9.jar" call "%~dp0compilar.bat"
java -cp "%~dp0..\dist\go9x9.jar" br.go9x9.cliente.Cliente %*
