package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val ciphertext: String,          // Base64 encrypted text
    val iv: String,                  // AES initialization vector
    val envelopeUserKey: String,     // AES session key encrypted with recipient's public key
    val envelopeDevEscrow: String,   // AES session key encrypted with Developer's master public key
    val timestamp: Long = System.currentTimeMillis()
)
