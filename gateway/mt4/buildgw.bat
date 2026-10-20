@echo off
REM Build the MT4 gateway. Run this in the "x64 Native Tools Command Prompt for VS"
REM from the folder that contains: mt4gateway.cpp, Include\MT4ManagerAPI.h, libs\mtmanapi64.dll
REM (the Include\ and libs\ come from the licensed MetaQuotes SDK — NOT in this repo).
set VCPKG=C:\vcpkg\installed\x64-windows
cl /EHsc /MD /D_CRT_SECURE_NO_WARNINGS /I Include /I "%VCPKG%\include" mt4gateway.cpp ws2_32.lib "%VCPKG%\lib\rdkafka.lib" /Fe:mt4gateway.exe
if errorlevel 1 ( echo. & echo BUILD FAILED - send me ALL the error text. & exit /b 1 )
copy /Y libs\mtmanapi64.dll . >nul
copy /Y "%VCPKG%\bin\*.dll" . >nul
echo.
echo BUILD OK. mtmanapi64.dll + librdkafka DLLs copied next to the exe. Run (IPv4!):
echo    mt4gateway.exe ^<server:port^> ^<managerLogin^> ^<password^> ^<sourceServerId^> 127.0.0.1:29092 30
