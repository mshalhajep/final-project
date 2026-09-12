import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE

# Initialize Presentation with 16:9 widescreen
prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)

# Visual Identity Palette: Dark Navy & Structured Engineering Accents
COLOR_BG = RGBColor(10, 17, 40)             # Deep Academic Navy (#0A1128)
COLOR_CARD_BG = RGBColor(18, 28, 56)        # Dark Slate Card (#121C38)
COLOR_CARD_BORDER = RGBColor(37, 56, 99)    # Card Border (#253863)
COLOR_PRIMARY_BLUE = RGBColor(56, 189, 248) # Sky Blue Accent (#38BDF8)
COLOR_ROYAL_BLUE = RGBColor(37, 99, 235)    # Architectural Blue (#2563EB)
COLOR_DEEP_BLUE = RGBColor(29, 78, 216)     # Navy Accent (#1D4ED8)
COLOR_TEXT_WHITE = RGBColor(248, 250, 252)  # Clean Pure Text (#F8FAFC)
COLOR_TEXT_MUTED = RGBColor(148, 163, 184)  # Secondary Muted Text (#94A3B8)
COLOR_TEXT_DIM = RGBColor(100, 116, 139)    # Dark Dim Text (#64748B)

# Status Colors
COLOR_GREEN_BG = RGBColor(6, 78, 59)
COLOR_GREEN_TEXT = RGBColor(52, 211, 153)   # Implemented / Tested
COLOR_AMBER_BG = RGBColor(120, 53, 15)
COLOR_AMBER_TEXT = RGBColor(251, 191, 36)   # Partially Implemented / Pending
COLOR_RED_BG = RGBColor(127, 29, 29)
COLOR_RED_TEXT = RGBColor(248, 113, 113)    # Missing / Architectural Smell / Risk

FONT_ARABIC = "Cairo"
FONT_MONO = "Consolas"

blank_slide_layout = prs.slide_layouts[6]

def apply_background(slide):
    bg = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(7.5))
    bg.fill.solid()
    bg.fill.fore_color.rgb = COLOR_BG
    bg.line.fill.background()
    return bg

def add_header(slide, slide_num, category, title, subtitle, status_text=None, status_type="blue"):
    apply_background(slide)

    # Top accent line
    top_bar = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(0.12))
    top_bar.fill.solid()
    top_bar.fill.fore_color.rgb = COLOR_ROYAL_BLUE
    top_bar.line.fill.background()

    # Category indicator
    cat_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.25), Inches(8.5), Inches(0.38))
    tf_cat = cat_box.text_frame
    tf_cat.word_wrap = True
    p_cat = tf_cat.paragraphs[0]
    p_cat.text = f"• {category} | هندسة البرمجيات — مشروع LOCAL CONTACT P2P"
    p_cat.font.name = FONT_ARABIC
    p_cat.font.size = Pt(11)
    p_cat.font.bold = True
    p_cat.font.color.rgb = COLOR_PRIMARY_BLUE

    # Slide Number pill (X / 12)
    num_box = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(11.4), Inches(0.25), Inches(1.1), Inches(0.38))
    num_box.fill.solid()
    num_box.fill.fore_color.rgb = COLOR_CARD_BG
    num_box.line.color.rgb = COLOR_ROYAL_BLUE
    num_box.line.width = Pt(1.5)
    p_num = num_box.text_frame.paragraphs[0]
    p_num.text = f"{slide_num} / 12"
    p_num.alignment = PP_ALIGN.CENTER
    p_num.font.name = FONT_MONO
    p_num.font.size = Pt(11)
    p_num.font.bold = True
    p_num.font.color.rgb = COLOR_PRIMARY_BLUE

    # Title
    t_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.68), Inches(9.6), Inches(0.55))
    tf_t = t_box.text_frame
    tf_t.word_wrap = True
    p_t = tf_t.paragraphs[0]
    p_t.text = title
    p_t.font.name = FONT_ARABIC
    p_t.font.size = Pt(20)
    p_t.font.bold = True
    p_t.font.color.rgb = COLOR_TEXT_WHITE

    # Subtitle
    sub_box = slide.shapes.add_textbox(Inches(0.8), Inches(1.22), Inches(9.6), Inches(0.38))
    tf_sub = sub_box.text_frame
    tf_sub.word_wrap = True
    p_sub = tf_sub.paragraphs[0]
    p_sub.text = subtitle
    p_sub.font.name = FONT_ARABIC
    p_sub.font.size = Pt(11.5)
    p_sub.font.bold = True
    p_sub.font.color.rgb = COLOR_TEXT_MUTED

    # Status Pill if specified
    if status_text:
        bg_col = COLOR_CARD_BG
        txt_col = COLOR_PRIMARY_BLUE
        border_col = COLOR_ROYAL_BLUE
        if status_type == "green":
            bg_col = COLOR_GREEN_BG
            txt_col = COLOR_GREEN_TEXT
            border_col = COLOR_GREEN_TEXT
        elif status_type == "amber":
            bg_col = COLOR_AMBER_BG
            txt_col = COLOR_AMBER_TEXT
            border_col = COLOR_AMBER_TEXT
        elif status_type == "red":
            bg_col = COLOR_RED_BG
            txt_col = COLOR_RED_TEXT
            border_col = COLOR_RED_TEXT
        
        stat_shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(9.8), Inches(0.75), Inches(2.7), Inches(0.42))
        stat_shape.fill.solid()
        stat_shape.fill.fore_color.rgb = bg_col
        stat_shape.line.color.rgb = border_col
        stat_shape.line.width = Pt(1.5)
        p_stat = stat_shape.text_frame.paragraphs[0]
        p_stat.text = status_text
        p_stat.alignment = PP_ALIGN.CENTER
        p_stat.font.name = FONT_MONO
        p_stat.font.size = Pt(10)
        p_stat.font.bold = True
        p_stat.font.color.rgb = txt_col

    # Bottom separation line
    line = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.8), Inches(1.68), Inches(11.7), Inches(0.02))
    line.fill.solid()
    line.fill.fore_color.rgb = COLOR_CARD_BORDER
    line.line.fill.background()

    # Footer slide indicator for strict adherence
    foot_box = slide.shapes.add_textbox(Inches(0.8), Inches(7.05), Inches(11.7), Inches(0.35))
    tf_foot = foot_box.text_frame
    p_f = tf_foot.paragraphs[0]
    p_f.text = f"مشروع LOCAL CONTACT P2P — وثائق ومناقشة هندسة البرمجيات الأكاديمية | الشريحة رقم {slide_num} من أصل 12 شريحة"
    p_f.font.name = FONT_ARABIC
    p_f.font.size = Pt(9.5)
    p_f.font.bold = True
    p_f.font.color.rgb = COLOR_TEXT_DIM

def set_speaker_notes(slide, notes_text):
    notes_slide = slide.notes_slide
    text_frame = notes_slide.notes_text_frame
    text_frame.text = "🎙️ ملاحظات الطالب الشفهية لمناقشة أستاذ هندسة البرمجيات:\n" + notes_text

# ==============================================================================
# SLIDE 1: Cover Slide
# ==============================================================================
s1 = prs.slides.add_slide(blank_slide_layout)
apply_background(s1)

# Top & bottom accent bands
top_b = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(0), Inches(13.333), Inches(0.2))
top_b.fill.solid()
top_b.fill.fore_color.rgb = COLOR_ROYAL_BLUE
top_b.line.fill.background()

bot_b = s1.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0), Inches(7.3), Inches(13.333), Inches(0.2))
bot_b.fill.solid()
bot_b.fill.fore_color.rgb = COLOR_DEEP_BLUE
bot_b.line.fill.background()

# Badge
badge = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(3.6), Inches(0.45), Inches(6.1), Inches(0.45))
badge.fill.solid()
badge.fill.fore_color.rgb = COLOR_CARD_BG
badge.line.color.rgb = COLOR_PRIMARY_BLUE
badge.line.width = Pt(1.5)
p_b = badge.text_frame.paragraphs[0]
p_b.text = "📐 المناقشة الأكاديمية — مادة هندسة البرمجيات المتقدمة"
p_b.alignment = PP_ALIGN.CENTER
p_b.font.name = FONT_ARABIC
p_b.font.size = Pt(12)
p_b.font.bold = True
p_b.font.color.rgb = COLOR_PRIMARY_BLUE

# Title
t_box = s1.shapes.add_textbox(Inches(0.8), Inches(1.02), Inches(11.7), Inches(0.9))
p_t = t_box.text_frame.paragraphs[0]
p_t.text = "هندسة البرمجيات في مشروع LOCAL CONTACT"
p_t.alignment = PP_ALIGN.CENTER
p_t.font.name = FONT_ARABIC
p_t.font.size = Pt(29)
p_t.font.bold = True
p_t.font.color.rgb = COLOR_TEXT_WHITE

