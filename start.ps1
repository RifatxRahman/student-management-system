# start.ps1 - স্কুল ম্যানেজমেন্ট সিস্টেম ওয়ান-ক্লিক স্টার্ট স্ক্রিপ্ট (Windows)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  School Management System - Docker Production Launcher   " -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# .env ফাইল চেক
if (-not (Test-Path ".env")) {
    if (Test-Path ".env.example") {
        Write-Host "[INFO] .env ফাইল পাওয়া যায়নি, .env.example থেকে কপি করা হচ্ছে..." -ForegroundColor Green
        Copy-Item ".env.example" ".env"
    } else {
        Write-Host "[ERROR] .env বা .env.example ফাইল পাওয়া যায়নি!" -ForegroundColor Red
        exit 1
    }
}

Write-Host "[INFO] ডকার কন্টেইনারগুলো বিল্ড ও চালু করা হচ্ছে..." -ForegroundColor Green
docker compose up -d --build

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUCCESS] সিস্টেম সফলভাবে ব্যাকগ্রাউন্ডে চালু হয়েছে!" -ForegroundColor Green
    Write-Host "ব্রাউজারে খুলুন: http://localhost" -ForegroundColor Cyan
    Write-Host "লগ দেখতে কমান্ড দিন: docker compose logs -f" -ForegroundColor Yellow
    Write-Host "বন্ধ করতে কমান্ড দিন: docker compose down" -ForegroundColor Yellow
} else {
    Write-Host "`n[ERROR] ডকার স্টার্ট হতে সমস্যা হয়েছে। অনুগ্রহ করে ডকার ডেস্কটপ চালু আছে কি না পরীক্ষা করুন।" -ForegroundColor Red
}
