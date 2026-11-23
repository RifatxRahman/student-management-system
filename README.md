# 🏫 স্কুল ম্যানেজমেন্ট সিস্টেম (Enterprise School Management System)

> **প্রোডাকশন-গ্রেড এন্টারপ্রাইজ ফুল-স্ট্যাক স্কুল ম্যানেজমেন্ট সিস্টেম**  
> নির্মিত হয়েছে **Java 21, Spring Boot, MySQL 8.4, React, Tailwind CSS, Docker, এবং Flyway** সহযোগে।

---

## 🌟 প্রযুক্তি স্ট্যাক (Technology Stack)

- **Backend**: Java 21, Spring Boot, Spring Security (OAuth2 Resource Server + JWT + Method Security), Spring Data JPA, Hibernate, Flyway Database Migrations.
- **Database**: MySQL 8.4 LTS (utf8mb4 encoding, strict transactional isolation).
- **Frontend**: React (Vite, SPA Router), Tailwind CSS, Headless UI concepts, Custom Hooks (`useFetch`, `useQuery`).
- **DevOps & Containerization**: Multi-stage Dockerfiles, Docker Compose, Nginx Reverse Proxy & Static Asset Caching, Alpine JRE 21.
- **Testing**: JUnit 5, Mockito, Testcontainers (Real MySQL in Docker for integration tests).

---

## 🚀 ২০-ধাপের সম্পূর্ণ রোডম্যাপ (20-Step Completed Architecture)

### Phase 1: Foundation (ভিত্তি স্থাপন)
- **Step 1**: Project Initialization & Modular Clean Architecture.
- **Step 2**: Flyway Database Migration Framework & Schema Versioning.
- **Step 3**: Spring Security, JWT Authentication, Refresh Token Rotation & Password Policy.
- **Step 4**: Student, Guardian & Enrollment Master Domain.

### Phase 2: Daily Operations (দৈনিক কার্যক্রম)
- **Step 5**: Daily Student Attendance Tracking & Consecutive Absence Alerts.
- **Step 6**: Teacher Management, Class Allocations & Academic Routine.
- **Step 7**: Teacher Attendance & Staff Leave Management System.

### Phase 3: Academic Core (একাডেমিক ইঞ্জিন)
- **Step 8**: Exam Configuration & Subject Full/Pass Marks Setup.
- **Step 9**: Class Routine & Timetable Conflict-Free Engine.
- **Step 10**: Academic Calendar, Holidays & Institutional Event Management.
- **Step 11**: Marks Entry, Auditing & Correction Workflow.
- **Step 12**: Automated Grading, GPA Calculation, Merit Ranking & Result Publication.

### Phase 4: Financial Core (আর্থিক ব্যবস্থাপনা)
- **Step 13**: Fee Head, Structure Setup & Class-wise Fee Templates.
- **Step 14**: Student Waiver, Invoice Lifecycle & Partial/Full Payment Collection.
- **Step 15**: Expense Tracking, Category Management & Income-Expense Accounts Summary.

### Phase 5: Communication, Security & Production Polish (সিস্টেম নিরাপত্তা ও ডিপ্লয়মেন্ট)
- **Step 16**: SMS & Email Notification Engine with Greenweb SMS Gateway & Campaigns.
- **Step 17**: Role-Based Access Control (RBAC), 18 Fine-Grained Permissions & Custom User Overrides.
- **Step 18**: Immutable System Audit Trail, Reverse-Proxy IP Capturing & Sliding-Window Rate Limiter.
- **Step 19**: Comprehensive Integration Testing & FinTech/Academic Edge Case Verification.
- **Step 20**: Multi-Stage Dockerization, Production Nginx Reverse Proxy & Senior Interview Defense.

---

## 🐳 এক ক্লিকে ডকার ডিপ্লয়মেন্ট (One-Click Deployment)

### ১. পূর্বশর্ত
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) ইনস্টল ও চালু থাকতে হবে।

### ২. সিস্টেম চালু করা (Run with 1 Command)

**Windows (PowerShell):**
```powershell
.\start.ps1
```

**Linux / macOS (Bash):**
```bash
chmod +x start.sh
./start.sh
```

অথবা সরাসরি ডকার কম্পোজ দিয়ে:
```bash
cp .env.example .env
docker compose up -d --build
```

### ৩. ব্রাউজারে প্রবেশ করুন
- **ওয়েব অ্যাপ্লিকেশন**: [http://localhost](http://localhost)
- **ডিফল্ট অ্যাডমিন ক্রেডেনশিয়াল**:
  - ইউজারনেম: `admin`
  - পাসওয়ার্ড: `AdminSecure2026!`

### ৪. লগ দেখা ও বন্ধ করা
```bash
# রিয়েলটাইম লগ দেখতে
docker compose logs -f

# সার্ভিস বন্ধ করতে
docker compose down
```

---

## 🔒 নিরাপত্তা ও হার্ডেনিং ফিচারসমূহ

1. **Layered Authorization Engine**:
   - Role Defaults (`ADMIN`, `TEACHER`, `ACCOUNTANT`, `STAFF`) + User Custom Overrides (Grant/Revoke).
   - JWT টোকেনের ভেতরে `authorities` ক্লেইমের মাধ্যমে প্রতি রিকোয়েস্টে ডাটাবেজ হিট না করেই `@PreAuthorize` এনফোর্সমেন্ট।
2. **Brute-Force & Credential Stuffing Protection**:
   - `LoginRateLimiter`: স্লাইডিং উইন্ডো রেট লিমিটার (প্রতি আইপিতে মিনিটে সর্বোচ্চ ১০টি রিকোয়েস্ট)।
   - অতিরিক্ত ভুল পাসওয়ার্ডে স্বয়ংক্রিয় একাউন্ট লকআউট (১৫ মিনিট)।
3. **Data Protection & Compliance**:
   - `AuditMasker`: অডিট লগে পাসওয়ার্ড, বেয়ারার টোকেন বা গোপন তথ্য স্বয়ংক্রিয় মাস্কিং (`***PROTECTED***`)।
   - Nginx ও `HttpRequestContext`-এর মাধ্যমে ক্লায়েন্টের প্রকৃত আইপি (`X-Forwarded-For`) ট্র্যাকিং।
4. **Security Headers**:
   - `Content-Security-Policy (CSP)`, `HSTS`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff` সক্রিয়।

---

## 🧪 টেস্ট সুইট চালানো (Running Tests)

```bash
cd backend
./mvnw clean test
```
- **Financial Tests**: `FeeLifecycleIntegrationTest` (আংশিক পরিশোধ, ওভারপেমেন্ট রিজেকশন, ওয়েভার ক্যালকুলেশন)।
- **Academic Tests**: `AcademicAndGradingIntegrationTest` (ফুল মার্কস ভ্যালিডেশন, ফেল পেনাল্টি, ৪র্থ বিষয় বোনাস)।
- **Security Tests**: `SecurityAndRbacIntegrationTest` (পারমিশন ওভাররাইড, কনকারেন্ট রেট লিমিট স্ট্রেস টেস্ট)।
- **Real MySQL Tests**: `MySqlIntegrationTest` (২০টি Flyway মাইগ্রেশন ভ্যালিডেশন)।
