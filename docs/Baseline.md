# Project Baseline

## Repository Information

- **Repository:** LOCAL CONTACT (https://github.com/AWNO-1/LOCAL-CONTACT-)
- **Baseline Commit:** `71111825645c2b3c6a926f739455c4ffda3c0963`
- **Commit Message:** `feat: comprehensive audit fixes (logic, audio mixer, UX, privacy, theme, and PiP call overhaul)`
- **Current Branch:** `HEAD (detached)`
- **Baseline Date:** 2026-09-11
- **Working Tree Status:** Clean (Verified via `git status`)

---

## Baseline Verification Limitations

> [!IMPORTANT]
> **محددات وضوابط التحقق من الـ Baseline الحالي:**
> 1. **لم يتم اختبار التطبيق على جهازين حقيقيين:** التقييم الحالي يستند إلى الفحص المكتبي والتحليلي للكود المصدري (Static Code Analysis).
> 2. **لم يتم إثبات نجاح الاكتشاف (Peer Discovery) عملياً:** لا يمكن الجزم بعمل الاكتشاف في بيئات الشبكات الحقيقية (مثل شبكات الراوتر التي تفعل عزل العملاء AP Isolation أو هواتف أندرويد التي تقيد حزم الـ Multicast).
> 3. **لم يتم إثبات نجاح الرسائل، المكالمات، ونقل الملفات عملياً:** وظائف تدفق الصوت عبر UDP ومكالمات الفيديو ونقل الملفات عبر TCP لم تُختبر في ظروف شبكة واقعية (Latency, Jitter, Packet Loss).
> 4. **لم يتم قياس نسبة تغطية فعلية (Code Coverage %):** لا توجد تقارير تغطية آلية (مثل JaCoCo) لقياس مسارات الكود المختبرة.
> 5. **نتائج الوظائف مبنية على فحص الكود فقط:** تصنيف أي ميزة كـ "Implemented" يعني أن هيكلها البرمجي موجود في الكود المصدري، ولكنه يظل غير مثبت تشغيلياً حتى يتم اختباره على أجهزة فعلية.

---

## Build Environment

- **IDE / Environment:** Android Studio 2025.2.x (installed at `C:\Program Files\Android\Android Studio`) / Antigravity IDE
- **Gradle Version:** `9.4.1` (declared in `gradle/wrapper/gradle-wrapper.properties`)
- **Android Gradle Plugin (AGP):** `9.1.1` (declared in `gradle/libs.versions.toml`)
- **Kotlin Version:** `2.2.10`
- **Compose Compiler / BOM:** `2024.09.00`
- **compileSdk:** `36` (Android 16 / Vanilla Ice Cream extension)
- **minSdk:** `24` (Android 7.0 Nougat)
- **targetSdk:** `36`
- **Java / JDK:** OpenJDK 21.0.8 (bundled JetBrains Runtime at `C:\Program Files\Android\Android Studio\jbr`) & Oracle JDK 22.0.1 (installed at `C:\Program Files\Java\jdk-22.0.1`)
- **Android SDK Path:** `H:\Sdk` (Build-Tools `36.1.0` & `35.0.0`, Platform `android-36`)

---

## Project Structure

المشروع عبارة عن تطبيق أندرويد أحادي الموديول (`:app`) مبني بلغة Kotlin وواجهات Jetpack Compose بالكامل.

### 1. Modules & Configuration
- **Root Project (`/`):**
  - `build.gradle.kts`: ملف إعداد البناء الجذري وتعريف الإضافات المشتركة.
  - `settings.gradle.kts`: تعريف موديول `:app` ومستودعات التبعيات (Google Maven, Maven Central).
  - `gradle.properties`: إعدادات الذاكرة للـ JVM وميزات AndroidX و KSP.
  - `local.properties`: مسار الـ Android SDK المحلي (`sdk.dir=H\:\\Sdk`).
  - `.env.example` / `.env`: متغيرات البيئة (مثل `GEMINI_API_KEY`) لقراءة الأسرار عبر `secrets-gradle-plugin`.
- **App Module (`/app`):**
  - `app/build.gradle.kts`: تبعيات التطبيق، إعدادات KSP لـ Room و Moshi، و task مخصصة لتصحيح الأرقام الهندية-العربية (`NormalizeGeneratedDigitsTask`).
  - `app/src/main/AndroidManifest.xml`: أذونات الشبكة، الصوت، الكاميرا، والخدمات الأمامية (`ScreenCaptureService`).

### 2. Package Hierarchy (`com.example`)
- **`com.example` (الجذر):**
  - `MainActivity.kt`: نقطة الدخول الرئيسية للشاشة وإدارة دورة الحياة والتنقل وتصاريح النظام.
- **`com.example.network` (طبقة الاتصال والمحركات الشبكية):**
  - `LocalP2PEngine.kt`: المحرك المركزي الضخم (2,496 سطر) لإدارة اتصالات P2P، واكتشاف الأقران (Discovery)، والمكالمات، والرسائل.
  - `LocalCryptoEngine.kt`: محرك التشفير المركزي باستخدام خوارزمية AES-256-GCM واشتقاق المفاتيح عبر PBKDF2.
  - `AudioEngine.kt`: محرك التقاط وبث وتشغيل الصوت ومزج المسارات الصوتية (AudioRecord/AudioTrack/UDP:8889).
  - `VideoEngine.kt`: محرك بث الفيديو عبر CameraX وضغط الإطارات بصيغة JPEG ونقلها عبر UDP:8890.
  - `FileTransferEngine.kt`: محرك نقل الملفات المقسمة والمشفرة عبر TCP Socket:8891 مع دعم الاستئناف (Resume).
  - `NetworkServiceDiscoveryEngine.kt`: محرك اكتشاف الخدمات عبر Android NSD (mDNS).
  - `NetworkUtils.kt`: دوال فحص الشبكة، واجهات Hotspot و Wi-Fi، عناوين البث، واستبعاد واجهات البيانات الخلوية.
  - `CallToneManager.kt`: إدارة نغمات الرنين، الاتصال، والاهتزاز.
  - `ScreenCaptureService.kt`: خدمة أمامية (Foreground Service) لمشاركة الشاشة عبر `MediaProjection`.
- **`com.example.data` (البيانات وقواعد البيانات المحلية):**
  - `data/local/AppDatabase.kt`: قاعدة بيانات Room (الإصدار 6) وتتضمن `ChatDao` وجداول الرسائل، الغرف، والأقران المحظورين والمفضلين.
  - `data/ThemePreferencesRepository.kt`: تخزين تفضيلات المظهر والألوان عبر Jetpack DataStore Preferences.
- **`com.example.model` (النماذج وهياكل البيانات):**
  - `Models.kt`: تعريف فئات البيانات (`ChatMessage`, `Peer`, `ActiveCall`, `ActiveGroupCall`, `RoomInfo`, `UserProfile`, `MessageType`).
- **`com.example.ui` (واجهات المستخدم وإدارة الحالة):**
  - `MainViewModel.kt`: الـ ViewModel المركزي (1,839 سطر) للربط بين الواجهات والمحركات وقاعدة البيانات.
  - `ui/screens/`: شاشات المحادثة (`ChatScreen.kt`)، الأقران (`PeersScreen.kt`)، الغرف (`RoomsScreen.kt`)، تسجيل الدخول/الملف الشخصي (`AuthScreen.kt`)، ودليل الشبكة (`NetworkGuideScreen.kt`).
  - `ui/dialogs/`: حوارات المكالمات الفردية (`CallScreenDialog.kt`)، مكالمات الغرف (`GroupCallScreenDialog.kt`)، والحوارات العامة (`Dialogs.kt`).
  - `ui/components/`: المكونات التفاعلية (فقاعات الرسائل، مؤشر الكتابة، عارض الصور، شريط حالة الشبكة، مراقب الإشارة، عارض موجات الصوت).
  - `ui/theme/`: إعدادات المظهر، الألوان (`Color.kt`)، التيبوغرافيا (`Type.kt`)، والأيقونات (`AppIcons.kt`).
- **`com.example.audio` (تسجيل وتشغيل الملاحظات الصوتية):**
  - `VoiceNoteRecorder.kt`: تسجيل الملاحظات الصوتية بصيغة AAC/M4A.
  - `VoiceNotePlayer.kt`: تشغيل الملاحظات الصوتية عبر `MediaPlayer`.
- **`com.example.utils` (الأدوات المساعدة):**
  - `LocalNotificationManager.kt`: إشعارات النظام للمكالمات والرسائل الواردة.
  - `StorageUtils.kt`: مسارات حفظ وتصدير الملفات والوسائط.

---

## Features Inventory

| # | الوظيفة (Feature) | الملفات المسؤولة | الدليل من الكود الفعلي | الحالة الدقيقة (Status) | يحتاج اختبار جهازين؟ |
|---|---|---|---|---|---|
| 1 | **Peer Discovery** | `NetworkUtils.kt`, `LocalP2PEngine.kt`, `NetworkServiceDiscoveryEngine.kt` | استماع وبث UDP Broadcast / Multicast على المنفذ `8888` ومسح الشبكة الفرعية Subnet Scan. | **Implemented - Not Tested** | نعم (ضروري بسبب عزل الراوتر AP Isolation) |
| 2 | **UDP Messaging** | `LocalP2PEngine.kt`, `MainViewModel.kt` | إرسال واستقبال حزم `CHAT_MSG` المشفرة عبر UDP Socket بدون وسيط خادم خارجي. | **Implemented - Not Tested** | نعم |
| 3 | **Group Messaging** | `LocalP2PEngine.kt`, `MainViewModel.kt`, `AppDatabase.kt` | إرسال رسائل موجهة للغرف (`targetRoomOrPeerId = roomId`) وتخزينها محلياً في Room DB. | **Implemented - Not Tested** | نعم |
| 4 | **Message Editing** | `LocalP2PEngine.kt`, `MainViewModel.kt` | حزمة `EDIT_MSG`، وبث التعديل في الوقت الفعلي وتحديث قاعدة البيانات `chatDao.updateMessageContent`. | **Implemented - Not Tested** | نعم |
| 5 | **Message Deletion** | `MainViewModel.kt` | الحذف المحلي موجود في قاعدة البيانات، أما حذف الرسالة من الطرف الآخر أو الحذف الشبكي فلم يتم العثور عليه أو لم يتم إثباته. لا تعتبره عيبًا نهائيًا قبل تحديد المتطلب الوظيفي المطلوب. | **Partially Implemented - Local Delete Only** | نعم |
| 6 | **Reply to Message** | `Models.kt`, `ChatMessageBubble.kt`, `LocalP2PEngine.kt` | حقول `replyToId`, `replyToSender`, `replyToText` في هيكل `ChatMessage` وحزم `CHAT_MSG`. | **Implemented - Not Tested** | نعم |
| 7 | **Typing Indicator** | `LocalP2PEngine.kt`, `TypingIndicator.kt` | حزم `TYPING_STATUS` وإدارتها عبر `typingMap` مع مهلة انتهاء تلقائي (3.5 ثوانٍ). | **Implemented - Not Tested** | نعم |
| 8 | **Delivery / Read Acks** | `LocalP2PEngine.kt`, `MainViewModel.kt` | حزم `MSG_ACK` للتأكيد على الوصول، و `MSG_READ` عند فتح المحادثة، وتحديث علامات الصح في `chatDao`. | **Implemented - Not Tested** | نعم |
| 9 | **File Transfer** | `FileTransferEngine.kt`, `LocalP2PEngine.kt` | خادم وعميل TCP على المنفذ `8891` لنقل الملفات المجزأة إلى Chunks مشفرة. | **Implemented - Not Tested** | نعم |
| 10 | **Resume Transfer** | `FileTransferEngine.kt` | فحص طول الملف الموجود واستئناف النقل من نقطة `offset` وتوليد Nonce قطعي للقطع المشفرة. | **Implemented - Not Tested** | نعم |
| 11 | **Audio Calls** | `AudioEngine.kt`, `LocalP2PEngine.kt`, `CallScreenDialog.kt` | بث صوتي 16kHz PCM أحادي مشفر بـ AES-GCM عبر UDP:8889 مع إدارة النغمات والاهتزاز. | **Implemented - Not Tested** | نعم |
| 12 | **Video Calls** | `VideoEngine.kt`, `LocalP2PEngine.kt`, `CallScreenDialog.kt` | التقاط عبر CameraX، ضغط إطارات JPEG، وتشفير وإرسال عبر UDP:8890. | **Implemented - Not Tested** | نعم |
| 13 | **Voice Notes** | `VoiceNoteRecorder.kt`, `VoiceNotePlayer.kt`, `AudioWaveformVisualizer.kt` | تسجيل AAC/M4A وعرض الموجات الصوتية وإرسالها عبر محرك الملفات كرسائل `VOICE_NOTE`. | **Implemented - Not Tested** | نعم |
| 14 | **Screen Sharing** | `ScreenCaptureService.kt`, `VideoEngine.kt` | استخدام Android `MediaProjection` و `VirtualDisplay` لبث الشاشة كفيديو للطرف الآخر. | **Implemented - Not Tested** | نعم |
| 15 | **Room / Group Calls** | `LocalP2PEngine.kt`, `GroupCallScreenDialog.kt`, `AudioEngine.kt` | حزم `GROUP_CALL_START` و `GROUP_CALL_JOIN` مع خوارزمية مزج المسارات الصوتية `mixPcmFrames`. | **Implemented - Not Tested** | نعم |
| 16 | **Network Rebinding** | `LocalP2PEngine.kt`, `NetworkUtils.kt` | استخدام `ConnectivityManager.NetworkCallback` لإعادة فتح المنافذ تلقائياً عند تغيير الـ IP أو انقطاع الشبكة. | **Implemented - Not Tested** | نعم |
| 17 | **Heartbeat** | `LocalP2PEngine.kt` | بث نبضات دورية `PING` كل 2500ms ومراقبة خمول الأقران مع مهلة 45,000ms. | **Implemented - Not Tested** | نعم |
| 18 | **Encryption (AES-256-GCM)** | `LocalCryptoEngine.kt` | تشفير AES-256-GCM لكافة حزم الإشارات والصوت والفيديو والملفات (بمفتاح مشتق من Passphrase ثابتة). | **Implemented - Not Tested** | نعم |
| 19 | **SHA-256: Nonce & Passwords** | `LocalCryptoEngine.kt` | اشتقاق Nonce قطعياً للقطع، وتجزئة كلمات مرور الغرف المحمية عبر `MessageDigest.getInstance("SHA-256")`. | **Implemented - Not Tested** | نعم |
| 20 | **SHA-256: File Integrity Checksum** | `FileTransferEngine.kt` | لم يتم العثور على مقارنة مؤكدة بين Hash الملف الأصلي والملف المستلم. | **Missing or Not Verified** | نعم |
| 21 | **Local Database** | `AppDatabase.kt`, `ChatDao` | حفظ الرسائل، الأقران، والغرف محلياً عبر Room v6 (SQLite) مع دعم الكيانات والعلاقات. | **Implemented - Partially Verified** | لا (تم فحص بنية الجداول محلياً) |
| 22 | **Notifications** | `LocalNotificationManager.kt` | قنوات إشعارات المكالمات والرسائل مع شاشات الرد الكاملة (`fullScreenIntent`). | **Implemented - Not Tested** | نعم (لاختبار أذونات أندرويد 13+) |

---

## Current Architecture

التطبيق حالياً يعتمد على نموذج تقريبي لـ **MVVM (Model-View-ViewModel)** ولكنه **يفتقر تماماً إلى Clean Architecture** ويعاني من تداخل شديد في المسؤوليات (Tight Coupling):

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Views)                     │
│    (MainActivity, ChatScreen, PeersScreen, Dialogs)    │
└───────────────────────────┬────────────────────────────┘
                            │ يعتمد مباشرة
                            ▼
