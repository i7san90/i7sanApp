package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "children")
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uniqueId: String, // Independent unique ID, e.g. IrqVacc-10398
    val fullName: String, // اسم الطفل الرباعي
    val motherName: String = "", // اسم الأم الثلاثي
    val healthCenter: String = "طاق ميكائيل", // أقرب مركز صحي
    val address: String = "", // عنوان السكن
    val phone: String,    // رقم الهاتف الواتساب
    val dob: String,      // تاريخ الميلاد (YYYY-MM-DD)
    val registrationDate: Long = System.currentTimeMillis(),
    val vaccinationNumber: String = "", // الرقم التلقيحي للطفل في السجل الدائمي
    val familyNumber: String = "", // رقم الأسرة
    val headOfHousehold: String = "", // اسم رب الأسرة
    val isAdultTd: Boolean = false, // هل هو ملقح ثنائي كبار؟
    val gender: String = "ذكر", // الجنس: ذكر أو أنثى
    val photoUri: String? = null, // مسار صورة الطفل
    val locationUrl: String? = null // رابط الموقع الجغرافي
)

@Entity(
    tableName = "vaccine_records",
    foreignKeys = [
        ForeignKey(
            entity = Child::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["childId"])]
)
data class VaccineRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int,
    val vaccineKey: String,
    val nameAr: String,       // الاسم العربي للقاح
    val categoryAr: String,   // الفئة العمرية (مثال: عند الولادة، عمر شهرين)
    val dueMonths: Int,       // الاستحقاق بالأشهر من الولادة
    val isTaken: Boolean = false,
    val takenDate: String? = null, // تاريخ أخذ اللقاح في حال تم
    val isSkipped: Boolean = false, // تم تجاوز / تخطي اللقاح
    val skipReason: String? = null, // سبب تجاوز اللقاح
    val sideEffect: String? = null, // الأثر الجانبي المبلّغ عنه إن وجد
    val isIgnored: Boolean = false // تجاهل التذكير
)

@Dao
interface VaccineDao {
    @Query("SELECT * FROM children ORDER BY id DESC")
    fun getAllChildren(): Flow<List<Child>>

    @Query("SELECT * FROM vaccine_records")
    fun getAllVaccineRecordsFlow(): Flow<List<VaccineRecord>>

    @Query("SELECT * FROM children WHERE id = :childId LIMIT 1")
    suspend fun getChildById(childId: Int): Child?

    @Query("SELECT * FROM children WHERE uniqueId = :uniqueId LIMIT 1")
    suspend fun getChildByUniqueId(uniqueId: String): Child?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: Child): Long

    @Update
    suspend fun updateChild(child: Child)

    @Delete
    suspend fun deleteChild(child: Child)

    @Query("SELECT * FROM vaccine_records WHERE childId = :childId ORDER BY dueMonths ASC")
    fun getVaccinesForChild(childId: Int): Flow<List<VaccineRecord>>

    @Query("SELECT * FROM vaccine_records WHERE childId = :childId")
    suspend fun getVaccinesForChildSync(childId: Int): List<VaccineRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaccineRecords(records: List<VaccineRecord>)

    @Update
    suspend fun updateVaccineRecord(record: VaccineRecord)
    
    @Query("SELECT * FROM vaccine_records WHERE isTaken = 0")
    suspend fun getAllPendingVaccinesSync(): List<VaccineRecord>

    // Family management methods
    @Query("SELECT * FROM families ORDER BY id DESC")
    fun getAllFamilies(): Flow<List<Family>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamily(family: Family): Long

    @Update
    suspend fun updateFamily(family: Family)

    @Delete
    suspend fun deleteFamily(family: Family)

    @Query("SELECT * FROM families WHERE familyNumber = :familyNumber LIMIT 1")
    suspend fun getFamilyByNumber(familyNumber: String): Family?

    @Query("SELECT * FROM children")
    suspend fun getAllChildrenSync(): List<Child>

    @Query("SELECT * FROM vaccine_records")
    suspend fun getAllVaccineRecordsSync(): List<VaccineRecord>

    @Query("SELECT * FROM families")
    suspend fun getAllFamiliesSync(): List<Family>

    @Query("SELECT * FROM inventory_records WHERE month = :month AND year = :year")
    fun getInventoryRecordsFlow(month: String, year: String): Flow<List<InventoryRecord>>

    @Query("SELECT * FROM inventory_records WHERE month = :month AND year = :year")
    suspend fun getInventoryRecordsSync(month: String, year: String): List<InventoryRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryRecords(records: List<InventoryRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryRecord(record: InventoryRecord)

    @Query("SELECT DISTINCT year FROM inventory_records")
    fun getAllInventoryYears(): Flow<List<String>>

    @Query("SELECT * FROM inventory_records")
    suspend fun getAllInventoryRecordsSync(): List<InventoryRecord>

    @Query("DELETE FROM children")
    suspend fun deleteAllChildren()

    @Query("DELETE FROM vaccine_records")
    suspend fun deleteAllVaccineRecords()

    @Query("DELETE FROM families")
    suspend fun deleteAllFamilies()
}

