package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.FocusSessionEntity
import com.example.data.local.SubjectEntity
import com.example.ui.i18n.AppStrings
import com.example.ui.viewmodel.TimerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(
  strings: AppStrings,
  timerState: TimerState,
  subjects: List<SubjectEntity>,
  todayFocusMinutes: Int,
  recentSessions: List<FocusSessionEntity>,
  onSelectMode: (String, Int?) -> Unit,
  onSelectSubject: (SubjectEntity?) -> Unit,
  onStartResume: () -> Unit,
  onPause: () -> Unit,
  onReset: () -> Unit,
  onCompleteAndLog: () -> Unit
) {
  val context = LocalContext.current
  val minutes = timerState.remainingSeconds / 60
  val seconds = timerState.remainingSeconds % 60
  val progress = if (timerState.totalSeconds > 0) {
    timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
  } else 0f

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = strings.timerTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.secondaryContainer
        ) {
          Text(
            text = "${strings.dailyFocusTime}: $todayFocusMinutes ${strings.minutesShort}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }

    // Mode Selector Chips
    item {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(
          "pomodoro" to strings.pomodoro25,
          "short_break" to strings.shortBreak5,
          "long_break" to strings.longBreak15,
          "custom" to strings.customTimer
        ).forEach { (mode, label) ->
          FilterChip(
            selected = timerState.mode == mode,
            onClick = { onSelectMode(mode, null) },
            label = { Text(label) },
            modifier = Modifier.testTag("timer_mode_$mode")
          )
        }
      }
    }

    if (timerState.mode == "custom") {
      item {
        Card(shape = RoundedCornerShape(16.dp)) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "${strings.customTimer}: ${timerState.customMinutes} ${strings.minutesShort}",
              style = MaterialTheme.typography.labelLarge
            )
            Slider(
              value = timerState.customMinutes.toFloat(),
              onValueChange = { onSelectMode("custom", (it.toInt() / 5).coerceAtLeast(1) * 5) },
              valueRange = 5f..120f
            )
          }
        }
      }
    }

    // Subject selector
    if (subjects.isNotEmpty()) {
      item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          subjects.forEach { subj ->
            FilterChip(
              selected = timerState.selectedSubjectId == subj.id,
              onClick = { onSelectSubject(subj) },
              label = { Text(subj.name) }
            )
          }
        }
      }
    }

    // Main Circular Countdown
    item {
      Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          val ringColor = if (timerState.mode == "short_break" || timerState.mode == "long_break") {
            MaterialTheme.colorScheme.secondary
          } else {
            MaterialTheme.colorScheme.primary
          }
          val trackColor = MaterialTheme.colorScheme.surfaceVariant

          Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
              )
              drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
              )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
              )
              if (timerState.selectedSubjectName.isNotBlank()) {
                Text(
                  text = timerState.selectedSubjectName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { if (timerState.isRunning) onPause() else onStartResume() },
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("timer_play_pause_button"),
              shape = RoundedCornerShape(16.dp)
            ) {
              Icon(
                imageVector = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                when {
                  timerState.isRunning -> strings.pause
                  timerState.remainingSeconds < timerState.totalSeconds -> strings.resume
                  else -> strings.start
                }
              )
            }
            OutlinedButton(
              onClick = onReset,
              modifier = Modifier
                .height(52.dp)
                .testTag("timer_reset_button"),
              shape = RoundedCornerShape(16.dp)
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = strings.reset)
            }
            OutlinedButton(
              onClick = onCompleteAndLog,
              modifier = Modifier
                .height(52.dp)
                .testTag("timer_complete_button"),
              shape = RoundedCornerShape(16.dp)
            ) {
              Icon(Icons.Filled.Check, contentDescription = strings.completeAndLog)
            }
          }
        }
      }
    }

    // Recent Focus Sessions
    item {
      Text(
        text = strings.recentFocusSessions,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }

    items(recentSessions.take(10), key = { it.id }) { session ->
      Card(shape = RoundedCornerShape(14.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(session.subjectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
              text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(session.completedAtMillis)),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Text(
            text = "${session.durationMinutes} ${strings.minutesShort}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}
