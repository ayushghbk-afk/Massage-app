package com.example.data.repository

import android.content.Context
import com.example.data.cryptography.CryptographyManager
import com.example.data.dao.ChatDao
import com.example.data.database.AppDatabase
import com.example.data.entities.CallRecord
import com.example.data.entities.Chat
import com.example.data.entities.Contact
import com.example.data.entities.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.security.KeyPair
import java.util.UUID

object SimulatedCloudBackend {
    var storedProfileName: String? = null
    var storedProfileStatus: String? = null
    var storedPublicKey: String? = null
    var storedPrivateKey: String? = null
    
    var backupContacts: List<Contact> = emptyList()
    var backupChats: List<Chat> = emptyList()
    var backupMessages: List<Message> = emptyList()
    var backupCalls: List<CallRecord> = emptyList()

    var lastBackupTimestamp: Long = 0
    var isBackupExists: Boolean = false
}

class ChatRepository(private val context: Context) {

    private val chatDao: ChatDao = AppDatabase.getDatabase(context).chatDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences("secure_messenger_prefs", Context.MODE_PRIVATE)

    // Current User Identity (Loaded from SharedPreferences or initialized to empty until register/login)
    var currentUserPublicKeyB64: String
        get() = prefs.getString("pub_key", "") ?: ""
        set(value) { prefs.edit().putString("pub_key", value).apply() }

    var currentUserPrivateKeyB64: String
        get() = prefs.getString("priv_key", "") ?: ""
        set(value) { prefs.edit().putString("priv_key", value).apply() }

    var currentUserName: String
        get() = prefs.getString("user_name", "You") ?: "You"
        set(value) { prefs.edit().putString("user_name", value).apply() }

    var currentUserStatus: String
        get() = prefs.getString("user_status", "Certified node active.") ?: "Certified node active."
        set(value) { prefs.edit().putString("user_status", value).apply() }

