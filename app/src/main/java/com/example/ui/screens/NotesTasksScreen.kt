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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.SubjectEntity
import com.example.ui.i18n.AppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NotesTasksScreen(
  strings: AppStrings,
  tasks: List<StudyTaskEntity>,
  notes: List<StudyNoteEntity>,
  subjects: List<SubjectEntity>,
  onSaveTask: (StudyTaskEntity) -> Unit,
  onToggleTask: (StudyTaskEntity) -> Unit,
  onDeleteTask: (String) -> Unit,
  onSaveNote: (StudyNoteEntity) -> Unit,
  onTogglePinNote: (StudyNoteEntity) -> Unit,
  onDeleteNote: (String) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var showAddTaskDialog by remember { mutableStateOf(false) }
  var showAddNoteDialog by remember { mutableStateOf(false) }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          if (selectedTab == 0) showAddTaskDialog = true else showAddNoteDialog = true
        },
        modifier = Modifier.testTag("add_note_or_task_fab")
      ) {
        Icon(Icons.Filled.Add, contentDescription = if (selectedTab == 0) strings.addTask else strings.addNote)
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Text(
        text = strings.notesAndTasksTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      )

      TabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("${strings.tabTasks} (${tasks.size})") }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("${strings.tabNotes} (${notes.size})") }
        )
      }

      if (selectedTab == 0) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(tasks, key = { it.id }) { task ->
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = task.isCompleted,
                  onCheckedChange = { onToggleTask(task) }
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                  )
                  val typeLabel = when (task.taskType) {
                    "assignment" -> strings.taskTypeAssignment
                    "chapter_task" -> strings.taskTypeChapter
                    "revision" -> strings.taskTypeRevision
                    else -> strings.taskTypeHomework
                  }
                  val deadlineStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(task.deadlineMillis))
                  Text(
                    text = listOf(task.subjectName, task.chapterName, typeLabel, deadlineStr).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                IconButton(onClick = { onDeleteTask(task.id) }) {
                  Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(notes, key = { it.id }) { note ->
            Card(
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (note.isPinned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
              ),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (note.subjectName.isNotBlank()) {
                      Text(note.subjectName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                  }
                  Row {
                    IconButton(onClick = { onTogglePinNote(note) }) {
                      Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                      )
                    }
                    IconButton(onClick = { onDeleteNote(note.id) }) {
                      Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                  }
                }
                if (note.content.isNotBlank()) {
                  Text(note.content, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddTaskDialog) {
    AddTaskDialog(
      strings = strings,
      subjects = subjects,
      onDismiss = { showAddTaskDialog = false },
      onConfirm = {
        onSaveTask(it)
        showAddTaskDialog = false
      }
    )
  }

  if (showAddNoteDialog) {
    AddNoteDialog(
      strings = strings,
      subjects = subjects,
      onDismiss = { showAddNoteDialog = false },
      onConfirm = {
        onSaveNote(it)
        showAddNoteDialog = false
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTaskDialog(
  strings: AppStrings,
  subjects: List<SubjectEntity>,
  onDismiss: () -> Unit,
  onConfirm: (StudyTaskEntity) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
  var chapterName by remember { mutableStateOf("") }
  var taskType by remember { mutableStateOf("homework") }
  var priority by remember { mutableStateOf("medium") }
  var deadlineMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 86_400_000L) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addTask) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.taskTitleLabel) },
          modifier = Modifier.fillMaxWidth().testTag("task_title_input")
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
        OutlinedTextField(
          value = chapterName,
          onValueChange = { chapterName = it },
          label = { Text(strings.chapterNameLabel) },
          modifier = Modifier.fillMaxWidth()
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(
            "homework" to strings.taskTypeHomework,
            "assignment" to strings.taskTypeAssignment,
            "chapter_task" to strings.taskTypeChapter,
            "revision" to strings.taskTypeRevision
          ).forEach { (key, label) ->
            FilterChip(
              selected = taskType == key,
              onClick = { taskType = key },
              label = { Text(label) }
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("high" to strings.priorityHigh, "medium" to strings.priorityMedium, "low" to strings.priorityLow).forEach { (k, l) ->
            FilterChip(selected = priority == k, onClick = { priority = k }, label = { Text(l) })
          }
        }
        OutlinedButton(
          onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = deadlineMillis }
            DatePickerDialog(
              context,
              { _, y, m, d ->
                val c = Calendar.getInstance().apply { set(y, m, d, 18, 0, 0) }
                deadlineMillis = c.timeInMillis
              },
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH),
              cal.get(Calendar.DAY_OF_MONTH)
            ).show()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("${strings.selectDate}: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(deadlineMillis))}")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val clean = title.trim().ifBlank { return@Button }
          onConfirm(
            StudyTaskEntity(
              subjectId = selectedSubject?.id.orEmpty(),
              subjectName = selectedSubject?.name.orEmpty(),
              chapterName = chapterName.trim(),
              title = clean,
              taskType = taskType,
              deadlineMillis = deadlineMillis,
              priority = priority
            )
          )
        },
        modifier = Modifier.testTag("save_task_confirm_button")
      ) { Text(strings.save) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddNoteDialog(
  strings: AppStrings,
  subjects: List<SubjectEntity>,
  onDismiss: () -> Unit,
  onConfirm: (StudyNoteEntity) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }
  var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addNote) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.noteTitleLabel) },
          modifier = Modifier.fillMaxWidth().testTag("note_title_input")
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
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text(strings.noteContentLabel) },
          minLines = 4,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val clean = title.trim().ifBlank { return@Button }
          onConfirm(
            StudyNoteEntity(
              subjectId = selectedSubject?.id.orEmpty(),
              subjectName = selectedSubject?.name.orEmpty(),
              title = clean,
              content = content.trim()
            )
          )
        },
        modifier = Modifier.testTag("save_note_confirm_button")
      ) { Text(strings.save) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
  )
}
