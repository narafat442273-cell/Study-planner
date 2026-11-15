package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.RoutineEntity
import java.io.File
import java.io.FileOutputStream

object PdfRoutineExporter {
  fun exportRoutineToPdf(
    context: Context,
    studentName: String,
    routines: List<RoutineEntity>,
    isBengali: Boolean
  ): File {
    val pdf = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdf.startPage(pageInfo)
    var canvas = page.canvas

    val titlePaint = Paint().apply {
      color = Color.rgb(27, 59, 111)
      textSize = 20f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val subPaint = Paint().apply {
      color = Color.rgb(71, 85, 105)
      textSize = 12f
    }
    val rowTitlePaint = Paint().apply {
      color = Color.rgb(15, 23, 42)
      textSize = 13f
      typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val rowBodyPaint = Paint().apply {
      color = Color.rgb(51, 65, 85)
      textSize = 11f
    }
    val dividerPaint = Paint().apply {
      color = Color.rgb(203, 213, 225)
      strokeWidth = 1f
    }

    var y = 52f
    val headerTitle = if (isBengali) "Study Planner – স্মার্ট পড়াশোনা রুটিন" else "Study Planner – Smart Study Routine"
    val studentLine = if (isBengali) "শিক্ষার্থী: ${studentName.ifBlank { "Student" }}" else "Student: ${studentName.ifBlank { "Student" }}"
    canvas.drawText(headerTitle, 40f, y, titlePaint)
    y += 22f
    canvas.drawText(studentLine, 40f, y, subPaint)
    y += 14f
    canvas.drawLine(40f, y, pageWidth - 40f, y, dividerPaint)
    y += 24f

    val sorted = routines.sortedWith(compareBy({ it.sortOrder }, { it.startTimeMinutes }))
    if (sorted.isEmpty()) {
      val emptyMsg = if (isBengali) "কোনো রুটিন যোগ করা হয়নি।" else "No study routines scheduled yet."
      canvas.drawText(emptyMsg, 40f, y, rowBodyPaint)
    } else {
      for ((idx, item) in sorted.withIndex()) {
        if (y > pageHeight - 80f) {
          pdf.finishPage(page)
          pageNumber++
          pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
          page = pdf.startPage(pageInfo)
          canvas = page.canvas
          y = 52f
        }
        val timeRange = "${formatTime(item.startTimeMinutes)} - ${formatTime(item.endTimeMinutes)}"
        val repeatLabel = when (item.repeatMode) {
          "daily" -> if (isBengali) "প্রতিদিন" else "Daily"
          "weekly" -> if (isBengali) "সাপ্তাহিক" else "Weekly"
          "custom" -> if (isBengali) "নির্বাচিত দিন" else "Custom days"
          else -> if (isBengali) "একবার" else "Once"
        }
        canvas.drawText("${idx + 1}. ${item.subjectName} ($timeRange)", 40f, y, rowTitlePaint)
        y += 16f
        val detail = if (isBengali) {
          "সময়কাল: ${item.durationMinutes} মি. | বিরতি: ${item.breakMinutes} মি. | ধরন: $repeatLabel"
        } else {
          "Duration: ${item.durationMinutes} min | Break: ${item.breakMinutes} min | Repeat: $repeatLabel"
        }
        canvas.drawText(detail, 54f, y, rowBodyPaint)
        y += 15f
        if (item.notes.isNotBlank()) {
          val noteLine = if (isBengali) "নোট: ${item.notes.take(85)}" else "Notes: ${item.notes.take(85)}"
          canvas.drawText(noteLine, 54f, y, rowBodyPaint)
          y += 15f
        }
        canvas.drawLine(40f, y, pageWidth - 40f, y, dividerPaint)
        y += 18f
      }
    }

    pdf.finishPage(page)

    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val outFile = File(exportDir, "study_routine_${System.currentTimeMillis()}.pdf")
    FileOutputStream(outFile).use { fos ->
      pdf.writeTo(fos)
    }
    pdf.close()
    return outFile
  }

  fun sharePdf(context: Context, file: File, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
  }

  fun shareRoutineText(context: Context, studentName: String, routines: List<RoutineEntity>, isBengali: Boolean) {
    val header = if (isBengali) {
      "📚 ${studentName.ifBlank { "আমার" }} পড়াশোনার রুটিন (Study Planner):\n\n"
    } else {
      "📚 ${studentName.ifBlank { "My" }} Study Routine (Study Planner):\n\n"
    }
    val body = routines.sortedWith(compareBy({ it.sortOrder }, { it.startTimeMinutes })).mapIndexed { idx, r ->
      val time = "${formatTime(r.startTimeMinutes)} - ${formatTime(r.endTimeMinutes)}"
      "${idx + 1}. ${r.subjectName} ($time • ${r.durationMinutes}m)"
    }.joinToString("\n")
    val shareText = header + body.ifBlank { if (isBengali) "রুটিন খালি আছে" else "Routine is empty" }
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, if (isBengali) "রুটিন শেয়ার করুন" else "Share Routine"))
  }

  fun formatTime(totalMinutes: Int): String {
    val safe = totalMinutes.coerceIn(0, 1439)
    val hour24 = safe / 60
    val min = safe % 60
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
    return String.format("%d:%02d %s", hour12, min, amPm)
  }
}
