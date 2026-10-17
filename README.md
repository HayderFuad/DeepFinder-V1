# DeepFinder Android (محرك البحث الذكي الشامل للهاتف)
**الإصدار:** v1.0 (Official Production Release)  
**فريق العمل:** المهندس حيدر فؤاد (eng. Hayder Fuad) والمهندس ابراهيم احمد محمد حاتم (eng. Ebraheem Ahmed)  

---

## 🌟 نظرة عامة (Overview)
**DeepFinder** هو تطبيق أندرويد محلي فائق السرعة والأمان، يتيح البحث الفوري في الهاتف الذكي عن أي نص أو مستند من مستويين:
1. **تطابق الأسماء (Metadata):** أي ملف أو مجلد يحتوي اسمه على كلمة البحث.
2. **تطابق المحتوى العميق (Deep Content Match):** البحث داخل نصوص المستندات (`PDF, Word/DOCX, Excel/XLSX, TXT, MD, JSON, CSV, XML, LOG, Code`) وعرض مقتطف (Snippet) من الجملة مع تظليل الكلمات المطابقة.
3. **محرك تطبيع اللغة العربية (Arabic Normalizer):** توحيد الهمزات (أ، إ، آ -> ا)، التاء المربوطة (ة -> ه)، الألف المقصورة (ى -> ي)، وإزالة التشكيل (الحركات) والمد (Tatweel) لضمان العثور على الأسماء العربية بدقة 100%.
4. **أمان وخصوصية 100% (Zero-Internet):** التطبيق أوفلاين تماماً ولا يطلب إذن الإنترنت في المانيفست.
5. **قفل التطبيق بالبصمة (Biometric Lock):** حماية نتائج البحث الحساسة ببصمة الإصبع أو رمز قفل الشاشة.

---

## 🛠️ متطلبات التشغيل والبناء (Prerequisites)
- **نظام التشغيل:** Windows 10/11 أو macOS أو Linux.
- **بيئة التطوير:** Android Studio Ladybug / Koala أو أحدث (2024.1+).
- **إصدار جافا (JDK):** **Java 17 (OpenJDK 17 / Eclipse Temurin 17)** وهو الإلزامي لأحدث إصدارات Gradle و Android Gradle Plugin.
- **إصدار أندرويد المدعوم:** من Android 7.0 (API 24) حتى Android 15+ (API 35/36).

---

## 🚀 خطوات فتح المشروع وتشغيله في Android Studio
1. **فك الضغط:**
   - قم بفك ضغط الملف `DeepFinder_Android_Project.zip` إلى مجلد على جهازك (مثلاً: `C:\Projects\DeepFinder`).
2. **فتح المشروع:**
   - افتح **Android Studio**.
   - اختر **Open** ثم حدد المجلد الرئيسي للمشروع (المجلد الذي يحتوي على `settings.gradle.kts`).
3. **ضبط إصدار JDK 17:**
   - من شريط القوائم اذهب إلى:
     `File` -> `Settings` (أو `Android Studio` -> `Settings` في الماك)
     -> `Build, Execution, Deployment` -> `Build Tools` -> `Gradle`.
   - في خانة **Gradle JDK**، اختر **JDK 17**.
4. **مزامنة المشروع (Gradle Sync):**
   - اضغط على أيقونة **Sync Project with Gradle Files** (أيقونة الفيل الأزرق بالأعلى) وانتظر حتى ينتهي التحميل.

---

## 📱 بناء الـ APK وتشغيله (Build APK)
### الطريقة الأولى: من خلال Android Studio
1. من القائمة العلوية: `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`.
2. بعد اكتمال البناء ستظهر رسالة بالأسفل مع رابط `locate`، اضغط عليه للوصول مباشرة إلى ملف `app-debug.apk`.
3. لتشغيل التطبيق على هاتفك: قم بتفعيل **تصحيح أخطاء USB (USB Debugging)** على الهاتف ثم اضغط زر **Run (السهم الأخضر)**.

### الطريقة الثانية: عبر سطر الأوامر (Terminal)
- **على أنظمة Linux / macOS:**
  ```bash
  chmod +x gradlew
  ./gradlew assembleDebug
  ```
- **على أنظمة Windows (PowerShell أو CMD):**
  ```cmd
  gradlew.bat assembleDebug
  ```
- مسار الـ APK الناتج:
  `app/build/outputs/apk/debug/app-debug.apk`

---

## 📋 صلاحية الوصول للملفات في أندرويد (All Files Access)
- في أول تشغيل، سيطلب التطبيق إذن الوصول لجميع الملفات عبر واجهة مخصصة.
- عند الضغط على **"منح الصلاحية الآن"**، ستفتح صفحة إعدادات النظام لتفعيل خيار **"السماح بالوصول لإدارة جميع الملفات" (MANAGE_EXTERNAL_STORAGE)** لتمكين محرك البحث من قراءة ملفات الـ PDF والـ Word والـ Excel الموجودة على الهاتف.

---

## 📞 قنوات التواصل الرسمية (Contact Information)
- **المهندسون:** eng. Hayder Fuad & eng. Ebraheem Ahmed
- **الهواتف:** `+967776797221` , `+967734414267`
- **البريد الإلكتروني:** `asydalwr@gmail.com`
- **LinkedIn:** `www.linkedin.com/in/hayderf` (https://www.linkedin.com/in/hayderf)
- **Instagram:** `@riv.m4` (https://instagram.com/riv.m4)
- **Telegram:** `@riv_m4` (https://t.me/riv_m4)
- **Facebook:** `@riv.m4` (https://facebook.com/riv.m4)
- **GitHub:** `@HayderFuad` (https://github.com/HayderFuad)
