with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

# Onboarding screen translations
onboarding_ar = """        OnboardingPageData(
            title = "بوابة إدخال البيانات 📋",
            description = "واجهة رئيسية منظمة تتيح لك الوصول السريع لتسجيل الأطفال، تحديث بياناتهم، أو إدارة اللقاحات والأرصدة بكفاءة.",
            icon = Icons.Default.Home,
            color = Color(0xFFE8F5E9)
        ),
        OnboardingPageData(
            title = "تسجيل طفل جديد 👶",
            description = "إضافة الأطفال دون سن الخامسة وتوليد جدول اللقاحات الوطني لهم بشكل تلقائي لضمان حمايتهم.",
            icon = Icons.Default.AddCircle,
            color = Color(0xFFE3F2FD)
        ),
        OnboardingPageData(
            title = "تسجيل البيانات الأساسية 📝",
            description = "توثيق معلومات الطفل، سجل التلقيح، ورقم الهوية بدقة مع نظام تتبع جغرافي ذكي لتجنب ضياع السجلات.",
            icon = Icons.Default.Edit,
            color = Color(0xFFFFF3E0)
        ),
        OnboardingPageData(
            title = "ربط الطفل بالأسرة 🔗",
            description = "ربط الطفل الجديد بالأسرة المسجلة مسبقاً لتنظيم المتابعة العائلية وإرسال التنبيهات المباشرة لأولياء الأمور.",
            icon = Icons.Default.Share,
            color = Color(0xFFF3E5F5)
        ),
        OnboardingPageData(
            title = "حفظ كارت اللقاح 🛡️",
            description = "توليد كارت اللقاح الملون والباركود الفريد QR ومشاركته وحفظه لتسهيل مراجعة وتدقيق الجرعات بكل سهولة.",
            icon = Icons.Default.CheckCircle,
            color = Color(0xFFE0F2F1)
        )"""

onboarding_new = """        OnboardingPageData(
            title = Transl.t("onboarding_title_1", appLanguage),
            description = Transl.t("onboarding_desc_1", appLanguage),
            icon = Icons.Default.Home,
            color = Color(0xFFE8F5E9)
        ),
        OnboardingPageData(
            title = Transl.t("onboarding_title_2", appLanguage),
            description = Transl.t("onboarding_desc_2", appLanguage),
            icon = Icons.Default.AddCircle,
            color = Color(0xFFE3F2FD)
        ),
        OnboardingPageData(
            title = Transl.t("onboarding_title_3", appLanguage),
            description = Transl.t("onboarding_desc_3", appLanguage),
            icon = Icons.Default.Edit,
            color = Color(0xFFFFF3E0)
        ),
        OnboardingPageData(
            title = Transl.t("onboarding_title_4", appLanguage),
            description = Transl.t("onboarding_desc_4", appLanguage),
            icon = Icons.Default.Share,
            color = Color(0xFFF3E5F5)
        ),
        OnboardingPageData(
            title = Transl.t("onboarding_title_5", appLanguage),
            description = Transl.t("onboarding_desc_5", appLanguage),
            icon = Icons.Default.CheckCircle,
            color = Color(0xFFE0F2F1)
        )"""

content = content.replace(onboarding_ar, onboarding_new)

# Add to Transl
transl_ar = """            "setting_center_save" to "حفظ اسم المركز الصحي","""
new_transl_ar = """            "setting_center_save" to "حفظ اسم المركز الصحي",
            "onboarding_title_1" to "بوابة إدخال البيانات 📋",
            "onboarding_desc_1" to "واجهة رئيسية منظمة تتيح لك الوصول السريع لتسجيل الأطفال، تحديث بياناتهم، أو إدارة اللقاحات والأرصدة بكفاءة.",
            "onboarding_title_2" to "تسجيل طفل جديد 👶",
            "onboarding_desc_2" to "إضافة الأطفال دون سن الخامسة وتوليد جدول اللقاحات الوطني لهم بشكل تلقائي لضمان حمايتهم.",
            "onboarding_title_3" to "تسجيل البيانات الأساسية 📝",
            "onboarding_desc_3" to "توثيق معلومات الطفل، سجل التلقيح، ورقم الهوية بدقة مع نظام تتبع جغرافي ذكي لتجنب ضياع السجلات.",
            "onboarding_title_4" to "ربط الطفل بالأسرة 🔗",
            "onboarding_desc_4" to "ربط الطفل الجديد بالأسرة المسجلة مسبقاً لتنظيم المتابعة العائلية وإرسال التنبيهات المباشرة لأولياء الأمور.",
            "onboarding_title_5" to "حفظ كارت اللقاح 🛡️",
            "onboarding_desc_5" to "توليد كارت اللقاح الملون والباركود الفريد QR ومشاركته وحفظه لتسهيل مراجعة وتدقيق الجرعات بكل سهولة.",
            "onboarding_skip" to "تخطي ⏭️","""

transl_en = """            "print_card" to "Print Vaccination Record","""
new_transl_en = """            "print_card" to "Print Vaccination Record",
            "onboarding_title_1" to "Data Entry Portal 📋",
            "onboarding_desc_1" to "Organized main interface for quick access to register children, update data, or manage vaccines efficiently.",
            "onboarding_title_2" to "Register New Child 👶",
            "onboarding_desc_2" to "Add children under 5 and generate their national vaccine schedule automatically to ensure protection.",
            "onboarding_title_3" to "Record Basic Data 📝",
            "onboarding_desc_3" to "Document child info, vaccine record, and ID accurately with a smart geographic tracking system.",
            "onboarding_title_4" to "Link Child to Family 🔗",
            "onboarding_desc_4" to "Link new child to pre-registered family to organize follow-ups and send direct alerts to parents.",
            "onboarding_title_5" to "Save Vaccine Card 🛡️",
            "onboarding_desc_5" to "Generate a colored vaccine card and unique QR barcode, share and save it for easy dose auditing.",
            "onboarding_skip" to "Skip ⏭️","""

content = content.replace(transl_ar, new_transl_ar)
content = content.replace(transl_en, new_transl_en)

# Also patch the actual skip button text
content = content.replace('text = "تخطي ⏭️"', 'text = Transl.t("onboarding_skip", appLanguage)')

# Also fetch appLanguage inside OnboardingScreen
content = content.replace('fun OnboardingScreen(viewModel: VaccineViewModel) {\n    var currentPage by remember { mutableStateOf(0) }', 'fun OnboardingScreen(viewModel: VaccineViewModel) {\n    var currentPage by remember { mutableStateOf(0) }\n    val appLanguage by viewModel.appLanguage.collectAsState()')

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
print("Onboarding translated.")
