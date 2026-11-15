package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.data.auth.onGoogleSignInClicked
import com.example.data.local.UserProfileEntity
import com.example.ui.i18n.AppStrings
import com.example.ui.viewmodel.StudyUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun MoreHubScreen(
  strings: AppStrings,
  onNavigateSubjects: () -> Unit,
  onNavigateCalendar: () -> Unit,
  onNavigateExams: () -> Unit,
  onNavigateProgress: () -> Unit,
  onNavigateProfile: () -> Unit,
  onNavigateSettings: () -> Unit
) {
  val items = listOf(
    Triple(strings.subjectsTitle, Icons.Filled.Book, onNavigateSubjects),
    Triple(strings.calendarTitle, Icons.Filled.CalendarMonth, onNavigateCalendar),
    Triple(strings.examPrepTitle, Icons.Filled.School, onNavigateExams),
    Triple(strings.progressReportTitle, Icons.Filled.BarChart, onNavigateProgress),
    Triple(strings.profileTitle, Icons.Filled.AccountCircle, onNavigateProfile),
    Triple(strings.settingsTitle, Icons.Filled.Settings, onNavigateSettings)
  )

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(strings.moreHubTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
    items.forEach { (title, icon, onClick) ->
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  strings: AppStrings,
  profile: UserProfileEntity?,
  isGuestMode: Boolean,
  onSaveProfile: (UserProfileEntity) -> Unit,
  onGoogleConnected: (UserProfileEntity) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var name by remember(profile) { mutableStateOf(profile?.displayName.orEmpty()) }
  var email by remember(profile) { mutableStateOf(profile?.email.orEmpty()) }
  var level by remember(profile) { mutableStateOf(profile?.educationLevel ?: "school") }
  var category by remember(profile) { mutableStateOf(profile?.categoryDetail.orEmpty()) }
  var dailyGoal by remember(profile) { mutableIntStateOf(profile?.dailyGoalMinutes ?: 180) }
  var goalNote by remember(profile) { mutableStateOf(profile?.personalGoalNote.orEmpty()) }
  var photoUri by remember(profile) { mutableStateOf(profile?.photoUri.orEmpty()) }
  var savedToast by remember { mutableStateOf(false) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri: Uri? -> uri?.let { photoUri = it.toString() } }
  )

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(strings.profileTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }

    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.clickable {
          photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
      ) {
        Surface(modifier = Modifier.size(72.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
          if (photoUri.isNotBlank()) {
            AsyncImage(model = photoUri, contentDescription = strings.pickProfilePhoto, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
          } else {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.AddAPhoto, contentDescription = strings.pickProfilePhoto) }
          }
        }
        Column {
          Text(name.ifBlank { strings.profileTitle }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(strings.pickProfilePhoto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
      }
    }

    if (isGuestMode && Firebase.auth.currentUser == null) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.connectGoogleHint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Button(onClick = {
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  val uid = Firebase.auth.currentUser?.uid.orEmpty()
                  onGoogleConnected(
                    (profile ?: UserProfileEntity()).copy(
                      userId = uid,
                      displayName = name.ifBlank { Firebase.auth.currentUser?.displayName ?: "Student" },
                      email = email.ifBlank { Firebase.auth.currentUser?.email.orEmpty() },
                      educationLevel = level,
                      categoryDetail = category,
                      photoUri = photoUri,
                      dailyGoalMinutes = dailyGoal,
                      weeklyGoalMinutes = dailyGoal * 7,
                      personalGoalNote = goalNote
                    )
                  )
                },
                onAuthError = {},
                scope = scope
              )
            }) {
              Text(strings.signInWithGoogle)
            }
          }
        }
      }
    }

    item {
      Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.fullNameLabel) }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(strings.emailLabel) }, modifier = Modifier.fillMaxWidth())
          Text(strings.educationLevelLabel, style = MaterialTheme.typography.labelLarge)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            strings.eduLevels.forEach { (k, l) ->
              FilterChip(selected = level == k, onClick = { level = k }, label = { Text(l) })
            }
          }
          OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(strings.categoryLabel) }, modifier = Modifier.fillMaxWidth())
          Text("${strings.dailyGoalLabel}: $dailyGoal ${strings.minutesShort}")
          Slider(value = dailyGoal.toFloat(), onValueChange = { dailyGoal = (it.toInt() / 15) * 15 }, valueRange = 30f..480f)
          OutlinedTextField(value = goalNote, onValueChange = { goalNote = it }, label = { Text(strings.personalGoalLabel) }, modifier = Modifier.fillMaxWidth())

          Button(
            onClick = {
              onSaveProfile(
                UserProfileEntity(
                  id = "local_user",
                  userId = Firebase.auth.currentUser?.uid.orEmpty(),
                  displayName = name.trim().ifBlank { "Student" },
                  email = email.trim(),
                  educationLevel = level,
                  categoryDetail = category.trim(),
                  photoUri = photoUri,
                  dailyGoalMinutes = dailyGoal,
                  weeklyGoalMinutes = dailyGoal * 7,
                  personalGoalNote = goalNote.trim()
                )
              )
              savedToast = true
            },
            modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
          ) {
            Text(strings.save)
          }
          if (savedToast) {
            Text(
              text = if (strings.isBengali) "প্রোফাইল সংরক্ষিত হয়েছে!" else "Profile saved!",
              color = MaterialTheme.colorScheme.secondary,
              style = MaterialTheme.typography.bodySmall
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  strings: AppStrings,
  state: StudyUiState,
  onLanguageChange: (String) -> Unit,
  onThemeModeChange: (String) -> Unit,
  onAccentChange: (String) -> Unit,
  onUpdateNotifications: (Boolean?, Boolean?, Boolean?, Boolean?, Boolean?, String?) -> Unit,
  onUpdateDashboard: (Boolean?, Boolean?, Boolean?) -> Unit,
  onBackupNow: () -> Unit,
  onRestoreFromCloud: () -> Unit,
  onExportBackupJson: suspend () -> String,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val prefs = state.prefs

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(strings.settingsTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }

    // Language & Theme
    item {
      Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = prefs.language == "bn", onClick = { onLanguageChange("bn") }, label = { Text("বাংলা") }, modifier = Modifier.testTag("settings_lang_bn"))
            FilterChip(selected = prefs.language == "en", onClick = { onLanguageChange("en") }, label = { Text("English") }, modifier = Modifier.testTag("settings_lang_en"))
          }

          HorizontalDivider()
          Text(strings.themeModeLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to strings.themeSystem, "light" to strings.themeLight, "dark" to strings.themeDark).forEach { (k, l) ->
              FilterChip(selected = prefs.themeMode == k, onClick = { onThemeModeChange(k) }, label = { Text(l) })
            }
          }

          Text(strings.accentThemeLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("indigo" to "Indigo", "emerald" to "Emerald", "amber" to "Amber", "rose" to "Rose").forEach { (k, l) ->
              FilterChip(selected = prefs.accentTheme == k, onClick = { onAccentChange(k) }, label = { Text(l) })
            }
          }
        }
      }
    }

    // Smart Notifications
    item {
      Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(strings.notificationsLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          ToggleRow(strings.notificationsLabel, prefs.notificationsEnabled) { onUpdateNotifications(it, null, null, null, null, null) }
          ToggleRow(strings.routineReminders, prefs.routineRemindersEnabled) { onUpdateNotifications(null, it, null, null, null, null) }
          ToggleRow(strings.examReminders, prefs.examRemindersEnabled) { onUpdateNotifications(null, null, it, null, null, null) }
          ToggleRow(strings.assignmentReminders, prefs.assignmentRemindersEnabled) { onUpdateNotifications(null, null, null, it, null, null) }
          ToggleRow(strings.breakReminders, prefs.breakRemindersEnabled) { onUpdateNotifications(null, null, null, null, it, null) }

          Text(strings.notificationSound, style = MaterialTheme.typography.labelLarge)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("default" to strings.soundDefault, "gentle_chime" to strings.soundChime, "silent" to strings.soundSilent).forEach { (k, l) ->
              FilterChip(selected = prefs.notificationSound == k, onClick = { onUpdateNotifications(null, null, null, null, null, k) }, label = { Text(l) })
            }
          }
        }
      }
    }

    // Custom Dashboard
    item {
      Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(strings.customDashboardLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          ToggleRow(strings.showQuoteCard, prefs.showQuoteCard) { onUpdateDashboard(it, null, null) }
          ToggleRow(strings.showProgressCard, prefs.showProgressCard) { onUpdateDashboard(null, it, null) }
          ToggleRow(strings.showUpcomingCard, prefs.showUpcomingCard) { onUpdateDashboard(null, null, it) }
        }
      }
    }

    // Cloud Sync, Backup & Restore
    item {
      Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(strings.cloudBackupSync, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          if (state.isSyncing) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBackupNow, enabled = !state.isSyncing && Firebase.auth.currentUser != null, modifier = Modifier.weight(1f)) {
              Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.backupNow, maxLines = 1)
            }
            OutlinedButton(onClick = onRestoreFromCloud, enabled = !state.isSyncing && Firebase.auth.currentUser != null, modifier = Modifier.weight(1f)) {
              Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(strings.restoreFromCloud, maxLines = 1)
            }
          }
          OutlinedButton(
            onClick = {
              scope.launch {
                val json = onExportBackupJson()
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_TEXT, json)
                }
                context.startActivity(Intent.createChooser(sendIntent, strings.exportBackupJson))
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.exportBackupJson)
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
      ) {
        Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.width(8.dp))
        Text(strings.signOut, color = MaterialTheme.colorScheme.error)
      }
    }
  }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