# Subtitle
sub_box = s1.shapes.add_textbox(Inches(1.2), Inches(1.9), Inches(10.9), Inches(0.55))
p_sub = sub_box.text_frame.paragraphs[0]
p_sub.text = "تحليل المتطلبات، التصميم المعماري، إدارة التغيير، الاختبارات، والتتبع"
p_sub.alignment = PP_ALIGN.CENTER
p_sub.font.name = FONT_ARABIC
p_sub.font.size = Pt(15)
p_sub.font.bold = True
p_sub.font.color.rgb = COLOR_PRIMARY_BLUE

# Engineering Core Pillars Cards (4 Cards)
pillars = [
    ("📋 هندسة المتطلبات (SRS)", "توثيق 31 متطلباً وظيفياً و 15 قصة مستخدم مع تصنيف خماسي صارم"),
    ("🏛️ التصميم المعماري (Architecture)", "معمارية P2P لا مركزية، تفكيك المسؤوليات، وتجريد واجهات SOLID"),
    ("🔄 إدارة التغيير وخط الأساس", "تثبيت Baseline Commit 7111182 وتطبيق نمط Provider لمنع كسر الكود"),
    ("🔍 مصفوفة التتبع والتحقق (RTM)", "ربط المتطلبات بالكود وبالاختبارات (33/33 اختبار وحدة JVM ناجح)")
]

for idx, (p_title, p_desc) in enumerate(pillars):
    col = idx % 2
    row = idx // 2
    cx = Inches(1.1 + col * 5.7)
    cy = Inches(2.65 + row * 1.1)
    
    c = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, cx, cy, Inches(5.4), Inches(0.95))
    c.fill.solid()
    c.fill.fore_color.rgb = COLOR_CARD_BG
    c.line.color.rgb = COLOR_CARD_BORDER
    c.line.width = Pt(1.5)
    
    tf = c.text_frame
    tf.word_wrap = True
    p1 = tf.paragraphs[0]
    p1.text = p_title
    p1.font.name = FONT_ARABIC
    p1.font.size = Pt(12)
    p1.font.bold = True
    p1.font.color.rgb = COLOR_TEXT_WHITE
    
    p2 = tf.add_paragraph()
    p2.text = p_desc
    p2.font.name = FONT_ARABIC
    p2.font.size = Pt(10)
    p2.font.color.rgb = COLOR_TEXT_MUTED

# Students Box
team_box = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(4.95), Inches(11.7), Inches(1.4))
team_box.fill.solid()
team_box.fill.fore_color.rgb = COLOR_CARD_BG
team_box.line.color.rgb = COLOR_ROYAL_BLUE
team_box.line.width = Pt(1.5)

tf_team = team_box.text_frame
p_tt = tf_team.paragraphs[0]
p_tt.text = "👥 الطلاب المنفذون للمشروع:"
p_tt.alignment = PP_ALIGN.CENTER
p_tt.font.name = FONT_ARABIC
p_tt.font.size = Pt(12)
p_tt.font.bold = True
p_tt.font.color.rgb = COLOR_PRIMARY_BLUE

students = [
    "إعداد الطالب: مشعل حاجب",
    "إعداد الطالب: محمد العيدروس",
    "إعداد الطالب: قحطان الشاجع",
    "إعداد الطالب: أواب النزييلي",
    "إعداد الطالب: محمد العواضي"
]

for sidx, sname in enumerate(students):
    sx = Inches(1.0 + sidx * 2.3)
    sy = Inches(5.55)
    scard = s1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, sx, sy, Inches(2.2), Inches(0.55))
    scard.fill.solid()
    scard.fill.fore_color.rgb = RGBColor(26, 42, 80)
    scard.line.color.rgb = COLOR_ROYAL_BLUE
    scard.line.width = Pt(1)
    
    sp = scard.text_frame.paragraphs[0]
    sp.text = sname
    sp.alignment = PP_ALIGN.CENTER
    sp.font.name = FONT_ARABIC
    sp.font.size = Pt(10)
    sp.font.bold = True
    sp.font.color.rgb = COLOR_TEXT_WHITE

# Meta bottom line
foot_s1 = s1.shapes.add_textbox(Inches(0.8), Inches(6.5), Inches(11.7), Inches(0.4))
p_fs1 = foot_s1.text_frame.paragraphs[0]
p_fs1.text = "مقدم إلى: أستاذ هندسة البرمجيات | البيئة: Android SDK (API 24+) & Kotlin 2.2 | Commit: 7111182 | الشريحة: 1 / 12"
p_fs1.alignment = PP_ALIGN.CENTER
p_fs1.font.name = FONT_ARABIC
p_fs1.font.size = Pt(10.5)
p_fs1.font.bold = True
p_fs1.font.color.rgb = COLOR_TEXT_MUTED

set_speaker_notes(s1, "أهلاً بكم يا دكتور في مناقشة الجانب الهندسي لمشروع LOCAL CONTACT. في هذا العرض، نركز على كيفية إخضاع المشروع لمبادئ هندسة البرمجيات المنهجية (تحليل المتطلبات، خط الأساس، مصفوفة التتبع، الروائح المعمارية، إدارة التغيير، واستراتيجية الاختبارات) بدلاً من الاكتفاء بعرض كود مجرد. قمنا بإعداد هذا العمل كفريق متكامل يضم الزملاء: مشعل حاجب، محمد العيدروس، قحطان الشاجع، أواب النزييلي، ومحمد العواضي.")

# ==============================================================================
# SLIDE 2: Pre-SE Analysis & Discovered Engineering Smells
# ==============================================================================
s2 = prs.slides.add_slide(blank_slide_layout)
add_header(s2, 2, "تشخيص واقع النظام", "حالة المشروع قبل التدخل الهندسي والمشكلات المكتشفة", "تحليل الفجوات المعمارية والتوثيقية التي استدعت تطبيق مبادئ هندسة البرمجيات", "PRE-SE AUDIT", "red")

# Card 1: Architectural & Documentation Gaps
c2_1 = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(4.9))
c2_1.fill.solid()
c2_1.fill.fore_color.rgb = COLOR_CARD_BG
c2_1.line.color.rgb = COLOR_RED_TEXT
c2_1.line.width = Pt(1.5)

tf2_1 = c2_1.text_frame
tf2_1.word_wrap = True
p = tf2_1.paragraphs[0]
p.text = "⚠️ المشكلات الهندسية والتوثيقية المكتشفة:"
p.font.name = FONT_ARABIC
p.font.size = Pt(14)
p.font.bold = True
p.font.color.rgb = COLOR_RED_TEXT

points_pre = [
    ("انعدام وثائق المتطلبات الرسمية (No SRS):", "التطبيق كان يحتوي على ميزات عديدة لكن دون توصيف رسمي للمتطلبات الوظيفية أو معايير قبول (Acceptance Criteria) محددة."),
    ("غياب خط الأساس الهندسي (No Baseline):", "لم يكن هناك Commit مرجعي ثابت يمثل خط الأساس للمشروع، مما صعب قياس الأثر والتحكم في التغيير والتفرع."),
    ("غياب مصفوفة التتبع (No Traceability):", "استحالة معرفة أي متطلب يقابله أي ملف برمجي، وأي أجزاء خضعت لاختبار وأيها يمثل ميزة غير مكتملة."),
    ("انعدام الاختبارات الآلية (Zero Unit Tests):", "كان المستودع يخلو تماماً من أي اختبارات آلية تفحص المحركات الحساسة وتثبت سلوكها برمجياً.")
]

for title, desc in points_pre:
    p = tf2_1.add_paragraph()
    p.text = f"• {title}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(11)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(8)
    
    p_sub = tf2_1.add_paragraph()
    p_sub.text = f"  {desc}"
    p_sub.font.name = FONT_ARABIC
    p_sub.font.size = Pt(9.5)
    p_sub.font.color.rgb = COLOR_TEXT_MUTED

# Card 2: Code & Coupling Smells
c2_2 = s2.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(4.9))
c2_2.fill.solid()
c2_2.fill.fore_color.rgb = COLOR_CARD_BG
c2_2.line.color.rgb = COLOR_CARD_BORDER
c2_2.line.width = Pt(1.5)

tf2_2 = c2_2.text_frame
tf2_2.word_wrap = True
p = tf2_2.paragraphs[0]
p.text = "🧩 الروائح المعمارية في الكود المصدري (Code Smells):"
p.font.name = FONT_ARABIC
p.font.size = Pt(14)
p.font.bold = True
p.font.color.rgb = COLOR_AMBER_TEXT

