with open("app/src/main/java/com/example/data/VaccineModels.kt", "r") as f:
    lines = f.readlines()

new_lines = []
imports = []
for line in lines:
    if line.startswith("import androidx.room.migration.Migration") or line.startswith("import androidx.sqlite.db.SupportSQLiteDatabase"):
        imports.append(line)
    else:
        new_lines.append(line)

new_content = ""
for line in new_lines:
    new_content += line
    if line.startswith("import java.util.*"):
        for imp in imports:
            new_content += imp

with open("app/src/main/java/com/example/data/VaccineModels.kt", "w") as f:
    f.write(new_content)
