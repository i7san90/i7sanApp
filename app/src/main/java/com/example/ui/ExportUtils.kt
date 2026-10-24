package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color as GColor
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.TextPaint
import android.widget.Toast
import com.example.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    // 📄 1. Export Child Digital Vaccine Card to PDF and save it local
    fun exportChildToPdf(context: Context, child: Child, vaccines: List<VaccineRecord>) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val paint = Paint()
            val textPaint = TextPaint().apply {
                color = GColor.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            
            // Draw clean background
            paint.color = GColor.WHITE
            canvas.drawRect(0f, 0f, 595f, 842f, paint)

            // Draw elegant Teal border line
            paint.color = GColor.parseColor("#0F766E") // Teal-700
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawRect(20f, 20f, 575f, 822f, paint)
            
            // Draw Header Card
            paint.color = GColor.parseColor("#0F766E")
            paint.style = Paint.Style.FILL
            canvas.drawRect(20f, 20f, 575f, 100f, paint)
            
            // Header Typography
            textPaint.color = GColor.WHITE
            textPaint.textSize = 18f
            textPaint.isFakeBoldText = true
            canvas.drawText("الجمهورية العراقية - وزارة الصحة", 195f, 50f, textPaint)
            textPaint.textSize = 13f
            textPaint.isFakeBoldText = false
            canvas.drawText("كارت تلقيح الطفل الإلكتروني الذكي (دائرة الرعاية الصحية الفورية)", 135f, 75f, textPaint)
            
            // Personal Details Box
            textPaint.color = GColor.parseColor("#0F766E")
            textPaint.textSize = 13f
            textPaint.isFakeBoldText = true
            var y = 135f
            canvas.drawText("• هوية ومعلومات الطفل والأسرة العائلية:", 40f, y, textPaint)
            
            textPaint.color = GColor.BLACK
            textPaint.isFakeBoldText = false
            textPaint.textSize = 11f
            y += 25f
            canvas.drawText("اسم الطفل الرباعي: ${child.fullName}", 45f, y, textPaint)
            y += 20f
            canvas.drawText("اسم الأم الثلاثي: ${child.motherName}", 45f, y, textPaint)
            y += 20f
            canvas.drawText("رقم المعرف الفريد للتلقيح (ID): ${child.uniqueId}", 45f, y, textPaint)
            y += 20f
            canvas.drawText("تاريخ الميلاد: ${child.dob} (${VaccineScheduler.computeAgeString(child.dob)})", 45f, y, textPaint)
            y += 20f
            canvas.drawText("العنوان والقرية السكنية: ${child.address}", 45f, y, textPaint)
            y += 20f
            canvas.drawText("رقم هاتف ولي الأمر (الواتساب): ${child.phone}", 45f, y, textPaint)
            y += 20f
            canvas.drawText("المركز الصحي المعتمد: ${child.healthCenter}", 45f, y, textPaint)
            
            // Draw separator line
            y += 15f
            paint.color = GColor.LTGRAY
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(40f, y, 550f, y, paint)
            
            // Vaccine Table
            y += 25f
            textPaint.color = GColor.parseColor("#0F766E")
            textPaint.textSize = 13f
            textPaint.isFakeBoldText = true
            canvas.drawText("• سجل تطعيمات الطفل الملقحة وجدول الاستحقاقات والجرعات المستلمة:", 40f, y, textPaint)
            
            textPaint.color = GColor.BLACK
            textPaint.textSize = 9f
            textPaint.isFakeBoldText = false
            
            // Table Header Bar
            y += 20f
            paint.color = GColor.parseColor("#F1F5F9")
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, y, 550f, y + 20f, paint)
            
            paint.color = GColor.DKGRAY
            paint.style = Paint.Style.STROKE
            canvas.drawRect(40f, y, 550f, y + 20f, paint)
            
            textPaint.isFakeBoldText = true
            canvas.drawText("اللقاح الطبي", 45f, y + 14f, textPaint)
            canvas.drawText("الفئة العمرية", 240f, y + 14f, textPaint)
            canvas.drawText("تاريخ الاستحقاق", 360f, y + 14f, textPaint)
            canvas.drawText("تاريخ وحالة الحقن والتلقيح", 450f, y + 14f, textPaint)
            textPaint.isFakeBoldText = false
            
            y += 20f
            
            for (v in vaccines) {
                if (y > 780f) {
                    break // Prevent single page overflow
                }
                
                // Horizontal divider
                paint.color = GColor.parseColor("#E2E8F0")
                paint.style = Paint.Style.STROKE
                canvas.drawLine(40f, y, 550f, y, paint)
                
                canvas.drawText(v.nameAr, 45f, y + 14f, textPaint)
                canvas.drawText(v.categoryAr, 240f, y + 14f, textPaint)
                
                val dueDate = VaccineScheduler.getRecordDueDate(child, v, vaccines)
                canvas.drawText(dueDate, 360f, y + 14f, textPaint)
                
                val statusText = if (v.isTaken) {
                    "أُخذ بتاريخ: ${v.takenDate ?: "مسجل"}"
                } else if (v.isSkipped) {
                    "متجاوز: ${v.skipReason ?: "تأجيل"}"
                } else {
                    "غير مكتمل (قيد الانتظار)"
                }
                canvas.drawText(statusText, 450f, y + 14f, textPaint)
                
                y += 18f
            }
            
            // Footer Section
            y = 790f
            paint.color = GColor.parseColor("#0F766E")
            paint.style = Paint.Style.FILL
            canvas.drawRect(40f, y, 550f, y + 2f, paint)
            
            textPaint.textSize = 8.5f
            textPaint.color = GColor.GRAY
            canvas.drawText("تم استخراج الكارت وتوليده رقمياً عبر نظام ومقاصد اللقاحات للمنسق البرمجي العام احسان ال شنتي.", 110f, y + 15f, textPaint)
            canvas.drawText("تاريخ المزامنة والطباعة: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 40f, y + 25f, textPaint)
            
            pdfDocument.finishPage(page)
            
            val fileName = "VaccineCard_${child.fullName.replace(" ", "_")}_${child.uniqueId}.pdf"
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val file = File(directory, fileName)
            
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            
            Toast.makeText(context, "تم حفظ كرت الـ PDF بنجاح 📄", Toast.LENGTH_LONG).show()
            
            // Launch print viewer intent
            val fileUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(viewIntent, "عرض وطباعة بطاقة اللقاح"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ أثناء توليد بطاقة الـ PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    // 📊 2. Export Children Archive to Excel (CSV with UTF-8 BOM to keep Arabic gorgeous in Excel)
    fun exportChildrenToExcel(context: Context, children: List<Child>, dao: VaccineDao) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val csvBuilder = StringBuilder()
                // Byte Order Mark (BOM) to instruct Microsoft Excel of active UTF-8
                csvBuilder.append('\uFEFF')
                
                // Columns Header as requested
                csvBuilder.append("التسلسل,اسم الطفل,رقم السجل الدائمي,رقم الاسرة,عنوان السكن,رقم هاتف رب الاسرة,اللقاحات المستلمة مع تاريخ التلقيح,اللقاحات المتبقية,اللقاحات المتسرب عنها\n")
                
                var serial = 1
                for (child in children) {
                    val vaxList = dao.getVaccinesForChildSync(child.id)
                    
                    // Received/Taken vaccines with date of vaccination
                    val received = vaxList.filter { it.isTaken }.joinToString(" - ") { 
                        "${it.nameAr} (${it.takenDate ?: "بدون تاريخ"})" 
                    }
                    
                    // Remaining vaccines (not taken, not skipped, and not overdue)
                    val remaining = vaxList.filter { 
                        !it.isTaken && !it.isSkipped && VaccineScheduler.getRecordStatus(child, it, vaxList) != VaccineStatus.OVERDUE 
                    }.joinToString(" - ") { it.nameAr }
                    
                    // Overdue/defaulter vaccines (not taken, not skipped, and overdue)
                    val overdueList = vaxList.filter { 
                        !it.isTaken && !it.isSkipped && VaccineScheduler.getRecordStatus(child, it, vaxList) == VaccineStatus.OVERDUE 
                    }.joinToString(" - ") { it.nameAr }
                    
                    fun escapeCsvCell(cell: String): String {
                        val clean = cell.replace("\"", "\"\"")
                        if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
                            return "\"$clean\""
                        }
                        return clean
                    }
                    
                    csvBuilder.append("${serial},")
                    csvBuilder.append("${escapeCsvCell(child.fullName)},")
                    csvBuilder.append("${escapeCsvCell(child.vaccinationNumber)},")
                    csvBuilder.append("${escapeCsvCell(child.familyNumber)},")
                    csvBuilder.append("${escapeCsvCell(child.address)},")
                    csvBuilder.append("${escapeCsvCell(child.phone)},")
                    csvBuilder.append("${escapeCsvCell(received)},")
                    csvBuilder.append("${escapeCsvCell(remaining)},")
                    csvBuilder.append("${escapeCsvCell(overdueList)}\n")
                    
                    serial++
                }
                
                val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val fileName = "اللقاحات.csv"
                val file = File(directory, fileName)
                
                file.writeText(csvBuilder.toString(), Charsets.UTF_8)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "تم تصدير أرشيف الأطفال بصيغة Excel/CSV إلى التنزيلات بنجاح 📁", Toast.LENGTH_LONG).show()
                    
                    val fileUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val excelIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileUri, "text/comma-separated-values")
                        flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    try {
                        context.startActivity(Intent.createChooser(excelIntent, "افتح الأرشيف عبر Microsoft Excel أو جداول جوجل"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "الملف محفوظ بنجاح في مجلد التنزيلات الخاص بالتطبيق", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "خطأ أثناء محاولة تصدير الأرشيف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 📁 3. Export entire Vaccine database to a local JSON file for bulletproof backup
    fun exportDatabaseToJson(context: Context, dao: VaccineDao) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val families = dao.getAllFamiliesSync()
                val children = dao.getAllChildrenSync()
                val vaccineRecords = dao.getAllVaccineRecordsSync()

                val rootJson = org.json.JSONObject()
                rootJson.put("version", 1)
                rootJson.put("backupDate", System.currentTimeMillis())

                // Families
                val familiesArray = org.json.JSONArray()
                for (f in families) {
                    val fJson = org.json.JSONObject()
                    fJson.put("id", f.id)
                    fJson.put("familyNumber", f.familyNumber)
                    fJson.put("headOfHousehold", f.headOfHousehold)
                    fJson.put("address", f.address)
                    fJson.put("phone", f.phone)
                    familiesArray.put(fJson)
                }
                rootJson.put("families", familiesArray)

                // Children
                val childrenArray = org.json.JSONArray()
                for (c in children) {
                    val cJson = org.json.JSONObject()
                    cJson.put("id", c.id)
                    cJson.put("uniqueId", c.uniqueId)
                    cJson.put("fullName", c.fullName)
                    cJson.put("motherName", c.motherName)
                    cJson.put("healthCenter", c.healthCenter)
                    cJson.put("address", c.address)
                    cJson.put("phone", c.phone)
                    cJson.put("dob", c.dob)
                    cJson.put("registrationDate", c.registrationDate)
                    cJson.put("vaccinationNumber", c.vaccinationNumber)
                    cJson.put("familyNumber", c.familyNumber)
                    cJson.put("headOfHousehold", c.headOfHousehold)
                    cJson.put("isAdultTd", c.isAdultTd)
                    childrenArray.put(cJson)
                }
                rootJson.put("children", childrenArray)

                // Vaccine records
                val recordsArray = org.json.JSONArray()
                for (r in vaccineRecords) {
                    val rJson = org.json.JSONObject()
                    rJson.put("id", r.id)
                    rJson.put("childId", r.childId)
                    rJson.put("vaccineKey", r.vaccineKey)
                    rJson.put("nameAr", r.nameAr)
                    rJson.put("categoryAr", r.categoryAr)
                    rJson.put("dueMonths", r.dueMonths)
                    rJson.put("isTaken", r.isTaken)
                    rJson.put("takenDate", r.takenDate ?: org.json.JSONObject.NULL)
                    rJson.put("isSkipped", r.isSkipped)
                    rJson.put("skipReason", r.skipReason ?: org.json.JSONObject.NULL)
                    rJson.put("sideEffect", r.sideEffect ?: org.json.JSONObject.NULL)
                    recordsArray.put(rJson)
                }
                rootJson.put("vaccineRecords", recordsArray)

                val jsonString = rootJson.toString(4)

                val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val fileName = "اللقاحات_نسخة_احتياطية.json"
                val file = File(directory, fileName)
                file.writeText(jsonString, Charsets.UTF_8)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "تم توليد النسخة الاحتياطية بنجاح 📂", Toast.LENGTH_LONG).show()

                    val fileUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية لقاعدة بيانات اللقاحات")
                        putExtra(Intent.EXTRA_TEXT, "النسخة الاحتياطية المعتمدة لقاعدة بيانات اللقاحات ورعاية الطفولة والأسرة بصيغة JSON")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة وحفظ النسخة الاحتياطية عبر"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "فشل تصدير النسخة الاحتياطية: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 📁 3b. Export entire Vaccine database directly to a URI selected by the user
    fun exportDatabaseToUri(context: Context, uri: android.net.Uri, dao: VaccineDao) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val families = dao.getAllFamiliesSync()
                val children = dao.getAllChildrenSync()
                val vaccineRecords = dao.getAllVaccineRecordsSync()

                val rootJson = org.json.JSONObject()
                rootJson.put("version", 1)
                rootJson.put("backupDate", System.currentTimeMillis())

                // Families
                val familiesArray = org.json.JSONArray()
                for (f in families) {
                    val fJson = org.json.JSONObject()
                    fJson.put("id", f.id)
                    fJson.put("familyNumber", f.familyNumber)
                    fJson.put("headOfHousehold", f.headOfHousehold)
                    fJson.put("address", f.address)
                    fJson.put("phone", f.phone)
                    familiesArray.put(fJson)
                }
                rootJson.put("families", familiesArray)

                // Children
                val childrenArray = org.json.JSONArray()
                for (c in children) {
                    val cJson = org.json.JSONObject()
                    cJson.put("id", c.id)
                    cJson.put("uniqueId", c.uniqueId)
                    cJson.put("fullName", c.fullName)
                    cJson.put("motherName", c.motherName)
                    cJson.put("healthCenter", c.healthCenter)
                    cJson.put("address", c.address)
                    cJson.put("phone", c.phone)
                    cJson.put("dob", c.dob)
                    cJson.put("registrationDate", c.registrationDate)
                    cJson.put("vaccinationNumber", c.vaccinationNumber)
                    cJson.put("familyNumber", c.familyNumber)
                    cJson.put("headOfHousehold", c.headOfHousehold)
                    cJson.put("isAdultTd", c.isAdultTd)
                    childrenArray.put(cJson)
                }
                rootJson.put("children", childrenArray)

                // Vaccine records
                val recordsArray = org.json.JSONArray()
                for (r in vaccineRecords) {
                    val rJson = org.json.JSONObject()
                    rJson.put("id", r.id)
                    rJson.put("childId", r.childId)
                    rJson.put("vaccineKey", r.vaccineKey)
                    rJson.put("nameAr", r.nameAr)
                    rJson.put("categoryAr", r.categoryAr)
                    rJson.put("dueMonths", r.dueMonths)
                    rJson.put("isTaken", r.isTaken)
                    rJson.put("takenDate", r.takenDate ?: org.json.JSONObject.NULL)
                    rJson.put("isSkipped", r.isSkipped)
                    rJson.put("skipReason", r.skipReason ?: org.json.JSONObject.NULL)
                    rJson.put("sideEffect", r.sideEffect ?: org.json.JSONObject.NULL)
                    recordsArray.put(rJson)
                }
                rootJson.put("vaccineRecords", recordsArray)

                val jsonString = rootJson.toString(4)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "تم حفظ ملف النسخة الاحتياطية بنجاح في جهازك 💾📂", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "فشل حفظ النسخة الاحتياطية: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 🔄 4. Restore entire database from a JSON backup file
    fun importDatabaseFromJson(context: Context, jsonContent: String, dao: VaccineDao, onComplete: () -> Unit) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val rootJson = org.json.JSONObject(jsonContent)
                
                if (!rootJson.has("children")) {
                    throw Exception("الملف غير صالح (مفقود قائمة الأطفال الكلية)")
                }

                // Parse families
                val familiesList = mutableListOf<Family>()
                if (rootJson.has("families")) {
                    val familiesArray = rootJson.getJSONArray("families")
                    for (i in 0 until familiesArray.length()) {
                        val fObj = familiesArray.getJSONObject(i)
                        familiesList.add(
                            Family(
                                id = fObj.optInt("id", 0),
                                familyNumber = fObj.optString("familyNumber", ""),
                                headOfHousehold = fObj.optString("headOfHousehold", ""),
                                address = fObj.optString("address", ""),
                                phone = fObj.optString("phone", "")
                            )
                        )
                    }
                }

                // Parse children
                val childrenList = mutableListOf<Child>()
                val childrenArray = rootJson.getJSONArray("children")
                for (i in 0 until childrenArray.length()) {
                    val cObj = childrenArray.getJSONObject(i)
                    childrenList.add(
                        Child(
                            id = cObj.optInt("id", 0),
                            uniqueId = cObj.optString("uniqueId", ""),
                            fullName = cObj.optString("fullName", ""),
                            motherName = cObj.optString("motherName", ""),
                            healthCenter = cObj.optString("healthCenter", "طاق ميكائيل"),
                            address = cObj.optString("address", ""),
                            phone = cObj.optString("phone", ""),
                            dob = cObj.optString("dob", ""),
                            registrationDate = cObj.optLong("registrationDate", System.currentTimeMillis()),
                            vaccinationNumber = cObj.optString("vaccinationNumber", ""),
                            familyNumber = cObj.optString("familyNumber", ""),
                            headOfHousehold = cObj.optString("headOfHousehold", ""),
                            isAdultTd = cObj.optBoolean("isAdultTd", false)
                        )
                    )
                }

                // Parse vaccine records
                val recordsList = mutableListOf<VaccineRecord>()
                if (rootJson.has("vaccineRecords")) {
                    val recordsArray = rootJson.getJSONArray("vaccineRecords")
                    for (i in 0 until recordsArray.length()) {
                        val rObj = recordsArray.getJSONObject(i)
                        recordsList.add(
                            VaccineRecord(
                                id = rObj.optInt("id", 0),
                                childId = rObj.optInt("childId", 0),
                                vaccineKey = rObj.optString("vaccineKey", ""),
                                nameAr = rObj.optString("nameAr", ""),
                                categoryAr = rObj.optString("categoryAr", ""),
                                dueMonths = rObj.optInt("dueMonths", 0),
                                isTaken = rObj.optBoolean("isTaken", false),
                                takenDate = if (rObj.isNull("takenDate")) null else rObj.optString("takenDate"),
                                isSkipped = rObj.optBoolean("isSkipped", false),
                                skipReason = if (rObj.isNull("skipReason")) null else rObj.optString("skipReason"),
                                sideEffect = if (rObj.isNull("sideEffect")) null else rObj.optString("sideEffect")
                            )
                        )
                    }
                }

                // Execute db cleanup and replacement in transactions or sequence
                dao.deleteAllVaccineRecords()
                dao.deleteAllChildren()
                dao.deleteAllFamilies()

                // Insert families
                for (f in familiesList) {
                    dao.insertFamily(f)
                }

                // Insert children
                for (c in childrenList) {
                    dao.insertChild(c)
                }

                // Insert vaccine records
                if (recordsList.isNotEmpty()) {
                    dao.insertVaccineRecords(recordsList)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "تمت استعادة البيانات بنجاح! تم استيراد ${childrenList.size} سجل طفل 🔄", Toast.LENGTH_LONG).show()
                    onComplete()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "خطأ أثناء محاولة استعادة البيانات: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
