package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.RoutineEntity
import com.example.ui.i18n.AppStrings
import com.example.ui.viewmodel.StudyUiState
import com.example.util.PdfRoutineExporter
import java.util.Calendar

@Composable
fun DashboardScreen(
  strings: AppStrings,
  state: StudyUiState,
  isGuestMode: Boolean,
  onQuickAddRoutine: () -> Unit,
  onToggleRoutineComplete: (RoutineEntity) -> Unit,
  onOpenRoutineBuilder: () -> Unit,
  onOpenTimer: () -> Unit,
  onOpenProfile: () -> Unit
) {
  val completedTasks = state.tasks.count { it.isCompleted }
  val remainingTasks = state.tasks.count { !it.isCompleted }
  val totalMinutesToday = state.todayFocusMinutes + state.routines.filter { it.isCompleted }.sumOf { it.durationMinutes }
  val hoursText = String.format("%.1f %s", totalMinutesToday / 60f, strings.hoursShort)

  val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
  val quote = strings.quotes[dayOfYear % strings.quotes.size]
  val upcomingRoutines = state.routines.filter { !it.isCompleted }.sortedBy { it.startTimeMinutes }.take(3)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Hero Banner Card
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_study),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .matchParentSize()
          )
          Box(
            modifier = Modifier
              .matchParentSize()
              .background(
                Brush.horizontalGradient(
                  listOf(
                    Color(0xD90F172A),
                    Color(0x991B3B6F)
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${strings.goodMorning}, ${state.profile?.displayName?.ifBlank { null } ?: (if (strings.isBengali) "শিক্ষার্থী" else "Student")}",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                val eduLabel = strings.eduLevels[state.profile?.educationLevel] ?: ""
                val cat = state.profile?.categoryDetail.orEmpty()
                Text(
                  text = listOf(eduLabel, cat).filter { it.isNotBlank() }.joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.85f)
                )
              }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.clickable { onOpenProfile() }
              ) {
                Text(
                  text = if (isGuestMode) strings.guestModeBadge else "Cloud Sync",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = onQuickAddRoutine,
                modifier = Modifier.testTag("dashboard_quick_add_button"),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.quickAddSession)
              }
              FilledTonalButton(
                onClick = onOpenTimer,
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(Icons.Filled.Timer, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.navTimer)
              }
            }
          }
        }
      }
    }

    // Daily Progress & Summary Stats
    if (state.prefs.showProgressCard) {
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
          ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Box(
              modifier = Modifier.size(86.dp),
              contentAlignment = Alignment.Center
            ) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                  color = trackColor,
                  startAngle = -90f,
                  sweepAngle = 360f,
                  useCenter = false,
                  style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                  color = primaryColor,
                  startAngle = -90f,
                  sweepAngle = (state.dailyProgressPercent / 100f) * 360f,
                  useCenter = false,
                  style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
              }
              Text(
                text = "${state.dailyProgressPercent}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(strings.dailyProgress, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(
                text = "$totalMinutesToday / ${state.profile?.dailyGoalMinutes ?: 180} ${strings.minutesShort}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "🔥 ${state.studyStreakDays} ${strings.daysLabel} ${strings.studyStreak}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
              )
            }
          }
        }
      }
    }

    // 3 Metric Cards Row
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        StatMiniCard(
          title = strings.totalStudyHours,
          value = hoursText,
          icon = Icons.Filled.Schedule,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.weight(1f)
        )
        StatMiniCard(
          title = strings.completedTasks,
          value = "$completedTasks",
          icon = Icons.Filled.CheckCircle,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.weight(1f)
        )
        StatMiniCard(
          title = strings.remainingTasks,
          value = "$remainingTasks",
          icon = Icons.Filled.PendingActions,
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Motivational Quote Card
    if (state.prefs.showQuoteCard) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Filled.FormatQuote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = strings.motivationalQuoteTitle,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = quote,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }

    // Upcoming Sessions Pill Strip
    if (state.prefs.showUpcomingCard && upcomingRoutines.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = strings.upcomingSessions,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            upcomingRoutines.forEach { routine ->
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(routine.colorHex).copy(alpha = 0.15f),
                modifier = Modifier
                  .weight(1f)
                  .clickable { onOpenRoutineBuilder() }
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = routine.subjectName,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(routine.colorHex),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = PdfRoutineExporter.formatTime(routine.startTimeMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    }

    // Today's Study Routine Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = strings.todaysRoutine,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.clickable { onOpenRoutineBuilder() }
        ) {
          Text(
            text = strings.routineBuilderTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }

    if (state.routines.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_empty_routine),
              contentDescription = strings.noRoutinesToday,
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
            )
            Text(
              text = strings.noRoutinesToday,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onQuickAddRoutine) {
              Icon(Icons.Filled.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text(strings.addRoutine)
            }
          }
        }
      }
    } else {
      items(state.routines, key = { it.id }) { routine ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(routine.colorHex))
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = routine.subjectName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (routine.isCompleted) TextDecoration.LineThrough else TextDecoration.None
              )
              Text(
                text = "${PdfRoutineExporter.formatTime(routine.startTimeMinutes)} - ${PdfRoutineExporter.formatTime(routine.endTimeMinutes)} • ${routine.durationMinutes}${strings.minutesShort}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(onClick = { onToggleRoutineComplete(routine) }) {
              Icon(
                imageVector = if (routine.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = strings.completedTasks,
                tint = if (routine.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatMiniCard(
  title: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        maxLines = 1
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor.copy(alpha = 0.85f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
