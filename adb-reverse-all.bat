@echo off
setlocal enabledelayedexpansion

where adb >nul 2>nul
if errorlevel 1 (
    echo adb not found in PATH.
    exit /b 1
)

for /f "skip=1 tokens=1,2" %%A in ('adb devices') do (
    if not "%%A"=="" if "%%B"=="device" (
        echo Applying adb reverse for %%A ...
        adb -s %%A reverse tcp:8082 tcp:8082
    )
)

echo Done. Run this again any time a device is unplugged and replugged.
endlocal
