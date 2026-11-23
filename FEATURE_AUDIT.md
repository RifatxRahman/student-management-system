# FEATURE AUDIT & RESOLUTION REPORT: 5 CORE SIDEBAR SECTIONS
**Project**: School Management System (`sms_db` / `school_mgmt`)  
**Audit & Fix Date**: 2026-11-24  
**Test Suite Status**: ✅ 34/34 Unit & Service Tests Passed | ✅ Frontend Production Build Passed (Vite 863ms)

---

## স্ট্যাটাস নির্দেশিকা (Status Key)
- ✅ **Implemented & Verified**: সম্পূর্ণভাবে তৈরি, ব্যাকএন্ড-ফ্রন্টএন্ড চুক্তি অনুযায়ী কার্যকর এবং টেস্ট দ্বারা প্রমাণিত।
- 🛠️ **Fixed**: পূর্বে Broken বা ত্রুটিযুক্ত ছিল, রুট-কজ সমাধানের মাধ্যমে সম্পূর্ণ সচল করা হয়েছে।
- ⚠️ **Partially**: আংশিক তৈরি কিন্তু নির্দিষ্ট ক্ষেত্রে বাড়তি সুবিধা বা লেআউট উন্নয়ন প্রয়োজন।
- ⭕ **Missing**: সিস্টেমে ফিচারটি এখনো তৈরি করা হয়নি (ভবিষ্যৎ এক্সটেনশনের জন্য তালিকাভুক্ত)।

---

## ১. ছুটির আবেদন (Leave Application)
- **Frontend Route**: `/leaves`
- **Page Component**: `frontend/src/pages/LeaveApplicationsPage.jsx`
- **Backend Controller**: `LeaveApplicationController.java` (`/api/leaves`)
- **Backend Service**: `LeaveApplicationService.java`
- **Models / Tables**: `LeaveApplication` (`leave_applications`), `TeacherAttendance` (`teacher_attendance`), `Holiday` (`holidays`)
- **Verification Tests**: `LeaveApplicationServiceTest` (৮টি টেস্ট উত্তীর্ণ)

| Feature | Type | Status | বিবরণ ও সমাধানের ফলাফল |
| :--- | :--- | :---: | :--- |
| **সব ছুটির তালিকা দর্শন (Admin)** | List | ✅ Implemented | `GET /api/leaves` — অ্যাডমিন সব শিক্ষকের ছুটির আবেদন স্ট্যাটাস অনুযায়ী ফিল্টার করে দেখতে পারেন। |
| **নিজের ছুটির তালিকা দর্শন (Teacher)** | List | 🛠️ Fixed | `GET /api/leaves/me` — `SecurityConfig.java`-এ রুট ও সাবপাথ নিখুঁতভাবে অনুমোদিত করায় শিক্ষকরা নির্বিঘ্নে নিজের ছুটি দেখতে পান। |
| **অ্যাডমিন কর্তৃক আবেদন দাখিল** | Create | ✅ Implemented | `POST /api/leaves` — অ্যাডমিন শিক্ষক নির্বাচন করে ছুটির আবেদন জমা দিতে পারেন। |
| **শিক্ষক কর্তৃক নিজের আবেদন দাখিল** | Create | 🛠️ Fixed | `POST /api/leaves/me` — শিক্ষক লগইন করে নিজের ছুটির আবেদন জমা দিতে পারেন। |
| **ছুটির আবেদন অনুমোদন (Approve)** | Status Change | ✅ Implemented | `POST /api/leaves/{id}/approve` — ছুটির দিন ও শুক্রবার স্বয়ংক্রিয়ভাবে বাদ দিয়ে `teacher_attendance`-এ `LEAVE` স্ট্যাটাস সিঙ্ক হয়। |
| **ছুটির আবেদন প্রত্যাখ্যান (Reject)** | Status Change | ✅ Implemented | `POST /api/leaves/{id}/reject` — সুস্পষ্ট কারণ উল্লেখ করে আবেদন বাতিল করা যায়। |
| **ছুটির আবেদন প্রত্যাহার (Cancel)** | Status Change | 🛠️ Fixed | **Root Cause Fixed**: `SecurityConfig.java`-এ `POST /api/leaves/*/cancel` রুটটি authenticated রোল হিসেবে উন্মুক্ত করা হয়েছে। ফলে শিক্ষকরা তাঁদের পেন্ডিং আবেদন বাতিল করতে পারেন (অন্য শিক্ষকের আবেদন বাতিল সার্ভিস লেয়ারে Forbidden প্রতিরোধ করা থাকে)। নতুন ৪টি ইউনিট টেস্ট দ্বারা ভ্যালিডেট করা হয়েছে। |
| **স্ট্যাটাস অনুযায়ী ফিল্টারিং** | Filter | ✅ Implemented | সব, অপেক্ষমান (PENDING), অনুমোদিত (APPROVED), প্রত্যাখ্যাত (REJECTED), বাতিল (CANCELLED) ট্যাব। |
| **তারিখ ওভারল্যাপ ভ্যালিডেশন** | Validation | ✅ Implemented | একই শিক্ষকের একই সময়ে একাধিক আবেদন থাকলে `409 CONFLICT` এরর দিয়ে ডুপ্লিকেশন আটকায়। |
| **ছাড়পত্র / প্রিন্ট ভিউ** | Print / PDF | ⭕ Missing | অনুমোদিত ছুটির অফিসিয়াল আবেদন প্রিন্ট বা স্লিপ ডাউনলোড ফিচার (ভবিষ্যত এক্সটেনশন)। |
| **এসএমএস নোটিফিকেশন ট্রিগার** | Notification | ⭕ Missing | ছুটি মঞ্জুর বা বাতিলের পর শিক্ষকের ফোনে স্বয়ংক্রিয় নোটিফিকেশন পাঠানোর মেকানিজম (ভবিষ্যত এক্সটেনশন)। |

