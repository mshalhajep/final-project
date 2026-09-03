# 🎓 دليل المناقشة الشفوي وأسئلة الدكتور المتوقعة: مشروع LOCAL CONTACT
## دليلك السريع لكل (مجلد، ملف، كلاس، دالة، ومتغير) في كود التطبيق

---

## 🧭 خريطة المجلدات والطبقات الرئيسية في المشروع

```
app/src/main/java/com/example/
│
├── network/              ⬅️ قلب المشروع (الشبكة، المنافذ، السوكيت، التشفير، الوسائط)
│   ├── LocalP2PEngine.kt                 (الاستكشاف 8888، نبضات القلب، الرسائل، إشارات المكالمات)
│   ├── AudioEngine.kt                    (المكالمات الصوتية 8889، تسجيل PCM، تشغيل AudioTrack)
│   ├── VideoEngine.kt                    (مكالمات الفيديو والشاشة 8890، تقطيع Chunks، تجميع الإطارات)
│   ├── FileTransferEngine.kt             (نقل الملفات 8891 TCP، الإيقاف والاستئناف، ملفات .part)
│   ├── ScreenCaptureService.kt           (خدمة بث الشاشة الأمامية MediaProjection)
│   ├── LocalCryptoEngine.kt              (تشفير AES-256-GCM، اشتقاق المفاتيح والـ Nonce)
│   ├── NetworkServiceDiscoveryEngine.kt  (بروتوكول mDNS واكتشاف الخدمات عبر الشبكة)
│   ├── NetworkUtils.kt                   (جلب IP المحلي وعنوان البث Broadcast Address)
│   └── CallToneManager.kt                (نغمات الرنين والانتظار والاهتزاز)
│
├── ui/                   ⬅️ واجهة المستخدم ومنطق العرض
│   ├── MainViewModel.kt                  (الرابط بين الواجهة ومحرك الشبكة وقاعدة البيانات)
│   ├── screens/                          (الشاشات: ChatScreen, RoomsScreen, PeersScreen, AuthScreen)
│   ├── components/                       (المكونات: فقاعات الرسائل، النافذة العائمة PiP، شبكة الفيديو)
│   └── dialogs/                          (نوافذ الاتصال والمكالمات الفردية والجماعية)
│
├── data/                 ⬅️ قاعدة البيانات المحلية (Room Database)
│   ├── local/AppDatabase.kt              (قاعدة البيانات، الجداول، والـ Migrations)
│   └── ThemePreferencesRepository.kt    (تفضيلات المظهر عبر DataStore)
│
├── model/                ⬅️ كائنات البيانات (Data Classes)
│   └── Models.kt                         (ChatMessage, Peer, RoomInfo, CallSession)
│
└── utils/                ⬅️ الأدوات المساعدة
    ├── LocalNotificationManager.kt       (نظام الإشعارات بنمط MessagingStyle)
    └── StorageUtils.kt                   (إدارة المجلدات وحفظ الصور والملفات في المعرض)
```

---

## ❓ الأسئلة الشفوية المتوقعة من الدكتور وإجاباتها الدقيقة من الكود

---

### س1: أين يتم الاستكشاف الشبكي (Discovery)؟ في أي ملف وما هو المتغير والمنفذ؟
* **المجلد:** `com/example/network/`
* **الملف:** `LocalP2PEngine.kt`
* **الكلاس:** `LocalP2PEngine`
* **المنفذ الثابت:**
  ```kotlin
  const val PORT_DISCOVERY = 8888
  ```
* **المتغير (السوكيت):**
  ```kotlin
  private var discoverySocket: DatagramSocket? = null
  ```
* **الدالة التي ترسل حزمة الاستكشاف (Ping):**
  - دالة `broadcastPresence()` أو `sendDiscoveryPing()`
  - ترسل حزمة UDP Broadcast إلى العنوان `255.255.255.255` أو عنوان Subnet Broadcast المستخرج من `NetworkUtils.getBroadcastAddress()`.
  - محتوى الحزمة: كائن JSON بنوع `MessageType.DISCOVERY_PING` يحمل: معرف الجهاز `peerId`، الاسم `username`، والصورة الرمزية `avatarBase64`.
