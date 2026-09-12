import os
import shutil
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE

# ==============================================================================
# UNIFIED PROFESSIONAL ACADEMIC DESIGN SYSTEM (PROJECTOR-OPTIMIZED)
# ==============================================================================

# Calm, High-Contrast Color Palette (Max 3 Main Colors per slide)
COLOR_BG = RGBColor(255, 255, 255)            # Crisp Clean White Background
COLOR_CARD_BG = RGBColor(248, 250, 252)       # Ultra Light Slate (#F8FAFC)
COLOR_CARD_BORDER = RGBColor(203, 213, 225)   # Subtle Slate Border (#CBD5E1)
COLOR_NAVY = RGBColor(15, 43, 92)             # Deep Navy for Headings (#0F2B5C)
COLOR_BLUE_ACCENT = RGBColor(30, 64, 175)     # Calm Royal Blue Accent (#1E40AF)
COLOR_TEXT_MAIN = RGBColor(15, 23, 42)        # Solid Dark Slate for Body (#0F172A)
COLOR_TEXT_MUTED = RGBColor(71, 85, 105)      # Secondary Slate Text (#475569)

# Controlled Semantic Colors (Used strictly for badges/status)
COLOR_GREEN = RGBColor(5, 150, 105)          # Verified / Implemented (#059669)
COLOR_AMBER = RGBColor(217, 119, 6)          # Partial / Pending (#D97706)
COLOR_RED = RGBColor(220, 38, 38)            # Gap / Risk / Smell (#DC2626)

FONT_HEADING = "Arial"
FONT_BODY = "Arial"
FONT_MONO = "Consolas"

STUDENTS = [
    "1. مشعل حاجب",
    "2. قحطان",
    "3. محمد العيدروس",
    "4. أواب النزيلي",
    "5. محمد العواضي"
]

def init_prs():
    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    return prs

def create_slide_header(slide, slide_num, category, title, subtitle=None, status_badge=None, status_col=COLOR_BLUE_ACCENT):
    # Top Subtle Accent Line (Calm Blue)
    top_line = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(0.1))
    top_line.fill.solid()
    top_line.fill.fore_color.rgb = COLOR_BLUE_ACCENT
    top_line.line.fill.background()

    # Category tracker
    cat_box = slide.shapes.add_textbox(Inches(0.9), Inches(0.25), Inches(8.5), Inches(0.35))
    tf_cat = cat_box.text_frame
    tf_cat.word_wrap = True
    p_cat = tf_cat.paragraphs[0]
    p_cat.text = f"LOCAL CONTACT P2P  |  {category}"
    p_cat.font.name = FONT_HEADING
    p_cat.font.size = Pt(13)
    p_cat.font.bold = True
    p_cat.font.color.rgb = COLOR_BLUE_ACCENT

    # Slide Counter Badge (Bottom Right for clean projector layout)
    num_box = slide.shapes.add_textbox(Inches(11.2), Inches(6.9), Inches(1.3), Inches(0.4))
    tf_num = num_box.text_frame
    p_num = tf_num.paragraphs[0]
    p_num.text = f"{slide_num} / 12"
    p_num.alignment = PP_ALIGN.RIGHT
    p_num.font.name = FONT_HEADING
    p_num.font.size = Pt(14)
    p_num.font.bold = True
    p_num.font.color.rgb = COLOR_NAVY

    # Slide Title (Large, Bold, High-Contrast for Projector)
    t_box = slide.shapes.add_textbox(Inches(0.9), Inches(0.65), Inches(9.8), Inches(0.75))
    tf_t = t_box.text_frame
    tf_t.word_wrap = True
    p_t = tf_t.paragraphs[0]
    p_t.text = title
    p_t.font.name = FONT_HEADING
    p_t.font.size = Pt(28)
    p_t.font.bold = True
    p_t.font.color.rgb = COLOR_NAVY

    # Status Pill (if provided)
    if status_badge:
        pill = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(9.8), Inches(0.68), Inches(2.6), Inches(0.48))
        pill.fill.solid()
        pill.fill.fore_color.rgb = COLOR_CARD_BG
        pill.line.color.rgb = status_col
        pill.line.width = Pt(1.5)
        p_pill = pill.text_frame.paragraphs[0]
        p_pill.text = status_badge
        p_pill.alignment = PP_ALIGN.CENTER
        p_pill.font.name = FONT_MONO
        p_pill.font.size = Pt(12)
        p_pill.font.bold = True
        p_pill.font.color.rgb = status_col

    # Separator Line
    sep = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.9), Inches(1.48), Inches(11.5), Inches(0.02))
    sep.fill.solid()
    sep.fill.fore_color.rgb = COLOR_CARD_BORDER
    sep.line.fill.background()

    # Footer subtle watermark
    foot = slide.shapes.add_textbox(Inches(0.9), Inches(6.9), Inches(8.0), Inches(0.35))
    p_f = foot.text_frame.paragraphs[0]
    p_f.text = "مشروع تخرج LOCAL CONTACT — مناقشة أكاديمية محكمة"
    p_f.font.name = FONT_BODY
    p_f.font.size = Pt(11)
    p_f.font.color.rgb = COLOR_TEXT_MUTED

def set_speaker_notes(slide, notes_text):
    notes_slide = slide.notes_slide
    text_frame = notes_slide.notes_text_frame
    text_frame.text = "🎙️ نص العرض الشفهي للمناقشة أمام اللجنة:\n" + notes_text

def create_cover_slide(prs, title, subtitle, track_name):
    s = prs.slides.add_slide(prs.slide_layouts[6])
    
    # Elegant Navy Top Accent Bar
    top_bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(0.25))
    top_bar.fill.solid()
    top_bar.fill.fore_color.rgb = COLOR_NAVY
    top_bar.line.fill.background()

    # Project Tag
    proj_tag = s.shapes.add_textbox(Inches(0.9), Inches(0.7), Inches(11.5), Inches(0.45))
    p_pt = proj_tag.text_frame.paragraphs[0]
    p_pt.text = "LOCAL CONTACT  •  Offline Local P2P Communication"
    p_pt.font.name = FONT_HEADING
    p_pt.font.size = Pt(15)
    p_pt.font.bold = True
    p_pt.font.color.rgb = COLOR_BLUE_ACCENT

    # Main Title (36 pt Bold, High Contrast)
    title_box = s.shapes.add_textbox(Inches(0.9), Inches(1.25), Inches(11.5), Inches(1.1))
    tf_title = title_box.text_frame
    tf_title.word_wrap = True
    p_title = tf_title.paragraphs[0]
    p_title.text = title
    p_title.font.name = FONT_HEADING
    p_title.font.size = Pt(36)
    p_title.font.bold = True
    p_title.font.color.rgb = COLOR_NAVY

    # Subtitle
    sub_box = s.shapes.add_textbox(Inches(0.9), Inches(2.35), Inches(11.5), Inches(0.55))
    p_sub = sub_box.text_frame.paragraphs[0]
    p_sub.text = subtitle
    p_sub.font.name = FONT_BODY
    p_sub.font.size = Pt(18)
    p_sub.font.bold = True
    p_sub.font.color.rgb = COLOR_TEXT_MUTED

    # Calm Architectural Motif Shape (Minimalist Network Node Icon Box)
    motif = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(3.1), Inches(11.5), Inches(0.65))
    motif.fill.solid()
    motif.fill.fore_color.rgb = COLOR_CARD_BG
    motif.line.color.rgb = COLOR_CARD_BORDER
    motif.line.width = Pt(1)
    p_m = motif.text_frame.paragraphs[0]
    p_m.text = f"🔒 المسار الأكاديمي: {track_name}  |  Android SDK 36  |  Kotlin 2.2  |  No Central Servers"
    p_m.alignment = PP_ALIGN.CENTER
    p_m.font.name = FONT_HEADING
    p_m.font.size = Pt(13)
    p_m.font.bold = True
    p_m.font.color.rgb = COLOR_NAVY

    # Students Container Card (Clean, Uncluttered, Exact Order)
    team_card = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(4.05), Inches(11.5), Inches(2.45))
    team_card.fill.solid()
    team_card.fill.fore_color.rgb = COLOR_CARD_BG
    team_card.line.color.rgb = COLOR_BLUE_ACCENT
    team_card.line.width = Pt(1.5)

    tf_team = team_card.text_frame
    tf_team.word_wrap = True
    p_head = tf_team.paragraphs[0]
    p_head.text = "إعداد الطلاب:"
    p_head.font.name = FONT_HEADING
    p_head.font.size = Pt(16)
    p_head.font.bold = True
    p_head.font.color.rgb = COLOR_NAVY

    # Add students horizontally in a neat grid (Exact Order strictly preserved)
    col_w = Inches(2.1)
    for idx, sname in enumerate(STUDENTS):
        col_x = Inches(1.2 + idx * 2.25)
        pill = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, col_x, Inches(4.9), Inches(2.05), Inches(0.9))
        pill.fill.solid()
        pill.fill.fore_color.rgb = RGBColor(255, 255, 255)
        pill.line.color.rgb = COLOR_CARD_BORDER
        pill.line.width = Pt(1)
        p_s = pill.text_frame.paragraphs[0]
        p_s.text = sname
        p_s.alignment = PP_ALIGN.CENTER
        p_s.font.name = FONT_HEADING
        p_s.font.size = Pt(15)
        p_s.font.bold = True
        p_s.font.color.rgb = COLOR_NAVY

    # Bottom Metadata
    meta = s.shapes.add_textbox(Inches(0.9), Inches(6.1), Inches(11.5), Inches(0.35))
    p_meta = meta.text_frame.paragraphs[0]
    p_meta.text = "المناقشة الأكاديمية الرسمية  •  نسخة العرض المحكمة (Projector Edition)  •  12 شريحة"
    p_meta.alignment = PP_ALIGN.CENTER
    p_meta.font.name = FONT_BODY
    p_meta.font.size = Pt(12)
    p_meta.font.color.rgb = COLOR_TEXT_MUTED

    return s

