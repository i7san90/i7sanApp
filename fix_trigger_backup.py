import re

with open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "r") as f:
    content = f.read()

target = """            _backupStatusMessage.value = if (_appLanguage.value == "en") {
                "Backup successfully saved to Google Drive!"
            } else {
                "تم حفظ النسخة الاحتياطية بنجاح في Google Drive!"
            }"""

replacement = """            val email = _backupEmail.value.ifBlank { "default" }
            _backupStatusMessage.value = if (_appLanguage.value == "en") {
                "Backup successfully saved to Google Drive ($email)!"
            } else {
                "تم حفظ النسخة الاحتياطية بنجاح في Google Drive ($email)!"
            }"""

if target in content:
    content = content.replace(target, replacement)
    print("triggerGoogleDriveBackup updated.")
else:
    print("triggerGoogleDriveBackup NOT updated.")

with open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "w") as f:
    f.write(content)
