package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String, // Dynamic string UUID, or contact identifier for direct chats
    val title: String,          // Group name or User name
    val isGroup: Boolean,       // True if group system
    val groupOwnerId: String?,  // Maker of the group
    val memberIdsString: String, // Comma-separated recipient contact IDs (e.g., "alice,bob")
    val createdAt: Long = System.currentTimeMillis()
)
