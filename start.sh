#!/usr/bin/env bash
# start.sh - স্কুল ম্যানেজমেন্ট সিস্টেম ওয়ান-ক্লিক স্টার্ট স্ক্রিপ্ট (Linux / macOS)

set -e

echo "=========================================================="
echo "  School Management System - Docker Production Launcher   "
echo "=========================================================="

if [ ! -f .env ]; then
    if [ -f .env.example ]; then
        echo "[INFO] .env ফাইল পাওয়া যায়নি, .env.example থেকে কপি করা হচ্ছে..."
        cp .env.example .env
    else
        echo "[ERROR] .env বা .env.example ফাইল পাওয়া যায়নি!"
        exit 1
    fi
fi

echo "[INFO] ডকার কন্টেইনারগুলো বিল্ড ও চালু করা হচ্ছে..."
docker compose up -d --build

echo ""
echo "[SUCCESS] সিস্টেম সফলভাবে ব্যাকগ্রাউন্ডে চালু হয়েছে!"
echo "ব্রাউজারে খুলুন: http://localhost"
echo "লগ দেখতে কমান্ড দিন: docker compose logs -f"
echo "বন্ধ করতে কমান্ড দিন: docker compose down"
