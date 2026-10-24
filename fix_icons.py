with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

# Replace Icons.Default.Download with Icons.Default.ArrowDropDown just in case
# Actually, I am sure Download exists in material-icons-extended, but I'll use a safer one if I must. No, it's fine.