points_code = [
    ("ظاهرة الكلاسات المتضخمة (God Classes):", "ملف LocalP2PEngine.kt بلغ 2,496 سطراً، وملف MainViewModel.kt بلغ 1,839 سطراً؛ مما انتهك مبدأ المسؤولية الواحدة (SRP)."),
    ("الاقتران الشديد في إدارة التشفير (Tight Coupling):", "كلاس LocalCryptoEngine كان يشتق المفتاح الثابت داخلياً ويدير عمليات Cipher معاً، مما صعّب إدخال أي خوارزميات جديدة دون كسر الكود."),
    ("فجوات وظيفية صامتة (Silent Missing Features):", "ميزات مهمة مثل حذف الرسائل شبكياً من أجهزة الأقران أو فحص الـ Hash للملفات المستلمة لم تكن موجودة ولكن لم توثق كفجوة."),
    ("الخلط بين الإمكانية البرمجية والجاهزية التشغيلية:", "وجود كود يرسل حزم صوت لا يعني إثبات عمله تحت ظروف انقطاع الشبكة وضياع الحزم دون قياس واختبار.")
]

for title, desc in points_code:
    p = tf2_2.add_paragraph()
    p.text = f"• {title}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(11)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(8)
    
    p_sub = tf2_2.add_paragraph()
    p_sub.text = f"  {desc}"
    p_sub.font.name = FONT_ARABIC
    p_sub.font.size = Pt(9.5)
    p_sub.font.color.rgb = COLOR_TEXT_MUTED

set_speaker_notes(s2, "في هذه الشريحة نبيّن للدكتور نقطة الصفر: عندما استلمنا المشروع، وجدنا تطبيقاً يعمل كـ prototype لكنه يفتقر تماماً للركائز الهندسية: لا وثيقة متطلبات SRS، لا خط أساس موثق، لا اختبارات آلية، وكلاسات عملاقة تجاوز أحدها 2400 سطر يجمع الاكتشاف والمكالمات والتراسل في مكان واحد. هذا التشخيص هو الأساس الذي بنينا عليه خطتنا الهندسية الشاملة.")

# ==============================================================================
# SLIDE 3: Requirements Engineering & SRS
# ==============================================================================
s3 = prs.slides.add_slide(blank_slide_layout)
add_header(s3, 3, "هندسة المتطلبات", "توثيق مواصفات المتطلبات البرمجية (SRS Specification)", "منهجية تحليل وتصنيف 31 متطلباً وظيفياً و 15 قصة مستخدم في وثيقة SRS.md", "docs/SRS.md (622 lines)", "green")

# Left Column: The 5-Tier Classification Model
c3_1 = s3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(4.9))
c3_1.fill.solid()
c3_1.fill.fore_color.rgb = COLOR_CARD_BG
c3_1.line.color.rgb = COLOR_PRIMARY_BLUE
c3_1.line.width = Pt(1.5)

tf3_1 = c3_1.text_frame
tf3_1.word_wrap = True
p = tf3_1.paragraphs[0]
p.text = "🎯 نموذج تصنيف المتطلبات الخماسي المبتكر (SRS.md):"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY_BLUE

tiers = [
    ("1. Official Requirement (متطلب رسمي):", "ما طُلب صراحة في التكليف الأكاديمي الرسمي؛ أي قيد لم ينص عليه لا يعد ملزماً.", COLOR_TEXT_WHITE),
    ("2. Existing Capability (إمكانية حالية في الكود):", "وظيفة مبنية بالفعل في الكود ولكنها تحتاج إثباتاً تشغيلياً على أرض الواقع.", COLOR_GREEN_TEXT),
    ("3. Proposed Improvement (تحسين مقترح):", "أفكار لتطوير النظام (مثل اقتراح تبادل المفاتيح) لا تعد جزءاً من خط الأساس الحالي.", COLOR_PRIMARY_BLUE),
    ("4. Missing (مفقود / غير منفذ):", "وظيفة متوقعة لكن فحص الكود أثبت غيابها (مثل حذف الرسائل شبكياً وفحص Hash الملف).", COLOR_RED_TEXT),
    ("5. Not Verified (غير مثبت تشغيلياً):", "كود موجود بالمستودع لم يُختبر بين جهازين حقيقيين في بيئة شبكة واقعية.", COLOR_AMBER_TEXT)
]

for t_name, t_desc, col in tiers:
    p = tf3_1.add_paragraph()
    p.text = t_name
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = col
    p.space_before = Pt(6)
    
    p_d = tf3_1.add_paragraph()
    p_d.text = f"  {t_desc}"
    p_d.font.name = FONT_ARABIC
    p_d.font.size = Pt(9.5)
    p_d.font.color.rgb = COLOR_TEXT_MUTED

# Right Column: Structure of SRS & User Stories
c3_2 = s3.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(4.9))
c3_2.fill.solid()
c3_2.fill.fore_color.rgb = COLOR_CARD_BG
c3_2.line.color.rgb = COLOR_CARD_BORDER
c3_2.line.width = Pt(1.5)

tf3_2 = c3_2.text_frame
tf3_2.word_wrap = True
p = tf3_2.paragraphs[0]
p.text = "📑 محتويات وثيقة SRS المعتمدة (docs/SRS.md):"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_TEXT_WHITE

srs_items = [
    ("نطاق النظام (In Scope vs Out of Scope):", "تحديد دقيق لما يقدمه النظام (P2P محلي) وما يقع خارجه (لا خوادم سحابية، لا STUN/TURN، لا PKI معقدة)."),
    ("الأطراف الفاعلة وسياق النظام (System Context):", "تعريف المستخدم، الأقران، نظام Android، عتاد الكاميرا والميكروفون، وقاعدة بيانات Room."),
    ("31 متطلباً وظيفياً مفصلاً (FR-01 إلى FR-31):", "توصيف دقيق لكل متطلب: المدخلات، المخرجات، المعالجة، والملفات المسؤولة في الكود."),
    ("15 قصة مستخدم قياسية (US-01 إلى US-15):", "صياغة هندسية قياسية: (كـ [مستخدم] أريد [وظيفة] حتى أتمكن من [قيمة مضافة])."),
    ("معايير القبول بصيغة (Given / When / Then):", "صياغة 28 معيار قبول لاختبار كل متطلب والتحقق من تحققه البرمجي.")
]

for it_title, it_desc in srs_items:
    p = tf3_2.add_paragraph()
    p.text = f"• {it_title}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(6)
    
    p_d = tf3_2.add_paragraph()
    p_d.text = f"  {it_desc}"
    p_d.font.name = FONT_ARABIC
    p_d.font.size = Pt(9.5)
    p_d.font.color.rgb = COLOR_TEXT_MUTED

set_speaker_notes(s3, "اشرح للدكتور أن وثيقة الـ SRS المكونة من 622 سطراً لم تكن مجرد نصوص عامة، بل بنيت وفق نموذج تصنيف خماسي صارم ابتكرناه لمنع الوعود غير الواقعية. بيّن له أننا صنفنا 31 متطلباً وظيفياً و 15 قصة مستخدم، ووضعنا حدوداً واضحة لما هو داخل النطاق وما هو خارجه، مما منح المشروع رصانة أكاديمية تحميه من ادعاء ميزات لم تنفذ بعد.")

# ==============================================================================
# SLIDE 4: Baseline Establishment & Version Control
# ==============================================================================
s4 = prs.slides.add_slide(blank_slide_layout)
add_header(s4, 4, "إدارة خط الأساس", "توثيق خط الأساس (Baseline) والتحكم في الإصدارات", "تثبيت النسخة المرجعية Commit 7111182 وضبط بيئة البناء وتكرارية النتائج", "docs/Baseline.md (308 lines)", "green")

# Left Column: Baseline Metadata & Environment
c4_1 = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(4.9))
c4_1.fill.solid()
c4_1.fill.fore_color.rgb = COLOR_CARD_BG
c4_1.line.color.rgb = COLOR_ROYAL_BLUE
c4_1.line.width = Pt(1.5)

tf4_1 = c4_1.text_frame
tf4_1.word_wrap = True
p = tf4_1.paragraphs[0]
p.text = "📌 بيانات خط الأساس المعتمد (Baseline Commit):"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY_BLUE

