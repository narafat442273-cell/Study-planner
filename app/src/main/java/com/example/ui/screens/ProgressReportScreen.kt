package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppStrings
import com.example.ui.viewmodel.StudyUiState

@Composable
fun ProgressReportScreen(
  strings: AppStrings,
  state: StudyUiState
) {
  val completedTasks = state.tasks.count { it.isCompleted }
  val pendingTasks = state.tasks.count { !it.isCompleted }
  val totalTasks = (completedTasks + pendingTasks).coerceAtLeast(1)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(strings.progressReportTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }

    // Streak & Monthly Summary Row
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(strings.studyStreak, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "🔥 ${state.studyStreakDays} ${strings.daysLabel}",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Card(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(strings.monthlyStats, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "${state.monthlyFocusMinutes} ${strings.minutesShort}",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Weekly Bar Chart in Canvas
    item {
      Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(strings.weeklyStats, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          val maxVal = (state.weeklyMinutesByDay.maxOfOrNull { it.second } ?: 60).coerceAtLeast(30)
          val barColor = MaterialTheme.colorScheme.primary
          val trackColor = MaterialTheme.colorScheme.surfaceVariant

          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
          ) {
            val count = state.weeklyMinutesByDay.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = slotWidth * 0.48f
            state.weeklyMinutesByDay.forEachIndexed { idx, (_, mins) ->
              val left = idx * slotWidth + (slotWidth - barWidth) / 2f
              drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(12f, 12f)
              )
              val barHeight = (mins.toFloat() / maxVal.toFloat()) * size.height
              if (barHeight > 0f) {
                drawRoundRect(
                  color = barColor,
                  topLeft = Offset(left, size.height - barHeight),
                  size = Size(barWidth, barHeight),
                  cornerRadius = CornerRadius(12f, 12f)
                )
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            state.weeklyMinutesByDay.forEach { (dayLabel, mins) ->
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$mins", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(dayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }

    // Completed vs Pending Tasks
    item {
      Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(strings.tasksRatio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          val ratio = completedTasks.toFloat() / totalTasks.toFloat()
          LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(12.dp)
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${strings.completedTasks}: $completedTasks", style = MaterialTheme.typography.bodyMedium)
            Text("${strings.remainingTasks}: $pendingTasks", style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }

    // Subject-wise Progress
    item {
      Text(strings.subjectProgress, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }

    items(state.subjects, key = { it.id }) { subject ->
      Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(subject.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${subject.progressPercent}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(subject.colorHex))
          }
          LinearProgressIndicator(
            progress = { subject.progressPercent / 100f },
            color = Color(subject.colorHex),
            modifier = Modifier.fillMaxWidth().height(8.dp)
          )
        }
      }
    }
  }
}
