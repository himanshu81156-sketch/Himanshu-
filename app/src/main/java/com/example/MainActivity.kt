package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.PostItem
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.data.prefs.ThemePreferences
import com.example.data.repository.MeraInstaRepository
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.StoryViewerOverlay
import com.example.ui.screens.AuthWelcomeScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PermissionWizardDialog
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MeraInstaTheme
import com.example.ui.viewmodel.MeraInstaViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { ThemePreferences(context) }
            val isDarkMode by prefs.isDarkMode.collectAsStateWithLifecycle(initialValue = false)
            val isPermissionsDone by prefs.isPermissionsWizardDone.collectAsStateWithLifecycle(initialValue = false)
            val scope = rememberCoroutineScope()

            MeraInstaTheme(darkTheme = isDarkMode) {
                MeraInstaRoot(
                    isDarkMode = isDarkMode,
                    isPermissionsDone = isPermissionsDone,
                    onToggleDarkMode = { enabled ->
                        scope.launch { prefs.setDarkMode(enabled) }
                    },
                    onPermissionsDone = {
                        scope.launch { prefs.setPermissionsWizardDone(true) }
                    }
                )
            }
        }
    }
}

@Composable
fun MeraInstaRoot(
    isDarkMode: Boolean,
    isPermissionsDone: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onPermissionsDone: () -> Unit
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (currentUser == null) {
        AuthWelcomeScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            val vm: MeraInstaViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val dbId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(dbId)
                        MeraInstaViewModel(MeraInstaRepository(db, Firebase.auth), uid)
                    }
                }
            )
            MeraInstaAppContent(
                viewModel = vm,
                isDarkMode = isDarkMode,
                isPermissionsDone = isPermissionsDone,
                onToggleDarkMode = onToggleDarkMode,
                onPermissionsDone = onPermissionsDone
            )
        }
    }
}

