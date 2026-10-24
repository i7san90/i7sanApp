package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class VaccineRepository(private val vaccineDao: VaccineDao) {

    val dao: VaccineDao = vaccineDao

    val allChildren: Flow<List<Child>> = vaccineDao.getAllChildren()
    val allFamilies: Flow<List<Family>> = vaccineDao.getAllFamilies()
    suspend fun getAllFamiliesSync(): List<Family> = withContext(Dispatchers.IO) { vaccineDao.getAllFamiliesSync() }
    suspend fun getAllChildrenSync(): List<Child> = withContext(Dispatchers.IO) { vaccineDao.getAllChildrenSync() }

    suspend fun insertFamily(familyNumber: String, headOfHousehold: String, address: String = "", phone: String = ""): Long = withContext(Dispatchers.IO) {
        val family = Family(
            familyNumber = familyNumber,
            headOfHousehold = headOfHousehold,
            address = address,
            phone = phone
        )
        vaccineDao.insertFamily(family)
    }

    suspend fun updateFamily(family: Family) = withContext(Dispatchers.IO) {
        vaccineDao.updateFamily(family)
    }

    suspend fun deleteFamily(family: Family) = withContext(Dispatchers.IO) {
        vaccineDao.deleteFamily(family)
    }

    suspend fun getFamilyByNumber(familyNumber: String): Family? = withContext(Dispatchers.IO) {
        vaccineDao.getFamilyByNumber(familyNumber)
    }

    fun getVaccinesForChild(childId: Int): Flow<List<VaccineRecord>> {
        return vaccineDao.getVaccinesForChild(childId)
    }

    suspend fun registerChild(
        fullName: String,
        motherName: String,
        healthCenter: String,
        address: String,
        phone: String,
        dob: String,
        vaccinationNumber: String,
        familyNumber: String,
        headOfHousehold: String,
        isAdultTd: Boolean,
        gender: String = "ذكر",
        photoUri: String? = null
    ): Long = withContext(Dispatchers.IO) {
        // Generate independent unique clinical ID, e.g., IRQ-26-XXXX
        val randomSuffix = (10000..99999).random()
        val uniqueId = "IRQ-26-$randomSuffix"

        val child = Child(
            uniqueId = uniqueId,
            fullName = fullName,
            motherName = motherName,
            healthCenter = healthCenter.ifBlank { "طاق ميكائيل" },
            address = address,
            phone = phone,
            dob = dob,
            vaccinationNumber = vaccinationNumber,
            familyNumber = familyNumber,
            headOfHousehold = headOfHousehold,
            isAdultTd = isAdultTd,
            gender = gender,
            photoUri = photoUri
        )

        val insertedId = vaccineDao.insertChild(child).toInt()

        // Choose schedule based on type
        val scheduleList = if (isAdultTd) {
            VaccineScheduler.adultTdSchedule
        } else {
            VaccineScheduler.iraqSchedule
        }

        val vaccineRecords = scheduleList.map { item ->
            VaccineRecord(
                childId = insertedId,
                vaccineKey = item.key,
                nameAr = item.nameAr,
                categoryAr = item.categoryAr,
                dueMonths = item.dueMonths,
                isTaken = false,
                takenDate = null,
                isSkipped = false,
                skipReason = null
            )
        }
        vaccineDao.insertVaccineRecords(vaccineRecords)

        insertedId.toLong()
    }

    suspend fun updateVaccineStatus(record: VaccineRecord, isTaken: Boolean, takenDate: String?) = withContext(Dispatchers.IO) {
        val updatedRecord = record.copy(isTaken = isTaken, takenDate = takenDate, isSkipped = false, skipReason = null)
        vaccineDao.updateVaccineRecord(updatedRecord)
    }

    suspend fun updateVaccineSkipStatus(record: VaccineRecord, isSkipped: Boolean, reason: String?) = withContext(Dispatchers.IO) {
        val updatedRecord = record.copy(isSkipped = isSkipped, skipReason = reason, isTaken = false, takenDate = null)
        vaccineDao.updateVaccineRecord(updatedRecord)
    }

    suspend fun getChildByUniqueId(uniqueId: String): Child? = withContext(Dispatchers.IO) {
        vaccineDao.getChildByUniqueId(uniqueId)
    }

    suspend fun getChildById(id: Int): Child? = withContext(Dispatchers.IO) {
        vaccineDao.getChildById(id)
    }

    suspend fun deleteChild(child: Child) = withContext(Dispatchers.IO) {
        vaccineDao.deleteChild(child)
    }

    suspend fun updateChild(child: Child) = withContext(Dispatchers.IO) {
        vaccineDao.updateChild(child)
    }

    suspend fun updateVaccineSideEffect(record: VaccineRecord, sideEffect: String?) = withContext(Dispatchers.IO) {
        val updatedRecord = record.copy(sideEffect = sideEffect)
        vaccineDao.updateVaccineRecord(updatedRecord)
    }

    suspend fun updateVaccineIgnoreStatus(record: VaccineRecord, isIgnored: Boolean) = withContext(Dispatchers.IO) {
        val updatedRecord = record.copy(isIgnored = isIgnored)
        vaccineDao.updateVaccineRecord(updatedRecord)
    }

    // Business Logic to check for all notifications/warnings
    data class VaccinationAlert(
        val child: Child,
        val vaccine: VaccineRecord,
        val status: VaccineStatus,
        val dueDate: String
    )

    suspend fun checkForAlertsSync(): List<VaccinationAlert> = withContext(Dispatchers.IO) {
        val alerts = mutableListOf<VaccinationAlert>()
        val pendingVaccines = vaccineDao.getAllPendingVaccinesSync()
        
        // Cache loaded children to avoid redundant queries
        val childMap = mutableMapOf<Int, Child>()
        val childRecordsMap = mutableMapOf<Int, List<VaccineRecord>>()

        for (record in pendingVaccines) {
            val child = childMap.getOrPut(record.childId) {
                vaccineDao.getChildById(record.childId) ?: continue
            }
            
            val allRecords = childRecordsMap.getOrPut(record.childId) {
                vaccineDao.getVaccinesForChildSync(record.childId)
            }
            
            val status = VaccineScheduler.getRecordStatus(child, record, allRecords)
            if (status == VaccineStatus.APPROACHING || status == VaccineStatus.OVERDUE) {
                val dueDate = VaccineScheduler.getRecordDueDate(child, record, allRecords)
                alerts.add(VaccinationAlert(child, record, status, dueDate))
            }
        }
        
        // Sort: Overdue first, then approaching by due dates
        alerts.sortedWith(compareBy<VaccinationAlert> { it.status != VaccineStatus.OVERDUE }.thenBy { it.dueDate })
    }

    fun getInventoryRecordsFlow(month: String, year: String): Flow<List<InventoryRecord>> {
        return vaccineDao.getInventoryRecordsFlow(month, year)
    }

    suspend fun getInventoryRecordsSync(month: String, year: String): List<InventoryRecord> = withContext(Dispatchers.IO) {
        vaccineDao.getInventoryRecordsSync(month, year)
    }

    suspend fun insertInventoryRecords(records: List<InventoryRecord>) = withContext(Dispatchers.IO) {
        vaccineDao.insertInventoryRecords(records)
    }

    suspend fun insertInventoryRecord(record: InventoryRecord) = withContext(Dispatchers.IO) {
        vaccineDao.insertInventoryRecord(record)
    }

    fun getAllInventoryYears(): Flow<List<String>> {
        return vaccineDao.getAllInventoryYears()
    }

    suspend fun getAllInventoryRecordsSync(): List<InventoryRecord> = withContext(Dispatchers.IO) {
        vaccineDao.getAllInventoryRecordsSync()
    }
}
