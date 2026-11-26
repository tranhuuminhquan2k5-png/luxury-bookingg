@echo off
setlocal
cd /d "%~dp0"
if not defined DB_USERNAME set "DB_USERNAME=root"
echo Luxury Hotel - demo local. Tu kiem tra MariaDB XAMPP rieng o cong 3309.
echo Dung du lieu cu tai C:\xampp\mysql\data; khong dung database tram sac o cong 3308.
echo Website: http://localhost:8080 - Nhan Ctrl+C de dung.
echo Neu database co mat khau, thiet lap DB_PASSWORD theo README truoc khi chay.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start.ps1" -Demo
if errorlevel 1 pause
endlocal
