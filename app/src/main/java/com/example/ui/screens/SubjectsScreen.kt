package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SubjectEntity
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SubjectColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SubjectsScreen(
  strings: AppStrings,
  subjects: List<SubjectEntity>,
  onSaveSubject: (SubjectEntity) -> Unit,
  onToggleChapter: (SubjectEntity, String) -> Unit,
  onDeleteSubject: (String) -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }
  var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingSubject = null
          showDialog = true
        },
        modifier = Modifier.testTag("add_subject_fab")
      ) {
        Icon(Icons.Filled.Add, contentDescription = strings.addSubject)
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = strings.subjectsTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(12.dp))

      if (subjects.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
              text = if (strings.isBengali) "এখনো কোনো বিষয় যোগ করা হয়নি" else "No subjects added yet",
              style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = {
              editingSubject = null
              showDialog = true
            }) {
              Text(strings.addSubject)
            }
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(subjects, key = { it.id }) { subject ->
            Card(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(16.dp)
                      .clip(CircleShape)
                      .background(Color(subject.colorHex))
                  )
                  Column(modifier = Modifier.weight(1f)) {
                    Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val priorityLabel = when (subject.priority) {
                      "high" -> strings.priorityHigh
                      "low" -> strings.priorityLow
                      else -> strings.priorityMedium
                    }
                    val examStr = if (subject.examDateMillis > 0L) {
                      " • ${strings.examDateLabel}: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(subject.examDateMillis))}"
                    } else ""
                    Text(
                      text = "${strings.priorityLabel}: $priorityLabel$examStr",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Text(
                    text = "${subject.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(subject.colorHex)
                  )
                  IconButton(onClick = {
                    editingSubject = subject
                    showDialog = true
                  }) {
                    Icon(Icons.Filled.Edit, contentDescription = strings.editSubject)
                  }
                  IconButton(onClick = { onDeleteSubject(subject.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                  }
                }

                LinearProgressIndicator(
                  progress = { subject.progressPercent / 100f },
                  color = Color(subject.colorHex),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                )

                if (subject.chapters.isNotEmpty()) {
                  Text(strings.chaptersLabel, style = MaterialTheme.typography.labelLarge)
                  subject.chapters.forEach { chapter ->
                    val checked = chapter in subject.completedChapters
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleChapter(subject, chapter) }
                        .padding(vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleChapter(subject, chapter) }
                      )
                      Text(
                        text = chapter,
                        style = MaterialTheme.typography.bodyMedium
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showDialog) {
    SubjectEditorDialog(
      strings = strings,
      existing = editingSubject,
      onDismiss = { showDialog = false },
      onConfirm = { sub ->
        onSaveSubject(sub)
        showDialog = false
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectEditorDialog(
  strings: AppStrings,
  existing: SubjectEntity?,
  onDismiss: () -> Unit,
  onConfirm: (SubjectEntity) -> Unit
) {
  val context = LocalContext.current
  var name by remember { mutableStateOf(existing?.name.orEmpty()) }
  var selectedColor by remember { mutableLongStateOf(existing?.colorHex ?: SubjectColors.first()) }
  var priority by remember { mutableStateOf(existing?.priority ?: "medium") }
  var examDateMillis by remember { mutableLongStateOf(existing?.examDateMillis ?: 0L) }
  var chapters by remember { mutableStateOf(existing?.chapters ?: emptyList()) }
  var newChapter by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) strings.addSubject else strings.editSubject) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.subjectName) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_name_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SubjectColors.forEach { colorHex ->
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(colorHex))
                .border(
                  width = if (selectedColor == colorHex) 3.dp else 0.dp,
                  color = MaterialTheme.colorScheme.onSurface,
                  shape = CircleShape
                )
                .clickable { selectedColor = colorHex }
            )
          }
        }

        Text(strings.priorityLabel, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("high" to strings.priorityHigh, "medium" to strings.priorityMedium, "low" to strings.priorityLow).forEach { (key, label) ->
            FilterChip(
              selected = priority == key,
              onClick = { priority = key },
              label = { Text(label) }
            )
          }
        }

        OutlinedButton(
          onClick = {
            val cal = Calendar.getInstance().apply {
              if (examDateMillis > 0L) timeInMillis = examDateMillis
            }
            DatePickerDialog(
              context,
              { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d, 9, 0, 0) }
                examDateMillis = picked.timeInMillis
              },
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH),
              cal.get(Calendar.DAY_OF_MONTH)
            ).show()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          val dateStr = if (examDateMillis > 0L) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(examDateMillis))
          } else {
            strings.selectDate
          }
          Text("${strings.examDateLabel}: $dateStr")
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedTextField(
            value = newChapter,
            onValueChange = { newChapter = it },
            label = { Text(strings.addChapterPlaceholder) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = {
            val clean = newChapter.trim()
            if (clean.isNotEmpty() && clean !in chapters) {
              chapters = chapters + clean
              newChapter = ""
            }
          }) {
            Icon(Icons.Filled.Add, contentDescription = strings.chaptersLabel)
          }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          chapters.forEach { ch ->
            AssistChip(
              onClick = { chapters = chapters - ch },
              label = { Text("$ch ×") }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalName = name.trim().ifBlank { return@Button }
          val base = existing ?: SubjectEntity()
          onConfirm(
            base.copy(
              name = finalName,
              colorHex = selectedColor,
              priority = priority,
              examDateMillis = examDateMillis,
              chapters = chapters
            )
          )
        },
        modifier = Modifier.testTag("save_subject_confirm_button")
      ) {
        Text(strings.save)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel) }
    }
  )
}
