package com.vaishali.aichat.data.local

import androidx.room.*
import java.util.UUID

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatWithMessages(
    @Embedded val chat: ChatEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "chatId"
    )
    val messages: List<MessageEntity>
)