* **الدالة التي تستقبل وتعالج الرد (Pong):**
  - دالة `startDiscoveryListener()`: حلقة غير منتهية في Coroutine تستمع على السوكيت عبر `discoverySocket?.receive(packet)`.
  - دالة `handleIncomingPacket()`: تفحص الحزمة، وإذا كانت `DISCOVERY_PING` ترد فوراً بحزمة `DISCOVERY_PONG` إلى IP المرسل مباشرة (Unicast).
* **المتغير الذي يخزن الأجهزة المكتشفة وتراقبه الشاشة:**
  - القائمة في المحرك: `private val peerMap = ConcurrentHashMap<String, Peer>()`
  - متغير الـ StateFlow للواجهة:
  ```kotlin
  private val _peers = MutableStateFlow<List<Peer>>(emptyList())
  val peers: StateFlow<List<Peer>> = _peers.asStateFlow()
  ```

---

### س2: كيف تتم المحادثة وإرسال الرسائل النصية؟ أين الكود؟
* **المجلد:** `com/example/network/`
* **الملف:** `LocalP2PEngine.kt`
* **دالة الإرسال:**
  ```kotlin
  fun sendMessage(targetIp: String, content: String, type: MessageType, ...)
  ```
* **كيف ترسل؟**
  - تنشئ كائن `ChatMessage` وتشفره باستخدام `LocalCryptoEngine.encrypt()`.
  - تحوله لحزمة `DatagramPacket` وترسله عبر `discoverySocket` إلى عنوان IP الطرف الآخر على منفذ `8888`.
* **دالة استقبال الرسالة وحفظها في قاعدة البيانات:**
  - في `handleIncomingPacket()` داخل `LocalP2PEngine.kt`.
  - عند استلام نوع `CHAT_MESSAGE`:
    1. يتم فك التشفير.
    2. يتم إرسال إشعار تأكيد استلام (Delivery Ack).
    3. تمرر الرسالة للـ ViewModel عبر `onMessageReceivedListener`.
    4. يقوم `MainViewModel.kt` بحفظها في قاعدة بيانات Room عبر `messageDao.insertMessage(message)`.

---

### س3: أين يتم نقل الملفات؟ وما هو المنفذ والسوكيت؟ وكيف يعمل استئناف التحميل؟
* **المجلد:** `com/example/network/`
* **الملف:** `FileTransferEngine.kt`
* **الكلاس:** `FileTransferEngine`
* **المنفذ الثابت:**
  ```kotlin
  const val PORT_FILE_TRANSFER = 8891 // TCP
  ```
* **المتغيرات الأساسية:**
  - سيرفر الاستماع للملفات: `private var serverSocket: ServerSocket? = null`
  - خريطة تتبع تقدم التنزيل: `private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())`
  - خريطة السوكيتات النشطة للإيقاف: `private val activeDownloadSockets = ConcurrentHashMap<String, Socket>()`
* **كيف يطلب العميل تحميل الملف واستئنافه؟ (الدالة والمتغير):**
  - دالة العميل: `downloadFileFromPeer(peerIp, fileId, fileName, ...)`
  - مسار الملف المؤقت: `File(cacheDir, "p2p_downloads_temp/${fileId}_${fileName}.part")`
  - فحص البايتات الموجودة مسبقاً: `val existingBytes = if (partFile.exists()) partFile.length() else 0L`
  - بروتوكول الطلب النصي المرسل للسيرفر:
    ```text
    DOWNLOAD <fileId> <existingBytes>\n
    ```
* **كيف يتعامل السيرفر مع الطلب؟ (دالة الإرسال):**
  - دالة `handleClientDownloadRequest(clientSocket: Socket)`:
  - تقرأ السطر، وتفتح الملف المطلوب عبر `FileInputStream`.
  - تعمل `fileIn.skip(existingBytes)` لتجاوز البايتات المحملة سابقاً!
  - ترد على العميل: `OK <totalSize> <fileName> <startOffset>\n`.
  - ترسل كتل مشفرة بحجم 64KB عبر `BufferedOutputStream` داخل كتلة `.use { ... }`.