base_info = [
    ("Commit Hash المرجعي:", "71111825645c2b3c6a926f739455c4ffda3c0963"),
    ("رسالة الـ Commit:", "feat: comprehensive audit fixes (logic, audio mixer, UX, privacy, theme, and PiP call overhaul)"),
    ("حالة شجرة العمل (Working Tree):", "Clean — تم التحقق الكامل عبر git status دون تعديلات معلقة"),
    ("نظام البناء وإصدار Gradle:", "Gradle 9.4.1 (Wrapper) & AGP 9.1.1 & Kotlin 2.2.10"),
    ("مستويات الـ SDK المستهدفة:", "compileSdk = 36, minSdk = 24 (Android 7.0+), targetSdk = 36"),
    ("حزم الجيل وتوليد الكود:", "KSP (Kotlin Symbol Processing) لـ Room DB 6 و Moshi JSON")
]

for b_name, b_val in base_info:
    p = tf4_1.add_paragraph()
    p.text = f"• {b_name}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(5)
    
    p_v = tf4_1.add_paragraph()
    p_v.text = f"  {b_val}"
    p_v.font.name = FONT_MONO if "Commit" in b_name or "Gradle" in b_name or "SDK" in b_name else FONT_ARABIC
    p_v.font.size = Pt(9.5)
    p_v.font.color.rgb = COLOR_PRIMARY_BLUE if "Commit" in b_name else COLOR_TEXT_MUTED

# Right Column: Significance & Boundaries
c4_2 = s4.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(4.9))
c4_2.fill.solid()
c4_2.fill.fore_color.rgb = COLOR_CARD_BG
c4_2.line.color.rgb = COLOR_CARD_BORDER
c4_2.line.width = Pt(1.5)

tf4_2 = c4_2.text_frame
tf4_2.word_wrap = True
p = tf4_2.paragraphs[0]
p.text = "🛡️ الأهمية الهندسية ومحددات خط الأساس:"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_TEXT_WHITE

significance = [
    ("منع انزلاق النطاق (Scope Creep):", "تثبيت نقطة مرجعية واضحة تمنع إضافة ميزات عشوائية دون توثيق وتحدد بدقة ما هو داخل التقييم."),
    ("تكرارية البناء (Build Reproducibility):", "أي مهندس أو أستاذ يمكنه سحب المستودع وبناء APK مطابق بالكامل بفضل تثبيت إصدارات الـ Wrapper والتبعيات."),
    ("الإقرار الصريح بالحدود التشغيلية:", "وثيقة Baseline.md أقرت بشفافية أن التقييم مبني على الفحص المكتبي والـ Unit Tests، ولم يُختبر على جهازين حقيقيين بعد."),
    ("حصر الوظائف في Features Inventory:", "جدول تفصيلي يضم 14 ميزة رئيسية يربط كل ميزة بملفاتها في الكود وحالتها الدقيقة (Implemented - Not Tested).")
]

for s_name, s_val in significance:
    p = tf4_2.add_paragraph()
    p.text = f"• {s_name}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(6)
    
    p_v = tf4_2.add_paragraph()
    p_v.text = f"  {s_val}"
    p_v.font.name = FONT_ARABIC
    p_v.font.size = Pt(9.5)
    p_v.font.color.rgb = COLOR_TEXT_MUTED

set_speaker_notes(s4, "وضّح للأستاذ كيف طبقنا مفهوم خط الأساس البرمجي (Software Baseline). قمنا بتثبيت الـ Commit رقم 7111182 وتوثيقه في ملف Baseline.md، وحددنا بدقة بيئة البناء (Gradle 9.4 و Kotlin 2.2). هذا التثبيت يحقق مبدأ Reproducibility ويمنع حدوث انزلاق في نطاق العمل، مع الإقرار الصريح بحدود التحقق الحالية.")

# ==============================================================================
# SLIDE 5: System Context & P2P Architecture
# ==============================================================================
s5 = prs.slides.add_slide(blank_slide_layout)
add_header(s5, 5, "التصميم المعماري", "السياق المعماري للنظام وهيكلية شبكة P2P اللامركزية", "توزيع المسؤوليات وتفكيك الحزم البرمجية دون الاعتماد على خادم مركزي", "DECENTRALIZED P2P", "blue")

# 4 Architectural Layer Cards
layers = [
    ("📱 طبقة العرض والواجهات (Presentation Layer)",
     "app/src/main/java/com/example/ui/",
     "بناء الواجهات كلياً بنمط Compose التعلاني، مقسمة إلى screens (المحادثة والأقران والغرف)، dialogs (المكالمات ومكالمات الغرف)، و components التفاعلية."),
    
    ("🧠 طبقة التنسيق وإدارة الحالة (ViewModel / Coordination)",
     "app/src/main/java/com/example/ui/MainViewModel.kt",
     "إدارة تدفقات StateFlow للواجهة، استقبال الأحداث، وتوجيه الرسائل والمكالمات بين المحركات الشبكية وقاعدة البيانات المحلية."),
    
    ("🌐 طبقة المحركات الشبكية المستقلة (Network Engine Layer)",
     "app/src/main/java/com/example/network/",
     "عزل قنوات الاتصال: LocalP2PEngine (اكتشاف ورسائل)، AudioEngine (UDP 8889)، VideoEngine (UDP 8890)، FileTransferEngine (TCP 8891)."),
    
    ("💾 طبقة البيانات والتخزين المحلي (Persistence Layer)",
     "app/src/main/java/com/example/data/",
     "إدارة جداول الرسائل والغرف والمستخدم والمحظورين عبر Room Database (AppDatabase.kt)، وتخزين التفضيلات عبر DataStore.")
]

for idx, (l_title, l_path, l_desc) in enumerate(layers):
    col = idx % 2
    row = idx // 2
    lx = Inches(0.8 + col * 5.95)
    ly = Inches(1.9 + row * 2.45)
    
    l_card = s5.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, lx, ly, Inches(5.75), Inches(2.3))
    l_card.fill.solid()
    l_card.fill.fore_color.rgb = COLOR_CARD_BG
    l_card.line.color.rgb = COLOR_CARD_BORDER
    l_card.line.width = Pt(1.5)
    
    tf = l_card.text_frame
    tf.word_wrap = True
    p1 = tf.paragraphs[0]
    p1.text = l_title
    p1.font.name = FONT_ARABIC
    p1.font.size = Pt(11.5)
    p1.font.bold = True
    p1.font.color.rgb = COLOR_PRIMARY_BLUE
    
    p2 = tf.add_paragraph()
    p2.text = l_path
    p2.font.name = FONT_MONO
    p2.font.size = Pt(9.5)
    p2.font.bold = True
    p2.font.color.rgb = COLOR_ROYAL_BLUE
    p2.space_before = Pt(3)
    
    p3 = tf.add_paragraph()
    p3.text = l_desc
    p3.font.name = FONT_ARABIC
    p3.font.size = Pt(9.5)
    p3.font.color.rgb = COLOR_TEXT_WHITE
    p3.space_before = Pt(4)

set_speaker_notes(s5, "اشرح للأستاذ كيف قسّمنا بنية المشروع معمارياً: كل جهاز في شبكة LOCAL CONTACT يعمل كـ Peer مكافئ (Client و Listener معاً). تم توزيع النظام على أربع طبقات واضحة: Presentation بنمط Jetpack Compose، و Coordination عبر ViewModel، و Network Engines معزولة لكل نوع بيانات، و Persistence محلي عبر Room و DataStore.")

# ==============================================================================
# SLIDE 6: Architectural Smells & God Classes
# ==============================================================================
s6 = prs.slides.add_slide(blank_slide_layout)
add_header(s6, 6, "الروائح المعمارية وإعادة الهيكلة", "تحليل الكلاسات المتضخمة ومبادئ SOLID الهندسية", "رصد انتهاكات SRP والاقتران الشديد ووضع خطط إعادة الهيكلة المستمرة", "ARCHITECTURAL SMELLS", "amber")

# Card 1: LocalP2PEngine Analysis
c6_1 = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(4.9))
c6_1.fill.solid()
c6_1.fill.fore_color.rgb = COLOR_CARD_BG
c6_1.line.color.rgb = COLOR_AMBER_TEXT
c6_1.line.width = Pt(1.5)

tf6_1 = c6_1.text_frame
tf6_1.word_wrap = True
p = tf6_1.paragraphs[0]
p.text = "⚠️ الكلاس المتضخم الأول (God Class):"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_AMBER_TEXT

p_path = tf6_1.add_paragraph()
p_path.text = "app/.../network/LocalP2PEngine.kt (2,496 lines)"
p_path.font.name = FONT_MONO
p_path.font.size = Pt(10)
p_path.font.bold = True
p_path.font.color.rgb = COLOR_PRIMARY_BLUE

