import sys

content = open("app/src/main/java/com/example/ui/VaccineViewModel.kt").read()

# find the exact string and replace it
content = content.replace("        }\n    }\n\n    private val prefs", "        }\n        } catch (e: Exception) {\n            android.util.Log.e(\"FirestoreSync\", \"Failed to sync: ${e.message}\")\n        }\n    }\n\n    private val prefs")

open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "w").write(content)