# ==============================================================================
# PRESENTATION 1: SECURITY & CRYPTOGRAPHY (التصميم الأمني والتشفير)
# ==============================================================================
def build_security_presentation():
    prs = init_prs()
    blank = prs.slide_layouts[6]

    # --------------------------------------------------------------------------
    # SLIDE 1: Cover
    # --------------------------------------------------------------------------
    s1 = create_cover_slide(
        prs,
        title="التصميم الأمني والتشفير في مشروع LOCAL CONTACT",
        subtitle="هندسة خوارزميات التشفير وإدارة المفاتيح في نظام اتصال P2P محلي مستقل",
        track_name="أمن الشبكات والتشفير المتقدم (Applied Cryptography)"
    )
    set_speaker_notes(s1, "نبدأ المناقشة بالترحيب بلجنة التحكيم. نناقش اليوم التصميم الأمني لنظام LOCAL CONTACT. هذا النظام يعمل P2P دون إنترنت أو خادم وسيط. نقدم العرض كفريق عمل مكون من: مشعل حاجب، قحطان، محمد العيدروس، أواب النزيلي، ومحمد العواضي.")

    # --------------------------------------------------------------------------
    # SLIDE 2: Pre-Security State & Static Key Pitfall
    # --------------------------------------------------------------------------
    s2 = prs.slides.add_slide(blank)
    create_slide_header(s2, 2, "تشخيص الوضع الأمني السابق", "حالة التشفير قبل التحسين: مخاطر المفتاح الثابت المشترك", "تحليل الثغرة الجوهرية في إدارة المفاتيح", "CRITICAL RISK", COLOR_RED)

    # 2 Wide Cards with Large Font
    card1 = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    card1.fill.solid()
    card1.fill.fore_color.rgb = COLOR_CARD_BG
    card1.line.color.rgb = COLOR_RED
    card1.line.width = Pt(1.5)
    tf1 = card1.text_frame
    tf1.word_wrap = True
    p = tf1.paragraphs[0]
    p.text = "⚠️ المشكلة الأمنية السابقة:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_RED

    points_s2_1 = [
        "مفتاح شبكي ثابت لجميع الأجهزة مشتق عبر PBKDF2 من كلمة مرور ثابتة داخل الكود.",
        "غياب بروتوكول لتبادل المفاتيح الديناميكية بين طرفي الاتصال.",
        "انعدام السرية المستقبلية (No Forward Secrecy)؛ كشف المفتاح يفضح الماضي بالكامل.",
        "غياب الهوية الرقمية الموثقة للأجهزة على الشبكة."
    ]
    for pt in points_s2_1:
        p = tf1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    card2 = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    card2.fill.solid()
    card2.fill.fore_color.rgb = COLOR_CARD_BG
    card2.line.color.rgb = COLOR_CARD_BORDER
    card2.line.width = Pt(1.5)
    tf2 = card2.text_frame
    tf2.word_wrap = True
    p = tf2.paragraphs[0]
    p.text = "📂 الملف والمسؤولية في الكود:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    p_f = tf2.add_paragraph()
    p_f.text = "app/src/main/java/com/example/network/LocalCryptoEngine.kt"
    p_f.font.name = FONT_MONO
    p_f.font.size = Pt(14)
    p_f.font.bold = True
    p_f.font.color.rgb = COLOR_BLUE_ACCENT
    p_f.space_before = Pt(8)

    points_s2_2 = [
        "المسؤولية: كان يقوم بالتشفير وإدارة المفتاح الثابت داخلياً في نفس الكلاس.",
        "الخلل المعماري: اقتران صلب (Tight Coupling) بين خوارزمية التشفير وتوليد المفتاح.",
        "الأثر: كشف ملف APK بالهندسة العكسية يعرض كافة رسائل الشبكة للفك الفوري."
    ]
    for pt in points_s2_2:
        p = tf2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s2, "نوضح هنا للدكتور الثغرة السابقة: التطبيق كان يستخدم AES-GCM لكن المفتاح كان ثابتاً ومشتقاً من كلمة مرور واحدة في الكلاس LocalCryptoEngine. كشف المفتاح كان يكسر أمان كل المحادثات الماضية. هذا ما دفعنا لإعادة هندسة التشفير.")

    # --------------------------------------------------------------------------
    # SLIDE 3: AES-256-GCM
    # --------------------------------------------------------------------------
    s3 = prs.slides.add_slide(blank)
    create_slide_header(s3, 3, "التشفير المتماثل المعتمد", "التشفير المتماثل ومصادقة البيانات: AES-256-GCM", "حماية السرية التامة وسلامة البيانات (AEAD)", "JVM TESTED (10/10)", COLOR_GREEN)

    c3 = s3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(11.5), Inches(4.8))
    c3.fill.solid()
    c3.fill.fore_color.rgb = COLOR_CARD_BG
    c3.line.color.rgb = COLOR_CARD_BORDER
    c3.line.width = Pt(1.5)
    tf3 = c3.text_frame
    tf3.word_wrap = True

    p = tf3.paragraphs[0]
    p.text = "🔒 الركائز الأساسية لخوارزمية AES-256-GCM في النظام:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    aes_points = [
        ("السرية التامة (Confidentiality):", "تشفير متماثل بمفتاح 256-bit قياسي ومعتمد دولياً من NIST."),
        ("سلامة البيانات (Data Integrity):", "وسم مصادقة 128-bit Authentication Tag يكشف أي تلاعب بالبيانات فوراً."),
        ("عشوائية الإرسال (Nonce Uniqueness):", "رقم عشوائي فريد بطول 12-byte (96-bit) يولد لكل حزمة لمنع هجمات التكرار."),
        ("تشفير تدفق الملفات (Chunk Encryption):", "اشتقاق Nonce مستقل لكل كتلة ملف (64KB) عبر deriveChunkNonce()."),
        ("الأدلة البرمجية والاختبارات:", "LocalCryptoEngine.kt تم فحصه بـ 10 اختبارات وحدة في LocalCryptoEngineTest.")
    ]
    for title, desc in aes_points:
        p = tf3.add_paragraph()
        p.text = f"• {title} {desc}"
        p.font.name = FONT_BODY
        p.font.size = Pt(18)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s3, "نؤكد للدكتور أننا اخترنا AES-256-GCM لأنه نمط AEAD يقدم سرية ومصادقة معاً. وسم الـ Tag يكشف أي عبث بالبايتات ويرفض فك التشفير، والـ Nonce الفريد يمنع كسر الـ GCM. تم إثبات كل ذلك بـ 10 اختبارات JVM ناجحة.")

    # --------------------------------------------------------------------------
    # SLIDE 4: Key Agreement: ECDH (NIST P-256)
    # --------------------------------------------------------------------------
    s4 = prs.slides.add_slide(blank)
    create_slide_header(s4, 4, "تبادل المفاتيح الديناميكي", "تبادل المفاتيح وتأمين السرية التامة: ECDH (NIST P-256)", "توليد أسرار مشتركة مؤقتة لكل جلسة اتصال", "JVM TESTED (8/8)", COLOR_GREEN)

    c4_1 = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c4_1.fill.solid()
    c4_1.fill.fore_color.rgb = COLOR_CARD_BG
    c4_1.line.color.rgb = COLOR_BLUE_ACCENT
    c4_1.line.width = Pt(1.5)
    tf4_1 = c4_1.text_frame
    tf4_1.word_wrap = True
    p = tf4_1.paragraphs[0]
    p.text = "⚡ المبادئ الأمنية لتبادل المفاتيح ECDH:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    ecdh_pts = [
        "المنحنى المعتمد: secp256r1 (NIST P-256) القياسي والمدعوم في عتاد أندرويد.",
        "مفاتيح مؤقتة (Ephemeral Keys): توليد زوج مفاتيح جديد تماماً لكل محادثة أو جلسة.",
        "السرية المستقبلية (Forward Secrecy): إتلاف المفتاح الخاص المؤقت فور حساب السر المشترك.",
        "منع التنصت: حتى لو صودر الهاتف لاحقاً، يستحيل فك تشفير الجلسات السابقة المسجلة."
    ]
    for pt in ecdh_pts:
        p = tf4_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    c4_2 = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c4_2.fill.solid()
    c4_2.fill.fore_color.rgb = COLOR_CARD_BG
    c4_2.line.color.rgb = COLOR_CARD_BORDER
    c4_2.line.width = Pt(1.5)
    tf4_2 = c4_2.text_frame
    tf4_2.word_wrap = True
    p = tf4_2.paragraphs[0]
    p.text = "📂 الملف والتحقق الهندسي:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    p_f = tf4_2.add_paragraph()
    p_f.text = "app/src/main/java/com/example/network/EcdhEngine.kt"
    p_f.font.name = FONT_MONO
    p_f.font.size = Pt(14)
    p_f.font.bold = True
    p_f.font.color.rgb = COLOR_BLUE_ACCENT
    p_f.space_before = Pt(8)

    pts_ecdh_impl = [
        "المسؤولية: توليد المفاتيح المؤقتة، وتشفير المفتاح العام بترميز X.509 DER، وحساب السر المشترك.",
        "التوافق: استخدام مكتبة Java Cryptography Architecture (JCA) القياسية بدون مكتبات خارجية.",
        "حزمة الاختبار: 8 اختبارات آلية ناجحة في EcdhEngineTest.kt تثبت تطابق السر بين الطرفين."
    ]
    for pt in pts_ecdh_impl:
        p = tf4_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s4, "هنا نوضح كيف حلت خوارزمية ECDH مشكلة المفتاح الثابت: الطرفان يولدان مفاتيح مؤقتة P-256، يتبادلان المفاتيح العامة، ويحسبان نفس السر المشترك دون إرساله على الشبكة. بمجرد انتهاء المحادثة يُحذف المفتاح الخاص من الذاكرة محققاً Forward Secrecy.")

    # --------------------------------------------------------------------------
    # SLIDE 5: Key Derivation Function: HKDF-SHA256
    # --------------------------------------------------------------------------
    s5 = prs.slides.add_slide(blank)
    create_slide_header(s5, 5, "اشتقاق مفاتيح الجلسة", "دالة اشتقاق المفاتيح القياسية: HKDF-SHA256 (RFC 5869)", "استخلاص مفاتيح تشفير آمنة ومستقلة من السر المشترك", "RFC 5869 TESTED", COLOR_GREEN)

    c5 = s5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(11.5), Inches(4.8))
    c5.fill.solid()
    c5.fill.fore_color.rgb = COLOR_CARD_BG
    c5.line.color.rgb = COLOR_CARD_BORDER
    c5.line.width = Pt(1.5)
    tf5 = c5.text_frame
    tf5.word_wrap = True

    p = tf5.paragraphs[0]
    p.text = "⚙️ معمارية اشتقاق المفاتيح وفق المعيار الدولي RFC 5869:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    hkdf_points = [
        ("مرحلة الاستخلاص (Extract):", "تحويل السر المشترك الناتج من ECDH مع Salt شبكي إلى مفتاح شبه عشوائي (PRK) عالي الإنتروبيا."),
        ("مرحلة التوسيع (Expand):", "اشتقاق مفاتيح مستقلة بطول 256-bit باستخدام سياقات مختلفة (Context Info)."),
        ("فصل مفاتيح الإرسال والاستقبال:", "اشتقاق مفتاح TX للإرسال ومفتاح RX للاستقبال لمنع هجمات التكرار والخلط."),
        ("استبعاد المفاتيح الضعيفة:", "ضمان توزيع بتات المفتاح بشكل متجانس إحصائياً بما يتوافق مع متطلبات AES-256."),
        ("الملف والاختبارات:", "الملف: HandshakeCryptoUtils.kt تم التحقق منه بمطابقة نواقل الاختبار الرسمية (Test Vectors) في KeyDerivationTest.")
    ]
    for title, desc in hkdf_points:
        p = tf5.add_paragraph()
        p.text = f"• {title} {desc}"
        p.font.name = FONT_BODY
        p.font.size = Pt(18)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s5, "نشرح للدكتور خطوة HKDF-SHA256: السر المشترك الناتج من ECDH لا يصلح كمفتاح AES مباشرة لأنه قد يحتوي على تحيز إحصائي. نطبق RFC 5869 عبر مرحلتي Extract و Expand لتوليد مفاتيح تشفير مستقلة للإرسال والاستقبال. تم اختبار ذلك بنواقل RFC الرسمية.")

    # --------------------------------------------------------------------------
    # SLIDE 6: Session Key Management
    # --------------------------------------------------------------------------
    s6 = prs.slides.add_slide(blank)
    create_slide_header(s6, 6, "إدارة الجلسات التشفيرية", "إدارة مفاتيح الجلسات المؤقتة (Session Key Management)", "عزل الجلسات وتجديد المفاتيح لكل قرين في الذاكرة الحية", "INTEGRATED IN CODE", COLOR_BLUE_ACCENT)

    c6_1 = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c6_1.fill.solid()
    c6_1.fill.fore_color.rgb = COLOR_CARD_BG
    c6_1.line.color.rgb = COLOR_CARD_BORDER
    c6_1.line.width = Pt(1.5)
    tf6_1 = c6_1.text_frame
    tf6_1.word_wrap = True
    p = tf6_1.paragraphs[0]
    p.text = "🎯 دور مدير مفاتيح الجلسات:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    pts_sess = [
        "عزل الجلسات: لكل جهاز قرين (Peer) جدول مفاتيح جلسة مستقل تماماً عن الآخرين.",
        "التخزين في الذاكرة الحية فقط: عدم كتابة مفاتيح الجلسات المؤقتة في الذاكرة الدائمة (RAM Only).",
        "مؤقت انتهاء الجلسة: إتلاف المفاتيح تلقائياً عند انقطاع الاتصال أو إغلاق التطبيق.",
        "حماية متعددة المسارات: دعم التشفير المتزامن لعدة محادثات دون تصادم المفاتيح."
    ]
    for pt in pts_sess:
        p = tf6_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    c6_2 = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c6_2.fill.solid()
    c6_2.fill.fore_color.rgb = COLOR_CARD_BG
    c6_2.line.color.rgb = COLOR_CARD_BORDER
    c6_2.line.width = Pt(1.5)
    tf6_2 = c6_2.text_frame
    tf6_2.word_wrap = True
    p = tf6_2.paragraphs[0]
    p.text = "📂 الملف والمسؤولية في الكود:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    p_f = tf6_2.add_paragraph()
    p_f.text = "app/src/main/java/com/example/network/PeerSessionKeyManager.kt"
    p_f.font.name = FONT_MONO
    p_f.font.size = Pt(13)
    p_f.font.bold = True
    p_f.font.color.rgb = COLOR_BLUE_ACCENT
    p_f.space_before = Pt(8)

    pts_sess_resp = [
        "المسؤولية: تتبع حالة المصافحة، تخزين مفاتيح TX و RX لكل PeerID، وإتلاف المفاتيح عند انتهاء الجلسة.",
        "التكامل: يزود المحركات الشبكية بالمفتاح النشط للقرين بدلاً من الاعتماد على مفتاح ثابت موحد.",
        "الحالة الهندسية: Integrated in Code — مدمج في هيكل المشروع البرمجي."
    ]
    for pt in pts_sess_resp:
        p = tf6_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s6, "نوضح هنا كيفية إدارة المفاتيح عملياً عبر PeerSessionKeyManager: كل جهاز متصل يحصل على جلسة خاصة به ومفاتيح منفصلة مخزنة في الذاكرة العشوائية فقط، مما يعزل المستخدمين ويمنع تسرب البيانات بين المحادثات المختلفة.")

    # --------------------------------------------------------------------------
    # SLIDE 7: Device Identity & Android Keystore
    # --------------------------------------------------------------------------
    s7 = prs.slides.add_slide(blank)
    create_slide_header(s7, 7, "هوية الأجهزة والتوقيع الرقمي", "حماية هوية الأجهزة: Android Keystore والتوقيع الرقمي ECDSA", "تخزين المفاتيح الخاصة في العتاد الآمن (TEE / StrongBox)", "HARDWARE BACKED", COLOR_BLUE_ACCENT)

    c7 = s7.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(11.5), Inches(4.8))
    c7.fill.solid()
    c7.fill.fore_color.rgb = COLOR_CARD_BG
    c7.line.color.rgb = COLOR_CARD_BORDER
    c7.line.width = Pt(1.5)
    tf7 = c7.text_frame
    tf7.word_wrap = True

    p = tf7.paragraphs[0]
    p.text = "🛡️ منظومة الهوية المشفرة والتخزين العتادي الآمن:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    keystore_points = [
        ("الحماية في عتاد الجهاز (Hardware Security):", "توليد زوج مفاتيح هوية طويل الأمد داخل رقاقة التشفير الآمنة (TEE / StrongBox) عبر Android Keystore."),
        ("استحالة استخراج المفتاح الخاص (Non-Exportable):", "المفتاح الخاص لا يغادر عتاد الهاتف أبداً حتى مع صلاحيات Root كاملة للنظام."),
        ("التوقيع الرقمي (ECDSA with SHA-256):", "توقيع حزم المصافحة لإثبات هوية الجهاز ومنع انتحال الشخصية على الشبكة المحلية."),
        ("التوقيع المرتبط بالسياق (Context-Bound Signature):", "ربط التوقيع بـ Nonce المصافحة وعنوان الجهاز لمنع هجمات إعادة التوقيع (Replay Signature)."),
        ("الملف المسؤول في الكود:", "KeystoreIdentityManager.kt — يتولى توليد وإدارة هوية الهاتف والتوقيع الرقمي.")
    ]
    for title, desc in keystore_points:
        p = tf7.add_paragraph()
        p.text = f"• {title} {desc}"
        p.font.name = FONT_BODY
        p.font.size = Pt(18)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s7, "نشرح للدكتور كيف أمّنا هوية الجهاز: لا نعتمد على مجرد IP أو اسم عشوائي، بل نولد مفتاح هوية غير قابل للاستخراج داخل Android Keystore المحمي بالعتاد. يستخدم الجهاز المفتاح لتوقيع حزم المصافحة عبر ECDSA لإثبات هويته دون تسريب المفتاح الخاص.")

    # --------------------------------------------------------------------------
    # SLIDE 8: Trust Model & TOFU (Trust On First Use)
    # --------------------------------------------------------------------------
    s8 = prs.slides.add_slide(blank)
    create_slide_header(s8, 8, "نموذج الثقة وتثبيت الهوية", "نموذج الثقة اللامركزي: بصمات الأجهزة وتثبيت الهوية (TOFU)", "التحقق من الأقران محلياً دون الاعتماد على هيئة شهادات مركزية (No CA)", "TOFU TRUST MODEL", COLOR_BLUE_ACCENT)

    c8_1 = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c8_1.fill.solid()
    c8_1.fill.fore_color.rgb = COLOR_CARD_BG
    c8_1.line.color.rgb = COLOR_CARD_BORDER
    c8_1.line.width = Pt(1.5)
    tf8_1 = c8_1.text_frame
    tf8_1.word_wrap = True
    p = tf8_1.paragraphs[0]
    p.text = "🤝 آلية نموذج الثقة عند أول اتصال (TOFU):"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    tofu_pts = [
        "لماذا TOFU؟ شبكات P2P تعمل Offline، فلا يمكن الاتصال بهيئة شهادات مركزية (CA).",
        "حساب البصمة: حساب بصمة SHA-256 Fingerprint للمفتاح العام لهوية القرين.",
        "تثبيت المفتاح (Key Pinning): حفظ البصمة محلياً في قاعدة البيانات عند أول اتصال ناجح.",
        "كشف التغيير المريب: إطلاق تحذير أمني فوري إذا تغير مفتاح هوية القرين المسجل سابقاً."
    ]
    for pt in tofu_pts:
        p = tf8_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    c8_2 = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c8_2.fill.solid()
    c8_2.fill.fore_color.rgb = COLOR_CARD_BG
    c8_2.line.color.rgb = COLOR_CARD_BORDER
    c8_2.line.width = Pt(1.5)
    tf8_2 = c8_2.text_frame
    tf8_2.word_wrap = True
    p = tf8_2.paragraphs[0]
    p.text = "📂 الملف والمسؤولية في الكود:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    p_f = tf8_2.add_paragraph()
    p_f.text = "app/src/main/java/com/example/network/PeerTrustStore.kt"
    p_f.font.name = FONT_MONO
    p_f.font.size = Pt(14)
    p_f.font.bold = True
    p_f.font.color.rgb = COLOR_BLUE_ACCENT
    p_f.space_before = Pt(8)

    pts_tofu_resp = [
        "المسؤولية: تخزين بصمات الأجهزة الموثوقة ومقارنتها مع كل اتصال وارد.",
        "إدارة الحالات: تصنيف القرين إلى: جديد (New)، موثوق (Trusted)، أو مشبوه/متغير (Key Changed).",
        "القيمة الهندسية: تحقيق أمان شبيه بـ SSH Host Keys دون الحاجة لإنترنت أو خادم مركزي."
    ]
    for pt in pts_tofu_resp:
        p = tf8_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s8, "نبيّن هنا كيف تحل مشكلة الثقة دون سيرفر مركزي: نستخدم نموذج TOFU مثل بروتوكول SSH. يتم حفظ بصمة المفتاح العام SHA-256 للمستخدم في PeerTrustStore. إذا حاول أي مهاجم انتحال هويته مستقبلاً بمفتاح آخر، يرفض التطبيق الاتصال فوراً.")

    # --------------------------------------------------------------------------
    # SLIDE 9: Handshake Protocol
    # --------------------------------------------------------------------------
    s9 = prs.slides.add_slide(blank)
    create_slide_header(s9, 9, "بروتوكول المصافحة الشبكية", "خطوات بروتوكول المصافحة والتفاوض الآمن (Handshake Protocol)", "تبادل المفاتيح والتحقق من الهوية قبل نقل أي بيانات", "HANDSHAKE PROTOCOL", COLOR_BLUE_ACCENT)

    c9 = s9.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(11.5), Inches(4.8))
    c9.fill.solid()
    c9.fill.fore_color.rgb = COLOR_CARD_BG
    c9.line.color.rgb = COLOR_CARD_BORDER
    c9.line.width = Pt(1.5)
    tf9 = c9.text_frame
    tf9.word_wrap = True

    p = tf9.paragraphs[0]
    p.text = "🔄 مراحل المصافحة الأربع المنفذة في HandshakeCryptoUtils.kt:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    hs_steps = [
        ("المرحلة 1 — طلب المصافحة (Handshake Init):", "المرسل يولد مفتاح ECDH مؤقت ويرسله مع Nonce ومعرف هويته وبصمته."),
        ("المرحلة 2 — رد المصافحة والتوقيع (Handshake Response):", "المستقبل يولد مفتاحه المؤقت، ويوقع الحزمة عبر ECDSA، ويرد بمفتاحه وبصمته."),
        ("المرحلة 3 — التحقق واشتقاق المفاتيح (Verify & Derive):", "الطرفان يتحققان من صحة التوقيع والبصمة، ويحسبان السر المشترك، ويشتقان مفاتيح الجلسة عبر HKDF."),
        ("المرحلة 4 — إتمام وتأكيد الجلسة (Handshake Complete):", "إرسال حزمة تأكيد مشفرة بمفتاح الجلسة الجديد والانتقال لقناة البيانات المشفرة."),
        ("الملف المسؤول:", "HandshakeCryptoUtils.kt — يتولى بناء وتفكيك حزم المصافحة والتحقق الرياضي من التوقيعات.")
    ]
    for title, desc in hs_steps:
        p = tf9.add_paragraph()
        p.text = f"• {title} {desc}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(11)

    set_speaker_notes(s9, "نستعرض للأستاذ بروتوكول المصافحة المكون من 4 خطوات: يتم تبادل المفاتيح المؤقتة والتوقيعات الرقمية والبصمات، ثم يتم اشتقاق مفاتيح الجلسة وتأكيدها بحزمة مشفرة قبل السماح بمرور أي رسالة نصية أو مكالمة صوتية.")

    # --------------------------------------------------------------------------
    # SLIDE 10: Streaming File Transfer & Media Protection
    # --------------------------------------------------------------------------
    s10 = prs.slides.add_slide(blank)
    create_slide_header(s10, 10, "تحصين الوسائط والملفات", "تأمين نقل الملفات وتدفق الصوت والفيديو (Media Security)", "حماية كتل البيانات التدفّقية عبر قنوات TCP و UDP", "MEDIA & STREAMING", COLOR_BLUE_ACCENT)

    c10_1 = s10.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c10_1.fill.solid()
    c10_1.fill.fore_color.rgb = COLOR_CARD_BG
    c10_1.line.color.rgb = COLOR_CARD_BORDER
    c10_1.line.width = Pt(1.5)
    tf10_1 = c10_1.text_frame
    tf10_1.word_wrap = True
    p = tf10_1.paragraphs[0]
    p.text = "📁 تأمين نقل الملفات عبر TCP: 8891:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    p_f = tf10_1.add_paragraph()
    p_f.text = "FileTransferEngine.kt"
    p_f.font.name = FONT_MONO
    p_f.font.size = Pt(14)
    p_f.font.bold = True
    p_f.font.color.rgb = COLOR_BLUE_ACCENT
    p_f.space_before = Pt(6)

    pts_files = [
        "نقل تدفقي مجزأ: قراءة وتشفير الملف في هيئة كتل 64KB لمنع استهلاك الذاكرة وحوادث OOM.",
        "اشتقاق Nonce لكل كتلة: اشتقاق رقم عشوائي محسوب لكل كتلة يضمن أمان تشفير GCM أثناء الاستئناف.",
        "دعم الاستئناف (Resume): إمكانية استكمال نقل الملف من موضع الإزاحة دون إعادة التشفير من الصفر.",
        "تعقيم المسارات: تنظيف أسماء الملفات عبر StorageUtils لمنع هجمات التراجع الدليلي (../)."
    ]
    for pt in pts_files:
        p = tf10_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    c10_2 = s10.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c10_2.fill.solid()
    c10_2.fill.fore_color.rgb = COLOR_CARD_BG
    c10_2.line.color.rgb = COLOR_CARD_BORDER
    c10_2.line.width = Pt(1.5)
    tf10_2 = c10_2.text_frame
    tf10_2.word_wrap = True
    p = tf10_2.paragraphs[0]
    p.text = "🎙️ تأمين تدفق الصوت والفيديو عبر UDP:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    p_f2 = tf10_2.add_paragraph()
    p_f2.text = "AudioEngine.kt (8889) & VideoEngine.kt (8890)"
    p_f2.font.name = FONT_MONO
    p_f2.font.size = Pt(13)
    p_f2.font.bold = True
    p_f2.font.color.rgb = COLOR_BLUE_ACCENT
    p_f2.space_before = Pt(6)

    pts_media = [
        "تشفير الإطارات الحية: تشفير عينات الصوت 16kHz PCM وإطارات الفيديو بصيغة AES-GCM.",
        "انخفاض زمن الكمون (Low Latency): معالجة خفيفة للتشفير الفوري تناسب الاتصال المباشر.",
        "إسقاط الحزم المتلاعب بها: اعتراض استثناء AEADBadTagException وإسقاط الإطار التالف فوراً دون انهيار التطبيق.",
        "استقلالية القنوات: فصل قنوات الصوت والفيديو والملفات على منافذ مقابس مستقلة."
    ]
    for pt in pts_media:
        p = tf10_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    set_speaker_notes(s10, "نبيّن هنا حماية الوسائط: الملفات تُقسّم إلى كتل 64KB وتشفّر مع Nonce مستقل لكل كتلة لضمان استئناف آمن. والصوت والفيديو يُشفران بنمط AES-GCM الفوري عبر UDP مع إسقاط الحزم التي تفشل في المصادقة لمنع هجمات الحقن الصوتي.")

    # --------------------------------------------------------------------------
    # SLIDE 11: Testing & Automated Verification
    # --------------------------------------------------------------------------
    s11 = prs.slides.add_slide(blank)
    create_slide_header(s11, 11, "التحقق والاختبارات الآلية", "مصفوفة التحقق والاختبارات الآلية (33/33 Tests Passed)", "إثبات الأمان الرياضي والبرمجي عبر اختبارات JVM شاملة", "100% JVM PASSED", COLOR_GREEN)

    c11 = s11.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(11.5), Inches(4.8))
    c11.fill.solid()
    c11.fill.fore_color.rgb = COLOR_CARD_BG
    c11.line.color.rgb = COLOR_GREEN
    c11.line.width = Pt(1.5)
    tf11 = c11.text_frame
    tf11.word_wrap = True

    p = tf11.paragraphs[0]
    p.text = "🧪 نتائج حزم الاختبارات الأربع المؤتمتة (JVM Automated Tests):"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    test_rows = [
        ("1. LocalCryptoEngineTest.kt (10/10 ناجح):", "إثبات تشفير وفك تشفير AES-GCM، ورفض المفاتيح الخاطئة، ورفض التلاعب بالنص المشفر."),
        ("2. EcdhEngineTest.kt (8/8 ناجح):", "إثبات توليد مفاتيح NIST P-256، وتطابق السر المشترك للطرفين، واستيراد وتصدير X.509."),
        ("3. KeyDerivationTest.kt (8/8 ناجح):", "إثبات مطابقة دالة HKDF-SHA256 لنواقل الاختبار القياسية المنصوص عليها في RFC 5869."),
        ("4. CryptoEngineTest.kt (7/7 ناجح):", "إثبات تكامل بروتوكول الجلسات المشفرة وحماية الرسائل من التعديل."),
        ("النتيجة الأكاديمية الإجمالية:", "33 اختبار وحدة آلي ناجح بنسبة 100% يثبت خلو الكود من الأخطاء المنطقية والرياضية.")
    ]
    for title, desc in test_rows:
        p = tf11.add_paragraph()
        p.text = f"• {title} {desc}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s11, "نقدم للدكتور برهان النجاح البرمجي: 33 اختبار وحدة آلي تم بناؤها واختبارها عبر Gradle ونجحت بالكامل بنسبة 100%. أثبتت الاختبارات أن التشفير سليم، وأن التلاعب بالبايتات يتم كشفه وإسقاطه، وأن دوال HKDF مطابقة لمعايير RFC.")

    # --------------------------------------------------------------------------
    # SLIDE 12: Security Boundaries & Scientific Integrity
    # --------------------------------------------------------------------------
    s12 = prs.slides.add_slide(blank)
    create_slide_header(s12, 12, "النزاهة الأكاديمية والحدود الأمنية", "الحدود الأمنية الحالية وما لم يُنفّذ بعد (Scientific Integrity)", "توثيق القيود وخارطة الطريق لتعزيز الحماية مستقبلاً", "HONEST BOUNDARIES", COLOR_AMBER)

    c12_1 = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c12_1.fill.solid()
    c12_1.fill.fore_color.rgb = COLOR_CARD_BG
    c12_1.line.color.rgb = COLOR_RED
    c12_1.line.width = Pt(1.5)
    tf12_1 = c12_1.text_frame
    tf12_1.word_wrap = True
    p = tf12_1.paragraphs[0]
    p.text = "⚠️ قيود أمنية موثقة كفجوات بأمانة علمية:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(21)
    p.font.bold = True
    p.font.color.rgb = COLOR_RED

    gaps_pts = [
        "غياب التحقق اليدوي البصري (SAS): نموذج TOFU يحمي بعد أول اتصال، لكن أول اتصال يظل عرضة لـ MITM نشط إذا لم تتم مقارنة البصمة شفهياً.",
        "غياب حماية الـ Replay الكاملة على مستوى الحزم: يوجد Nonce فريد يمنع فك التشفير، لكن لا توجد نافذة تسلسل حزم صارمة (Packet Sliding Window).",
        "عدم فحص SHA-256 للملف كاملاً بعد الاستلام: كتل الملفات مشفرة ومصادقة، لكن ينقص فحص Hash الملف النهائي.",
        "مفتاح الغرف الجماعية: المحادثات الجماعية لا تزال تعتمد على المفتاح المشترك وتفتقر لـ Group Key Exchange."
    ]
    for pt in gaps_pts:
        p = tf12_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    c12_2 = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c12_2.fill.solid()
    c12_2.fill.fore_color.rgb = COLOR_CARD_BG
    c12_2.line.color.rgb = COLOR_GREEN
    c12_2.line.width = Pt(1.5)
    tf12_2 = c12_2.text_frame
    tf12_2.word_wrap = True
    p = tf12_2.paragraphs[0]
    p.text = "🚀 خارطة الطريق الأمنية والخلاصة:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(21)
    p.font.bold = True
    p.font.color.rgb = COLOR_GREEN

    pts_future = [
        "1. إضافة رمز الأمان المرئي (SAS / QR Code) لمصادقة أول اتصال وتفادي هجوم الرجل في المنتصف تماماً.",
        "2. تطبيق بروتوكول متقدم للمجموعات (Tree-based Group Key Exchange) لتأمين الغرف بمفاتيح جماعية ديناميكية.",
        "3. دمج التحقق النهائي من بصمة الملف الكامل SHA-256 قبل فتح الملف من التنزيلات.",
        "الخلاصة: تحول المشروع من تطبيق بسيط بمفتاح مكشوف إلى نظام أمني رصين يطبق معايير التشفير العالمية بوضوح وشفافية."
    ]
    for pt in pts_future:
        p = tf12_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    set_speaker_notes(s12, "نختم العرض بالأمانة العلمية: نعترف بوضوح بالقيود التي ما زالت تحتاج تطويراً مثل إضافة كود SAS لمنع MITM في اللقاء الأول، وتشفير الغرف بمفاتيح جماعية. ونؤكد أن المشروع قفز قفزة نوعية من كود بمفتاح ثابت إلى بنية تشفير متكاملة وموثقة ومختبرة. شكراً لكم ومستعدون للمناقشة.")

    # Save Presentation
    p_en = os.path.abspath("LOCAL_CONTACT_Security_Presentation.pptx")
    prs.save(p_en)
    print(f"[OK] Security Presentation Updated: {p_en} ({os.path.getsize(p_en):,} bytes)")