smells_p2p = [
    ("انتهاك مبدأ المسؤولية الواحدة (SRP):", "الكلاس يتولى مهام متعددة في آن واحد: بث واكتشاف الأقران، إدارة مقابس UDP، معالجة إشارات المكالمات، وإدارة الـ Typing و Acknowledgment."),
    ("صعوبة الاختبار المعزول (Low Testability):", "تداخل منطق الشبكة مع إدارة الذاكرة الحية (ConcurrentHashMap) يجعل كتابة Unit Tests نقية أمراً معقداً دون محاكاة المقابس."),
    ("المسار الهندسي للإصلاح (Refactoring Plan):", "تفكيك الكلاس مستقبلاً إلى 3 مكونات مستقلة: DiscoveryCoordinator, CallSignalingHandler, PacketDispatcher.")
]

for s_t, s_d in smells_p2p:
    p = tf6_1.add_paragraph()
    p.text = f"• {s_t}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(6)
    
    p_sub = tf6_1.add_paragraph()
    p_sub.text = f"  {s_d}"
    p_sub.font.name = FONT_ARABIC
    p_sub.font.size = Pt(9.5)
    p_sub.font.color.rgb = COLOR_TEXT_MUTED

# Card 2: MainViewModel & DIP Application
c6_2 = s6.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(4.9))
c6_2.fill.solid()
c6_2.fill.fore_color.rgb = COLOR_CARD_BG
c6_2.line.color.rgb = COLOR_CARD_BORDER
c6_2.line.width = Pt(1.5)

tf6_2 = c6_2.text_frame
tf6_2.word_wrap = True
p = tf6_2.paragraphs[0]
p.text = "🧩 الكلاس الثاني وتطبيق مبدأ DIP المعماري:"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_TEXT_WHITE

p_path2 = tf6_2.add_paragraph()
p_path2.text = "app/.../ui/MainViewModel.kt (1,839 lines)"
p_path2.font.name = FONT_MONO
p_path2.font.size = Pt(10)
p_path2.font.bold = True
p_path2.font.color.rgb = COLOR_PRIMARY_BLUE

smells_vm = [
    ("تضخم الـ ViewModel (Fat ViewModel):", "تجميع منطق التنقل بين الشاشات، الاستعلامات المباشرة من Room Database، والتفاعل المباشر مع محركات الشبكة في كلاس واحد."),
    ("تطبيق مبدأ انعكاس الاعتمادية (DIP):", "في طبقة التشفير، تم كسر الاقتران الصلب عبر تجريد KeyProvider.kt، بحيث لم يعد محرك التشفير يعتمد على كلاس ملموس بل على واجهة."),
    ("الدرس الهندسي المستفاد:", "الاعتراف بوجود الروائح المعمارية والديون التقنية (Technical Debt) في وثائق المشروع هو دليل نضج هندسي، حيث تم توثيقها كخطة تطوير رسمية.")
]

for s_t, s_d in smells_vm:
    p = tf6_2.add_paragraph()
    p.text = f"• {s_t}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(6)
    
    p_sub = tf6_2.add_paragraph()
    p_sub.text = f"  {s_d}"
    p_sub.font.name = FONT_ARABIC
    p_sub.font.size = Pt(9.5)
    p_sub.font.color.rgb = COLOR_TEXT_MUTED

set_speaker_notes(s6, "ركز هنا أمام أستاذ هندسة البرمجيات: نحن لا ندّعي كمال الكود، بل طبقنا التحليل المعماري للكشف عن الروائح البرمجية (Code Smells). رصدنا تضخم كلاس LocalP2PEngine وكلاس MainViewModel، ووضحنا انتهاكها لمبدأ SRP. وبيّنا كيف بدأنا حل المشكلة بتطبيق مبدأ Dependency Inversion في التشفير عبر KeyProvider تمهيداً لتفكيك بقية الأجزاء.")

# ==============================================================================
# SLIDE 7: Requirements Traceability Matrix - RTM
# ==============================================================================
s7 = prs.slides.add_slide(blank_slide_layout)
add_header(s7, 7, "إدارة التتبع والتحقق", "مصفوفة تتبع المتطلبات (Traceability Matrix - RTM)", "الربط الثلاثي المحكم: من المتطلبات إلى الكود المصدري، قصص المستخدمين، ومعايير القبول", "docs/TraceabilityMatrix.md", "green")

# Table of Traceability Samples
t_shape = s7.shapes.add_table(6, 5, Inches(0.8), Inches(1.9), Inches(11.7), Inches(3.7))
tbl = t_shape.table
tbl.columns[0].width = Inches(1.2)  # Req ID
tbl.columns[1].width = Inches(2.2)  # Title
tbl.columns[2].width = Inches(3.6)  # Code File & Evidence
tbl.columns[3].width = Inches(2.2)  # User Story & AC
tbl.columns[4].width = Inches(2.5)  # Exact Status

headers = ["رمز المتطلب", "عنوان المتطلب الوظيفي", "الدليل من الكود المصدري الفعلي", "قصة المستخدم ومعيار القبول", "الحالة الهندسية الدقيقة"]
for cidx, h in enumerate(headers):
    cell = tbl.cell(0, cidx)
    cell.fill.solid()
    cell.fill.fore_color.rgb = COLOR_CARD_BG
    p = cell.text_frame.paragraphs[0]
    p.text = h
    p.alignment = PP_ALIGN.CENTER
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY_BLUE

rtm_samples = [
    ("FR-01", "اكتشاف الأجهزة المحلية", "LocalP2PEngine.kt\nstartDiscoveryListener() & broadcastPresence()", "US-01 / AC-01\nظهور القرين خلال 3 ثوانٍ", "Implemented - Not Tested\n(يحتاج اختبار جهازين)"),
    ("FR-05", "إرسال رسالة فردية", "LocalP2PEngine.kt & MainViewModel.kt\nsendMessage(), حزمة CHAT_MSG", "US-02 / AC-02\nتسليم وتخزين وتأكيد ACK", "Implemented - Not Tested\n(مبني في الكود والقاعدة)"),
    ("FR-13", "حذف الرسالة شبكياً", "LocalP2PEngine.kt\n(لا يوجد كود أو حزمة شبكية للحذف عن بعد)", "US-06 / AC-02.6\nحذف الرسالة لدى القرين", "Missing / Not Implemented\n(موثق كفجوة بأمانة)"),
    ("FR-17", "استئناف نقل الملفات", "FileTransferEngine.kt & LocalCryptoEngine.kt\nderiveChunkNonce() واشتقاق الإزاحة", "US-08 / AC-03.1\nاستئناف التحميل بعد القطع", "Implemented - Not Tested\n(يحتاج قياس عملي)"),
    ("FR-28", "تشفير الحزم AES-GCM", "LocalCryptoEngine.kt\nencrypt() بنمط AES/GCM/NoPadding و Tag 128", "US-15 / AC-05\nحماية السرية وسلامة الحزم", "JVM Tested (10/10 Passed)\n(مثبت آلياً بوحدة الاختبار)")
]

for ridx, row in enumerate(rtm_samples):
    for cidx, val in enumerate(row):
        cell = tbl.cell(ridx + 1, cidx)
        cell.fill.solid()
        cell.fill.fore_color.rgb = COLOR_CARD_BG if ridx % 2 == 0 else RGBColor(14, 22, 45)
        p = cell.text_frame.paragraphs[0]
        p.text = val
        p.font.name = FONT_MONO if cidx == 0 or "Engine" in val else FONT_ARABIC
        p.font.size = Pt(9)
        p.font.bold = True if cidx in [0, 4] else False
        
        # Color coding status
        if cidx == 4:
            if "Tested" in val and "Not" not in val:
                p.font.color.rgb = COLOR_GREEN_TEXT
            elif "Missing" in val:
                p.font.color.rgb = COLOR_RED_TEXT
            else:
                p.font.color.rgb = COLOR_AMBER_TEXT
        else:
            p.font.color.rgb = COLOR_TEXT_WHITE

# Bottom Summary Card
rtm_sum = s7.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(5.8), Inches(11.7), Inches(1.0))
rtm_sum.fill.solid()
rtm_sum.fill.fore_color.rgb = COLOR_CARD_BG
rtm_sum.line.color.rgb = COLOR_ROYAL_BLUE
rtm_sum.line.width = Pt(1.5)

tf_r = rtm_sum.text_frame
tf_r.word_wrap = True
p1 = tf_r.paragraphs[0]
p1.text = "🎯 القيمة الهندسية لمصفوفة التتبع (RTM):"
p1.font.name = FONT_ARABIC
p1.font.size = Pt(11)
p1.font.bold = True
p1.font.color.rgb = COLOR_PRIMARY_BLUE

