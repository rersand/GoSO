@echo off
rem Garante que "java", "javac" e "jar" estejam no PATH.
rem Usa o que ja estiver no PATH; se nao houver javac, procura um JDK baixado pelo IntelliJ.

where javac >nul 2>nul
if not errorlevel 1 goto :fim

for /d %%D in ("%USERPROFILE%\.jdks\*") do (
    if exist "%%D\bin\javac.exe" set "JDK_ENCONTRADO=%%D"
)
if defined JDK_ENCONTRADO set "PATH=%JDK_ENCONTRADO%\bin;%PATH%"

:fim
exit /b 0
