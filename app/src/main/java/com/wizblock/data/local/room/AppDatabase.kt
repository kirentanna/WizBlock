package com.wizblock.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BlockEventEntity::class,
        RuleEntity::class,
        ScheduleEntity::class,
        UsageLimitEntity::class,
        UsageCounterEntity::class,
        ProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockEventDao(): BlockEventDao
    abstract fun ruleDao(): RuleDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun usageLimitDao(): UsageLimitDao
    abstract fun usageCounterDao(): UsageCounterDao
    abstract fun profileDao(): ProfileDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS profiles (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        color_token TEXT NOT NULL,
                        icon_name TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        enabled INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO profiles (id, name, color_token, icon_name, mode, enabled)
                    VALUES ('default', 'Focus', 'blue', 'shield', 'BLOCKLIST', 1)
                    """.trimIndent()
                )
                addColumnIfMissing(db, "rules", "profile_id", "TEXT DEFAULT 'default'")
                addColumnIfMissing(db, "schedules", "profile_id", "TEXT DEFAULT 'default'")
                addColumnIfMissing(db, "usage_limits", "profile_id", "TEXT DEFAULT 'default'")
            }
        }

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
            typeClause: String
        ) {
            db.query("PRAGMA table_info($table)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == column) return
                }
            }
            db.execSQL("ALTER TABLE $table ADD COLUMN $column $typeClause")
        }
    }
}
