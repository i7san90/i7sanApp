import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

content = content.replace(
    'fun ChildCard(child: Child, viewModel: VaccineViewModel, onClick: () -> Unit, onEdit: () -> Unit = {}, onDelete: () -> Unit = {}) {',
    'fun ChildCard(child: Child, viewModel: VaccineViewModel, onClick: () -> Unit, onEdit: () -> Unit = {}, onDelete: () -> Unit = {}) {\n    val appLanguage by viewModel.appLanguage.collectAsState()'
)

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
print("ChildCard fixed.")
