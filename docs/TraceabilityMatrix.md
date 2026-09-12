# Requirements Traceability Matrix (RTM)

## Document Information
- **Project:** LOCAL CONTACT
- **Document:** Requirements Traceability Matrix (RTM)
- **Baseline Commit:** `71111825645c2b3c6a926f739455c4ffda3c0963`
- **Specification Reference:** `docs/SRS.md`
- **Baseline Reference:** `docs/Baseline.md`
- **Date:** 2026-09-11
- **Status:** Baseline / Academic Verification & Traceability Audit

---

## 1. Requirements to Code (ربط المتطلبات بالكود المصدري الفعلي)

| Requirement ID | Requirement Title | File Path | Class / Component | Code Evidence | Current Status |
|---|---|---|---|---|---|
| **FR-01** | اكتشاف الأجهزة المحلية | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | `LocalP2PEngine` | الدالتان `startDiscoveryListener()` و `broadcastPresence()` مع مقبس `MulticastSocket` على `239.255.42.99:8888` | Implemented - Not Tested |
| **FR-02** | تحديث قائمة الأقران بالذاكرة | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | `LocalP2PEngine` | الدالة `parseAndStorePeer()`، تحديث `ConcurrentHashMap` في `peersMap`، وتدفق `_discoveredPeers` | Implemented - Not Tested |
| **FR-03** | إزالة الأقران الخاملين | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | `LocalP2PEngine` | الدالة `startPeerCleanup()` ومؤقت `PEER_TIMEOUT_MS` (45 ثانية) لحذف الأقران الخاملين من الذاكرة | Implemented - Not Tested |
| **FR-04** | إعادة ربط الشبكة التلقائي | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | `LocalP2PEngine` | الحقل `networkCallback` عبر `ConnectivityManager` والدالة `restartNetworkDiscovery()` | Implemented - Not Tested |
| **FR-05** | إرسال رسالة فردية | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/ui/MainViewModel.kt`<br>`app/src/main/java/com/example/data/local/AppDatabase.kt` | `LocalP2PEngine`<br>`MainViewModel`<br>interface `ChatDao` | الدالة `sendMessage()`، حزمة `CHAT_MSG`، وإدراج الرسالة في جدول `chat_messages` عبر واجهة `ChatDao` المعرفة داخل `AppDatabase.kt` | Implemented - Not Tested |
| **FR-06** | إرسال رسالة جماعية للغرفة | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/ui/screens/ChatScreen.kt` | `LocalP2PEngine` | الدالة `sendMessage()` واستدعاء `sendJsonPacket()` عبر عنوان البث المتعدد | Implemented - Not Tested |
| **FR-07** | تعديل محتوى الرسالة | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/ui/MainViewModel.kt`<br>`app/src/main/java/com/example/data/local/AppDatabase.kt` | `LocalP2PEngine`<br>`MainViewModel`<br>interface `ChatDao` | الدالة `sendEditMessage()`، حزمة `EDIT_MSG`، وتحديث سجل الرسالة عبر `ChatDao` داخل `AppDatabase.kt` | Implemented - Not Tested |
| **FR-08** | الرد على الرسالة والاقتباس | `app/src/main/java/com/example/model/Models.kt`<br>`app/src/main/java/com/example/ui/components/ChatMessageBubble.kt` | data class `ChatMessage` | حقول الاقتباس `replyToId` و `replyToSender` و `replyToText` داخل فئة البيانات `ChatMessage` وعرضها في الفقاعة | Implemented - Not Tested |
| **FR-09** | مؤشر جاري الكتابة | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/ui/components/TypingIndicator.kt` | `LocalP2PEngine` | الدالة `sendTypingStatus()`، حزمة `TYPING_STATUS`، وتحديث خريطة `typingMap` | Implemented - Not Tested |
| **FR-10** | إشعار تسليم الرسالة | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/data/local/AppDatabase.kt` | `LocalP2PEngine`<br>interface `ChatDao` | معالجة حزمة `MSG_ACK` في `LocalP2PEngine`، واستدعاء الدالة `upgradeMessageDeliveryStatus()` داخل واجهة `ChatDao` المعرفة في `AppDatabase.kt` | Implemented - Not Tested |
| **FR-11** | إشعار قراءة الرسالة | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/data/local/AppDatabase.kt` | `LocalP2PEngine`<br>interface `ChatDao` | إرسال حزمة `MSG_READ` عبر `sendMsgRead()`، واستدعاء الدالة `markMessagesAsRead()` داخل واجهة `ChatDao` المعرفة في `AppDatabase.kt` | Implemented - Not Tested |
| **FR-12** | حذف الرسالة محلياً | `app/src/main/java/com/example/ui/MainViewModel.kt`<br>`app/src/main/java/com/example/data/local/AppDatabase.kt` | `MainViewModel`<br>interface `ChatDao` | استدعاء الدالة `deleteMessage(messageId)` في `MainViewModel` وتنفيذ استعلام `DELETE FROM chat_messages` في واجهة `ChatDao` المعرفة داخل `AppDatabase.kt` | Implemented - Not Tested (Static SQL Query Code Present) |
| **FR-13** | حذف الرسالة شبكياً | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | غير موجود | لا يوجد أي كود أو حزمة شبكية لحذف الرسالة من أجهزة الأقران عن بعد | Missing / Proposed Improvement |
| **FR-14** | إرسال الملفات عبر TCP | `app/src/main/java/com/example/network/FileTransferEngine.kt` | `FileTransferEngine` | تدفق فتح مقبس `Socket` على المنفذ `8891` وإرسال ترويسة الملف وأجزاء البيانات عبر `OutputStream` | Implemented - Not Tested |
| **FR-15** | استقبال وحفظ الملفات | `app/src/main/java/com/example/network/FileTransferEngine.kt`<br>`app/src/main/java/com/example/utils/StorageUtils.kt` | `FileTransferEngine` | الدالة `startServer()` مع `ServerSocket(8891)` وتدفق كتابة الأجزاء في ملف مؤقت `.part` ثم نقله للتنزيلات | Implemented - Not Tested |
| **FR-16** | نقل الملفات تدفقياً ومجزأً | `app/src/main/java/com/example/network/FileTransferEngine.kt` | `FileTransferEngine` | قراءة الملف في هيئة كتل عبر بفر تدفقي لتقليل استهلاك الذاكرة (مع الحاجة لقياس عدم حدوث OOM عملياً) | Implemented - Not Tested / Needs Measurement & Documentation |
| **FR-17** | استئناف نقل الملفات | `app/src/main/java/com/example/network/FileTransferEngine.kt`<br>`app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `FileTransferEngine`<br>`LocalCryptoEngine` | قراءة إزاحة البايتات `offset`، واشتقاق Nonce لكل كتلة عبر الدالة `deriveChunkNonce()` | Implemented - Not Tested / Needs Empirical Testing |
| **FR-18** | تعقيم مسارات الملفات | `app/src/main/java/com/example/utils/StorageUtils.kt`<br>`app/src/main/java/com/example/network/FileTransferEngine.kt` | `StorageUtils` | استخراج اسم الملف الآمن عبر `File(fileName).name` لمنع مسارات التراجع `../` | Implemented - Not Tested |
| **FR-19** | فحص سلامة الملف الكامل | `app/src/main/java/com/example/network/FileTransferEngine.kt` | غير موجود | لا توجد دالة أو منطق لمقارنة قيمة SHA-256 للملف الكامل بعد اكتمال الاستلام | Missing / Proposed Improvement |
| **FR-20** | مكالمات صوتية فردية | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/network/AudioEngine.kt`<br>`app/src/main/java/com/example/network/CallToneManager.kt` | `LocalP2PEngine`<br>`AudioEngine` | حزم إشارات المكالمات (`CALL_INVITE`, `CALL_ACCEPT`, `CALL_END`, `CALL_BUSY`) وتدفق صوت 16kHz PCM عبر UDP 8889 | Implemented - Not Tested |
| **FR-21** | مكالمات فيديو مباشرة | `app/src/main/java/com/example/network/VideoEngine.kt`<br>`app/src/main/java/com/example/ui/components/CameraStreamView.kt` | `VideoEngine` | التقاط إطارات الكاميرا وضغطها JPEG وتشفيرها وبثها عبر مقبس UDP على المنفذ `8890` | Implemented - Not Tested |
| **FR-22** | مكالمات جماعية للغرفة | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/network/AudioEngine.kt` | `LocalP2PEngine`<br>`AudioEngine` | حزم `GROUP_CALL_START` و `GROUP_CALL_JOIN` ومزج المسارات الصوتية برمجياً عبر `mixPcmFrames()` | Implemented - Not Tested |
| **FR-23** | ملاحظات صوتية مسجلة | `app/src/main/java/com/example/audio/VoiceNoteRecorder.kt`<br>`app/src/main/java/com/example/audio/VoiceNotePlayer.kt`<br>`app/src/main/java/com/example/ui/components/AudioWaveformVisualizer.kt` | `VoiceNoteRecorder`<br>`VoiceNotePlayer` | تسجيل صوت بصيغة AAC/M4A وعرض تمثيل الموجات الصوتية وإرسالها كملف صوتي | Implemented - Not Tested |
| **FR-24** | مشاركة شاشة الجهاز | `app/src/main/java/com/example/network/ScreenCaptureService.kt`<br>`app/src/main/java/com/example/network/VideoEngine.kt` | `ScreenCaptureService` | خدمة أمامية بنوع `mediaProjection` لالتقاط شاشة أندرويد وتمرير الإطارات لمحرك الفيديو | Implemented - Not Tested |
| **FR-25** | دوام البيانات في Room | `app/src/main/java/com/example/data/local/AppDatabase.kt` | `AppDatabase` | جداول `chat_messages` و `rooms` و `user_accounts` و `blocked_peers` المثبتة في الكود (مع إدارة الأقران المكتشفين في الذاكرة عبر `peersMap` فقط دون جدول في Room) | Implemented - Not Tested (Static Schema Code Present) |
| **FR-26** | حفظ تفضيلات المظهر | `app/src/main/java/com/example/data/ThemePreferencesRepository.kt`<br>`app/src/main/java/com/example/ui/components/ThemeSettingsComponent.kt` | `ThemePreferencesRepository` | استخدام Jetpack DataStore Preferences لحفظ مفاتيح `THEME_MODE` و `ACCENT_COLOR` | Implemented - Not Tested (Static DataStore Keys Present) |
| **FR-27** | إشعارات النظام والمكالمات | `app/src/main/java/com/example/utils/LocalNotificationManager.kt` | `LocalNotificationManager` | قنوات إشعارات النظام المخصصة للمكالمات والرسائل مع دعم `fullScreenIntent` للمكالمات | Implemented - Not Tested |
| **FR-28** | تشفير الحزم الصادرة | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `LocalCryptoEngine` | الدوال `encrypt()` و `encryptJson()` بنمط `AES/GCM/NoPadding` مع وسام مصادقة 128-bit و Nonce بطول 12 بايت | Implemented - Partially Verified (JVM Unit Tests Passed: 10/10) |
| **FR-29** | فك تشفير الحزم الواردة | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `LocalCryptoEngine` | الدالة `decrypt()`، استخراج الـ Nonce والتحقق من صحة وسام المصادقة في نمط GCM | Implemented - Partially Verified (JVM Unit Tests Passed: 10/10) |
| **FR-30** | رفض الحزم المتلاعب بها | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `LocalCryptoEngine` | اعتراض استثناءات المصادقة (`AEADBadTagException`) وإرجاع `null` لإسقاط الحزمة بصمت | Implemented - Partially Verified (JVM Unit Tests Passed: 10/10) |
| **FR-31** | تجزئة كلمات مرور الغرف | `app/src/main/java/com/example/ui/MainViewModel.kt`<br>`app/src/main/java/com/example/data/local/AppDatabase.kt` | `MainViewModel`<br>Class `RoomEntity` | حساب SHA-256 داخل الدالة `hashPassword()` في `MainViewModel` وتخزين الـ Hash في حقل `RoomEntity.passwordHash` المعرف في `AppDatabase.kt` | Implemented - Not Tested |

---

## 2. Requirements to User Stories (ربط المتطلبات بقصص المستخدمين)

| Requirement ID | User Story ID | صيغة قصة المستخدم (User Story) | نوع العلاقة |
|---|---|---|---|
| **FR-01, FR-02, FR-03, FR-04** | **US-01** | كـ **مستخدم محلي**، أريد **اكتشاف أجهزة زملائي المتصلين معي بنفس شبكة الـ Wi-Fi تلقائياً**، حتى **أتمكن من التواصل معهم دون إدخال عناوين IP يدوياً**. | Primary Driver |
| **FR-05, FR-08, FR-09** | **US-02** | كـ **مستخدم محلي**، أريد **إرسال واستقبال رسائل نصية مع قرين محدد مع دعم الرد والمؤشرات الحية**، حتى **أجري محادثة ثنائية فورية**. | Primary Driver |
| **FR-06, FR-31** | **US-03** | كـ **عضو في فريق**، أريد **إنشاء غرف محادثة جماعية مفتوحة أو محمية بكلمة مرور**، حتى **نتشارك الرسائل بين عدة أجهزة معاً**. | Primary Driver |
| **FR-07** | **US-04** | كـ **مرسل رسالة**، أريد **تعديل نص الرسالة بعد إرسالها**، حتى **أصحح أي أخطاء إملائية مباشرة لدى الطرفين**. | Feature Extension |
| **FR-10, FR-11** | **US-05** | كـ **مستخدم محادثة**، أريد **رؤية علامات وصول وقراءة رسائلي (صحين)**، حتى **أتأكد من استلام وقراءة زميلي للرسالة**. | Usability Driver |
| **FR-12, FR-13** | **US-06** | كـ **مستخدم**، أريد **حذف رسائلي محلياً (وشبكياً عند توفر الدعم)**، حتى **أحافظ على خصوصيتي ونظافة سجل المحادثة**. | Privacy Driver |
| **FR-14, FR-15, FR-16, FR-18** | **US-07** | كـ **مستخدم**، أريد **إرسال واستقبال ملفات وصور ومستندات مباشرة عبر الشبكة**، حتى **أتبادل الملفات دون الحاجة إلى فلاش ميموري أو إنترنت**. | Primary Driver |
| **FR-17, FR-19** | **US-08** | كـ **مستخدم يرسل ملفات كبيرة**، أريد **استئناف التنزيل عند انقطاع الشبكة والتحقق من سلامة الملف**، حتى **لا أضيع الوقت في إعادة التحميل من الصفر**. | Reliability Driver |
| **FR-20, FR-21** | **US-09** | كـ **مستخدم**، أريد **إجراء مكالمات صوتية ومكالمات فيديو مباشرة عالية الوضوح**، حتى **أتواصل صوتياً ومرئياً دون تكلفة رصيد أو إنترنت**. | Primary Driver |
| **FR-22** | **US-10** | كـ **فريق عمل**، أريد **الانضمام لمكالمة جماعية في الغرفة بسماع وتحدث مشترك**، حتى **نعقد اجتماعات صوتية لا مركزية**. | Primary Driver |
| **FR-23** | **US-11** | كـ **مستخدم**، أريد **تسجيل وإرسال ملاحظات صوتية فورية**، حتى **أرسل الرسائل السريعة دون الحاجة للكتابة**. | Feature Extension |
| **FR-24** | **US-12** | كـ **مقدم عرض**، أريد **مشاركة شاشة هاتفي أثناء المكالمة**، حتى **أشرح لزميلي المستندات والتطبيقات مباشرة**. | Feature Extension |
| **FR-25, FR-26** | **US-13** | كـ **مستخدم**، أريد **حفظ محادثاتي وسجلاتي وتفضيلات المظهر على جهازي**، حتى **أجدها دائماً عند فتح التطبيق مجدداً**. | Persistence Driver |
| **FR-27** | **US-14** | كـ **مستخدم والتطبيق في الخلفية**، أريد **استلام إشعارات بالرسائل والمكالمات الواردة**، حتى **لا تفوتني أي اتصالات هامة**. | Usability Driver |
| **FR-28, FR-29, FR-30** | **US-15** | كـ **مستخدم مهتم بالأمان**، أريد **تشفير كافة اتصالاتي ورسائلي بـ AES-256-GCM ورفض أي حزم متلاعب بها**، حتى **لا يستطيع أي متلصص على الشبكة قراءة بياناتي**. | Security Driver |

---

## 3. Requirements to Acceptance Criteria (ربط المتطلبات بمعايير القبول)

> [!NOTE]
> معايير القبول التالية بصيغة (Given / When / Then) تمثل شروط التحقق المستهدفة في وثيقة SRS، ولا تعتبر إثباتاً على نجاح الاختبار الفعلي حتى يتم تنفيذ اختبارات تكاملية على أجهزة حقيقية.

| Requirement ID | Acceptance Criteria ID | معيار القبول المرتبط به في وثيقة SRS | آلية التحقق المستهدفة |
|---|---|---|---|
| **FR-01, FR-02** | **AC-01** | ظهور الأجهزة المكتشفة في قائمة الأقران خلال زمن استجابة سريع (Proposed Target: 3 ثوانٍ) دون أخطاء. | اختبار تكاملي يدوي على جهازين حقيقيين. |
| **FR-03** | **AC-01.1** | اختفاء القرين تلقائياً من الواجهة بعد مرور 45 ثانية خمول في الذاكرة. | مؤقت زمني على جهازين حقيقيين. |
| **FR-04** | **AC-01.2** | إعادة تهيئة وربط المقابس تلقائياً واستمرار الاكتشاف عند تغير عنوان IP الشبكة. | إيقاف وتشغيل Wi-Fi ومراقبة السجلات. |
| **FR-05, FR-06** | **AC-02** | تسليم الرسائل وتشفيرها وتخزينها في قاعدة بيانات الطرف الآخر مع إرسال إشعار استلام `MSG_ACK`. | فحص الشاشة وقاعدة البيانات في كلا الطرفين. |
| **FR-07** | **AC-02.1** | تحديث محتوى الرسالة وظهور إشارة "معدلة" لدى المستلم فور استلام حزمة التعديل. | تعديل رسالة ومراقبة التحديث في الطرف الآخر. |
| **FR-08** | **AC-02.2** | ظهور اقتباس الرسالة الأصلية بشكل سليم فوق فقاعة الرد لدى الطرفين. | إجراء رد والتحقق من عرض بيانات الاقتباس. |
| **FR-09** | **AC-02.3** | ظهور مؤشر "يكتب الآن" واختفاؤه تلقائياً بعد توقف الكتابة. | الكتابة في حقل الإدخال ومراقبة شاشة القرين. |
| **FR-10, FR-11** | **AC-02.4** | تحول علامات الصح إلى صحين رماديين عند الوصول، وصحين ملونين عند القراءة. | فتح المحادثة ومراقبة تغير الأيقونة لدى المرسل. |
| **FR-12** | **AC-02.5** | حذف الرسالة من قاعدة البيانات المحلية للجهاز وحذف ملفها إن وجد. | فحص اختفاء الرسالة محلياً بعد إعادة التشغيل. |
| **FR-13** | **AC-02.6** | حذف الرسالة من أجهزة الأقران شبكياً (متطلب مقترح غير منفذ حالياً). | اختبار استجابة حزمة الحذف بعد برمجتها مستقبلاً. |
| **FR-14, FR-15, FR-16** | **AC-03** | إرسال واستقبال ملفات تدفقياً عبر TCP المنفذ 8891 وحفظها في التنزيلات بنجاح. | إرسال ملف وفحصه في الجهاز المستقبل. |
| **FR-17** | **AC-03.1** | استئناف نقل الملف من موضع الإزاحة السابق بعد انقطاع الاتصال دون تلف. | محاكاة انقطاع شبكي عند 50% ثم الضغط على استئناف. |
| **FR-18** | **AC-03.2** | منع حفظ الملفات في أي مسارات تراجعية خارج مجلد التنزيلات المخصص. | اختبار اسم ملف يحمل مسار `../`. |
| **FR-19** | **AC-03.3** | تطابق SHA-256 للملف بعد الاستلام مع ملف المرسل الأصلي. | مقارنة Hash الملفين بعد تنفيذ ميزة الفحص. |
| **FR-20** | **AC-04** | رنين هاتف المستقبل وتدفق صوت نقي في الاتجاهين عبر UDP 8889 عند قبول المكالمة. | تجربة مكالمة صوتية بين جهازين. |
| **FR-21** | **AC-04.1** | عرض تدفق الفيديو بسلاسة مع دعم التبديل بين الكاميرا الأمامية والخلفية. | تجربة مكالمة فيديو حية. |
| **FR-22** | **AC-04.2** | اتصال 3 أجهزة وسماع أصوات المشاركين معاً بوضوح بفضل مزج المسارات الصوتية. | مكالمة جماعية متعددة الأطراف. |
| **FR-23** | **AC-04.3** | تسجيل رسالة صوتية وإرسالها وتشغيلها بنجاح مع عرض الموجات الصوتية. | تسجيل مقطع صوتي والاستماع له في الجهاز الآخر. |
| **FR-24** | **AC-04.4** | ظهور شاشة الهاتف كتدفق فيديو لدى الهاتف الثاني بدلاً من الكاميرا. | تفعيل مشاركة الشاشة ومراقبة العرض. |
| **FR-25, FR-26** | **AC-05** | بقاء الرسائل والغرف وتفضيلات المظهر محفوظة بعد إغلاق التطبيق وإعادة تشغيله. | إعادة تشغيل التطبيق وفحص الشاشات. |
| **FR-27** | **AC-05.1** | ظهور إشعار في شريط النظام وفتح شاشة الاتصال عند ورود مكالمة والشاشة مغلقة. | قفل الشاشة وإجراء مكالمة واردة. |
| **FR-28, FR-29** | **AC-06** | تشفير الحزم كبيانات عشوائية عبر AES-256-GCM وفك تشفيرها السليم لدى المستقبل. | التقاط الحزم عبر Wireshark والتحقق من Ciphertext. |
| **FR-30** | **AC-06.1** | إسقاط أي حزمة يتم تعديل بايت واحد فيها فوراً وعدم معالجتها لعدم تطابق وسام المصادقة. | إرسال حزمة مشوهة والتحقق من إرجاع `null`. |
| **FR-31** | **AC-06.2** | عدم قبول الانضمام للغرفة المحمية إلا إذا تطابقت قيمة تجزئة SHA-256 لكلمة المرور محلياً. | إدخال كلمة مرور خاطئة ثم صحيحة محلياً. |

---

## 4. Requirements to Tests (ربط المتطلبات بالاختبارات المؤتمتة)

> [!NOTE]
> كما هو موثق في ملف الأساس `docs/Baseline.md`، مجلد الاختبارات الحالي (`app/src/test/` و `app/src/androidTest/`) كان يحتوي على اختبارات القالب الافتراضية الفارغة (`ExampleUnitTest` و `ExampleInstrumentedTest`)، وقد تمت إضافة فئة اختبارات محرك التشفير `LocalCryptoEngineTest.kt` وتنفيذها بنجاح عبر Gradle.
> 
> **بيان تنفيذ الاختبارات المؤتمتة الفعلي (Actual Automated Test Execution Statement):**  
> تم حل تعارض تبعيات مهام Gradle بنجاح وتم الوصول لمرحلة التجميع وتنفيذ الاختبارات:
> - `:app:kspDebugKotlin` تم تنفيذه.
> - `:app:compileDebugKotlin` تم تنفيذه.
> - `:app:compileDebugJavaWithJavac` تم تنفيذه.
> - `:app:kspDebugUnitTestKotlin` تم تنفيذه.
> - `:app:compileDebugUnitTestKotlin` تم تنفيذه.
> - `:app:testDebugUnitTest` نجح.
> - **نتيجة البناء:** `BUILD SUCCESSFUL in 8m 41s` (34 actionable tasks: 34 executed).
> - **إحصائيات الاختبار:** Total tests: 10 | Passed: 10 | Failed: 0 | Skipped: 0.
> - **مسار تقرير الاختبار:** `app/build/reports/tests/testDebugUnitTest/index.html`.
> 
> **ملاحظة منهجية بشأن التغطية:** لم يتم قياس التغطية الكودية المؤتمتة لعدم تشغيل أو ضبط أداة JaCoCo أو أي تقرير تغطية مكافئ، وبالتالي تُصنف حالة التغطية لجميع المتطلبات بـ `Not Measured` وليس بنسبة رقمية. نتيجة (10/10) تخص فقط اختبارات JVM لفئة `LocalCryptoEngineTest` ولا تمثل نسبة تغطية لكامل المشروع، ولا تشكل تحققاً أمنياً أو ميدانياً شاملاً.

| Requirement ID | Title | Automated Test File | Test Case Name | Current Test Status | Test Result | Test Gap Action Required |
|---|---|---|---|---|---|---|
| **FR-01** | اكتشاف الأجهزة | *None* | *None* | Missing - Test Required | Not Run | محاكاة حزم PING وبث UDP |
| **FR-02** | تحديث الأقران | *None* | *None* | Missing - Test Required | Not Run | فحص تحديث `peersMap` في الذاكرة |
| **FR-03** | مهلة خمول القرين | *None* | *None* | Missing - Test Required | Not Run | اختبار وحدة لمؤقت انتهاء الصلاحية |
| **FR-04** | إعادة ربط الشبكة | *None* | *None* | Missing - Test Required | Not Run | محاكاة استدعاءات NetworkCallback |
| **FR-05** | إرسال رسالة فردية | *None* | *None* | Missing - Test Required | Not Run | اختبار تشفير وبناء حزمة CHAT_MSG |
| **FR-06** | رسائل الغرف | *None* | *None* | Missing - Test Required | Not Run | اختبار بث الحزمة عبر المنفذ 8888 |
| **FR-07** | تعديل الرسائل | *None* | *None* | Missing - Test Required | Not Run | اختبار تحديث المحتوى وتوليد EDIT_MSG |
| **FR-08** | الرد والاقتباس | *None* | *None* | Missing - Test Required | Not Run | اختبار إسناد حقول الاقتباس للرسالة |
| **FR-09** | مؤشر الكتابة | *None* | *None* | Missing - Test Required | Not Run | اختبار إضافة وإزالة مؤشرات typingMap |
| **FR-10** | إشعار التسليم | *None* | *None* | Missing - Test Required | Not Run | اختبار معالجة MSG_ACK وترقية الحالة |
| **FR-11** | إشعار القراءة | *None* | *None* | Missing - Test Required | Not Run | اختبار معالجة MSG_READ وتحديث الحالة |
| **FR-12** | الحذف المحلي | *None* | *None* | Missing - Test Required | Not Run | اختبار استعلام Room deleteMessage |
| **FR-13** | الحذف الشبكي | *None* | *None* | Missing - Test Required | Not Run | بناء الميزة المفقودة ثم إنشاء اختبارها |
| **FR-14** | إرسال الملفات TCP | *None* | *None* | Missing - Test Required | Not Run | اختبار تدفق مقبس TCP عبر Mock Socket |
| **FR-15** | استقبال الملفات | *None* | *None* | Missing - Test Required | Not Run | اختبار كتابة التدفق في ملف مؤقت |
| **FR-16** | تجزئة الملف لـ Chunks | *None* | *None* | Missing - Test Required | Not Run | اختبار قياس استهلاك الذاكرة وتجزئة البايتات |
| **FR-17** | استئناف النقل | *None* | *None* | Missing - Test Required | Not Run | اختبار استئناف القراءة من موضع Offset |
| **FR-18** | تعقيم المسارات | *None* | *None* | Missing - Test Required | Not Run | اختبار وحدة لاستبعاد مسارات `../` |
| **FR-19** | فحص تجزئة الملف | *None* | *None* | Missing - Test Required | Not Run | بناء ميزة مقارنة SHA-256 واختبارها |
| **FR-20** | مكالمات صوتية | *None* | *None* | Missing - Test Required | Not Run | اختبار إشارات جلسات المكالمة الصوتية |
| **FR-21** | مكالمات فيديو | *None* | *None* | Missing - Test Required | Not Run | اختبار التقاط وتشفير وبث إطارات الفيديو |
| **FR-22** | مكالمات جماعية | *None* | *None* | Missing - Test Required | Not Run | اختبار وحدة لدالة mixPcmFrames |
| **FR-23** | ملاحظات صوتية | *None* | *None* | Missing - Test Required | Not Run | اختبار تسجيل وتشغيل ملفات الصوت AAC |
| **FR-24** | مشاركة الشاشة | *None* | *None* | Missing - Test Required | Not Run | اختبار إطلاق خدمة ScreenCaptureService |
| **FR-25** | دوام قاعدة البيانات | *None* | *None* | Missing - Test Required | Not Run | اختبار Room Database عبر In-Memory DB |
| **FR-26** | حفظ التفضيلات | *None* | *None* | Missing - Test Required | Not Run | اختبار DataStore Preferences |
| **FR-27** | قنوات الإشعارات | *None* | *None* | Missing - Test Required | Not Run | اختبار قنوات NotificationManager |
| **FR-28** | تشفير AES-256-GCM | `app/src/test/java/com/example/network/LocalCryptoEngineTest.kt` | `encryptThenDecryptRestoresPlaintext()`<br>`encryptingSamePlaintextProducesDifferentCiphertext()`<br>`nonceLengthIsTwelveBytes()`<br>`chunkNonceDeterminism()`<br>`chunkEncryptAndDecryptRoundTrip()` | Passed on JVM | Passed | تم التحقق على مستوى JVM Unit Tests (10/10)؛ يتطلب التحقق الميداني والـ Instrumented لاحقاً على أجهزة حقيقية |
| **FR-29** | فك تشفير AES-256-GCM | `app/src/test/java/com/example/network/LocalCryptoEngineTest.kt` | `encryptThenDecryptRestoresPlaintext()`<br>`arabicUtf8RoundTrip()`<br>`emptyOrSmallPayloadBehavior()`<br>`jsonRoundTrip()`<br>`chunkEncryptAndDecryptRoundTrip()` | Passed on JVM | Passed | تم التحقق على مستوى JVM Unit Tests (10/10)؛ يتطلب التحقق الميداني والـ Instrumented لاحقاً على أجهزة حقيقية |
| **FR-30** | رفض الحزم المشوهة | `app/src/test/java/com/example/network/LocalCryptoEngineTest.kt` | `tamperedCiphertextIsRejected()`<br>`wrongKeyIsRejected()`<br>`emptyOrSmallPayloadBehavior()`<br>`jsonRoundTrip()`<br>`chunkEncryptAndDecryptRoundTrip()` | Passed on JVM | Passed | تم التحقق على مستوى JVM Unit Tests (10/10)؛ يتطلب التحقق الميداني والـ Instrumented لاحقاً على أجهزة حقيقية |
| **FR-31** | تجزئة كلمات المرور | *None* | *None* | Missing - Test Required | Not Run | اختبار وحدة لحساب SHA-256 في MainViewModel |

---

## 5. Requirements to Non-Functional Requirements (ربط المتطلبات بالمتطلبات غير الوظيفية)

| NFR ID | Requirement | Source in SRS | Related Files | Verification Method | Current Status | Target Status |
|---|---|---|---|---|---|---|
| **NFR-01** | زمن استجابة تسليم الرسائل (<200ms) | SRS: Section 7.1 | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | قياس زمن انتقال حزمة الرسالة وحزمة MSG_ACK بشبكة محلية | Current Status: Proposed Performance Target (Measurement Not Performed) | Target Status: Verified through two-device latency measurement |
| **NFR-02** | كمون الصوت الحي في المكالمات (<150ms) | SRS: Section 7.1 | `app/src/main/java/com/example/network/AudioEngine.kt` | قياس الفارق الزمني بين الإدخال والإخراج الصوتي (Round-trip) | Current Status: Proposed Performance Target (Measurement Not Performed) | Target Status: Verified through round-trip audio latency measurement |
| **NFR-03** | كفاءة استهلاك الذاكرة وتدفق الملفات | SRS: Section 7.1 | `app/src/main/java/com/example/network/FileTransferEngine.kt` | مراقبة استهلاك Memory Profiler أثناء نقل ملفات بأحجام تفوق 500MB | Current Status: Implemented - Not Tested / Needs Measurement & Documentation | Target Status: Verified through Android Studio Profiler benchmark |
| **NFR-04** | الصمود أمام انقطاع الشبكة المفاجئ | SRS: Section 7.2 | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | اختبار إطفاء Wi-Fi أثناء المكالمة والتحقق من عدم حدوث Crash | Current Status: Implemented - Not Tested | Target Status: Verified through physical network resilience testing |
| **NFR-05** | استئناف نقل الملفات عند انقطاع الاتصال | SRS: Section 7.2 | `app/src/main/java/com/example/network/FileTransferEngine.kt` | قطع الاتصال عند 50% واستئنافه وفحص سلامة الملف الناتج | Current Status: Implemented - Not Tested / Needs Empirical Testing | Target Status: Verified through disconnect-resume hash integrity test |
| **NFR-06** | عزل أخطاء المقابس واستمرار الاستماع | SRS: Section 7.2 | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | ضخ حزم تالفة في مقبس الاستماع والتأكد من بقاء الخيط نشطاً | Current Status: Implemented - Not Tested | Target Status: Verified through socket stress and malformed packet injection test |
| **NFR-07** | سرية وسلامة الحزم عبر AES-256-GCM | SRS: Section 7.3 | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | التقاط الحزم ومحاولة التلاعب والتأكد من فشل المصادقة | Current Status: Implemented - Not Tested / Defective Secret Management (Dynamic Key Exchange غير موجود حالياً) | Target Status: Requires future security design and testing |
| **NFR-08** | عدم طباعة الأسرار في السجلات | SRS: Section 7.3 | كافة ملفات الكود المصدري | مراجعة استدعاءات Logcat وتطهير أي طباعة للمفاتيح أو المحتوى | Current Status: Needs Refactoring | Target Status: Verified through automated logcat security audit |
| **NFR-09** | فصل المسؤوليات المعمارية | SRS: Section 7.4 | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/ui/MainViewModel.kt` | إعادة هيكلة الأصناف الضخمة وتطبيق نمط المستودعات | Current Status: Violated - God Classes Existing | Target Status: Refactored under Clean Architecture with Repository pattern |
| **NFR-10** | قابلية المكونات للاختبار المعزول | SRS: Section 7.4 | كافة ملفات الكود المصدري | توفير Interfaces و Dependency Injection لتسهيل الاختبارات | Current Status: Missing | Target Status: Established through Testable Interface Architecture |
| **NFR-11** | دعم اللغة العربية والاتجاه RTL | SRS: Section 7.5 | `AndroidManifest.xml`<br>ملفات واجهة Compose | فحص محاذاة العناصر والنصوص على أجهزة مختلفة باللغة العربية | Current Status: Implemented - Not Tested (Layout RTL Configured in Manifest) | Target Status: Verified on UI/RTL visual layout inspection |
| **NFR-12** | وضوح مؤشرات حالة الرسائل | SRS: Section 7.5 | `app/src/main/java/com/example/ui/components/ChatMessageBubble.kt` | مراقبة انتقال الأيقونات (صح واحد، صحين رماديين، صحين أزرقين) | Current Status: Implemented - Not Tested | Target Status: Verified through two-device test |
| **NFR-13** | التوافق مع إصدارات أندرويد (API 24 - 36) | SRS: Section 7.6 | `app/build.gradle.kts` | تشغيل التطبيق واختبار دوال التوافق على مستويات API مختلفة | Current Status: Configured in Gradle - Runtime Compatibility Not Tested | Target Status: Verified on selected Android API levels |

