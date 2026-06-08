package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cryptography.CryptographyManager
import com.example.data.entities.CallRecord
import com.example.data.entities.Chat
import com.example.data.entities.Contact
import com.example.data.entities.Message
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)

    // Identity Expose
    val currentUserPublicKeyB64: String get() = repository.currentUserPublicKeyB64
    val currentUserPrivateKeyB64: String get() = repository.currentUserPrivateKeyB64
    val devPublicKeyB64: String get() = repository.devPublicKeyB64
    val devPrivateKeyB64: String get() = repository.devPrivateKeyB64

    // Auth & Profile states
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered = _isRegistered.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _currentUserName = MutableStateFlow("You")
    val currentUserName = _currentUserName.asStateFlow()

    private val _currentUserStatus = MutableStateFlow("Certified node active.")
    val currentUserStatus = _currentUserStatus.asStateFlow()

    private val _currentUserAvatar = MutableStateFlow("")
    val currentUserAvatar = _currentUserAvatar.asStateFlow()

    // Cloud Sync States
    private val _isCloudBackupAvailable = MutableStateFlow(false)
    val isCloudBackupAvailable = _isCloudBackupAvailable.asStateFlow()

    private val _lastCloudBackupTime = MutableStateFlow(0L)
    val lastCloudBackupTime = _lastCloudBackupTime.asStateFlow()

    private val _cloudLogs = MutableStateFlow<List<String>>(emptyList())
    val cloudLogs = _cloudLogs.asStateFlow()

    private val _isSyncInProgress = MutableStateFlow(false)
    val isSyncInProgress = _isSyncInProgress.asStateFlow()

    // Cryptographic In-App Secure Notifications
    data class InAppNotification(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val body: String,
        val type: String, // "MESSAGE", "SYNC", "SECURITY", "CALL"
        val chatId: String? = null
    )

    private val _activeNotification = MutableStateFlow<InAppNotification?>(null)
    val activeNotification = _activeNotification.asStateFlow()

    private var lastObservedMessageTimestamp = System.currentTimeMillis()

    fun triggerNotification(title: String, body: String, type: String, chatId: String? = null) {
        viewModelScope.launch {
            val notification = InAppNotification(title = title, body = body, type = type, chatId = chatId)
            _activeNotification.value = notification
            delay(4000)
            if (_activeNotification.value?.id == notification.id) {
                _activeNotification.value = null
            }
        }
    }

    fun dismissNotification() {
        _activeNotification.value = null
    }

    init {
        _isRegistered.value = repository.isRegistered()
        _currentUserName.value = repository.currentUserName
        _currentUserStatus.value = repository.currentUserStatus
        _currentUserAvatar.value = repository.currentUserAvatar
        _isCloudBackupAvailable.value = repository.isCloudBackupAvailable
        _lastCloudBackupTime.value = repository.lastCloudBackupTime

        // Start real-time observer for incoming peer messages to show floating cyber alert
        viewModelScope.launch {
            allMessagesRaw.collect { list ->
                if (list.isEmpty()) return@collect
                val latest = list.maxByOrNull { it.timestamp } ?: return@collect
                if (latest.timestamp > lastObservedMessageTimestamp && latest.senderId != "my_secure_id") {
                    val activeId = _activeChatId.value
                    if (latest.chatId != activeId) {
                        val decrypted = decryptMessage(latest)
                        triggerNotification(
                            title = "DECRYPTED INCOMING RAW PACKET: ${latest.senderName.uppercase()}",
                            body = decrypted,
                            type = "MESSAGE",
                            chatId = latest.chatId
                        )
                    }
                }
                lastObservedMessageTimestamp = maxOf(lastObservedMessageTimestamp, latest.timestamp)
            }
        }
    }

    // Auth & Registration Actions
    fun registerUser(passphrase: String, name: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.registerUser(passphrase, name, status)
            _isRegistered.value = true
            _isLoggedIn.value = true
            _currentUserName.value = repository.currentUserName
            _currentUserStatus.value = repository.currentUserStatus
            _currentUserAvatar.value = repository.currentUserAvatar
        }
    }

    fun loginUser(passphrase: String): Boolean {
        val verified = repository.verifyPassphrase(passphrase)
        if (verified) {
            _isLoggedIn.value = true
            _currentUserName.value = repository.currentUserName
            _currentUserStatus.value = repository.currentUserStatus
            _currentUserAvatar.value = repository.currentUserAvatar
            _isCloudBackupAvailable.value = repository.isCloudBackupAvailable
            _lastCloudBackupTime.value = repository.lastCloudBackupTime
        }
        return verified
    }

    fun logoutUser() {
        _isLoggedIn.value = false
    }

    fun updateProfile(name: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.currentUserName = name
            repository.currentUserStatus = status
            _currentUserName.value = name
            _currentUserStatus.value = status
        }
    }

    fun wipeLocalDataAndLogOut() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.wipeLocalDatabase()
            _isLoggedIn.value = false
            // Reset state to empty local details but preserve registration
            _currentUserName.value = "You"
            _currentUserStatus.value = "Certified node active."
            triggerNotification(
                title = "LOCAL DATABASE STORAGE SHREDDED",
                body = "All local SQLite conversation history logs and device caching layers were permanently scrubbed.",
                type = "SECURITY"
            )
        }
    }

    fun deleteIdentityCloudAndLogOut() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCredentialsAndLogout()
            _isRegistered.value = false
            _isLoggedIn.value = false
            _currentUserName.value = "You"
            _currentUserStatus.value = "Certified node active."
            _isCloudBackupAvailable.value = repository.isCloudBackupAvailable
            _lastCloudBackupTime.value = repository.lastCloudBackupTime
        }
    }

    // Cloud Sync Core Actions
    fun uploadBackup(onSuccess: (String) -> Unit) {
        if (_isSyncInProgress.value) return
        _isSyncInProgress.value = true
        _cloudLogs.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            addLog("⚡ Initializing secure channel to cloud backup node...")
            delay(1000)
            addLog("🔒 Resolving asymmetric RSA-2048 escrow handshakes...")
            delay(800)
            addLog("⚙️ Compiling user database records (Keyring, History)...")
            delay(1000)
            addLog("🛡️ Wrapping packages with symmetric AES-GCM-256 seal...")
            delay(1200)
            addLog("📤 Transmitting 14.8 KB secure telemetry blocks...")
            
            val checksum = repository.uploadToCloudBackend()
            delay(1200)

            addLog("✔️ Transmit complete. ACK-200. Backend verification hash:")
            addLog("   $checksum")
            
            _isCloudBackupAvailable.value = repository.isCloudBackupAvailable
            _lastCloudBackupTime.value = repository.lastCloudBackupTime
            _isSyncInProgress.value = false
            triggerNotification(
                title = "CLOUD BACKUP VERIFIED",
                body = "Encrypted database parcel uploaded successfully. SHA-256 Checksum: ${checksum.take(18)}...",
                type = "SYNC"
            )
            onSuccess(checksum)
        }
    }

    fun restoreBackup(onSuccess: () -> Unit) {
        if (_isSyncInProgress.value) return
        _isSyncInProgress.value = true
        _cloudLogs.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            addLog("📡 Fetching encrypted database parcel indexing...")
            delay(1000)
            addLog("🔑 Authenticating remote profile certificates...")
            delay(1000)
            addLog("⬇️ Downloading secure binary blobs from backend node...")
            delay(1200)
            addLog("📥 Appending synced keys, contacts, chats, and call history...")
            
            repository.restoreFromCloudBackend()
            delay(1500)

            addLog("✔️ Decrypting local database cell... Success!")
            addLog("♻️ Identity, profiles, and 100% messages restored.")
            
            _currentUserName.value = repository.currentUserName
            _currentUserStatus.value = repository.currentUserStatus
            _currentUserAvatar.value = repository.currentUserAvatar
            _isSyncInProgress.value = false
            triggerNotification(
                title = "CLOUD RESTORATION COMPLETE",
                body = "Locally cached RSA keypair, private keyring, and messages fully restored.",
                type = "SYNC"
            )
            onSuccess()
        }
    }

    private fun addLog(message: String) {
        _cloudLogs.value = _cloudLogs.value + message
    }

    // Master flows from Room
    val chats: StateFlow<List<Chat>> = repository.allChats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calls: StateFlow<List<CallRecord>> = repository.allCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessagesRaw: StateFlow<List<Message>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    // Decryptor flows
    val activeChatMessages: StateFlow<List<Message>> = _activeChatId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getMessagesForChat(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Call state variables
    data class ActiveCall(
        val name: String,
        val avatarUrl: String,
        val direction: String, // "Incoming" or "Outgoing"
        val status: String,    // "dialing", "ringing", "active"
        val durationSeconds: Int,
        val verificationHash: String
    )

    private val _activeCall = MutableStateFlow<ActiveCall?>(null)
    val activeCall = _activeCall.asStateFlow()

    private var callTimerJob: Job? = null

    // Message decryption utility for view level (decrypting locally with User Key)
    fun decryptMessage(message: Message): String {
        return repository.decryptMessageAsUser(message)
    }

    // Message decryption utility for developer view level (decrypting using Developer Key escrow)
    fun decryptMessageAsDev(message: Message): String {
        return repository.decryptMessageAsDeveloper(message)
    }

    // --- Navigation ---
    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    // --- Chat Actions ---
    fun sendMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(chatId, text)
        }
    }

    fun createGroupChat(title: String, selectedContactIds: List<String>) {
        if (title.isBlank() || selectedContactIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val members = selectedContactIds + repository.currentUserId
            val uniqueGroupId = "group_${UUID.randomUUID()}"
            val chat = repository.createChatRoom(uniqueGroupId, title, isGroup = true, memberIds = members)
            _activeChatId.value = chat.id
        }
    }

    fun startSecureConversation(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = chats.value.find { it.id == contact.id }
            if (existing != null) {
                _activeChatId.value = existing.id
            } else {
                val chat = repository.createChatRoom(
                    id = contact.id,
                    title = contact.name,
                    isGroup = false,
                    memberIds = listOf(repository.currentUserId, contact.id)
                )
                _activeChatId.value = chat.id
            }
        }
    }

    fun addNewContact(name: String, status: String, customPublicKey: String? = null) {
        val uniqueId = "contact_${UUID.randomUUID().toString().substring(0, 8)}"
        val finalPubKey = if (!customPublicKey.isNullOrBlank()) {
            customPublicKey
        } else {
            val kp = CryptographyManager.generateRSAKeyPair()
            CryptographyManager.encodeKeyToBase64(kp.public)
        }
        val randomAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=120"
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.addNewContact(uniqueId, name, status, finalPubKey, randomAvatar)
        }
    }

    // --- Call Actions ---
    fun initiateCall(contactName: String, avatarUrl: String, isIncoming: Boolean) {
        val hashInput = "call-$contactName-${System.currentTimeMillis()}"
        val verificationHash = CryptographyManager.sha256(hashInput).substring(0, 16).uppercase()

        _activeCall.value = ActiveCall(
            name = contactName,
            avatarUrl = avatarUrl,
            direction = if (isIncoming) "Incoming" else "Outgoing",
            status = if (isIncoming) "ringing" else "dialing",
            durationSeconds = 0,
            verificationHash = verificationHash
        )

        // Run connection handshake simulation
        viewModelScope.launch {
            if (isIncoming) {
                // Wait for answer
            } else {
                delay(1500)
                _activeCall.value = _activeCall.value?.copy(status = "ringing")
                delay(1500)
                answerCall()
            }
        }
    }

    fun answerCall() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(status = "active")
        startCallTimer()
    }

    fun rejectOrHangUpCall() {
        val current = _activeCall.value ?: return
        callTimerJob?.cancel()
        
        viewModelScope.launch {
            // Save call record to database
            repository.saveCallRecord(
                contactName = current.name,
                avatarUrl = current.avatarUrl,
                direction = current.direction,
                status = if (current.status == "active") "Answered" else "Missed",
                durationSec = current.durationSeconds
            )
            _activeCall.value = null
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCall.value = _activeCall.value?.let {
                    it.copy(durationSeconds = it.durationSeconds + 1)
                }
            }
        }
    }
}
