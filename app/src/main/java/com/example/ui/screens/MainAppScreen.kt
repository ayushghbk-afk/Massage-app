package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.zIndex
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.cryptography.CryptographyManager
import com.example.data.entities.CallRecord
import com.example.data.entities.Chat
import com.example.data.entities.Contact
import com.example.data.entities.Message
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

// Custom Dark Cybersecurity Theme Colors
val SecureDarkBackground = Color(0xFF0A0E17)
val SecureSurface = Color(0xFF121824)
val SecureSurfaceVariant = Color(0xFF1D263B)
val SecureAccentGreen = Color(0xFF00E676)
val SecureElectricBlue = Color(0xFF00B0FF)
val SecureMutedText = Color(0xFF94A3B8)
val SecureSenderBubble = Color(0xFF0B2545)
val SecureReceipientBubble = Color(0xFF134074)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isRegistered by viewModel.isRegistered.collectAsStateWithLifecycle()
    val focusedTab = remember { mutableIntStateOf(0) } // 0: Chats, 1: Contacts, 2: Calls, 3: Security Audit, 4: Cloud Sync
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()

    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SecureDarkBackground, Color(0xFF05070B))
                )
            )
    ) {
        // System Wide Cryptographic Notification Overlay
        val activeNotification by viewModel.activeNotification.collectAsStateWithLifecycle()
        AnimatedVisibility(
            visible = activeNotification != null,
            enter = fadeIn(animationSpec = tween(350)) + expandVertically(),
            exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                .zIndex(99f)
        ) {
            activeNotification?.let { notification ->
                SecureNotificationBanner(
                    notification = notification,
                    onDismiss = { viewModel.dismissNotification() },
                    onClick = {
                        viewModel.dismissNotification()
                        if (notification.type == "MESSAGE" && notification.chatId != null) {
                            viewModel.setActiveChat(notification.chatId)
                        }
                    }
                )
            }
        }

        if (!isLoggedIn) {
            if (!isRegistered) {
                RegistrationScreen(viewModel = viewModel)
            } else {
                LoginScreen(viewModel = viewModel)
            }
        } else {
            // Main Screen Router
            if (activeChatId != null) {
                ChatDetailScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.setActiveChat(null) }
                )
            } else {
                Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield Icon",
                                    tint = SecureAccentGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SECURE MESSENGER",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.5.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Escrow encryption: Operational",
                                        fontSize = 11.sp,
                                        color = SecureAccentGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SecureDarkBackground.copy(alpha = 0.95f),
                            titleContentColor = Color.White
                        ),
                        actions = {
                            if (focusedTab.intValue == 0) {
                                IconButton(onClick = { showCreateGroupDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.GroupAdd,
                                        contentDescription = "Create Group System",
                                        tint = SecureElectricBlue
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = SecureDarkBackground,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = focusedTab.intValue == 0,
                            onClick = { focusedTab.intValue = 0 },
                            icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                            label = { Text("Chats", fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SecureAccentGreen,
                                selectedTextColor = SecureAccentGreen,
                                unselectedIconColor = SecureMutedText,
                                unselectedTextColor = SecureMutedText,
                                indicatorColor = SecureSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = focusedTab.intValue == 1,
                            onClick = { focusedTab.intValue = 1 },
                            icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                            label = { Text("Contacts", fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SecureAccentGreen,
                                selectedTextColor = SecureAccentGreen,
                                unselectedIconColor = SecureMutedText,
                                unselectedTextColor = SecureMutedText,
                                indicatorColor = SecureSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = focusedTab.intValue == 2,
                            onClick = { focusedTab.intValue = 2 },
                            icon = { Icon(Icons.Default.Phone, contentDescription = "Calls") },
                            label = { Text("Calls", fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SecureAccentGreen,
                                selectedTextColor = SecureAccentGreen,
                                unselectedIconColor = SecureMutedText,
                                unselectedTextColor = SecureMutedText,
                                indicatorColor = SecureSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = focusedTab.intValue == 3,
                            onClick = { focusedTab.intValue = 3 },
                            icon = { Icon(Icons.Default.Terminal, contentDescription = "Security console") },
                            label = { Text("Audit Key", fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SecureAccentGreen,
                                selectedTextColor = SecureAccentGreen,
                                unselectedIconColor = SecureMutedText,
                                unselectedTextColor = SecureMutedText,
                                indicatorColor = SecureSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            selected = focusedTab.intValue == 4,
                            onClick = { focusedTab.intValue = 4 },
                            icon = { Icon(Icons.Default.Cloud, contentDescription = "Cloud backup sync") },
                            label = { Text("Cloud Sync", fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SecureAccentGreen,
                                selectedTextColor = SecureAccentGreen,
                                unselectedIconColor = SecureMutedText,
                                unselectedTextColor = SecureMutedText,
                                indicatorColor = SecureSurfaceVariant
                            )
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (focusedTab.intValue) {
                        0 -> ConversationsTab(
                            viewModel = viewModel,
                            onChatSelected = { viewModel.setActiveChat(it.id) }
                        )
                        1 -> ContactsTab(
                            viewModel = viewModel,
                            onStartConversation = { contact ->
                                viewModel.startSecureConversation(contact)
                                focusedTab.intValue = 0
                            }
                        )
                        2 -> CallsTab(
                            viewModel = viewModel
                        )
                        3 -> SecurityAuditTab(
                            viewModel = viewModel
                        )
                        4 -> CloudSyncTab(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Animated Call Screen Overlay
        AnimatedVisibility(
            visible = activeCall != null,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400))
        ) {
            activeCall?.let { call ->
                CallOverlayScreen(
                    call = call,
                    onAnswer = { viewModel.answerCall() },
                    onDecline = { viewModel.rejectOrHangUpCall() }
                )
            }
        }

        // Group creation pop up
        if (showCreateGroupDialog) {
            CreateGroupDialog(
                viewModel = viewModel,
                onDismiss = { showCreateGroupDialog = false },
                onCreated = { title, ids ->
                    viewModel.createGroupChat(title, ids)
                    showCreateGroupDialog = false
                }
            )
        }
        }
    }
}

// --- CONVERSATIONS LIST TAB ---
@Composable
fun ConversationsTab(
    viewModel: ChatViewModel,
    onChatSelected: (Chat) -> Unit
) {
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val allMessagesRaw by viewModel.allMessagesRaw.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    if (chats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Empty chat",
                    tint = SecureMutedText,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No conversations active",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Establish a secure key session to start messaging.",
                    color = SecureMutedText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "ACTIVE SYMMETRIC CHANNELS",
                    color = SecureMutedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                    fontFamily = FontFamily.Monospace
                )
            }

            items(chats) { chat ->
                // Fetch last message preview
                val lastMessageForChat = allMessagesRaw
                    .filter { it.chatId == chat.id }
                    .maxByOrNull { it.timestamp }
                
                // Get decrypted version if available, else standard intro
                val displayMsg = if (lastMessageForChat != null) {
                    if (lastMessageForChat.senderId == "system") {
                        "Session keys verified."
                    } else {
                        viewModel.decryptMessage(lastMessageForChat)
                    }
                } else {
                    "Establishing handshake..."
                }

                // Fetch avatar identifier
                val avatarUrl = if (chat.isGroup) {
                    "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&q=80&w=120"
                } else {
                    contacts.find { it.id == chat.id }?.avatarUrl ?: ""
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecureSurface)
                        .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(12.dp))
                        .clickable { onChatSelected(chat) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chat avatar with security badge
                    Box(modifier = Modifier.size(50.dp)) {
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(SecureSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (chat.isGroup) Icons.Default.Groups else Icons.Default.Person,
                                contentDescription = "Avatar Placeholder",
                                tint = SecureElectricBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Round Active status dot
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(SecureAccentGreen)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = chat.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = "Fully Encrypted",
                                tint = SecureAccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "AES",
                                color = SecureAccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayMsg,
                            color = SecureMutedText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// --- SECURE CHAT DETAILS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val messages by viewModel.activeChatMessages.collectAsStateWithLifecycle()

    val chat = chats.find { it.id == activeChatId } ?: return
    val opposingContact = contacts.find { it.id == chat.id }

    var textInput by remember { mutableStateOf("") }
    var showCryptoDetailsSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = SecureDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showCryptoDetailsSheet = true }
                    ) {
                        Box(modifier = Modifier.size(36.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(SecureSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (chat.isGroup) Icons.Default.Groups else Icons.Default.Person,
                                    contentDescription = "Avatar Details",
                                    tint = SecureAccentGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                chat.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SecureAccentGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "E2E + DEV ESCROW SHIELD",
                                    fontSize = 9.sp,
                                    color = SecureAccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Goback",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SecureSurface
                ),
                actions = {
                    // Call triggering buttons
                    IconButton(onClick = {
                        val avatarUrl = opposingContact?.avatarUrl ?: ""
                        viewModel.initiateCall(chat.title, avatarUrl, isIncoming = false)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Secure audio Call",
                            tint = SecureAccentGreen
                        )
                    }
                    IconButton(onClick = {
                        val avatarUrl = opposingContact?.avatarUrl ?: ""
                        viewModel.initiateCall(chat.title, avatarUrl, isIncoming = false)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Secure video Call",
                            tint = SecureElectricBlue
                        )
                    }
                    IconButton(onClick = { showCryptoDetailsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Verify Encryption Matrix",
                            tint = SecureMutedText
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Typing response box
            Surface(
                color = SecureSurface,
                tonalElevation = 4.dp,
                modifier = Modifier.imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "Locked message...",
                                color = SecureMutedText,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SecureDarkBackground,
                            unfocusedContainerColor = SecureDarkBackground,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        containerColor = SecureAccentGreen,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send secure AES payload",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Secure Session Notification Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "verified",
                        tint = SecureAccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Messages are encrypted locally at SQLite. Tap key icon to explain.",
                        color = SecureAccentGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isCurrentUser = message.senderId == "my_secure_id"
                    val isSystem = message.senderId == "system"

                    if (isSystem) {
                        // System handshake notification row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SecureSurface)
                                        .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🔒 ${viewModel.decryptMessage(message)}",
                                        color = SecureElectricBlue,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Regular chat message bubbles
                        val align = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = align
                        ) {
                            Column(
                                horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
                            ) {
                                if (chat.isGroup && !isCurrentUser) {
                                    Text(
                                        text = message.senderName,
                                        color = SecureElectricBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                                    )
                                }

                                val bubbleColor = if (isCurrentUser) SecureSenderBubble else SecureReceipientBubble
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                                                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(bubbleColor)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    // Live decrypting content
                                    val decrypted = viewModel.decryptMessage(message)
                                    Text(
                                        text = decrypted,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatTimestamp(message.timestamp),
                                            color = SecureMutedText,
                                            fontSize = 9.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked message",
                                            tint = SecureAccentGreen,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Handshake Explainer Details Bottom Sheet dialog representing full Cryptographic Envelope
    if (showCryptoDetailsSheet) {
        Dialog(onDismissRequest = { showCryptoDetailsSheet = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurface),
                border = BorderStroke(1.dp, SecureAccentGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = null,
                            tint = SecureAccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CHANNEL ENCRYPTION MATRIX",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This session encapsulates 2 mathematical envelopes simultaneously:",
                        color = Color.White,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    // Envelope 1: User Key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecureDarkBackground)
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = SecureElectricBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Envelope 1: Recipient User Key", color = SecureElectricBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("The random AES message session key is encrypted with your local RSA-2048 public key. Your device's private key alone can decrypt it.", color = SecureMutedText, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Envelope 2: Developer Escrow
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecureDarkBackground)
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = SecureAccentGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Envelope 2: Developer Escrow Key", color = SecureAccentGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text("The same message session key is encrypted with the Developer's Master Public Key. This guarantees administrative recovery, matching the developer trust policy.", color = SecureMutedText, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showCryptoDetailsSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SecureAccentGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Verifications Completed", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// --- SECURE CONTACTS LIST TAB ---
@Composable
fun ContactsTab(
    viewModel: ChatViewModel,
    onStartConversation: (Contact) -> Unit
) {
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var selectedContactForDetails by remember { mutableStateOf<Contact?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var visualKeyScrambler by remember { mutableStateOf(false) }

    // Helper to scramble text visually for simulated encryption
    val scrambleText = { text: String, active: Boolean ->
        if (!active) {
            text
        } else {
            text.map { char ->
                val hex = char.code.toString(16).uppercase()
                if (hex.length == 1) "0$hex" else hex
            }.joinToString(" ").let {
                if (it.length > 28) it.substring(0, 25) + "..." else it
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ENCRYPTED USER PROFILES",
                color = SecureMutedText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            
            // Toggle Visual Scrambler
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (visualKeyScrambler) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (visualKeyScrambler) SecureAccentGreen else SecureMutedText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (visualKeyScrambler) "SCRAMBLE LOCAL" else "UNSEAL PROFILE",
                    color = if (visualKeyScrambler) SecureAccentGreen else SecureMutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Switch(
                    checked = visualKeyScrambler,
                    onCheckedChange = { visualKeyScrambler = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = SecureAccentGreen,
                        uncheckedThumbColor = SecureMutedText,
                        uncheckedTrackColor = SecureDarkBackground
                    ),
                    modifier = Modifier.scale(0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Info Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SecureSurfaceVariant.copy(alpha = 0.4f))
                .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint = SecureElectricBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Asymmetric Key Ring", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Contacts store certified RSA-2048 Public Keys used to seal symmetric AES session folders.", color = SecureMutedText, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ADD NEW CONTACT ACTION ROW
        Button(
            onClick = { showAddContactDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = SecureSurface,
                contentColor = SecureElectricBlue
            ),
            border = BorderStroke(1.dp, SecureElectricBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SecureElectricBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GENERATE NEW PARTNER KEY", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (contacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No secure contacts stored locally.", color = SecureMutedText, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SecureSurface)
                            .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(12.dp))
                            .clickable { selectedContactForDetails = contact }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar with glowing border indicating encryption
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SecureSurfaceVariant)
                                .border(1.5.dp, if (visualKeyScrambler) SecureAccentGreen else SecureElectricBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (visualKeyScrambler) SecureAccentGreen else SecureElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = scrambleText(contact.name, visualKeyScrambler),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (visualKeyScrambler) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Encrypted",
                                        tint = SecureAccentGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Profile",
                                        tint = SecureElectricBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = scrambleText(contact.status, visualKeyScrambler),
                                color = SecureMutedText,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Display Public Key Fingerprint
                            val fingerprint = remember(contact.publicKeyB64) {
                                CryptographyManager.sha256(contact.publicKeyB64).substring(0, 16).uppercase()
                            }
                            Text(
                                text = "FP: ${fingerprint.chunked(4).joinToString("-")}",
                                color = SecureAccentGreen.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Connect session button
                        IconButton(
                            onClick = { onStartConversation(contact) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecureSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Initiate E2E Chat",
                                tint = SecureAccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogue: View Profile Key Details
    if (selectedContactForDetails != null) {
        val contact = selectedContactForDetails!!
        Dialog(onDismissRequest = { selectedContactForDetails = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurface),
                border = BorderStroke(1.dp, SecureElectricBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SecureDarkBackground)
                            .border(2.dp, SecureAccentGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = SecureAccentGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = contact.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = contact.status,
                        color = SecureMutedText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "RSA-2048 CRYPTOGRAPHIC PUBLIC KEY",
                        color = SecureElectricBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Key scrollable text
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecureDarkBackground)
                            .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = contact.publicKeyB64,
                                    color = SecureAccentGreen,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedContactForDetails = null }) {
                            Text("Dismiss", color = SecureMutedText, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = {
                                onStartConversation(contact)
                                selectedContactForDetails = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecureAccentGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Initialize Handshake", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // Dialogue: Add Secure Contact
    if (showAddContactDialog) {
        var nameInput by remember { mutableStateOf("") }
        var statusInput by remember { mutableStateOf("") }
        var customPublicKey by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddContactDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurface),
                border = BorderStroke(1.dp, SecureElectricBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = SecureElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ADD SECURE IDENTITY KEY",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Contact Name", color = SecureMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("Dave (HQ SecOps)", color = SecureMutedText, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SecureDarkBackground,
                            unfocusedContainerColor = SecureDarkBackground,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Status Payload (Details)", color = SecureMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = statusInput,
                        onValueChange = { statusInput = it },
                        placeholder = { Text("Certified node active.", color = SecureMutedText, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SecureDarkBackground,
                            unfocusedContainerColor = SecureDarkBackground,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Asymmetric Public Key (RSA Base64)", color = SecureMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Leave empty to auto-generate fully qualified RSA-2048 keys for E2E handshakes.", color = SecureElectricBlue, fontSize = 9.sp, lineHeight = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = customPublicKey,
                        onValueChange = { customPublicKey = it },
                        placeholder = { Text("Paste B64 RSA key (Optional)", color = SecureMutedText, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SecureDarkBackground,
                            unfocusedContainerColor = SecureDarkBackground,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddContactDialog = false }) {
                            Text("Cancel", color = SecureMutedText, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank()) {
                                    val finalStatus = statusInput.ifBlank { "Certified node session" }
                                    viewModel.addNewContact(nameInput, finalStatus, customPublicKey)
                                    showAddContactDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecureAccentGreen,
                                contentColor = Color.Black
                            ),
                            enabled = nameInput.isNotBlank()
                        ) {
                            Text("Verify & Save Identity", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// --- CALLS TAB ---
@Composable
fun CallsTab(
    viewModel: ChatViewModel
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "SECURE SINGLE CALLS ENGINE",
            color = SecureMutedText,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
            fontFamily = FontFamily.Monospace
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecureSurfaceVariant.copy(alpha = 0.4f))
                        .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SecureAccentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("SRTP Cipher Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Acoustic calls are digitised with 256-bit AES streams, validating caller integrity tokens.", color = SecureMutedText, fontSize = 11.sp)
                    }
                }
            }

            if (calls.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No call records found.", color = SecureMutedText, fontSize = 13.sp)
                    }
                }
            } else {
                items(calls) { call ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SecureSurface)
                            .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SecureSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call History Icon",
                                tint = if (call.status == "Missed") Color.Red else SecureAccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = call.contactName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (call.direction == "Incoming") Icons.Default.CallReceived else Icons.Default.CallMade,
                                    contentDescription = null,
                                    tint = SecureElectricBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${call.direction} • ${if (call.status == "Missed") "Missed" else "${call.durationSeconds}s completed"}",
                                    color = SecureMutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "HASH CERT",
                                color = SecureAccentGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = call.keyVerificationHash.substring(0, 8).uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick contacts list to start new calls
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DIAL RECIPIENTS",
                    color = SecureMutedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                    fontFamily = FontFamily.Monospace
                )
            }

            val phoneContacts = contacts.filter { it.id != "dev_team" }
            items(phoneContacts) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecureSurface)
                        .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.initiateCall(contact.name, contact.avatarUrl, isIncoming = false)
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SecureSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = SecureElectricBlue)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(contact.status, color = SecureAccentGreen, fontSize = 11.sp)
                    }

                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "Trigger call",
                        tint = SecureAccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- ACTIVE ENCRYPTED SINGLE CALL SCREEN (FULL LAYOUT DIALOG SIMULATOR) ---
@Composable
fun CallOverlayScreen(
    call: ChatViewModel.ActiveCall,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    // Halo pulse animation
    val infiniteTransition = rememberInfiniteTransition()
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(24.dp)
    ) {
        // Glowing background gradient accents
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            colors = listOf(
                                SecureElectricBlue.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        // Top encryption state
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurfaceVariant.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, SecureAccentGreen)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = null,
                        tint = SecureAccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SRTP AES-256 SECURED LINE",
                        color = SecureAccentGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Center contact profile with glowing pulse halo
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing Halo Circle
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(haloPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SecureAccentGreen.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Avatar container
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(SecureSurface)
                        .border(2.dp, SecureAccentGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SecureAccentGreen,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = call.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (call.status) {
                    "dialing" -> "ESTABLISHING CIPHER HANDSHAKE..."
                    "ringing" -> "RINGING (SECURED)..."
                    "active" -> "CONNECTED: ${formatDuration(call.durationSeconds)}"
                    else -> "CONNECTING..."
                },
                color = if (call.status == "active") SecureAccentGreen else SecureMutedText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )

            if (call.status == "active") {
                Spacer(modifier = Modifier.height(24.dp))
                // Cryptographic calling hash verification key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecureSurface)
                        .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "LINE INTEGRITY TOKEN",
                            color = SecureMutedText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = call.verificationHash,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Both callers should match this hash value",
                            color = SecureAccentGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Call Control Buttons at Bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (call.status == "ringing" && call.direction == "Incoming") {
                // Large Green accept button
                FloatingActionButton(
                    onClick = onAnswer,
                    containerColor = SecureAccentGreen,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Answer Call", modifier = Modifier.size(28.dp))
                }
            }

            // Decline/Hang Up Button
            FloatingActionButton(
                onClick = onDecline,
                containerColor = Color.Red,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "Hang Up", modifier = Modifier.size(28.dp))
            }
        }
    }
}

// --- SECURITY & DECODE CONSOLE TAB (DEVELOPER AUDIT PORTAL) ---
@Composable
fun SecurityAuditTab(
    viewModel: ChatViewModel
) {
    val clipboard = LocalClipboardManager.current
    val rawMessages by viewModel.allMessagesRaw.collectAsStateWithLifecycle()
    
    // Developer Decryption Escrow Mode State
    var useDeveloperEscrowBypass by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "CRYPTOGRAPHIC SECURITY CONSOLE",
                color = SecureMutedText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                fontFamily = FontFamily.Monospace
            )
        }

        // Key overview card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurface),
                border = BorderStroke(1.dp, SecureSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = SecureElectricBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "YOUR DEVICE RSA KEY PAIR",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Public Key (Sent to other chat peers):",
                        color = SecureElectricBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecureDarkBackground)
                            .clickable {
                                clipboard.setText(AnnotatedString(viewModel.currentUserPublicKeyB64))
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.currentUserPublicKeyB64,
                            color = SecureMutedText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy to clipboard", tint = SecureElectricBlue, modifier = Modifier.size(14.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Private Key (Strictly restricted to runtime memory):",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecureDarkBackground)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = viewModel.currentUserPrivateKeyB64,
                            color = Color.Red.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Developer Key & Policy Explanation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurface),
                border = BorderStroke(1.dp, SecureAccentGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SecureAccentGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "DEVELOPER ESCROW POLICY BOARD",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Each chatting payload contains a secondary encrypted session key envelope targeting the Developer Master RSA Key. As requested, raw SQLite storage is ciphertext, but app developers can audit communications under security clearance policy.",
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Developer Public Escrow Key
                    Text(
                        "Active Developer Master Public Key (Escrow target):",
                        color = SecureAccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecureDarkBackground)
                            .clickNoRipple {
                                clipboard.setText(AnnotatedString(viewModel.devPublicKeyB64))
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.devPublicKeyB64,
                            color = SecureAccentGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SecureAccentGreen, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // LIVE SQLITE DATABASE INSPECTOR & RUNTIME DECRYPTION BYPASS
        item {
            Text(
                text = "LIVE SQLITE CELL EXAMINER",
                color = SecureMutedText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 6.dp),
                fontFamily = FontFamily.Monospace
            )
        }

        // Overriding Toggle Box representing Developer clearance toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (useDeveloperEscrowBypass) SecureReceipientBubble.copy(alpha = 0.5f) else SecureSurface)
                    .border(
                        BorderStroke(
                            1.dp,
                            if (useDeveloperEscrowBypass) SecureAccentGreen else SecureSurfaceVariant
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { useDeveloperEscrowBypass = !useDeveloperEscrowBypass }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (useDeveloperEscrowBypass) SecureAccentGreen else SecureSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (useDeveloperEscrowBypass) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (useDeveloperEscrowBypass) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Developer Escrow Bypass",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (useDeveloperEscrowBypass) "DECRYPTED WITH DEVELOPER MASTER KEY" else "VIEWING SCOPED SECURE RAW DATABASE RECORDS",
                        color = if (useDeveloperEscrowBypass) SecureAccentGreen else SecureMutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Switch(
                    checked = useDeveloperEscrowBypass,
                    onCheckedChange = { useDeveloperEscrowBypass = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = SecureAccentGreen,
                        uncheckedThumbColor = SecureMutedText,
                        uncheckedTrackColor = SecureDarkBackground
                    )
                )
            }
        }

        if (rawMessages.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No records stored in SQLite messages.db", color = SecureMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        } else {
            items(rawMessages) { msg ->
                if (msg.senderId != "system") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SecureDarkBackground),
                        border = BorderStroke(1.dp, SecureSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = SecureElectricBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "messages.db [ID: ${msg.id}]",
                                        color = SecureElectricBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (useDeveloperEscrowBypass) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (useDeveloperEscrowBypass) SecureAccentGreen else SecureMutedText,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (useDeveloperEscrowBypass) "Decrypted" else "Scrambled",
                                        color = if (useDeveloperEscrowBypass) SecureAccentGreen else SecureMutedText,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Display cell content
                            val cellContent = if (useDeveloperEscrowBypass) {
                                viewModel.decryptMessageAsDev(msg)
                            } else {
                                msg.ciphertext
                            }

                            Text(
                                text = "sender_id: ${msg.senderId}\nciphertext: $cellContent\niv: ${msg.iv}\nenvelope_dev: ${msg.envelopeDevEscrow.substring(0, 16)}...",
                                color = if (useDeveloperEscrowBypass) SecureAccentGreen else SecureMutedText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- GROUP CREATION DIALOG ---
@Composable
fun CreateGroupDialog(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onCreated: (String, List<String>) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val selectableContacts = contacts.filter { it.id != "dev_team" }
    
    val selectedContactIds = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SecureSurface),
            border = BorderStroke(1.dp, SecureElectricBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = SecureElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "INITIALIZE SECURE GROUP SYSTEM",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Group name text field
                TextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = { Text("Group title...", color = SecureMutedText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SecureDarkBackground,
                        unfocusedContainerColor = SecureDarkBackground,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Select chat participants to load keys:",
                    color = SecureMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter list of contacts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(selectableContacts) { contact ->
                        val isSelected = selectedContactIds.contains(contact.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SecureSenderBubble else SecureDarkBackground)
                                .clickable {
                                    if (isSelected) {
                                        selectedContactIds.remove(contact.id)
                                    } else {
                                        selectedContactIds.add(contact.id)
                                    }
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SecureSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = SecureElectricBlue, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                contact.name,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (isSelected) {
                                        selectedContactIds.remove(contact.id)
                                    } else {
                                        selectedContactIds.add(contact.id)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SecureAccentGreen,
                                    uncheckedColor = SecureMutedText,
                                    checkmarkColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = SecureMutedText, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (titleInput.isNotBlank() && selectedContactIds.isNotEmpty()) {
                                onCreated(titleInput, selectedContactIds.toList())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecureAccentGreen,
                            contentColor = Color.Black
                        ),
                        enabled = titleInput.isNotBlank() && selectedContactIds.isNotEmpty()
                    ) {
                        Text("Exchange Keys & Create", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// --- UTILITY STYLINGS / FORMATTERS ---

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

fun formatTimestamp(millis: Long): String {
    val date = java.util.Date(millis)
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(date)
}

// Special click Modifier that removes native Material ripple effect for code components
@Composable
fun Modifier.clickNoRipple(onClick: () -> Unit): Modifier = this.then(
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
)

// --- REGISTRATION SCREEN ---
@Composable
fun RegistrationScreen(
    viewModel: ChatViewModel
) {
    var aliasInput by remember { mutableStateOf("") }
    var statusInput by remember { mutableStateOf("") }
    var passphraseInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SecureDarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SecureSurface),
            border = BorderStroke(1.dp, SecureElectricBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SecureAccentGreen,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "IDENTITY SEED HANDSHAKE",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Establish your secure local communication cell.",
                    color = SecureMutedText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Node Alias Input
                Text(
                    text = "CLIENT IDENTIFIER NODE NAME",
                    color = SecureElectricBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = aliasInput,
                    onValueChange = { aliasInput = it },
                    placeholder = { Text("e.g. Node 808, Mr. Hackerdon", color = SecureMutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SecureDarkBackground,
                        unfocusedContainerColor = SecureDarkBackground,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Node Status Payload Info
                Text(
                    text = "NODE STATUS PAYLOAD",
                    color = SecureElectricBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = statusInput,
                    onValueChange = { statusInput = it },
                    placeholder = { Text("e.g. RSA operational. Listening.", color = SecureMutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SecureDarkBackground,
                        unfocusedContainerColor = SecureDarkBackground,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Passphrase Input
                Text(
                    text = "SECURE DATABASE PASSPHRASE",
                    color = SecureElectricBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = passphraseInput,
                    onValueChange = { passphraseInput = it },
                    placeholder = { Text("Master password to lock database keys", color = SecureMutedText.copy(alpha = 0.5f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SecureDarkBackground,
                        unfocusedContainerColor = SecureDarkBackground,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isGenerating) {
                    CircularProgressIndicator(color = SecureAccentGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generating RSA-2048 keypair keys...",
                        color = SecureAccentGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Button(
                        onClick = {
                            if (aliasInput.isNotBlank() && passphraseInput.isNotBlank()) {
                                isGenerating = true
                                val finalStatus = statusInput.ifBlank { "Cipher node operational." }
                                viewModel.registerUser(passphraseInput, aliasInput, finalStatus)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecureAccentGreen,
                            contentColor = Color.Black
                        ),
                        enabled = aliasInput.isNotBlank() && passphraseInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "INITIALIZE SECURE KEYRINGS",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// --- LOGIN SCREEN ---
@Composable
fun LoginScreen(
    viewModel: ChatViewModel
) {
    var passphraseInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }
    val currentUserName by viewModel.currentUserName.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SecureDarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SecureSurface),
            border = BorderStroke(1.dp, SecureElectricBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = SecureElectricBlue,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "VAULT LOCK VERIFICATION",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Active Node identity: $currentUserName",
                    color = SecureMutedText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ENTER MASTER SECURE PASSPHRASE",
                    color = SecureElectricBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = passphraseInput,
                    onValueChange = { 
                        passphraseInput = it
                        loginError = false
                    },
                    placeholder = { Text("••••••••••••••••", color = SecureMutedText.copy(alpha = 0.4f), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SecureDarkBackground,
                        unfocusedContainerColor = SecureDarkBackground,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                if (loginError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔒 Cryptographic verify signature failed.",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val success = viewModel.loginUser(passphraseInput)
                        if (!success) {
                            loginError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecureElectricBlue,
                        contentColor = Color.White
                    ),
                    enabled = passphraseInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "UNSEAL SYSTEM VAULT",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        viewModel.deleteIdentityCloudAndLogOut()
                    }
                ) {
                    Text(
                        text = "⚠️ Destroy Credentials & Re-initialize",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- CLOUD BACKEND SYNC AND STORAGE TAB ---
@Composable
fun CloudSyncTab(
    viewModel: ChatViewModel
) {
    val currentUserName by viewModel.currentUserName.collectAsStateWithLifecycle()
    val currentUserStatus by viewModel.currentUserStatus.collectAsStateWithLifecycle()
    val isSyncInProgress by viewModel.isSyncInProgress.collectAsStateWithLifecycle()
    val cloudLogs by viewModel.cloudLogs.collectAsStateWithLifecycle()
    val isCloudBackupAvailable by viewModel.isCloudBackupAvailable.collectAsStateWithLifecycle()
    val lastCloudBackupTime by viewModel.lastCloudBackupTime.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var openWipeWarningDialog by remember { mutableStateOf(false) }

    val terminalListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to end of logs on change
    LaunchedEffect(cloudLogs.size) {
        if (cloudLogs.isNotEmpty()) {
            scope.launch {
                terminalListState.animateScrollToItem(cloudLogs.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // PROFILE SECTION CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SecureSurface),
            border = BorderStroke(1.dp, SecureSurfaceVariant),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SecureDarkBackground)
                        .border(2.dp, SecureElectricBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = SecureElectricBlue,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUserName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = currentUserStatus,
                        color = SecureMutedText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showEditProfileDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecureSurfaceVariant)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile Node", tint = SecureAccentGreen, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CLOUD STATUS CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = SecureSurface),
            border = BorderStroke(1.dp, if (isCloudBackupAvailable) SecureAccentGreen else SecureElectricBlue),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (isCloudBackupAvailable) SecureAccentGreen else SecureMutedText,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURE CLOUD BACKEND REPOSITORY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Network Host: https://api.secnode-datacenter.cloud/v1",
                    color = SecureMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Storage Endpoint: /sync/vault-backup",
                    color = SecureMutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Backup Parcel Status: " + if (isCloudBackupAvailable) {
                        "SEALED PACKET STORED (${formatTimestamp(lastCloudBackupTime)} UTC)"
                    } else {
                        "NO BACKUP DETECTED"
                    },
                    color = if (isCloudBackupAvailable) SecureAccentGreen else Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // CLOUD ACTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Backup action button
            Button(
                onClick = { viewModel.uploadBackup {} },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecureAccentGreen,
                    contentColor = Color.Black
                ),
                enabled = !isSyncInProgress
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PUSH TO CLOUD", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            // Restore action button
            Button(
                onClick = { viewModel.restoreBackup {} },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecureElectricBlue,
                    contentColor = Color.White
                ),
                enabled = !isSyncInProgress && isCloudBackupAvailable
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PULL DATABASE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Danger logout/wipe rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.logoutUser() },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SecureMutedText),
                border = BorderStroke(1.dp, SecureSurfaceVariant)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("LOCK SESSION", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = { openWipeWarningDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.Red),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WIPE NODE DATA", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // REAL-TIME CLI CONSOLE TERMINAL MONITOR
        Text(
            text = "SYNC TERMINAL STREAM:",
            color = SecureMutedText,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SecureDarkBackground)
                .border(1.dp, SecureSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (cloudLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "CLI Terminal Standing By...\nClick Push or Pull Sync to establish sockets.",
                        color = SecureMutedText.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = terminalListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(cloudLogs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("Success") || log.contains("✔️")) SecureAccentGreen else if (log.contains("🔒") || log.contains("🛡️")) SecureElectricBlue else Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Modal Edit Profile Node Dialog
    if (showEditProfileDialog) {
        var nameField by remember { mutableStateOf(currentUserName) }
        var statusField by remember { mutableStateOf(currentUserStatus) }

        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurface),
                border = BorderStroke(1.dp, SecureElectricBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "UPDATE NODE IDENTITY PROFILE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Alias Node NAME", color = SecureMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SecureDarkBackground,
                            unfocusedContainerColor = SecureDarkBackground,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Bio Status Signature", color = SecureMutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = statusField,
                        onValueChange = { statusField = it },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SecureDarkBackground,
                            unfocusedContainerColor = SecureDarkBackground,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditProfileDialog = false }) {
                            Text("Cancel", color = SecureMutedText, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nameField.isNotBlank()) {
                                    viewModel.updateProfile(nameField, statusField)
                                    showEditProfileDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecureAccentGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Apply Signature", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // Danger Zone Warning Dialog
    if (openWipeWarningDialog) {
        Dialog(onDismissRequest = { openWipeWarningDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecureSurface),
                border = BorderStroke(1.dp, Color.Red),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "DESTRUCTIVE REWIPE PROTOCOL",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This will wipe ALL chats, keyring, message logs, and local profile from SQLite Room database storage. You can restore your data ONLY if you pushed a cloud backup envelope.",
                        color = SecureMutedText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { openWipeWarningDialog = false }) {
                            Text("Safe Abort", color = SecureMutedText, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = {
                                viewModel.wipeLocalDataAndLogOut()
                                openWipeWarningDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            )
                        ) {
                            Text("EXECUTE PURGE", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// In-App floating cryptographic alert banner
@Composable
fun SecureNotificationBanner(
    notification: ChatViewModel.InAppNotification,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SecureSurface),
        border = BorderStroke(
            1.5.dp,
            when (notification.type) {
                "MESSAGE" -> SecureMutedText
                "SYNC" -> SecureAccentGreen
                "SECURITY" -> Color.Red
                else -> SecureAccentGreen
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SecureDarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        "MESSAGE" -> Icons.Default.Chat
                        "SYNC" -> Icons.Default.Cloud
                        "SECURITY" -> Icons.Default.Warning
                        else -> Icons.Default.Shield
                    },
                    contentDescription = null,
                    tint = if (notification.type == "MESSAGE") SecureElectricBlue else if (notification.type == "SYNC") SecureAccentGreen else Color.Red,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.body,
                    color = SecureMutedText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Alert",
                    tint = SecureMutedText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
