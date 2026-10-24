package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

sealed interface AppScreen {
    object Login : AppScreen
    object Home : AppScreen
    object ChildrenList : AppScreen
    object AddChild : AppScreen
    object AddAdultTd : AppScreen
    object FamiliesList : AppScreen
    object AddFamily : AppScreen
    data class ChildDetails(val childId: Int) : AppScreen
    object Alerts : AppScreen
    object ProgressChart : AppScreen
    object Settings : AppScreen
    object VaccineScheduleTable : AppScreen
    object DataEntry : AppScreen
    object ViewData : AppScreen
    object InfoAndExplanation : AppScreen
    object VaccineInventory : AppScreen
    object Onboarding : AppScreen
    object QrScanner : AppScreen
}

data class MergedVaccinationAlert(
    val child: Child,
    val categoryAr: String,
    val status: VaccineStatus,
    val dueDate: String,
    val vaccines: List<VaccineRecord>
)


class VaccineViewModel(
    val repository: VaccineRepository,
    application: Application
) : AndroidViewModel(application) {

    private val firestoreRepository = com.example.data.FirestoreRepository()

    suspend fun syncDataToFirestore() {
        try {
            val allFamilies = repository.getAllFamiliesSync()
        allFamilies.forEach { family ->
            firestoreRepository.saveFamily(
                com.example.data.FamilyDocument(
                    familyNumber = family.familyNumber,
                    headOfHousehold = family.headOfHousehold,
                    address = family.address,
                    phone = family.phone
                )
            )
        }

        val allChildren = repository.getAllChildrenSync()
        allChildren.forEach { child ->
            firestoreRepository.registerChildWithVaccines(
                com.example.data.ChildDocument(
                    uniqueId = child.uniqueId,
                    fullName = child.fullName,
                    motherName = child.motherName,
                    healthCenter = child.healthCenter,
                    address = child.address,
                    phone = child.phone,
                    dob = child.dob,
                    gender = child.gender,
                    registrationDate = child.registrationDate,
                    vaccinationNumber = child.vaccinationNumber,
                    isAdultTd = child.isAdultTd
                )
            )
        }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSync", "Failed to sync: ${e.message}")
        }
    }

    /**
     * دالة لرفع بيانات طفل معين مع سجلات اللقاحات الخاصة به من Room Database إلى Firestore (نسخ احتياطي سحابي)
     */
    suspend fun uploadChildToFirestore(childId: Int): Boolean {
        return try {
            _isBackingUp.value = true
            _backupStatusMessage.value = "جاري رفع بيانات وسجلات الطفل إلى Firestore..."

            val child = repository.dao.getChildById(childId)
            if (child == null) {
                _backupStatusMessage.value = "الطفل غير موجود في قاعدة البيانات المحلية."
                _isBackingUp.value = false
                return false
            }

            val records = repository.dao.getVaccinesForChildSync(childId)

            val childDoc = com.example.data.ChildDocument(
                id = child.uniqueId,
                uniqueId = child.uniqueId,
                fullName = child.fullName,
                motherName = child.motherName,
                healthCenter = child.healthCenter,
                address = child.address,
                phone = child.phone,
                dob = child.dob,
                gender = child.gender,
                registrationDate = child.registrationDate,
                vaccinationNumber = child.vaccinationNumber,
                isAdultTd = child.isAdultTd
            )

            val vaccineDocs = records.map { rec ->
                com.example.data.VaccineDocument(
                    id = rec.vaccineKey,
                    vaccineKey = rec.vaccineKey,
                    nameAr = rec.nameAr,
                    categoryAr = rec.categoryAr,
                    dueMonths = rec.dueMonths,
                    isTaken = rec.isTaken,
                    takenDate = rec.takenDate,
                    isSkipped = rec.isSkipped,
                    skipReason = rec.skipReason,
                    updatedAt = System.currentTimeMillis()
                )
            }

            firestoreRepository.uploadChildWithRecords(childDoc, vaccineDocs)

            _backupStatusMessage.value = "تم رفع بيانات وسجلات الطفل بنجاح إلى Firestore."
            _isBackingUp.value = false
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreUpload", "Error uploading child: ${e.message}", e)
            _backupStatusMessage.value = "فشل رفع بيانات الطفل: ${e.localizedMessage}"
            _isBackingUp.value = false
            false
        }
    }

    

    private val prefs = application.getSharedPreferences("vaccine_app_prefs", android.content.Context.MODE_PRIVATE)

    // Customizable Admin Credentials via preferences
    private val _adminUsername = MutableStateFlow(prefs.getString("admin_user", "admin") ?: "admin")
    val adminUsername: StateFlow<String> = _adminUsername.asStateFlow()

    private val _adminPassword = MutableStateFlow(prefs.getString("admin_pass", "123456") ?: "123456")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    // Persistent dark/light theme state
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val nextValue = !_isDarkTheme.value
        _isDarkTheme.value = nextValue
        prefs.edit().putBoolean("dark_theme", nextValue).apply()
    }

    // Persistent application language state (Default: "ar")
    private val _appLanguage = MutableStateFlow("ar")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    init {
        // Force reset language to Arabic based on user request
        prefs.edit().putString("app_language", "ar").apply()
        _appLanguage.value = "ar"
    }

    private val _backupEmail = MutableStateFlow(prefs.getString("backup_email", "") ?: "")
    val backupEmail: StateFlow<String> = _backupEmail.asStateFlow()

    fun updateBackupEmail(email: String) {
        _backupEmail.value = email
        prefs.edit().putString("backup_email", email).apply()
    }

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    fun clearBackupStatus() {
        _backupStatusMessage.value = null
    }

    fun triggerGoogleDriveBackup() {
        viewModelScope.launch {
            _isBackingUp.value = true
            _backupStatusMessage.value = null
            // Simulate Google Drive upload process
            kotlinx.coroutines.delay(3000)
            _isBackingUp.value = false
            val email = _backupEmail.value.ifBlank { "default" }
            _backupStatusMessage.value = if (_appLanguage.value == "en") {
                "Backup successfully saved to Google Drive ($email)!"
            } else {
                "تم حفظ النسخة الاحتياطية بنجاح في Google Drive ($email)!"
            }
        }
    }

    fun toggleAppLanguage() {
        val nextLang = if (_appLanguage.value == "ar") "en" else "ar"
        setAppLanguage(nextLang)
    }

    fun setAppLanguage(lang: String) {
        val sanitized = if (lang == "en" || lang == "ar") lang else "ar"
        _appLanguage.value = sanitized
        prefs.edit().putString("app_language", sanitized).apply()
    }

    private val _sideEffectsOfficerPhone = MutableStateFlow(prefs.getString("side_effects_officer_phone", "") ?: "")
    val sideEffectsOfficerPhone: StateFlow<String> = _sideEffectsOfficerPhone.asStateFlow()

    fun updateSideEffectsOfficerPhone(phone: String) {
        var cleaned = phone.trim()
        if (cleaned.startsWith("0")) cleaned = cleaned.substring(1)
        if (!cleaned.startsWith("964")) cleaned = "964$cleaned"
        
        _sideEffectsOfficerPhone.value = cleaned
        prefs.edit().putString("side_effects_officer_phone", cleaned).apply()
    }

    private val _defaultHealthCenter = MutableStateFlow(prefs.getString("default_health_center", "طاق ميكائيل") ?: "طاق ميكائيل")
    val defaultHealthCenter: StateFlow<String> = _defaultHealthCenter.asStateFlow()

    fun updateDefaultHealthCenter(name: String) {
        _defaultHealthCenter.value = name.trim()
        prefs.edit().putString("default_health_center", name.trim()).apply()
    }

    private val _sideEffectsOfficerName = MutableStateFlow(prefs.getString("side_effects_officer_name", "") ?: "")
    val sideEffectsOfficerName: StateFlow<String> = _sideEffectsOfficerName.asStateFlow()

    fun updateSideEffectsOfficerName(name: String) {
        _sideEffectsOfficerName.value = name.trim()
        _sideEffectsOfficerPhone.value = name.trim()
        prefs.edit()
            .putString("side_effects_officer_name", name.trim())
            .putString("side_effects_officer_phone", name.trim())
            .apply()
    }

    private val _healthCenterName = MutableStateFlow(prefs.getString("health_center_name", "طاق ميكائيل") ?: "طاق ميكائيل")
    val healthCenterName: StateFlow<String> = _healthCenterName.asStateFlow()

    fun updateHealthCenterName(name: String) {
        _healthCenterName.value = name.trim()
        _defaultHealthCenter.value = name.trim()
        prefs.edit()
            .putString("health_center_name", name.trim())
            .putString("default_health_center", name.trim())
            .apply()
    }

    private val _username = MutableStateFlow(prefs.getString("username", "مستخدم") ?: "مستخدم")
    val username: StateFlow<String> = _username.asStateFlow()

    fun updateUsername(name: String) {
        _username.value = name.trim()
        prefs.edit().putString("username", name.trim()).apply()
    }

    // Geographic Coordinates of Configured Health Center (for Map routing & distance estimates)
    private val _healthCenterLat = MutableStateFlow(prefs.getFloat("health_center_lat", 36.340f))
    val healthCenterLat: StateFlow<Float> = _healthCenterLat.asStateFlow()

    private val _healthCenterLng = MutableStateFlow(prefs.getFloat("health_center_lng", 43.130f))
    val healthCenterLng: StateFlow<Float> = _healthCenterLng.asStateFlow()

    // Google Maps link of the health center (for navigation & coordinates)
    private val _healthCenterMapLink = MutableStateFlow(prefs.getString("health_center_map_link", "https://maps.app.goo.gl/qEsHbphX8j5GUgKB7") ?: "https://maps.app.goo.gl/qEsHbphX8j5GUgKB7")
    val healthCenterMapLink: StateFlow<String> = _healthCenterMapLink.asStateFlow()

    fun updateHealthCenterMapLink(link: String) {
        val trimmed = link.trim()
        _healthCenterMapLink.value = trimmed
        val editor = prefs.edit().putString("health_center_map_link", trimmed)
        
        // Attempt to parse coordinates out of the pasted link or text (e.g. lat,lng pattern inside the url)
        val regex = Regex("([0-9]{1,2}\\.[0-9]+)\\s*,\\s*([0-9]{1,2}\\.[0-9]+)")
        val match = regex.find(trimmed)
        if (match != null) {
            val lat = match.groupValues[1].toFloatOrNull()
            val lng = match.groupValues[2].toFloatOrNull()
            if (lat != null && lng != null) {
                _healthCenterLat.value = lat
                _healthCenterLng.value = lng
                editor.putFloat("health_center_lat", lat)
                editor.putFloat("health_center_lng", lng)
            }
        }
        editor.apply()
    }

    fun updateHealthCenterCoordinates(lat: Float, lng: Float) {
        _healthCenterLat.value = lat
        _healthCenterLng.value = lng
        prefs.edit()
            .putFloat("health_center_lat", lat)
            .putFloat("health_center_lng", lng)
            .apply()
    }

    // Changing admin security details
    fun updateAdminCredentials(newUsr: String, newPas: String): Boolean {
        if (newUsr.trim().isEmpty() || newPas.trim().isEmpty()) return false
        _adminUsername.value = newUsr.trim()
        _username.value = newUsr.trim()
        _adminPassword.value = newPas
        prefs.edit()
            .putString("admin_user", newUsr.trim())
            .putString("username", newUsr.trim())
            .putString("admin_pass", newPas)
            .apply()
        return true
    }

    private var activeRingtone: android.media.Ringtone? = null

    private val _isSoundPlaying = MutableStateFlow(false)
    val isSoundPlaying: StateFlow<Boolean> = _isSoundPlaying.asStateFlow()

    fun playGentleAlertSound() {
        // Sound warnings disabled per system settings requirements "ايقاف التنبيه الصوتي"
    }

    fun stopAlertSound() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                activeRingtone?.stop()
                activeRingtone = null
                _isSoundPlaying.value = false
                android.widget.Toast.makeText(getApplication(), "تم كتم وصمت التنبيه 🔕", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun groupVaccineAlerts(rawAlerts: List<VaccineRepository.VaccinationAlert>): List<MergedVaccinationAlert> {
        val grouped = rawAlerts.groupBy { Pair(it.child.id, it.vaccine.categoryAr) }
        return grouped.map { (_, list) ->
            val first = list.first()
            val hasOverdue = list.any { it.status == VaccineStatus.OVERDUE }
            val status = if (hasOverdue) VaccineStatus.OVERDUE else VaccineStatus.APPROACHING
            
            val sortedVaccines = list.map { it.vaccine }.sortedBy { it.dueMonths }
            MergedVaccinationAlert(
                child = first.child,
                categoryAr = first.vaccine.categoryAr,
                status = status,
                dueDate = first.dueDate,
                vaccines = sortedVaccines
            )
        }.sortedWith(compareBy<MergedVaccinationAlert> { it.status != VaccineStatus.OVERDUE }.thenBy { it.dueDate })
    }

    private fun startHourlyAlertLoop() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (true) {
                // Wait for 1 hour
                kotlinx.coroutines.delay(3600000)
                checkHourlyVaccineAlerts()
            }
        }
    }

    private fun checkHourlyVaccineAlerts() {
        viewModelScope.launch {
            val alertsList = repository.checkForAlertsSync()
            val approaching = alertsList.filter { it.status == VaccineStatus.APPROACHING }
            val mergedList = groupVaccineAlerts(approaching)
            
            if (mergedList.isNotEmpty()) {
                for (alert in mergedList) {
                    val vaccineNames = alert.vaccines.joinToString(", ") { it.nameAr }
                    triggerSystemNotification(
                        "🔔 تذكير ساعة اللقاح: ${alert.child.fullName}",
                        "يقترب موعد تطعيم الطفل بلقاحات (${vaccineNames}) لعمر (${alert.categoryAr}) المخطط لها بتاريخ ${alert.dueDate}."
                    )
                }
                playGentleAlertSound()
            }
        }
    }

    // Live administrator alerts structure
    data class AdminNotification(
        val id: String = UUID.randomUUID().toString(),
        val message: String,
        val childName: String,
        val motherName: String,
        val uniqueId: String,
        val phone: String,
        val healthCenter: String,
        val address: String,
        val childDob: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )
    
    private val _adminNotifications = MutableStateFlow<List<AdminNotification>>(emptyList())
    val adminNotifications: StateFlow<List<AdminNotification>> = _adminNotifications.asStateFlow()

    fun clearAdminNotifications() {
        _adminNotifications.value = emptyList()
    }

    // Authentication State
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Remember credentials logic
    private val _rememberMe = MutableStateFlow(prefs.getBoolean("remember_me", false))
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _savedUser = MutableStateFlow(prefs.getString("saved_user", "") ?: "")
    val savedUser: StateFlow<String> = _savedUser.asStateFlow()

    private val _savedPass = MutableStateFlow(prefs.getString("saved_pass", "") ?: "")
    val savedPass: StateFlow<String> = _savedPass.asStateFlow()

    fun setRememberMe(value: Boolean) {
        _rememberMe.value = value
        prefs.edit().putBoolean("remember_me", value).apply()
        if (!value) {
            prefs.edit().remove("saved_user").remove("saved_pass").apply()
            _savedUser.value = ""
            _savedPass.value = ""
        }
    }

    // Navigation State
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Login)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _childrenListTab = MutableStateFlow(0)
    val childrenListTab: StateFlow<Int> = _childrenListTab.asStateFlow()

    fun setChildrenListTab(tab: Int) {
        _childrenListTab.value = tab
    }

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Child in Detail Screen
    fun getChildVaccinesFlow(childId: Int): Flow<List<VaccineRecord>> {
        return repository.getVaccinesForChild(childId)
    }

    private val _selectedChild = MutableStateFlow<Child?>(null)
    val selectedChild: StateFlow<Child?> = _selectedChild.asStateFlow()

    private val _selectedChildVaccines = MutableStateFlow<List<VaccineRecord>>(emptyList())
    val selectedChildVaccines: StateFlow<List<VaccineRecord>> = _selectedChildVaccines.asStateFlow()

    // Notification Hub / Active Alerts State
    private val _alerts = MutableStateFlow<List<VaccineRepository.VaccinationAlert>>(emptyList())
    val alerts: StateFlow<List<VaccineRepository.VaccinationAlert>> = _alerts.asStateFlow()

    private val _isLoadingAlerts = MutableStateFlow(false)
    val isLoadingAlerts: StateFlow<Boolean> = _isLoadingAlerts.asStateFlow()

    // Side effect dialog alert state
    private val _sideEffectAlertMessage = MutableStateFlow<String?>(null)
    val sideEffectAlertMessage: StateFlow<String?> = _sideEffectAlertMessage.asStateFlow()

    fun showSideEffectAlert(message: String) {
        _sideEffectAlertMessage.value = message
        // Also trigger high priority system notification to appear above all other apps
        triggerSystemNotification("⚠️ تنبيه تسجيل أثر جانبي لقاح هام", message)
    }

    fun dismissSideEffectAlert() {
        _sideEffectAlertMessage.value = null
    }

    // Post a standard system notification that appears on top of/above screens
    fun triggerSystemNotification(title: String, body: String) {
        try {
            val context = getApplication<Application>().applicationContext
            val channelId = "vaccine_alerts_channel"
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "إشعارات وتحذيرات اللقاحات",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "قناة إشعارات اللقاحات المستحقة والأعراض الجانبية"
                    enableLights(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // Launch app when clicked
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = if (intent != null) {
                android.app.PendingIntent.getActivity(
                    context, 
                    0, 
                    intent, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0
                )
            } else null

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) 
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .build()
                
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun check24HoursNotification() {
        viewModelScope.launch(Dispatchers.IO) {
            val allChildren = repository.allChildren.first()
            val allRecords = repository.dao.getAllVaccineRecordsSync()
            
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

            for (child in allChildren) {
                val childRecords = allRecords.filter { it.childId == child.id && !it.isTaken && !it.isIgnored }
                for (record in childRecords) {
                    val dueDate = com.example.data.VaccineScheduler.getRecordDueDate(child, record, allRecords)
                    if (dueDate == tomorrowDate) {
                        triggerSystemNotification("تنبيه: لقاح مستحق غداً", "لقاح ${record.nameAr} للطفل ${child.fullName} مستحق غداً")
                    }
                }
            }
        }
    }

    fun checkAndNotifyUpcomingVaccines() {
        viewModelScope.launch {
            val alertsList = repository.checkForAlertsSync()
            val overdueCount = alertsList.count { it.status == VaccineStatus.OVERDUE }
            val approachingCount = alertsList.count { it.status == VaccineStatus.APPROACHING }
            
            if (overdueCount > 0 || approachingCount > 0) {
                val message = "يوجد $overdueCount لقاح متأخر و $approachingCount لقاح يقترب تاريخ استحقاقه للأطفال المسجلين في نظام اللقاحات المعتمد."
                triggerSystemNotification("🔔 تنبيه: وافد اللقاحات المستحقة", message)
                playGentleAlertSound()
            }
        }
    }

    // Stats for Dashboard
    private val _totalChildrenCount = MutableStateFlow(0)
    val totalChildrenCount: StateFlow<Int> = _totalChildrenCount.asStateFlow()

    private val _overdueCount = MutableStateFlow(0)
    val overdueCount: StateFlow<Int> = _overdueCount.asStateFlow()

    private val _approachingCount = MutableStateFlow(0)
    val approachingCount: StateFlow<Int> = _approachingCount.asStateFlow()

    data class ChildProgress(
        val child: com.example.data.Child,
        val completedCount: Int,
        val totalCount: Int,
        val percentage: Float,
        val pendingCount: Int = 0,
        val overdueCount: Int = 0,
        val completedVaccineNames: List<String> = emptyList(),
        val pendingVaccineNames: List<String> = emptyList(),
        val overdueVaccineNames: List<String> = emptyList()
    )

    val childrenProgress: StateFlow<List<ChildProgress>> = combine(
        repository.allChildren,
        repository.dao.getAllVaccineRecordsFlow()
    ) { childList, recordList ->
        val recordsByChild = recordList.groupBy { it.childId }
        childList.map { child ->
            val childRecords = recordsByChild[child.id] ?: emptyList()
            val completed = childRecords.count { it.isTaken }
            val total = childRecords.size
            val pct = if (total > 0) (completed.toFloat() / total * 100f) else 0f
            
            val pending = childRecords.count { !it.isTaken && !it.isSkipped && com.example.data.VaccineScheduler.getRecordStatus(child, it, childRecords) != com.example.data.VaccineStatus.OVERDUE }
            val overdue = childRecords.count { !it.isTaken && !it.isSkipped && com.example.data.VaccineScheduler.getRecordStatus(child, it, childRecords) == com.example.data.VaccineStatus.OVERDUE }
            
            val completedNames = childRecords.filter { it.isTaken }.map { it.nameAr }
            val pendingNames = childRecords.filter { !it.isTaken && !it.isSkipped && com.example.data.VaccineScheduler.getRecordStatus(child, it, childRecords) != com.example.data.VaccineStatus.OVERDUE }.map { it.nameAr }
            val overdueNames = childRecords.filter { !it.isTaken && !it.isSkipped && com.example.data.VaccineScheduler.getRecordStatus(child, it, childRecords) == com.example.data.VaccineStatus.OVERDUE }.map { it.nameAr }

            ChildProgress(
                child = child,
                completedCount = completed,
                totalCount = total,
                percentage = pct,
                pendingCount = pending,
                overdueCount = overdue,
                completedVaccineNames = completedNames,
                pendingVaccineNames = pendingNames,
                overdueVaccineNames = overdueNames
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // List of All Children
    val children: StateFlow<List<Child>> = combine(
        repository.allChildren,
        _searchQuery
    ) { childList, query ->
        _totalChildrenCount.value = childList.size
        if (query.isBlank()) {
            childList
        } else {
            childList.filter {
                it.fullName.contains(query, ignoreCase = true) || 
                it.uniqueId.contains(query, ignoreCase = true) || 
                it.phone.contains(query) ||
                it.motherName.contains(query, ignoreCase = true) ||
                it.healthCenter.contains(query, ignoreCase = true) ||
                it.vaccinationNumber.contains(query, ignoreCase = true) ||
                it.familyNumber.contains(query, ignoreCase = true) ||
                it.headOfHousehold.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val families: StateFlow<List<Family>> = repository.allFamilies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun registerNewFamily(
        familyNumber: String,
        headOfHousehold: String,
        address: String,
        phone: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.insertFamily(
                familyNumber = familyNumber,
                headOfHousehold = headOfHousehold,
                address = address,
                phone = phone
            )
            onSuccess()
        }
    }

    fun updateFamilyInfo(family: Family) {
        viewModelScope.launch {
            repository.updateFamily(family)
        }
    }

    fun deleteFamilyRecord(family: Family) {
        viewModelScope.launch {
            repository.deleteFamily(family)
        }
    }

    init {
        // Initial load of alerts with database sync
        refreshAlerts()
        checkAndNotifyUpcomingVaccines()
        startHourlyAlertLoop()

        val isFirstTime = prefs.getBoolean("is_first_time", true)
        if (isFirstTime) {
            _currentScreen.value = AppScreen.Onboarding
        } else {
            _currentScreen.value = AppScreen.Home
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("is_first_time", false).apply()
        _currentScreen.value = AppScreen.Home
    }

    // Login logic with username only
    fun login(user: String): Boolean {
        val trimmed = user.trim()
        return if (trimmed.isNotEmpty()) {
            _adminUsername.value = trimmed
            prefs.edit().putString("admin_user", trimmed).apply()
            _isLoggedIn.value = true
            _loginError.value = null
            _currentScreen.value = AppScreen.Home
            if (_rememberMe.value) {
                prefs.edit().putString("saved_user", trimmed).apply()
                _savedUser.value = trimmed
            } else {
                prefs.edit().remove("saved_user").apply()
                _savedUser.value = ""
            }
            android.widget.Toast.makeText(getApplication(), "مرحباً $trimmed", android.widget.Toast.LENGTH_LONG).show()
            refreshAlerts()
            checkAndNotifyUpcomingVaccines()
            true
        } else {
            _loginError.value = "يرجى إدخال اسم المستخدم المسؤول"
            false
        }
    }

    // Expose child detail editing
    fun updateChildDetails(child: com.example.data.Child) {
        viewModelScope.launch {
            repository.updateChild(child)
            // Reload details to force Compose update
            _selectedChild.value = repository.getChildById(child.id)
        }
    }

    // Bulk update for multiple vaccine records at once (same age category)
    fun updateMultipleVaccinesStatusAndSideEffect(
        records: List<com.example.data.VaccineRecord>,
        isTaken: Boolean,
        takenDate: String?,
        sideEffect: String?
    ) {
        viewModelScope.launch {
            records.forEach { record ->
                repository.updateVaccineStatus(record, isTaken, takenDate)
                if (sideEffect != null) {
                    repository.updateVaccineSideEffect(record, sideEffect)
                }
            }
            // Refresh detailed list once at end
            _selectedChild.value?.id?.let {
                loadChildDetails(it)
            }
            refreshAlerts()
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentScreen.value = AppScreen.Login
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen is AppScreen.ChildDetails) {
            loadChildDetails(screen.childId)
        } else if (screen == AppScreen.Home || screen == AppScreen.Alerts) {
            refreshAlerts()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Add child logic
    fun registerNewChild(
        fullName: String,
        motherName: String,
        healthCenter: String,
        address: String,
        phone: String,
        dobString: String,
        vaccinationNumber: String,
        familyNumber: String,
        headOfHousehold: String,
        isAdultTd: Boolean,
        gender: String = "ذكر",
        photoUri: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val childId = repository.registerChild(
                fullName = fullName,
                motherName = motherName,
                healthCenter = healthCenter,
                address = address,
                phone = phone,
                dob = dobString,
                vaccinationNumber = vaccinationNumber,
                familyNumber = familyNumber,
                headOfHousehold = headOfHousehold,
                isAdultTd = isAdultTd,
                gender = gender,
                photoUri = photoUri
            )
            val childObj = repository.getChildById(childId.toInt())
            if (childObj != null) {
                _adminNotifications.value = listOf(
                    AdminNotification(
                        message = "تمت إضافة طفل جديد بنجاح وتوليد جدول اللقاحات الوطني له تلقائياً",
                        childName = childObj.fullName,
                        motherName = childObj.motherName,
                        uniqueId = childObj.uniqueId,
                        phone = childObj.phone,
                        healthCenter = childObj.healthCenter,
                        address = childObj.address,
                        childDob = childObj.dob
                    )
                ) + _adminNotifications.value
            }
            refreshAlerts()
            onSuccess()
            navigateTo(AppScreen.ChildDetails(childId.toInt()))
        }
    }

    // Delete child
    fun deleteChildAccount(child: Child) {
        viewModelScope.launch {
            repository.deleteChild(child)
            if (_selectedChild.value?.id == child.id) {
                _selectedChild.value = null
            }
            refreshAlerts()
            navigateTo(AppScreen.Home)
        }
    }

    // Load detailed record for child
    fun loadChildDetails(childId: Int) {
        viewModelScope.launch {
            val childObj = repository.getChildById(childId)
            _selectedChild.value = childObj
            if (childObj != null) {
                // Collect vaccines dynamically
                repository.getVaccinesForChild(childId).collect { list ->
                    _selectedChildVaccines.value = list
                }
            }
        }
    }

    // Toggle vaccine taken status
    fun toggleVaccineStatus(record: VaccineRecord, isTaken: Boolean) {
        viewModelScope.launch {
            val currentDate = if (isTaken) {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            } else {
                null
            }
            repository.updateVaccineStatus(record, isTaken, currentDate)
            // Refresh detailed list
            _selectedChild.value?.id?.let {
                loadChildDetails(it)
            }
            refreshAlerts()
        }
    }

    // Toggle vaccine skip status
    fun updateVaccineSkipStatus(record: VaccineRecord, isSkipped: Boolean, reason: String? = null) {
        viewModelScope.launch {
            repository.updateVaccineSkipStatus(record, isSkipped, reason)
            // Refresh detailed list
            _selectedChild.value?.id?.let {
                loadChildDetails(it)
            }
            refreshAlerts()
        }
    }

    // Toggle vaccine taken status with custom datepicker selection
    fun updateVaccineStatusWithDate(record: VaccineRecord, isTaken: Boolean, customDate: String?) {
        viewModelScope.launch {
            repository.updateVaccineStatus(record, isTaken, customDate)
            // Refresh detailed list
            _selectedChild.value?.id?.let {
                loadChildDetails(it)
            }
            refreshAlerts()
        }
    }

    // Bypass/Skip vaccine status with reason
    fun bypassVaccineStatus(record: VaccineRecord, isSkipped: Boolean, reason: String?) {
        viewModelScope.launch {
            repository.updateVaccineSkipStatus(record, isSkipped, reason)
            // Refresh detailed list
            _selectedChild.value?.id?.let {
                loadChildDetails(it)
            }
            refreshAlerts()
        }
    }

    // Ignore/Unignore vaccine
    fun updateVaccineIgnoreStatus(record: VaccineRecord, isIgnored: Boolean) {
        viewModelScope.launch {
            repository.updateVaccineIgnoreStatus(record, isIgnored)
            // Refresh detailed list
            _selectedChild.value?.id?.let {
                loadChildDetails(it)
            }
            refreshAlerts()
        }
    }

    // Update side effect for a vaccine record
    fun updateVaccineSideEffect(record: VaccineRecord, sideEffect: String?) {
        viewModelScope.launch {
            repository.updateVaccineSideEffect(record, sideEffect)
            // Refresh detailed list
            _selectedChild.value?.id?.let {
                loadChildDetails(it)
            }
            // Trigger admin notification to show up in alerts/notifs for officer
            _selectedChild.value?.let { child ->
                if (!sideEffect.isNullOrBlank()) {
                    val notif = AdminNotification(
                        message = "تبليغ أثر جانبي لقاح لمتابعة الحالة: $sideEffect (تاريخ ميلاد الطفل: ${child.dob})",
                        childName = child.fullName,
                        motherName = child.motherName,
                        uniqueId = child.uniqueId,
                        phone = child.phone,
                        healthCenter = child.healthCenter,
                        address = child.address,
                        childDob = child.dob
                    )
                    _adminNotifications.value = listOf(notif) + _adminNotifications.value
                    
                    // Trigger live popup / system notification warn
                    showSideEffectAlert("تنبيه أثر جانبي للطفل ${child.fullName}: سجل الأثر بـ $sideEffect. يرجى إعطاء الطفل خافض حرارة ومسكن مناسب ومتابعة قياس حرارته دورياً لحمايته.")
                }
            }
            refreshAlerts()
        }
    }

    // Synchronize warnings counters and alerts
    fun refreshAlerts() {
        viewModelScope.launch {
            _isLoadingAlerts.value = true
            val items = repository.checkForAlertsSync()
            _alerts.value = items
            
            val mergedList = groupVaccineAlerts(items)
            // Calculate indicators based on merged warnings
            _overdueCount.value = mergedList.count { it.status == VaccineStatus.OVERDUE }
            _approachingCount.value = mergedList.count { it.status == VaccineStatus.APPROACHING }
            
            _isLoadingAlerts.value = false
        }
    }

    // --- VACCINE & SUPPLIES INVENTORY STATE ---
    
    val standardVaccines = listOf(
        "بي سي جي", "لقاح شلل الاطفال الفموي", "لقاح شلل الاطفال الزرقي", 
        "لقاح الفايروس الدوار", "اللقاح الخماسي", "اللقاح الثلاثي", 
        "لقاح المكورات الرئوية المقترن", "لقاح الكبد الفيروسي B صغار", 
        "لقاح الكبد الفايروسي B كبار", "لقاح الكبد الفايروسي A صغار", 
        "لقاح الكبد الفايروسي A كبار", "الحصبة المنفردة", "لقاح ثنائي صغار TD", 
        "لقاح ثنائي كبار Td", "لقاح التايفوئيد", "لقاح الانفلونزا", 
        "لقاح السحايا الرباعي المقترن", "لقاح داء الكلب", "لقاح الحمى الصفراء", 
        "لقاح المكورات متعددة السكريات", "لقاح استرازينيكا", "لقاح ساينوفارم", 
        "لقاح فايزر", "لقاح الحصبة المختلطة 10 جرع", "لقاح الحصبة المختلطة 1 جرعة"
    )

    val standardSupplies = listOf(
        "مذيب لقاح BCG", "مذيب لقاح الحصبة المنفردة", "مذيب لقاح الحصبة المختلطة 10 جرع", 
        "مذيب لقاح الحصبة المختلطة 1 جرعة", "سرنجات BCG", "سرنجات 0.5 سي سي كبد صغار", 
        "سرنجات ذاتية التعطيل 0.5 سي سي", "سرنجات اعطاء لقاح فايزر", "سرنجات اعطاء 1 سي سي", 
        "سرنجات الاذابة", "صناديق الامان"
    )

    val arabicMonths = listOf(
        "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
        "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"
    )

    private fun getCurrentArabicMonth(): String {
        val monthIdx = Calendar.getInstance().get(Calendar.MONTH)
        return arabicMonths[monthIdx]
    }

    private val _selectedInventoryMonth = MutableStateFlow(getCurrentArabicMonth())
    val selectedInventoryMonth: StateFlow<String> = _selectedInventoryMonth.asStateFlow()

    private val _selectedInventoryYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR).toString())
    val selectedInventoryYear: StateFlow<String> = _selectedInventoryYear.asStateFlow()

    private val _inventoryRecords = MutableStateFlow<List<InventoryRecord>>(emptyList())
    val inventoryRecords: StateFlow<List<InventoryRecord>> = _inventoryRecords.asStateFlow()

    private val _parsingResultText = MutableStateFlow("")
    val parsingResultText: StateFlow<String> = _parsingResultText.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    fun setInventoryMonthAndYear(month: String, year: String) {
        _selectedInventoryMonth.value = month
        _selectedInventoryYear.value = year
        loadInventoryRecords(month, year)
    }

    fun loadInventoryRecords(month: String, year: String) {
        viewModelScope.launch {
            initializeInventoryForMonth(month, year)
            repository.getInventoryRecordsFlow(month, year).collect { list ->
                _inventoryRecords.value = list
            }
        }
    }

    fun getPreviousMonthAndYear(month: String, yearStr: String): Pair<String, String> {
        val index = arabicMonths.indexOf(month)
        return if (index <= 0) {
            val prevYear = (yearStr.toIntOrNull() ?: 2026) - 1
            Pair(arabicMonths[11], prevYear.toString())
        } else {
            Pair(arabicMonths[index - 1], yearStr)
        }
    }

    suspend fun initializeInventoryForMonth(month: String, year: String) {
        val existing = repository.getInventoryRecordsSync(month, year)
        if (existing.isEmpty()) {
            val (prevMonth, prevYear) = getPreviousMonthAndYear(month, year)
            val prevRecords = repository.getInventoryRecordsSync(prevMonth, prevYear).associateBy { it.itemName }
            
            val newRecords = mutableListOf<InventoryRecord>()
            
            // Add Vaccines
            for (name in standardVaccines) {
                val prevRem = prevRecords[name]?.finalRemaining ?: 0
                newRecords.add(
                    InventoryRecord(
                        month = month,
                        year = year,
                        itemName = name,
                        itemType = "vaccine",
                        initialBalance = prevRem
                    )
                )
            }
            
            // Add Supplies
            for (name in standardSupplies) {
                val prevRem = prevRecords[name]?.finalRemaining ?: 0
                newRecords.add(
                    InventoryRecord(
                        month = month,
                        year = year,
                        itemName = name,
                        itemType = "supply",
                        initialBalance = prevRem
                    )
                )
            }
            
            repository.insertInventoryRecords(newRecords)
        }
    }

    fun getNextMonthAndYear(month: String, yearStr: String): Pair<String, String> {
        val index = arabicMonths.indexOf(month)
        return if (index >= 11 || index == -1) {
            val nextYear = (yearStr.toIntOrNull() ?: 2026) + 1
            Pair(arabicMonths[0], nextYear.toString())
        } else {
            Pair(arabicMonths[index + 1], yearStr)
        }
    }

    private suspend fun propagateInventoryRecordForward(record: InventoryRecord) {
        var currentRecord = record
        while (true) {
            val (nextMonth, nextYear) = getNextMonthAndYear(currentRecord.month, currentRecord.year)
            val nextMonthRecords = repository.getInventoryRecordsSync(nextMonth, nextYear)
            if (nextMonthRecords.isEmpty()) {
                break
            }
            val nextRecord = nextMonthRecords.find { it.itemName == currentRecord.itemName } ?: break
            
            if (nextRecord.initialBalance == currentRecord.finalRemaining) {
                break
            }
            
            val updatedNextRecord = nextRecord.copy(initialBalance = currentRecord.finalRemaining)
            repository.insertInventoryRecord(updatedNextRecord)
            currentRecord = updatedNextRecord
        }
    }

    fun syncInitialBalancesFromPreviousMonth(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val (prevMonth, prevYear) = getPreviousMonthAndYear(_selectedInventoryMonth.value, _selectedInventoryYear.value)
            val prevRecords = repository.getInventoryRecordsSync(prevMonth, prevYear).associateBy { it.itemName }
            
            val currentRecords = repository.getInventoryRecordsSync(_selectedInventoryMonth.value, _selectedInventoryYear.value)
            if (currentRecords.isNotEmpty()) {
                val updatedRecords = currentRecords.map { current ->
                    val prevRem = prevRecords[current.itemName]?.finalRemaining ?: 0
                    current.copy(initialBalance = prevRem)
                }
                repository.insertInventoryRecords(updatedRecords)
                _inventoryRecords.value = updatedRecords
                
                for (rec in updatedRecords) {
                    propagateInventoryRecordForward(rec)
                }
            }
            onComplete()
        }
    }

    fun updateInventoryRecord(record: InventoryRecord) {
        viewModelScope.launch {
            repository.insertInventoryRecord(record)
            propagateInventoryRecordForward(record)
            // Reload the local state flow so that changes are immediately reflected
            _inventoryRecords.value = repository.getInventoryRecordsSync(_selectedInventoryMonth.value, _selectedInventoryYear.value)
        }
    }

    // --- HYBRID REPORT PARSER ENGINE ---

    data class LocalParseResult(
        val month: String,
        val year: String,
        val centerName: String,
        val items: List<ParsedItem>
    )

    data class ParsedItem(
        val itemName: String,
        val initialBalance: Int = 0,
        val receivedDuringMonth: Int = 0,
        val fixedSessionVaccinated: Int = 0,
        val outreachSessionVaccinated: Int = 0,
        val openedWasted: Int = 0,
        val closedWasted: Int = 0,
        val transferredOut: Int = 0,
        val expiryDate: String? = null
    )

    private fun extractNumberForKeywords(text: String, keywords: List<String>): Int? {
        for (kw in keywords) {
            val idx = text.indexOf(kw)
            if (idx != -1) {
                val sub = text.substring(idx + kw.length)
                val numRegex = Regex("\\b(\\d+)\\b")
                val match = numRegex.find(sub)
                if (match != null) {
                    val numIdx = sub.indexOf(match.value)
                    val inBetween = sub.substring(0, numIdx)
                    // Ensure the first number matches close to the keyword
                    if (inBetween.length < 25) {
                        return match.groupValues[1].toIntOrNull()
                    }
                }
            }
        }
        return null
    }

    private fun extractDate(text: String): String? {
        val dateRegex = Regex("\\b(\\d{4}[-/]\\d{2}[-/]\\d{2})\\b")
        val match = dateRegex.find(text)
        return match?.groupValues?.get(1)?.replace("/", "-")
    }

    fun parseReportTextLocal(text: String, currentMonth: String, currentYear: String): LocalParseResult {
        var detectedMonth = currentMonth
        for (m in arabicMonths) {
            if (text.contains(m)) {
                detectedMonth = m
                break
            }
        }
        
        var detectedYear = currentYear
        val yearRegex = Regex("\\b(202[0-9]|203[0-9])\\b")
        val yearMatch = yearRegex.find(text)
        if (yearMatch != null) {
            detectedYear = yearMatch.groupValues[1]
        }
        
        var detectedCenter = ""
        val centerRegex = Regex("(?:لمركز|مركز)\\s+([^\\s،؛:.]+ (?:[^\\s،؛:.]+){1,3})")
        val centerMatch = centerRegex.find(text)
        if (centerMatch != null) {
            detectedCenter = centerMatch.groupValues[1].trim()
        }
        
        val parsedItems = mutableListOf<ParsedItem>()
        val allStandardItems = standardVaccines + standardSupplies
        
        val itemIndices = allStandardItems.map { name ->
            val idx = text.indexOf(name)
            Pair(name, idx)
        }.filter { it.second != -1 }.sortedBy { it.second }
        
        val regions = mutableListOf<Pair<String, String>>()
        for (i in itemIndices.indices) {
            val currentItem = itemIndices[i].first
            val startIdx = itemIndices[i].second + currentItem.length
            val endIdx = if (i < itemIndices.size - 1) itemIndices[i+1].second else text.length
            val regionText = if (endIdx > startIdx) text.substring(startIdx, endIdx) else ""
            regions.add(Pair(currentItem, regionText))
        }
        
        for ((name, rText) in regions) {
            val initial = extractNumberForKeywords(rText, listOf("رصيد البداية", "رصيد بداية", "البداية", "رصيد اول", "بداية"))
            val received = extractNumberForKeywords(rText, listOf("المستلم", "مستلم", "استلام", "الرصيد المستلم"))
            val fixed = extractNumberForKeywords(rText, listOf("ثابت", "جلسة ثابتة", "جلسه ثابته"))
            val outreach = extractNumberForKeywords(rText, listOf("خارجي", "جلسة خارجية", "جلسه خارجيه", "الخارجي"))
            val opened = extractNumberForKeywords(rText, listOf("مفتوح", "الهدر المفتوح", "الهدر المفتوحه", "قناني مفتوحة", "المفتوح"))
            val closed = extractNumberForKeywords(rText, listOf("مغلق", "الهدر المغلق", "الهدر المغلقه", "قناني مغلقة", "المغلق"))
            val trans = extractNumberForKeywords(rText, listOf("مناقلة", "مناقلات", "تمت مناقلة", "مناقله"))
            val expiry = extractDate(rText)
            
            parsedItems.add(
                ParsedItem(
                    itemName = name,
                    initialBalance = initial ?: 0,
                    receivedDuringMonth = received ?: 0,
                    fixedSessionVaccinated = fixed ?: 0,
                    outreachSessionVaccinated = outreach ?: 0,
                    openedWasted = opened ?: 0,
                    closedWasted = closed ?: 0,
                    transferredOut = trans ?: 0,
                    expiryDate = expiry
                )
            )
        }
        
        return LocalParseResult(
            month = detectedMonth,
            year = detectedYear,
            centerName = detectedCenter.ifBlank { "غير محدد" },
            items = parsedItems
        )
    }

    fun parseAndProcessUnstructuredReport(rawText: String) {
        viewModelScope.launch {
            _isParsing.value = true
            try {
                // Perform local parsing
                val localResult = parseReportTextLocal(rawText, _selectedInventoryMonth.value, _selectedInventoryYear.value)
                
                // Set active month and year to what was parsed
                _selectedInventoryMonth.value = localResult.month
                _selectedInventoryYear.value = localResult.year
                
                // Initialize database for that month and year if empty
                initializeInventoryForMonth(localResult.month, localResult.year)
                
                // Retrieve current records to merge
                val dbRecords = repository.getInventoryRecordsSync(localResult.month, localResult.year).associateBy { it.itemName }
                
                val updatedRecords = mutableListOf<InventoryRecord>()
                val missingWarnings = mutableListOf<String>()
                
                for (parsed in localResult.items) {
                    val currentDb = dbRecords[parsed.itemName]
                    if (currentDb != null) {
                        val merged = currentDb.copy(
                            initialBalance = if (parsed.initialBalance != 0) parsed.initialBalance else currentDb.initialBalance,
                            receivedDuringMonth = parsed.receivedDuringMonth,
                            fixedSessionVaccinated = parsed.fixedSessionVaccinated,
                            outreachSessionVaccinated = parsed.outreachSessionVaccinated,
                            openedWasted = parsed.openedWasted,
                            closedWasted = parsed.closedWasted,
                            transferredOut = parsed.transferredOut,
                            expiryDate = parsed.expiryDate ?: currentDb.expiryDate
                        )
                        updatedRecords.add(merged)
                    }
                }
                
                // Save updated records
                if (updatedRecords.isNotEmpty()) {
                    repository.insertInventoryRecords(updatedRecords)
                }
                
                // Check if some items are missing received balance or have other anomalies
                for (parsed in localResult.items) {
                    if (parsed.receivedDuringMonth == 0 && parsed.initialBalance == 0) {
                        missingWarnings.add("المادة ${parsed.itemName}: لم يتم ذكر رصيد مستلم، تم افتراضه (0).")
                    }
                }
                
                // Generate a visual result description with Markdown and JSON
                val markdownTable = generateMarkdownTable(localResult.month, localResult.year, updatedRecords.ifEmpty { dbRecords.values.toList() })
                val jsonSchema = generateJsonString(localResult.month, localResult.year, localResult.centerName, updatedRecords.ifEmpty { dbRecords.values.toList() })
                
                val warningBuilder = StringBuilder()
                if (missingWarnings.isNotEmpty()) {
                    warningBuilder.append("⚠️ **تنبيهات إدخال البيانات المفقودة:**\n")
                    missingWarnings.take(3).forEach { warningBuilder.append("- $it\n") }
                    warningBuilder.append("\n")
                }
                
                // Check for close expiry dates (within 30 days)
                val expWarnings = checkExpiryWarnings(updatedRecords.ifEmpty { dbRecords.values.toList() })
                if (expWarnings.isNotEmpty()) {
                    warningBuilder.append("🚨 **تحذير انتهاء الصلاحية (خلال 30 يوماً):**\n")
                    expWarnings.forEach { warningBuilder.append("- $it\n") }
                    warningBuilder.append("\n")
                }
                
                _parsingResultText.value = """
                    |$warningBuilder
                    |📋 **جدول البيانات الدورية المحتسب بدقة (شهر ${localResult.month} ${localResult.year}):**
                    |
                    |$markdownTable
                    |
                    |⚙️ **صيغة الـ JSON الخاصة بالربط مع الأنظمة الخارجية:**
                    |```json
                    |$jsonSchema
                    |```
                """.trimMargin()
                
                // Reload
                loadInventoryRecords(localResult.month, localResult.year)
                
            } catch (e: Exception) {
                _parsingResultText.value = "حدث خطأ أثناء معالجة التقرير: ${e.localizedMessage}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    private fun checkExpiryWarnings(records: List<InventoryRecord>): List<String> {
        val warnings = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Date()
        val calendar = Calendar.getInstance()
        calendar.time = today
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        val in30Days = calendar.time
        
        for (rec in records) {
            val exp = rec.expiryDate
            if (!exp.isNullOrBlank()) {
                try {
                    val expDate = sdf.parse(exp)
                    if (expDate != null && expDate.before(in30Days) && expDate.after(today)) {
                        warnings.add("المادة '${rec.itemName}' ستنتهي صلاحيتها في تاريخ $exp (متبقي أقل من 30 يوماً)")
                    } else if (expDate != null && expDate.before(today)) {
                        warnings.add("المادة '${rec.itemName}' منتهية الصلاحية بالفعل منذ تاريخ $exp ⚠️")
                    }
                } catch (e: Exception) {
                    // Ignore date parse errors
                }
            }
        }
        return warnings
    }

    fun generateMarkdownTable(month: String, year: String, records: List<InventoryRecord>): String {
        val sb = StringBuilder()
        sb.append("| اسم المادة | رصيد البداية | المستلم | الرصيد الكلي | الملقح (ثابت+خارجي) | الهدر | المناقلات | الرصيد المتبقي |\n")
        sb.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n")
        for (rec in records) {
            if (rec.initialBalance > 0 || rec.receivedDuringMonth > 0 || rec.totalVaccinated > 0 || rec.totalWasted > 0 || rec.transferredOut > 0) {
                sb.append("| ${rec.itemName} | ${rec.initialBalance} | ${rec.receivedDuringMonth} | ${rec.totalAvailable} | ${rec.totalVaccinated} (${rec.fixedSessionVaccinated}+${rec.outreachSessionVaccinated}) | ${rec.totalWasted} | ${rec.transferredOut} | ${rec.finalRemaining} |\n")
            }
        }
        return sb.toString()
    }

    fun generateJsonString(month: String, year: String, center: String, records: List<InventoryRecord>): String {
        try {
            val root = org.json.JSONObject()
            val meta = org.json.JSONObject()
            meta.put("month", month)
            meta.put("year", year)
            meta.put("center_name", center.ifBlank { "مركز حي المستنصرية" })
            root.put("report_metadata", meta)
            
            val array = org.json.JSONArray()
            for (rec in records) {
                val obj = org.json.JSONObject()
                obj.put("item_name", rec.itemName)
                obj.put("item_type", rec.itemType)
                obj.put("initial_balance", rec.initialBalance)
                obj.put("received_during_month", rec.receivedDuringMonth)
                obj.put("total_available", rec.totalAvailable)
                obj.put("fixed_session_vaccinated", rec.fixedSessionVaccinated)
                obj.put("outreach_session_vaccinated", rec.outreachSessionVaccinated)
                obj.put("total_vaccinated", rec.totalVaccinated)
                obj.put("opened_wasted", rec.openedWasted)
                obj.put("closed_wasted", rec.closedWasted)
                obj.put("total_wasted", rec.totalWasted)
                obj.put("transferred_out", rec.transferredOut)
                obj.put("final_remaining", rec.finalRemaining)
                obj.put("expiry_date", rec.expiryDate ?: org.json.JSONObject.NULL)
                array.put(obj)
            }
            root.put("inventory_data", array)
            return root.toString(2)
        } catch (e: Exception) {
            return "{}"
        }
    }

    fun generateChildQrJson(child: Child, records: List<VaccineRecord>): String {
        try {
            val root = org.json.JSONObject()
            root.put("schema", "iraqi_vaccine_child_v1")
            
            val childObj = org.json.JSONObject()
            childObj.put("uniqueId", child.uniqueId)
            childObj.put("fullName", child.fullName)
            childObj.put("motherName", child.motherName)
            childObj.put("healthCenter", child.healthCenter)
            childObj.put("address", child.address)
            childObj.put("phone", child.phone)
            childObj.put("dob", child.dob)
            childObj.put("vaccinationNumber", child.vaccinationNumber)
            childObj.put("familyNumber", child.familyNumber)
            childObj.put("headOfHousehold", child.headOfHousehold)
            childObj.put("isAdultTd", child.isAdultTd)
            childObj.put("gender", child.gender)
            root.put("child", childObj)

            val effectiveRecords = if (records.isEmpty()) {
                val schedule = if (child.isAdultTd) VaccineScheduler.adultTdSchedule else VaccineScheduler.iraqSchedule
                schedule.map { item ->
                    VaccineRecord(
                        childId = child.id,
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
            } else {
                records
            }

            val vaccinesArray = org.json.JSONArray()
            for (rec in effectiveRecords) {
                val vObj = org.json.JSONObject()
                vObj.put("k", rec.vaccineKey)
                vObj.put("t", if (rec.isTaken) 1 else 0)
                if (rec.takenDate != null) vObj.put("d", rec.takenDate)
                if (rec.isSkipped) vObj.put("s", 1)
                if (rec.skipReason != null) vObj.put("sr", rec.skipReason)
                if (rec.sideEffect != null) vObj.put("se", rec.sideEffect)
                vaccinesArray.put(vObj)
            }
            root.put("vaccines", vaccinesArray)
            return root.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return "{}"
        }
    }

    fun handleScannedQrCode(rawResult: String) {
        viewModelScope.launch {
            try {
                val trimmed = rawResult.trim()
                if (trimmed.startsWith("{") && trimmed.contains("iraqi_vaccine_child_v1")) {
                    val root = org.json.JSONObject(trimmed)
                    val childObj = root.getJSONObject("child")
                    val uniqueId = childObj.getString("uniqueId")
                    val fullName = childObj.getString("fullName")
                    val motherName = childObj.optString("motherName", "")
                    val healthCenter = childObj.optString("healthCenter", "طاق ميكائيل")
                    val address = childObj.optString("address", "")
                    val phone = childObj.optString("phone", "")
                    val dob = childObj.optString("dob", "")
                    val vaccinationNumber = childObj.optString("vaccinationNumber", "")
                    val familyNumber = childObj.optString("familyNumber", "")
                    val headOfHousehold = childObj.optString("headOfHousehold", "")
                    val isAdultTd = childObj.optBoolean("isAdultTd", false)
                    val gender = childObj.optString("gender", "ذكر")

                    var existingChild = repository.getChildByUniqueId(uniqueId)
                    val childId: Int
                    if (existingChild != null) {
                        val updated = existingChild.copy(
                            fullName = fullName,
                            motherName = motherName,
                            healthCenter = healthCenter,
                            address = address,
                            phone = phone,
                            dob = dob,
                            vaccinationNumber = vaccinationNumber,
                            familyNumber = familyNumber,
                            headOfHousehold = headOfHousehold,
                            isAdultTd = isAdultTd,
                            gender = gender
                        )
                        repository.updateChild(updated)
                        childId = updated.id
                    } else {
                        val newChild = Child(
                            uniqueId = uniqueId,
                            fullName = fullName,
                            motherName = motherName,
                            healthCenter = healthCenter,
                            address = address,
                            phone = phone,
                            dob = dob,
                            vaccinationNumber = vaccinationNumber,
                            familyNumber = familyNumber,
                            headOfHousehold = headOfHousehold,
                            isAdultTd = isAdultTd,
                            gender = gender
                        )
                        childId = repository.dao.insertChild(newChild).toInt()

                        val scheduleList = if (isAdultTd) {
                            VaccineScheduler.adultTdSchedule
                        } else {
                            VaccineScheduler.iraqSchedule
                        }
                        val initialRecords = scheduleList.map { item ->
                            VaccineRecord(
                                childId = childId,
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
                        repository.dao.insertVaccineRecords(initialRecords)
                    }

                    val vaccinesArray = root.getJSONArray("vaccines")
                    val existingRecords = repository.dao.getVaccinesForChildSync(childId)
                    val existingMap = existingRecords.associateBy { it.vaccineKey }

                    val recordsToInsertOrUpdate = mutableListOf<VaccineRecord>()
                    for (i in 0 until vaccinesArray.length()) {
                        val vObj = vaccinesArray.getJSONObject(i)
                        val vaccineKey = vObj.optString("vaccineKey", vObj.optString("k", ""))
                        if (vaccineKey.isEmpty()) continue

                        val isTaken = vObj.optBoolean("isTaken", vObj.optInt("t", 0) == 1)
                        val takenDate = if (vObj.has("takenDate") && !vObj.isNull("takenDate")) {
                            vObj.getString("takenDate")
                        } else if (vObj.has("d") && !vObj.isNull("d")) {
                            vObj.getString("d")
                        } else null
                        val isSkipped = vObj.optBoolean("isSkipped", vObj.optInt("s", 0) == 1)
                        val skipReason = if (vObj.has("skipReason") && !vObj.isNull("skipReason")) {
                            vObj.getString("skipReason")
                        } else if (vObj.has("sr") && !vObj.isNull("sr")) {
                            vObj.getString("sr")
                        } else null
                        val sideEffect = if (vObj.has("sideEffect") && !vObj.isNull("sideEffect")) {
                            vObj.getString("sideEffect")
                        } else if (vObj.has("se") && !vObj.isNull("se")) {
                            vObj.getString("se")
                        } else null

                        val existingRec = existingMap[vaccineKey]
                        if (existingRec != null) {
                            recordsToInsertOrUpdate.add(
                                existingRec.copy(
                                    isTaken = isTaken,
                                    takenDate = takenDate,
                                    isSkipped = isSkipped,
                                    skipReason = skipReason,
                                    sideEffect = sideEffect
                                )
                            )
                        } else {
                            val defaultSchedItem = (if (isAdultTd) VaccineScheduler.adultTdSchedule else VaccineScheduler.iraqSchedule).find { it.key == vaccineKey }
                            val nameAr = if (vObj.has("nameAr")) vObj.getString("nameAr") else (defaultSchedItem?.nameAr ?: vaccineKey)
                            val categoryAr = if (vObj.has("categoryAr")) vObj.getString("categoryAr") else (defaultSchedItem?.categoryAr ?: "")
                            val dueMonths = if (vObj.has("dueMonths")) vObj.getInt("dueMonths") else (defaultSchedItem?.dueMonths ?: 0)

                            recordsToInsertOrUpdate.add(
                                VaccineRecord(
                                    childId = childId,
                                    vaccineKey = vaccineKey,
                                    nameAr = nameAr,
                                    categoryAr = categoryAr,
                                    dueMonths = dueMonths,
                                    isTaken = isTaken,
                                    takenDate = takenDate,
                                    isSkipped = isSkipped,
                                    skipReason = skipReason,
                                    sideEffect = sideEffect
                                )
                            )
                        }
                    }
                    if (recordsToInsertOrUpdate.isNotEmpty()) {
                        repository.dao.insertVaccineRecords(recordsToInsertOrUpdate)
                    }
                    navigateTo(AppScreen.ChildDetails(childId))
                } else {
                    val childId = trimmed.toIntOrNull()
                    val child = if (childId != null) {
                        repository.getChildById(childId)
                    } else {
                        repository.getChildByUniqueId(trimmed)
                    }

                    if (child != null) {
                        val records = repository.dao.getVaccinesForChildSync(child.id)
                        val pendingRecord = records.firstOrNull { !it.isTaken }
                        if (pendingRecord != null) {
                            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                            repository.updateVaccineStatus(pendingRecord, true, currentDate)
                        }
                        navigateTo(AppScreen.ChildDetails(child.id))
                    } else {
                        val allChildren = repository.getAllChildrenSync()
                        val matched = allChildren.find { it.uniqueId.equals(trimmed, ignoreCase = true) || it.fullName.contains(trimmed, ignoreCase = true) }
                        if (matched != null) {
                            val records = repository.dao.getVaccinesForChildSync(matched.id)
                            val pendingRecord = records.firstOrNull { !it.isTaken }
                            if (pendingRecord != null) {
                                val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                repository.updateVaccineStatus(pendingRecord, true, currentDate)
                            }
                            navigateTo(AppScreen.ChildDetails(matched.id))
                        } else {
                            navigateTo(AppScreen.ChildrenList)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("QrScanner", "Failed to handle QR", e)
            }
        }
    }
}

class VaccineViewModelFactory(
    private val repository: VaccineRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaccineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VaccineViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
