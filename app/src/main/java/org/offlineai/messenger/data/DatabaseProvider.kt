package org.offlineai.messenger.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit migrations preserve conversations, model records and workspace projects.
 * Schema history: v1 had conversations/messages; v2 added model/workspace tables and
 * the message conversation index; v3 added local_models.modelPath; v4 added attachmentText;
 * v5 links conversations to workspace projects.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS local_models (" +
                "uri TEXT NOT NULL, displayName TEXT NOT NULL, mimeType TEXT, " +
                "sizeBytes INTEGER NOT NULL, importedAt INTEGER NOT NULL, " +
                "runtimeStatus TEXT NOT NULL, PRIMARY KEY(uri))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS workspace_projects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, " +
                "description TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_messages_conversationId ON messages(conversationId)"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE local_models ADD COLUMN modelPath TEXT")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN attachmentText TEXT")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN workspaceProjectId INTEGER")
    }
}

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "astronaut-local.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
            .also { instance = it }
    }
}
