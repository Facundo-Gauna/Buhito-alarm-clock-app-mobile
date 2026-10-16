package com.buhito.alarm.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versión 6 (sueño automático). Las migraciones 2→3 … 5→6 conservan tus datos.
 * Solo desde la versión 1 la base se recrea.
 */
@Database(
    entities = [Alarm::class, Template::class, Stopwatch::class, CustomCategory::class, HistoryEntry::class, SleepRecord::class],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun templateDao(): TemplateDao
    abstract fun stopwatchDao(): StopwatchDao
    abstract fun customCategoryDao(): CustomCategoryDao
    abstract fun historyDao(): HistoryDao
    abstract fun sleepDao(): SleepDao

    companion object {
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `qrStop` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sleep_records` ADD COLUMN `auto` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sleep_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`sleepAt` INTEGER NOT NULL, " +
                        "`wakeAt` INTEGER NOT NULL, " +
                        "`quality` INTEGER NOT NULL)",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `confirmStop` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `speakLabel` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `chainCount` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `chainGapMin` INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `chainLeft` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `custom_categories` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`emoji` TEXT NOT NULL, " +
                        "`color` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`alarmId` INTEGER NOT NULL, " +
                        "`label` TEXT NOT NULL, " +
                        "`hour` INTEGER NOT NULL, " +
                        "`minute` INTEGER NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`firedAt` INTEGER NOT NULL, " +
                        "`result` TEXT NOT NULL)",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stopwatches` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`accumulatedMs` INTEGER NOT NULL, " +
                        "`runningSince` INTEGER NOT NULL, " +
                        "`laps` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "buhito.db",
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
