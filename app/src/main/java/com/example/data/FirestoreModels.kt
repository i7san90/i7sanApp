package com.example.data

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * هيكلية قاعدة بيانات Firestore المقترحة للنظام العراقي
 *
 * Collections Structure:
 * 
 * 1. "families" (Collection)
 *    -> document(familyId) 
 *       -> FamilyDocument
 * 
 * 2. "children" (Collection)
 *    -> document(childId)
 *       -> ChildDocument
 *       -> "vaccinations" (Sub-collection)
 *          -> document(vaccineKey)
 *             -> VaccineDocument
 */

@IgnoreExtraProperties
data class FamilyDocument(
    @get:Exclude var id: String = "",
    val familyNumber: String = "",
    val headOfHousehold: String = "",
    val address: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class ChildDocument(
    @get:Exclude var id: String = "",
    val uniqueId: String = "",
    val fullName: String = "",
    val motherName: String = "",
    val healthCenter: String = "",
    val address: String = "",
    val phone: String = "",
    val dob: String = "",
    val gender: String = "ذكر",
    val registrationDate: Long = System.currentTimeMillis(),
    val vaccinationNumber: String = "",
    val familyId: String = "", // Reference to family
    val isAdultTd: Boolean = false
)

@IgnoreExtraProperties
data class VaccineDocument(
    @get:Exclude var id: String = "",
    val vaccineKey: String = "",
    val nameAr: String = "",
    val categoryAr: String = "",
    val dueMonths: Int = 0,
    val isTaken: Boolean = false,
    val takenDate: String? = null,
    val isSkipped: Boolean = false,
    val skipReason: String? = null,
    val administeredBy: String? = null, // Health worker name
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Standard Iraqi Vaccination Schedule (جدول اللقاحات الوطني العراقي)
 * 
 * - At Birth (0 months): BCG, OPV0, HepB
 * - 2 Months: OPV1, Penta1, Rota1
 * - 4 Months: OPV2, Penta2, Rota2
 * - 6 Months: OPV3, Penta3, Rota3
 * - 9 Months: Measles (أو MMR1 حسب التحديث), Vitamin A
 * - 15/18 Months: MMR, DPT1, OPV booster 1, Vitamin A
 * - 4-6 Years: DPT2, OPV booster 2, MMR2
 */
object IraqiVaccineSchedule {
    val schedule = listOf(
        // الولادة
        VaccineDocument(vaccineKey = "bcg", nameAr = "لقاح التدرن (BCG)", categoryAr = "عند الولادة", dueMonths = 0),
        VaccineDocument(vaccineKey = "opv0", nameAr = "شلل الأطفال الفموي (الجرعة الصفرية)", categoryAr = "عند الولادة", dueMonths = 0),
        VaccineDocument(vaccineKey = "hepb", nameAr = "الكبد الفيروسي ب (جرعة الولادة)", categoryAr = "عند الولادة", dueMonths = 0),
        // شهرين
        VaccineDocument(vaccineKey = "opv1", nameAr = "شلل الأطفال الفموي (الجرعة الأولى)", categoryAr = "شهران", dueMonths = 2),
        VaccineDocument(vaccineKey = "penta1", nameAr = "اللقاح الخماسي (الجرعة الأولى)", categoryAr = "شهران", dueMonths = 2),
        VaccineDocument(vaccineKey = "rota1", nameAr = "لقاح الروتا (الجرعة الأولى)", categoryAr = "شهران", dueMonths = 2),
        // 4 أشهر
        VaccineDocument(vaccineKey = "opv2", nameAr = "شلل الأطفال الفموي (الجرعة الثانية)", categoryAr = "4 أشهر", dueMonths = 4),
        VaccineDocument(vaccineKey = "penta2", nameAr = "اللقاح الخماسي (الجرعة الثانية)", categoryAr = "4 أشهر", dueMonths = 4),
        VaccineDocument(vaccineKey = "rota2", nameAr = "لقاح الروتا (الجرعة الثانية)", categoryAr = "4 أشهر", dueMonths = 4),
        // 6 أشهر
        VaccineDocument(vaccineKey = "opv3", nameAr = "شلل الأطفال الفموي (الجرعة الثالثة)", categoryAr = "6 أشهر", dueMonths = 6),
        VaccineDocument(vaccineKey = "penta3", nameAr = "اللقاح الخماسي (الجرعة الثالثة)", categoryAr = "6 أشهر", dueMonths = 6),
        VaccineDocument(vaccineKey = "rota3", nameAr = "لقاح الروتا (الجرعة الثالثة)", categoryAr = "6 أشهر", dueMonths = 6),
        // 9 أشهر
        VaccineDocument(vaccineKey = "measles", nameAr = "الحصبة المنفردة / الزرقاء", categoryAr = "9 أشهر", dueMonths = 9),
        VaccineDocument(vaccineKey = "vitA1", nameAr = "فيتامين أ (الجرعة الأولى)", categoryAr = "9 أشهر", dueMonths = 9),
        // 15 - 18 شهر
        VaccineDocument(vaccineKey = "mmr1", nameAr = "الحصبة المختلطة (MMR)", categoryAr = "15 شهر", dueMonths = 15),
        VaccineDocument(vaccineKey = "dpt1", nameAr = "الثلاثي (الجرعة المنشطة الأولى)", categoryAr = "18 شهر", dueMonths = 18),
        VaccineDocument(vaccineKey = "opv_booster1", nameAr = "شلل الأطفال الفموي (منشطة أولى)", categoryAr = "18 شهر", dueMonths = 18),
        // 4 - 6 سنوات
        VaccineDocument(vaccineKey = "dpt2", nameAr = "الثلاثي (الجرعة المنشطة الثانية)", categoryAr = "4 - 6 سنوات", dueMonths = 48),
        VaccineDocument(vaccineKey = "opv_booster2", nameAr = "شلل الأطفال الفموي (منشطة ثانية)", categoryAr = "4 - 6 سنوات", dueMonths = 48),
        VaccineDocument(vaccineKey = "mmr2", nameAr = "الحصبة المختلطة (MMR الثانية)", categoryAr = "4 - 6 سنوات", dueMonths = 48)
    )
}
