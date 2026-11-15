package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [
    UserProfileEntity::class,
    SubjectEntity::class,
    RoutineEntity::class,
    StudyTaskEntity::class,
    StudyNoteEntity::class,
    ExamEntity::class,
    FocusSessionEntity::class
  ],
  version = 1,
  exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun studyDao(): StudyDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "study_planner_db"
        ).build().also { INSTANCE = it }
      }
    }
  }
}
