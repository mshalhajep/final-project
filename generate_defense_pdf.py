import os
import subprocess

html_content = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>تقرير مشروع LOCAL CONTACT - هندسة البرمجيات والتشفير المتقدم</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800;900&family=Tajawal:wght@400;500;700;800&family=Fira+Code:wght@400;500;600&display=swap');

  :root {
    --primary: #0f2b5c;
    --primary-light: #1e40af;
    --secondary: #0d9488;
    --accent: #2563eb;
    --dark: #0f172a;
    --gray-bg: #f8fafc;
    --gray-border: #cbd5e1;
    --gray-text: #475569;
    --success: #059669;
    --success-bg: #ecfdf5;
    --warning: #d97706;
    --warning-bg: #fffbeb;
    --danger: #dc2626;
    --danger-bg: #fef2f2;
    --info: #0284c7;
    --info-bg: #f0f9ff;
  }

  * {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
  }

  body {
    font-family: 'Cairo', 'Tajawal', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    color: var(--dark);
    background-color: #ffffff;
    line-height: 1.55;
    font-size: 12.5px;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  @page {
    size: A4;
    margin: 12mm 12mm 12mm 12mm;
  }

  .page-break {
    page-break-before: always;
    break-before: page;
  }

  .no-break {
    page-break-inside: avoid;
    break-inside: avoid;
  }

  h1, h2, h3 {
    break-after: avoid;
    page-break-after: avoid;
  }

  /* Cover Page */
  .cover {
    height: 96vh;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    padding: 30px 25px 25px 25px;
    border: 2px solid #cbd5e1;
    border-radius: 12px;
    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
    position: relative;
    overflow: hidden;
  }

  .cover::before {
    content: "";
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 8px;
    background: linear-gradient(90deg, #0f2b5c, #2563eb, #0d9488);
  }

  .cover-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid var(--gray-border);
    padding-bottom: 12px;
  }

  .cover-header .inst-title {
    font-size: 13px;
    font-weight: 700;
    color: var(--gray-text);
  }

  .badge-academic {
    background: #e0e7ff;
    color: #3730a3;
    padding: 4px 14px;
    border-radius: 9999px;
    font-size: 11.5px;
    font-weight: 800;
  }

  .cover-body {
    margin: auto 0;
    text-align: center;
    padding: 10px 0;
  }

  .app-icon-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 68px;
    height: 68px;
    background: linear-gradient(135deg, #1e40af, #0f2b5c);
    color: #ffffff;
    border-radius: 18px;
    font-size: 30px;
    font-weight: 900;
    box-shadow: 0 10px 25px rgba(30, 64, 175, 0.25);
    margin-bottom: 18px;
  }

  .cover-title {
    font-size: 25px;
    font-weight: 900;
    color: var(--primary);
    margin-bottom: 8px;
    line-height: 1.35;
  }

  .cover-subtitle {
    font-size: 15px;
    font-weight: 700;
    color: #2563eb;
    margin-bottom: 16px;
  }

  .cover-desc {
    font-size: 12.5px;
    color: var(--gray-text);
    max-width: 640px;
    margin: 0 auto 24px auto;
    line-height: 1.65;
  }

  .meta-cards-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    max-width: 580px;
    margin: 0 auto;
    text-align: right;
  }

  .meta-card {
    background: #ffffff;
    border: 1px solid var(--gray-border);
    border-radius: 8px;
    padding: 10px 14px;
    display: flex;
    flex-direction: column;
    box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  }

  .meta-label {
    font-size: 11px;
    color: #64748b;
    font-weight: 600;
    margin-bottom: 2px;
  }

  .meta-val {
    font-size: 13.5px;
    font-weight: 800;
    color: var(--dark);
  }

  .cover-footer {
    border-top: 1px solid var(--gray-border);
    padding-top: 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 11.5px;
    color: var(--gray-text);
  }

  /* Section Styling */
  .section {
    margin-bottom: 16px;
  }

  .page-header-line {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e2e8f0;
    padding-bottom: 6px;
    margin-bottom: 12px;
    font-size: 10.5px;
    color: #64748b;
  }

  h1.sec-title {
    font-size: 16px;
    font-weight: 800;
    color: var(--primary);
    border-bottom: 2px solid #e2e8f0;
    padding-bottom: 5px;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  h1.sec-title .num-badge {
    background: var(--primary);
    color: #ffffff;
    font-size: 11px;
    width: 22px;
    height: 22px;
    border-radius: 6px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  h2.sub-title {
    font-size: 13.5px;
    font-weight: 700;
    color: var(--primary-light);
    margin: 12px 0 6px 0;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  p {
    margin-bottom: 8px;
    text-align: justify;
    color: #1e293b;
    font-size: 12px;
  }

  /* Tables */
  table {
    width: 100%;
    border-collapse: collapse;
    margin: 8px 0 12px 0;
    font-size: 11px;
  }

  th, td {
    border: 1px solid #cbd5e1;
    padding: 5px 8px;
    text-align: right;
    vertical-align: middle;
  }

  th {
    background-color: #f1f5f9;
    color: var(--primary);
    font-weight: 700;
    font-size: 11px;
  }

  tr:nth-child(even) {
    background-color: #f8fafc;
  }

  /* Badges */
  .badge {
    display: inline-block;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 700;
    white-space: nowrap;
  }

  .badge-success { background-color: var(--success-bg); color: var(--success); border: 1px solid #a7f3d0; }
  .badge-warning { background-color: var(--warning-bg); color: var(--warning); border: 1px solid #fde68a; }
  .badge-danger { background-color: var(--danger-bg); color: var(--danger); border: 1px solid #fecaca; }
  .badge-info { background-color: var(--info-bg); color: var(--info); border: 1px solid #bae6fd; }

  /* Callouts */
  .callout {
    border-right: 4px solid;
    border-radius: 6px;
    padding: 8px 12px;
    margin: 8px 0;
    font-size: 11.5px;
  }

  .callout-info { background-color: var(--info-bg); border-color: var(--info); color: #0369a1; }
  .callout-success { background-color: var(--success-bg); border-color: var(--success); color: #065f46; }
  .callout-warning { background-color: var(--warning-bg); border-color: var(--warning); color: #92400e; }
  .callout-danger { background-color: var(--danger-bg); border-color: var(--danger); color: #991b1b; }

  .callout-title {
    font-weight: 800;
    font-size: 12px;
    margin-bottom: 3px;
  }

  .code-inline {
    font-family: 'Fira Code', Consolas, monospace;
    background-color: #f1f5f9;
    color: #b91c1c;
    padding: 1px 5px;
    border-radius: 4px;
    font-size: 10.5px;
    direction: ltr;
    unicode-bidi: embed;
    display: inline-block;
  }

  /* Pipeline Diagram */
  .pipeline-diagram {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #f8fafc;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    padding: 10px;
    margin: 10px 0;
    gap: 6px;
  }

  .pipe-step {
    flex: 1;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    padding: 6px 8px;
    text-align: center;
    box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  }

  .pipe-step.success {
    border-color: #a7f3d0;
    background: #f0fdf4;
  }

  .pipe-title {
    font-size: 11.5px;
    font-weight: 800;
    color: var(--primary);
    margin-bottom: 2px;
  }

  .pipe-sub {
    font-size: 10px;
    color: #64748b;
    line-height: 1.3;
  }

  .pipe-arrow {
    font-size: 14px;
    font-weight: 900;
    color: #94a3b8;
  }

  /* Highlight Grid */
  .highlight-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    margin: 8px 0;
  }

  .highlight-card {
    background: #ffffff;
    border: 1px solid var(--gray-border);
    border-top: 3px solid var(--primary-light);
    border-radius: 6px;
    padding: 8px 10px;
    box-shadow: 0 1px 2px rgba(0,0,0,0.02);
  }

  .highlight-card.crypto { border-top-color: var(--secondary); }
  .highlight-card.tests { border-top-color: var(--success); }
  .highlight-card.limits { border-top-color: var(--warning); }

  .highlight-title {
    font-weight: 700;
    font-size: 12px;
    color: var(--dark);
    margin-bottom: 2px;
  }

  .highlight-desc {
    font-size: 11px;
    color: var(--gray-text);
    line-height: 1.45;
  }

  /* QA Cards */
  .qa-card {
    background: #ffffff;
    border: 1px solid #cbd5e1;
    border-radius: 6px;
    margin-bottom: 8px;
    overflow: hidden;
  }

  .qa-question {
    background-color: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    padding: 7px 10px;
    font-weight: 700;
    font-size: 12px;
    color: var(--primary);
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .qa-answer {
    padding: 8px 10px;
    font-size: 11.5px;
    line-height: 1.55;
    color: #334155;
  }

  .qa-code-ref {
    background-color: #f1f5f9;
    border-radius: 4px;
    padding: 3px 6px;
    margin-top: 4px;
    font-family: 'Fira Code', monospace;
    font-size: 10.5px;
    direction: ltr;
    text-align: left;
    color: #1e293b;
    border-left: 3px solid var(--accent);
  }

  .checklist {
    list-style: none;
    padding: 0;
    margin: 6px 0;
  }

  .checklist li {
    position: relative;
    padding-right: 18px;
    margin-bottom: 4px;
    font-size: 11.5px;
  }

  .checklist li::before {
    content: "✔";
    position: absolute;
    right: 0;
    color: var(--success);
    font-weight: bold;
  }

  .checklist.limits li::before {
    content: "⚠";
    color: var(--warning);
  }

  .doc-footer {
    text-align: center;
    font-size: 10px;
    color: #94a3b8;
    margin-top: 15px;
    padding-top: 8px;
    border-top: 1px solid var(--gray-border);
  }

  .ref-box {
    direction: ltr;
    text-align: left;
    font-size: 10px;
    color: #475569;
    line-height: 1.5;
    padding-left: 8px;
  }
</style>
</head>
<body>

<!-- صفحة 1: الغلاف التنفيذي الفخم -->
<div class="cover">
  <div class="cover-header">
    <div class="inst-title">مشروع تخرج / تطبيق عملي متقدم — هندسة البرمجيات والتشفير</div>
    <div class="badge-academic">نسخة المناقشة والتسليم الأكاديمي</div>
  </div>

  <div class="cover-body">
    <div>
      <div class="app-icon-badge">LC</div>
    </div>
    
    <div class="cover-title">مقارنة التطوير المعماري في هندسة البرمجيات والتشفير المتقدم</div>
    <div class="cover-subtitle">مشروع تطبيق الاتصال المحلي المباشر (LOCAL CONTACT P2P)</div>
    
    <div class="cover-desc">
      وثيقة هندسية وأمنية تبرز التحول النوعي للتطبيق من نموذج أولي يعتمد على مفتاح تشفير ثابت إلى نظام شبكي P2P مهيكل وفق أحدث معايير هندسة البرمجيات (SRS, Baseline, RTM) ومحصن بمنظومة تشفير ديناميكية (AES-256-GCM, ECDH P-256, HKDF-SHA256, Android Keystore, TOFU).
    </div>

    <div class="meta-cards-grid">
      <div class="meta-card">
        <span class="meta-label">إعداد الطالب المنفذ:</span>
        <span class="meta-val" style="color: #1e40af; font-size: 15px;">مشعل</span>
      </div>
      <div class="meta-card">
        <span class="meta-label">لجنة المناقشة والتحكيم:</span>
        <span class="meta-val">أستاذ هندسة البرمجيات + أستاذ التشفير</span>
      </div>
      <div class="meta-card">
        <span class="meta-label">حالة بناء الحزمة (Build Status):</span>
        <span class="meta-val" style="color: var(--success);">BUILD SUCCESSFUL (APK جاهز)</span>
      </div>
      <div class="meta-card">
        <span class="meta-label">نتائج الاختبارات الآلية (Unit Tests):</span>
        <span class="meta-val" style="color: var(--success);">33 ناجح من أصل 33 (100% JVM)</span>
      </div>
    </div>
  </div>

  <div class="cover-footer">
    <div>مسار ملف التطبيق: <span class="code-inline">app/build/outputs/apk/debug/app-debug.apk</span></div>
    <div>العام الجامعي 2025 - 2026 م | إعداد الطالب: مشعل</div>
  </div>
</div>

<div class="page-break"></div>

<!-- صفحة 2: الملخص التنفيذي + جدول التغييرات الشامل للملفات -->
<div class="page-header-line">
  <span>مشروع LOCAL CONTACT P2P — تقرير التطوير الأكاديمي</span>
  <span>إعداد الطالب: مشعل | صفحة 2</span>
</div>

<div class="section">
  <h1 class="sec-title"><span class="num-badge">1</span> الملخص التنفيذي وأهداف المشروع</h1>
  
  <p>
    مشروع <strong>LOCAL CONTACT</strong> هو منصة اتصال مباشر بين أجهزة Android عبر الشبكة المحلية (Wi-Fi LAN أو Hotspot) دون الاعتماد على خادم مركزي أو إنترنت. قبل بدء التطوير، كان التطبيق يمتلك وظائف تشغيلية ولكن كان يفتقر إلى التوثيق المعماري الرسمي ويعتمد على <strong>مفتاح تشفير ثابت</strong> مشتق من عبارة مرور داخل الكود، مما يمثل ثغرة معمارية لكافة الجلسات المسجلة.
  </p>

  <p>
    تم تنفيذ التطوير عبر مسارين متوازيين: <strong>(1) هندسة البرمجيات:</strong> توثيق المتطلبات (SRS)، تثبيت خط الأساس (Baseline)، بناء مصفوفة التتبع (RTM)، وفصل إدارة المفاتيح بنمط Provider. <strong>(2) التشفير وأمن الاتصال:</strong> الحفاظ على خوارزمية AES-256-GCM، وإضافة تبادل المفاتيح المنحني ECDH P-256 مع دالة الاشتقاق HKDF-SHA256، وحفظ هوية الجهاز داخل معالج Android Keystore، وتثبيت الهوية بنموذج TOFU والتوقيع الحتمي.
  </p>
</div>

<div class="section">
  <h1 class="sec-title"><span class="num-badge">2</span> خريطة الملفات والمجلدات: ما تم إضافته وتعديله ولماذا؟</h1>
  
  <p>يوضح الجدول التالي كافة الملفات المضافة والمعدلة في المشروع والهدف الهندسي والأمني الدقيق منها:</p>

  <table>
    <thead>
      <tr>
        <th style="width: 25%;">المسار والملف</th>
        <th style="width: 12%;">نوع الإجراء</th>
        <th style="width: 23%;">المسؤولية</th>
        <th style="width: 40%;">لماذا تم الإجراء؟ (القيمة المضافة في المشروع)</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><span class="code-inline">docs/SRS.md</span></td>
        <td><span class="badge badge-info">ملف جديد</span></td>
        <td>مواصفات المتطلبات</td>
        <td>توثيق 31 متطلباً وظيفياً (FR-01 إلى FR-31) لربط الكود بمرجعية رسمية.</td>
      </tr>
      <tr>
        <td><span class="code-inline">docs/Baseline.md</span></td>
        <td><span class="badge badge-info">ملف جديد</span></td>
        <td>خط الأساس المعماري</td>
        <td>تحديد الحالة المرجعية للنظام قبل وبعد التحسينات لتسهيل الصيانة.</td>
      </tr>
      <tr>
        <td><span class="code-inline">docs/TraceabilityMatrix.md</span></td>
        <td><span class="badge badge-info">ملف جديد</span></td>
        <td>مصفوفة تتبع RTM</td>
        <td>ربط صارم: <em>متطلب ← ملف الكود ← كلاس الاختبار ← حالة الإنجاز</em>.</td>
      </tr>
      <tr>
        <td><span class="code-inline">.../network/KeyProvider.kt</span></td>
        <td><span class="badge badge-success">كود جديد</span></td>
        <td>واجهة تجريد المفاتيح</td>
        <td>فصل إدارة المفاتيح عن محرك التشفير بنمط Provider لتطبيق مبدأ DIP.</td>
      </tr>
      <tr>
        <td><span class="code-inline">.../network/EcdhEngine.kt</span></td>
        <td><span class="badge badge-success">كود جديد</span></td>
        <td>تبادل المفاتيح المنحني</td>
        <td>توليد مفاتيح P-256 واشتقاق مفاتيح جلسة ديناميكية عبر HKDF-SHA256.</td>
      </tr>
      <tr>
        <td><span class="code-inline">.../network/KeystoreIdentityManager.kt</span></td>
        <td><span class="badge badge-success">كود جديد</span></td>
        <td>هوية الجهاز بالعتاد</td>
        <td>إنشاء زوج مفاتيح EC غير قابل للتصدير داخل Android Keystore للتوقيع.</td>
      </tr>
      <tr>
        <td><span class="code-inline">.../network/HandshakeCryptoUtils.kt</span></td>
        <td><span class="badge badge-success">كود جديد</span></td>
        <td>التوقيع المرتبط بالسياق</td>
        <td>بناء مصفوفة بايت حتمية تضم سياق الجلسة لمنع هجمات إعادة البث.</td>
      </tr>
      <tr>
        <td><span class="code-inline">.../network/PeerTrustStore.kt</span></td>
        <td><span class="badge badge-success">كود جديد</span></td>
        <td>مخزن ثقة الأقران</td>
        <td>تخزين بصمات SHA-256 لمفاتيح الأجهزة بنموذج TOFU واكتشاف تغيرها.</td>
      </tr>
      <tr>
        <td><span class="code-inline">.../network/PeerSessionKeyManager.kt</span></td>
        <td><span class="badge badge-success">كود جديد</span></td>
        <td>إدارة دورة حياة الجلسة</td>
        <td>حفظ مفاتيح الجلسات وإدارة حالات الأمان ومنع هجمات Downgrade.</td>
      </tr>
      <tr>
        <td><span class="code-inline">.../network/LocalCryptoEngine.kt</span></td>
        <td><span class="badge badge-warning">تعديل هيكلي</span></td>
        <td>محرك التشفير المتماثل</td>
        <td>الاعتماد على <span class="code-inline">KeyProvider</span> والحفاظ الكامل على خوارزمية AES-256-GCM.</td>
      </tr>
      <tr>
        <td><span class="code-inline">.../network/LocalP2PEngine.kt</span></td>
        <td><span class="badge badge-warning">تعديل ودمج</span></td>
        <td>الاتصال والمصافحة</td>
        <td>دمج رسائل المصافحة الثلاثية وربط الدردشة المباشرة بمفاتيح الجلسات.</td>
      </tr>
      <tr>
        <td><span class="code-inline">app/src/test/.../</span></td>
        <td><span class="badge badge-info">اختبارات جديدة</span></td>
        <td>حزم اختبارات JVM</td>
        <td>4 ملفات اختبارات للمحرك، التبادل، الجلسات، والمصافحة (33 اختباراً ناجحاً).</td>
      </tr>
    </tbody>
  </table>
</div>

<div class="page-break"></div>

<!-- صفحة 3: هندسة البرمجيات والاختبارات الآلية -->
<div class="page-header-line">
  <span>مشروع LOCAL CONTACT P2P — تقرير التطوير الأكاديمي</span>
  <span>إعداد الطالب: مشعل | صفحة 3</span>
</div>

<div class="section">
  <h1 class="sec-title"><span class="num-badge">3</span> الجزء الأول: منهجية هندسة البرمجيات وإدارة الجودة</h1>

  <div class="pipeline-diagram">
    <div class="pipe-step">
      <div class="pipe-title">1. وثيقة المتطلبات</div>
      <div class="pipe-sub">docs/SRS.md<br>31 متطلباً وظيفياً ومعايير قبول</div>
    </div>
    <div class="pipe-arrow">←</div>
    <div class="pipe-step">
      <div class="pipe-title">2. الكود البرمجي</div>
      <div class="pipe-sub">فصل المفاتيح في KeyProvider ومبادئ SOLID</div>
    </div>
    <div class="pipe-arrow">←</div>
    <div class="pipe-step">
      <div class="pipe-title">3. مصفوفة التتبع</div>
      <div class="pipe-sub">docs/TraceabilityMatrix.md<br>ربط كل متطلب بالكود والفحص</div>
    </div>
    <div class="pipe-arrow">←</div>
    <div class="pipe-step success">
      <div class="pipe-title">4. اختبارات JVM</div>
      <div class="pipe-sub">33 / 33 فحصاً ناجحاً 100%<br>حزمة APK مبنية بنجاح</div>
    </div>
  </div>

  <h2 class="sub-title">1.3 إنجازات هندسة البرمجيات المحققة:</h2>
  <ul class="checklist">
    <li><strong>SRS.md:</strong> وثيقة رسمية للمتطلبات تشمل 31 متطلباً (FR-01 إلى FR-31) تغطي الاستكشاف، المراسلة، نقل الملفات، المكالمات، الأمان، وقاعدة البيانات.</li>
    <li><strong>Baseline.md:</strong> خط أساس معماري يوثق حالة النظام الأصلية، منافذ السوكيت (8888-8891)، تدفق الحزم، والفجوات المعروفة قبل التحسين.</li>
    <li><strong>TraceabilityMatrix.md:</strong> مصفوفة تتبع صارمة تمنع الخلط بين ما هو مختبر على JVM، وما هو مدمج في الكود، وما هو قيد التحسين المستقبلي.</li>
    <li><strong>KeyProvider Pattern:</strong> تجريد إدارة المفاتيح لعزل مسؤولية اشتقاق المفاتيح عن محرك التشفير وتسهيل دعم مفاتيح متعددة لاحقاً.</li>
  </ul>

  <h2 class="sub-title">2.3 جدول نتائج الاختبارات الآلية المنفذة (33 / 33 اختباراً ناجحاً):</h2>
  <table>
    <thead>
      <tr>
        <th style="width: 25%;">كلاس الاختبار</th>
        <th style="width: 48%;">ما يتم التحقق منه واختباره برمجياً</th>
        <th style="width: 12%;">العدد</th>
        <th style="width: 15%;">النتيجة</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><strong>LocalCryptoEngineTest</strong></td>
        <td>صحة AES-256-GCM، رفض المفتاح الخاطئ، كشف التلاعب بالنص المشفر وبالوسم (Tag)، اشتقاق Nonce الكتل، ودعم UTF-8 ونصوص JSON.</td>
        <td>10 فحوصات</td>
        <td><span class="badge badge-success">10/10 ناجح</span></td>
      </tr>
      <tr>
        <td><strong>EcdhEngineTest</strong></td>
        <td>توليد أزواج P-256 المؤقتة، إثبات اتفاق طرفين مستقلين على نفس السر المشترك، واشتقاق مفتاح جلسة متطابق عبر HKDF-SHA256.</td>
        <td>8 فحوصات</td>
        <td><span class="badge badge-success">8/8 ناجح</span></td>
      </tr>
      <tr>
        <td><strong>PeerSessionKeyManagerTest</strong></td>
        <td>إدارة جلسات الأقران، التحقق من صلاحية الجلسات النشطة، انتهاء صلاحية المصافحة بعد 30 ثانية، واسترجاع المفتاح الصحيح لكل جهاز.</td>
        <td>4 فحوصات</td>
        <td><span class="badge badge-success">4/4 ناجح</span></td>
      </tr>
      <tr>
        <td><strong>HandshakeHardeningTest</strong></td>
        <td>التحقق من التوقيع المرتبط بالسياق، كشف التلاعب في معرف الجلسة أو الوقت، وحفظ ومطابقة بصمة الهوية بمخزن ثقة TOFU.</td>
        <td>8 فحوصات</td>
        <td><span class="badge badge-success">8/8 ناجح</span></td>
      </tr>
      <tr>
        <td><strong>اختبارات تراكيب البيانات</strong></td>
        <td>فحوصات سلامة نماذج البيانات ونظام الكبسولات في المشروع.</td>
        <td>3 فحوصات</td>
        <td><span class="badge badge-success">3/3 ناجح</span></td>
      </tr>
      <tr style="background-color: #ecfdf5; font-weight: 800;">
        <td colspan="2"><strong>المجموع الكلي للاختبارات المنفذة والمحققة بنجاح تام:</strong></td>
        <td><strong>33 فحصاً</strong></td>
        <td><span class="badge badge-success">33 / 33 (100%)</span></td>
      </tr>
    </tbody>
  </table>

  <div class="callout callout-info">
    <div class="callout-title">دلالة نتيجة 33/33 الهندسية:</div>
    تثبت هذه النتيجة السلامة الخوارزمية للمحرك البرمجي وإدارة الجلسات على بيئة JVM، لكنها لا تعني اختبار الاتصال الميداني بين هاتفين حقيقيين عبر راوتر Wi-Fi.
  </div>
</div>

<div class="page-break"></div>

<!-- صفحة 4: التشفير وأمن الاتصال ونطاق المفاتيح -->
<div class="page-header-line">
  <span>مشروع LOCAL CONTACT P2P — تقرير التطوير الأكاديمي</span>
  <span>إعداد الطالب: مشعل | صفحة 4</span>
</div>

<div class="section">
  <h1 class="sec-title"><span class="num-badge">4</span> الجزء الثاني: التشفير المتقدم وأمن الاتصال الشبكي</h1>

  <p>
    كان النظام القديم يعتمد على مفتاح AES-256 ثابت مشتق عبر PBKDF2 من عبارة مرور موحدة داخل الكود. تم تطوير النظام ليعتمد على منظومة تشفير حديثة:
  </p>

  <div class="highlight-grid">
    <div class="highlight-card crypto">
      <div class="highlight-title">🔐 التشفير المتماثل: AES-256-GCM</div>
      <div class="highlight-desc">مفتاح 256 بت مع وسم مصادقة 128 بت ورقم عشوائي Nonce بطول 12 بايت، يضمن السرية وسلامة البيانات ويرفض أي تلاعب بالحزم.</div>
    </div>
    <div class="highlight-card crypto">
      <div class="highlight-title">⚡ تبادل المفاتيح: ECDH (NIST P-256)</div>
      <div class="highlight-desc">توليد مفاتيح مؤقتة وحساب السر المشترك رياضياً على المنحنى <span class="code-inline">secp256r1</span> محققاً سرية التوجيه المستقبلي (Forward Secrecy).</div>
    </div>
    <div class="highlight-card crypto">
      <div class="highlight-title">🎛️ اشتقاق المفاتيح: HKDF-SHA256</div>
      <div class="highlight-desc">تطبيق معيار RFC 5869 في مرحلتي Extract و Expand لاشتقاق مفاتيح جلسة عشوائية متماثلة ومحمية بسياق الجلسة.</div>
    </div>
    <div class="highlight-card crypto">
      <div class="highlight-title">🛡️ هوية العتاد: Android Keystore</div>
      <div class="highlight-desc">حفظ مفتاح الهوية غير المتماثل في العتاد الآمن (TEE) وتوقيع بيانات المصافحة بـ ECDSA دون إمكانية تصدير المفتاح الخاص.</div>
    </div>
  </div>

  <h2 class="sub-title">1.4 حماية المصافحة: التوقيع المرتبط بالسياق (Context-Bound) ونموذج TOFU</h2>
  <div class="highlight-grid">
    <div class="highlight-card">
      <div class="highlight-title">📜 التوقيع المرتبط بالسياق (Handshake Hardening)</div>
      <div class="highlight-desc">يتم تجميع حقول المصافحة في هيكل باينري حتمي يضم: (إصدار البروتوكول 2، معرف المرسل، المستقبل، الجلسة، والوقت، والمفتاح المؤقت). أي تعديل في سياق الجلسة يؤدي فوراً لفشل التحقق بالتوقيع الرقمي.</div>
    </div>
    <div class="highlight-card">
      <div class="highlight-title">🤝 تثبيت الهوية (Trust On First Use - TOFU)</div>
      <div class="highlight-desc">عند أول اتصال يُحفظ بصمة مفتاح القرين (SHA-256 Fingerprint) بمخزن الثقة. في الاتصالات اللاحقة، يُرفض الاتصال إذا تغير المفتاح، مما يحمي الأجهزة من انتحال الهوية بعد المصافحة الأولى.</div>
    </div>
  </div>

  <h2 class="sub-title">2.4 الشفافية الأكاديمية: مصفوفة نطاق المفاتيح الفعلي (Key Scope Matrix)</h2>
  <p>يوضح الجدول التالي ما تم تحويله فعلياً لمفاتيح الجلسات المشتقة، وما لا يزال يعمل بالمفتاح الشبكي القديم:</p>

  <table>
    <thead>
      <tr>
        <th>المسار / نوع البيانات</th>
        <th>المفتاح المستخدم حالياً</th>
        <th>المنفذ والبروتوكول</th>
        <th>حالة الحماية والأمان</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><strong>استكشاف الأجهزة (Discovery)</strong></td>
        <td>المفتاح الشبكي: <span class="code-inline">networkKey</span></td>
        <td>8888 (UDP Broadcast)</td>
        <td>طبيعي (مطلوب عاماً للاكتشاف الأولي للأجهزة).</td>
      </tr>
      <tr>
        <td><strong>الدردشة المباشرة (Direct Chat)</strong></td>
        <td>مفتاح الجلسة: <span class="code-inline">sessionKey</span></td>
        <td>8888 (UDP Unicast)</td>
        <td><span class="badge badge-success">محمي بجلسة ECDH ديناميكية لكل قرين</span></td>
      </tr>
      <tr>
        <td><strong>غرف المحادثة الجماعية (Rooms)</strong></td>
        <td>المفتاح الشبكي: <span class="code-inline">networkKey</span></td>
        <td>8888 (UDP Multicast)</td>
        <td>مسار قديم للتوافق (يحتاج Group Key مستقبلاً).</td>
      </tr>
      <tr>
        <td><strong>نقل وتبادل الملفات (Files)</strong></td>
        <td>المفتاح الشبكي: <span class="code-inline">networkKey</span></td>
        <td>8891 (TCP Stream)</td>
        <td>مشفر بكتل 64KB بمفتاح الشبكة القديم.</td>
      </tr>
      <tr>
        <td><strong>المكالمات الصوتية (Audio)</strong></td>
        <td>المفتاح الشبكي: <span class="code-inline">networkKey</span></td>
        <td>8889 (UDP Stream)</td>
        <td>مشفر بحزم PCM بمفتاح الشبكة القديم.</td>
      </tr>
      <tr>
        <td><strong>مكالمات الفيديو والشاشة</strong></td>
        <td>المفتاح الشبكي: <span class="code-inline">networkKey</span></td>
        <td>8890 (UDP Stream)</td>
        <td>مشفر بإطارات JPEG بمفتاح الشبكة القديم.</td>
      </tr>
    </tbody>
  </table>
</div>

<div class="page-break"></div>

<!-- صفحة 5: المقارنات الشاملة قبل وبعد والحدود الأكاديمية -->
<div class="page-header-line">
  <span>مشروع LOCAL CONTACT P2P — تقرير التطوير الأكاديمي</span>
  <span>إعداد الطالب: مشعل | صفحة 5</span>
</div>

<div class="section">
  <h1 class="sec-title"><span class="num-badge">5</span> المقارنات الشاملة (قبل وبعد) والأمانة الأكاديمية</h1>

  <h2 class="sub-title">1.5 مقارنة هندسة البرمجيات والتشفير:</h2>
  <table>
    <thead>
      <tr>
        <th>المجال</th>
        <th>قبل التحسين (الحالة السابقة)</th>
        <th>بعد التحسين (النسخة المنجزة)</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><strong>توثيق المتطلبات</strong></td>
        <td>فهم شفهي وضمني داخل الكود</td>
        <td>موثقة في <span class="code-inline">docs/SRS.md</span> لـ 31 متطلباً وظيفياً ومعايير قبول.</td>
      </tr>
      <tr>
        <td><strong>الخط المرجعي المعماري</strong></td>
        <td>غير موحد ويصعب تمييز الإضافات</td>
        <td>موثق في <span class="code-inline">docs/Baseline.md</span> كنقطة ارتكاز معمارية واضحة.</td>
      </tr>
      <tr>
        <td><strong>تتبع المتطلبات RTM</strong></td>
        <td>غير موجود</td>
        <td>مسار هندسي موثق في <span class="code-inline">docs/TraceabilityMatrix.md</span>.</td>
      </tr>
      <tr>
        <td><strong>تجريد إدارة المفاتيح</strong></td>
        <td>مقترن بشدة داخل محرك التشفير</td>
        <td>معزول ومجرد عبر واجهة <span class="code-inline">KeyProvider</span> وفق مبادئ SOLID.</td>
      </tr>
      <tr>
        <td><strong>مصدر مفتاح التشفير</strong></td>
        <td>عبارة مرور ثابتة مشتقة عبر PBKDF2</td>
        <td>تبادل ديناميكي ECDH P-256 واشتقاق HKDF-SHA256 لكل جلسة.</td>
      </tr>
      <tr>
        <td><strong>سرية التوجيه المستقبلي</strong></td>
        <td>منعدمة تماماً</td>
        <td>محققة في محادثات الجلسات (المفاتيح المؤقتة تُتلف فوراً).</td>
      </tr>
      <tr>
        <td><strong>هوية الجهاز المشفرة</strong></td>
        <td>غير موجودة (معرف عشوائي فقط)</td>
        <td>مفتاح EC غير قابل للتصدير داخل Android Keystore بتوقيع ECDSA.</td>
      </tr>
      <tr>
        <td><strong>حماية المصافحة من العبث</strong></td>
        <td>غير موجودة</td>
        <td>توقيع رقمي موحد مرتبط بالسياق (Context-Bound Signature).</td>
      </tr>
      <tr>
        <td><strong>التحقق من هوية الأقران</strong></td>
        <td>قبول أي جهاز على الشبكة</td>
        <td>نموذج Trust On First Use (TOFU) ومطابقة بصمات SHA-256.</td>
      </tr>
    </tbody>
  </table>

  <h2 class="sub-title">2.5 حدود التحقق والأمانة الأكاديمية (ما تبقى للتحسين المستقبلي):</h2>
  <div class="callout callout-warning">
    <div class="callout-title">حدود العمل الحالي والمسارات المفتوحة للتحسين:</div>
    <ul class="checklist limits">
      <li><strong>التحقق اليدوي SAS (Short Authentication String):</strong> حماية MITM في الاتصال الأول تظل "جزئية" لغياب مقارنة كود صوتي أو مسح QR بين المستخدمين في أول لقاء.</li>
      <li><strong>الحماية الكاملة من إعادة الإرسال (Replay Protection):</strong> التوقيع يغطي سياق المصافحة فقط، ولا توجد حتى الآن أرقام تسلسلية للحزم الشبكية (Sequence Numbers) مع نافذة رفض للمكرر.</li>
      <li><strong>فحص سلامة الملف كاملاً (Whole-File SHA-256):</strong> يتم التحقق من سلامة كل كتلة 64KB عبر AES-GCM Tag، ولكن لا يوجد فحص بصمة شامل للملف الكامل بعد تجميعه.</li>
      <li><strong>مفاتيح المجموعات (Group Keys):</strong> غرف المحادثة الجماعية ما زالت تستخدم المفتاح الشبكي القديم المشترك.</li>
      <li><strong>الاختبار الميداني المباشر:</strong> نجاح 33/33 اختباراً تم على بيئة JVM ولم يُختبر ميدانياً بين هاتفين حقيقيين عبر راوتر فيزيائي.</li>
    </ul>
  </div>
</div>

<div class="page-break"></div>

<!-- صفحة 6: دليل المناقشة الشفوي للأساتذة -->
<div class="page-header-line">
  <span>مشروع LOCAL CONTACT P2P — تقرير التطوير الأكاديمي</span>
  <span>إعداد الطالب: مشعل | صفحة 6</span>
</div>

<div class="section">
  <h1 class="sec-title"><span class="num-badge">6</span> دليل المناقشة الشفوي والأسئلة المتوقعة من الدكاترة</h1>

  <h2 class="sub-title">أولاً: أسئلة أستاذ هندسة البرمجيات (Software Engineering):</h2>

  <div class="qa-card">
    <div class="qa-question">
      <span>س1: ما الجدوى الهندسية لتوثيق SRS و Baseline لمشروع كان يعمل برمجياً بالفعل؟</span>
      <span class="badge badge-info">هندسة متطلبات</span>
    </div>
    <div class="qa-answer">
      <strong>الإجابة:</strong> البرمجيات الاحترافية لا تقاس بمجرد تشغيل الكود، بل بـ <strong>قابلية الصيانة (Maintainability)</strong> والتتبع وإثبات مطابقة الوظائف لمعايير القبول. بدون SRS لا نملك توصيفاً رسمياً للحدود التشغيلية أو الحالات الحدية، وبدون Baseline لا يمكن لأي مطور قياس أثر التعديلات المعمارية.
      <div class="qa-code-ref">المرجع: docs/SRS.md و docs/Baseline.md</div>
    </div>
  </div>

  <div class="qa-card">
    <div class="qa-question">
      <span>س2: أين يظهر تطبيق مبادئ SOLID ونمط التصميم (Design Patterns) في الكود؟</span>
      <span class="badge badge-info">تصميم معماري</span>
    </div>
    <div class="qa-answer">
      <strong>الإجابة:</strong> طبقنا مبدأ <strong>DIP (عكس التبعيات)</strong> ونمط <strong>Strategy/Provider Pattern</strong> عبر واجهة <span class="code-inline">KeyProvider</span>. كان محرك التشفير مقترناً بشدة بدوال اشتقاق المفتاح الثابت، فقمنا بفصل المسؤوليات بحيث يعتمد التشفير على تجريد مجرد، مما أتاح إضافة مفاتيح الجلسات دون المساس بخوارزميات التشفير AES-GCM.
      <div class="qa-code-ref">المرجع: app/src/main/java/com/example/network/KeyProvider.kt</div>
    </div>
  </div>

  <h2 class="sub-title">ثانياً: أسئلة أستاذ التشفير وأمن المعلومات (Cryptography):</h2>

  <div class="qa-card">
    <div class="qa-question">
      <span>س1: لماذا تم اختيار خوارزمية AES-GCM بالتحديد؟ وما وسم المصادقة؟</span>
      <span class="badge badge-success">تشفير متماثل</span>
    </div>
    <div class="qa-answer">
      <strong>الإجابة:</strong> لأن AES-GCM يوفر <strong>Authenticated Encryption (AEAD)</strong> في خطوة واحدة، حيث يدمج السرية مع سلامة البيانات. يولد النمط وسماً بطول 128 بت يسمى <span class="code-inline">Authentication Tag</span> يضمن كشف أي تعديل في النص المشفر أو الـ Nonce، ويرفض فك التشفير تلقائياً عند التلاعب، محققاً حماية تامة من هجمات Bit-Flipping.
      <div class="qa-code-ref">المرجع: LocalCryptoEngine.kt (encrypt / decrypt)</div>
    </div>
  </div>

  <div class="qa-card">
    <div class="qa-question">
      <span>س2: كيف يحقق بروتوكول ECDH سرية التوجيه المستقبلي (Forward Secrecy)؟</span>
      <span class="badge badge-success">تبادل مفاتيح</span>
    </div>
    <div class="qa-answer">
      <strong>الإجابة:</strong> نستخدم منحنى NIST P-256 لتوليد أزواج مفاتيح <strong>مؤقتة (Ephemeral)</strong> لكل جلسة اتصال مستقلة. الطرفان يحسبان السر المشترك ثم يُتلف المفتاح الخاص المؤقت فوراً من الذاكرة. في حال اختراق الجهاز لاحقاً، يستحيل رياضياً فك تشفير حزم الجلسات السابقة المسجلة. ودور HKDF-SHA256 هو تنقية السر المشترك وتحويله لمفتاح AES متماثل نقي عشوائياً.
      <div class="qa-code-ref">المرجع: EcdhEngine.kt (deriveSharedSecret / deriveSessionKey)</div>
    </div>
  </div>

  <div class="qa-card">
    <div class="qa-question">
      <span>س3: لماذا وصفتم حماية رجل المنتصف (MITM) بأنها "جزئية وليست كاملة"؟</span>
      <span class="badge badge-warning">تحليل ثغرات</span>
    </div>
    <div class="qa-answer">
      <strong>الإجابة:</strong> دقة علمية: لأننا نعتمد نموذج <strong>TOFU</strong> لغياب خادم شهادات مركزي (No CA). في أول اتصال، إذا تواجد مهاجم نشط (Active MITM) يمكنه استبدال المفتاح العام. نموذج TOFU يحمي بامتياز بدءاً من الاتصال الثاني؛ وللحماية التامة في أول لقاء يلزم قناة خارجية (SAS أو QR Code).
      <div class="qa-code-ref">المرجع: PeerTrustStore.kt و PeerSessionKeyManager.kt</div>
    </div>
  </div>
</div>

<div class="page-break"></div>

<!-- صفحة 7: الفهرس البرمجي السريع والصياغة الختامية والمراجع -->
<div class="page-header-line">
  <span>مشروع LOCAL CONTACT P2P — تقرير التطوير الأكاديمي</span>
  <span>إعداد الطالب: مشعل | صفحة 7</span>
</div>

<div class="section">
  <h1 class="sec-title"><span class="num-badge">7</span> الخريطة المرجعية السريعة في الكود (دوال ومتغيرات هامة)</h1>
  
  <table>
    <thead>
      <tr>
        <th>الميزة / الوظيفة</th>
        <th>الملف</th>
        <th>اسم المتغير أو الدالة في الكود</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td><strong>منفذ وسوكيت الاستكشاف</strong></td>
        <td><span class="code-inline">LocalP2PEngine.kt</span></td>
        <td><span class="code-inline">PORT_DISCOVERY = 8888</span> / <span class="code-inline">discoverySocket</span></td>
      </tr>
      <tr>
        <td><strong>إرسال واستقبال الرسائل</strong></td>
        <td><span class="code-inline">LocalP2PEngine.kt</span></td>
        <td><span class="code-inline">sendMessage()</span> / <span class="code-inline">handleIncomingPacket()</span></td>
      </tr>
      <tr>
        <td><strong>تشفير وفك تشفير AES-GCM</strong></td>
        <td><span class="code-inline">LocalCryptoEngine.kt</span></td>
        <td><span class="code-inline">encrypt()</span> / <span class="code-inline">decrypt()</span></td>
      </tr>
      <tr>
        <td><strong>توليد واشتقاق مفاتيح ECDH</strong></td>
        <td><span class="code-inline">EcdhEngine.kt</span></td>
        <td><span class="code-inline">generateEphemeralKeyPair()</span> / <span class="code-inline">deriveSessionKey()</span></td>
      </tr>
      <tr>
        <td><strong>هوية وتوقيع Android Keystore</strong></td>
        <td><span class="code-inline">KeystoreIdentityManager.kt</span></td>
        <td><span class="code-inline">getOrCreateIdentityKeyPair()</span> / <span class="code-inline">signHandshakePayload()</span></td>
      </tr>
      <tr>
        <td><strong>التوقيع المرتبط بالسياق وبصمة TOFU</strong></td>
        <td><span class="code-inline">HandshakeCryptoUtils.kt</span></td>
        <td><span class="code-inline">buildCanonicalSignatureInput()</span> / <span class="code-inline">computeFingerprint()</span></td>
      </tr>
      <tr>
        <td><strong>مخزن الثقة وإدارة الجلسات</strong></td>
        <td><span class="code-inline">PeerSessionKeyManager.kt</span></td>
        <td><span class="code-inline">getSessionKey()</span> / <span class="code-inline">PeerSecurityStatus</span></td>
      </tr>
      <tr>
        <td><strong>منافذ نقل الملفات، الصوت، والفيديو</strong></td>
        <td><span class="code-inline">File/Audio/VideoEngine.kt</span></td>
        <td><span class="code-inline">8891 (TCP File)</span>, <span class="code-inline">8889 (Audio UDP)</span>, <span class="code-inline">8890 (Video UDP)</span></td>
      </tr>
    </tbody>
  </table>
</div>

<div class="section">
  <h1 class="sec-title"><span class="num-badge">8</span> الصياغة النموذجية للإلقاء والتسليم النهائي</h1>

  <div class="callout callout-info" style="font-size: 11.5px; line-height: 1.6;">
    <div class="callout-title">🎙️ الكلمة الافتتاحية للمناقشة (احفظ هذا الملخص لإلقائه أمام اللجنة):</div>
    "بسم الله الرحمن الرحيم، أساتذتي الأفاضل، يسرني تقديم مشروع <strong>LOCAL CONTACT</strong>، وهو تطبيق اتصال شبكي محلي P2P مستقل تماماً عن السحابة. خلال هذا العمل طورنا النظام على مسارين:
    <br>
    <strong>في هندسة البرمجيات:</strong> صغنا وثيقة SRS لـ 31 متطلباً وظيفياً، وثبتنا خط الأساس Baseline، وأنشأنا مصفوفة تتبع RTM تربط كل متطلب بالكود والاختبارات. وطبقنا مبادئ SOLID عبر فصل إدارة المفاتيح بواجهة KeyProvider، وحققنا نجاح 33/33 اختباراً آلياً على JVM بنسبة 100%.
    <br>
    <strong>في التشفير:</strong> حافظنا على AES-256-GCM، واستبدلنا المفتاح الثابت بتبادل مفاتيح ديناميكي ECDH P-256 مع اشتقاق HKDF-SHA256 لتحقيق Forward Secrecy في الدردشة المباشرة. وربطنا هوية الجهاز بمخزن Android Keystore المشفر عتادياً بتوقيع مرتبط بالسياق ونموذج TOFU.
    <br>
    وانطلاقاً من الأمانة الأكاديمية، نوضح أن المشروع مجاز ومختبر برمجياً مع APK جاهز، مع توثيق التحسينات المستقبلية مثل حماية Replay الشاملة وفحص الملف الكامل SHA-256. نشكركم ومستعدون لملاحظاتكم."
  </div>

  <h2 class="sub-title">المراجع والمعايير القياسية المعتمدة (References):</h2>
  <div class="ref-box">
    [1] NIST FIPS 197 / SP 800-38D: Advanced Encryption Standard (AES) in Galois/Counter Mode (GCM).<br>
    [2] RFC 5869 / RFC 6090: HKDF Key Derivation & Elliptic Curve Cryptography (ECDH secp256r1).<br>
    [3] Android Developer Security: Hardware-backed Android Keystore System (TEE / StrongBox).<br>
    [4] ISO/IEC/IEEE 29148: Systems and Software Engineering — Requirements Engineering.
  </div>

  <div class="doc-footer">
    تقرير التطوير الأكاديمي والمناقشة — مشروع LOCAL CONTACT P2P — <strong>إعداد الطالب: مشعل</strong> — جميع الحقوق الأكاديمية محفوظة © 2026
  </div>
</div>

</body>
</html>
"""

html_path = os.path.abspath("report_meshal_defense.html")
pdf_path = os.path.abspath("report_meshal_defense.pdf")

with open(html_path, "w", encoding="utf-8") as f:
    f.write(html_content)

print(f"HTML saved successfully at: {html_path}")

chrome_path = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
if not os.path.exists(chrome_path):
    chrome_path = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

cmd = [
    chrome_path,
    "--headless=new",
    "--disable-gpu",
    "--no-pdf-header-footer",
    f"--print-to-pdf={pdf_path}",
    html_path
]

res = subprocess.run(cmd, capture_output=True, text=True)
print("Browser Return Code:", res.returncode)
if os.path.exists(pdf_path):
    print(f"PDF generated successfully! Size: {os.path.getsize(pdf_path):,} bytes")
