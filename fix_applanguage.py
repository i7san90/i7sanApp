import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

# Fix ChildItem
content = content.replace("fun ChildItem(child: com.example.data.Child, onClick: () -> Unit) {", 'fun ChildItem(child: com.example.data.Child, onClick: () -> Unit) {\n    val context = androidx.compose.ui.platform.LocalContext.current\n    val prefs = context.getSharedPreferences("vaccine_prefs", android.content.Context.MODE_PRIVATE)\n    val appLanguage = prefs.getString("app_language", "ar") ?: "ar"')

# Fix OnboardingUiMockup
content = content.replace("fun OnboardingUiMockup(pageIndex: Int, modifier: Modifier = Modifier) {", 'fun OnboardingUiMockup(pageIndex: Int, modifier: Modifier = Modifier) {\n    val context = androidx.compose.ui.platform.LocalContext.current\n    val prefs = context.getSharedPreferences("vaccine_prefs", android.content.Context.MODE_PRIVATE)\n    val appLanguage = prefs.getString("app_language", "ar") ?: "ar"')

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
print("appLanguage fixed.")
