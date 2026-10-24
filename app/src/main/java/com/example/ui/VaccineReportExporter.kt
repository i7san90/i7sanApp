package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.InventoryRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar

object VaccineReportExporter {

    /**
     * Prints the inventory report as PDF using WebView and Android PrintManager.
     * This is highly reliable for RTL (Arabic) rendering, table layouts, and auto-pagination.
     * It allows users to "Save as PDF" to any folder on their device natively.
     */
    fun printReportAsHtml(context: Context, month: String, year: String, records: List<InventoryRecord>, centerName: String = "مركز حي المستنصرية الصحي") {
        val jobName = "تقرير_اللقاحات_${month}_${year}"
        val htmlContent = generateReportHtml(context, month, year, records, centerName)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                try {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                } catch (e: Exception) {
                    Toast.makeText(context, "فشلت عملية الطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }

    /**
     * Programmatically generates a PDF file using PdfDocument, saves it to the cache directory,
     * and triggers the system share sheet.
     */
    fun shareReportAsPdf(context: Context, month: String, year: String, records: List<InventoryRecord>, centerName: String = "مركز حي المستنصرية الصحي") {
        try {
            val file = generatePdfFile(context, month, year, records, centerName)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير اللقاحات لشهر $month $year")
                putExtra(Intent.EXTRA_TEXT, "مرفق تقرير جرد اللقاحات ومستلزمات التلقيح لشهر $month $year")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة تقرير PDF عبر..."))
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ أثناء توليد ملف المشاركة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Generates a PDF file, saves it to the Cache directory, and displays a success toast.
     * The user is also shown options to view or copy the file.
     */
    fun saveReportToCache(context: Context, month: String, year: String, records: List<InventoryRecord>, centerName: String = "مركز حي المستنصرية الصحي"): File? {
        return try {
            val file = generatePdfFile(context, month, year, records, centerName)
            Toast.makeText(context, "تم حفظ ملف PDF في الحافظة المؤقتة للتطبيق: ${file.name}", Toast.LENGTH_LONG).show()
            file
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ أثناء حفظ الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Generates a styled A4 PDF document and writes it directly to the provided OutputStream.
     * This handles multi-page layouts gracefully to include all elements.
     */
    fun writeReportToStream(
        context: Context,
        month: String,
        year: String,
        records: List<InventoryRecord>,
        outputStream: java.io.OutputStream,
        centerName: String = "مركز حي المستنصرية الصحي"
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var pageNumber = 1
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // 1. Draw top header card
        paint.color = 0xFF1976D2.toInt() // Primary Blue
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Draw custom logo in the header card (left side)
        try {
            val logoResId = context.resources.getIdentifier("img_clean_i7san_app_icon_1782895431554", "drawable", context.packageName)
            if (logoResId != 0) {
                val logoBitmap = BitmapFactory.decodeResource(context.resources, logoResId)
                if (logoBitmap != null) {
                    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 60, 60, true)
                    canvas.drawBitmap(scaledLogo, 30f, 15f, paint)
                }
            }
        } catch (e: Exception) {
            // ignore if failed to load
        }

        // 2. Draw white texts
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 18f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("تطبيق إحسان لإدارة التلقيح والتحصين", 595f / 2 + 30f, 32f, paint)
        
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("تقرير جرد اللقاحات ومستلزمات التلقيح لشهر $month $year", 595f / 2 + 30f, 55f, paint)
        paint.textSize = 9f
        canvas.drawText("الجهة المصدرة: $centerName | تاريخ الطباعة: " + SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(Calendar.getInstance().time), 595f / 2 + 30f, 75f, paint)

        // 3. Overall Statistics Section - REMOVED AS REQUESTED by user

        // 4. Draw Table Headers (shifted up since stats section is removed)
        paint.color = 0xFF1976D2.toInt()
        canvas.drawRect(20f, 105f, 575f, 130f, paint)

        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 9f
        paint.isFakeBoldText = true

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("اسم المادة / اللقاح", 565f, 121f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("رصيد البداية", 340f, 121f, paint)
        canvas.drawText("المستلم", 280f, 121f, paint)
        canvas.drawText("الكلي المتاح", 220f, 121f, paint)
        canvas.drawText("الملقحين", 160f, 121f, paint)
        canvas.drawText("الهدر كلي", 100f, 121f, paint)
        canvas.drawText("صافي المتبقي", 45f, 121f, paint)

        // 5. Draw Row Records
        var yPos = 150f
        paint.color = 0xFF333333.toInt()
        paint.isFakeBoldText = false

        // Group records: Vaccines first, then Supplies
        val vaccines = records.filter { it.itemType == "vaccine" }
        val supplies = records.filter { it.itemType == "supply" }
        val recordsToDraw = vaccines + supplies

        for (rec in recordsToDraw) {
            if (yPos > 790f) {
                // Finish current page
                pdfDocument.finishPage(page)
                
                // Start a new page
                pageNumber++
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                
                // Draw smaller page header
                paint.color = 0xFF1976D2.toInt()
                canvas.drawRect(0f, 0f, 595f, 40f, paint)
                
                paint.color = 0xFFFFFFFF.toInt()
                paint.textSize = 11f
                paint.isFakeBoldText = true
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("تابع تقرير اللقاحات ومستلزمات التلقيح - صفحة $pageNumber", 595f / 2, 25f, paint)

                // Draw Table Headers again
                paint.color = 0xFF1976D2.toInt()
                canvas.drawRect(20f, 55f, 575f, 80f, paint)
                paint.color = 0xFFFFFFFF.toInt()
                paint.textSize = 9f
                paint.isFakeBoldText = true

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("اسم المادة / اللقاح", 565f, 71f, paint)

                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("رصيد البداية", 340f, 71f, paint)
                canvas.drawText("المستلم", 280f, 71f, paint)
                canvas.drawText("الكلي المتاح", 220f, 71f, paint)
                canvas.drawText("الملقحين", 160f, 71f, paint)
                canvas.drawText("الهدر كلي", 100f, 71f, paint)
                canvas.drawText("صافي المتبقي", 45f, 71f, paint)

                yPos = 105f
            }

            // Alternating row background
            val idx = recordsToDraw.indexOf(rec)
            if (idx % 2 == 0) {
                paint.color = 0xFFF7F9FB.toInt()
                canvas.drawRect(20f, yPos - 14f, 575f, yPos + 6f, paint)
            }

            paint.color = 0xFF333333.toInt()
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.RIGHT
            val cleanName = if (rec.itemName.length > 34) rec.itemName.substring(0, 31) + "..." else rec.itemName
            canvas.drawText(cleanName, 565f, yPos, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(rec.initialBalance.toString(), 340f, yPos, paint)
            canvas.drawText(rec.receivedDuringMonth.toString(), 280f, yPos, paint)
            canvas.drawText(rec.totalAvailable.toString(), 220f, yPos, paint)
            canvas.drawText(rec.totalVaccinated.toString(), 160f, yPos, paint)
            
            if (rec.totalWasted > 0) {
                paint.color = 0xFFD32F2F.toInt() // Red for wasted
                canvas.drawText(rec.totalWasted.toString(), 100f, yPos, paint)
                paint.color = 0xFF333333.toInt()
            } else {
                canvas.drawText("0", 100f, yPos, paint)
            }

            paint.isFakeBoldText = true
            paint.color = 0xFF1976D2.toInt()
            canvas.drawText(rec.finalRemaining.toString(), 45f, yPos, paint)
            paint.isFakeBoldText = false
            paint.color = 0xFF333333.toInt()

            // bottom border
            paint.color = 0xFFE0E0E0.toInt()
            canvas.drawLine(20f, yPos + 6f, 575f, yPos + 6f, paint)

            yPos += 20f
        }

        // Draw Footer on last page
        paint.color = 0xFF777777.toInt()
        paint.textSize = 8f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("تم استخراج وطباعة هذا التقرير تلقائياً عبر تطبيق إحسان لإدارة التلقيح والتحصين 🌟", 595f / 2, 815f, paint)

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    /**
     * Generates a styled A4 PDF document programmatically using Canvas drawing.
     */
    private fun generatePdfFile(context: Context, month: String, year: String, records: List<InventoryRecord>, centerName: String = "مركز حي المستنصرية الصحي"): File {
        val file = File(context.cacheDir, "تقرير_جرد_اللقاحات_${month}_${year}.pdf")
        val fos = FileOutputStream(file)
        writeReportToStream(context, month, year, records, fos, centerName)
        fos.close()
        return file
    }

    private fun getLogoBase64(context: Context): String {
        return try {
            val logoResId = context.resources.getIdentifier("img_clean_i7san_app_icon_1782895431554", "drawable", context.packageName)
            if (logoResId != 0) {
                val logoBitmap = BitmapFactory.decodeResource(context.resources, logoResId)
                if (logoBitmap != null) {
                    val baos = java.io.ByteArrayOutputStream()
                    logoBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val bytes = baos.toByteArray()
                    android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                } else ""
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Generates a beautifully styled HTML string for PDF Print Preview rendering.
     */
    private fun generateReportHtml(context: Context, month: String, year: String, records: List<InventoryRecord>, centerName: String = "مركز حي المستنصرية الصحي"): String {
        val logoBase64 = getLogoBase64(context)
        val totalVaccines = records.filter { it.itemType == "vaccine" }.sumOf { it.totalAvailable }
        val totalVaccinated = records.sumOf { it.totalVaccinated }
        val totalWasted = records.sumOf { it.totalWasted }
        val totalSupplies = records.filter { it.itemType == "supply" }.sumOf { it.totalAvailable }
        val wasteRate = if (totalVaccines > 0) "%.1f".format((totalWasted.toFloat() / totalVaccines.toFloat()) * 100) else "0"

        val vaccinesRows = records.filter { it.itemType == "vaccine" }.joinToString("") { rec ->
            """
            <tr>
                <td>${rec.itemName}</td>
                <td class="num">${rec.initialBalance}</td>
                <td class="num">${rec.receivedDuringMonth}</td>
                <td class="num font-bold">${rec.totalAvailable}</td>
                <td class="num">${rec.fixedSessionVaccinated}</td>
                <td class="num">${rec.outreachSessionVaccinated}</td>
                <td class="num font-bold">${rec.totalVaccinated}</td>
                <td class="num wasted">${rec.openedWasted}</td>
                <td class="num wasted">${rec.closedWasted}</td>
                <td class="num font-bold wasted">${rec.totalWasted}</td>
                <td class="num">${rec.transferredOut}</td>
                <td class="num final-rem">${rec.finalRemaining}</td>
                <td>${rec.expiryDate ?: "-"}</td>
            </tr>
            """.trimIndent()
        }

        val suppliesRows = records.filter { it.itemType == "supply" }.joinToString("") { rec ->
            """
            <tr>
                <td>${rec.itemName}</td>
                <td class="num">${rec.initialBalance}</td>
                <td class="num">${rec.receivedDuringMonth}</td>
                <td class="num font-bold">${rec.totalAvailable}</td>
                <td class="num">${rec.fixedSessionVaccinated}</td>
                <td class="num">${rec.outreachSessionVaccinated}</td>
                <td class="num font-bold">${rec.totalVaccinated}</td>
                <td class="num wasted">${rec.openedWasted}</td>
                <td class="num wasted">${rec.closedWasted}</td>
                <td class="num font-bold wasted">${rec.totalWasted}</td>
                <td class="num">${rec.transferredOut}</td>
                <td class="num final-rem">${rec.finalRemaining}</td>
                <td>${rec.expiryDate ?: "-"}</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html dir="rtl">
        <head>
            <meta charset="utf-8">
            <title>تقرير جرد اللقاحات ومستلزمات التلقيح</title>
            <style>
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif, 'Cairo';
                    margin: 20px;
                    color: #333;
                    background-color: #fff;
                }
                .header {
                    text-align: center;
                    border-bottom: 3px double #1976D2;
                    padding-bottom: 10px;
                    margin-bottom: 20px;
                }
                .header h1 {
                    margin: 0;
                    font-size: 22px;
                    color: #1976D2;
                }
                .header h2 {
                    margin: 5px 0;
                    font-size: 16px;
                    color: #555;
                }
                .meta-table {
                    width: 100%;
                    margin-bottom: 20px;
                    border: none;
                }
                .meta-table td {
                    border: none;
                    padding: 4px;
                    font-size: 13px;
                }
                .stats-grid {
                    display: table;
                    width: 100%;
                    margin-bottom: 25px;
                    border-collapse: separate;
                    border-spacing: 10px;
                }
                .stats-card {
                    display: table-cell;
                    background-color: #f0f4f8;
                    border: 1px solid #d0d7de;
                    border-radius: 6px;
                    padding: 12px;
                    text-align: center;
                    width: 25%;
                }
                .stats-card h3 {
                    margin: 0 0 5px 0;
                    font-size: 12px;
                    color: #555;
                }
                .stats-card p {
                    margin: 0;
                    font-size: 18px;
                    font-weight: bold;
                    color: #1976D2;
                }
                table.data-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 10px;
                    margin-bottom: 30px;
                    font-size: 11px;
                }
                table.data-table th {
                    background-color: #1976D2;
                    color: #fff;
                    border: 1px solid #115293;
                    padding: 6px;
                    font-weight: bold;
                }
                table.data-table td {
                    border: 1px solid #d0d7de;
                    padding: 5px;
                }
                table.data-table tr:nth-child(even) {
                    background-color: #f6f8fa;
                }
                .num {
                    text-align: center;
                }
                .font-bold {
                    font-weight: bold;
                }
                .wasted {
                    color: #b71c1c;
                }
                .final-rem {
                    color: #1565c0;
                    font-weight: bold;
                }
                .section-title {
                    font-size: 14px;
                    font-weight: bold;
                    color: #1976D2;
                    border-right: 4px solid #1976D2;
                    padding-right: 8px;
                    margin-top: 20px;
                    margin-bottom: 8px;
                }
                .footer {
                    text-align: center;
                    margin-top: 40px;
                    font-size: 10px;
                    color: #777;
                    border-top: 1px solid #ddd;
                    padding-top: 10px;
                }
            </style>
        </head>
        <body>
            <div class="header" style="display: flex; align-items: center; justify-content: center; gap: 18px; border-bottom: 3px double #1976D2; padding-bottom: 12px; margin-bottom: 20px;">
                ${if (logoBase64.isNotEmpty()) """<img src="data:image/jpeg;base64,$logoBase64" style="width: 70px; height: 70px; border-radius: 12px;" />""" else ""}
                <div style="text-align: right;">
                    <h1 style="margin: 0; font-size: 24px; color: #1976D2; font-weight: bold;">تطبيق إحسان لإدارة التلقيح والتحصين</h1>
                    <div style="font-size: 13px; color: #666; margin-top: 4px;">نظام الرصد والتحصين الصحي المتكامل للأطفال واللقاحات</div>
                </div>
            </div>

            <table class="meta-table">
                <tr>
                    <td><strong>المركز الصحي:</strong> $centerName</td>
                    <td style="text-align: left;"><strong>التاريخ والوقت:</strong> ${SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(Calendar.getInstance().time)}</td>
                </tr>
                <tr>
                    <td><strong>فترة التقرير:</strong> شهر $month $year</td>
                    <td style="text-align: left;"><strong>الحالة:</strong> جرد دوري مكتمل</td>
                </tr>
            </table>

            <div class="section-title">أولاً: اللقاحات الحيوية (Vaccines Inventory)</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th rowspan="2">اسم المادة / اللقاح</th>
                        <th rowspan="2">رصيد أول</th>
                        <th rowspan="2">المستلم</th>
                        <th rowspan="2">الكلي المتاح</th>
                        <th colspan="3">الملقحين</th>
                        <th colspan="3">الهدر والتالف</th>
                        <th rowspan="2">مناقلة مغادرة</th>
                        <th rowspan="2">المتبقي الصافي</th>
                        <th rowspan="2">تاريخ الصلاحية</th>
                    </tr>
                    <tr>
                        <th>جلسة ثابتة</th>
                        <th>جلسة خارجية</th>
                        <th>المجموع</th>
                        <th>مفتوح</th>
                        <th>مغلق</th>
                        <th>المجموع</th>
                    </tr>
                </thead>
                <tbody>
                    $vaccinesRows
                </tbody>
            </table>

            <div class="section-title">ثانياً: مستلزمات التلقيح والسرنجات والأمان (Supplies Inventory)</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th rowspan="2">اسم المادة / المستلزم</th>
                        <th rowspan="2">رصيد أول</th>
                        <th rowspan="2">المستلم</th>
                        <th rowspan="2">الكلي المتاح</th>
                        <th colspan="3">المستهلك في التلقيح</th>
                        <th colspan="3">التالف والهدر</th>
                        <th rowspan="2">مناقلة مغادرة</th>
                        <th rowspan="2">المتبقي الصافي</th>
                        <th rowspan="2">تاريخ الصلاحية</th>
                    </tr>
                    <tr>
                        <th>جلسة ثابتة</th>
                        <th>جلسة خارجية</th>
                        <th>المجموع</th>
                        <th>مفتوح</th>
                        <th>مغلق</th>
                        <th>المجموع</th>
                    </tr>
                </thead>
                <tbody>
                    $suppliesRows
                </tbody>
            </table>

            <div class="footer">
                تم استخراج وطباعة هذا التقرير تلقائياً عبر تطبيق إحسان لإدارة التلقيح والتحصين 🌟
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Generates a styled A4 PDF document for the child's vaccination card and writes it directly to the provided OutputStream.
     */
    fun shareChildCardAsPdf(
        context: Context,
        child: com.example.data.Child,
        records: List<com.example.data.VaccineRecord>
    ) {
        try {
            val fileName = "vaccination_card_${child.fullName.replace(" ", "_")}.pdf"
            val file = java.io.File(context.cacheDir, fileName)
            file.outputStream().use { outputStream ->
                writeVaccinationCardToStream(context, child, records, outputStream)
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "بطاقة التلقيح للطفل ${child.fullName}")
                putExtra(android.content.Intent.EXTRA_TEXT, "مرفق بطاقة التلقيح للطفل ${child.fullName}")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "مشاركة البطاقة عبر..."))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "خطأ أثناء توليد ملف المشاركة: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun writeVaccinationCardToStream(
        context: Context,
        child: com.example.data.Child,
        records: List<com.example.data.VaccineRecord>,
        outputStream: java.io.OutputStream
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Background color based on gender
        val backgroundColor = if (child.gender == "ذكر") 0xFFE3F2FD.toInt() else 0xFFFCE4EC.toInt()
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 150f, paint)

        // General settings
        paint.color = 0xFF000000.toInt()
        paint.textSize = 14f
        paint.textAlign = Paint.Align.RIGHT

        // Header info
        val yOffsetHeader = 30f
        val lineSpacing = 30f

        // Right side (Name, DOB, Center)
        paint.isFakeBoldText = true
        canvas.drawText("الاسم الثلاثي: ${child.fullName}", 560f, yOffsetHeader, paint)
        canvas.drawText("تاريخ الميلاد: ${child.dob}", 560f, yOffsetHeader + lineSpacing, paint)
        canvas.drawText("اسم المركز الصحي المعتمد: المركز الصحي", 560f, yOffsetHeader + 2 * lineSpacing, paint)

        // Left side (Vac Num, Family Num, Address)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("الرقم التلقيحي: ${child.vaccinationNumber}", 40f, yOffsetHeader, paint)
        canvas.drawText("رقم الأسرة: ${child.familyNumber}", 40f, yOffsetHeader + lineSpacing, paint)
        canvas.drawText("عنوان السكن: ${child.address}", 40f, yOffsetHeader + 2 * lineSpacing, paint)

        // Middle box (Child Photo in Circle with Progress Ring)
        val centerX = 595f / 2
        val centerY = 75f
        val radius = 50f
        
        // Progress ring
        val totalVaccines = records.size
        val takenVaccines = records.count { it.isTaken }
        val progress = if (totalVaccines > 0) takenVaccines.toFloat() / totalVaccines else 0f
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = 0xFFCCCCCC.toInt() // Gray background arc
        canvas.drawCircle(centerX, centerY, radius + 3f, paint)
        
        paint.color = 0xFF4CAF50.toInt() // Green progress arc
        canvas.drawArc(centerX - (radius + 3f), centerY - (radius + 3f), centerX + (radius + 3f), centerY + (radius + 3f), -90f, 360f * progress, false, paint)

        // Photo in Circle
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Clipping for circular photo
        canvas.save()
        val path = android.graphics.Path()
        path.addCircle(centerX, centerY, radius, android.graphics.Path.Direction.CW)
        canvas.clipPath(path)

        if (!child.photoUri.isNullOrEmpty()) {
            try {
                val uri = android.net.Uri.parse(child.photoUri)
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
                    canvas.drawBitmap(scaledBitmap, centerX - 50f, centerY - 50f, paint)
                }
            } catch (e: Exception) {}
        }
        canvas.restore() // Undo clipping

        // Title
        val yTitle = 180f
        paint.color = 0xFFB71C1C.toInt()
        paint.textSize = 18f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("◀ جدول اللقاحات الوطني للاطفال في العراق ▶", 595f / 2, yTitle, paint)

        // Table Header
        var yTable = 210f
        paint.color = 0xFF607D8B.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(20f, yTable - 20f, 575f, yTable + 10f, paint) // Header rect

        paint.style = Paint.Style.FILL
        paint.color = 0xFF000000.toInt()
        paint.textSize = 12f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER

        val col1x = 520f // Next date
        val col2x = 420f // Taken date
        val col3x = 300f // Age
        val col4x = 150f // Vaccine type
        val col5x = 50f  // Checkmark

        // Drawing header text
        canvas.drawText("موعد التلقيح", col1x, yTable - 5f, paint)
        canvas.drawText("تاريخ التلقيح", col2x, yTable - 5f, paint)
        canvas.drawText("العمر عند التلقيح", col3x, yTable - 5f, paint)
        canvas.drawText("نوع اللقاح ورقم الجرعة", col4x, yTable - 5f, paint)
        canvas.drawText("إعطاء اللقاح", col5x, yTable - 5f, paint)

        // Table content
        paint.isFakeBoldText = false
        yTable += 10f

        records.forEach { record ->
            yTable += 25f
            val takenDate = record.takenDate ?: "-"
            val isTakenMark = if (record.isTaken) "✓" else ""

            canvas.drawText("-", col1x, yTable, paint)
            canvas.drawText(takenDate, col2x, yTable, paint)
            canvas.drawText(record.categoryAr, col3x, yTable, paint)
            canvas.drawText(record.nameAr, col4x, yTable, paint)
            canvas.drawText(isTakenMark, col5x, yTable, paint)

            paint.color = 0xFF607D8B.toInt()
            canvas.drawLine(20f, yTable + 5f, 575f, yTable + 5f, paint)
            paint.color = 0xFF000000.toInt()
        }

        // Draw vertical lines for the table
        paint.color = 0xFF607D8B.toInt()
        val bottomY = yTable + 5f
        canvas.drawLine(575f, 190f, 575f, bottomY, paint) // right border
        canvas.drawLine(470f, 190f, 470f, bottomY, paint) // vertical
        canvas.drawLine(360f, 190f, 360f, bottomY, paint) // vertical
        canvas.drawLine(220f, 190f, 220f, bottomY, paint) // vertical
        canvas.drawLine(80f, 190f, 80f, bottomY, paint)  // vertical
        canvas.drawLine(20f, 190f, 20f, bottomY, paint)  // left border

        // Footer notes
        yTable += 30f
        paint.color = 0xFF000000.toInt()
        paint.textSize = 12f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText("تم طباعة الكارت التفاعلي عن طريق تطبيق احسان", 595f / 2, yTable, paint)
        
        yTable += 20f
        try {
            val logoResId = context.resources.getIdentifier("img_clean_i7san_app_icon_1782895431554", "drawable", context.packageName)
            if (logoResId != 0) {
                val logoBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, logoResId)
                if (logoBitmap != null) {
                    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 60, 60, true)
                    canvas.drawBitmap(scaledLogo, (595f / 2) - 30f, yTable, paint)
                }
            }
        } catch (e: Exception) {}

        pdfDocument.finishPage(page)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    fun printChildReportAsHtml(context: Context, child: com.example.data.Child, vaccines: List<com.example.data.VaccineRecord>, centerName: String = "المركز الصحي المعتمد") {
        val jobName = "سجل_تلقيحات_${child.fullName.replace(" ", "_")}"
        val htmlContent = generateChildReportHtml(context, child, vaccines, centerName)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                try {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                } catch (e: Exception) {
                    Toast.makeText(context, "فشلت عملية الطباعة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }

    private fun generateChildReportHtml(context: Context, child: com.example.data.Child, vaccines: List<com.example.data.VaccineRecord>, centerName: String): String {
        val logoBase64 = getLogoBase64(context)
        var photoHtml = "<div class=\"child-image-box\">صورة الطفل</div>"
        if (!child.photoUri.isNullOrEmpty()) {
            try {
                val uri = android.net.Uri.parse(child.photoUri)
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bitmap != null) {
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val photoBase64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                    photoHtml = "<img src=\"data:image/jpeg;base64,$photoBase64\" alt=\"صورة الطفل\" style=\"width: 90px; height: 110px; object-fit: cover; border: 2px solid #ff0000;\" />"
                }
            } catch (e: Exception) {
                // Ignore and use default box
            }
        }

        val tableRows = vaccines.joinToString("\n") { record ->
            val takenDate = record.takenDate ?: "-"
            val isTakenMark = if (record.isTaken) "✓" else ""
            """
            <tr>
                <td style="text-align: center; font-weight: bold; font-size: 14px;">$isTakenMark</td>
                <td>${record.nameAr}</td>
                <td>${record.categoryAr}</td>
                <td>$takenDate</td>
                <td>-</td>
            </tr>
            """.trimIndent()
        }

        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Tajawal', 'Arial', sans-serif; padding: 10px; color: #000; line-height: 1.4; font-size: 12px; }
                    .header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
                    .header-col { flex: 1; padding: 5px; font-size: 14px; font-weight: bold; line-height: 1.8; }
                    .child-image-box { border: 2px dashed #ff0000; color: #ff0000; width: 90px; height: 110px; display: flex; align-items: center; justify-content: center; font-weight: bold; margin: 0 auto; }
                    .title-container { text-align: center; margin-top: 10px; margin-bottom: 10px; }
                    .title { font-size: 18px; font-weight: bold; color: #b71c1c; display: inline-flex; align-items: center; }
                    .title::before, .title::after { content: '◀'; color: #b71c1c; font-size: 12px; margin: 0 5px; }
                    table { width: 100%; border-collapse: collapse; border: 2px solid #607d8b; }
                    th, td { border: 1px solid #607d8b; padding: 4px; text-align: center; }
                    th { background-color: #e0f2f1; font-weight: bold; font-size: 11px; }
                    .footer-notes { margin-top: 10px; font-size: 11px; line-height: 1.6; }
                    .footer-notes h4 { color: #b71c1c; display: inline-flex; align-items: center; margin: 0 0 5px 0; }
                    .footer-notes h4::before { content: '◀'; color: #b71c1c; font-size: 10px; margin-left: 5px; }
                    .footer-notes ol { margin: 0; padding-right: 20px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="header-col" style="text-align: right;">
                        <div style="margin-bottom: 10px;">الاسم الثلاثي: ${child.fullName}</div>
                        <div style="margin-bottom: 10px;">تاريخ الميلاد: ${child.dob}</div>
                        <div>اسم المركز<br>الصحي المعتمد: $centerName</div>
                    </div>
                    
                    <div class="header-col" style="text-align: center;">
                        $photoHtml
                    </div>
                    
                    <div class="header-col" style="text-align: right;">
                        <div style="margin-bottom: 10px;">الرقم التلقيحي<br>للطفل: ${child.vaccinationNumber}</div>
                        <div style="margin-bottom: 10px;">رقم الأسرة: ${child.familyNumber}</div>
                        <div>عنوان السكن: ${child.address}</div>
                    </div>
                </div>

                <div class="title-container">
                    <div class="title">جدول اللقاحات الوطني للاطفال في العراق</div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th style="width: 10%;">ضع ✓<br>عـنـد<br>إعطاء<br>اللقاح</th>
                            <th style="width: 35%;">نوع اللقاح ورقم الجرعة</th>
                            <th style="width: 20%;">العمر عند التلقيح</th>
                            <th style="width: 15%;">تاريخ التلقيح</th>
                            <th style="width: 20%;">موعد التلقيح القادم</th>
                        </tr>
                    </thead>
                    <tbody>
                        $tableRows
                    </tbody>
                </table>

                <div class="footer-notes">
                    <p style="text-align: center; font-weight: bold; font-size: 14px;">تم انشاء هذا الجدول عن طريق تطبيق إحسان لإدارة اللقاحات ومتابعة تطعيمات الأطفال</p>
                    <div style="text-align: center; margin-top: 10px;">
                        <img src="$logoBase64" alt="شعار التطبيق" style="width: 60px; height: 60px;" />
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
