@echo off
rem Compila o projeto (compativel com Java 8+) e gera dist\go9x9.jar.
setlocal
call "%~dp0ambiente.bat"
cd /d "%~dp0.."

where javac >nul 2>nul
if errorlevel 1 (
    echo ERRO: javac nao encontrado. Instale um JDK ou abra o projeto no IntelliJ.
    exit /b 1
)

if exist build rmdir /s /q build
if exist dist rmdir /s /q dist
mkdir build\classes
mkdir dist

javac --release 8 -encoding UTF-8 -Xlint:all,-options -d build\classes ^
    src\br\go9x9\modelo\*.java ^
    src\br\go9x9\regras\*.java ^
    src\br\go9x9\protocolo\*.java ^
    src\br\go9x9\servidor\*.java ^
    src\br\go9x9\cliente\*.java
if errorlevel 1 exit /b 1

jar cfe dist\go9x9.jar br.go9x9.cliente.Cliente -C build\classes .
if errorlevel 1 exit /b 1

echo Compilado com sucesso: dist\go9x9.jar