p2 = tf_r.add_paragraph()
p2.text = "تثبت المصفوفة عدم وجود أي 'كود طفيلي' لا يرتبط بمتطلب وظيفي، وتمنع الادعاءات غير الدقيقة بفصلها الصارم بين الميزات المختبرة بوحدات الاختبار (JVM Tested)، والميزات المبنية في الكود ولكنها تحتاج أجهزة فعلية (Implemented - Not Tested)، والميزات غير المكتملة الموثقة رسمياً كفجوات (Missing)."
p2.font.name = FONT_ARABIC
p2.font.size = Pt(9.5)
p2.font.color.rgb = COLOR_TEXT_MUTED

set_speaker_notes(s7, "هذه الشريحة تمثل جوهر هندسة البرمجيات: استعرض للدكتور مصفوفة تتبع المتطلبات RTM في ملف TraceabilityMatrix.md المكون من 324 سطراً. وضّح له كيف ربطنا 31 متطلباً وظيفياً بالكود المصدري وبالاختبارات، وأكد على الأمانة العلمية: FR-13 (الحذف الشبكي) وثقناها صراحة كـ Missing لأن الكود لا يدعمها، بينما FR-28 أثبتناها بـ 10 اختبارات JVM.")

# ==============================================================================
# SLIDE 8: Change Management & Backward Compatibility
# ==============================================================================
s8 = prs.slides.add_slide(blank_slide_layout)
add_header(s8, 8, "إدارة التغيير المعماري", "إدارة التغيير والتوافق العكسي (Change Management)", "تطبيق نمط المزود (Provider Pattern) لإعادة هيكلة التشفير دون كسر الكود القديم", "SOLID / DIP PATTERN", "green")

# Left Column: The Problem & The Solution
c8_1 = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(4.9))
c8_1.fill.solid()
c8_1.fill.fore_color.rgb = COLOR_CARD_BG
c8_1.line.color.rgb = COLOR_ROYAL_BLUE
c8_1.line.width = Pt(1.5)

tf8_1 = c8_1.text_frame
tf8_1.word_wrap = True
p = tf8_1.paragraphs[0]
p.text = "🔄 تحدي إدارة التغيير وتطبيق نمط Provider:"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY_BLUE

ch_points = [
    ("تحدي التغيير (The Change Challenge):", "أردنا ترقية منظومة التشفير لدعم المفاتيح الديناميكية و ECDH دون كسر عشرات المسارات التي تعتمد على التشفير القديم في الواجهات ومحركات الصوت والفيديو."),
    ("نمط التصميم المطبق (Design Pattern):", "تم تطبيق نمط Provider / Strategy عبر إنشاء واجهة KeyProvider.kt لفصل اشتقاق المفاتيح عن عمليات التشفير والـ Cipher."),
    ("التوافق العكسي (Backward Compatibility):", "بناء كلاس LegacyStaticKeyProvider ليحتفظ بسلوك PBKDF2 القديم بنفس الملح والـ Iterations، مما جعل الكود القديم يعمل بسلاسة كـ Default."),
    ("صفر تعديلات كاسرة (Zero Breaking Changes):", "لم تتأثر استدعاءات encrypt() و decrypt() في باقي أجزاء التطبيق، مما حقق تغييراً آمناً وخالياً من الانتكاسات (No Regressions).")
]

for ct, cd in ch_points:
    p = tf8_1.add_paragraph()
    p.text = f"• {ct}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(6)
    
    p_sub = tf8_1.add_paragraph()
    p_sub.text = f"  {cd}"
    p_sub.font.name = FONT_ARABIC
    p_sub.font.size = Pt(9.5)
    p_sub.font.color.rgb = COLOR_TEXT_MUTED

# Right Column: Modified & Created Components with Line Numbers
c8_2 = s8.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(4.9))
c8_2.fill.solid()
c8_2.fill.fore_color.rgb = COLOR_CARD_BG
c8_2.line.color.rgb = COLOR_CARD_BORDER
c8_2.line.width = Pt(1.5)

tf8_2 = c8_2.text_frame
tf8_2.word_wrap = True
p = tf8_2.paragraphs[0]
p.text = "📂 الملفات الفعلية والمسؤوليات البرمجية:"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_TEXT_WHITE

files_ch = [
    ("الملف المضاف:", "app/src/main/java/com/example/network/KeyProvider.kt",
     "الأسطر 17-26: تعريف الواجهة المجردة KeyProvider.\nالأسطر 38-94: تطبيق LegacyStaticKeyProvider مع PBKDF2 والـ 12,000 تكرار للتوافق العكسي."),
    
    ("الملف المعدل معمارياً:", "app/src/main/java/com/example/network/LocalCryptoEngine.kt",
     "الأسطر 28-30: استقبال المزود عبر @Volatile var keyProvider: KeyProvider.\nالأسطر 43-45: استرجاع المفتاح عبر keyProvider.getNetworkKey() بدلاً من تثبيته داخلياً."),
    
    ("ملفات الاختبار المضافة:", "app/src/test/java/com/example/network/LocalCryptoEngineTest.kt",
     "10 اختبارات وحدة آلية تفحص المزود الجديد والقديم، وتثبت سلامة التشفير وعدم انكسار التوافق العكسي.")
]

for ft, fp, fd in files_ch:
    p = tf8_2.add_paragraph()
    p.text = f"• {ft}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(10.5)
    p.font.bold = True
    p.font.color.rgb = COLOR_PRIMARY_BLUE
    p.space_before = Pt(6)
    
    p_p = tf8_2.add_paragraph()
    p_p.text = f"  {fp}"
    p_p.font.name = FONT_MONO
    p_p.font.size = Pt(9)
    p_p.font.color.rgb = COLOR_TEXT_WHITE
    
    p_d = tf8_2.add_paragraph()
    p_d.text = f"  {fd}"
    p_d.font.name = FONT_ARABIC
    p_d.font.size = Pt(9)
    p_d.font.color.rgb = COLOR_TEXT_MUTED

set_speaker_notes(s8, "اشرح للأستاذ كيف تعاملنا مع إدارة التغيير: عند ترقية التشفير، طبقنا نمط Provider وعزلنا إدارة المفتاح في KeyProvider.kt. أنشأنا LegacyStaticKeyProvider للحفاظ على التوافق مع الكود القديم. النتيجة الهندسية: حدث التغيير المعماري دون كسر سطر واحد في كود المحادثات أو مكالمات الصوت أو الفيديو القديمة.")

# ==============================================================================
# SLIDE 9: Verification, Testing & QA Strategy
# ==============================================================================
s9 = prs.slides.add_slide(blank_slide_layout)
add_header(s9, 9, "ضمان الجودة والتحقق", "استراتيجية الاختبارات وضمان الجودة (QA & Testing)", "إثبات سلوك المحركات الحرجة عبر 33 اختبار وحدة JVM آلي ناجح بنسبة 100%", "33/33 TESTS PASSED", "green")

# 4 Test Suites Cards
suites = [
    ("🧪 LocalCryptoEngineTest.kt",
     "10 اختبارات آلية ناجحة (100%)",
     "فحص تشفير وفك تشفير البيانات بـ AES-GCM، رفض المفاتيح الخاطئة، رفض التلاعب بالنص المشفر، واشتقاق Nonce فريد لكل كتلة ملف."),
    
    ("🧪 EcdhEngineTest.kt",
     "8 اختبارات آلية ناجحة (100%)",
     "فحص توليد مفاتيح NIST P-256، تبادل المفاتيح المؤقتة، تطابق السر المشترك للطرفين، وفحص استيراد وتصدير المفاتيح بصيغة X.509 DER."),
    
    ("🧪 KeyDerivationTest.kt",
     "8 اختبارات آلية ناجحة (100%)",
     "فحص دالة اشتقاق المفاتيح HKDF-SHA256 وفق RFC 5869، وفحص تطابق نواقل الاختبار القياسية (RFC Test Vectors) ومرحلتي Extract و Expand."),
    
    ("🧪 CryptoEngineTest.kt",
     "7 اختبارات آلية ناجحة (100%)",
     "فحص تكامل بروتوكول الجلسات المشفرة، حماية إرسال واستقبال الحزم المتسلسلة، ورفض الحزم ذات البصمات غير المطابقة.")
]