---

## 6. Edge Case Traceability (تتبع الحالات الحدية)

| EC ID | Edge Case | Expected Behavior | Current Handling | Related Files | Test Required | Status |
|---|---|---|---|---|---|---|
| **EC-01** | القرين المستهدف غير متصل بالشبكة | بقاء الرسالة معلقة دون انهيار التطبيق مع إشعار المستخدم | ترسل الرسالة عبر UDP دون رد وتبقى بحالة صح واحد | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/ui/MainViewModel.kt` | Missing - Test Required | Partially Handled - Not Tested |
| **EC-02** | انقطاع Wi-Fi أثناء المكالمة | إيقاف المكالمة بسلام وعرض شريط تحذيري | إيقاف المقابس عبر `networkCallback` وعرض `NetworkStatusBanner` | `app/src/main/java/com/example/network/LocalP2PEngine.kt`<br>`app/src/main/java/com/example/ui/components/NetworkStatusBanner.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-03** | تغير IP الجهاز أثناء التشغيل (DHCP) | إعادة ربط المقابس فوراً وبث Ping لتحديث الأقران | استدعاء `restartNetworkDiscovery()` في `networkCallback` | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-04** | وصول حزمة مشوهة أو متلاعب بها | فشل المصادقة وإسقاط الحزمة بصمت دون فكها | التقاط الاستثناء في `LocalCryptoEngine.decrypt` وإرجاع `null` | `app/src/main/java/com/example/network/LocalCryptoEngine.kt`<br>`app/src/main/java/com/example/network/LocalP2PEngine.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-05** | وصول حزمة مشفرة بمفتاح خاطئ | رفض فك التشفير التلقائي لعدم تطابق وسام المصادقة | التقاط `AEADBadTagException` وإرجاع `null` | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-06** | محاولة إرسال رسالة فارغة | تعطيل زر الإرسال ومنع توليد الحزمة | التحقق عبر `content.isNotBlank()` في شاشة المحادثة | `app/src/main/java/com/example/ui/screens/ChatScreen.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-07** | وصول حزم رسائل مكررة | تجاهل الرسالة المكررة وعدم تكرارها بالقاعدة | استراتيجية `OnConflictStrategy.REPLACE` أو التحقق بالـ ID | `app/src/main/java/com/example/data/local/AppDatabase.kt`<br>`app/src/main/java/com/example/ui/MainViewModel.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-08** | وصول حزمة بنوع غير معروف | تجاهل الحزمة بأمان دون حدوث Crash | استخدام فرع `else` أو تجاهل الحالات غير المعرفة في `when (type)` | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-09** | محاولة إرسال ملف محذوف | تنبيه المستخدم بعدم وجود الملف وإلغاء النقل | فحص `file.exists()` قبل فتح تدفق النقل | `app/src/main/java/com/example/network/FileTransferEngine.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-10** | امتلاء التخزين أثناء استلام ملف | إيقاف النقل بأمان وحذف الملف غير المكتمل | التقاط `IOException` (مع قصور في الفحص المسبق للمساحة) | `app/src/main/java/com/example/network/FileTransferEngine.kt` | Missing - Test Required | Partially Handled - Not Tested |
| **EC-11** | انقطاع الشبكة أثناء نقل ملف واستئنافه | استئناف النقل من موضع آخر بايت مسجل | إرسال الإزاحة الحالية والبدء من موضع `offset` | `app/src/main/java/com/example/network/FileTransferEngine.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-12** | إدخال كلمة مرور خاطئة للغرفة | رفض الدخول وعرض رسالة خطأ للمستخدم | مقارنة `SHA-256(password)` مع `passwordHash` محلياً | `app/src/main/java/com/example/ui/MainViewModel.kt` -> دالة `joinRoom()` | Missing - Test Required | Implemented - Not Tested |
| **EC-13** | رفض منح أذونات الكاميرا/الميكروفون | تعطيل وظائف الاتصال وعرض توضيح للحاجة للإذن | فحص الأذونات عبر Accompanist Permissions وعرض طلب الإذن | `app/src/main/java/com/example/MainActivity.kt`<br>`app/src/main/java/com/example/ui/screens/PeersScreen.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-14** | إنهاء العملية في الخلفية (Process Death) | استعادة البيانات والرسائل بأمان عند إعادة الفتح | استرجاع السجلات من Room (مع فقدان الجلسات في الذاكرة) | `app/src/main/java/com/example/data/local/AppDatabase.kt`<br>`app/src/main/java/com/example/MainActivity.kt` | Missing - Test Required | Partially Handled - Not Tested |
| **EC-15** | استقبال رسائل من قرين محظور | إسقاط الحزم الواردة منه فوراً وتجاهلها | التحقق من `isPeerBlocked(senderId)` في مستهل المعالجة | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | Missing - Test Required | Implemented - Not Tested |
| **EC-16** | الاتصال بقرين منخرط في مكالمة أخرى | إرسال حزمة `CALL_BUSY` أو دعم انتظار المكالمة | معالجة حزمة `CALL_INVITE` مع دعم `CALL_BUSY` و `_callWaitingInvite` | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | Missing - Test Required | Implemented - Not Tested |

