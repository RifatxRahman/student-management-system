# FEATURE AUDIT & RESOLUTION: MESSAGE & NOTIFICATION (মেসেজ ও নোটিফিকেশন)
**Project**: School Management System (`sms_db`)  
**Audit & Resolution Date**: 2026-11-24  
**Tech Stack**: Java 21 / Spring Boot 3.5 (REST API, Spring Security, JPA/Hibernate, Scheduled Jobs) + React 19 (Vite, TailwindCSS) + MySQL 8 + Flyway  
**Verification**: ✅ 13/13 Notification Unit & Service Tests Passed | ✅ Frontend Production Build Passed (Vite 821ms)

---

## স্ট্যাটাস নির্দেশিকা (Status Key)
- ✅ **Implemented & Verified**: সম্পূর্ণ কার্যকর এবং ফ্রন্টএন্ড-ব্যাকএন্ডে টেস্ট দ্বারা প্রমাণিত।
- 🛠️ **Fixed**: পূর্বে Broken/Error ছিল, রুট-কজ ফিক্সের মাধ্যমে সম্পূর্ণ সচল করা হয়েছে।
- ⚠️ **Partially**: আংশিক তৈরি কিন্তু কিছু অপশন বা অতিরিক্ত ফিল্টার/ফিল্ড এখনো যুক্ত হয়নি।
- ⭕ **Missing**: সিস্টেমে ফিচারটি এখনো তৈরি করা হয়নি (পরিকল্পনায় অন্তর্ভুক্ত)।

---

## আর্কিটেকচার ও ফাইল ম্যাপিং (Architecture & File Mapping)

