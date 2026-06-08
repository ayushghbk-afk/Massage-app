package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val id: String,
    val name: String,
    val status: String,
    val avatarUrl: String,
    val publicKeyB64: String // RSA public key for securing conversations
)
