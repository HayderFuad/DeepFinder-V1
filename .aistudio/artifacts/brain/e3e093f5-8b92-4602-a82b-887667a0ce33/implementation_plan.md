# DeepFinder Android — محرك البحث الفوري الشامل وتحديث قنوات التواصل

تهدف هذه التحديثات إلى تعزيز شاشة البحث الرئيسية بشريط بحث دائم ومثبت بالأعلى مع فلاتر لحظية وقائمة نتائج كسولة (Lazy List) فائقة السرعة، إلى جانب دمج رابط حساب LinkedIn الرسمي ضمن نافذة التواصل مع المهندسين وتجهيز الحزمة الكاملة للمشروع.

### User Review & Critical Decisions

> [!IMPORTANT]
> تم اعتماد القرارات التصميمية والهندسية التالية بناءً على اختياراتك المؤكدة:

- **تصميم شريط البحث الدائم (Confirmed Decision 1)**: تثبيت شريط البحث الدائم في أعلى الشاشة الرئيسية (Sticky Top Header) مع شريط الفلاتر الأفقي وشريط الإحصائيات اللحظية ليبقى مرئياً أثناء التمرير في قائمة النتائج.
- **أسلوب عرض بطاقات النتائج (Confirmed Decision 2)**: عرض المقتطف النصي (Content Snippet) مباشرة داخل البطاقة مع تظليل الكلمات المطابقة وأزرار الإجراءات السريعة (فتح، مشاركة، نسخ المسار/المقتطف).
- **قسم تواصل معنا وحساب LinkedIn (Confirmed Decision 3)**: عرض نافذة منبثقة احترافية (Modal Dialog) من الشريط العلوي تضم بيانات المهندسين (`eng. Hayder Fuad & eng. Ebraheem Ahmed`) وكافة الحسابات مع إضافة زر تفاعلي مباشر لحساب **LinkedIn** (`www.linkedin.com/in/hayderf`).

---

### 1. Overview & Core Concept

- **What It Does**: يوفر تجربة بحث لحظية (Real-time Instant Search) في أسماء الملفات والمجلدات ومحتواها الداخلي (PDF, Word, Excel, النصوص، والبرمجيات) مع تطبيع كامل للغة العربية، مدعوماً بنافذة تواصل متكاملة مع فريق التطوير.
- **Target Audience / Persona**: المستخدمون والمطورون والمهندسون الباحثون عن أداة بحث محلية سريعة وآمنة تحترم الخصوصية وتعمل بدون اتصال بالإنترنت (Zero-Internet Permission).
- **Key Value**: الوصول الفوري لأي معلومة داخل آلاف المستندات في أجزاء من الثانية مع واجهة عربية (RTL) عصرية تدعم الوضعين الداكن والفاتح.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **البحث اللحظي والفلترة**: يكتب المستخدم نص البحث في الشريط العلوي الثابت، فيقوم المحرك بتطبيع النص العربي وعرض النتائج فوراً في قائمة `LazyColumn` مع إمكانية التصفية السريعة (الكل، تطابق الاسم، تطابق المحتوى، PDF، Word/Excel، نصوص، مجلدات).
  2. **معاينة المقتطف والتفاعل**: تعرض كل بطاقة اسم الملف، مساره، حجمه، وتاريخ تعديله، بالإضافة إلى صندوق المقتطف النصي المظلل وأزرار الفتح والمشاركة والنسخ.
  3. **التواصل مع فريق التطوير**: الضغط على أيقونة "تواصل معنا" في الشريط العلوي يفتح نافذة منبثقة تعرض بيانات المهندسين وأزرار الاتصال المباشر (الهواتف، البريد الإلكتروني، LinkedIn، GitHub، Telegram، Instagram، Facebook).
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: تصميم تقني عصري (Modern Deep Tech) يجمع بين الدقة الهندسية والوضوح البصري مع دعم كامل للاتجاه من اليمين إلى اليسار (RTL).
  - *Color Palette & Mood*:
    - الوضع الداكن (`DarkTheme`): خلفية كحلية عميقة (`#0B132B`)، أسطح بطاقات (`#16223F`)، لون أساسي سماوي مشرق (`#38BDF8`)، ولون تمييز ذهبي (`#FBBF24`) لتظليل الكلمات المطابقة.
    - الوضع الفاتح (`LightTheme`): خلفية نقية (`#F8FAFC`)، أسطح بيضاء (`#FFFFFF`)، لون أساسي أزرق محيطي (`#0284C7`)، ولون تمييز كهرماني (`#D97706`).
  - *Typography & Hierarchy*: تباين واضح بين عناوين الملفات العريضة، المسارات المختصرة، ونصوص المقتطفات ذات المسافات المريحة للقراءة، مع الالتزام بمعايير اللمس القياسية (`48.dp`).