┌────────────────────────────────────────────────────────┐
│                      ViewModel                         │
│                    MainViewModel                       │
│    (1,839 سطر: حالة الواجهة، استدعاء مباشر للشبكة)     │
└─────────────┬────────────────────────────┬─────────────┘
              │ يعتمد مباشرة                │ يعتمد مباشرة
              ▼                            ▼
┌───────────────────────────┐ ┌──────────────────────────┐
│        Room Database      │ │      LocalP2PEngine      │
│  (AppDatabase / ChatDao)  │ │      (2,496 سطر)         │
│     تخزين محلي مباشر      │ │  (منسق مركزي ضخم / God)  │
└───────────────────────────┘ └────────────┬─────────────┘
                                           │ يتحكم مباشرة
              ┌────────────────────────────┼───────────────────────────┐
              ▼                            ▼                           ▼
    ┌──────────────────┐         ┌───────────────────┐       ┌───────────────────┐
    │   AudioEngine    │         │    VideoEngine    │       │FileTransferEngine │
    │   (UDP: 8889)    │         │    (UDP: 8890)    │       │   (TCP: 8891)     │
    └─────────┬────────┘         └─────────┬─────────┘       └─────────┬─────────┘
              │                            │                           │
              └────────────────────────────┼───────────────────────────┘
                                           │ تشفير مباشر
                                           ▼
                                 ┌───────────────────┐
                                 │ LocalCryptoEngine │
                                 │   (AES-256-GCM)   │
                                 └───────────────────┘