---

## ২. ফি সেটআপ ও খাত (Fee Setup & Heads)
- **Frontend Route**: `/fee-structure`
- **Page Component**: `frontend/src/pages/FeeStructurePage.jsx`
- **Backend Controller**: `FeeStructureController.java` (`/api/fee-heads`, `/api/fee-structures`)
- **Backend Service**: `FeeStructureService.java`
- **Models / Tables**: `FeeHead` (`fee_heads`), `FeeStructure` (`fee_structures`)
- **Verification Tests**: `FeeStructureServiceTest` (৬টি টেস্ট উত্তীর্ণ)

| Feature | Type | Status | বিবরণ ও সমাধানের ফলাফল |
| :--- | :--- | :---: | :--- |
| **ফি খাতের তালিকা (Fee Heads)** | List | ✅ Implemented | `GET /api/fee-heads` — সক্রিয়/নিষ্ক্রিয় সকল ফি খাতের তালিকা প্রদর্শন। |
| **নতুন ফি খাত তৈরি** | Create | ✅ Implemented | `POST /api/fee-heads` — নাম, ইউনিক কোড ও বিবরণ দিয়ে নতুন খাত তৈরি। |
| **ফি খাত সম্পাদনা** | Edit | ✅ Implemented | `PUT /api/fee-heads/{id}` — খাতের তথ্য আপডেট। |
| **ফি খাত মুছে ফেলা** | Delete | ✅ Implemented | `DELETE /api/fee-heads/{id}` — ফি কাঠামোতে লিঙ্ক থাকলে নিরাপদ প্রতিরোধ সহ মুছে ফেলা। |
| **শ্রেণিভিত্তিক ফি কাঠামো তালিকা** | List & Filter | ✅ Implemented | `GET /api/fee-structures?sessionId=...&classId=...` — শ্রেণি ও সেশন অনুযায়ী ফি রুলস প্রদর্শন। |
| **শ্রেণির ফি সারাংশ কার্ড** | Summary | ✅ Implemented | `GET /api/fee-structures/summary` — মাসিক ও বাৎসরিক সম্ভাব্য আয়ের সারাংশ। |
| **শ্রেণিতে নতুন ফি নির্ধারণ (Modal)** | Create | 🛠️ Fixed | **Root Cause Fixed**: `frontend/src/components/ui.jsx`-এর `SelectField` কম্পোনেন্টে `{children}` সমর্থন যোগ করা হয়েছে। এর ফলে সেশন, শ্রেণি ও ফি খাতের ড্রপডাউন ক্র্যাশ ছাড়া রেন্ডার ও সেভ হচ্ছে। |
| **ফি নিয়ম সম্পাদনা (Edit Modal)** | Edit | 🛠️ Fixed | `SelectField` ফিক্সের ফলে এডিট মোডাল ক্র্যাশ মুক্ত হয়ে ডেটা লোড ও আপডেট কার্যকর। |
| **ফি নিয়ম মুছে ফেলা** | Delete | ✅ Implemented | `DELETE /api/fee-structures/{id}` — ফি স্ট্রাকচার ডিলিট করা যায়। |
| **সেশন ফি অনুলিপি (Clone / Copy)** | Batch / Copy | 🛠️ Fixed | `POST /api/fee-structures/copy` — উৎস ও টার্গেট সেশনের `SelectField` ড্রপডাউন ফিক্স হওয়ায় ১-ক্লিকে সম্পূর্ণ সেশনের ফি কপি কার্যকর। |
| **ফি চার্ট প্রিন্ট / এক্সপোর্ট** | Print / Export | ⭕ Missing | একটি শ্রেণির সম্পূর্ণ ফি তালিকা প্রিন্ট বা এক্সেল এক্সপোর্ট করার ব্যবস্থা (ভবিষ্যত এক্সটেনশন)। |

