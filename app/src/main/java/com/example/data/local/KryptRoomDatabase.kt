package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_messages")
data class CachedMessageEntity(
    @PrimaryKey val messageId: String,
    val threadId: String,
    val isGroup: Boolean,
    val senderId: String,
    val senderName: String,
    val plaintext: String,
    val messageType: String,
    val mediaName: String,
    val mediaMime: String,
    val mediaDataBase64: String,
    val mediaDurationMs: Long,
    val replyToMessageId: String,
    val replyToPreview: String,
    val forwardedFrom: String,
    val syncStatus: String, // SYNCED, PENDING_SEND, FAILED
    val createdAtMillis: Long
)

@Entity(tableName = "cached_conversations")
data class CachedConversationEntity(
    @PrimaryKey val conversationId: String,
    val peerUid: String,
    val peerName: String,
    val lastPreview: String,
    val lastAtMillis: Long,
    val unreadCount: Int
)

@Dao
interface KryptCacheDao {
    @Query("SELECT * FROM cached_messages WHERE threadId = :threadId ORDER BY createdAtMillis ASC")
    fun observeMessagesForThread(threadId: String): Flow<List<CachedMessageEntity>>

    @Query("SELECT * FROM cached_messages WHERE syncStatus != 'SYNCED' ORDER BY createdAtMillis ASC")
    suspend fun getPendingOutgoingMessages(): List<CachedMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(entity: CachedMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessages(entities: List<CachedMessageEntity>)

    @Query("DELETE FROM cached_messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("SELECT * FROM cached_conversations ORDER BY lastAtMillis DESC")
    fun observeCachedConversations(): Flow<List<CachedConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversations(entities: List<CachedConversationEntity>)

    @Query("DELETE FROM cached_messages")
    suspend fun clearMessages()

    @Query("DELETE FROM cached_conversations")
    suspend fun clearConversations()
}

@Database(
    entities = [CachedMessageEntity::class, CachedConversationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KryptDatabase : RoomDatabase() {
    abstract fun cacheDao(): KryptCacheDao

    companion object {
        @Volatile
        private var INSTANCE: KryptDatabase? = null

        fun getInstance(context: Context): KryptDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KryptDatabase::class.java,
                    "krypt_local_cache.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
