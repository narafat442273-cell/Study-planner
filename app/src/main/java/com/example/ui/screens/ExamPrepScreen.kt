package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ExamEntity
import com.example.data.local.SubjectEntity
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExamPrepScreen(
  strings: AppStrings,
  exams: List<ExamEntity>,
  subjects: List<SubjectEntity>,
  onSaveExam: (ExamEntity) -> Unit,
  onDeleteExam: (String) -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showDialog = true },
        modifier = Modifier.testTag("add_exam_fab")
      ) {
        Icon(Icons.Filled.Add, contentDescription = strings.addExam)
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Text(strings.examPrepTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      }

      if (exams.isEmpty()) {
        item {
          Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = if (strings.isBengali) "কোনো পরীক্ষার সময়সূচি নেই। পরীক্ষার তারিখ, রিভিশন ও মক টেস্ট যোগ করুন!" else "No exams scheduled yet. Add exam dates, revision plans & mock tests!",
                style = MaterialTheme.typography.bodyMedium
              )
              Button(onClick = { showDialog = true }) { Text(strings.addExam) }
            }
          }
        }
      } else {
        items(exams, key = { it.id }) { exam ->
          val daysLeft = ((exam.examDateMillis - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0L)
          val typeLabel = when (exam.examType) {
            "semester" -> strings.examTypeSemester
            "mock_test" -> strings.examTypeMock
            "class_test" -> strings.examTypeClassTest
            "admission" -> strings.examTypeAdmission
            else -> strings.examTypeBoard
          }

          Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(exam.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  Text(
                    text = "${exam.subjectName} • $typeLabel • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(exam.examDateMillis))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                  Text(
                    text = "$daysLeft ${strings.daysRemaining}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
                IconButton(onClick = { onDeleteExam(exam.id) }) {
                  Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                }
              }

              if (exam.syllabusNotes.isNotBlank()) {
                Text(exam.syllabusNotes, style = MaterialTheme.typography.bodySmall)
              }

              Text("${strings.prepProgress}: ${exam.preparationPercent}%", style = MaterialTheme.typography.labelMedium)
              LinearProgressIndicator(
                progress = { exam.preparationPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp)
              )
              Slider(
                value = exam.preparationPercent.toFloat(),
                onValueChange = { pct ->
                  onSaveExam(exam.copy(preparationPercent = pct.toInt()))
                },
                valueRange = 0f..100f
              )
              if (exam.examType == "mock_test" || exam.mockScorePercent > 0) {
                Text(
                  text = "${strings.mockScore}: ${exam.mockScorePercent}%",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    AddExamDialog(
      strings = strings,
      subjects = subjects,
      onDismiss = { showDialog = false },
      onConfirm = {
        onSaveExam(it)
        showDialog = false
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddExamDialog(
  strings: AppStrings,
  subjects: List<SubjectEntity>,
  onDismiss: () -> Unit,
  onConfirm: (ExamEntity) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
  var examType by remember { mutableStateOf("board") }
  var examDateMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 7 * 86_400_000L) }
  var syllabusNotes by remember { mutableStateOf("") }
  var mockScore by remember { mutableIntStateOf(0) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addExam) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.examTitleLabel) },
          modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
        )
        if (subjects.isNotEmpty()) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            subjects.forEach { subj ->
              FilterChip(
                selected = selectedSubject?.id == subj.id,
                onClick = { selectedSubject = subj },
                label = { Text(subj.name) }
              )
            }
          }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(
            "board" to strings.examTypeBoard,
            "semester" to strings.examTypeSemester,
            "mock_test" to strings.examTypeMock,
            "class_test" to strings.examTypeClassTest,
            "admission" to strings.examTypeAdmission
          ).forEach { (k, l) ->
            FilterChip(selected = examType == k, onClick = { examType = k }, label = { Text(l) })
          }
        }
        OutlinedButton(
          onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = examDateMillis }
            DatePickerDialog(
              context,
              { _, y, m, d ->
                val c = Calendar.getInstance().apply { set(y, m, d, 10, 0, 0) }
                examDateMillis = c.timeInMillis
              },
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH),
              cal.get(Calendar.DAY_OF_MONTH)
            ).show()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("${strings.examDateLabel}: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(examDateMillis))}")
        }
        OutlinedTextField(
          value = syllabusNotes,
          onValueChange = { syllabusNotes = it },
          label = { Text(strings.notesLabel) },
          modifier = Modifier.fillMaxWidth()
        )
        if (examType == "mock_test") {
          Text("${strings.mockScore}: $mockScore%")
          Slider(value = mockScore.toFloat(), onValueChange = { mockScore = it.toInt() }, valueRange = 0f..100f)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val clean = title.trim().ifBlank { return@Button }
          onConfirm(
            ExamEntity(
              subjectId = selectedSubject?.id.orEmpty(),
              subjectName = selectedSubject?.name.orEmpty(),
              title = clean,
              examType = examType,
              examDateMillis = examDateMillis,
              syllabusNotes = syllabusNotes.trim(),
              mockScorePercent = mockScore
            )
          )
        },
        modifier = Modifier.testTag("save_exam_confirm_button")
      ) { Text(strings.save) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
  )
}