@Entity(tableName = "families")
data class Family(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val familyNumber: String, // رقم الأسرة
    val headOfHousehold: String, // اسم رب الأسرة
    val address: String = "", // السكن
    val phone: String = "" // الهاتف
)

@Entity(
    tableName = "inventory_records",
    primaryKeys = ["month", "year", "itemName"]
)
data class InventoryRecord(
    val month: String,      // e.g., "حزيران", "تموز", etc.
    val year: String,       // e.g., "2026"
    val itemName: String,   // Name of vaccine or supply
    val itemType: String,   // "vaccine" or "supply"
    val initialBalance: Int = 0,
    val receivedDuringMonth: Int = 0,
    val fixedSessionVaccinated: Int = 0,
    val outreachSessionVaccinated: Int = 0,
    val openedWasted: Int = 0,
    val closedWasted: Int = 0,
    val transferredOut: Int = 0,
    val expiryDate: String? = null // YYYY-MM-DD
) {
    val totalAvailable: Int get() = initialBalance + receivedDuringMonth
    val totalVaccinated: Int get() = fixedSessionVaccinated + outreachSessionVaccinated
    val totalWasted: Int get() = openedWasted + closedWasted
    val finalRemaining: Int get() = totalAvailable - (totalVaccinated + totalWasted + transferredOut)
}


