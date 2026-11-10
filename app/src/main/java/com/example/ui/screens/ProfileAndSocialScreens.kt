package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FollowRecord
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.ui.components.AvatarView
import com.example.ui.components.FilteredMediaBox
import com.example.ui.theme.InstaBlue
import com.example.ui.theme.InstaMagenta
import com.example.ui.theme.OnlineGreen
import com.example.ui.util.MediaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileUser: UserProfile,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isBlocked: Boolean,
    userPosts: List<PostItem>,
    savedPosts: List<PostItem>,
    allUsers: List<UserProfile>,
    followingIds: Set<String>,
    followersList: List<FollowRecord>,
    followingList: List<FollowRecord>,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onBackClick: (() -> Unit)? = null,
    onEditProfileClick: () -> Unit,
    onFollowToggle: () -> Unit,
    onMessageClick: () -> Unit,
    onBlockToggle: () -> Unit,
    onFollowOtherUser: (UserProfile) -> Unit,
    onRemoveFollower: (String) -> Unit,
    onOpenPermissionsWizard: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFollowSheetType by remember { mutableStateOf<String?>(null) } // "followers" | "following"
    var showSettingsSheet by remember { mutableStateOf(false) }

    if (onBackClick != null) {
        BackHandler { onBackClick() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profileUser.username, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (profileUser.isOnline) OnlineGreen else Color.Gray)
                    )
                }
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche Jayein")
                    }
                }
            },
            actions = {
                if (isOwnProfile) {
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier.testTag("profile_settings_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AvatarView(
                    avatarData = profileUser.avatarData,
                    name = profileUser.name.ifBlank { profileUser.username },
                    size = 82.dp,
                    isOnline = profileUser.isOnline
                )
                Spacer(modifier = Modifier.width(20.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("Posts", userPosts.size.toString()) {}
                    StatColumn("Followers", profileUser.followersCount.toString()) {
                        showFollowSheetType = "followers"
                    }
                    StatColumn("Following", profileUser.followingCount.toString()) {
                        showFollowSheetType = "following"
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(profileUser.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (profileUser.bio.isNotBlank()) {
                Text(profileUser.bio, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (isOwnProfile) {
                OutlinedButton(
                    onClick = onEditProfileClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_profile_btn")
                ) {
                    Text("Edit Profile / Bio")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (isFollowing) {
                        OutlinedButton(
                            onClick = onFollowToggle,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("profile_unfollow_btn")
                        ) {
                            Text("Following")
                        }
                    } else {
                        Button(
                            onClick = onFollowToggle,
                            colors = ButtonDefaults.buttonColors(containerColor = InstaBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("profile_follow_btn")
                        ) {
                            Text("Follow", color = Color.White)
                        }
                    }
                    OutlinedButton(
                        onClick = onMessageClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("profile_message_btn")
                    ) {
                        Text("Message")
                    }
                    IconButton(onClick = onBlockToggle, modifier = Modifier.testTag("profile_block_btn")) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = if (isBlocked) "Unblock" else "Block",
                            tint = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Posts (${userPosts.size})") }
            )
            if (isOwnProfile) {
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Saved (${savedPosts.size})") }
                )
            }
        }

        val gridPosts = if (selectedTab == 0) userPosts else savedPosts
        if (gridPosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selectedTab == 0) "Abhi koi post nahi hai." else "Abhi koi saved post nahi hai.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(gridPosts, key = { it.id }) { post ->
                    Box(modifier = Modifier.aspectRatio(1f)) {
                        FilteredMediaBox(
                            mediaUrl = post.mediaUrl,
                            mediaType = post.mediaType,
                            filterName = post.filterName,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Followers / Following Sheet
    if (showFollowSheetType != null) {
        ModalBottomSheet(onDismissRequest = { showFollowSheetType = null }) {
            val isFollowersTab = showFollowSheetType == "followers"
            val records = if (isFollowersTab) followersList else followingList
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = if (isFollowersTab) "Followers List" else "Following List",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (records.isEmpty()) {
                    Text(
                        text = if (isFollowersTab) "Abhi koi follower nahi hai." else "Aapne abhi kisi ko follow nahi kiya.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(records, key = { it.id }) { rec ->
                            val targetId = if (isFollowersTab) rec.followerId else rec.followingId
                            val targetUser = allUsers.find { it.userId == targetId }
                            val targetName = targetUser?.username
                                ?: if (isFollowersTab) rec.followerUsername else rec.followingUsername
                            val amFollowing = targetId in followingIds

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarView(
                                    avatarData = targetUser?.avatarData ?: "",
                                    name = targetName,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = targetName.ifBlank { "User" },
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isFollowersTab && isOwnProfile) {
                                    TextButton(onClick = { onRemoveFollower(targetId) }) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (targetUser != null) {
                                    if (amFollowing) {
                                        OutlinedButton(onClick = { onFollowOtherUser(targetUser) }) {
                                            Text("Unfollow")
                                        }
                                    } else {
                                        Button(
                                            onClick = { onFollowOtherUser(targetUser) },
                                            colors = ButtonDefaults.buttonColors(containerColor = InstaBlue)
                                        ) {
                                            Text("Follow", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Settings Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark Mode", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isDarkMode, onCheckedChange = onToggleDarkMode)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSettingsSheet = false
                            onEditProfileClick()
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Naam aur Bio Change Karein")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSettingsSheet = false
                            onOpenPermissionsWizard()
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("App Permissions Dekhein (Hindi Guide)")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSettingsSheet = false
                            onLogoutClick()
                        }
                        .padding(vertical = 14.dp)
                        .testTag("logout_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Logout Karein", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(6.dp)
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onBack: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit
) {
    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche")
                }
            }
        )
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Abhi koi naya notification nahi hai.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNotificationClick(notif) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                avatarData = notif.actorAvatar,
                                name = notif.actorUsername,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = notif.message,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (!notif.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(InstaBlue)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUserId: String,
    chats: List<ChatThread>,
    allUsers: List<UserProfile>,
    onBack: () -> Unit,
    onOpenChat: (UserProfile) -> Unit
) {
    BackHandler { onBack() }

    val otherUsers = remember(allUsers, currentUserId) {
        allUsers.filter { it.userId != currentUserId }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Messages / Chat", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche")
                }
            }
        )

        if (otherUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Abhi chat ke liye koi aur user online nahi hai.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(otherUsers, key = { it.userId }) { user ->
                    val existingChat = chats.find { user.userId in it.participantIds }
                    val isTyping = existingChat?.typingUserIds?.contains(user.userId) == true

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChat(user) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarView(
                                avatarData = user.avatarData,
                                name = user.name.ifBlank { user.username },
                                size = 50.dp,
                                isOnline = user.isOnline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name.ifBlank { user.username }, fontWeight = FontWeight.Bold)
                                Text(
                                    text = when {
                                        isTyping -> "Typing..."
                                        !existingChat?.lastMessage.isNullOrBlank() -> existingChat!!.lastMessage
                                        else -> "@${user.username} • Message bhejein"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isTyping) InstaBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (user.isOnline) {
                                Text("Online", fontSize = 11.sp, color = OnlineGreen, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    currentUserId: String,
    otherUser: UserProfile,
    chatThread: ChatThread?,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (type: String, text: String, mediaUrl: String) -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onMarkSeen: (List<ChatMessage>) -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }

    BackHandler {
        onTypingChanged(false)
        onBack()
    }

    LaunchedEffect(messages) {
        onMarkSeen(messages)
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = MediaUtils.uriToCompressedBase64(context, uri, maxDim = 420, quality = 60)
            if (encoded != null) {
                onSendMessage("photo", "Photo", encoded)
            }
        }
    }

    val isOtherTyping = chatThread?.typingUserIds?.contains(otherUser.userId) == true

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarView(
                        avatarData = otherUser.avatarData,
                        name = otherUser.name.ifBlank { otherUser.username },
                        size = 36.dp,
                        isOnline = otherUser.isOnline
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(otherUser.name.ifBlank { otherUser.username }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = when {
                                isOtherTyping -> "Typing..."
                                otherUser.isOnline -> "Online"
                                else -> "Offline"
                            },
                            fontSize = 12.sp,
                            color = if (isOtherTyping) InstaBlue else if (otherUser.isOnline) OnlineGreen else Color.Gray
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    onTypingChanged(false)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Peeche")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isMine) InstaBlue else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 260.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (msg.type == "photo" && msg.mediaUrl.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    FilteredMediaBox(
                                        mediaUrl = msg.mediaUrl,
                                        mediaType = "photo",
                                        filterName = "Normal",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = when (msg.type) {
                                    "voice" -> "🎤 Voice Message (0:08)"
                                    "video" -> "🎥 Video Clip: ${msg.text}"
                                    else -> msg.text
                                },
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (isMine) {
                                Row(
                                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (msg.isSeen) Icons.Default.DoneAll else Icons.Default.Check,
                                        contentDescription = if (msg.isSeen) "Seen" else "Sent",
                                        tint = if (msg.isSeen) Color(0xFFA7F3D0) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Photo Bhejein", tint = InstaMagenta)
            }
            IconButton(
                onClick = {
                    onSendMessage("video", "Short Video Clip", "")
                }
            ) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Bhejein", tint = InstaBlue)
            }
            IconButton(
                onClick = {
                    if (!isRecordingVoice) {
                        isRecordingVoice = true
                    } else {
                        isRecordingVoice = false
                        onSendMessage("voice", "Voice Message", "")
                    }
                }
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice Message",
                    tint = if (isRecordingVoice) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    onTypingChanged(it.isNotBlank())
                },
                placeholder = { Text(if (isRecordingVoice) "Recording... Mic dubakar bhejein" else "Message likhein...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendMessage("text", textInput.trim(), "")
                        textInput = ""
                        onTypingChanged(false)
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = InstaBlue)
            }
        }
    }
}