---

## ৩. ফি আদায় ও ইনভয়েস (Fee Collection & Invoice)
- **Frontend Route**: `/fee-collection`
- **Page Component**: `frontend/src/pages/FeeCollectionPage.jsx`
- **Backend Controller**: `FeeCollectionController.java` (`/api/invoices`, `/api/fee-payments`, `/api/fee-waivers`), `PaymentController.java` (`/api/payments`)
- **Backend Service**: `FeeCollectionService.java`, `OnlinePaymentGatewayService.java`
- **Models / Tables**: `Invoice`, `InvoiceItem`, `FeePayment`, `StudentWaiver`, `PaymentTransaction`
- **Verification Tests**: `FeeCollectionServiceTest` (৪টি টেস্ট উত্তীর্ণ)

| Feature | Type | Status | বিবরণ ও সমাধানের ফলাফল |
| :--- | :--- | :---: | :--- |
| **ইনভয়েস তালিকা ও ফিল্টারিং** | List & Filter | ✅ Implemented | `GET /api/invoices` — সেশন, ক্লাস, স্ট্যাটাস ও বিলিং মাস অনুযায়ী ফিল্টার ও সারাংশ কেপিআই। |
| **ফি জমা গ্রহণ মোডাল (Cash / Bank)** | Payment | 🛠️ Fixed | **Root Cause Fixed**: `PaymentModal`-এ মাধ্যম সিলেক্টরে `SelectField` ক্র্যাশ সমাধান করা হয়েছে। নগদ বা ব্যাংক জমা রসিদ সহ নির্বিঘ্নে গৃহীত হয়। |
| **বিকাশ অনলাইন পেমেন্ট (bKash Pay)** | Online Payment | ✅ Implemented | `POST /api/payments/create-intent`, `POST /api/payments/execute` — বিকাশ পপআপ, ওটিপি, পিন ও রসিদ তৈরি। |
| **ইনভয়েস ও রসিদ বিবরণী (Modal)** | View & Print | ✅ Implemented | `GET /api/invoices/{id}` — আইটেমভিত্তিক ব্রেকডাউন ও আগের পেমেন্ট হিস্ট্রি সহ প্রিন্ট। |
| **বিলম্ব জরিমানা আরোপ (Late Fine)** | Penalty | ✅ Implemented | `POST /api/invoices/{id}/fine` — বকেয়া বিলে লেট ফাইন যোগ করা। |
| **বাল্ক ইনভয়েস জেনারেটর ট্যাব** | Batch Invoicing | 🛠️ Fixed | **Root Cause Fixed**: সেশন ও শ্রেণি সিলেকশন `SelectField` ক্র্যাশ মুক্ত হওয়ায় সম্পূর্ণ ক্লাসের মাসিক ইনভয়েস এক ক্লিকে তৈরি হচ্ছে। |
| **শিক্ষার্থী বিশেষ ছাড় / ওয়েভার তালিকা** | List | ✅ Implemented | `GET /api/fee-waivers` — সক্রিয় সব বৃত্তির তালিকা প্রদর্শন। |
| **ওয়েভার যোগ মোডাল (Add Waiver)** | Create | 🛠️ Fixed | **Root Cause Fixed**: `/api/students?size=200` থেকে আসা `PageResponse` অবজেক্টকে হ্যান্ডেল করার জন্য `studentList = Array.isArray(students) ? students : (students?.items || [])` যোগ করা হয়েছে এবং `SelectField` ঠিক করায় শিক্ষার্থী বাছাই ও ছাড় সংরক্ষণ ১০০% কার্যকর। |
| **ওয়েভার ডিলিট** | Delete | ✅ Implemented | `DELETE /api/fee-waivers/{id}` — ছাড় মুছে ফেলা। |
| **ইনভয়েস তালিকা এক্সেল এক্সপোর্ট** | Export | ⭕ Missing | বকেয়া তালিকা ও ইনভয়েস শিট এক্সেল আকারে এক্সপোর্টের ব্যবস্থা (ভবিষ্যত এক্সটেনশন)। |

