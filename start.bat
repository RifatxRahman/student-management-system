@echo off
chcp 65001 >nul
echo ==========================================================
echo   School Management System - Docker Production Launcher
echo ==========================================================

if not exist .env (
    if exist .env.example (
        echo [INFO] .env ফাইল পাওয়া যায়নি, .env.example থেকে কপি করা হচ্ছে...
        copy .env.example .env
    ) else (
        echo [ERROR] .env বা .env.example ফাইল পাওয়া যায়নি!
        pause
        exit /b 1
    )
)

echo [INFO] ডকার কন্টেইনারগুলো বিল্ড ও চালু করা হচ্ছে...
docker compose up -d --build

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] সিস্টেম সফলভাবে ব্যাকগ্রাউন্ডে চালু হয়েছে!
    echo ব্রাউজারে খুলুন: http://localhost
    echo লগ দেখতে কমান্ড: docker compose logs -f
    echo বন্ধ করতে কমান্ড: docker compose down
) else (
    echo.
    echo [ERROR] ডকার স্টার্ট হতে সমস্যা হয়েছে। Docker Desktop চালু আছে কি না পরীক্ষা করুন।
)
pause