@Composable
fun MeraInstaAppContent(
    viewModel: MeraInstaViewModel,
    isDarkMode: Boolean,
    isPermissionsDone: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onPermissionsDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val feedPosts by viewModel.feedPosts.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val reels by viewModel.reels.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val followerRecords by viewModel.followerRecords.collectAsStateWithLifecycle()
    val followingRecords by viewModel.followingRecords.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val savedPostIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val chats by viewModel.chats.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val activeComments by viewModel.activeComments.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var createMode by remember { mutableStateOf("photo") }
    var isEditingProfile by remember { mutableStateOf(false) }
    var viewingUserId by remember { mutableStateOf<String?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    var showChatList by remember { mutableStateOf(false) }
    var activeChatUser by remember { mutableStateOf<UserProfile?>(null) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var activeCommentPost by remember { mutableStateOf<PostItem?>(null) }
    var activeStory by remember { mutableStateOf<StoryItem?>(null) }
    var showPermissionWizardManual by remember { mutableStateOf(false) }
    var profileLoadedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(allUsers, currentUserProfile) {
        if (currentUserProfile != null || allUsers.isNotEmpty()) {
            profileLoadedOnce = true
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val me = currentUserProfile
    val firebaseUser = Firebase.auth.currentUser

    if (!profileLoadedOnce && me == null) {
        // Give Firestore a brief moment to emit cached/server profile
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(900)
            profileLoadedOnce = true
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (me == null || isEditingProfile) {
        ProfileSetupScreen(
            initialName = me?.name ?: firebaseUser?.displayName ?: "",
            initialUsername = me?.username ?: (firebaseUser?.email?.substringBefore("@") ?: "user"),
            initialBio = me?.bio ?: "",
            initialAvatar = me?.avatarData ?: "",
            isEditing = isEditingProfile,
            onSave = { name, username, bio, avatar ->
                viewModel.saveProfile(name, username, bio, avatar) {
                    isEditingProfile = false
                }
            }
        )
        return
    }

    if (!isPermissionsDone || showPermissionWizardManual) {
        PermissionWizardDialog(
            onFinished = {
                showPermissionWizardManual = false
                onPermissionsDone()
            }
        )
    }

    if (activeStory != null) {
        StoryViewerOverlay(
            story = activeStory!!,
            onClose = { activeStory = null }
        )
        return
    }

    if (activeChatId != null && activeChatUser != null) {
        val thread = chats.find { it.id == activeChatId }
        ChatRoomScreen(
            chatId = activeChatId!!,
            currentUserId = viewModel.currentUserId,
            otherUser = activeChatUser!!,
            chatThread = thread,
            messages = activeMessages,
            onBack = {
                activeChatId = null
                activeChatUser = null
                viewModel.closeChat()
            },
            onSendMessage = { type, text, media ->
                val participants = listOf(viewModel.currentUserId, activeChatUser!!.userId).sorted()
                viewModel.sendMessage(activeChatId!!, participants, activeChatUser!!.userId, type, text, media)
            },
            onTypingChanged = { typing -> viewModel.setTyping(activeChatId!!, typing) },
            onMarkSeen = { msgs -> viewModel.markMessagesSeen(activeChatId!!, msgs) }
        )
        return
    }

    if (showChatList) {
        ChatListScreen(
            currentUserId = viewModel.currentUserId,
            chats = chats,
            allUsers = allUsers,
            onBack = { showChatList = false },
            onOpenChat = { targetUser ->
                viewModel.openChatWithUser(targetUser.userId) { chatId ->
                    activeChatUser = targetUser
                    activeChatId = chatId
                }
            }
        )
        return
    }

    if (showNotifications) {
        NotificationsScreen(
            notifications = notifications,
            onBack = { showNotifications = false },
            onNotificationClick = { notif ->
                viewModel.markNotificationRead(notif.id)
                if (notif.type == "follow") {
                    showNotifications = false
                    viewingUserId = notif.actorId
                }
            }
        )
        return
    }

    if (viewingUserId != null && viewingUserId != viewModel.currentUserId) {
        val target = allUsers.find { it.userId == viewingUserId }
        if (target != null) {
            ProfileScreen(
                profileUser = target,
                isOwnProfile = false,
                isFollowing = target.userId in followingIds,
                isBlocked = target.userId in me.blockedUserIds,
                userPosts = allPosts.filter { it.authorId == target.userId },
                savedPosts = emptyList(),
                allUsers = allUsers,
                followingIds = followingIds,
                followersList = emptyList(),
                followingList = emptyList(),
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onBackClick = { viewingUserId = null },
                onEditProfileClick = {},
                onFollowToggle = { viewModel.toggleFollow(target) },
                onMessageClick = {
                    viewModel.openChatWithUser(target.userId) { chatId ->
                        activeChatUser = target
                        activeChatId = chatId
                    }
                },
                onBlockToggle = { viewModel.toggleBlockUser(target.userId) },
                onFollowOtherUser = { viewModel.toggleFollow(it) },
                onRemoveFollower = {},
                onOpenPermissionsWizard = {},
                onLogoutClick = {}
            )
            return
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                val tabs = listOf(
                    Triple(0, "Home", Icons.Filled.Home to Icons.Outlined.Home),
                    Triple(1, "Search", Icons.Filled.Search to Icons.Outlined.Search),
                    Triple(2, "+ Post", Icons.Filled.AddBox to Icons.Outlined.AddBox),
                    Triple(3, "Reels", Icons.Filled.Movie to Icons.Outlined.Movie),
                    Triple(4, "Profile", Icons.Filled.Person to Icons.Outlined.Person)
                )
                tabs.forEach { (idx, label, icons) ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = {
                            if (idx == 2) createMode = "photo"
                            selectedTab = idx
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == idx) icons.first else icons.second,
                                contentDescription = label
                            )
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("tab_$idx")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> HomeFeedScreen(
                    currentUser = me,
                    stories = stories,
                    posts = feedPosts,
                    followingIds = followingIds,
                    likedPostIds = likedPostIds,
                    savedPostIds = savedPostIds,
                    unreadNotificationCount = notifications.count { !it.isRead },
                    isDarkMode = isDarkMode,
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshFeed() },
                    onToggleDarkMode = { onToggleDarkMode(!isDarkMode) },
                    onOpenNotifications = { showNotifications = true },
                    onOpenChats = { showChatList = true },
                    onAddStoryClick = {
                        createMode = "story"
                        selectedTab = 2
                    },
                    onStoryClick = { activeStory = it },
                    onUserClick = { uid ->
                        if (uid == viewModel.currentUserId) selectedTab = 4 else viewingUserId = uid
                    },
                    onFollowAuthor = { authorId ->
                        allUsers.find { it.userId == authorId }?.let { viewModel.toggleFollow(it) }
                    },
                    onLikePost = { post, forceLike -> viewModel.toggleLike(post, forceLike) },
                    onCommentPost = { post ->
                        activeCommentPost = post
                        viewModel.openComments(post.id)
                    },
                    onSavePost = { postId -> viewModel.toggleSave(postId) }
                )

                1 -> SearchScreen(
                    currentUserId = viewModel.currentUserId,
                    allUsers = allUsers,
                    allPosts = allPosts,
                    followingIds = followingIds,
                    likedPostIds = likedPostIds,
                    savedPostIds = savedPostIds,
                    onFollowToggle = { viewModel.toggleFollow(it) },
                    onUserClick = { uid ->
                        if (uid == viewModel.currentUserId) selectedTab = 4 else viewingUserId = uid
                    },
                    onLikePost = { post, forceLike -> viewModel.toggleLike(post, forceLike) },
                    onCommentPost = { post ->
                        activeCommentPost = post
                        viewModel.openComments(post.id)
                    },
                    onSavePost = { postId -> viewModel.toggleSave(postId) }
                )

                2 -> CreatePostScreen(
                    allUsers = allUsers.filter { it.userId != viewModel.currentUserId },
                    initialMode = createMode,
                    onCreatePost = { mediaType, mediaUrl, filter, caption, location, tags, music, dur ->
                        viewModel.createPost(mediaType, mediaUrl, filter, caption, location, tags, music, dur) {
                            selectedTab = if (mediaType == "reel") 3 else 0
                        }
                    },
                    onCreateStory = { mediaUrl, caption, filter ->
                        viewModel.createStory(mediaUrl, caption, filter) {
                            selectedTab = 0
                        }
                    }
                )

                3 -> ReelsScreen(
                    currentUserId = viewModel.currentUserId,
                    reels = reels,
                    followingIds = followingIds,
                    likedPostIds = likedPostIds,
                    savedPostIds = savedPostIds,
                    onCreateReelClick = {
                        createMode = "reel"
                        selectedTab = 2
                    },
                    onAuthorClick = { uid ->
                        if (uid == viewModel.currentUserId) selectedTab = 4 else viewingUserId = uid
                    },
                    onFollowAuthor = { authorId ->
                        allUsers.find { it.userId == authorId }?.let { viewModel.toggleFollow(it) }
                    },
                    onLikeReel = { reel -> viewModel.toggleLike(reel) },
                    onCommentReel = { reel ->
                        activeCommentPost = reel
                        viewModel.openComments(reel.id)
                    },
                    onSaveReel = { postId -> viewModel.toggleSave(postId) }
                )

                4 -> ProfileScreen(
                    profileUser = me,
                    isOwnProfile = true,
                    isFollowing = false,
                    isBlocked = false,
                    userPosts = allPosts.filter { it.authorId == me.userId },
                    savedPosts = allPosts.filter { it.id in savedPostIds },
                    allUsers = allUsers,
                    followingIds = followingIds,
                    followersList = followerRecords,
                    followingList = followingRecords,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    onEditProfileClick = { isEditingProfile = true },
                    onFollowToggle = {},
                    onMessageClick = {},
                    onBlockToggle = {},
                    onFollowOtherUser = { viewModel.toggleFollow(it) },
                    onRemoveFollower = { viewModel.removeFollower(it) },
                    onOpenPermissionsWizard = { showPermissionWizardManual = true },
                    onLogoutClick = {
                        viewModel.setOfflineOnSignOut()
                        GoogleAuthHelper.signOut(credentialManager, scope)
                    }
                )
            }
        }
    }

    if (activeCommentPost != null) {
        CommentsBottomSheet(
            comments = activeComments,
            onDismiss = {
                activeCommentPost = null
                viewModel.openComments(null)
            },
            onSendComment = { text ->
                viewModel.addComment(activeCommentPost!!, text)
            }
        )
    }
}
