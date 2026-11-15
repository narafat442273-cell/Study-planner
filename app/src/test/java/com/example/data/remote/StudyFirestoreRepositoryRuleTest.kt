package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StudyFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun upsertAndObserveSubject_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = StudyFirestoreRepository(firestore, auth)
    val subId = "sub_${UUID.randomUUID().toString().take(8)}"

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertSubject(
        SubjectDoc(
          id = subId,
          userId = uid,
          name = "Physics",
          colorHex = 0xFF2563EBL,
          priority = "high",
          chapters = listOf("Optics", "Waves"),
          completedChapters = listOf("Optics"),
          examDateMillis = 1799000000000L,
          progressPercent = 50L
        )
      )
    }
    assertTrue(saveResult.isSuccess)

    val subjects = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeSubjects().first { list -> list.any { it.id == subId } }
    }
    assertEquals("Physics", subjects.first { it.id == subId }.name)
  }

  @Test
  fun upsertProfileAndRoutine_authenticatedOwner_succeedsAndUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = StudyFirestoreRepository(firestore, auth)

    val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertProfile(
        UserProfileDoc(
          userId = uid,
          displayName = "Arafat",
          email = ALICE_EMAIL,
          educationLevel = "university",
          categoryDetail = "CSE",
          dailyGoalMinutes = 180L,
          weeklyGoalMinutes = 1260L
        )
      )
    }
    assertTrue(profileRes.isSuccess)

    val routineId = "rt_${UUID.randomUUID().toString().take(8)}"
    val routineRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertRoutine(
        RoutineDoc(
          id = routineId,
          userId = uid,
          subjectId = "sub_1",
          subjectName = "Math",
          dateMillis = 1794600000000L,
          startTimeMinutes = 480L,
          endTimeMinutes = 540L,
          durationMinutes = 60L,
          breakMinutes = 10L
        )
      )
    }
    assertTrue(routineRes.isSuccess)

    val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertRoutine(
        RoutineDoc(
          id = routineId,
          userId = uid,
          subjectId = "sub_1",
          subjectName = "Math Advanced",
          dateMillis = 1794600000000L,
          startTimeMinutes = 480L,
          endTimeMinutes = 540L,
          durationMinutes = 60L,
          breakMinutes = 10L,
          isCompleted = true
        )
      )
    }
    assertTrue(updateRes.isSuccess)
  }

  @Test
  fun observeSubjects_unauthenticatedUser_throwsIllegalStateOrPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = StudyFirestoreRepository(firestore, auth)

    var caught: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeSubjects().first()
      }
    } catch (t: Throwable) {
      caught = t
    }
    assertTrue(caught != null)
  }

  @Test
  fun crossUserRead_directlyFromFirestore_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = StudyFirestoreRepository(firestore, auth)
    val noteId = "nt_${UUID.randomUUID().toString().take(8)}"
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.upsertNote(
        StudyNoteDoc(
          id = noteId,
          userId = aliceUid,
          subjectId = "s1",
          subjectName = "Bangla",
          title = "Grammar Notes",
          content = "Important rules"
        )
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    var denied = false
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("notes").document(noteId).get().let {
          com.google.android.gms.tasks.Tasks.await(it)
        }
      }
    } catch (e: Exception) {
      val ffe = e.unwrapFirestoreException()
      if (ffe?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        denied = true
      }
    }
    assertTrue("Expected PERMISSION_DENIED for cross-user read", denied)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_study@test.com"
    const val BOB_EMAIL = "bob_study@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
