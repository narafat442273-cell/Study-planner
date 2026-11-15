package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.local.RoutineEntity
import com.example.data.local.SubjectEntity
import com.example.data.repository.RoutineTemplate
import com.example.data.repository.StudyPlannerRepository
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.SubjectColors
import com.example.util.PdfRoutineExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBuilderScreen(
  strings: AppStrings,
  studentName: String,
  routines: List<RoutineEntity>,
  subjects: List<SubjectEntity>,
  onSaveRoutine: (RoutineEntity) -> Unit,
  onDeleteRoutine: (String) -> Unit,
  onToggleCompleted: (RoutineEntity) -> Unit,
  onMoveRoutine: (Int, Int) -> Unit,
  onApplyTemplate: (RoutineTemplate) -> Unit
) {
  val context = LocalContext.current
  var editingRoutine by remember { mutableStateOf<RoutineEntity?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }
  var showTemplatesSheet by remember { mutableStateOf(false) }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingRoutine = null
          showAddDialog = true
        },
        modifier = Modifier.testTag("add_routine_fab")
      ) {
        Icon(Icons.Filled.Add, contentDescription = strings.addRoutine)
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
        text = strings.routineBuilderTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = strings.dragToReorderHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = { showTemplatesSheet = true },
          modifier = Modifier
            .weight(1f)
            .testTag("routine_templates_button"),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Filled.AutoAwesomeMotion, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(strings.templates, maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
          onClick = {
            val file = PdfRoutineExporter.exportRoutineToPdf(context, studentName, routines, strings.isBengali)
            PdfRoutineExporter.sharePdf(context, file, strings.exportPdf)
          },
          modifier = Modifier
            .weight(1f)
            .testTag("routine_export_pdf_button"),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(strings.exportPdf, maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
        OutlinedButton(
          onClick = {
            PdfRoutineExporter.shareRoutineText(context, studentName, routines, strings.isBengali)
          },
          modifier = Modifier
            .weight(1f)
            .testTag("routine_share_button"),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(strings.shareRoutine, maxLines = 1, style = MaterialTheme.typography.labelMedium)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        itemsIndexed(routines, key = { _, item -> item.id }) { index, item ->
          var dragOffsetY by remember(item.id) { mutableFloatStateOf(0f) }

          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
              .fillMaxWidth()
              .offset { IntOffset(0, dragOffsetY.roundToInt()) }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Drag handle + color bar
              Box(
                modifier = Modifier
                  .width(6.dp)
                  .height(56.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(item.colorHex))
              )
              Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = strings.dragToReorderHint,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                  .size(28.dp)
                  .pointerInput(index, routines.size) {
                    detectDragGesturesAfterLongPress(
                      onDragEnd = {
                        val steps = (dragOffsetY / 140f).roundToInt()
                        if (steps != 0) {
                          val target = (index + steps).coerceIn(0, routines.lastIndex)
                          onMoveRoutine(index, target)
                        }
                        dragOffsetY = 0f
                      },
                      onDragCancel = { dragOffsetY = 0f },
                      onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                      }
                    )
                  }
              )

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.subjectName,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${PdfRoutineExporter.formatTime(item.startTimeMinutes)} – ${PdfRoutineExporter.formatTime(item.endTimeMinutes)} (${item.durationMinutes}${strings.minutesShort}, +${item.breakMinutes}${strings.minutesShort} break)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.notes.isNotBlank()) {
                  Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Row {
                  IconButton(
                    onClick = { onToggleCompleted(item) },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(
                      imageVector = if (item.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                      contentDescription = strings.completedTasks,
                      tint = if (item.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                  }
                  IconButton(
                    onClick = {
                      editingRoutine = item
                      showAddDialog = true
                    },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(Icons.Filled.Edit, contentDescription = strings.editRoutine)
                  }
                  IconButton(
                    onClick = { onDeleteRoutine(item.id) },
                    modifier = Modifier.size(36.dp)
                  ) {
                    Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                  }
                }
                Row {
                  if (index > 0) {
                    IconButton(onClick = { onMoveRoutine(index, index - 1) }, modifier = Modifier.size(32.dp)) {
                      Icon(Icons.Filled.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                    }
                  }
                  if (index < routines.lastIndex) {
                    IconButton(onClick = { onMoveRoutine(index, index + 1) }, modifier = Modifier.size(32.dp)) {
                      Icon(Icons.Filled.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
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

  if (showAddDialog) {
    RoutineEditorDialog(
      strings = strings,
      existing = editingRoutine,
      subjects = subjects,
      onDismiss = { showAddDialog = false },
      onConfirm = { routine ->
        onSaveRoutine(routine)
        showAddDialog = false
      }
    )
  }

  if (showTemplatesSheet) {
    ModalBottomSheet(onDismissRequest = { showTemplatesSheet = false }) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(strings.templates, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        StudyPlannerRepository.BuiltInTemplates.forEach { template ->
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (strings.isBengali) template.titleBn else template.titleEn,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (strings.isBengali) template.subtitleBn else template.subtitleEn,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Button(onClick = {
                onApplyTemplate(template)
                showTemplatesSheet = false
              }) {
                Text(strings.applyTemplate)
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineEditorDialog(
  strings: AppStrings,
  existing: RoutineEntity?,
  subjects: List<SubjectEntity>,
  onDismiss: () -> Unit,
  onConfirm: (RoutineEntity) -> Unit
) {
  val context = LocalContext.current
  var subjectName by remember { mutableStateOf(existing?.subjectName ?: subjects.firstOrNull()?.name.orEmpty()) }
  var selectedSubjectId by remember { mutableStateOf(existing?.subjectId ?: subjects.firstOrNull()?.id.orEmpty()) }
  var selectedColor by remember { mutableLongStateOf(existing?.colorHex ?: subjects.firstOrNull()?.colorHex ?: SubjectColors.first()) }
  var dateMillis by remember { mutableLongStateOf(existing?.dateMillis ?: StudyPlannerRepository.startOfDayMillis(System.currentTimeMillis())) }
  var startMinutes by remember { mutableIntStateOf(existing?.startTimeMinutes ?: 480) }
  var endMinutes by remember { mutableIntStateOf(existing?.endTimeMinutes ?: 540) }
  var breakMinutes by remember { mutableIntStateOf(existing?.breakMinutes ?: 10) }
  var repeatMode by remember { mutableStateOf(existing?.repeatMode ?: "daily") }
  var repeatDays by remember { mutableStateOf(existing?.repeatDays ?: listOf(1, 2, 3, 4, 5, 6, 7)) }
  var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }

  val computedDuration = (endMinutes - startMinutes).coerceAtLeast(15)
  val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) strings.addRoutine else strings.editRoutine) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (subjects.isNotEmpty()) {
          Text(strings.selectSubject, style = MaterialTheme.typography.labelMedium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            subjects.forEach { subj ->
              FilterChip(
                selected = selectedSubjectId == subj.id,
                onClick = {
                  selectedSubjectId = subj.id
                  subjectName = subj.name
                  selectedColor = subj.colorHex
                },
                label = { Text(subj.name) }
              )
            }
          }
        }

        OutlinedTextField(
          value = subjectName,
          onValueChange = { subjectName = it },
          label = { Text(strings.subjectName) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("routine_subject_input")
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

        OutlinedButton(
          onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
            DatePickerDialog(
              context,
              { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                dateMillis = StudyPlannerRepository.startOfDayMillis(picked.timeInMillis)
              },
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH),
              cal.get(Calendar.DAY_OF_MONTH)
            ).show()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("${strings.selectDate}: $dateLabel")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              TimePickerDialog(
                context,
                { _, h, m ->
                  startMinutes = h * 60 + m
                  if (endMinutes <= startMinutes) {
                    endMinutes = (startMinutes + 60).coerceAtMost(1440)
                  }
                },
                startMinutes / 60,
                startMinutes % 60,
                false
              ).show()
            },
            modifier = Modifier.weight(1f)
          ) {
            Text("${strings.startTime}\n${PdfRoutineExporter.formatTime(startMinutes)}")
          }
          OutlinedButton(
            onClick = {
              TimePickerDialog(
                context,
                { _, h, m ->
                  val candidate = h * 60 + m
                  endMinutes = if (candidate > startMinutes) candidate else (startMinutes + 30).coerceAtMost(1440)
                },
                endMinutes / 60,
                endMinutes % 60,
                false
              ).show()
            },
            modifier = Modifier.weight(1f)
          ) {
            Text("${strings.endTime}\n${PdfRoutineExporter.formatTime(endMinutes)}")
          }
        }

        Text("${strings.durationMin}: $computedDuration ${strings.minutesShort}", style = MaterialTheme.typography.labelLarge)
        Text("${strings.breakMin}: $breakMinutes ${strings.minutesShort}", style = MaterialTheme.typography.labelLarge)
        Slider(
          value = breakMinutes.toFloat(),
          onValueChange = { breakMinutes = (it.toInt() / 5) * 5 },
          valueRange = 0f..45f
        )

        Text(strings.repeatLabel, style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(
            "once" to strings.repeatOnce,
            "daily" to strings.repeatDaily,
            "weekly" to strings.repeatWeekly,
            "custom" to strings.repeatCustom
          ).forEach { (key, label) ->
            FilterChip(
              selected = repeatMode == key,
              onClick = { repeatMode = key },
              label = { Text(label) }
            )
          }
        }

        if (repeatMode == "custom") {
          val dayNames = if (strings.isBengali) {
            listOf(1 to "সোম", 2 to "মঙ্গল", 3 to "বুধ", 4 to "বৃহ", 5 to "শুক্র", 6 to "শনি", 7 to "রবি")
          } else {
            listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
          }
          FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            dayNames.forEach { (dayNum, dayLabel) ->
              FilterChip(
                selected = dayNum in repeatDays,
                onClick = {
                  repeatDays = if (dayNum in repeatDays) repeatDays - dayNum else repeatDays + dayNum
                },
                label = { Text(dayLabel) }
              )
            }
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text(strings.notesLabel) },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalName = subjectName.trim().ifBlank { if (strings.isBengali) "পড়াশোনা" else "Study Session" }
          val base = existing ?: RoutineEntity()
          onConfirm(
            base.copy(
              subjectId = selectedSubjectId,
              subjectName = finalName,
              colorHex = selectedColor,
              dateMillis = dateMillis,
              startTimeMinutes = startMinutes,
              endTimeMinutes = endMinutes,
              durationMinutes = computedDuration,
              breakMinutes = breakMinutes,
              repeatMode = repeatMode,
              repeatDays = repeatDays,
              notes = notes.trim()
            )
          )
        },
        modifier = Modifier.testTag("save_routine_confirm_button")
      ) {
        Text(strings.save)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(strings.cancel) }
    }
  )
}
