# Student Management System

একটি আধুনিক, নিরাপদ ও হালকা Student Management System।

## প্রযুক্তি
- Backend: Java, Spring Boot, Spring Data JPA, Spring Security
- Database: MySQL, Flyway
- Frontend: React, Vite, Tailwind CSS

## চালানোর নিয়ম (ডেভেলপমেন্ট)

### Backend
1. MySQL-এ `sms_db` ডেটাবেস ও একটি ব্যবহারকারী তৈরি করুন
2. Environment Variable সেট করুন: `DB_USER`, `DB_PASSWORD`
3. `backend` ফোল্ডার থেকে চালান: `./mvnw spring-boot:run`

### Frontend
```
cd frontend
npm install
npm run dev
```

## অবস্থা
প্রজেক্ট চলমান (Phase 0 সম্পন্ন)।


### Phase 2: একাডেমিক সিস্টেম
- গ্রেড স্কেল (বদলানো যায়) ও GPA হিসাব (সীমানার মানসহ স্বয়ংক্রিয় টেস্ট)
- পরীক্ষা ও বিষয়ভিত্তিক ফুল/পাস মার্কস, ঐচ্ছিক বিষয়
- মার্কস এন্ট্রি: শিক্ষক শুধু নিজের সেকশন-বিষয়ে; ফল প্রকাশের পর Lock, শুধু Admin কারণ লিখে সংশোধন (অডিট লগসহ)
- ফল ও মেধাক্রম (শ্রেণি ও শাখা), বাংলা মার্কশিট ও রেজাল্ট শিট (ব্রাউজার PDF)
- শিক্ষার্থীর আগের ফল
- ক্লাস রুটিন: রুম, পিরিয়ড, শিক্ষক/রুম/সেকশনের সংঘাত ডেটাবেসে তালা-দেওয়া