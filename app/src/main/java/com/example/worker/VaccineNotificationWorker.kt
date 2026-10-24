package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.AppDatabase
import java.util.Calendar

class VaccineNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.vaccineDao()
            
            val children = dao.getAllChildrenSync()
            val records = dao.getAllVaccineRecordsSync()
            
            val calendar = Calendar.getInstance()
            val currentMillis = calendar.timeInMillis
            val oneDayMillis = 24 * 60 * 60 * 1000L
            val sevenDaysMillis = 7 * oneDayMillis
            val threeDaysMillis = 3 * oneDayMillis
            
            val prefs = applicationContext.getSharedPreferences("whatsapp_defaulter_prefs", Context.MODE_PRIVATE)
            
            var upcomingCount = 0
            val childrenWithUpcoming = mutableSetOf<String>()
            
            for (child in children) {
                val childRecords = records.filter { it.childId == child.id && !it.isTaken && !it.isSkipped }
                
                // Group pending records by category to check for overdue / defaulters (> 28 days overdue)
                val categoryMap = childRecords.groupBy { it.categoryAr }
                for ((categoryAr, catRecords) in categoryMap) {
                    val dobCalendar = Calendar.getInstance()
                    val dobParts = child.dob.split("-", "/")
                    if (dobParts.size == 3) {
                        try {
                            val year = dobParts[0].toInt()
                            val month = dobParts[1].toInt() - 1
                            val day = dobParts[2].toInt()
                            dobCalendar.set(year, month, day)
                            
                            val firstRecord = catRecords.minByOrNull { it.dueMonths } ?: continue
                            val dueCalendar = dobCalendar.clone() as Calendar
                            dueCalendar.add(Calendar.MONTH, firstRecord.dueMonths)
                            val dueMillis = dueCalendar.timeInMillis
                            
                            val diff = currentMillis - dueMillis // positive if overdue
                            val overdueDays = diff / oneDayMillis
                            
                            // 1. Check if OVERDUE (> 28 days overdue) -> Defaulter / المتسربين
                            if (overdueDays > 28) {
                                val lastSentKey = "last_sent_${child.id}_${categoryAr.hashCode()}"
                                val lastSentMillis = prefs.getLong(lastSentKey, 0L)
                                
                                // Check if 7 days have passed since last WhatsApp reminder (or never sent)
                                if (currentMillis - lastSentMillis >= sevenDaysMillis) {
                                    val vaccineNames = catRecords.joinToString(" + ") { it.nameAr }
                                    val formattedPhone = child.phone.replace("+", "").replace(" ", "").replace("-", "").trim()
                                    
                                    val idText = if (child.vaccinationNumber.isNotBlank()) "رقم السجل: ${child.vaccinationNumber}" else "الاسم: ${child.fullName}"
                                    val text = "🛡️ تطبيق إحسان لإدارة التلقيح والتحصين\nتنبيه أسبوعي عاجل للتسرب اللقاحي! إن الطفل (${child.fullName}) ذو ($idText) متأخر عن تلقي جرعته بلقاحات ($vaccineNames) المستحقة لعمر ($categoryAr) المفترض أخذها منذ تاريخ ${firstRecord.takenDate ?: "الموعد المحدد"}. يرجى مراجعة أقرب مركز صحي بالسرعة الممكنة لحمايته.\n\nتم إرسال هذه الرسالة عبر نظام التذكير الأسبوعي التلقائي في تطبيق إحسان 🌟\n\n📢 قناة التطبيق على التليكرام:\nhttps://t.me/i7san_App\n\n📥 لتحميل التطبيق:\nhttps://www.mediafire.com/folder/246decua5ij07/i7san"
                                    
                                    showWhatsAppDefaulterNotification(child.fullName, categoryAr, vaccineNames, formattedPhone, text, child.id + categoryAr.hashCode())
                                    
                                    // Record last sent timestamp
                                    prefs.edit().putLong(lastSentKey, currentMillis).apply()
                                }
                            }
                            
                            // 2. Check approaching (within 3 days)
                            val diffToDue = dueMillis - currentMillis
                            if (diffToDue in -7 * oneDayMillis..threeDaysMillis) {
                                upcomingCount++
                                childrenWithUpcoming.add(child.fullName)
                            }
                        } catch (e: Exception) {
                            // parse error
                        }
                    }
                }
            }
            
            if (upcomingCount > 0) {
                showNotification(upcomingCount, childrenWithUpcoming.joinToString(", "))
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }

    private fun showWhatsAppDefaulterNotification(childName: String, category: String, vaccines: String, phone: String, message: String, notificationId: Int) {
        val channelId = "whatsapp_defaulter_channel"
        val context = applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات واتساب الأسبوعية للمتسربين"
            val descriptionText = "إرسال تذكير واتساب أسبوعي تلقائي للأطفال المتسربين عن اللقاح"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 تذكير واتساب أسبوعي: تسرب لقاحي")
            .setContentText("الطفل $childName متسرب عن لقاحات ($vaccines) لعمر $category. انقر للإرسال عبر واتساب")
            .setStyle(NotificationCompat.BigTextStyle().bigText("تنبيه أسبوعي تلقائي للمتسربين:\nالطفل: $childName\nالعمر: $category\nاللقاحات المتسرب عنها: $vaccines\n\nانقر هنا لفتح واتساب وإرسال رسالة التذكير فوراً."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // permission missing
        }
    }

    private fun showNotification(count: Int, names: String) {
        val channelId = "vaccine_alerts_channel"
        val notificationId = 1001

        val context = applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات اللقاحات"
            val descriptionText = "إشعارات بمواعيد اللقاحات القادمة للأطفال"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("مواعيد لقاحات قريبة!")
            .setContentText("يوجد $count جرعات مستحقة قريباً لـ: $names")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            // Permission for POST_NOTIFICATIONS missing
        }
    }
}
