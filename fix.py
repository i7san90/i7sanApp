import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

pattern = re.compile(r"val appLanguage by viewModel\.appLanguage\.collectAsState\(\)\n    val isBackingUp by viewModel\.isBackingUp\.collectAsState\(\)\n    val backupStatusMessage by viewModel\.backupStatusMessage\.collectAsState\(\)\n    LaunchedEffect\(backupStatusMessage\) \{\n        backupStatusMessage\?\.let \{\n            android\.widget\.Toast\.makeText\(context, it, android\.widget\.Toast\.LENGTH_LONG\)\.show\(\)\n            viewModel\.clearBackupStatus\(\)\n        \}\n    \}")

content = pattern.sub("val appLanguage by viewModel.appLanguage.collectAsState()", content)

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