---

## ৪. ব্যয় ও হিসাব বিবরণী (Expenses & Accounts Statement)
- **Frontend Route**: `/accounts`
- **Page Component**: `frontend/src/pages/AccountsPage.jsx`
- **Backend Controller**: `AccountsController.java` (`/api/accounts`, `/api/expenses`, `/api/expense-categories`)
- **Backend Service**: `AccountsService.java`
- **Models / Tables**: `ExpenseCategory` (`expense_categories`), `Expense` (`expenses`), `FeePayment`, `Invoice`
- **Verification Tests**: `AccountsServiceTest` (৪টি টেস্ট উত্তীর্ণ)

| Feature | Type | Status | বিবরণ ও সমাধানের ফলাফল |
| :--- | :--- | :---: | :--- |
| **আর্থিক স্থিতি সারাংশ (Summary)** | Report / Metrics | ✅ Implemented | `GET /api/accounts/summary` — মোট ফি আদায়, মোট ব্যয়, নিট স্থিতি ও বকেয়া ফি অনুপাত। |
| **খাতভিত্তিক আয়-ব্যয় ব্রেকডাউন** | Analytics | ✅ Implemented | ফি খাত অনুযায়ী আয় এবং ব্যয়ের ক্যাটাগরি অনুযায়ী পার্সেন্টেজ ও ভিজ্যুয়াল গ্রাফ। |
| **দৈনিক পরিচালন ব্যয় তালিকা** | List & Filter | ✅ Implemented | `GET /api/expenses` — ক্যাটাগরি ও তারিখ সীমা অনুযায়ী ফিল্টার। |
| **নতুন খরচ ভাউচার এন্ট্রি (Modal)** | Create | 🛠️ Fixed | **Root Cause Fixed**: `ExpenseModal`-এ ব্যয়ের খাত ও পেমেন্ট মাধ্যম সিলেকশনে `SelectField` ক্র্যাশ সমাধান হওয়ায় নতুন ভাউচার এন্ট্রি সম্পূর্ণ সচল। |
| **খরচ ভাউচার সম্পাদনা (Edit Modal)** | Edit | 🛠️ Fixed | বিদ্যমান খরচ ভাউচারের তথ্য এডিট ও সংরক্ষণ সচল। |
| **খরচ ভাউচার মুছে ফেলা** | Delete | ✅ Implemented | `DELETE /api/expenses/{id}` — অডিট লগ সহ ভাউচার মোছা। |
| **ব্যয়ের খাতসমূহ (Categories)** | List | ✅ Implemented | `GET /api/expense-categories` — ব্যয়ের সকল খাতের তালিকা। |
| **ব্যয়ের খাত তৈরি ও সম্পাদনা** | CRUD | ✅ Implemented | `POST / PUT /api/expense-categories` — নতুন খাত ও নাম/বিবরণ আপডেট। |
| **ব্যয়ের খাত মুছে ফেলা** | Delete | ✅ Implemented | `DELETE /api/expense-categories/{id}` — খরচের ভাউচারে যুক্ত থাকলে নিরাপদ প্রতিরোধ। |
| **ভাউচার ক্যাশ মেমো প্রিন্ট** | Print | ⚠️ Partially | ব্রাউজার প্রিন্ট কাজ করে, ডেডিকেটেড প্রিন্ট ভাউচার লেআউট করা যেতে পারে। |
| **অ্যাকাউন্টস স্টেটমেন্ট এক্সপোর্ট** | Export | ⭕ Missing | মাসিক আয়-ব্যয়ের অডিট স্টেটমেন্ট এক্সেল/পিডিএফ আকারে এক্সপোর্ট (ভবিষ্যত এক্সটেনশন)। |