for idx, (s_title, s_stat, s_desc) in enumerate(suites):
    col = idx % 2
    row = idx // 2
    sx = Inches(0.8 + col * 5.95)
    sy = Inches(1.9 + row * 2.1)
    
    s_card = s9.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, sx, sy, Inches(5.75), Inches(1.95))
    s_card.fill.solid()
    s_card.fill.fore_color.rgb = COLOR_CARD_BG
    s_card.line.color.rgb = COLOR_CARD_BORDER
    s_card.line.width = Pt(1.5)
    
    tf = s_card.text_frame
    tf.word_wrap = True
    p1 = tf.paragraphs[0]
    p1.text = s_title
    p1.font.name = FONT_MONO
    p1.font.size = Pt(11)
    p1.font.bold = True
    p1.font.color.rgb = COLOR_PRIMARY_BLUE
    
    p2 = tf.add_paragraph()
    p2.text = f"• النتيجة: {s_stat}"
    p2.font.name = FONT_ARABIC
    p2.font.size = Pt(10)
    p2.font.bold = True
    p2.font.color.rgb = COLOR_GREEN_TEXT
    p2.space_before = Pt(2)
    
    p3 = tf.add_paragraph()
    p3.text = s_desc
    p3.font.name = FONT_ARABIC
    p3.font.size = Pt(9)
    p3.font.color.rgb = COLOR_TEXT_MUTED
    p3.space_before = Pt(3)

# Bottom Academic Boundary Note
b_note = s9.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(6.15), Inches(11.7), Inches(0.75))
b_note.fill.solid()
b_note.fill.fore_color.rgb = COLOR_AMBER_BG
b_note.line.color.rgb = COLOR_AMBER_TEXT
b_note.line.width = Pt(1.5)

tf_bn = b_note.text_frame
tf_bn.word_wrap = True
p = tf_bn.paragraphs[0]
p.text = "⚠️ الضوابط العلمية لنتائج الاختبارات: الاختبارات المذكورة أثبتت صحة العمليات الحسابية والمنطقية على مستوى JVM، لكنها لا تغني عن الاختبارات التكاملية على أجهزة أندرويد حقيقية (Two Physical Devices) لقياس تأثير ضياع الحزم وتقييد النظام للبث المتعدد."
p.font.name = FONT_ARABIC
p.font.size = Pt(9.5)
p.font.bold = True
p.font.color.rgb = COLOR_TEXT_WHITE

set_speaker_notes(s9, "أكد للدكتور على منهجية التحقق: بنينا 33 اختبار وحدة آلي على مستوى الـ JVM تعمل تلقائياً عبر Gradle Test وتنجح بنسبة 100%. غطت الاختبارات التشفير، تبادل المفاتيح، والـ HKDF. واختم بتوضيح الفرق العلمي: هذه الاختبارات أثبتت المنطق البرمجي، لكن إثبات الأداء الشبكي التام يتطلب فحصاً على أجهزة حقيقية.")

# ==============================================================================
# SLIDE 10: Engineering Toolchain & Build System
# ==============================================================================
s10 = prs.slides.add_slide(blank_slide_layout)
add_header(s10, 10, "أدوات التطوير والبناء", "منظومة أدوات البناء وتوليد الكود (Engineering Toolchain)", "تكامل Gradle 9.4، لغة Kotlin 2.2، ومعالجة الرموز KSP لإدارة دورة حياة المشروع", "TOOLCHAIN & CI", "blue")

tools = [
    ("⚙️ محرك البناء والـ Wrapper", "Gradle 9.4.1 (Kotlin DSL)",
     "إدارة دورة حياة المشروع، تنزيل التبعيات آلياً، وعزل بيئة البناء لضمان التكرارية عبر gradle-wrapper.properties."),
    
    ("🤖 مجمع أندرويد الرسمي", "Android Gradle Plugin 9.1.1",
     "دعم أحدث واجهات أندرويد 16 (Vanilla Ice Cream)، compileSdk 36، و task مخصصة لتصحيح الأرقام الهندية-العربية."),
    
    ("💎 لغة البرمجة ومنظومة Coroutines", "Kotlin 2.2.10 & StateFlow",
     "برمجة غير تزامنية عالية الكفاءة للمقابس والشبكة دون حظر واجهة المستخدم عبر Coroutines و Dispatchers.IO."),
    
    ("⚡ معالجة الرموز البرمجية", "KSP (Kotlin Symbol Processing)",
     "توليد كود قاعدة البيانات Room Database 6 وكود محولات Moshi JSON بسرعة فائقة أثناء وقت الترجمة (Compile-time)."),
    
    ("🎨 إطار الواجهات التعلاني", "Jetpack Compose BOM 2024.09",
     "بناء واجهات حديثة وقابلة لإعادة الاستخدام ومبنية على أحداث تدفق الحالة (Single Source of Truth)."),
    
    ("📦 نظام التحكم بالإصدارات", "Git & GitHub Remote Repo",
     "إدارة الفروع، تتبع الـ Commits، والربط مع المستودع السحابي: github.com/mshalhajep/final-project.")
]

for idx, (t_name, t_sub, t_desc) in enumerate(tools):
    col = idx % 3
    row = idx // 3
    tx = Inches(0.8 + col * 3.95)
    ty = Inches(1.9 + row * 2.5)
    
    t_card = s10.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, tx, ty, Inches(3.8), Inches(2.3))
    t_card.fill.solid()
    t_card.fill.fore_color.rgb = COLOR_CARD_BG
    t_card.line.color.rgb = COLOR_CARD_BORDER
    t_card.line.width = Pt(1.5)
    
    tf = t_card.text_frame
    tf.word_wrap = True
    p1 = tf.paragraphs[0]
    p1.text = t_name
    p1.font.name = FONT_ARABIC
    p1.font.size = Pt(11)
    p1.font.bold = True
    p1.font.color.rgb = COLOR_PRIMARY_BLUE
    
    p2 = tf.add_paragraph()
    p2.text = t_sub
    p2.font.name = FONT_MONO
    p2.font.size = Pt(9.5)
    p2.font.bold = True
    p2.font.color.rgb = COLOR_ROYAL_BLUE
    p2.space_before = Pt(2)
    
    p3 = tf.add_paragraph()
    p3.text = t_desc
    p3.font.name = FONT_ARABIC
    p3.font.size = Pt(9)
    p3.font.color.rgb = COLOR_TEXT_MUTED
    p3.space_before = Pt(3)

set_speaker_notes(s10, "استعرض للأستاذ منظومة الأدوات الهندسية: نستخدم أحدث ممارسات البناء في أندرويد: Gradle 9.4 و Kotlin 2.2 و KSP لمعالجة الأكواد البرمجية بدلاً من kapt البطيء. كل هذه الأدوات مضبوطة في build.gradle.kts وتضمن استقرار البناء، والتكامل الكامل مع Git لتتبع الإصدارات.")

# ==============================================================================
# SLIDE 11: Risk Management & Documented Gaps
# ==============================================================================
s11 = prs.slides.add_slide(blank_slide_layout)
add_header(s11, 11, "إدارة المخاطر والنزاهة العلمية", "إدارة المخاطر والفجوات الموثقة (Risk Management & Gaps)", "التوثيق الصريح للقيود والميزات غير المكتملة كأعلى معايير النضج الهندسي", "HONEST ACADEMIC BOUNDARIES", "amber")

# 3 Risk / Gap Cards
risks = [
    ("🚫 ميزات موثقة كـ Missing في المستودع الحالي",
     "• FR-13: حذف الرسائل شبكياً من أجهزة الأقران — منفذ محلياً فقط في قاعدة بيانات الجهاز، ولا توجد حزمة شبكية لإزالته عن بعد.\n• FR-19: فحص سلامة الملف كاملاً — نقل الملفات مجزأ ويعمل، ولكن لا توجد آلية لمقارنة SHA-256 للملف كاملاً بعد اكتمال الاستلام.\n• Group Keys: المحادثات والمكالمات الجماعية للغرف لا تزال تستخدم مفتاح الشبكة الموحد للتوافق دون مفاتيح غرف مشتقة.",
     COLOR_RED_TEXT, COLOR_RED_BG),
    
    ("⚠️ مخاطر بيئة الشبكات الحقيقية (Real-world Risks)",
     "• عزل العملاء في الراوتر (AP Isolation): ميزة في بعض أجهزة التوجيه تمنع الأجهزة من رؤية بعضها البعض محلياً.\n• قيود أندرويد على Multicast / Broadcast: تتطلب أجهزة أندرويد الحديثة حيازة MulticastLock بشكل صريح وإلا قد تُسقط الحزم.\n• ضياع حزم الـ UDP: بروتوكول UDP غير موثوق بطبيعته، مما قد يؤثر على ثبات المكالمات الصوتية والمرئية في بيئات الشبكة المزدحمة.",
     COLOR_AMBER_TEXT, COLOR_AMBER_BG),
    
    ("🛡️ استراتيجيات التخفيف المقترحة (Mitigation Strategy)",
     "• توثيق الفجوات رسمياً في وثيقتي SRS و RTM كـ Proposed Improvements لضمان الشفافية الأكاديمية التامة.\n• إضافة مسح الشبكة الفرعية (Subnet Sweep) كبديل في حال فشل حزم الـ Multicast على المنفذ 8888.\n• وضع التحقق اليدوي (SAS - Short Authentication String) ضمن خارطة الطريق لمنع هجمات الرجل في المنتصف (MITM).",
     COLOR_GREEN_TEXT, COLOR_GREEN_BG)
]

