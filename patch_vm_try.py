import sys

content = open("app/src/main/java/com/example/ui/VaccineViewModel.kt").read()

if "try {" not in content.split("suspend fun syncDataToFirestore() {")[1][:50]:
    content = content.replace("suspend fun syncDataToFirestore() {\n        val allFamilies", "suspend fun syncDataToFirestore() {\n        try {\n            val allFamilies")
    content = content.replace("        }\n    }\n\n    private val prefs", "        }\n        } catch (e: Exception) {\n            android.util.Log.e(\"FirestoreSync\", \"Failed to sync: ${e.message}\")\n        }\n    }\n\n    private val prefs")
    open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "w").write(content)