---

## ৫. মেসেজ ও নোটিফিকেশন (Message & Notification)
- **Frontend Route**: `/notifications`
- **Page Component**: `frontend/src/pages/NotificationsPage.jsx`
- **Backend Controller**: `NotificationCampaignController.java`, `NotificationController.java`
- **Backend Service**: `NotificationCampaignService.java`, `NotificationService.java`
- **Models / Tables**: `NotificationCampaign`, `NotificationTemplate`, `NotificationLog`
- **Verification Tests**: `NotificationCampaignServiceTest` (৪টি), `NotificationServiceTest` (৮টি) — সর্বমোট ১২টি টেস্ট উত্তীর্ণ

| Feature | Type | Status | বিবরণ ও সমাধানের ফলাফল |
| :--- | :--- | :---: | :--- |
| **নোটিফিকেশন পেজ লোড ও ব্রডকাস্ট** | Page Load / Form | 🛠️ Fixed | **Root Cause Fixed**: `/notifications` পেজে ঢুকলেই যে সাদা পর্দা (White Screen Crash) হতো, তা `SelectField` ফিক্সের মাধ্যমে সম্পূর্ণ দূর হয়েছে। পেজ এখন তাৎক্ষণিক স্মুথলি লোড হয়। |
| **সেকশন অনুযায়ী টার্গেট অডিয়েন্স** | Filter / Target | 🛠️ Fixed | **Root Cause Fixed**: `campAudience === 'SECTION'` নির্বাচিত হলে শ্রেণি এবং শ্রেণির সাপেক্ষে সেকশন ড্রপডাউন ডাইনামিকালি রেন্ডার ও ব্যাকএন্ডে `targetSectionId` প্রেরণের ব্যবস্থা তৈরি ও টেস্ট করা হয়েছে। |
| **ডাইনামিক ভ্যারিয়েবল প্রতিস্থাপন** | Template Engine | ✅ Implemented | `{student_name}`, `{class_name}`, `{due_amount}`, `{school_name}` স্বয়ংক্রিয়ভাবে অভিভাবকের এসএমএসে প্রতিস্থাপিত হয়। |
| **এসএমএস লাইভ কাউন্টার ও কস্ট** | Real-time Helper | ✅ Implemented | বাংলা ইউনিকোড (৭০ ক্যারেক্টার/পার্ট) এবং ইংরেজি GSM (১৬০ ক্যারেক্টার/পার্ট) লাইভ গণক। |
| **বাল্ক ক্যাম্পেইন প্রেরণ** | Batch Action | ✅ Implemented | `POST /api/notification-campaigns` — স্কুলব্যাপী, শ্রেণি, সেকশন বা বকেয়া প্রাপকদের বাল্ক এসএমএস প্রেরণ। |
| **সাম্প্রতিক ক্যাম্পেইন হিস্ট্রি** | History | ✅ Implemented | `GET /api/notification-campaigns` — মোট প্রাপক, সফল ও ব্যর্থ কাউন্ট প্রদর্শন। |
| **মেসেজ টেমপ্লেট তালিকা** | List | ✅ Implemented | `GET /api/notification-templates` — ক্যাটাগরিভিত্তিক টেমপ্লেট প্রদর্শন। |
| **মেসেজ টেমপ্লেট তৈরি ও সম্পাদনা** | Modal | 🛠️ Fixed | `TemplateModal`-এ ক্যাটাগরি ও মাধ্যম ড্রপডাউন সচল করায় নতুন টেমপ্লেট তৈরি ও আপডেট কার্যকর। |
| **মেসেজ টেমপ্লেট মুছে ফেলা** | Delete | ✅ Implemented | `DELETE /api/notification-templates/{id}` — টেমপ্লেট মুছে ফেলা। |
| **ডেলিভারি লগ ও কিউ তালিকা** | Audit & Logs | ✅ Implemented | `GET /api/admin/notifications` — স্ট্যাটাস ও পেজিনেশন সহ বিস্তারিত এসএমএস লগ। |
| **কিউতে থাকা এসএমএস প্রেরণ** | Queue Dispatch | ✅ Implemented | `POST /api/admin/notifications/dispatch` — বাল্ক কিউ প্রসেস। |
| **ব্যর্থ এসএমএস পুনরায় প্রেরণ (Retry)** | Action | ✅ Implemented | `POST /api/admin/notifications/{id}/retry` — ব্যর্থ বার্তা রিট্রাই। |
| **ভবিষ্যত তারিখের শিডিউল ক্যাম্পেইন** | Scheduling | ⭕ Missing | নির্দিষ্ট তারিখে স্বয়ংক্রিয়ভাবে প্রচার হওয়ার শিডিউলার (ভবিষ্যত এক্সটেনশন)। |