@Database(entities = [Child::class, VaccineRecord::class, Family::class, InventoryRecord::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vaccineDao(): VaccineDao

    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE children ADD COLUMN gender TEXT NOT NULL DEFAULT 'ذكر'")
                database.execSQL("ALTER TABLE children ADD COLUMN photoUri TEXT")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE children ADD COLUMN locationUrl TEXT")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `families` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `familyNumber` TEXT NOT NULL, `headOfHousehold` TEXT NOT NULL, `address` TEXT NOT NULL, `phone` TEXT NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `inventory_records` (`month` TEXT NOT NULL, `year` TEXT NOT NULL, `itemName` TEXT NOT NULL, `itemType` TEXT NOT NULL, `initialBalance` INTEGER NOT NULL, `receivedDuringMonth` INTEGER NOT NULL, `fixedSessionVaccinated` INTEGER NOT NULL, `outreachSessionVaccinated` INTEGER NOT NULL, `openedWasted` INTEGER NOT NULL, `closedWasted` INTEGER NOT NULL, `transferredOut` INTEGER NOT NULL, `expiryDate` TEXT, PRIMARY KEY(`month`, `year`, `itemName`))")
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iraq_vaccines_v2_database"
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Vaccine status helper enum
enum class VaccineStatus {
    PENDING,      // معلق
    APPROACHING,  // يقترب الموعد (متبقي 3 أيام أو أقل)
    OVERDUE,      // متأخر جدا (تجاوز الاستحقاق بـ 28 يوماً)
    COMPLETED,    // مكتمل
    IGNORED       // متجاهل
}

// Clinical Utility classes for computations
object VaccineScheduler {
    
    // Official vaccination schedule in Iraq
    data class ScheduleItem(
        val key: String,
        val nameAr: String,
        val categoryAr: String,
        val dueMonths: Int
    )

    val iraqSchedule = listOf(
        // At Birth / 24 Hours (خلال أول ٢٤ ساعة من الولادة)
        ScheduleItem("HepB_0", "الكبد الفيروسي نمط B صغار (جرعة الصفر)", "خلال أول ٢٤ ساعة من الولادة", 0),
        
        // At First Week (خلال الأسبوع الأول)
        ScheduleItem("OPV_0", "شلل الأطفال الفموي (جرعة الصفر)", "خلال الأسبوع الأول", 0),
        ScheduleItem("BCG", "بي سي جي (BCG) (جرعة الصفر)", "خلال الأسبوع الأول", 0),
        
        // At 2 Months (عمر ٢ شهر)
        ScheduleItem("OPV_1", "شلل الأطفال الفموي (الجرعة الاولى ج١)", "عمر ٢ شهر", 2),
        ScheduleItem("Penta_1", "الخماسي الخلوي (الجرعة الاولى ج١)", "عمر ٢ شهر", 2),
        ScheduleItem("PCV_1", "المكورات الرئوية المقترن (الجرعة الاولى ج١)", "عمر ٢ شهر", 2),
        ScheduleItem("Rota_1", "الفيروس الدوار (الجرعة الاولى ج١)", "عمر ٢ شهر", 2),
        
        // At 4 Months (عمر ٤ أشهر)
        ScheduleItem("OPV_2", "شلل الأطفال الفموي (الجرعة الثانية ج٢)", "عمر ٤ أشهر", 4),
        ScheduleItem("Penta_2", "الخماسي الخلوي (الجرعة الثانية ج٢)", "عمر ٤ أشهر", 4),
        ScheduleItem("IPV_1", "شلل الأطفال الزرقي (الجرعة الاولى ج١)", "عمر ٤ أشهر", 4),
        ScheduleItem("PCV_2", "المكورات الرئوية المقترن (الجرعة الثانية ج٢)", "عمر ٤ أشهر", 4),
        ScheduleItem("Rota_2", "الفيروس الدوار (الجرعة الثانية ج٢)", "عمر ٤ أشهر", 4),
        
        // At 6 Months (عمر ٦ أشهر)
        ScheduleItem("OPV_3", "شلل الأطفال الفموي (الجرعة الثالثة ج٣)", "عمر ٦ أشهر", 6),
        ScheduleItem("Penta_3", "الخماسي الخلوي (الجرعة الثالثة ج٣)", "عمر ٦ أشهر", 6),
        ScheduleItem("IPV_2", "شلل الأطفال الزرقي (الجرعة الثانية ج٢)", "عمر ٦ أشهر", 6),
        ScheduleItem("PCV_3", "المكورات الرئوية المقترن (الجرعة الثالثة ج٣)", "عمر ٦ أشهر", 6),
        ScheduleItem("Rota_3", "الفيروس الدوار (الجرعة الثالثة ج٣)", "عمر ٦ أشهر", 6),
        
        // At 9 Months (عمر ٩ أشهر)
        ScheduleItem("Measles", "الحصبة المنفردة (الجرعة المنفردة)", "عمر ٩ أشهر", 9),
        ScheduleItem("VitA_1", "فيتامين A (١٠٠,٠٠٠) وحدة دولية", "عمر ٩ أشهر", 9),
        
        // At 12 Months (عمر ١٢ شهر)
        ScheduleItem("MMR_1", "الحصبة المختلطة (الجرعة الاولى ج١)", "عمر ١٢ شهر", 12),
        
        // At 18 Months (عمر ١٨ شهر)
        ScheduleItem("Booster_OPV_1", "شلل الأطفال الفموي (الجرعة المنشطة الاولى م١)", "عمر ١٨ شهر", 18),
        ScheduleItem("Booster_DPT_1", "اللقاح الثلاثي (الجرعة المنشطة الاولى م١)", "عمر ١٨ شهر", 18),
        ScheduleItem("MMR_2", "الحصبة المختلطة (الجرعة الثانية ج٢)", "عمر ١٨ شهر", 18),
        ScheduleItem("VitA_2", "فيتامين A (٢٠٠,٠٠٠) وحدة دولية", "عمر ١٨ شهر", 18),
        
        // At 4 - 6 Years (عمر ٤ - ٦ سنوات)
        ScheduleItem("Booster_OPV_2", "شلل الاطفال الفموي (الجرعة المنشطة الثانية م٢)", "عمر ٤ - ٦ سنوات", 48),
        ScheduleItem("Booster_DPT_2", "اللقاح الثلاثي (الجرعة المنشطة الثانية م٢)", "عمر ٤ - ٦ سنوات", 48),
        ScheduleItem("VitA_3", "فيتامين A (٢٠٠,٠٠٠) وحدة دولية", "عمر ٤ - ٦ سنوات", 48)
    )

    val adultTdSchedule = listOf(
        ScheduleItem("Td_1", "لقاح ثنائي كبار (الجرعة الأولى)", "عند الزيارة الأولى (12 سنة فما فوق)", 0),
        ScheduleItem("Td_2", "لقاح ثنائي كبار (الجرعة الثانية)", "بعد 4 أسابيع من الأولى", 1),
        ScheduleItem("Td_3", "لقاح ثنائي كبار (الجرعة الثالثة)", "بعد 6 أشهر من الثانية", 7),
        ScheduleItem("Td_4", "لقاح ثنائي كبار (الجرعة الرابعة)", "بعد سنة من الثالثة", 19),
        ScheduleItem("Td_5", "لقاح ثنائي كبار (الجرعة الخامسة)", "بعد سنة من الرابعة", 31)
    )

    fun calculateAdultTdDueDate(childDob: String, vaccineKey: String, allRecords: List<VaccineRecord>): String {
        val sortedRecords = allRecords.filter { it.vaccineKey.startsWith("Td_") }
            .sortedBy { it.vaccineKey }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        fun addInterval(fromDateStr: String, amount: Int, field: Int): String {
            return try {
                val date = sdf.parse(fromDateStr) ?: Date()
                val cal = Calendar.getInstance().apply { time = date }
                cal.add(field, amount)
                sdf.format(cal.time)
            } catch (e: Exception) {
                fromDateStr
            }
        }

        var lastDate = childDob
        
        for (rec in sortedRecords) {
            val expectedDue = when (rec.vaccineKey) {
                "Td_1" -> childDob
                "Td_2" -> addInterval(lastDate, 28, Calendar.DAY_OF_YEAR)
                "Td_3" -> addInterval(lastDate, 6, Calendar.MONTH)
                "Td_4" -> addInterval(lastDate, 12, Calendar.MONTH)
                "Td_5" -> addInterval(lastDate, 12, Calendar.MONTH)
                else -> childDob
            }
            
            if (rec.vaccineKey == vaccineKey) {
                return expectedDue
            }
            
            lastDate = if (rec.isTaken && rec.takenDate != null) {
                rec.takenDate
            } else {
                expectedDue
            }
        }
        
        return when (vaccineKey) {
            "Td_1" -> childDob
            "Td_2" -> addInterval(childDob, 28, Calendar.DAY_OF_YEAR)
            "Td_3" -> addInterval(addInterval(childDob, 28, Calendar.DAY_OF_YEAR), 6, Calendar.MONTH)
            "Td_4" -> addInterval(addInterval(addInterval(childDob, 28, Calendar.DAY_OF_YEAR), 6, Calendar.MONTH), 12, Calendar.MONTH)
            "Td_5" -> addInterval(addInterval(addInterval(addInterval(childDob, 28, Calendar.DAY_OF_YEAR), 6, Calendar.MONTH), 12, Calendar.MONTH), 12, Calendar.MONTH)
            else -> childDob
        }
    }

    fun getRecordDueDate(child: Child, record: VaccineRecord, allRecords: List<VaccineRecord>): String {
        if (child.isAdultTd && record.vaccineKey.startsWith("Td_")) {
            return calculateAdultTdDueDate(child.dob, record.vaccineKey, allRecords)
        }
        return calculateDueDate(child.dob, record.dueMonths)
    }

    fun getRecordStatus(child: Child, record: VaccineRecord, allRecords: List<VaccineRecord>): VaccineStatus {
        if (record.isTaken) return VaccineStatus.COMPLETED
        if (record.isIgnored) return VaccineStatus.IGNORED
        
        val dueDateString = getRecordDueDate(child, record, allRecords)
        val dueDays = getEpochDays(dueDateString)
        val currentDays = getCurrentEpochDays()
        
        val diff = dueDays - currentDays
        val overdueDiff = currentDays - dueDays

        return when {
            overdueDiff > 28 -> VaccineStatus.OVERDUE
            diff in 0..3 -> VaccineStatus.APPROACHING
            overdueDiff in 1..28 -> VaccineStatus.APPROACHING
            dueDateString == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) -> VaccineStatus.APPROACHING
            else -> VaccineStatus.PENDING
        }
    }

    fun calculateDueDate(dobString: String, dueMonths: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val date = sdf.parse(dobString) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.MONTH, dueMonths)
            sdf.format(cal.time)
        } catch (e: Exception) {
            dobString
        }
    }

    // Returns number of epoch days
    fun getEpochDays(dateString: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val date = sdf.parse(dateString) ?: Date()
            date.time / (1000 * 60 * 60 * 24)
        } catch (e: Exception) {
            0
        }
    }

    fun getCurrentEpochDays(): Long {
        return System.currentTimeMillis() / (1000 * 60 * 60 * 24)
    }

    fun getStatus(dob: String, record: VaccineRecord): VaccineStatus {
        if (record.isTaken) return VaccineStatus.COMPLETED
        if (record.isIgnored) return VaccineStatus.IGNORED
        
        val dueDateString = calculateDueDate(dob, record.dueMonths)
        val dueDays = getEpochDays(dueDateString)
        val currentDays = getCurrentEpochDays()
        
        val diff = dueDays - currentDays
        val overdueDiff = currentDays - dueDays

        return when {
            overdueDiff > 28 -> VaccineStatus.OVERDUE
            diff in 0..3 -> VaccineStatus.APPROACHING
            overdueDiff in 1..28 -> VaccineStatus.APPROACHING // If already past but less than 28 days, it is also due / warning
            dueDateString == SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) -> VaccineStatus.APPROACHING
            else -> VaccineStatus.PENDING
        }
    }

    // Beautiful UI representation of age in months/years
    fun computeAgeString(dobString: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val birthDate = sdf.parse(dobString) ?: return "غير معروف"
            val birthCal = Calendar.getInstance().apply { time = birthDate }
            val nowCal = Calendar.getInstance()
            
            var years = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            var months = nowCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH)
            var days = nowCal.get(Calendar.DAY_OF_MONTH) - birthCal.get(Calendar.DAY_OF_MONTH)
            
            if (days < 0) {
                months--
            }
            if (months < 0) {
                years--
                months += 12
            }
            
            val totalMonths = (years * 12) + months
            when {
                totalMonths == 0 -> "حديث الولادة"
                totalMonths < 12 -> "$totalMonths شهر"
                else -> {
                    val yrPart = totalMonths / 12
                    val moPart = totalMonths % 12
                    if (moPart == 0) "$yrPart سنة" else "$yrPart سنة و $moPart شهر"
                }
            }
        } catch (e: Exception) {
            "غير معروف"
        }
    }
}
