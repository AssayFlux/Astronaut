package org.offlineai.messenger.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Entity(tableName = "conversations")
data class ConversationEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val createdAt: Long = System.currentTimeMillis(), val archived: Boolean = false, val pinned: Boolean = false)
@Entity(tableName = "messages")
data class MessageEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val conversationId: Long, val body: String, val role: String, val createdAt: Long = System.currentTimeMillis(), val status: String = "complete")
@Dao interface ChatDao {
 @Query("SELECT * FROM conversations WHERE archived = 0 ORDER BY pinned DESC, createdAt DESC") fun conversations(): Flow<List<ConversationEntity>>
 @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY createdAt") fun messages(id: Long): Flow<List<MessageEntity>>
 @Insert suspend fun addConversation(item: ConversationEntity): Long
 @Insert suspend fun addMessage(item: MessageEntity): Long
 @Query("UPDATE conversations SET title = :title WHERE id = :id") suspend fun rename(id: Long, title: String)
 @Query("UPDATE conversations SET archived = :value WHERE id = :id") suspend fun archive(id: Long, value: Boolean)
 @Query("DELETE FROM conversations WHERE id = :id") suspend fun deleteConversation(id: Long)
}
@Database(entities = [ConversationEntity::class, MessageEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() { abstract fun chatDao(): ChatDao }