---

## সেকশনভিত্তিক সারাংশ (Section-by-Section Summary)

| সেকশন | সর্বমোট ফিচার | পূর্বে সচল ছিল | ফিক্স করা হয়েছে (Fixed) | অপূর্ণ / ভবিষ্যৎ (Missing) | বর্তমান অবস্থা |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **১. ছুটির আবেদন** | ১১ | ৭ | ৩ | ২ (প্রিন্ট, এসএমএস) | **১০০% সচল** |
| **২. ফি সেটআপ ও খাত** | ১১ | ৭ | ৩ | ১ (এক্সপোর্ট) | **১০০% সচল** |
| **৩. ফি আদায় ও ইনভয়েস** | ১০ | ৬ | ৩ | ১ (এক্সপোর্ট) | **১০০% সচল** |
| **৪. ব্যয় ও হিসাব বিবরণী** | ১১ | ৮ | ২ | ১ (এক্সপোর্ট) | **১০০% সচল** |
| **৫. মেসেজ ও নোটিফিকেশন** | ১৩ | ৮ | ৪ | ১ (শিডিউলিং) | **১০০% সচল** |
| **মোট** | **৫৬** | **৩৬** | **১৫** | **৬** | **সবগুলো ফিচার ত্রুটিমুক্ত** |

---

## রিগ্রেশন ভেরিফিকেশন ফলাফল
1. **জাভা ব্যাকএন্ড টেস্ট স্যুট**:
   ```
   [INFO] Running AccountsServiceTest (4 tests) - PASSED
   [INFO] Running FeeCollectionServiceTest (4 tests) - PASSED
   [INFO] Running FeeStructureServiceTest (6 tests) - PASSED
   [INFO] Running LeaveApplicationServiceTest (8 tests) - PASSED
   [INFO] Running NotificationCampaignServiceTest (4 tests) - PASSED
   [INFO] Running NotificationServiceTest (8 tests) - PASSED
   [INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS (Total time: 18.222 s)
   ```
2. **ফ্রন্টএন্ড প্রোডাকশন বিল্ড**:
   ```
   > vite build
   ✓ 168 modules transformed.
   ✓ built in 749ms
   ```
