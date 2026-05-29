package com.ai.asset.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ai.asset.model.ChatMessage

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
) {
    fun toChatMessage(): ChatMessage = ChatMessage(
        id = id,
        text = text,
        isUser = isUser,
        timestamp = timestamp
    )
    
    companion object {
        fun fromChatMessage(msg: ChatMessage): MessageEntity = MessageEntity(
            id = msg.id,
            text = msg.text,
            isUser = msg.isUser,
            timestamp = msg.timestamp
        )
    }
}

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity)
    
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>
    
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getCount(): Int
}

@Database(
    entities = [MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_history_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ChatHistoryRepository(private val context: Context) {
    private val database: AppDatabase = AppDatabase.getInstance(context)
    
    fun loadChatHistory(): Flow<List<ChatMessage>> {
        return database.messageDao().getAllMessages().map { entities ->
            entities.map { it.toChatMessage() }
        }
    }
    
    suspend fun saveMessage(message: ChatMessage) {
        database.messageDao().insert(MessageEntity.fromChatMessage(message))
    }
    
    suspend fun clearAllMessages() {
        database.messageDao().deleteAll()
    }
    
    suspend fun getMessageCount(): Int {
        return database.messageDao().getCount()
    }
}
