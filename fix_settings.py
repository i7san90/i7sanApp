import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

target = """    val appLanguage by viewModel.appLanguage.collectAsState()

    var newUsername by remember(currentAdminUser) { mutableStateOf(currentAdminUser) }"""

replacement = """    val appLanguage by viewModel.appLanguage.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val backupStatusMessage by viewModel.backupStatusMessage.collectAsState()

    LaunchedEffect(backupStatusMessage) {
        backupStatusMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearBackupStatus()
        }
    }

    var newUsername by remember(currentAdminUser) { mutableStateOf(currentAdminUser) }"""

if target in content:
    print("Found target!")
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
        f.write(content)
else:
    print("Target not found. Let's try with regex.")
    
