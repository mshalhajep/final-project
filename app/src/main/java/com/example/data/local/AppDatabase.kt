package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["targetRoomOrPeerId"])]
)
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
    val isDownloaded: Boolean = false,
    val isRead: Boolean = true,
    val isEdited: Boolean = false,
    val deliveryStatus: Int = 0,
    val replyToId: String? = null,
    val replyToSender: String? = null,
    val replyToText: String? = null
)

data class UnreadCountRecord(
    val targetRoomOrPeerId: String,
    val unreadCount: Int
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
    val isPrivate: Boolean = false,
    val passwordHash: String? = null
)

@Entity(tableName = "blocked_peers")
data class BlockedPeerEntity(
    @PrimaryKey
    val peerId: String,
    val peerName: String,
    val blockedAt: Long = System.currentTimeMillis()
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
    val avatarUri: String? = null,
    val avatarBase64: String? = null,
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

    @Query("UPDATE chat_messages SET isRead = 1 WHERE targetRoomOrPeerId = :targetId")
    suspend fun markMessagesAsRead(targetId: String)

    /**
     * Monotonic delivery-status upgrade (SENT -> DELIVERED -> READ):
     * a late ACK can never downgrade an already-READ message.
     */
    @Query("UPDATE chat_messages SET deliveryStatus = :status WHERE id = :messageId AND deliveryStatus < :status")
    suspend fun upgradeMessageDeliveryStatus(messageId: String, status: Int)

    @Query("UPDATE chat_messages SET content = :newContent, isEdited = 1 WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, newContent: String)

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): ChatMessageEntity?

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

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

    @Query("UPDATE user_accounts SET displayName = :displayName, avatarColor = :avatarColor, userStatus = :userStatus, statusMessage = :statusMessage, bio = :bio, avatarUri = :avatarUri, avatarBase64 = :avatarBase64 WHERE username = :username")
    suspend fun updateProfile(username: String, displayName: String, avatarColor: Long, userStatus: String, statusMessage: String, bio: String, avatarUri: String?, avatarBase64: String?)

    @Query("SELECT targetRoomOrPeerId, COUNT(*) as unreadCount FROM chat_messages WHERE isMine = 0 AND isRead = 0 GROUP BY targetRoomOrPeerId")
    fun getUnreadCounts(): Flow<List<UnreadCountRecord>>

    @Query("UPDATE user_accounts SET userStatus = :userStatus WHERE username = :username")
    suspend fun updateUserStatus(username: String, userStatus: String)

    // Blocked Peers Queries (S-02)
    @Query("SELECT * FROM blocked_peers ORDER BY blockedAt DESC")
    fun getAllBlockedPeers(): Flow<List<BlockedPeerEntity>>

    @Query("SELECT * FROM blocked_peers")
    suspend fun getBlockedPeersList(): List<BlockedPeerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedPeer(peer: BlockedPeerEntity)

    @Query("DELETE FROM blocked_peers WHERE peerId = :peerId")
    suspend fun deleteBlockedPeer(peerId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_peers WHERE peerId = :peerId)")
    suspend fun isPeerBlocked(peerId: String): Boolean
}

@Database(entities = [ChatMessageEntity::class, RoomEntity::class, UserAccountEntity::class, BlockedPeerEntity::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        /** v7: adds chat_messages.deliveryStatus for delivery/read receipts (no data loss). */
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(connection: androidx.sqlite.db.SupportSQLiteDatabase) {
                connection.execSQL(
                    "ALTER TABLE chat_messages ADD COLUMN deliveryStatus INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** v8: adds chat_messages reply fields for quoted reply messages. */
        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToSender TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN replyToText TEXT DEFAULT NULL")
            }
        }

        /** v9: adds index on chat_messages.targetRoomOrPeerId for faster queries. */
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_targetRoomOrPeerId ON chat_messages(targetRoomOrPeerId)")
            }
        }

        /** v10: adds passwordHash to saved_rooms and creates blocked_peers table (S-01, S-02). */
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_rooms ADD COLUMN passwordHash TEXT DEFAULT NULL")
                db.execSQL("CREATE TABLE IF NOT EXISTS blocked_peers (peerId TEXT NOT NULL PRIMARY KEY, peerName TEXT NOT NULL, blockedAt INTEGER NOT NULL)")
            }
        }
    }
}