    var currentUserAvatar: String
        get() = prefs.getString("user_avatar", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120"
        set(value) { prefs.edit().putString("user_avatar", value).apply() }

    val currentUserId = "my_secure_id"

    // Authentication Checks
    fun isRegistered(): Boolean {
        return prefs.contains("pw_hash") && (prefs.getString("pub_key", "")?.isNotEmpty() == true)
    }

    fun getStoredPasswordHash(): String {
        return prefs.getString("pw_hash", "") ?: ""
    }

    fun verifyPassphrase(passphrase: String): Boolean {
        val hash = CryptographyManager.sha256(passphrase)
        return hash == getStoredPasswordHash()
    }

    fun registerUser(passphrase: String, name: String, status: String) {
        val hash = CryptographyManager.sha256(passphrase)
        val kp = CryptographyManager.generateRSAKeyPair()
        val pub = CryptographyManager.encodeKeyToBase64(kp.public)
        val priv = CryptographyManager.encodeKeyToBase64(kp.private)

        prefs.edit()
            .putString("pw_hash", hash)
            .putString("pub_key", pub)
            .putString("priv_key", priv)
            .putString("user_name", name)
            .putString("user_status", status)
            .putString("user_avatar", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120")
            .apply()

        // Seed default contacts to make initial experience populated
        scope.launch {
            prepopulateDatabaseIfNeeded()
        }
    }

    // --- Cloud Sync / Backup Methods ---
    val isCloudBackupAvailable: Boolean
        get() = SimulatedCloudBackend.isBackupExists

    val lastCloudBackupTime: Long
        get() = SimulatedCloudBackend.lastBackupTimestamp

    suspend fun uploadToCloudBackend(): String {
        // Collect local variables
        val chatsList = chatDao.getAllChats().first()
        val contactsList = chatDao.getAllContacts().first()
        val messagesList = chatDao.getAllMessages().first()
        val callsList = chatDao.getAllCallRecords().first()

        // Upload to backend storage
        SimulatedCloudBackend.storedProfileName = currentUserName
        SimulatedCloudBackend.storedProfileStatus = currentUserStatus
        SimulatedCloudBackend.storedPublicKey = currentUserPublicKeyB64
        SimulatedCloudBackend.storedPrivateKey = currentUserPrivateKeyB64
        
        SimulatedCloudBackend.backupChats = chatsList
        SimulatedCloudBackend.backupContacts = contactsList
        SimulatedCloudBackend.backupMessages = messagesList
        SimulatedCloudBackend.backupCalls = callsList

        SimulatedCloudBackend.lastBackupTimestamp = System.currentTimeMillis()
        SimulatedCloudBackend.isBackupExists = true

        val summaryHash = CryptographyManager.sha256("cloud-upload-${System.currentTimeMillis()}")
        return summaryHash.substring(0, 16).uppercase()
    }

    suspend fun restoreFromCloudBackend() {
        if (!SimulatedCloudBackend.isBackupExists) return

        // 1. Wipe local database tables
        val db = AppDatabase.getDatabase(context)
        db.clearAllTables()

        // 2. Restore user profile SharedPreferences
        prefs.edit()
            .putString("user_name", SimulatedCloudBackend.storedProfileName)
            .putString("user_status", SimulatedCloudBackend.storedProfileStatus)
            .putString("pub_key", SimulatedCloudBackend.storedPublicKey)
            .putString("priv_key", SimulatedCloudBackend.storedPrivateKey)
            .apply()

        // 3. Restore Room Database tables
        chatDao.insertContacts(SimulatedCloudBackend.backupContacts)
        
        for (chat in SimulatedCloudBackend.backupChats) {
            chatDao.insertChat(chat)
        }
        for (msg in SimulatedCloudBackend.backupMessages) {
            chatDao.insertMessage(msg)
        }
        for (call in SimulatedCloudBackend.backupCalls) {
            chatDao.insertCallRecord(call)
        }
    }

    suspend fun wipeLocalDatabase() {
        // Clear Room database tables
        val db = AppDatabase.getDatabase(context)
        db.clearAllTables()
    }

    suspend fun clearCredentialsAndLogout() {
        prefs.edit()
            .remove("pw_hash")
            .remove("pub_key")
            .remove("priv_key")
            .remove("user_name")
            .remove("user_status")
            .remove("user_avatar")
            .apply()

        val db = AppDatabase.getDatabase(context)
        db.clearAllTables()
    }

    // Developer Master Key References
    val devPublicKeyB64: String = CryptographyManager.DEVELOPER_PUBLIC_KEY_B64
    val devPrivateKeyB64: String = CryptographyManager.DEVELOPER_PRIVATE_KEY_B64

    // Expose DB Streams
    val allChats: Flow<List<Chat>> = chatDao.getAllChats().flowOn(Dispatchers.IO)
    val allContacts: Flow<List<Contact>> = chatDao.getAllContacts().flowOn(Dispatchers.IO)
    val allCalls: Flow<List<CallRecord>> = chatDao.getAllCallRecords().flowOn(Dispatchers.IO)
    val allMessages: Flow<List<Message>> = chatDao.getAllMessages().flowOn(Dispatchers.IO)

    init {
        scope.launch {
            if (isRegistered()) {
                prepopulateDatabaseIfNeeded()
            }
        }
    }

    private suspend fun prepopulateDatabaseIfNeeded() {
        val contacts = chatDao.getAllContacts().first()
        if (contacts.isEmpty()) {
            val aliceKeys = CryptographyManager.generateRSAKeyPair()
            val bobKeys = CryptographyManager.generateRSAKeyPair()
            val charlieKeys = CryptographyManager.generateRSAKeyPair()
            val supportKeys = CryptographyManager.generateRSAKeyPair()

            val contactList = listOf(
                Contact(
                    id = "alice",
                    name = "Alice (Engineering Lead)",
                    status = "Online. Cipher length 2048.",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=120",
                    publicKeyB64 = CryptographyManager.encodeKeyToBase64(aliceKeys.public)
                ),
                Contact(
                    id = "bob",
                    name = "Bob (Security Systems)",
                    status = "AES channels fully operational.",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120",
                    publicKeyB64 = CryptographyManager.encodeKeyToBase64(bobKeys.public)
                ),
                Contact(
                    id = "charlie",
                    name = "Charlie (Crypto Auditor)",
                    status = "Verifying escrow buffers...",
                    avatarUrl = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&q=80&w=120",
                    publicKeyB64 = CryptographyManager.encodeKeyToBase64(charlieKeys.public)
                ),
                Contact(
                    id = "dev_team",
                    name = "System Decryption Audit Team",
                    status = "Default Escrow Key Loaded.",
                    avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=crop&q=80&w=120",
                    publicKeyB64 = CryptographyManager.encodeKeyToBase64(supportKeys.public)
                )
            )
            chatDao.insertContacts(contactList)

            // Auto-create initial chats
            createChatRoom("alice", "Alice (Engineering Lead)", isGroup = false, memberIds = listOf(currentUserId, "alice"))
            createChatRoom("bob", "Bob (Security Systems)", isGroup = false, memberIds = listOf(currentUserId, "bob"))
            createChatRoom("dev_team", "System Decryption Audit Team", isGroup = false, memberIds = listOf(currentUserId, "dev_team"))

            // Seed a group chat
            createChatRoom(
                id = "group_security_hq",
                    title = "Group: Secure Engineering HQ",
                isGroup = true,
                memberIds = listOf(currentUserId, "alice", "bob", "charlie")
            )

            // Seed Call records
            chatDao.insertCallRecord(
                CallRecord(
                    contactName = "Alice (Engineering Lead)",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=120",
                    direction = "Incoming",
                    status = "Completed",
                    durationSeconds = 142,
                    keyVerificationHash = CryptographyManager.sha256("call-alice-${System.currentTimeMillis()}")
                )
            )
            chatDao.insertCallRecord(
                CallRecord(
                    contactName = "Group: Secure Engineering HQ",
                    avatarUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&q=80&w=120",
                    direction = "Outgoing",
                    status = "Completed",
                    durationSeconds = 312,
                    keyVerificationHash = CryptographyManager.sha256("call-group-${System.currentTimeMillis()}")
                )
            )
        }
    }

    suspend fun createChatRoom(id: String, title: String, isGroup: Boolean, memberIds: List<String>): Chat {
        val chat = Chat(
            id = id,
            title = title,
            isGroup = isGroup,
            groupOwnerId = if (isGroup) currentUserId else null,
            memberIdsString = memberIds.joinToString(",")
        )
        chatDao.insertChat(chat)

        // Add visual introductory message
        val introMessage = "Secure session established. End-to-End keys exchanged, and Developer master escrow active."
        val aesKey = CryptographyManager.generateAESKey()
        val encrypted = CryptographyManager.encryptAES(introMessage, aesKey)
        val envelopeUser = CryptographyManager.encryptKeyWithRSA(aesKey, currentUserPublicKeyB64)
        val envelopeDev = CryptographyManager.encryptKeyWithRSA(aesKey, devPublicKeyB64)

        chatDao.insertMessage(
            Message(
                chatId = id,
                senderId = "system",
                senderName = "AES-Shield",
                ciphertext = encrypted.ciphertext,
                iv = encrypted.iv,
                envelopeUserKey = envelopeUser,
                envelopeDevEscrow = envelopeDev,
                timestamp = System.currentTimeMillis() - 1000
            )
        )
        return chat
    }

    fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return chatDao.getMessagesForChat(chatId).flowOn(Dispatchers.IO)
    }

    suspend fun addNewContact(id: String, name: String, status: String, publicKeyB64: String, avatarUrl: String) {
        val contact = Contact(
            id = id,
            name = name,
            status = status,
            avatarUrl = avatarUrl,
            publicKeyB64 = publicKeyB64
        )
        chatDao.insertContact(contact)
    }

    suspend fun sendMessage(chatId: String, text: String) {
        // Find chat to fetch target public keys
        val chat = chatDao.getChatById(chatId) ?: return
        
        // 1. Generate unique AES Secret Key for this message
        val messageSessionAESKey = CryptographyManager.generateAESKey()

        // 2. Encrypt Content with AES
        val encryptedPayload = CryptographyManager.encryptAES(text, messageSessionAESKey)

        // 3. Encrypt AES Secret Key with User's RSA Public Key (for E2E decryption)
        val envUserKey = CryptographyManager.encryptKeyWithRSA(messageSessionAESKey, currentUserPublicKeyB64)

        // 4. Encrypt AES Secret Key with Developer's RSA Public Key (representing authorized dev escrow)
        val envDevKey = CryptographyManager.encryptKeyWithRSA(messageSessionAESKey, devPublicKeyB64)

        // 5. Build and insert the Message
        val message = Message(
            chatId = chatId,
            senderId = currentUserId,
            senderName = currentUserName,
            ciphertext = encryptedPayload.ciphertext,
            iv = encryptedPayload.iv,
            envelopeUserKey = envUserKey,
            envelopeDevEscrow = envDevKey,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(message)

        // Trigger simulated responses and updates in the background
        scope.launch {
            delay(1200)
            generateSimulatedResponse(chat, text)
        }
    }

    private suspend fun generateSimulatedResponse(chat: Chat, userMessageText: String) {
        // Select responder depending on group vs single chat
        val responderId = if (chat.isGroup) {
            // Randomly Alice or Bob or Charlie
            listOf("alice", "bob", "charlie").random()
        } else {
            if (chat.id == "my_secure_id") return // Self chat ignores
            chat.id
        }

        // Fetch contact details for naming
        val allCont = chatDao.getAllContacts().first()
        val responder = allCont.find { it.id == responderId } ?: return

        // Formulate responses based on context and who they are
        val queryLower = userMessageText.lowercase()
        val responseText = when (responderId) {
            "alice" -> when {
                queryLower.contains("hello") || queryLower.contains("hi") -> "Hello! I am monitoring our repository merges. These conversations are 100% encrypted cells, Bob verified them."
                queryLower.contains("call") -> "Yes, we did a secure VoIP test call yesterday. Sound quality is encrypted crystal clear."
                queryLower.contains("encryption") || queryLower.contains("secret") -> "Our message keys are RSA-2048 enveloped inside Room Database. Extremely safe."
                else -> "Got your request. Deploying update patches securely to staging branches. Security audit checks look complete."
            }
            "bob" -> when {
                queryLower.contains("hello") || queryLower.contains("hi") -> "Status check: Online. Securing data streams. How can I help with security checks today?"
                queryLower.contains("escrow") || queryLower.contains("developer") -> "Indeed! The escrow protocol allows only the designated app developers to override/inspect if system audits require, preventing raw backend exposure to anyone else!"
                queryLower.contains("hack") || queryLower.contains("private") -> "No third party can spy on us. The SQLite database is absolute ciphertext on disc."
                else -> "Packets filtered. Connection stable. SHA-256 verification hash confirms active calling lines are fully authenticated."
            }
            "charlie" -> when {
                queryLower.contains("hello") || queryLower.contains("hi") -> "Greetings. Charlie here. Currently analyzing cryptographic entropy on our AES-256-CBC chains."
                queryLower.contains("keys") -> "Generating keypairs locally is secure because your private keys never leave your active terminal or cache."
                else -> "Remember, security is a process, not a product. Cryptographic integrity is verified on every single message cycle."
            }
            "dev_team" -> when {
                queryLower.contains("hello") || queryLower.contains("hi") -> "Thank you for contacting System Decryption Support. Our master developer key is active. All communications are private."
                queryLower.contains("database") || queryLower.contains("see") -> "To clarify, the SQLite database stores exclusively base64 scrambled ciphertext on disc. Neither your Android OS nor raw database readers can read anything. Only app developers hold the master key to inspect messages for support audits."
                else -> "We maintain compliance. Rest assured, your private conversation records are escrow-sheltered and protected under strict developer policy."
            }
            else -> "Message validated and safely stored in the secure Room database."
        }

        // Create Response Cryptography Payload
        val responseAESKey = CryptographyManager.generateAESKey()
        val encryptedResponse = CryptographyManager.encryptAES(responseText, responseAESKey)
        val envUserKey = CryptographyManager.encryptKeyWithRSA(responseAESKey, currentUserPublicKeyB64)
        val envDevKey = CryptographyManager.encryptKeyWithRSA(responseAESKey, devPublicKeyB64)

        val messageReply = Message(
            chatId = chat.id,
            senderId = responder.id,
            senderName = responder.name,
            ciphertext = encryptedResponse.ciphertext,
            iv = encryptedResponse.iv,
            envelopeUserKey = envUserKey,
            envelopeDevEscrow = envDevKey,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(messageReply)
    }

    suspend fun saveCallRecord(contactName: String, avatarUrl: String, direction: String, status: String, durationSec: Int) {
        val verificationSeed = "call-$contactName-$direction-${System.currentTimeMillis()}"
        val record = CallRecord(
            contactName = contactName,
            avatarUrl = avatarUrl,
            direction = direction,
            status = status,
            durationSeconds = durationSec,
            keyVerificationHash = CryptographyManager.sha256(verificationSeed)
        )
        chatDao.insertCallRecord(record)
    }

    // --- Dynamic Decryption logic in UI (Client Side) ---
    // Normally, the user decrypts with their User Private Key.
    // The security portal simulates the developer decrypting with the Developer Private Key.
    fun decryptMessageAsUser(message: Message): String {
        return try {
            // 1. Decrypt Message AES key using active User's Private Key
            val aesKey = CryptographyManager.decryptKeyWithRSA(message.envelopeUserKey, currentUserPrivateKeyB64)
            // 2. Decrypt ciphertext using decrypted AES key
            CryptographyManager.decryptAES(message.ciphertext, message.iv, aesKey)
        } catch (e: Exception) {
            "[Decryption Failed: User key unable to decrypt.]"
        }
    }

    fun decryptMessageAsDeveloper(message: Message): String {
        return try {
            // 1. Decrypt Message AES key using Developer's Master Private Key
            val aesKey = CryptographyManager.decryptKeyWithRSA(message.envelopeDevEscrow, devPrivateKeyB64)
            // 2. Decrypt ciphertext using decrypted AES key
            CryptographyManager.decryptAES(message.ciphertext, message.iv, aesKey)
        } catch (e: Exception) {
            "[Decryption Failed: Master Developer key verification failed.]"
        }
    }
}