---

## 7. Security Traceability (التتبع الأمني والتحليل التشفيري)

| Security Item | Component / File | Class / Method | Algorithm / Mechanism | Current Status | Test / Verification |
|---|---|---|---|---|---|
| **AES-256-GCM** | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `LocalCryptoEngine.encrypt()`<br>`LocalCryptoEngine.decrypt()` | تشفير متناظر بنمط GCM ووسام مصادقة 128-bit | Implemented - JVM Unit Tests Passed | تم التحقق بنجاح عبر اختبارات JVM في `LocalCryptoEngineTest.kt` (Passed: 10/10)؛ التحقق الميداني على أجهزة حقيقية لا يزال مطلوباً. |
| **PBKDF2WithHmacSHA256** | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `LocalCryptoEngine.deriveKey()` | اشتقاق مفتاح 256-bit بعدد 12,000 دورة تكرارية وملح ثابت | Implemented - Present, Direct Test Not Confirmed | الدالة خاصة (private) وتُستدعى ضمنياً عبر تهيئة المفتاح الافتراضي؛ لا يوجد اختبار وحدة مباشر ومعزول لاشتقاق المفتاح. |
| **SHA-256** | `app/src/main/java/com/example/ui/MainViewModel.kt`<br>`app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `MainViewModel.hashPassword()`<br>`LocalCryptoEngine.deriveChunkNonce()` | تجزئة وحيدة الاتجاه لكلمات مرور الغرف محلياً واشتقاق Nonce كتل الملفات | Implemented - Present, Direct Test Not Confirmed | تم اختبار `deriveChunkNonce` ضمن اختبارات `LocalCryptoEngineTest.kt`، بينما دالة `hashPassword` في `MainViewModel` لا يوجد لها اختبار وحدة مباشر حتى الآن. |
| **GCM Nonce (12-byte)** | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `LocalCryptoEngine.generateNonce()`<br>`LocalCryptoEngine.deriveChunkNonce()` | توليد Nonce عشوائي (12 بايت) للحزم، أو مشتق قطعياً لقطع الملفات | Implemented - JVM Unit Tests Passed | تم التحقق بنجاح من طول الـ Nonce وعشوائيته وحتمية nonces الكتل عبر `LocalCryptoEngineTest.kt` (Passed). |
| **GCM Authentication Tag** | `app/src/main/java/com/example/network/LocalCryptoEngine.kt` | نمط `AES/GCM/NoPadding` | وسام مصادقة 128-bit للتحقق من سلامة الحزمة وأصالتها | Implemented - JVM Unit Tests Passed | تم التحقق بنجاح من رفض الحزم المشوهة وتعديل وسام المصادقة وإرجاع `null` عبر `LocalCryptoEngineTest.kt` (Passed). |
| **Android Keystore Identity Key** | `app/src/main/java/com/example/network/KeystoreIdentityManager.kt` | `KeystoreIdentityManager` | توليد مفتاح هوية EC P-256 داخل AndroidKeyStore والتوقيع عبر SHA256withECDSA | **Implemented - Not Verified on Device** | تم تنفيذ إدارة مفتاح هوية الجهاز كـ Phase 2؛ Keystore يُستخدم لمفتاح هوية فقط؛ اختبارات الأجهزة: Keystore Instrumented Test: Not Executed / Environment Blocked (لا يوجد جهاز متصل أو بيئة تشغيل مدعومة حالياً). |
| **Replay Protection** | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | فحص جزئي للرسائل | جدول أرقام تسلسلية وأختام زمنية لمنع إعادة بث الحزم الملتقطة | **Missing / Not Implemented** | Context-bound signatures and timestamps do not provide sequence-number-based replay rejection. |
| **Dynamic Key Exchange (ECDH + HKDF)** | `app/src/main/java/com/example/network/EcdhEngine.kt`<br>`app/src/main/java/com/example/network/PeerSessionKeyManager.kt`<br>`app/src/main/java/com/example/network/LocalP2PEngine.kt` | `EcdhEngine`<br>`PeerSessionKeyManager`<br>`LocalP2PEngine` | تبادل مفاتيح الجلسات عبر ECDH (منحنى NIST P-256) واشتقاق مفاتيح AES-256 عبر HKDF-SHA256 (RFC 5869) | **Implemented - Partially Verified (JVM Tests Passed: 12/12)** | تم تنفيذ محرك ECDH وإدارة مفاتيح الجلسات بنجاح؛ نجحت اختبارات الوحدة على JVM (EcdhEngineTest: 8/8, PeerSessionKeyManagerTest: 4/4). |
| **Network Handshake** | `app/src/main/java/com/example/network/LocalP2PEngine.kt` | `LocalP2PEngine.initiateKeyExchange()` | تبادل حزم المصافحة الثنائية (KEY_EXCHANGE_INIT, KEY_EXCHANGE_REPLY, KEY_EXCHANGE_ACK) | **Integrated in Code - Two-Device Wi-Fi Verification Pending** | تم دمج بروتوكول المصافحة التلقائي كودياً في المحرك؛ التحقق الشامل عبر شبكة Wi-Fi على جهازين فيزيائيين لا يزال قيد الانتظار. |
| **Context-Bound Signature** | `app/src/main/java/com/example/network/HandshakeCryptoUtils.kt` | `HandshakeCryptoUtils.buildCanonicalSignatureInput()` | ربط التوقيع الرقمي بسياق الجلسة الثنائي الحتمي (Domain Tag, Protocol Version, Sender, Receiver, Session, Timestamp, Ephemeral Key) | **Implemented - JVM Tested; Network Verification Pending** | تم التحقق باختبارات JVM (8/8 في HandshakeHardeningTest)؛ التحقق الشبكي الكامل على أجهزة حقيقية لا يزال قيد الانتظار. |
| **TOFU (Trust On First Use)** | `app/src/main/java/com/example/network/PeerTrustStore.kt` | `PeerTrustStore`<br>`PersistentPeerTrustStore` | تخزين وحساب بصمة SHA-256 لمفتاح الهوية وتثبيتها ورفض تبديل المفاتيح المسجلة | **Implemented - JVM Tested; First-Connection MITM Protection Requires SAS or Equivalent Out-of-Band Verification** | تم التحقق باختبارات JVM؛ حماية الاتصال الأول ضد MITM تتطلب آلية SAS أو تحققاً خارج النطاق. |
| **MITM Protection** | `app/src/main/java/com/example/network/PeerTrustStore.kt`<br>`app/src/main/java/com/example/network/KeystoreIdentityManager.kt` | `PeerTrustStore`<br>`KeystoreIdentityManager` | حماية ضد هجمات رجل في المنتصف عبر مفاتيح الهوية وتثبيت البصمات | **Partial - TOFU Implemented, SAS Not Implemented** | الحماية فعالة للاتصالات اللاحقة بعد التثبيت الأول، وتبقى غير مكتملة في أول اتصال بدون توثيق SAS. |
| **Downgrade Protection** | `app/src/main/java/com/example/network/PeerSessionKeyManager.kt`<br>`app/src/main/java/com/example/network/LocalP2PEngine.kt` | `PeerSessionKeyManager`<br>`LocalP2PEngine.sendJsonToIp()` | وسم الأقران بـ HANDSHAKE_FAILED ورفض الإرسال التراجعي للرسائل الأحادية | **Partially Implemented - Enforced on Direct Send Path; Incoming Discovery/Receive Path Requires Further Hardening** | مفروض برمجياً على مسار الإرسال الفردي لمنع استخدام المفتاح المشترك عند فشل المصافحة؛ مسار الاستقبال والاكتشاف الوارد يتطلب تحصيناً إضافياً. |
| **File Integrity Checksum** | `app/src/main/java/com/example/network/FileTransferEngine.kt` | غير موجود | حساب ومقارنة SHA-256 Checksum للملف الكامل بعد اكتمال النقل | **Missing / Not Implemented** | المقارنة الكاملة بين SHA-256 للملف الأصلي والمستلم غير موجودة في الكود الحالي؛ يجب تنفيذها مستقبلاً ثم إنشاء اختبار لها. |
| **Key Management Abstraction** | `app/src/main/java/com/example/network/KeyProvider.kt`<br>`app/src/main/java/com/example/network/LocalCryptoEngine.kt` | `KeyProvider`<br>`LegacyStaticKeyProvider` | واجهة تجريدية تعزل اشتقاق المفتاح وإدارته عن عمليات التشفير (Phase 1) | Implemented (Phase 1 Complete) | تم عزل المفتاح بنجاح؛ نجحت اختبارات التراجع 10/10 مع الحفاظ على التوافق الكامل. |
| **Default Passphrase** | `app/src/main/java/com/example/network/KeyProvider.kt` | الحقل الثابت `DEFAULT_PASSPHRASE` في `LegacyStaticKeyProvider` | عبارة مرور نصية ثابتة معزولة خلف واجهة `KeyProvider` | **Defective Secret Management (Isolated in LegacyStaticKeyProvider)** | LegacyStaticKeyProvider لا يزال يستخدم العبارة الثابتة القديمة؛ حزم البث العام لا تزال تعتمد عليها. |

---

## 8. Coverage Summary (ملخص التغطية ومؤشرات القياس)

> [!IMPORTANT]
> **بيان قياس التغطية الكودية وبيئة التنفيذ:**  
> "Automated code coverage was not measured because no JaCoCo or equivalent coverage report was executed. The current test status is therefore reported as Not Measured, not as a numerical percentage."  
> 
> > The 10/10 result applies only to the executed LocalCryptoEngine JVM unit tests. It is not a project-wide coverage percentage and does not constitute full security verification.

### ملخص حالة الاختبارات والتغطية:

| Item | Status |
|---|---|
| Test Source Added | Yes (`LocalCryptoEngineTest.kt`) |
| Tests Executed | Yes (10 tests executed via Gradle) |
| Test Result | Passed: 10/10 (0 failed, 0 skipped) |
| Build Status | BUILD SUCCESSFUL in 8m 41s |
| Code Coverage | Not Measured (No JaCoCo report executed) |

### أبعاد التغطية الحالية للمشروع:

| Coverage Dimension | Current Evidence | Status |
|---|---|---|
| Crypto Unit Test Source | LocalCryptoEngineTest.kt | Added |
| Crypto Unit Test Execution | 10 tests executed | Passed: 10/10 |
| Full Feature Test Suite | No complete suite | Not Complete |
| Instrumented Tests | No feature-level device validation | Not Verified |
| Manual Two-Device Tests | Not completed | Missing - Test Required |
| JaCoCo Coverage | No report executed | Not Measured |

> [!WARNING]
> نتيجة 10/10 لا تعني أن جميع متطلبات المشروع مختبرة؛ بل تعكس حصراً نجاح اختبارات JVM لفئة `LocalCryptoEngineTest`، بينما بقية متطلبات المشروع (من FR-01 إلى FR-27 و FR-31) لا تزال تتطلب أجنحة اختبارات وظيفية وميدانية وتكاملية.

---

## 9. Gaps and Recommended Future Work (فجوات التتبع والعمل المستقبلي الموصى به)

### توثيق إصلاح بيئة البناء (Build Notes & Gradle Task Dependency):
> A circular Gradle task dependency was resolved in `app/build.gradle.kts` by excluding test KSP tasks from the `normalizeGeneratedDigits` finalizer relationship. The fix affected build configuration only and did not modify production Kotlin code.

### توثيق المرحلة الأولى لتحسين الأمان (Security Plan - Phase 1 Status):
> - **KeyProvider abstraction added:** تم إنشاء الواجهة التجريدية `KeyProvider` في `app/src/main/java/com/example/network/KeyProvider.kt` لعزل إدارة واشتقاق المفاتيح عن محرك التشفير `LocalCryptoEngine`.
> - **LegacyStaticKeyProvider still uses the old static passphrase:** تم توفير فئة `LegacyStaticKeyProvider` للحفاظ على التوافق التام (نفس `DEFAULT_PASSPHRASE` ونفس بارامترات PBKDF2).
> - **No ECDH or Keystore implemented yet:** لم يتم تضمين بروتوكول تبادل المفاتيح الديناميكي (ECDH) أو Android Keystore في هذه المرحلة للحفاظ على استقرار البروتوكول.
> - **Existing crypto regression tests passed 10/10:** تم تنفيذ اختبارات التشفير القائمة `LocalCryptoEngineTest` بنجاح كامل (10 passed, 0 failed) دون أي انكسار في السلوك التشفيري.

### توثيق المرحلة الثانية لتحسين الأمان (Security Plan - Phase 2 Status):
> - **Android Keystore Identity Key:** Implemented - Not Verified on Device (تم إنشاء `KeystoreIdentityManager` لمفتاح هوية طويل الأمد EC P-256 وتوقيع SHA256withECDSA).
> - **Keystore Scope:** Keystore يُستخدم لمفتاح هوية الجهاز فقط وتوقيع المصادقة، ولا يُستخدم لتخزين مفتاح AES للشبكة.
> - **AES network key:** ما زال مفتاح AES-256 يعتمد على `LegacyStaticKeyProvider` دون تغيير.
> - **No ECDH or Session Keys:** لم يتم تنفيذ تبادل مفاتيح ECDH أو Session Keys في هذه المرحلة.
> - **No Replay Protection or File Checksum:** لم يتم تنفيذ Replay Protection أو فحص سلامة الملفات الكامل بعد.
> - **Keystore Instrumented Test:** Not Executed / Environment Blocked (لا يوجد جهاز أندرويد حقيقي أو محاكي متصل في بيئة الاختبار الحالية، وتم تجهيز أجنحة اختبارات `KeystoreIdentityManagerTest` للتشغيل المستقبلي).
> - **Crypto Regression Tests:** لا تزال اختبارات التشفير القائمة `LocalCryptoEngineTest` ناجحة بنسبة 10/10 على مستوى JVM.

### توثيق المرحلة الثالثة لتحسين الأمان (Security Plan - Phase 3 Status):
> - **Phase 3A: Standalone ECDH Engine:** تم إنشاء `EcdhEngine.kt` لإنشاء أزواج مفاتيح EC NIST P-256 وحساب السر المشترك واشتقاق مفاتيح AES-256 عبر RFC 5869 HKDF-SHA256، مع نجاح كامل لكافة اختبارات الوحدة المعزولة على JVM في `EcdhEngineTest` (بنتيجة 8/8 Passed).
> - **Phase 3B: Handshake Integration:** تم إنشاء `PeerSessionKeyManager.kt` لإدارة وتخزين مفاتيح الجلسات لكل قرين مع نجاح اختبارات `PeerSessionKeyManagerTest` (بنتيجة 4/4 Passed).
> - **Network Protocol Integration:** تم دمج حزم المصافحة (`KEY_EXCHANGE_INIT`, `KEY_EXCHANGE_REPLY`, `KEY_EXCHANGE_ACK`) وتوثيق الهوية عبر `KeystoreIdentityManager` داخل `LocalP2PEngine.kt`.
> - **Hybrid Encryption Architecture:** الاتصالات الفردية المباشرة (Unicast) تستخدم مفتاح الجلسة المشتق الخاص بالقرين بمجرد اكتمال المصافحة، بينما اتصالات الاكتشاف والبث العام (Multicast/Broadcast) تستخدم مفتاح الشبكة لمنع انكسار الاكتشاف التلقائي.
> - **Overall Crypto Tests Status:** نجحت كافة اختبارات التشفير بنسبة 100% (22/22 tests passed: 10 LocalCryptoEngineTest + 8 EcdhEngineTest + 4 PeerSessionKeyManagerTest).

### توثيق تصليب المصافحة والأمان المتقدم (Phase 3B Hardening Status):
> - **Context-Bound Signature (`HandshakeCryptoUtils.kt`):** Implemented - JVM Tested; Network Verification Pending (ربط التوقيع الرقمي للمفتاح المؤقت بسياق الجلسة الثنائي الحتمي عبر Domain Tag, Protocol Version, Sender ID, Receiver ID, Session ID, Timestamp, Ephemeral Public Key).
> - **TOFU Identity Trust Establishment (`PeerTrustStore.kt`):** Implemented - JVM Tested; First-Connection MITM Protection Requires SAS or Equivalent Out-of-Band Verification (تثبيت بصمة مفتاح الهوية ورفض تغييرها في الاتصالات اللاحقة).
> - **MITM Protection:** Partial - TOFU Implemented, SAS Not Implemented (الحماية ضد رجل في المنتصف متحققة جزئياً للأجهزة الموثقة سابقاً ومفتوحة في أول اتصال لغياب SAS).
> - **Downgrade Attack Prevention (`PeerSessionKeyManager.kt` & `LocalP2PEngine.kt`):** Partially Implemented - Enforced on Direct Send Path; Incoming Discovery/Receive Path Requires Further Hardening (وسم القرين بـ HANDSHAKE_FAILED ومنع التراجع التلقائي إلى المفتاح الافتراضي في الإرسال الفردي المباشر، بينما يتطلب مسار الاستقبال مزيداً من التحصين).
> - **Network Handshake:** Integrated in Code - Two-Device Wi-Fi Verification Pending (دمج بروتوكول تبادل المفاتيح التلقائي في المحرك بانتظار التحقق الميداني على جهازين حقيقيين عبر Wi-Fi).
> - **Replay Protection:** Missing / Not Implemented (Context-bound signatures and timestamps do not provide sequence-number-based replay rejection).
> - **File Integrity Checksum:** Missing / Not Implemented (فحص ومقارنة SHA-256 للملف الكامل بعد النقل غير منفذ حالياً).
> - **Session Key Scope:**
>   - Direct Chat: sessionKey if available, otherwise legacy behavior remains where applicable.
>   - Room Chat: networkKey.
>   - File Transfer: networkKey.
>   - Audio: networkKey.
>   - Video: networkKey.
>   - Screen Sharing: networkKey.
>   - Discovery: networkKey.
>   - Handshake: mixed/legacy bootstrap path.
> - **Hardening Unit Tests Status:** تم إنشاء وتنفيذ 8 اختبارات وحدة معزولة جديدة في `HandshakeHardeningTest` بنجاح كامل 100% (8/8 Passed على JVM).


1. **فجوة استراتيجية الاختبارات (Comprehensive Testing Strategy Gap):**
   - المتطلبات الـ31 تحتاج استراتيجية اختبار متعددة الأنواع، تشمل Unit Tests للمنطق والتشفير، وIntegration Tests للشبكة وقاعدة البيانات، وInstrumented أو Manual Two-Device Tests للوظائف التي تعتمد على Android hardware والشبكة الحقيقية.

2. **فجوة إدارة المفاتيح والتشفير (Key Management & Exchange Gap):**
   - إزالة العبارة الثابتة `DEFAULT_PASSPHRASE`، ودراسة تصميم وتبني بروتوكول تبادل مفاتيح خفيف مناسب لبيئة P2P (مثل ECDH) كمقترح مستقبلي يتطلب تصميماً واختباراً أمنياً، مع استبعاد المنظومات المركزية المعقدة.

3. **فجوة التحقق من سلامة الملفات (File Integrity Checksum Gap):**
   - بناء وظيفة لحساب ومقارنة بصمة SHA-256 للملفات المنقولة بعد اكتمال النقل للتأكد من خلوها من التلف أو التعديل.

4. **فجوة الحذف الشبكي للرسائل (Remote Message Deletion Gap):**
   - تصميم وتنفيذ حزمة شبكية مخصصة لحذف الرسائل من أجهزة الأقران لضمان تكامل ميزة الحذف على مستوى الشبكة بالكامل.

5. **فجوة الفصل المعماري وقابلية الصيانة (Architectural Refactoring Gap):**
   - تفكيك الأصناف الضخمة (`LocalP2PEngine.kt` و `MainViewModel.kt`) وفصل طبقات الاتصال والوسائط والبيانات عبر واجهات تجريدية (Clean Architecture) لتمكين الاختبار المعزول والقياس الموثوق.
