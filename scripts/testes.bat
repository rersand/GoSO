@echo off
rem Compila codigo + testes e executa todos os testes automatizados.
setlocal
call "%~dp0ambiente.bat"
cd /d "%~dp0.."

where javac >nul 2>nul
if errorlevel 1 (
    echo ERRO: javac nao encontrado. Instale um JDK ou abra o projeto no IntelliJ.
    exit /b 1
)

if exist build\test-classes rmdir /s /q build\test-classes
mkdir build\test-classes

javac --release 8 -encoding UTF-8 -Xlint:all,-options -d build\test-classes ^
    src\br\go9x9\modelo\*.java ^
    src\br\go9x9\regras\*.java ^
    src\br\go9x9\protocolo\*.java ^
    src\br\go9x9\servidor\*.java ^
    src\br\go9x9\cliente\*.java ^
    test\br\go9x9\*.java
if errorlevel 1 exit /b 1

java -cp build\test-classes br.go9x9.ExecutarTestes
exit /b %errorlevel%