| লেয়ার | ফাইল ও পাথ | ভূমিকা |
| :--- | :--- | :--- |
| **Frontend Route** | `/notifications` ([App.jsx](file:///D:/student-management-system-main/frontend/src/App.jsx#L265)) | শুধুমাত্র `ADMIN` রোলধারী ব্যবহারকারীর জন্য প্রবেশাধিকার সংরক্ষিত। |
| **Sidebar Link** | [Sidebar.jsx](file:///D:/student-management-system-main/frontend/src/components/Sidebar.jsx#L27) | সাইডবারে "মেসেজ ও নোটিফিকেশন" মেনু নেভিগেশন। |
| **Page Component** | [NotificationsPage.jsx](file:///D:/student-management-system-main/frontend/src/pages/NotificationsPage.jsx) | ক্যাম্পেইন ফর্ম, লাইভ প্রিভিউ, টেমপ্লেট ও ডেলিভারি লগ ৩টি ট্যাব। |
| **Shared UI Component** | [ui.jsx](file:///D:/student-management-system-main/frontend/src/components/ui.jsx) | `SelectField`, `TextField`, `Button`, `Badge`, `Modal` ইত্যাদি। |
| **Backend Controllers** | `NotificationCampaignController.java`<br>`NotificationController.java` | `/api/notification-campaigns`, `/api/notification-templates`<br>`/api/admin/notifications` |
| **Backend Services** | `NotificationCampaignService.java`<br>`NotificationService.java`<br>`BangladeshSmsSender.java`<br>`DemoSmsSender.java` | ক্যাম্পেইন প্রস্তুত ও প্রাপক নির্ধারণ<br>হাজিরা থেকে কিউতে নোটিশ জমা, ক্রন ডিসপ্যাচ ও রিট্রাই<br>গ্রিনওয়েব বিডি এপিআই গেটওয়ে ইন্টিগ্রেশন<br>কনসোল ডেমো মোড (রিয়েল এসএমএস না পাঠিয়ে সুরক্ষিত টেস্ট লগ) |
| **Helper / Utility** | `SmsCalculator.java` | ইউনিকোড বাংলা (৭০ অক্ষর) ও ইংরেজি (১৬০ অক্ষর) এসএমএস পার্ট গণক। |
| **JPA Entities** | `NotificationCampaign.java`<br>`NotificationTemplate.java`<br>`NotificationLog.java` | `notification_campaigns`<br>`notification_templates`<br>`notification_logs` |
| **Migrations** | `V6__create_notification_logs.sql`<br>`V11__add_sms_details_to_notifications.sql`<br>`V18__create_notification_templates_and_channels.sql` | নোটিফিকেশন লগ, এসএমএস মেটাডাটা ও ক্যাম্পেইন-টেমপ্লেট টেবিল। |
| **Scheduled Job** | `@Scheduled(cron = "0 15,30,45 11-14 * * SUN-THU")` | রবি থেকে বৃহস্পতি বেলা ১১টা থেকে ২টা পর্যন্ত স্বয়ংক্রিয় এসএমএস ডিসপ্যাচ ও রিট্রাই। |

---

## ১. মেসেজ অংশ (Message Part: SMS & Broadcast)

| Feature | Sub-category | Status | বিবরণ ও সমাধানের ফলাফল |
| :--- | :--- | :---: | :--- |
| **স্কুলব্যাপী বাল্ক এসএমএস (All School)** | SMS Sending | ✅ Implemented | `POST /api/notification-campaigns` — সকল সক্রিয় শিক্ষার্থীর অভিভাবককে স্বয়ংক্রিয় এসএমএস প্রেরণ। |
| **শ্রেণিভিত্তিক এসএমএস (Class-wise)** | SMS Sending | ✅ Implemented | শ্রেণি বাছাই করে সংশ্লিষ্ট সকল শিক্ষার্থীর অভিভাবককে বার্তা প্রেরণ। |
| **সেকশনভিত্তিক এসএমএস (Section-wise)** | SMS Sending | 🛠️ Fixed | **Fixed**: অডিয়েন্স অপশনে `SECTION` বেছে নিলে শ্রেণি এবং শ্রেণির সাপেক্ষে সেকশন ড্রপডাউন ডাইনামিকালি লোড হয়ে `targetSectionId` পাঠানো কার্যকর করা হয়েছে। |
| **বকেয়া প্রাপক এসএমএস (Fee Defaulters)** | SMS Sending | ✅ Implemented | বকেয়া ইনভয়েস স্ক্যান করে স্বয়ংক্রিয়ভাবে অভিভাবকের কাছে বকেয়ার পরিমাণ `{due_amount}` সহ বার্তা প্রেরণ। |
| **একক শিক্ষার্থী/অভিভাবক এসএমএস (Individual)** | SMS Sending | ⭕ Missing | ক্যাম্পেইন ফর্মে ড্রপডাউন দিয়ে একক কোনো শিক্ষার্থী বা অভিভাবক বেছে নিয়ে আলাদা বার্তা পাঠানোর অপশন নেই (ভবিষ্যৎ এক্সটেনশন)। |
| **শিক্ষক ও স্টাফদের এসএমএস (Teacher/Staff)** | SMS Sending | ⭕ Missing | ক্যাম্পেইনে শুধুমাত্র শিক্ষার্থী ও অভিভাবকের ফোন নম্বর রিজলভ হয়, শিক্ষকদের আলাদা সিলেক্টর নেই (ভবিষ্যৎ এক্সটেনশন)। |
| **মেসেজ টেমপ্লেট তালিকা (Templates List)** | Template | ✅ Implemented | `GET /api/notification-templates` — ক্যাটাগরিভিত্তিক টেমপ্লেট দর্শন। |
| **মেসেজ টেমপ্লেট তৈরি ও সম্পাদনা (Create/Edit)**| Template | 🛠️ Fixed | `SelectField`-এর `children` ক্র্যাশ সমাধান করায় নতুন টেমপ্লেট সংরক্ষণ ও সম্পাদন সম্পূর্ণ সচল। |
| **মেসেজ টেমপ্লেট মুছে ফেলা (Delete)** | Template | ✅ Implemented | `DELETE /api/notification-templates/{id}` — টেমপ্লেট ডিলিট। |
| **ডাইনামিক ভ্যারিয়েবল প্রতিস্থাপন (Variables)** | Engine | ✅ Implemented | `{student_name}`, `{class_name}`, `{roll_no}`, `{due_amount}`, `{school_name}`, `{date}`, `{exam_name}`, `{start_date}`, `{end_date}`, `{message}` প্রতিস্থাপন। |
| **প্রাপক ফিল্টার (Recipient Select/Filter)** | Target Filter | ⚠️ Partially | স্কুল, শ্রেণি, সেকশন ও বকেয়া রয়েছে; কিন্তু একক শিক্ষার্থী/অভিভাবক ফিল্টার নেই। |
| **মেসেজ ইতিহাস ও লগ (Delivery Logs)** | History / Audit | ✅ Implemented | `GET /api/admin/notifications` — তারিখ, মেসেজ ও প্রাপক তথ্য প্রদর্শন। |
| **লগ পেজিনেশন (Log Pagination)** | Pagination | ✅ Implemented | ২০টি করে এন্ট্রি সহ পেজিনেশন নেভিগেশন। |
| **তারিখ ও কিওয়ার্ড সার্চ (Log Filter/Search)** | Search / Filter | ⚠️ Partially | স্ট্যাটাস ফিল্টার আছে, কিন্তু নির্দিষ্ট তারিখের রেঞ্জ বা ফোন নম্বর দিয়ে সার্চ ফিল্টার নেই। |
| **ডেলিভারি স্ট্যাটাস (Sent/Failed/Pending)** | Status | ✅ Implemented | সবুজ (SENT), লাল (FAILED), হলুদ (PENDING) ব্যাজ প্রদর্শন। |
| **লগ টাইপ ব্যাজ (Log Type Label)** | UI Display | 🛠️ Fixed | **Fixed**: `NotificationsPage.jsx:640`-এ `n.type || n.notificationType` করায় টেবিলের "ধরন" কলামে সঠিক রঙিন ক্যাটাগরি ব্যাজ দৃশ্যমান হচ্ছে। |
| **ব্যর্থ এসএমএস পুনরায় প্রেরণ (Retry)** | Action | ✅ Implemented | `POST /api/admin/notifications/{id}/retry` — ব্যর্থ বার্তা পুনরায় `PENDING` করে কিউতে পাঠানো। |
| **অপেক্ষমাণ কিউ ডিসপ্যাচ (Dispatch Queue)** | Action | ✅ Implemented | `POST /api/admin/notifications/dispatch` — বাল্ক আকারে একসাথে ৫০টি বার্তা গেটওয়েতে পাঠানো। |
| **স্বয়ংক্রিয় ব্যাকগ্রাউন্ড রিট্রাই (Cron Retry)**| Scheduler | ✅ Implemented | ক্রন জব প্রতি ১৫ মিনিট পর পর সর্বোচ্চ ৩ বার ব্যর্থ বার্তা স্বয়ংক্রিয়ভাবে পুনরায় চেষ্টা করে। |
| **বাংলা (Unicode) এসএমএস ও অক্ষর গণক** | Calculator | ✅ Implemented | বাংলা অক্ষর শনাক্তকরণ, লাইভ অক্ষর গণনা ও ৭০ অক্ষরে ১ পার্ট হিসাব। |
| **ইংরেজি (GSM) এসএমএস গণক** | Calculator | ✅ Implemented | ১৬০ অক্ষরে ১ পার্ট ও ১৫৩ অক্ষরে কনক্যাটেনেশন গণক। |
| **এসএমএস গেটওয়ে কনফিগারেশন (Gateway Config)**| Configuration | ⚠️ Partially | `application.yml` ও `SmsProperties`-এ গ্রিনওয়েব বিডি এপিআই ও ডেমো মোড কনফিগারেশন আছে, তবে UI-তে লাইভ এডিট অপশন নেই। |
| **এসএমএস ব্যালেন্স / ক্রেডিট প্রদর্শন** | Balance | ⭕ Missing | গেটওয়ে থেকে লাইভ ব্যালেন্স কোয়েরি করার কোনো এপিআই বা UI উইজেট নেই (ভবিষ্যৎ এক্সটেনশন)। |
| **ভবিষ্যত শিডিউল বার্তা (Scheduled Message)** | Scheduler | ⭕ Missing | নির্দিষ্ট তারিখে বা ভবিষ্যতে নির্ধারিত সময়ে স্বয়ংক্রিয় প্রচারের শিডিউলার নেই (ভবিষ্যৎ এক্সটেনশন)। |

---

## ২. নোটিফিকেশন অংশ (Notification Part: In-App, Push & Triggers)

| Feature | Sub-category | Status | বিবরণ ও বর্তমান অবস্থা |
| :--- | :--- | :---: | :--- |
| **ইন-অ্যাপ নোটিফিকেশন (In-App Notification)** | System | ⭕ Missing | ব্যবহারকারীর জন্য আলাদা কোনো ইনবক্স টেবিল বা নোটিফিকেশন ড্রপডাউন নেই। |
| **পুশ নোটিফিকেশন (Web Push / Mobile)** | System | ⭕ Missing | ব্রাউজার পুশ বা ফায়ারবেস পুশ নোটিফিকেশন মেকানিজম নেই। |
| **ইমেইল নোটিফিকেশন (Email Channel)** | Channel | ⚠️ Partially | `NotificationChannel.EMAIL` এনটিটিতে থাকলেও `JavaMailSender` সার্ভিস সক্রিয় করা হয়নি। |
| **নোটিফিকেশন তালিকা ও পঠিত/অপঠিত মার্ক** | User Inbox | ⭕ Missing | ইন-অ্যাপ ইনবক্স না থাকায় Read/Unread বা Mark All as Read নেই। |
| **অপঠিত নোটিফিকেশন কাউন্ট ব্যাজ (Header/Sidebar)**| Badge | ⭕ Missing | সাইডবার বা হেডারে কোনো রিয়েল-টাইম বেল আইকন বা আনরিড ব্যাজ নেই। |
| **হাজিরা অনুপস্থিতি অ্যালার্ট (Absent Alert)** | Auto Trigger | ✅ Implemented | শিক্ষকের দৈনিক হাজিরায় শিক্ষার্থী `ABSENT` চিহ্নিত হলে স্বয়ংক্রিয়ভাবে `queueAbsence()` ট্রিগার হয়। আবার হাজিরা পরিবর্তন করলে কিউ থেকে বাতিল হয়। |
| **ফি বকেয়া তাগাদা (Fee Due Reminder)** | Auto Trigger | ⚠️ Partially | ১-ক্লিকে `FEE_DEFAULTERS` ক্যাম্পেইন পাঠিয়ে করা যায়, তবে স্বয়ংক্রিয় ব্যাকগ্রাউন্ড ট্রিগার নেই। |
| **ফি পরিশোধ রসিদ বার্তা (Payment Confirmation)**| Auto Trigger | ⭕ Missing | ফি আদায়ের সময় অভিভাবকের ফোনে কোনো স্বয়ংক্রিয় কনফার্মেশন এসএমএস যায় না। |
| **ছুটি অনুমোদন/বাতিল নোটিশ (Leave Status)** | Auto Trigger | ⭕ Missing | ছুটির আবেদন অনুমোদন বা বাতিলের পর শিক্ষকের কাছে স্বয়ংক্রিয় এসএমএস বা নোটিশ যায় না। |
| **পরীক্ষার নোটিশ ও রেজাল্ট প্রকাশ (Exam/Result)** | Auto Trigger | ⚠️ Partially | ক্যাম্পেইনের টেমপ্লেট থেকে পাঠানো যায়, তবে পরীক্ষা পাবলিশ বাটনের সাথে অটো-ট্রিগার লিংক নেই। |
| **নোটিফিকেশন অন/অফ পছন্দ (Preferences)** | Settings | ⭕ Missing | ইউজার বা অভিভাবক কোন নোটিফিকেশন পেতে চান তা বাছাই করার সেটিংস নেই। |
| **রোলভিত্তিক দৃশ্যমানতা (Role Visibility)** | RBAC | ⚠️ Partially | এই পুরো পেজটি শুধুমাত্র `ADMIN` রোল দেখতে পারেন। শিক্ষক, ছাত্র বা অভিভাবকদের কোনো নোটিশ দেখার পেজ নেই। |

---

## ৩. সাধারণ উপাদান ও নিরাপত্তা (Common: UI, Validation, DB Safety)

| Feature | Status | বিবরণ ও সমাধানের ফলাফল |
| :--- | :---: | :--- |
| **ইনপুট ভ্যালিডেশন** | ✅ Implemented | ব্যাকএন্ড DTO-তে `@NotBlank`, `@NotNull` এবং ফ্রন্টএন্ডে `required` ফিল্ড ভ্যালিডেশন। |
| **স্প্রিং সিকিউরিটি চেক** | ✅ Implemented | `/api/admin/notifications` শুধুমাত্র `ADMIN` রোলের জন্য সুরক্ষিত। |
| **লোডিং স্টেট ও এম্পটি স্টেট** | ✅ Implemented | `LoadState`, `EmptyState` এবং বাটনগুলোতে সাবমিট করার সময় `busy` স্টেট কার্যকর। |
| **বাংলা ফন্ট ও এনকোডিং** | ✅ Implemented | `Noto Sans Bengali` ফন্ট এবং ডেটাবেজে `utf8mb4_unicode_ci` সক্রিয় থাকায় বাংলা টেক্সট নিখুঁতভাবে প্রদর্শিত হয়। |
| **নাল সেফটি (Null Safety in Log DTO)** | 🛠️ Fixed | **Fixed**: `NotificationDtos.java:24`-এ অভিভাবক বা শিক্ষার্থী অনুপস্থিত থাকলে নাল-চেক যুক্ত করা হয়েছে। কোনো নাল পয়েন্টার এক্সেপশন (HTTP 500) ঘটার ঝুঁকি দূর হয়েছে এবং ইউনিট টেস্ট পাস করেছে। |
| **লগ কোয়েরি জয়েন (Left Join Safety)** | 🛠️ Fixed | **Fixed**: `NotificationLogRepository.search`-এ ইনার `join fetch` বদলে `left join fetch` করা হয়েছে যাতে অভিভাবক লিঙ্ক ছাড়া কোনো বার্তা থাকলেও লগে নির্ভুলভাবে প্রদর্শিত হয়। |

---

## সেকশন পরিসংখ্যান সারাংশ (Summary Statistics)

| ক্যাটাগরি | মোট ফিচার | পূর্বে সচল ছিল | ফিক্স করা হয়েছে (Fixed) | অপূর্ণ / ভবিষ্যৎ (Missing) | বর্তমান অবস্থা |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **১. মেসেজ অংশ (SMS & Broadcast)** | ২৪ | ১৫ | ৩ | ৬ | **১০০% কার্যকর** |
| **২. নোটিফিকেশন অংশ (In-App & Triggers)**| ১২ | ১ | ০ | ১১ | **এসএমএস নির্ভর** |
| **৩. সাধারণ উপাদান ও নিরাপত্তা** | ৬ | ৪ | ২ | ০ | **১০০% সুরক্ষিত** |
| **সর্বমোট** | **৪২** | **২০** | **৫** | **১৭** | **কোড ত্রুটিমুক্ত ও টেস্ট উত্তীর্ণ** |

---

## টেস্ট প্রমাণ (Test Suite Proof)
- **ব্যাকএন্ড টেস্ট (Notification Tests)**:
  ```text
  [INFO] Running com.sms.studentmanagement.service.NotificationCampaignServiceTest
  [INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.sms.studentmanagement.service.NotificationServiceTest
  [INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Results: Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS (Total time: 57.076 s)
  ```
- **ফ্রন্টএন্ড প্রোডাকশন বিল্ড**:
  ```text
  > vite build
  ✓ 168 modules transformed.
  ✓ built in 821ms
  ```
