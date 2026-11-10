package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FollowRecord
import com.example.data.model.PostComment
import com.example.data.model.PostItem
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.data.repository.MeraInstaRepository
import com.example.data.repository.toHindiErrorMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MeraInstaViewModel(
    private val repository: MeraInstaRepository,
    val currentUserId: String
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    private val _activeCommentPostId = MutableStateFlow<String?>(null)

    val currentUserProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { e -> handleStreamError("profile", e); emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsers()
        .catch { e -> handleStreamError("users", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followingRecords: StateFlow<List<FollowRecord>> = repository.observeFollowing(currentUserId)
        .catch { e -> handleStreamError("following", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followerRecords: StateFlow<List<FollowRecord>> = repository.observeFollowers(currentUserId)
        .catch { e -> handleStreamError("followers", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followingIds: StateFlow<Set<String>> = followingRecords
        .map { list -> list.map { it.followingId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val allPosts: StateFlow<List<PostItem>> = repository.observePosts()
        .catch { e -> handleStreamError("posts", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val feedPosts: StateFlow<List<PostItem>> = combine(
        allPosts,
        followingIds,
        currentUserProfile
    ) { posts, following, me ->
        val blocked = me?.blockedUserIds?.toSet() ?: emptySet()
        val visible = posts.filter { it.authorId !in blocked && it.mediaType != "reel" }
        val followedPosts = visible.filter { it.authorId == currentUserId || it.authorId in following }
        if (followedPosts.isNotEmpty()) followedPosts else visible
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reels: StateFlow<List<PostItem>> = combine(allPosts, currentUserProfile) { posts, me ->
        val blocked = me?.blockedUserIds?.toSet() ?: emptySet()
        posts.filter { it.mediaType == "reel" && it.authorId !in blocked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stories: StateFlow<List<StoryItem>> = repository.observeStories()
        .catch { e -> handleStreamError("stories", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications(currentUserId)
        .catch { e -> handleStreamError("notifications", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val chats: StateFlow<List<ChatThread>> = repository.observeChats(currentUserId)
        .catch { e -> handleStreamError("chats", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedPostIds: StateFlow<Set<String>> = repository.observeSavedPostIds(currentUserId)
        .catch { e -> handleStreamError("saved", e); emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(emptyList())
            else repository.observeMessages(chatId).catch { e -> handleStreamError("messages", e); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeComments: StateFlow<List<PostComment>> = _activeCommentPostId
        .flatMapLatest { postId ->
            if (postId == null) flowOf(emptyList())
            else repository.observeComments(postId).catch { e -> handleStreamError("comments", e); emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            repository.setOnlineStatus(true)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(500)
            _isRefreshing.value = false
            _statusMessage.value = "Feed refresh ho gaya!"
        }
    }

    fun saveProfile(name: String, username: String, bio: String, avatarData: String, onDone: () -> Unit = {}) {
        if (name.isBlank() || username.isBlank()) {
            _statusMessage.value = "Naam aur Username likhna zaroori hai."
            return
        }
        viewModelScope.launch {
            repository.createOrUpdateProfile(name, username, bio, avatarData)
                .onSuccess {
                    _statusMessage.value = "Profile save ho gaya!"
                    onDone()
                }
                .onFailure { _statusMessage.value = toHindiErrorMessage(it) }
        }
    }

    fun toggleFollow(targetUser: UserProfile) {
        val me = currentUserProfile.value ?: return
        val isFollowing = targetUser.userId in followingIds.value
        viewModelScope.launch {
            val result = if (isFollowing) {
                repository.unfollowUser(targetUser.userId)
            } else {
                repository.followUser(me, targetUser)
            }
            result.onSuccess {
                _statusMessage.value = if (isFollowing) {
                    "${targetUser.username} ko Unfollow kiya"
                } else {
                    "${targetUser.username} ko Follow kiya!"
                }
            }.onFailure {
                _statusMessage.value = toHindiErrorMessage(it)
            }
        }
    }

    fun removeFollower(followerUserId: String) {
        viewModelScope.launch {
            repository.removeFollower(followerUserId)
                .onSuccess { _statusMessage.value = "Follower hata diya gaya." }
                .onFailure { _statusMessage.value = toHindiErrorMessage(it) }
        }
    }

    fun toggleBlockUser(targetUserId: String) {
        val blocked = currentUserProfile.value?.blockedUserIds ?: emptyList()
        val shouldBlock = targetUserId !in blocked
        viewModelScope.launch {
            repository.toggleBlockUser(targetUserId, shouldBlock)
                .onSuccess {
                    _statusMessage.value = if (shouldBlock) "User ko Block kar diya." else "User ko Unblock kar diya."
                }
                .onFailure { _statusMessage.value = toHindiErrorMessage(it) }
        }
    }

    fun createPost(
        mediaType: String,
        mediaUrl: String,
        filterName: String,
        caption: String,
        location: String,
        taggedUsers: List<String>,
        musicName: String = "",
        durationSec: Long = 0L,
        onSuccess: () -> Unit
    ) {
        val me = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.createPost(
                currentUser = me,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                filterName = filterName,
                caption = caption,
                location = location,
                taggedUsers = taggedUsers,
                musicName = musicName,
                durationSec = durationSec
            ).onSuccess {
                _statusMessage.value = if (mediaType == "reel") "Reel upload ho gayi!" else "Post upload ho gaya!"
                onSuccess()
            }.onFailure {
                _statusMessage.value = toHindiErrorMessage(it)
            }
        }
    }

    fun createStory(mediaUrl: String, caption: String, filterName: String, onSuccess: () -> Unit) {
        val me = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.createStory(me, mediaUrl, caption, filterName)
                .onSuccess {
                    _statusMessage.value = "24 ghante ki Story lag gayi!"
                    onSuccess()
                }
                .onFailure {
                    _statusMessage.value = toHindiErrorMessage(it)
                }
        }
    }

    fun checkPostLiked(postId: String) {
        viewModelScope.launch {
            val liked = repository.isPostLikedByUser(postId, currentUserId)
            if (liked) {
                _likedPostIds.value = _likedPostIds.value + postId
            }
        }
    }

    fun toggleLike(post: PostItem, forceLike: Boolean = false) {
        val me = currentUserProfile.value ?: return
        val currentlyLiked = post.id in _likedPostIds.value
        if (forceLike && currentlyLiked) return
        val nextLiked = !currentlyLiked
        _likedPostIds.value = if (nextLiked) _likedPostIds.value + post.id else _likedPostIds.value - post.id
        viewModelScope.launch {
            repository.toggleLikePost(post, me, currentlyLiked)
                .onFailure {
                    _likedPostIds.value = if (currentlyLiked) _likedPostIds.value + post.id else _likedPostIds.value - post.id
                    _statusMessage.value = toHindiErrorMessage(it)
                }
        }
    }

    fun toggleSave(postId: String) {
        val currentlySaved = postId in savedPostIds.value
        viewModelScope.launch {
            repository.toggleSavePost(postId, currentlySaved)
                .onSuccess {
                    _statusMessage.value = if (!currentlySaved) "Post Save ho gaya!" else "Saved se hata diya."
                }
                .onFailure { _statusMessage.value = toHindiErrorMessage(it) }
        }
    }

    fun openComments(postId: String?) {
        _activeCommentPostId.value = postId
    }

    fun addComment(post: PostItem, text: String) {
        val me = currentUserProfile.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(post, me, text)
                .onFailure { _statusMessage.value = toHindiErrorMessage(it) }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun openChatWithUser(otherUserId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            repository.getOrCreateChat(otherUserId)
                .onSuccess { chatId ->
                    _activeChatId.value = chatId
                    onChatReady(chatId)
                }
                .onFailure { _statusMessage.value = toHindiErrorMessage(it) }
        }
    }

    fun closeChat() {
        _activeChatId.value = null
    }

    fun sendMessage(chatId: String, participantIds: List<String>, otherUserId: String, type: String, text: String, mediaUrl: String = "") {
        val me = currentUserProfile.value ?: return
        viewModelScope.launch {
            repository.sendMessage(chatId, participantIds, me, otherUserId, type, text, mediaUrl)
                .onFailure { _statusMessage.value = toHindiErrorMessage(it) }
        }
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            repository.setTyping(chatId, isTyping)
        }
    }

    fun markMessagesSeen(chatId: String, messages: List<ChatMessage>) {
        viewModelScope.launch {
            repository.markMessagesSeen(chatId, messages)
        }
    }

    fun setOfflineOnSignOut() {
        viewModelScope.launch {
            repository.setOnlineStatus(false)
        }
    }

    private fun handleStreamError(tag: String, error: Throwable) {
        val fe = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fe == null && error is CancellationException) throw error
        Log.w("MeraInstaVM", "Stream error ($tag)", fe ?: error)
    }
}
