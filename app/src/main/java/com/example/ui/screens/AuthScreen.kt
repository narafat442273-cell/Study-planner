package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
  strings: AppStrings,
  initialProfile: UserProfileEntity?,
  onLanguageChange: (String) -> Unit,
  onSaveProfileAndEnterGoogle: (UserProfileEntity) -> Unit,
  onContinueAsGuest: (UserProfileEntity) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var name by remember(initialProfile) { mutableStateOf(initialProfile?.displayName.orEmpty()) }
  var email by remember(initialProfile) { mutableStateOf(initialProfile?.email.orEmpty()) }
  var selectedLevel by remember(initialProfile) { mutableStateOf(initialProfile?.educationLevel ?: "school") }
  var categoryDetail by remember(initialProfile) { mutableStateOf(initialProfile?.categoryDetail.orEmpty()) }
  var personalGoal by remember(initialProfile) { mutableStateOf(initialProfile?.personalGoalNote.orEmpty()) }
  var dailyGoalMinutes by remember(initialProfile) { mutableIntStateOf(initialProfile?.dailyGoalMinutes ?: 180) }
  var photoUri by remember(initialProfile) { mutableStateOf(initialProfile?.photoUri.orEmpty()) }

  var isSigningIn by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri: Uri? ->
      uri?.let { photoUri = it.toString() }
    }
  )

  fun buildProfile(firebaseUid: String = ""): UserProfileEntity {
    val authUser = Firebase.auth.currentUser
    val finalName = name.trim().ifBlank { authUser?.displayName ?: (if (strings.isBengali) "শিক্ষার্থী" else "Student") }
    val finalEmail = email.trim().ifBlank { authUser?.email.orEmpty() }
    val finalPhoto = photoUri.ifBlank { authUser?.photoUrl?.toString().orEmpty() }
    return UserProfileEntity(
      id = "local_user",
      userId = firebaseUid,
      displayName = finalName,
      email = finalEmail,
      educationLevel = selectedLevel,
      categoryDetail = categoryDetail.trim(),
      photoUri = finalPhoto,
      dailyGoalMinutes = dailyGoalMinutes,
      weeklyGoalMinutes = dailyGoalMinutes * 7,
      personalGoalNote = personalGoal.trim()
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .systemBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = strings.appTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
          selected = strings.isBengali,
          onClick = { onLanguageChange("bn") },
          label = { Text("বাংলা") }
        )
        FilterChip(
          selected = !strings.isBengali,
          onClick = { onLanguageChange("en") },
          label = { Text("EN") }
        )
      }
    }

    Text(
      text = strings.loginTitle,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = strings.loginSubtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Profile photo picker
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable {
          photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        .padding(vertical = 4.dp)
        .testTag("auth_pick_photo_button")
    ) {
      Surface(
        modifier = Modifier.size(68.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
      ) {
        if (photoUri.isNotBlank()) {
          AsyncImage(
            model = photoUri,
            contentDescription = strings.pickProfilePhoto,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.AddAPhoto,
              contentDescription = strings.pickProfilePhoto,
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }
      Column {
        Text(strings.pickProfilePhoto, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
          text = if (strings.isBengali) "গ্যালারি থেকে প্রোফাইল ছবি বেছে নিন" else "Tap to choose from Photo Picker",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(strings.fullNameLabel) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_name_input")
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text(strings.emailLabel) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_email_input")
        )

        Text(
          text = strings.educationLevelLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          strings.eduLevels.forEach { (key, label) ->
            FilterChip(
              selected = selectedLevel == key,
              onClick = { selectedLevel = key },
              label = { Text(label) }
            )
          }
        }

        OutlinedTextField(
          value = categoryDetail,
          onValueChange = { categoryDetail = it },
          label = { Text(strings.categoryLabel) },
          placeholder = {
            Text(if (strings.isBengali) "যেমন: ১০ম শ্রেণি / HSC বিজ্ঞান / BCS / CSE" else "e.g. Class 10 / HSC Science / BCS / CSE")
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Column {
          Text(
            text = "${strings.dailyGoalLabel}: $dailyGoalMinutes ${strings.minutesShort}",
            style = MaterialTheme.typography.labelLarge
          )
          Slider(
            value = dailyGoalMinutes.toFloat(),
            onValueChange = { dailyGoalMinutes = (it.toInt() / 15) * 15 },
            valueRange = 30f..480f
          )
        }

        OutlinedTextField(
          value = personalGoal,
          onValueChange = { personalGoal = it },
          label = { Text(strings.personalGoalLabel) },
          placeholder = {
            Text(if (strings.isBengali) "যেমন: প্রতিদিন গণিত ও ইংরেজি শেষ করা" else "e.g. Finish Calculus & Daily Mock Tests")
          },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    if (errorMsg != null) {
      Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = errorMsg.orEmpty(),
          color = MaterialTheme.colorScheme.onErrorContainer,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(12.dp)
        )
      }
    }

    Button(
      onClick = {
        isSigningIn = true
        errorMsg = null
        onGoogleSignInClicked(
          context = context,
          credentialManager = credentialManager,
          onAuthSuccess = {
            isSigningIn = false
            val uid = Firebase.auth.currentUser?.uid.orEmpty()
            onSaveProfileAndEnterGoogle(buildProfile(uid))
          },
          onAuthError = { err ->
            isSigningIn = false
            errorMsg = err
          },
          scope = scope,
          onAuthCancelled = { isSigningIn = false }
        )
      },
      enabled = !isSigningIn,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("google_sign_in_button"),
      shape = RoundedCornerShape(16.dp)
    ) {
      if (isSigningIn) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          color = MaterialTheme.colorScheme.onPrimary,
          strokeWidth = 2.dp
        )
      } else {
        Icon(Icons.Filled.CloudDone, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(strings.signInWithGoogle, style = MaterialTheme.typography.titleSmall)
      }
    }

    OutlinedButton(
      onClick = { onContinueAsGuest(buildProfile()) },
      enabled = !isSigningIn,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("guest_mode_button"),
      shape = RoundedCornerShape(16.dp)
    ) {
      Icon(Icons.Filled.PersonOutline, contentDescription = null)
      Spacer(modifier = Modifier.width(8.dp))
      Text(strings.continueAsGuest, style = MaterialTheme.typography.titleSmall)
    }
  }
}