* **دالة إيقاف التحميل مؤقتاً (Cancel/Pause):**
  - دالة `cancelDownload(messageId: String)`: تجلب السوكيت من `activeDownloadSockets[messageId]?.close()`، فتحتفظ بملف `.part` دون حذفه ليتم استئنافه لاحقاً.

---

### س4: أين كود المكالمات الصوتية؟ وما هي المتغيرات المسؤولة عن الميكروفون والسماعة؟
* **المجلد:** `com/example/network/`
* **الملف:** `AudioEngine.kt`
* **الكلاس:** `AudioEngine`
* **المنفذ الثابت:** `const val PORT_AUDIO = 8889` (UDP)
* **المتغير المسؤول عن تسجيل الصوت من الميكروفون:**
  ```kotlin
  private var audioRecord: AudioRecord? = null
  ```
  - الإعدادات: تردد 16,000Hz، أحادي القناة (`CHANNEL_IN_MONO`)، ترميز `ENCODING_PCM_16BIT`.
* **المتغير المسؤول عن تشغيل الصوت في السماعة:**
  ```kotlin
  private var audioTrack: AudioTrack? = null
  ```
* **حلقة التسجيل والإرسال (دالة التسجيل):**
  - دالة `startRecordingInternal()`: تقرأ عينات الصوت في مصفوفة `val buffer = ByteArray(BUFFER_SIZE)` (حجم 960 بايت = 30 ملي ثانية صوت)، وترسلها فوراً كحزمة UDP إلى منفذ `8889` لهاتف الطرف الآخر.
* **حلقة الاستقبال والتشغيل (دالة التشغيل):**
  - دالة `startPlaybackInternal()`: تستقبل الحزم في مصفوفة وترسلها لـ `audioTrack?.write(buffer, 0, bytesRead)`.

---

### س5: أين كود مكالمات الفيديو وبث الكاميرا؟
* **المجلد:** `com/example/network/`
* **الملف:** `VideoEngine.kt`
* **الكلاس:** `VideoEngine`
* **المنفذ الثابت:** `const val PORT_VIDEO = 8890` (UDP)
* **المتغيرات الأساسية للكاميرا:**
  - محرك CameraX: `private var cameraProvider: ProcessCameraProvider? = null`
  - محلل الإطارات: `private var imageAnalyzer: ImageAnalysis? = null`
* **كيف يتم ضغط وتقطيع إطارات الفيديو؟ (دالة الإرسال):**
  - دالة `sendDirectCameraBitmap(bitmap)` أو `sendDirectScreenShareBitmap(bitmap)`:
  - تضغط الـ Bitmap إلى صيغة JPEG عبر `bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)`.
  - تقسم المصفوفة الناتجة إلى كتل صغيرة بحجم `CHUNK_SIZE = 1200 بايت` (أقل من MTU الشبكة 1500 بايت).
  - ترسل كل كتلة كحزمة UDP مع Header يحمل: `frameId`، `chunkIndex`، `totalChunks`.
* **دالة إعادة تجميع الإطار عند المستقبل:**
  - دالة `reassembleFrame(senderIp, packet)`:
  - تجمع الكتل في كائن `FrameReassembly`.
  - فور اكتمال جميع الكتل (`receivedChunks == totalChunks`)، تحولها إلى صورة عبر `BitmapFactory.decodeByteArray()`.
  - وتحدّث الـ StateFlow المربوط بالواجهة: `_remoteVideoFrames`.

---

### س6: أين كود مشاركة الشاشة الحية (Screen Sharing)؟ وما هي الخدمة المسؤولة؟
* **المجلد:** `com/example/network/`
* **الملف:** `ScreenCaptureService.kt`
* **الكلاس:** `ScreenCaptureService : Service()`
* **المتغيرات المسؤولة عن التقاط الشاشة:**
  ```kotlin
  private var mediaProjection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var imageReader: ImageReader? = null
  ```
* **المعالج الذي ينقل الإطارات إلى محرك الفيديو:**
  ```kotlin
  companion object {
      var onFrameCaptured: ((Bitmap) -> Unit)? = null
  }
  ```
