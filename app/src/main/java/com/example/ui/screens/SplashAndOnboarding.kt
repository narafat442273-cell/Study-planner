package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.i18n.AppStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
  strings: AppStrings,
  onFinished: () -> Unit
) {
  val scale = remember { Animatable(0.75f) }
  LaunchedEffect(Unit) {
    scale.animateTo(
      targetValue = 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    delay(1200L)
    onFinished()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
          )
        )
      )
      .systemBarsPadding(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(32.dp)
    ) {
      Surface(
        modifier = Modifier
          .size(104.dp)
          .scale(scale.value),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.AutoStories,
            contentDescription = strings.appTitle,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = strings.appTitle,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = strings.appSubtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun OnboardingScreen(
  strings: AppStrings,
  onLanguageChange: (String) -> Unit,
  onComplete: () -> Unit
) {
  val pagerState = rememberPagerState(pageCount = { strings.onboardingTitles.size })
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .systemBarsPadding()
      .padding(20.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        FilterChip(
          selected = strings.isBengali,
          onClick = { onLanguageChange("bn") },
          label = { Text("বাংলা") },
          modifier = Modifier.testTag("onboarding_lang_bn")
        )
        FilterChip(
          selected = !strings.isBengali,
          onClick = { onLanguageChange("en") },
          label = { Text("English") },
          modifier = Modifier.testTag("onboarding_lang_en")
        )
      }
      TextButton(
        onClick = onComplete,
        modifier = Modifier.testTag("onboarding_skip_button")
      ) {
        Text(strings.skip)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    HorizontalPager(
      state = pagerState,
      modifier = Modifier.weight(1f)
    ) { page ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Card(
          shape = RoundedCornerShape(28.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
        ) {
          Image(
            painter = painterResource(
              id = when (page) {
                0 -> R.drawable.img_onboarding_study
                1 -> R.drawable.img_hero_study
                else -> R.drawable.img_empty_routine
              }
            ),
            contentDescription = strings.onboardingTitles[page],
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
          text = strings.onboardingTitles[page],
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = strings.onboardingBodies[page],
          style = MaterialTheme.typography.bodyLarge,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp),
      horizontalArrangement = Arrangement.Center
    ) {
      repeat(strings.onboardingTitles.size) { idx ->
        val active = pagerState.currentPage == idx
        Box(
          modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(8.dp)
            .width(if (active) 28.dp else 8.dp)
            .clip(CircleShape)
            .background(
              if (active) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outline
            )
        )
      }
    }

    val isLast = pagerState.currentPage == strings.onboardingTitles.lastIndex
    if (isLast) {
      Button(
        onClick = onComplete,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("onboarding_get_started_button"),
        shape = RoundedCornerShape(16.dp)
      ) {
        Text(strings.getStarted, style = MaterialTheme.typography.titleMedium)
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onComplete,
          modifier = Modifier
            .weight(1f)
            .height(52.dp),
          shape = RoundedCornerShape(16.dp)
        ) {
          Text(strings.skip)
        }
        Button(
          onClick = {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
          },
          colors = ButtonDefaults.buttonColors(),
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .testTag("onboarding_next_button"),
          shape = RoundedCornerShape(16.dp)
        ) {
          Text(strings.next)
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
      }
    }
  }
}
