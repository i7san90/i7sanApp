import re

with open("app/src/main/java/com/example/data/VaccineModels.kt", "r") as f:
    content = f.read()

target = """    val familyNumber: String = "", // رقم الأسرة
    val headOfHousehold: String = "", // اسم رب الأسرة
    val isAdultTd: Boolean = false // هل هو ملقح ثنائي كبار؟
)"""

replacement = """    val familyNumber: String = "", // رقم الأسرة
    val headOfHousehold: String = "", // اسم رب الأسرة
    val isAdultTd: Boolean = false, // هل هو ملقح ثنائي كبار؟
    val gender: String = "ذكر", // الجنس: ذكر أو أنثى
    val photoUri: String? = null // مسار صورة الطفل
)"""

content = content.replace(target, replacement)

target2 = """@Database(entities = [Child::class, VaccineRecord::class, Family::class, InventoryRecord::class], version = 7, exportSchema = false)"""
replacement2 = """import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Child::class, VaccineRecord::class, Family::class, InventoryRecord::class], version = 8, exportSchema = false)"""
content = content.replace(target2, replacement2)

target3 = """.fallbackToDestructiveMigration()
                .build()"""
replacement3 = """.addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigration()
                .build()"""
content = content.replace(target3, replacement3)

target4 = """    companion object {"""
replacement4 = """    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE children ADD COLUMN gender TEXT NOT NULL DEFAULT 'ذكر'")
                database.execSQL("ALTER TABLE children ADD COLUMN photoUri TEXT")
            }
        }"""
content = content.replace(target4, replacement4)

with open("app/src/main/java/com/example/data/VaccineModels.kt", "w") as f:
    f.write(content)