* **الآلية:**
  - `ImageReader` يلتقط أحدث صورة من الشاشة `acquireLatestImage()`.
  - تحول الصورة إلى `Bitmap` وتمرر عبر `onFrameCaptured` إلى دالة `videoEngine.sendDirectScreenShareBitmap()`.
* **نوع الخدمة في AndroidManifest.xml:**
  ```xml
  <service
      android:name=".network.ScreenCaptureService"
      android:foregroundServiceType="mediaProjection" />
  ```

---

### س7: أين يتم التشفير في التطبيق؟ وما هي الخوارزمية والمتغيرات؟
* **المجلد:** `com/example/network/`
* **الملف:** `LocalCryptoEngine.kt`
* **الكائن:** `object LocalCryptoEngine`
* **الخوارزمية:**
  - `AES-256-GCM` (`AES/GCM/NoPadding`)
  - طول مفتاح التشفير: 256 بت (32 بايت).
  - طول علامة المصادقة (Tag): 128 بت (`GCM_TAG_BITS = 128`).
  - حجم الـ Nonce: 12 بايت (`NONCE_SIZE = 12`).
* **الدوال الأساسية:**
  - `encrypt(plainText: String): String`
  - `decrypt(cipherText: String): String?`
  - `encryptChunk(plain: ByteArray, fileId: String, chunkIndex: Long, sessionId: String): ByteArray`
* **كيف حلت مشكلة Nonce Reuse في التشفير؟**
  - عبر دمج `fileId` مع `sessionId` عشوائي و `chunkIndex` واشتقاق SHA-256 منها لضمان عدم تكرار الـ Nonce أبداً لنفس الملف.

---

### س8: كيف يرتبط الـ UI (واجهات Compose) بمحرك الشبكة وقاعدة البيانات؟
* **المجلد:** `com/example/ui/`
* **الملف:** `MainViewModel.kt`
* **الكلاس:** `MainViewModel : AndroidViewModel(application)`
* **المتغير الذي يحتوي على محرك الشبكة بالكامل:**
  ```kotlin
  val engine = LocalP2PEngine(application.applicationContext)
  ```
* **المتغير الذي يحتوي على قاعدة البيانات:**
  ```kotlin
  val database = AppDatabase.getDatabase(application.applicationContext)
  val messageDao = database.chatMessageDao()
  ```
* **كيف تستجيب الواجهة للمتغيرات (Reactivity)؟**
  - الـ ViewModel يعرّض البيانات كـ `StateFlow` مثل `peers = engine.peers` و `chatMessages`.
  - الشاشات (مثل `ChatScreen.kt` و `PeersScreen.kt`) تجمع هذه البيانات عبر:
  ```kotlin
  val peers by viewModel.peers.collectAsState()
  ```
  - بمجرد دخول هاتف جديد للشبكة وتحديث `peerMap` في الخلفية، يتغير الـ StateFlow وتعيد شاشة Jetpack Compose رسم نفسها تلقائياً (Recomposition).

---

## 🎯 ملخص سريع في جدول للمراجعة قبل الامتحان بدقيقة:

| السؤال | الملف | الكلاس | المتغير الأهم | المنفذ |
|---|---|---|---|---|
| **الاستكشاف والرسائل** | `LocalP2PEngine.kt` | `LocalP2PEngine` | `discoverySocket`, `_peers` | `8888 UDP` |
| **المكالمات الصوتية** | `AudioEngine.kt` | `AudioEngine` | `audioRecord`, `audioTrack` | `8889 UDP` |
| **الفيديو والكاميرا** | `VideoEngine.kt` | `VideoEngine` | `cameraProvider`, `_remoteVideoFrames` | `8890 UDP` |
| **مشاركة الشاشة** | `ScreenCaptureService.kt` | `ScreenCaptureService` | `mediaProjection`, `imageReader` | `8890 UDP` |
| **نقل الملفات واستئنافها**| `FileTransferEngine.kt` | `FileTransferEngine` | `serverSocket`, `_downloadProgressMap` | `8891 TCP` |
| **الأمان والتشفير** | `LocalCryptoEngine.kt` | `LocalCryptoEngine` | `_networkKey`, `AES/GCM/NoPadding` | — |
| **منطق الربط المركزي** | `MainViewModel.kt` | `MainViewModel` | `engine`, `database` | — |
