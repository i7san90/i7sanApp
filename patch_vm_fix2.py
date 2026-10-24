import sys

content = open("app/src/main/java/com/example/ui/VaccineViewModel.kt").read()

idx = content.find("        val allChildren = repository.getAllChildrenSync()")
if idx != -1:
    end_idx = content.find("private val prefs", idx)
    if end_idx != -1:
        # replace the last } } before private val prefs
        sub = content[idx:end_idx]
        sub = sub.replace("        }\n    }\n", "        }\n        } catch (e: Exception) {\n            android.util.Log.e(\"FirestoreSync\", \"Failed to sync: ${e.message}\")\n        }\n    }\n\n    ")
        content = content[:idx] + sub + content[end_idx:]

open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "w").write(content)
