package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,
    val avatarUrl: String,
    val direction: String,        // "Incoming" or "Outgoing"
    val status: String,           // "Answered", "Missed", "Rejected"
    val durationSeconds: Int,     // Call duration
    val keyVerificationHash: String, // SHA-256 verification hash of the secure Line
    val timestamp: Long = System.currentTimeMillis()
)