for idx, (r_title, r_desc, t_col, b_col) in enumerate(risks):
    rx = Inches(0.8 + idx * 3.95)
    ry = Inches(1.9)
    
    r_card = s11.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, rx, ry, Inches(3.8), Inches(4.9))
    r_card.fill.solid()
    r_card.fill.fore_color.rgb = COLOR_CARD_BG
    r_card.line.color.rgb = t_col
    r_card.line.width = Pt(1.5)
    
    tf = r_card.text_frame
    tf.word_wrap = True
    p1 = tf.paragraphs[0]
    p1.text = r_title
    p1.font.name = FONT_ARABIC
    p1.font.size = Pt(12)
    p1.font.bold = True
    p1.font.color.rgb = t_col
    
    p2 = tf.add_paragraph()
    p2.text = r_desc
    p2.font.name = FONT_ARABIC
    p2.font.size = Pt(9.5)
    p2.font.color.rgb = COLOR_TEXT_WHITE
    p2.space_before = Pt(8)

set_speaker_notes(s11, "هذه الشريحة تكسبك ثقة لجنة المناقشة بالكامل: اعترف بالقيود بشجاعة علمية. وضّح للدكتور أن هندسة البرمجيات الحقيقية لا تخفي العيوب، بل توثقها كديون تقنية ومخاطر. اعترف بأن الحذف شبكياً مفقود، وأن فحص SHA-256 للملف الكامل مقترح مستقبلي، وأن شبكات الـ Wi-Fi التي تفعل AP Isolation تمثل عائقاً شبكياً وثقناه ووضعنا خطة للتعامل معه.")

# ==============================================================================
# SLIDE 12: Executive Summary & Future Roadmap
# ==============================================================================
s12 = prs.slides.add_slide(blank_slide_layout)
add_header(s12, 12, "الخاتمة والتوصيات", "الخلاصة الهندسية وخارطة الطريق المستقبلية", "ملخص ما تم إنجازه هندسياً وما يقع خارج النطاق وتوصيات التطوير المستمر", "EXECUTIVE SUMMARY", "green")

# Left Column: What Was Accomplished
c12_1 = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(1.9), Inches(5.7), Inches(3.7))
c12_1.fill.solid()
c12_1.fill.fore_color.rgb = COLOR_CARD_BG
c12_1.line.color.rgb = COLOR_GREEN_TEXT
c12_1.line.width = Pt(1.5)

tf12_1 = c12_1.text_frame
tf12_1.word_wrap = True
p = tf12_1.paragraphs[0]
p.text = "✅ ما تم إنجازه وتوثيقه هندسياً في المشروع:"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_GREEN_TEXT

acc_points = [
    ("هندسة المتطلبات المنهجية:", "صياغة وثيقة SRS.md تضم 31 متطلباً و 15 قصة مستخدم و 28 معيار قبول بنموذج تصنيف خماسي."),
    ("تثبيت خط الأساس البرمجي (Baseline):", "تثبيت Commit 7111182 وتوثيق بيئة البناء لضمان التكرارية ومكافحة انزلاق النطاق."),
    ("مصفوفة تتبع ثلاثية الأبعاد (RTM):", "ربط المتطلبات بالكود وبالاختبارات بدقة في TraceabilityMatrix.md لمنع الكود الزائد."),
    ("إدارة التغيير وتجريد المفاتيح:", "تطبيق نمط Provider لتحقيق التوافق العكسي وتحديث منظومة التشفير دون كسر الكود."),
    ("إثبات السلوك بـ 33 اختبار وحدة آلي:", "تحقيق نسبة نجاح 100% في اختبارات JVM للمحركات الحرجة.")
]

for at, ad in acc_points:
    p = tf12_1.add_paragraph()
    p.text = f"• {at} {ad}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(9.5)
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(4)

# Right Column: Future Roadmap
c12_2 = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(6.8), Inches(1.9), Inches(5.7), Inches(3.7))
c12_2.fill.solid()
c12_2.fill.fore_color.rgb = COLOR_CARD_BG
c12_2.line.color.rgb = COLOR_ROYAL_BLUE
c12_2.line.width = Pt(1.5)

tf12_2 = c12_2.text_frame
tf12_2.word_wrap = True
p = tf12_2.paragraphs[0]
p.text = "🚀 خارطة الطريق الهندسية المستقبلية (Roadmap):"
p.font.name = FONT_ARABIC
p.font.size = Pt(13)
p.font.bold = True
p.font.color.rgb = COLOR_PRIMARY_BLUE

road_points = [
    ("تفكيك الكلاسات الضخمة (God Classes Refactoring):", "تقسيم LocalP2PEngine.kt و MainViewModel.kt إلى خدمات فرعية متخصصة ومستقلة تماماً."),
    ("بناء اختبارات تكاملية على أجهزة حقيقية:", "إعداد Instrumented Tests باستخدام AndroidX Test و Espresso لفحص الاكتشاف والمكالمات في بيئة شبكة فعلية."),
    ("استكمال الميزات الموثقة كفجوات:", "برمجة حزمة الحذف الشبكي للرسائل (FR-13) وتضمين فحص SHA-256 للملفات المنقولة (FR-19)."),
    ("تعزيز الحماية ضد هجمات MITM:", "إضافة التحقق اليدوي برمز الأمان القصير (SAS) في أول اتصال بين الأجهزة.")
]

for rt, rd in road_points:
    p = tf12_2.add_paragraph()
    p.text = f"• {rt} {rd}"
    p.font.name = FONT_ARABIC
    p.font.size = Pt(9.5)
    p.font.color.rgb = COLOR_TEXT_WHITE
    p.space_before = Pt(4)

# Final Conclusion Banner
final_ban = s12.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.8), Inches(5.75), Inches(11.7), Inches(1.15))
final_ban.fill.solid()
final_ban.fill.fore_color.rgb = COLOR_CARD_BG
final_ban.line.color.rgb = COLOR_ROYAL_BLUE
final_ban.line.width = Pt(1.5)

tf_fb = final_ban.text_frame
tf_fb.word_wrap = True
p1 = tf_fb.paragraphs[0]
p1.text = "🎓 الخلاصة الهندسية والتوصية الختامية:"
p1.font.name = FONT_ARABIC
p1.font.size = Pt(11.5)
p1.font.bold = True
p1.font.color.rgb = COLOR_PRIMARY_BLUE

p2 = tf_fb.add_paragraph()
p2.text = "نجح مشروع LOCAL CONTACT في التحول من تطبيق تجريبي يعتمد على كود أولي غير موثق، إلى منظومة برمجية خاضعة لأدق مبادئ هندسة البرمجيات (وثائق SRS، خط أساس Baseline، مصفوفة تتبع RTM، إدارة تغيير آمنة، واختبارات آلية ناجحة)، مع خارطة طريق هندسية واضحة وموثقة تضمن استدامة وتطوير النظام مستقبلاً."
p2.font.name = FONT_ARABIC
p2.font.size = Pt(9.5)
p2.font.color.rgb = COLOR_TEXT_WHITE
p2.space_before = Pt(3)

set_speaker_notes(s12, "اختم العرض بقوة وثقة أمام أستاذ هندسة البرمجيات: لخص الإنجاز في نقطتين: 1- فرضنا الانضباط الهندسي الكامل عبر SRS و Baseline و RTM و 33 اختبار وحدة آلي. 2- وضعنا خارطة طريق واضحة للمستقبل لمعالجة الروائح المعمارية واختبار الأجهزة الحقيقية. أكد للدكتور أن هندسة البرمجيات هي التي نقلت المشروع من مجرد 'كود هواة' إلى 'نظام هندسي رصين قابل للتطوير الأكاديمي والمهني'. نشكركم على حسن الاستماع ومستعدون لمناقشة أسئلتكم.")

# Save presentation
out_pptx_en = os.path.abspath("LOCAL_CONTACT_Software_Engineering_Presentation.pptx")
prs.save(out_pptx_en)
out_pptx_ar = os.path.join(os.path.dirname(out_pptx_en), "هندسة_البرمجيات_مشروع_LOCAL_CONTACT.pptx")
import shutil
shutil.copyfile(out_pptx_en, out_pptx_ar)

print("Software Engineering Presentation saved successfully:")
print(f"1: {out_pptx_en} ({os.path.getsize(out_pptx_en):,} bytes)")
print(f"2: {out_pptx_ar} ({os.path.getsize(out_pptx_ar):,} bytes)")
