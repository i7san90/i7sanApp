import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

# Home Screen strings
replacements = {
    '"المنصة الطبية الذكية لإدارة لقاحات ورعاية الطفولة والأسرة"': 'Transl.t("home_slogan", appLanguage)',
    '"إدخال البيانات 📝"': 'Transl.t("home_data_entry_title", appLanguage)',
    '"إضافة بيانات أسرة جديدة، تسجيل طفل جديد، وتسجيل لقاح ثنائي كبار"': 'Transl.t("home_data_entry_desc", appLanguage)',
    '"عرض الملقحين والبيانات 📋"': 'Transl.t("home_view_data_title", appLanguage)',
    '"عرض بيانات الأسر، الأطفال المسجلين، والملقحين بالثنائي كبار"': 'Transl.t("home_view_data_desc", appLanguage)',
    '"إدارة اللقاحات ومستلزمات التلقيح 📊"': 'Transl.t("home_inventory_title", appLanguage)',
    '"متابعة الرصيد المستلم والمتبقي، حساب نسب الهدر بدقة، والترحيل التلقائي الشهري والتحليل الذكي للتقارير"': 'Transl.t("home_inventory_desc", appLanguage)',
    '"الملقحين المسجلين 📋"': 'Transl.t("home_registered_title", appLanguage)',
    '"قائمة الملقحين بالمركز"': 'Transl.t("home_registered_desc", appLanguage)',
    '"ابحث باسم الطفل أو رقم السجل..."': 'Transl.t("search_placeholder_2", appLanguage)',
    '"الكل 🌐"': 'Transl.t("tab_all", appLanguage)',
    '"الملقحين الأطفال 👶"': 'Transl.t("tab_children_only", appLanguage)',
    '"ثنائي كبار 💉"': 'Transl.t("tab_adult_td", appLanguage)'
}

for k, v in replacements.items():
    content = content.replace(k, v)

# Update Transl object
transl_ar = """            "setting_center_save" to "حفظ اسم المركز الصحي","""
new_transl_ar = """            "setting_center_save" to "حفظ اسم المركز الصحي",
            "home_slogan" to "المنصة الطبية الذكية لإدارة لقاحات ورعاية الطفولة والأسرة",
            "home_data_entry_title" to "إدخال البيانات 📝",
            "home_data_entry_desc" to "إضافة بيانات أسرة جديدة، تسجيل طفل جديد، وتسجيل لقاح ثنائي كبار",
            "home_view_data_title" to "عرض الملقحين والبيانات 📋",
            "home_view_data_desc" to "عرض بيانات الأسر، الأطفال المسجلين، والملقحين بالثنائي كبار",
            "home_inventory_title" to "إدارة اللقاحات ومستلزمات التلقيح 📊",
            "home_inventory_desc" to "متابعة الرصيد المستلم والمتبقي، حساب نسب الهدر بدقة، والترحيل التلقائي الشهري والتحليل الذكي للتقارير",
            "home_registered_title" to "الملقحين المسجلين 📋",
            "home_registered_desc" to "قائمة الملقحين بالمركز",
            "search_placeholder_2" to "ابحث باسم الطفل أو رقم السجل...",
            "tab_all" to "الكل 🌐",
            "tab_children_only" to "الملقحين الأطفال 👶",
            "tab_adult_td" to "ثنائي كبار 💉",
            "download_pdf" to "تنزيل كارت لقاح (PDF)",
            "print_card" to "طباعة سجل اللقاحات","""

transl_en = """            "menu_children" to "Home","""
new_transl_en = """            "menu_children" to "Home",
            "home_slogan" to "Smart Medical Platform for Vaccine & Family Care Management",
            "home_data_entry_title" to "Data Entry 📝",
            "home_data_entry_desc" to "Add new family data, register a new child, and record adult Td vaccine",
            "home_view_data_title" to "View Vaccinated & Data 📋",
            "home_view_data_desc" to "View families data, registered children, and adult Td records",
            "home_inventory_title" to "Vaccine Inventory Management 📊",
            "home_inventory_desc" to "Track received & remaining stock, calculate waste rates accurately, and monthly automated reporting",
            "home_registered_title" to "Registered Vaccinees 📋",
            "home_registered_desc" to "List of center vaccinees",
            "search_placeholder_2" to "Search by child name or record number...",
            "tab_all" to "All 🌐",
            "tab_children_only" to "Children 👶",
            "tab_adult_td" to "Adult Td 💉",
            "download_pdf" to "Download Vaccine Card (PDF)",
            "print_card" to "Print Vaccination Record","""

content = content.replace(transl_ar, new_transl_ar)
content = content.replace(transl_en, new_transl_en)

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
print("Language translation patched.")
