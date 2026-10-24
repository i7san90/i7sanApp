import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

target = """    val backupStatusMessage by viewModel.backupStatusMessage.collectAsState()"""

replacement = """    val backupStatusMessage by viewModel.backupStatusMessage.collectAsState()
    val backupEmail by viewModel.backupEmail.collectAsState()"""

if target in content:
    content = content.replace(target, replacement)
    print("Settings vars updated.")
else:
    print("Settings vars NOT updated.")

target2 = """            CollapsibleSection(
                title = if (appLanguage == "en") "Cloud Backup (Google Drive)" else "النسخ الاحتياطي السحابي (Google Drive)",
                icon = Icons.Default.Settings,
            ) {"""

replacement2 = """            CollapsibleSection(
                title = if (appLanguage == "en") "Cloud Backup (Google Drive)" else "النسخ الاحتياطي السحابي (Google Drive)",
                icon = Icons.Default.Settings,
            ) {
                OutlinedTextField(
                    value = backupEmail,
                    onValueChange = { viewModel.updateBackupEmail(it) },
                    label = { Text(if (appLanguage == "en") "Backup Gmail Account" else "حساب جيميل المعتمد للنسخ الاحتياطي") },
                    placeholder = { Text("example@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    singleLine = true
                )
"""

if target2 in content:
    content = content.replace(target2, replacement2)
    print("Settings UI updated.")
else:
    print("Settings UI NOT updated.")

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