# ==============================================================================
# PRESENTATION 2: SOFTWARE ENGINEERING (هندسة البرمجيات)
# ==============================================================================
def build_se_presentation():
    prs = init_prs()
    blank = prs.slide_layouts[6]

    # --------------------------------------------------------------------------
    # SLIDE 1: Cover
    # --------------------------------------------------------------------------
    s1 = create_cover_slide(
        prs,
        title="هندسة البرمجيات في مشروع LOCAL CONTACT",
        subtitle="تحليل المتطلبات، التصميم المعماري، إدارة التغيير، الاختبارات، والتتبع",
        track_name="مبادئ هندسة البرمجيات وإدارة دورة حياة النظم (Software Engineering)"
    )
    set_speaker_notes(s1, "نرحب بأستاذ هندسة البرمجيات ولجنة التحكيم. نستعرض اليوم منهجية هندسة البرمجيات المطبقة في مشروع LOCAL CONTACT. عملنا على تحليل المتطلبات، تثبيت خط الأساس، إدارة التغيير، مصفوفة التتبع، والاختبارات الآلية. يقدم العرض فريق العمل: مشعل حاجب، قحطان، محمد العيدروس، أواب النزيلي، ومحمد العواضي.")

    # --------------------------------------------------------------------------
    # SLIDE 2: Pre-SE State & Discovered Smells
    # --------------------------------------------------------------------------
    s2 = prs.slides.add_slide(blank)
    create_slide_header(s2, 2, "تشخيص واقع النظام", "حالة المشروع قبل التدخل الهندسي والمشكلات المكتشفة", "تحليل الفجوات المنهجية والمعمارية في الكود المصدري", "PRE-SE AUDIT", COLOR_RED)

    c2_1 = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c2_1.fill.solid()
    c2_1.fill.fore_color.rgb = COLOR_CARD_BG
    c2_1.line.color.rgb = COLOR_RED
    c2_1.line.width = Pt(1.5)
    tf2_1 = c2_1.text_frame
    tf2_1.word_wrap = True
    p = tf2_1.paragraphs[0]
    p.text = "⚠️ غياب التوثيق والمنهجية الهندسية:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_RED

    points_se_pre = [
        "غياب وثيقة المتطلبات الرسمية (No SRS): الميزات مبنية دون توصيف دقيق للمدخلات والمخرجات أو معايير القبول.",
        "انعدام خط الأساس الهندسي (No Baseline): عدم وجود مرجع ثابت لتتبع أثر التعديلات البرمجية وضبط التفرع.",
        "غياب مصفوفة التتبع (No Traceability): صعوبة تحديد أي متطلب وظيفي يرتبط بأي ملف برمجي في المشروع.",
        "انعدام الاختبارات الآلية (Zero Unit Tests): الاعتماد على الاختبار اليدوي العشوائي دون إثبات برمجي مؤتمت."
    ]
    for pt in points_se_pre:
        p = tf2_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    c2_2 = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c2_2.fill.solid()
    c2_2.fill.fore_color.rgb = COLOR_CARD_BG
    c2_2.line.color.rgb = COLOR_CARD_BORDER
    c2_2.line.width = Pt(1.5)
    tf2_2 = c2_2.text_frame
    tf2_2.word_wrap = True
    p = tf2_2.paragraphs[0]
    p.text = "🧩 الروائح المعمارية في الكود (Code Smells):"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    pts_se_smells = [
        "ظاهرة الكلاسات المتضخمة (God Classes): كلاس LocalP2PEngine.kt بلغ 2,496 سطراً ليجمع الاكتشاف والرسائل والمكالمات في مكان واحد.",
        "تضخم الـ ViewModel: كلاس MainViewModel.kt بلغ 1,839 سطراً ليجمع إدارة الواجهات واستعلامات قاعدة البيانات معاً.",
        "الاقتران الصلب (Tight Coupling): دمج منطق التشفير وإدارة المفاتيح داخل نفس الكلاس في LocalCryptoEngine.kt.",
        "انتهاك مبادئ SOLID: انتهاك صريح لمبدأ المسؤولية الواحدة (SRP) ومبدأ انعكاس الاعتمادية (DIP)."
    ]
    for pt in pts_se_smells:
        p = tf2_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s2, "نوضح هنا للأستاذ نقطة الصفر: المشروع بدأ كنموذج أولي بدون هندسة برمجيات: لا وثيقة SRS، لا خط أساس، وكلاسات ضخمة جداً انتهكت مبدأ Single Responsibility. هذه الفجوات هي التي استوجبت التدخل الهندسي لإعادة الهيكلة.")

    # --------------------------------------------------------------------------
    # SLIDE 3: Requirements Engineering & SRS.md
    # --------------------------------------------------------------------------
    s3 = prs.slides.add_slide(blank)
    create_slide_header(s3, 3, "هندسة المتطلبات", "توثيق مواصفات المتطلبات البرمجية (SRS Specification)", "منهجية تحليل وتصنيف المتطلبات في docs/SRS.md (622 lines)", "docs/SRS.md", COLOR_GREEN)

    c3_1 = s3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c3_1.fill.solid()
    c3_1.fill.fore_color.rgb = COLOR_CARD_BG
    c3_1.line.color.rgb = COLOR_BLUE_ACCENT
    c3_1.line.width = Pt(1.5)
    tf3_1 = c3_1.text_frame
    tf3_1.word_wrap = True
    p = tf3_1.paragraphs[0]
    p.text = "🎯 نموذج تصنيف المتطلبات الخماسي المبتكر:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(21)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    tiers = [
        ("1. Official Requirement:", "المتطلبات الرسمية الصريحة المطلوبة أكاديمياً."),
        ("2. Existing Capability:", "وظيفة مبنية بالفعل في الكود ولكنها تحتاج إثباتاً تشغيلياً."),
        ("3. Proposed Improvement:", "تحسينات مقترحة للمستقبل ولا تعد جزءاً من خط الأساس."),
        ("4. Missing:", "وظيفة متوقعة لكن فحص الكود أثبت غيابها (مثل الحذف الشبكي)."),
        ("5. Not Verified:", "كود موجود بالمستودع لم يُختبر بين جهازين حقيقيين.")
    ]
    for t_n, t_d in tiers:
        p = tf3_1.add_paragraph()
        p.text = f"• {t_n} {t_d}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(8)

    c3_2 = s3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c3_2.fill.solid()
    c3_2.fill.fore_color.rgb = COLOR_CARD_BG
    c3_2.line.color.rgb = COLOR_CARD_BORDER
    c3_2.line.width = Pt(1.5)
    tf3_2 = c3_2.text_frame
    tf3_2.word_wrap = True
    p = tf3_2.paragraphs[0]
    p.text = "📑 محتويات وثيقة SRS المعتمدة:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(21)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    srs_cont = [
        "نطاق النظام (Scope): تحديد دقيق لما يقدمه النظام (P2P محلي) وما يقع خارجه (لا خوادم سحابية، لا STUN/TURN).",
        "الأطراف الفاعلة (Actors): المستخدم المحلي، القرين البعيد، نظام Android، عتاد الكاميرا والصوت، وقاعدة بيانات Room.",
        "31 متطلباً وظيفياً (FR-01 إلى FR-31): تغطي الاكتشاف، التراسل، نقل الملفات، المكالمات الصوتية والمرئية، والتشفير.",
        "15 قصة مستخدم قياسية (US-01 إلى US-15): صياغة هندسية قياسية تعبر عن أهداف المستخدمين.",
        "معايير القبول (Acceptance Criteria): 28 معيار قبول بصيغة (Given / When / Then) لضبط التحقق."
    ]
    for sc in srs_cont:
        p = tf3_2.add_paragraph()
        p.text = f"• {sc}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(8)

    set_speaker_notes(s3, "نشرح للأستاذ وثيقة SRS المكونة من 622 سطراً. ركزنا فيها على تصنيف المتطلبات إلى 5 مستويات لفصل ما هو رسمي عما هو مقترح أو مفقود، ووثقنا 31 متطلباً وظيفياً و 15 قصة مستخدم مع معايير قبول دقيقة.")

    # --------------------------------------------------------------------------
    # SLIDE 4: Baseline & Version Control (Git)
    # --------------------------------------------------------------------------
    s4 = prs.slides.add_slide(blank)
    create_slide_header(s4, 4, "إدارة خط الأساس", "توثيق خط الأساس وضبط بيئة البناء (Baseline & Git)", "تثبيت النسخة المرجعية وضمان تكرارية النتائج ومكافحة انزلاق النطاق", "docs/Baseline.md", COLOR_GREEN)

    c4 = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(11.5), Inches(4.8))
    c4.fill.solid()
    c4.fill.fore_color.rgb = COLOR_CARD_BG
    c4.line.color.rgb = COLOR_CARD_BORDER
    c4.line.width = Pt(1.5)
    tf4 = c4.text_frame
    tf4.word_wrap = True

    p = tf4.paragraphs[0]
    p.text = "📌 عناصر خط الأساس المعتمد في docs/Baseline.md (308 lines):"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    base_pts = [
        ("Commit Hash المرجعي الثابت:", "71111825645c2b3c6a926f739455c4ffda3c0963 — تم التحقق من نظافة شجرة العمل (Clean Tree)."),
        ("تكرارية بيئة البناء (Reproducibility):", "Gradle 9.4.1 و AGP 9.1.1 و Kotlin 2.2.10 لضمان بناء نفس حزمة الـ APK عند أي طرف."),
        ("مستويات SDK ومكتبات النظام:", "compileSdk = 36، minSdk = 24 (Android 7.0+)، واستخدام KSP لمعالجة الرموز في Room DB و Moshi."),
        ("منع انزلاق النطاق (Scope Creep):", "تثبيت النطاق المرجعي يمنع إضافة ميزات غير مدروسة دون توثيق أثرها الهندسي على النظام."),
        ("حصر الوظائف (Features Inventory):", "توثيق 14 ميزة رئيسية مع ربطها بملفات الكود وحالتها الدقيقة (Implemented - Not Tested).")
    ]
    for title, desc in base_pts:
        p = tf4.add_paragraph()
        p.text = f"• {title} {desc}"
        p.font.name = FONT_BODY
        p.font.size = Pt(18)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s4, "نبيّن للأستاذ دور خط الأساس: قمنا بتثبيت الـ Commit رقم 7111182 وتوثيق بيئة البناء بالكامل في Baseline.md. هذا الإجراء يحقق التكرارية Reproducibility ويمنع حدوث انزلاق في نطاق العمل مع إقرار صريح بحدود التحقق.")

    # --------------------------------------------------------------------------
    # SLIDE 5: P2P Architectural Decomposition
    # --------------------------------------------------------------------------
    s5 = prs.slides.add_slide(blank)
    create_slide_header(s5, 5, "التصميم المعماري", "السياق المعماري للنظام وتوزيع المسؤوليات (P2P Architecture)", "تفكيك الطبقات والحزم البرمجية دون الحاجة لخادم مركزي", "DECENTRALIZED P2P", COLOR_BLUE_ACCENT)

    # 4 Clean Cards for Layers
    layers = [
        ("📱 طبقة العرض والواجهات (Presentation Layer)", "app/.../ui/", "واجهات Jetpack Compose التعلانية: شاشات المحادثة والأقران والغرف، وحوارات المكالمات، والمكونات التفاعلية."),
        ("🧠 طبقة التنسيق والحالة (ViewModel / Coordination)", "MainViewModel.kt", "إدارة تدفقات StateFlow للواجهات، معالجة الأحداث، وتوجيه البيانات بين المحركات الشبكية وقاعدة البيانات."),
        ("🌐 طبقة المحركات الشبكية (Network Engines Layer)", "app/.../network/", "عزل قنوات الاتصال المستقلة: الاكتشاف والرسائل (8888)، الصوت (8889)، الفيديو (8890)، ونقل الملفات (8891)."),
        ("💾 طبقة البيانات والتخزين (Persistence Layer)", "app/.../data/", "إدارة البيانات محلياً عبر Room Database (AppDatabase.kt) لرسائل الغرف والمستخدم، وحفظ التفضيلات عبر DataStore.")
    ]
    for idx, (lt, lp, ld) in enumerate(layers):
        col = idx % 2
        row = idx // 2
        lx = Inches(0.9 + col * 5.8)
        ly = Inches(1.8 + row * 2.45)
        c = s5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, lx, ly, Inches(5.6), Inches(2.25))
        c.fill.solid()
        c.fill.fore_color.rgb = COLOR_CARD_BG
        c.line.color.rgb = COLOR_CARD_BORDER
        c.line.width = Pt(1.5)
        tf = c.text_frame
        tf.word_wrap = True
        p1 = tf.paragraphs[0]
        p1.text = lt
        p1.font.name = FONT_HEADING
        p1.font.size = Pt(17)
        p1.font.bold = True
        p1.font.color.rgb = COLOR_NAVY
        p2 = tf.add_paragraph()
        p2.text = lp
        p2.font.name = FONT_MONO
        p2.font.size = Pt(13)
        p2.font.bold = True
        p2.font.color.rgb = COLOR_BLUE_ACCENT
        p2.space_before = Pt(3)
        p3 = tf.add_paragraph()
        p3.text = ld
        p3.font.name = FONT_BODY
        p3.font.size = Pt(15)
        p3.font.bold = True
        p3.font.color.rgb = COLOR_TEXT_MAIN
        p3.space_before = Pt(4)

    set_speaker_notes(s5, "نستعرض معمارية النظام: كل هاتف يعمل كعميل وخادم في نفس الوقت P2P. قسّمنا النظام لأربع طبقات معمارية واضحة: Presentation بـ Jetpack Compose، و Coordination بـ ViewModel، ومحركات شبكة معزولة لكل نوع بيانات، وتخزين محلي بـ Room.")

    # --------------------------------------------------------------------------
    # SLIDE 6: Architectural Smells & SOLID Principles
    # --------------------------------------------------------------------------
    s6 = prs.slides.add_slide(blank)
    create_slide_header(s6, 6, "الروائح المعمارية وإعادة الهيكلة", "تحليل الكلاسات المتضخمة وتطبيق مبادئ SOLID", "رصد انتهاكات المسؤولية الواحدة ووضع مسار إعادة الهيكلة المستمر", "ARCHITECTURAL SMELLS", COLOR_AMBER)

    c6_1 = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c6_1.fill.solid()
    c6_1.fill.fore_color.rgb = COLOR_CARD_BG
    c6_1.line.color.rgb = COLOR_AMBER
    c6_1.line.width = Pt(1.5)
    tf6_1 = c6_1.text_frame
    tf6_1.word_wrap = True
    p = tf6_1.paragraphs[0]
    p.text = "⚠️ الكلاسات المتضخمة المرصودة:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_AMBER

    pts_smells = [
        "LocalP2PEngine.kt (2,496 سطر): يجمع مهام بث واكتشاف الأقران، إدارة مقابس UDP، إشارات المكالمات، وإشعارات التسليم في كلاس واحد.",
        "MainViewModel.kt (1,839 سطر): يجمع منطق تنقل الشاشات، استعلامات قاعدة بيانات Room، والتفاعل المباشر مع محركات الشبكة.",
        "انتهاك مبدأ Single Responsibility (SRP): تجميع عدة مسؤوليات غير متجانسة يزيد من صعوبة الصيانة والاختبار المعزول.",
        "الديون التقنية (Technical Debt): توثيق هذه الكلاسات رسمياً في الـ SRS كديون تقنية مطلوب إعادة هيكلتها."
    ]
    for pt in pts_smells:
        p = tf6_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    c6_2 = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c6_2.fill.solid()
    c6_2.fill.fore_color.rgb = COLOR_CARD_BG
    c6_2.line.color.rgb = COLOR_GREEN
    c6_2.line.width = Pt(1.5)
    tf6_2 = c6_2.text_frame
    tf6_2.word_wrap = True
    p = tf6_2.paragraphs[0]
    p.text = "🧩 الحلول الهندسية المطبقة والمقترحة:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_GREEN

    pts_sol = [
        "تطبيق مبدأ انعكاس الاعتمادية (DIP): عزل التشفير عبر واجهة KeyProvider.kt بحيث يعتمد المحرك على واجهة مجردة بدلاً من كلاس ملموس.",
        "خطة تفكيك LocalP2PEngine: تقسيمه مستقبلاً إلى 3 محركات فرعية: DiscoveryCoordinator, CallSignalingHandler, PacketDispatcher.",
        "تفكيك ViewModel: إنشاء ViewModels متخصصة لكل شاشة (ChatViewModel, CallsViewModel) لتخفيف الحمل المعماري.",
        "النضج الهندسي: الاعتراف بوجود الروائح المعمارية هو الخطوة الأولى لتنفيذ إعادة الهيكلة المستمرة (Refactoring)."
    ]
    for pt in pts_sol:
        p = tf6_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    set_speaker_notes(s6, "نناقش هنا الروائح المعمارية بشفافية: رصدنا تضخم LocalP2PEngine (أكثر من 2400 سطر) و MainViewModel، ووضحنا انتهاكها لمبدأ SRP. وطبقنا مبدأ DIP عبر KeyProvider لحل مشكلة الاقتران، ووضعنا خطة واضحة لتفكيك هذه الكلاسات مستقبلاً.")

    # --------------------------------------------------------------------------
    # SLIDE 7: Traceability Matrix (RTM)
    # --------------------------------------------------------------------------
    s7 = prs.slides.add_slide(blank)
    create_slide_header(s7, 7, "إدارة التتبع والتحقق", "مصفوفة تتبع المتطلبات (Requirements Traceability Matrix)", "الربط الثلاثي: من المتطلب إلى الكود الفعلي والاختبارات ومعايير القبول", "docs/TraceabilityMatrix.md", COLOR_GREEN)

    # Clean Table
    t_shape = s7.shapes.add_table(6, 4, Inches(0.9), Inches(1.8), Inches(11.5), Inches(4.8))
    tbl = t_shape.table
    tbl.columns[0].width = Inches(1.5)  # Req ID
    tbl.columns[1].width = Inches(2.7)  # Title
    tbl.columns[2].width = Inches(4.3)  # Code Evidence
    tbl.columns[3].width = Inches(3.0)  # Status

    hdrs = ["رمز المتطلب", "عنوان المتطلب الوظيفي", "الدليل من الكود المصدري الفعلي", "الحالة الهندسية الدقيقة"]
    for cidx, h in enumerate(hdrs):
        cell = tbl.cell(0, cidx)
        cell.fill.solid()
        cell.fill.fore_color.rgb = COLOR_CARD_BG
        p = cell.text_frame.paragraphs[0]
        p.text = h
        p.font.name = FONT_HEADING
        p.font.size = Pt(15)
        p.font.bold = True
        p.font.color.rgb = COLOR_NAVY

    rtm_data = [
        ("FR-01", "اكتشاف الأجهزة المحلية", "LocalP2PEngine.kt — startDiscoveryListener() مع Multicast 8888", "Implemented - Not Tested\n(يحتاج اختبار جهازين)"),
        ("FR-05", "إرسال رسالة فردية", "LocalP2PEngine.kt & AppDatabase.kt — حزمة CHAT_MSG وجدول chat_messages", "Implemented - Not Tested\n(مبني في الكود والقاعدة)"),
        ("FR-13", "حذف الرسالة شبكياً", "LocalP2PEngine.kt — (لا يوجد كود أو حزمة شبكية للحذف عن بعد)", "Missing / Not Implemented\n(موثق كفجوة بأمانة)"),
        ("FR-17", "استئناف نقل الملفات", "FileTransferEngine.kt & LocalCryptoEngine.kt — اشتقاق الإزاحة والـ Nonce", "Implemented - Not Tested\n(مبني ويحتاج قياس)"),
        ("FR-28", "تشفير الحزم AES-GCM", "LocalCryptoEngine.kt — encrypt() بنمط AES/GCM/NoPadding و Tag 128", "JVM Tested (10/10 Passed)\n(مثبت آلياً بالاختبارات)")
    ]
    for ridx, row in enumerate(rtm_data):
        for cidx, val in enumerate(row):
            cell = tbl.cell(ridx + 1, cidx)
            cell.fill.solid()
            cell.fill.fore_color.rgb = RGBColor(255, 255, 255) if ridx % 2 == 0 else COLOR_CARD_BG
            p = cell.text_frame.paragraphs[0]
            p.text = val
            p.font.name = FONT_MONO if cidx == 0 or "Engine" in val else FONT_BODY
            p.font.size = Pt(13)
            p.font.bold = True
            
            if cidx == 3:
                if "Tested" in val and "Not" not in val:
                    p.font.color.rgb = COLOR_GREEN
                elif "Missing" in val:
                    p.font.color.rgb = COLOR_RED
                else:
                    p.font.color.rgb = COLOR_AMBER
            else:
                p.font.color.rgb = COLOR_TEXT_MAIN

    set_speaker_notes(s7, "نستعرض للأستاذ مصفوفة تتبع المتطلبات RTM في ملف TraceabilityMatrix.md المكون من 324 سطراً. تربط المصفوفة 31 متطلباً وظيفياً بالكود المصدري الدقيق، وتفصل بين ما تم اختباره على JVM وما هو مبني ويحتاج أجهزة، وما هو مفقود وموثق كفجوة.")

    # --------------------------------------------------------------------------
    # SLIDE 8: Change Management & Backward Compatibility
    # --------------------------------------------------------------------------
    s8 = prs.slides.add_slide(blank)
    create_slide_header(s8, 8, "إدارة التغيير المعماري", "إدارة التغيير والتوافق العكسي (Change Management)", "تطبيق نمط المزود (Provider Pattern) لإعادة الهيكلة دون كسر النظام القائم", "ZERO BREAKING CHANGES", COLOR_GREEN)

    c8_1 = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c8_1.fill.solid()
    c8_1.fill.fore_color.rgb = COLOR_CARD_BG
    c8_1.line.color.rgb = COLOR_BLUE_ACCENT
    c8_1.line.width = Pt(1.5)
    tf8_1 = c8_1.text_frame
    tf8_1.word_wrap = True
    p = tf8_1.paragraphs[0]
    p.text = "🔄 تحدي التغيير ونمط التصميم المطبق:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    ch_pts = [
        "التحدي الهندسي: تحديث وتطوير منظومة التشفير لدعم المفاتيح الديناميكية دون كسر عشرات المسارات التي تستدعي التشفير القديم في الواجهات والمكالمات.",
        "نمط المزود (Provider / Strategy Pattern): إنشاء واجهة KeyProvider.kt لفصل منطق جلب واشتقاق المفتاح عن عمليات Cipher.",
        "التوافق العكسي (Backward Compatibility): بناء LegacyStaticKeyProvider ليحتفظ بسلوك المفتاح القديم كخيار افتراضي (Default).",
        "صفر تعديلات كاسرة (Zero Breaking Changes): لم يتأثر أي سطر كود خارجي يستدعي encrypt() و decrypt()."
    ]
    for pt in ch_pts:
        p = tf8_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    c8_2 = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c8_2.fill.solid()
    c8_2.fill.fore_color.rgb = COLOR_CARD_BG
    c8_2.line.color.rgb = COLOR_CARD_BORDER
    c8_2.line.width = Pt(1.5)
    tf8_2 = c8_2.text_frame
    tf8_2.word_wrap = True
    p = tf8_2.paragraphs[0]
    p.text = "📂 الملفات الفعلية والمسؤوليات:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    ch_files = [
        ("KeyProvider.kt (ملف مضاف جديد):", "الأسطر 17-26: تعريف الواجهة المجردة KeyProvider.\nالأسطر 38-94: كلاس LegacyStaticKeyProvider للتوافق العكسي."),
        ("LocalCryptoEngine.kt (تعديل معماري):", "الأسطر 28-30: استقبال المزود عبر @Volatile var keyProvider: KeyProvider.\nالأسطر 43-45: استرجاع المفتاح عبر keyProvider.getNetworkKey()."),
        ("الأثر الهندسي:", "إتاحة التبديل بين مفتاح PBKDF2 القديم ومفاتيح الجلسات الديناميكية و Android Keystore في وقت التشغيل (Runtime) بسلاسة.")
    ]
    for title, desc in ch_files:
        p = tf8_2.add_paragraph()
        p.text = f"• {title}"
        p.font.name = FONT_HEADING
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_BLUE_ACCENT
        p.space_before = Pt(8)
        p_d = tf8_2.add_paragraph()
        p_d.text = f"  {desc}"
        p_d.font.name = FONT_BODY
        p_d.font.size = Pt(15)
        p_d.font.bold = True
        p_d.font.color.rgb = COLOR_TEXT_MAIN

    set_speaker_notes(s8, "نشرح هنا إدارة التغيير: عند ترقية التشفير، طبقنا نمط Provider وعزلنا مسؤولية المفتاح في KeyProvider.kt مع الحفاظ على التوافق العكسي في LegacyStaticKeyProvider. النتيجة الهندسية: صفر تعديلات كاسرة في كود الشبكة والواجهات القديمة.")

    # --------------------------------------------------------------------------
    # SLIDE 9: Testing & Quality Assurance (QA)
    # --------------------------------------------------------------------------
    s9 = prs.slides.add_slide(blank)
    create_slide_header(s9, 9, "ضمان الجودة والتحقق", "استراتيجية الاختبارات وضمان الجودة (QA & Verification)", "إثبات سلوك المحركات عبر 33 اختبار وحدة آلي ناجح بنسبة 100%", "33/33 TESTS PASSED", COLOR_GREEN)

    c9 = s9.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(11.5), Inches(4.8))
    c9.fill.solid()
    c9.fill.fore_color.rgb = COLOR_CARD_BG
    c9.line.color.rgb = COLOR_GREEN
    c9.line.width = Pt(1.5)
    tf9 = c9.text_frame
    tf9.word_wrap = True

    p = tf9.paragraphs[0]
    p.text = "🧪 نتائج حزم الاختبارات المؤتمتة ومحدداتها المنهجية:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(22)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    qa_pts = [
        ("LocalCryptoEngineTest.kt (10 اختبارات):", "فحص التشفير، فك التشفير، كشف التلاعب بالبيانات، واشتقاق Nonce فريد للكتل."),
        ("EcdhEngineTest.kt (8 اختبارات):", "فحص توليد مفاتيح P-256 المؤقتة، وتطابق السر المشترك، وتبادل المفاتيح بصيغة X.509."),
        ("KeyDerivationTest.kt (8 اختبارات):", "فحص دالة اشتقاق المفاتيح HKDF-SHA256 وفق RFC 5869 ونواقل الاختبار الرسمية."),
        ("CryptoEngineTest.kt (7 اختبارات):", "فحص بروتوكول الجلسات وحماية الإرسال والاستقبال المتزامن."),
        ("الضوابط الأكاديمية الصريحة:", "الاختبارات أثبتت صحة المنطق والرياضيات على JVM بنسبة 100%، لكنها لا تغني عن الاختبارات التكاملية على جهازين حقيقيين (Two Physical Devices) للتحقق من أداء الشبكة الواقعي.")
    ]
    for title, desc in qa_pts:
        p = tf9.add_paragraph()
        p.text = f"• {title} {desc}"
        p.font.name = FONT_BODY
        p.font.size = Pt(17)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(12)

    set_speaker_notes(s9, "نؤكد للأستاذ على استراتيجية ضمان الجودة: بنينا 33 اختبار وحدة آلي تفحص المحركات الحساسة وتنجح بنسبة 100%. واختتمنا بالتمييز الأكاديمي: الاختبارات تثبت المنطق البرمجي، لكن إثبات الأداء الشبكي التام يتطلب فحصاً على أجهزة حقيقية.")

    # --------------------------------------------------------------------------
    # SLIDE 10: Engineering Toolchain & Build System
    # --------------------------------------------------------------------------
    s10 = prs.slides.add_slide(blank)
    create_slide_header(s10, 10, "أدوات التطوير والبناء", "منظومة أدوات البناء وتوليد الكود (Engineering Toolchain)", "تكامل منظومة Gradle 9.4 و Kotlin 2.2 و KSP في دورة حياة التطبيق", "TOOLCHAIN & BUILD", COLOR_BLUE_ACCENT)

    # 4 Clean Cards
    tcards = [
        ("⚙️ محرك البناء والـ Wrapper", "Gradle 9.4.1 (Kotlin DSL)", "إدارة دورة حياة البناء، تنزيل التبعيات تلقائياً، وعزل بيئة البناء لضمان التكرارية عبر gradle-wrapper.properties."),
        ("🤖 مجمع أندرويد الرسمي", "Android Gradle Plugin 9.1.1", "دعم أحدث واجهات أندرويد 16 (compileSdk 36)، وتصحيح الأرقام الهندية-العربية عبر NormalizeGeneratedDigitsTask."),
        ("💎 لغة البرمجة والتزامن", "Kotlin 2.2.10 & Coroutines", "إدارة غير تزامنية عالية الكفاءة للمقابس والشبكة دون حظر واجهة المستخدم عبر Coroutines و Dispatchers.IO."),
        ("⚡ معالجة الرموز البرمجية", "KSP (Kotlin Symbol Processing)", "توليد كود Room Database 6 ومحولات Moshi JSON بسرعة فائقة أثناء وقت الترجمة (Compile-time) بدلاً من kapt.")
    ]
    for idx, (tt, tp, td) in enumerate(tcards):
        col = idx % 2
        row = idx // 2
        tx = Inches(0.9 + col * 5.8)
        ty = Inches(1.8 + row * 2.45)
        c = s10.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, tx, ty, Inches(5.6), Inches(2.25))
        c.fill.solid()
        c.fill.fore_color.rgb = COLOR_CARD_BG
        c.line.color.rgb = COLOR_CARD_BORDER
        c.line.width = Pt(1.5)
        tf = c.text_frame
        tf.word_wrap = True
        p1 = tf.paragraphs[0]
        p1.text = tt
        p1.font.name = FONT_HEADING
        p1.font.size = Pt(17)
        p1.font.bold = True
        p1.font.color.rgb = COLOR_NAVY
        p2 = tf.add_paragraph()
        p2.text = tp
        p2.font.name = FONT_MONO
        p2.font.size = Pt(13)
        p2.font.bold = True
        p2.font.color.rgb = COLOR_BLUE_ACCENT
        p2.space_before = Pt(3)
        p3 = tf.add_paragraph()
        p3.text = td
        p3.font.name = FONT_BODY
        p3.font.size = Pt(15)
        p3.font.bold = True
        p3.font.color.rgb = COLOR_TEXT_MAIN
        p3.space_before = Pt(4)

    set_speaker_notes(s10, "نستعرض منظومة الأدوات الهندسية: نستخدم Gradle 9.4 و Kotlin 2.2 و KSP لمعالجة الرموز البرمجية بسرعة، مع ضبط build.gradle.kts لتوليد APK سليم ومختبر والتكامل الكامل مع Git.")

    # --------------------------------------------------------------------------
    # SLIDE 11: Risk Management & Documented Gaps
    # --------------------------------------------------------------------------
    s11 = prs.slides.add_slide(blank)
    create_slide_header(s11, 11, "إدارة المخاطر والنزاهة العلمية", "إدارة المخاطر والفجوات الموثقة (Risk Management & Gaps)", "التوثيق الصريح للقيود كأحد أهم معايير النضج الهندسي والأكاديمي", "HONEST ACADEMIC GAPS", COLOR_AMBER)

    c11_1 = s11.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c11_1.fill.solid()
    c11_1.fill.fore_color.rgb = COLOR_CARD_BG
    c11_1.line.color.rgb = COLOR_RED
    c11_1.line.width = Pt(1.5)
    tf11_1 = c11_1.text_frame
    tf11_1.word_wrap = True
    p = tf11_1.paragraphs[0]
    p.text = "⚠️ ميزات موثقة كفجوات رسمية في SRS:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(21)
    p.font.bold = True
    p.font.color.rgb = COLOR_RED

    se_gaps = [
        "FR-13: حذف الرسائل شبكياً — متاح محلياً فقط في قاعدة بيانات الهاتف، ولا توجد حزمة شبكية لحذفها من أجهزة الأقران عن بعد.",
        "FR-19: فحص سلامة الملف كاملاً — الملف ينقل مجزأً ومشفر بنجاح، لكن تنقص مقارنة SHA-256 للملف كاملاً بعد اكتمال التنزيل.",
        "مخاطر الشبكة الواقعية: عزل العملاء (AP Isolation) في أجهزة الراوتر يمنع الأجهزة من رؤية بعضها محلياً.",
        "قيود أندرويد على Multicast: تتطلب أجهزة أندرويد حيازة MulticastLock بشكل صريح وإلا قد تُسقط حزم الاكتشاف."
    ]
    for pt in se_gaps:
        p = tf11_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    c11_2 = s11.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c11_2.fill.solid()
    c11_2.fill.fore_color.rgb = COLOR_CARD_BG
    c11_2.line.color.rgb = COLOR_GREEN
    c11_2.line.width = Pt(1.5)
    tf11_2 = c11_2.text_frame
    tf11_2.word_wrap = True
    p = tf11_2.paragraphs[0]
    p.text = "🛡️ استراتيجيات التخفيف والشفافية:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(21)
    p.font.bold = True
    p.font.color.rgb = COLOR_GREEN

    se_mitig = [
        "التوثيق الشفاف: إدراج الفجوات صراحة في وثيقتي SRS و TraceabilityMatrix كـ Missing لمنع الادعاءات المضللة.",
        "بدائل الاكتشاف الشبكي: إضافة مسح الشبكة الفرعية (Subnet Sweep) كبديل احتياطي في حال حظر حزم الـ Multicast على المنفذ 8888.",
        "المعايير الهندسية: التعامل مع الفجوات كـ Backlog رسمي للتطوير المستقبلي بدلاً من تجاهلها.",
        "القيمة المضافة: إثبات النزاهة العلمية يمنح المشروع مصداقية أكاديمية عالية أمام لجنة التحكيم."
    ]
    for pt in se_mitig:
        p = tf11_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    set_speaker_notes(s11, "نبيّن للأستاذ النزاهة الهندسية: اعترفنا بالقيود صراحة؛ ميزة الحذف شبكياً غير موجودة ووثقناها كـ Missing، وفحص سلامة الملف الكامل مقترح مستقبلي، وقيود عزل الراوتر وثقناها ووضعنا مسح الـ Subnet كبديل. هندسة البرمجيات تعني الشفافية والوعي بالمخاطر.")

    # --------------------------------------------------------------------------
    # SLIDE 12: Executive Summary & Roadmap
    # --------------------------------------------------------------------------
    s12 = prs.slides.add_slide(blank)
    create_slide_header(s12, 12, "الخاتمة والتوصيات", "الخلاصة الهندسية وخارطة الطريق المستقبلية (Roadmap)", "حصيلة الإنجاز الهندسي وما تم إثباته وما يقع خارج النطاق", "EXECUTIVE SUMMARY", COLOR_GREEN)

    c12_1 = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.9), Inches(1.8), Inches(5.6), Inches(4.8))
    c12_1.fill.solid()
    c12_1.fill.fore_color.rgb = COLOR_CARD_BG
    c12_1.line.color.rgb = COLOR_GREEN
    c12_1.line.width = Pt(1.5)
    tf12_1 = c12_1.text_frame
    tf12_1.word_wrap = True
    p = tf12_1.paragraphs[0]
    p.text = "✅ ما تم إنجازه وتوثيقه هندسياً:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(21)
    p.font.bold = True
    p.font.color.rgb = COLOR_GREEN

    fin_acc = [
        "صياغة وثيقة SRS.md تضم 31 متطلباً و 15 قصة مستخدم بنموذج تصنيف خماسي صارم.",
        "تثبيت خط الأساس البرمجي (Baseline Commit 7111182) لضمان تكرارية البناء.",
        "بناء مصفوفة تتبع RTM تربط كل متطلب بالكود المصدري ومعايير القبول.",
        "تطبيق نمط المزود Provider لتحقيق إدارة تغيير آمنة دون كسر الكود القديم.",
        "إثبات سلامة المحركات الحساسة عبر 33 اختبار وحدة آلي ناجح بنسبة 100%."
    ]
    for pt in fin_acc:
        p = tf12_1.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    c12_2 = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.8), Inches(5.6), Inches(4.8))
    c12_2.fill.solid()
    c12_2.fill.fore_color.rgb = COLOR_CARD_BG
    c12_2.line.color.rgb = COLOR_BLUE_ACCENT
    c12_2.line.width = Pt(1.5)
    tf12_2 = c12_2.text_frame
    tf12_2.word_wrap = True
    p = tf12_2.paragraphs[0]
    p.text = "🚀 خارطة الطريق الهندسية المستقبلية:"
    p.font.name = FONT_HEADING
    p.font.size = Pt(21)
    p.font.bold = True
    p.font.color.rgb = COLOR_NAVY

    fin_road = [
        "1. تفكيك الكلاسات الضخمة (God Classes): تقسيم LocalP2PEngine و MainViewModel إلى خدمات فرعية متخصصة ومستقلة.",
        "2. بناء اختبارات تكاملية على أجهزة حقيقية: إعداد Instrumented Tests عبر AndroidX Test و Espresso لفحص بيئة الشبكة الفعلية.",
        "3. استكمال ميزة الحذف الشبكي (FR-13) وتدقيق تجزئة الملف النهائي SHA-256.",
        "الخلاصة: تحول المشروع من كود أولي غير منظم إلى منظومة برمجية خاضعة لأرقى معايير هندسة البرمجيات وقابلة للاستدامة والتطوير."
    ]
    for pt in fin_road:
        p = tf12_2.add_paragraph()
        p.text = f"• {pt}"
        p.font.name = FONT_BODY
        p.font.size = Pt(16)
        p.font.bold = True
        p.font.color.rgb = COLOR_TEXT_MAIN
        p.space_before = Pt(10)

    set_speaker_notes(s12, "نختم المناقشة: فرضنا الانضباط الهندسي الكامل عبر وثائق SRS، خط الأساس، مصفوفة التتبع، وإدارة التغيير الآمنة مع 33 اختباراً ناجحاً. ووضعنا خارطة طريق واضحة لتفكيك الكلاسات واختبار الأجهزة الحقيقية مستقبلاً. نشكركم على حسن الاستماع ومستعدون للإجابة عن أسئلتكم.")

    # Save Presentation
    p_en = os.path.abspath("LOCAL_CONTACT_Software_Engineering_Presentation.pptx")
    prs.save(p_en)
    print(f"[OK] Software Engineering Presentation Updated: {p_en} ({os.path.getsize(p_en):,} bytes)")

if __name__ == "__main__":
    print("Building refined presentations with unified academic projector theme...")
    build_security_presentation()
    build_se_presentation()
    print("All presentations regenerated successfully!")
