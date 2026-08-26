package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderColor: Long,
    val targetRoomOrPeerId: String,
    val isDirect: Boolean,
    val content: String,
    val timestamp: Long,
    val isMine: Boolean,
    val messageType: String,
    val imageBase64: String? = null,
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val mimeType: String? = null,
    val durationSeconds: Int = 0,
    val senderIp: String? = null,
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false
)

@Entity(tableName = "saved_rooms")
data class RoomEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val isDefault: Boolean,
    val iconName: String,
    val creatorId: String = "system",
    val creatorName: String = "النظام",
    val maxCapacity: Int = 10,
    val isPrivate: Boolean = false
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey
    val username: String,
    val passwordHash: String,
    val userId: String,
    val displayName: String,
    val avatarColor: Long,
    val userStatus: String = "ONLINE",
    val statusMessage: String,
    val bio: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActiveSession: Boolean = false
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE targetRoomOrPeerId = :targetId ORDER BY timestamp ASC")
    fun getMessagesForTarget(targetId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 100")
    fun getAllRecentMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET localFilePath = :localFilePath, isDownloaded = 1 WHERE id = :messageId")
    suspend fun updateMessageFileDownloaded(messageId: String, localFilePath: String)

    @Query("DELETE FROM chat_messages WHERE targetRoomOrPeerId = :targetId")
    suspend fun deleteMessagesForTarget(targetId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()

    @Query("SELECT * FROM saved_rooms")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM saved_rooms")
    suspend fun getAllRoomsList(): List<RoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Query("DELETE FROM saved_rooms WHERE id = :roomId")
    suspend fun deleteRoom(roomId: String)

    // User Account Queries
    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE isActiveSession = 1 LIMIT 1")
    suspend fun getActiveUser(): UserAccountEntity?

    @Query("SELECT * FROM user_accounts ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastLoggedInUser(): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE isActiveSession = 1 LIMIT 1")
    fun observeActiveUser(): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccountEntity)

    @Query("UPDATE user_accounts SET isActiveSession = 0")
    suspend fun clearActiveSessions()

    @Query("UPDATE user_accounts SET isActiveSession = 1 WHERE username = :username")
    suspend fun setActiveSession(username: String)

    @Query("UPDATE user_accounts SET displayName = :displayName, avatarColor = :avatarColor, userStatus = :userStatus, statusMessage = :statusMessage, bio = :bio WHERE username = :username")
    suspend fun updateProfile(username: String, displayName: String, avatarColor: Long, userStatus: String, statusMessage: String, bio: String)

    @Query("UPDATE user_accounts SET userStatus = :userStatus WHERE username = :username")
    suspend fun updateUserStatus(username: String, userStatus: String)
}

@Database(entities = [ChatMessageEntity::class, RoomEntity::class, UserAccountEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
