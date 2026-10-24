import re

with open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "r") as f:
    content = f.read()

target = """    private val _isBackingUp = MutableStateFlow(false)"""

replacement = """    private val _backupEmail = MutableStateFlow(prefs.getString("backup_email", "") ?: "")
    val backupEmail: StateFlow<String> = _backupEmail.asStateFlow()

    fun updateBackupEmail(email: String) {
        _backupEmail.value = email
        prefs.edit().putString("backup_email", email).apply()
    }

    private val _isBackingUp = MutableStateFlow(false)"""

if target in content:
    content = content.replace(target, replacement)
    print("ViewModel email updated.")
else:
    print("ViewModel email NOT updated.")

with open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "w") as f:
    f.write(content)