- **Interactive Feedback & Motion**:
  - مؤشر تقدم ناعم للفهرسة يختفي تلقائياً عند الاكتمال (`AnimatedVisibility`).
  - تفاعلات لمس فورية (Material Ripples) وإشعارات سريعة (Toast) عند نسخ المسار أو المقتطف.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: شريط بحث دائم مع تفاعل لحظي (Debounced Reactive Search)**
  - *Chosen Approach*: استخدام `StateFlow` مع `debounce(250ms)` ودمج حالة الفلتر النشط مع استعلامات قاعدة البيانات المحلية.
  - *Why*: يمنع التقطيع أثناء الكتابة السريعة ويحافظ على سلاسة الواجهة بمعدل 60 إطاراً في الثانية.
  - *Alternatives Considered*: البحث عند الضغط على زر الإدخال فقط، وتم استبعاده لأنه يقلل من تفاعلية وسرعة التجربة.
- **Decision 2: الفهرسة المحلية الكاملة بدون إنترنت (100% Offline Room Persistence)**
  - *Chosen Approach*: تخزين الفهرس النصي المطبّع في قاعدة بيانات `Room` محلية مع عدم طلب صلاحية الإنترنت.
  - *Why*: يضمن الخصوصية المطلقة لملفات المستخدم الشخصية والتوافق التام مع متطلبات الأمان وسياسات Google Play.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        MainScreen (RTL Compose UI)                     │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ Sticky Top Header: AppBar + Persistent SearchBar + FilterChips   │  │
│  └───────────────┬──────────────────────────────────┬───────────────┘  │
│                  │ Query & Filter Events            │ Dialog Triggers  │
│                  ▼                                  ▼                  │
│  ┌───────────────────────────────┐  ┌───────────────────────────────┐  │
│  │ Real-Time LazyColumn Results  │  │ ContactDialog & SettingsModal │  │
│  │  • FileResultCard             │  │  • Founders Info              │  │
│  │  • Highlighted Snippet Box    │  │  • Direct Intents (Phone,     │  │
│  │  • Quick Actions (Open/Share/ │  │    Email, LinkedIn, GitHub,   │  │
│  │    Copy Path & Snippet)       │  │    Telegram, Instagram, FB)   │  │
│  └───────────────▲───────────────┘  └───────────────────────────────┘  │
└──────────────────┼─────────────────────────────────────────────────────┘
                   │ Reactive StateFlow<List<SearchResult>>
┌──────────────────┴─────────────────────────────────────────────────────┐
│                             MainViewModel                              │
│  • Debounced Query Stream + Filter Combination                         │
│  • Theme Mode & Biometric Lock State Management                        │
└──────────────────┬─────────────────────────────────────────────────────┘
                   │ Suspend / Flow Queries
┌──────────────────▼─────────────────────────────────────────────────────┐
│               SearchRepository & ArabicNormalizer Engine               │
│  • Unifies Alef, Ta Marbuta, Yaa & Strips Tashkeel / Tatweel           │
│  • Extracts Context Window Snippets around Matched Terms               │
└──────────────────┬─────────────────────────────────────────────────────┘
                   │
┌──────────────────▼─────────────────────────────────────────────────────┐
│            Room Database (FileDao) & On-Device DocumentParser          │
│  • IndexedFile Entity (Metadata + Normalized Content)                  │
│  • PDFBox-Android + Lightweight Zip/XML Parsers (DOCX, XLSX, TXT)      │
└────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - كيان `IndexedFile`: يحفظ بيانات الملف (الاسم، المسار، الامتداد، الحجم، تاريخ التعديل، الاسم المطبّع، والمحتوى النصي المطبّع).
  - نموذج `SearchResult`: يربط الملف المفهرس بنوع التطابق (`NAME_MATCH`, `CONTENT_MATCH`, `BOTH`) والمقتطف النصي المستخرج والكلمة المفتاحية لتظليلها.
- **Interactive Component & State Mapping**:
  - **شريط البحث الثابت**: يحدّث `searchQuery` في `MainViewModel` فورياً، مع زر مسح النص بضغطة واحدة وإحصائية فورية لعدد النتائج المطابقة مقابل إجمالي الملفات المفهرسة.
  - **قائمة النتائج الكسولة (`LazyColumn`)**: تعرض عناصر `FileResultCard` بمفاتيح فريدة (`file.id`) مع دعم التمرير السلس، وأزرار تفاعلية لفتح الملف عبر `FileProvider`، مشاركته، أو نسخ مساره ومقتطفه.
  - **نافذة التواصل (`ContactDialog`)**: تتضمن كافة بيانات المهندسين (`eng. Hayder Fuad & eng. Ebraheem Ahmed`) مع إضافة بطاقة تفاعلية جديدة لحساب **LinkedIn** (`https://www.linkedin.com/in/hayderf`) تفتح الرابط مباشرة عبر `Intent.ACTION_VIEW`.
  - **التجميع والحزمة المضغوطة**: بعد التحقق من بناء التطبيق بنجاح، يتم تحديث ملف `DeepFinder_Android_Project.zip` ليضم كامل ملفات المشروع المحدثة.