```

### الملاحظات المعمارية على الوضع الحالي:
1. **غياب طبقة الـ Repository والـ Abstraction:** الـ `MainViewModel` يخاطب `LocalP2PEngine` و `AppDatabase` بشكل مباشر دون أي واجهات وسيطة (Interfaces) أو Repositories.
2. **الـ God Class:** `LocalP2PEngine` يجمع بين: فتح الـ Sockets، فك التشفير، اكتشاف الأجهزة، نبضات القلب، حالة المكالمات، حالة المكالمات الجماعية، نغمات الرنين، تسجيل الملاحظات الصوتية، ومراقبة تغير عناوين IP.
3. **انعدام القابلية للاختبار المعزول (Testability):** لا يمكن كتابة Unit Test لـ `MainViewModel` أو محركات الشبكة دون بناء سياق أندرويد حقيقي (Android Context & Sockets).

---

## Encryption Baseline

محرك التشفير المطبق هو `LocalCryptoEngine.kt`، وتفاصيله الدقيقة من واقع الكود:

1. **الخوارزمية:** Symmetric Encryption — `AES` (Advanced Encryption Standard).
2. **نمط التشفير (Transformation):** `AES/GCM/NoPadding` (Galois/Counter Mode) — تشفير موثق معتمد (AEAD).
3. **طول المفتاح (Key Size):** 256 بت (`KEY_BITS = 256` / 32 بايت).
4. **طول وسام المصادقة (Authentication Tag Size):** 128 بت (`GCM_TAG_BITS = 128` / 16 بايت).
5. **طول المتغير العشوائي (Nonce / IV Size):** 12 بايت (`NONCE_SIZE = 12`).
6. **توليد واشتقاق المفتاح:**
   - خوارزمية الاشتقاق: `PBKDF2WithHmacSHA256`.
   - عدد الدورات (Iterations): `12,000` دورة.
   - قيمة الملح (Salt): ثابت ومكتوب في الكود: `"LocalConnect::P2P::NetworkSalt::v1"`.
   - كلمة المرور (Passphrase): ثابتة وافتراضية: `"LocalConnect-Offline-Pairing-Key-v1"`.
   - مفتاح احتياطي ثابت مصفوفة أصفار: `defaultKeyBytes = ByteArray(32) { 0 }`.
7. **طبيعة المفتاح الافتراضي ومخاطره الأكاديمية:**
   - وجود Passphrase ثابتة داخل الكود المصدري للتطبيق يعني إمكانية استخراجها بسهولة عند عمل Reverse Engineering للـ APK.
   - هذا يؤدي إلى **اشتراك جميع نسخ التطبيق المنشورة في سر تشفير واحد مشترك**، وبالتالي **لا يوفر التشفير الحالي عزلاً حقيقياً بين الشبكات المنفصلة أو بين أزواج المستخدمين المختلفين**.
8. **هل يُستخدم Android Keystore؟** **كلا، نهائياً.** الكود يستخدم `SecretKeySpec` و `SecretKeyFactory` القياسية في الذاكرة الحية (RAM)، ولا يوجد أي تعامل مع مخزن مفاتيح أندرويد الآمن (Keystore).
9. **مواقع التشفير وفك التشفير في النظام:**
   - **الإشارات والرسائل (Signaling & Messages):** في `LocalP2PEngine.kt` عبر `LocalCryptoEngine.encryptJson` عند الإرسال، و `LocalCryptoEngine.decrypt` عند الاستقبال عبر منفذ UDP 8888.
   - **الصوت (Audio):** في `AudioEngine.kt` عبر `encrypt` لإطارات PCM، و `decrypt` قبل التمرير إلى `AudioTrack` (UDP 8889).
   - **الفيديو (Video):** في `VideoEngine.kt` عبر `encrypt` لمصفوفات JPEG، و `decrypt` قبل فك تشفير الصورة عبر `BitmapFactory` (UDP 8890).
   - **الملفات (File Chunks):** في `FileTransferEngine.kt` عبر `encryptChunk` و `decryptChunk` لكل جزء من الملف (TCP 8891).
10. **سلامة Nonce قطع الملفات (File Chunk Nonce Analysis):**
    - يعتمد المحرك على توليد Nonce قطعي لكل جزء عبر:
      ```kotlin
      SHA-256("$fileId:$sessionId:$chunkIndex")[0..11]
      ```
    - **التقييم الأمني:** هذا الأسلوب (Deterministic Nonce) ليس خاطئاً بحد ذاته، بل سلامته التشفيرية تعتمد بشكل قطعي على **فرادة (Uniqueness) التركيبة الثلاثية:**
      $$\text{fileId} + \text{sessionId} + \text{chunkIndex}$$
      تحت نفس مفتاح التشفير.
    - **الملاحظة الحرجة في الكود:** المعامل `sessionId` معرّف حالياً كقيمة اختيارية افتراضية فارغة (`sessionId: String = ""`). في بيئة الإنتاج أو عند تكرار النقل لنفس الملف، **يجب ألا يكون `sessionId` فارغاً أبداً** لضمان عدم تكرار Nonce مع نفس المفتاح تحت نمط GCM، وهذه النقطة تحتاج إلى اختبار إثبات وتوثيق دقيق.
11. **فحص سلامة الملفات (File Integrity Checksum):**
    - يُستخدم SHA-256 في الكود لاشتقاق الـ Nonces وتجزئة كلمات مرور الغرف فقط.
    - **لا يوجد كود يحسب Hash الملف الأصلي بالكامل ويقارنه مع Hash الملف بعد اكتمال التنزيل** للتأكد من عدم تلف الملف أو تعديله (`File Integrity Checksum: Missing or Not Verified`).
12. **حماية إعادة الإرسال (Replay Protection):**
    - في حزم الرسائل العادية، يتم توليد Nonce عشوائي جديد لكل حزمة، ولكن **لا توجد آلية لفحص تسلسل الأرقام (Sequence Numbers) أو تخزين الطوابع الزمنية (Timestamps Cache)** لمنع إعادة إرسال الحزم الملتقطة.
13. **الاختبارات الآلية للتشفير:** **غير موجودة حالياً (0 اختبارات)**.

---

## Existing Tests

تم فحص مجلدات الاختبارات بالكامل (`app/src/test` و `app/src/androidTest`):

| اسم ملف الاختبار | الوظيفة / الدوال المختبرة | نوع الاختبار | التغطية الفعلية لكود المشروع |
|---|---|---|---|
| `ExampleUnitTest.kt` | دالة `addition_isCorrect()` تختبر `assertEquals(4, 2 + 2)` | Boilerplate Template | لا يغطي أي جزء من كود المشروع نهائياً. |
| `ExampleRobolectricTest.kt` | دالة `read string from context()` تقرأ `R.string.app_name` وتتحقق من مطابقتها لـ "LocalConnect" | Context Test (Robolectric) | اختبار لقراءة ملف الموارد `strings.xml` فقط، ولا يغطي أي منطق عمل. |
| `GreetingScreenshotTest.kt` | دالة `app_screenshot()` تلتقط لقطة شاشة بصرية لمكون `MainAppScreen()` عبر Roborazzi | UI Screenshot Test | اختبار عرض واجهة مرئي فقط، لا يختبر أي وظيفة شبكية أو منطقية. |
| `ExampleInstrumentedTest.kt` | دالة `useAppContext()` تتحقق من أن معرف الحزمة يطابق `APPLICATION_ID` | Device Template Test | قالب أندرويد ستوديو الافتراضي، لا يختبر أي ميزة. |

**الخلاصة:**
- تغطية التشفير (Cryptography Coverage): **0% (معدومة)**.
- تغطية ترميز الحزم (Packet Encoding/Decoding): **0% (معدومة)**.
- تغطية نقل الملفات (File Transfer): **0% (معدومة)**.
- تغطية منطق الاتصال والأعمال (Business Logic & P2P): **0% (معدومة)**.

---

## Build Result

### تصنيف حالة البناء الحالية:
**`BLOCKED BY ENVIRONMENT` (محظور بسبب عوائق بيئية)**

> [!NOTE]
> هذا الفشل **ليس خطأ في الكود المصدري (Not a Source Code Failure)**، حيث لم يظهر أي خطأ في تصريف أكواد Kotlin أو تضارب في الكلاسات.

### التفاصيل التقنية لمحاولات البناء:
1. **أمر `./gradlew.bat --version` و `./gradlew testDebugUnitTest` المباشر:**
   - **النتيجة:** تعذر التنفيذ الفوري بسبب البيئة والمتغيرات (Exit code 1).
   - **الرسالة:**
     ```text
     ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
     Please set the JAVA_HOME variable in your environment to match the location of your Java installation.
     ```
   - **السبب البيئي:** وجود خلل في ترميز اسم متغير النظام `JAVA_HOME` في سجل النظام بالويندوز (Encoding Glitch)، مما منع بيئة التشغيل من التعرف عليه تلقائياً.
2. **عند توجيه المتغير لمترجم الجافا المعتمد (`JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`):**
   - يبدأ `gradlew.bat` بمحاولة تنزيل حزمة التوزيع الرسمية `gradle-9.4.1-bin.zip` من موقع `services.gradle.org`.
   - **عائق البيئة والشبكة:** خوادم تنزيل Gradle تشهد بطئاً شديداً على الشبكة الحالية (معدل النقل يتراوح بين 0.5 إلى 30 كيلوبايت/ثانية)، مما تسبب في تعليق استكمال تنزيل حزمة Gradle Wrapper البالغة 131 ميجابايت إلى الكاش المحلي.
3. **حالة الـ Android SDK:**
   - الـ SDK متوفر محلياً على المسار `H:\Sdk` ويتضمن الحزم المطابقة تماماً لمتطلبات المشروع: `platforms/android-36` و `build-tools/36.1.0`.

---

## Manual Device Test Plan

خطة الاختبار اليدوية بين **جهازين أندرويد حقيقيين (Device A و Device B)** متصلين بنقطة اتصال محلية (Wi-Fi Hotspot) بدون إنترنت:

| # | اسم الاختبار (Scenario) | المتطلبات المسبقة (Preconditions) | خطوات التنفيذ (Steps) | النتيجة المتوقعة (Expected Result) | النتيجة الفعلية (Actual) | الحالة الدقيقة |
|---|---|---|---|---|---|---|
| 1 | **اكتشاف الجهازين (Peer Discovery)** | تشغيل التطبيق على الجهازين وربطهما بنفس الشبكة المحلية. | فتح التطبيق في Device A و Device B على شاشة الأقران. | يظهر كل جهاز في قائمة أقران الجهاز الآخر خلال 3 ثوانٍ بلونه واسمه. | لم تُختبر بعد على جهازين | **NOT TESTED** |
| 2 | **إرسال رسالة عادية (Direct Chat)** | اكتمال الاكتشاف بين الجهازين. | اختيار Device B من قائمة الأقران وكتابة "Hello P2P" والضغط على إرسال. | تصل الرسالة فوراً إلى Device B وتظهر في الشاشة وتُخزن في قاعدة البيانات. | لم تُختبر بعد | **NOT TESTED** |
| 3 | **إرسال رسالة باللغة العربية (UTF-8)** | نافذة المحادثة مفتوحة. | إرسال رسالة: "تجربة مشروع التخرج - اتصال بدون إنترنت". | تظهر الرسالة بنفس الأحرف والترميز الصحيح دون أي تشويه أو علامات استفهام. | لم تُختبر بعد | **NOT TESTED** |
| 4 | **رسالة جماعية في الغرفة (Room Chat)** | دخول الجهازين إلى غرفة "عامة" (General). | إرسال رسالة من Device A داخل الغرفة. | تصل الرسالة إلى Device B وإلى كافة المنضمين للغرفة فوراً عبر البث الجماعي. | لم تُختبر بعد | **NOT TESTED** |
| 5 | **تعديل وحذف الرسالة (Edit & Delete)** | وجود رسالة مرسلة سابقاً. | 1. تعديل الرسالة إلى "نص معدل".<br>2. حذف الرسالة. | 1. يتغير النص عند الطرفين وتظهر علامة (معدلة).<br>2. **تُحذف محلياً فقط وتظل لدى الطرف الآخر (خلل موثق)**. | لم تُختبر بعد | **NOT TESTED** |
| 6 | **إشعارات الوصول والقراءة (Delivery & Read)** | نافذة المحادثة بين الجهازين. | إرسال رسالة ثم قيام الطرف الآخر بفتحها. | تظهر علامة صح واحدة للإرسال، ثم صحين رماديين للوصول، ثم صحين ملونين للقراءة. | لم تُختبر بعد | **NOT TESTED** |
| 7 | **إرسال ملف صغير (صورة)** | اختيار أيقونة إرفاق الوسائط. | تحديد صورة بحجم 2 ميجابايت وإرسالها. | يُنقل الملف بنجاح وتظهر الصورة في فقاعة المحادثة مع إمكانية عرضها وتنزيلها. | لم تُختبر بعد | **NOT TESTED** |
| 8 | **إرسال ملف كبير (فيديو 100MB+)** | ذاكرة كافية على الجهازين. | إرسال ملف فيديو كبير ومراقبة شريط التقدم وسرعة النقل. | يكتمل النقل بنجاح عبر بروتوكول TCP المنفذ 8891 دون توقف أو انهيار للذاكرة. | لم تُختبر بعد | **NOT TESTED** |
| 9 | **إيقاف واستئناف نقل الملف (Resume)** | بدء نقل ملف كبير (50MB+). | فصل شبكة الجهاز المستقبل عند 40%، ثم إعادة الاتصال والضغط على "استئناف". | يُستأنف النقل من آخر بايت مسجل دون إعادة تنزيل الـ 40% الأولى. | لم تُختبر بعد | **NOT TESTED** |
| 10 | **مكالمة صوتية (Voice Call)** | منح أذونات الميكروفون لكلا الجهازين. | الضغط على زر الاتصال الصوتي من Device A باتجاه Device B. | يرن هاتف B بنغمة واهتزاز، وعند القبول يتدفق الصوت النقي في الاتجاهين دون صدى. | لم تُختبر بعد | **NOT TESTED** |
| 11 | **مكالمة فيديو (Video Call)** | منح أذونات الكاميرا والميكروفون. | بدء مكالمة فيديو من Device A. | يظهر بث الكاميرا المتبادل في الوقت الفعلي مع إمكانية التبديل بين الكاميرتين الأمامية والخلفية. | لم تُختبر بعد | **NOT TESTED** |
| 12 | **ملاحظة صوتية (Voice Note)** | شاشة المحادثة المباشرة. | الضغط المطول على زر التسجيل لـ 5 ثوانٍ ثم الإفلات للإرسال. | تظهر فقاعة بصيغة الملاحظة الصوتية مع الموجات التفاعلية وتعمل عند الضغط على Play. | لم تُختبر بعد | **NOT TESTED** |
| 13 | **مشاركة الشاشة (Screen Share)** | مكالمة فيديو نشطة. | تفعيل خيار "مشاركة الشاشة" والموافقة على إذن MediaProjection. | يرى الطرف الآخر شاشة جهازك بالكامل بدلاً من إطار الكاميرا. | لم تُختبر بعد | **NOT TESTED** |
| 14 | **تغيير الشبكة وإعادة الربط (Rebind)** | اتصال نشط ومحادثة جارية. | إيقاف Wi-Fi وتشغيله مجدداً، أو التبديل من Hotspot إلى شبكة راوتر. | يقوم `NetworkCallback` بإعادة ربط الـ Sockets تلقائياً واستعادة الاتصال دون الحاجة لإعادة فتح التطبيق. | لم تُختبر بعد | **NOT TESTED** |
| 15 | **انقطاع الشبكة المفاجئ والخمول** | جهاز متصل في قائمة الأقران. | إغلاق Wi-Fi في Device B فجأة دون تسجيل خروج. | يستمر مؤقت الـ Heartbeat، وبعد مرور 45 ثانية يختفي Device B تلقائياً من قائمة الأقران. | لم تُختبر بعد | **NOT TESTED** |
| 16 | **إثبات التشفير العملي (Wireshark)** | تشغيل برنامج تحليل الحزم (Wireshark) على نفس الشبكة ومراقبة المنفذ 8888 و 8891. | إرسال رسالة "TopSecretPassword" وملف عبر التطبيق. | كافة الحزم الملتقطة تظهر كبيانات عشوائية مبهمة (Ciphertext)، ولا يمكن قراءة أي نص صريح (Plaintext). | لم تُختبر بعد | **NOT TESTED** |

---

## Known Problems Before Refactoring

جدول المشاكل الهندسية والأمنية المرصودة في الكود الفعلي قبل أي تعديل:

| # | المشكلة المرصودة | الملف المسئول | الدليل من الكود | التأثير المعماري والتشغيلي | الأولوية (Priority) |
|---|---|---|---|---|---|
| 1 | **صنف إلهي عملاق (God Class)** | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | يتجاوز 2,496 سطراً ويحتوي على منطق النقل، المكالمات، التشفير، النغمات، ومسح الشبكة. | انتهاك صارخ لمبدأ المسؤولية الواحدة (SRP)، استحالة كتابة اختبارات معزولة، وصعوبة تتبع الأخطاء. | **Critical** |
| 2 | **متحكم عملاق (Massive ViewModel)** | `app/src/main/java/com/example/ui/MainViewModel.kt` | يتجاوز 1,839 سطراً ويحتوي على استدعاءات قاعدة البيانات المباشرة والشبكة والواجهات. | تداخل طبقة العرض مع منطق الأعمال، تكرار الأكواد، وعدم استقرار إدارة الحالة (State Management). | **High** |
| 3 | **غياب الواجهات والتجريد (No Interfaces / DIP Violation)** | كافة طبقات المشروع | `MainViewModel` يعتمد مباشرة على الأصناف المادية `LocalP2PEngine` و `AppDatabase`. | اقتران شديد (Tight Coupling) يجعل من المستحيل استبدال محرك الشبكة بمحرك تجريبي (Mock) للاختبار. | **High** |
| 4 | **مفاتيح التشفير ثابتة ومشتركة في كل النسخ** | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | الأسطر 37 و 40: `DEFAULT_PASSPHRASE` و `defaultKeyBytes` ثابتة. | إمكانية استخراج كلمة المرور من الـ APK تجعل جميع النسخ تشترك في سر واحد دون عزل حقيقي بين الشبكات. | **Critical** (أكاديمياً في التشفير) |
| 5 | **عدم استخدام Android Keystore** | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | يتم تخزين وتمرير المفاتيح كـ `SecretKeySpec` داخل الذاكرة دون حماية أجهزة الأمان (TEE/SE). | إمكانية استخراج المفاتيح عبر أدوات الـ Reverse Engineering أو Memory Dump. | **High** |
| 6 | **حذف الرسائل محلي فقط (No Network Delete Protocol)** | `app/src/main/java/com/example/ui/MainViewModel.kt` (السطر 1721) | الدالة `deleteMessage()` تحذف الرسالة من جدول `chatDao` فقط، ولا تبث أي حزمة `DELETE_MSG`. | الرسالة المحذوفة تبقى مرئية ومخزنة لدى الطرف الآخر دون إشعار. | **Medium** |
| 7 | **اعتماد سلامة Nonce القطع على فرادة sessionId** | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` (السطر 134) | دالة `deriveChunkNonce` تشتق 12 بايت، مع وجود `sessionId` افتراضي فارغ. | إذا تُرِك `sessionId` فارغاً أو تكرر مع نفس المفتاح والملف، يتكرر الـ Nonce تحت نمط GCM مما يمثل خطراً أمنياً. | **High** |
| 8 | **غياب فحص سلامة الملفات (File Integrity Checksum)** | `app/src/main/java/com/example/network/FileTransferEngine.kt` | لا توجد دالة لحساب SHA-256 للملف ومقارنتها بعد انتهاء التنزيل. | عدم وجود آلية للتحقق من خلو الملف المنقول من التلف أو التعديل غير المصرح به. | **High** |
| 9 | **غياب الاختبارات الآلية الحقيقية (Zero Real Tests)** | مجلد `app/src/test/` | لا توجد اختبارات لتشفير الحزم، ولا لمنطق المحرك، ولا لتجزئة الملفات (فقط اختبار 2+2 الافتراضي). | عدم القدرة على إجراء Refactoring آمن دون مخاطرة كسر الوظائف الأساسية. | **High** |
| 10 | **طباعة بيانات حساسة وعناوين IP في السجلات (Unsanitized Logging)** | `LocalP2PEngine.kt`, `FileTransferEngine.kt` | استدعاءات مكثفة لـ `Log.d` و `Log.i` تحتوي على نصوص الحزم وعناوين الـ IP الخاصة بالأجهزة. | تسريب بيانات خصوصية المستخدم للأجهزة الأخرى أو لتطبيقات الطرف الثالث على نفس الجهاز. | **Low** |
| 11 | **عدم وجود وثائق معمارية وهندسية رسمية (Missing SRS & Architecture Docs)** | مجلد `docs/` | لا توجد وثيقة متطلبات برمجية رسمية (SRS)، ولا مخططات تدفق UML، ولا وثيقة مواصفات البروتوكول. | ضعف الملف التوثيقي الأكاديمي لمادتي هندسة البرمجيات والتشفير للمناقشة مع الأساتذة. | **High** |

---

## Baseline Rules

لضمان سلامة المشروع والحفاظ على استقراره الأكاديمي والعملي، يجب تطبيق القواعد التالية في جميع المراحل القادمة:

1. **الحفاظ الصارم على الوظائف (Functionality Preservation):**
   أي إعادة هيكلة معمارية (Refactoring) لاحقة يجب ألا تعطل أو تحذف أي وظيفة من الوظائف المرصودة في جدول `Features Inventory`.
2. **التحقق المستمر (Continuous Verification):**
   كل تعديل على الكود يجب أن يتبعه محاولة بناء وتمرير للاختبارات للتأكد من عدم حدوث Regressions.
3. **التغيير بالتدريج (Incremental Refactoring):**
   فصل المحركات العملاقة عبر Interfaces وتطبيق Clean Architecture يجب أن يتم خطوة بخطوة، مع إبقاء الكود القديم يعمل كـ Implementation حتى يتم استبداله بأمان.
4. **التوثيق المتزامن:**
   أي تعديل يتم إقراره يجب أن يُحدث في وثائق المشروع ووثيقة التشفير والهندسة البرمجية مع توضيح سبب التغيير الأكاديمي والعملي.
