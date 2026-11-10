package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.FollowRecord
import com.example.data.model.PostComment
import com.example.data.model.PostItem
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class MeraInstaRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Aap login nahi hain. Kripya pehle Google se login karein.")
    }

    // --- USER PROFILES ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.GET, path, auth)
                    throw e
                }
        )
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap -> snap.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val docRef = db.collection("users").document(userId)
        return try {
            docRef.get().await().toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            throw e
        }
    }

    suspend fun createOrUpdateProfile(
        name: String,
        username: String,
        bio: String,
        avatarData: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val payload = mapOf(
                "userId" to uid,
                "name" to name.trim().take(60),
                "username" to username.trim().lowercase().replace(" ", "_").take(30),
                "bio" to bio.trim().take(200),
                "avatarData" to avatarData,
                "isOnline" to true,
                "followersCount" to 0L,
                "followingCount" to 0L,
                "postsCount" to 0L,
                "blockedUserIds" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } else {
            val updates = mapOf(
                "name" to name.trim().take(60),
                "username" to username.trim().lowercase().replace(" ", "_").take(30),
                "bio" to bio.trim().take(200),
                "avatarData" to avatarData,
                "isOnline" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
        }
        Unit
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}", auth)
        }
    }

    suspend fun setOnlineStatus(isOnline: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "isOnline" to isOnline,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
        }
    }

    suspend fun toggleBlockUser(targetUserId: String, shouldBlock: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val update = if (shouldBlock) {
            mapOf(
                "blockedUserIds" to FieldValue.arrayUnion(targetUserId),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "blockedUserIds" to FieldValue.arrayRemove(targetUserId),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        docRef.update(update).await()
        Unit
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}", auth)
        }
    }

    // --- FOLLOW SYSTEM ---
    fun observeFollowing(userId: String): Flow<List<FollowRecord>> = flow {
        val path = "follows"
        emitAll(
            db.collection("follows")
                .whereEqualTo("followerId", userId)
                .snapshots()
                .map { it.toObjects(FollowRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    fun observeFollowers(userId: String): Flow<List<FollowRecord>> = flow {
        val path = "follows"
        emitAll(
            db.collection("follows")
                .whereEqualTo("followingId", userId)
                .snapshots()
                .map { it.toObjects(FollowRecord::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun followUser(currentUser: UserProfile, targetUser: UserProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(uid != targetUser.userId) { "Khud ko follow nahi kar sakte" }
        val followId = "${uid}_${targetUser.userId}"
        val followRef = db.collection("follows").document(followId)
        val existing = followRef.get().await()
        if (existing.exists()) return@runCatching

        val batch = db.batch()
        batch.set(
            followRef,
            mapOf(
                "followerId" to uid,
                "followingId" to targetUser.userId,
                "followerUsername" to currentUser.username,
                "followingUsername" to targetUser.username,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "followingCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("users").document(targetUser.userId),
            mapOf(
                "followersCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val notifRef = db.collection("notifications").document()
        batch.set(
            notifRef,
            mapOf(
                "recipientId" to targetUser.userId,
                "actorId" to uid,
                "actorUsername" to currentUser.username,
                "actorAvatar" to currentUser.avatarData,
                "type" to "follow",
                "message" to "${currentUser.name.ifBlank { currentUser.username }} ne tumhe Follow kiya",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.WRITE, "follows", auth)
        }
    }

    suspend fun unfollowUser(targetUserId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val followId = "${uid}_${targetUserId}"
        val followRef = db.collection("follows").document(followId)
        val existing = followRef.get().await()
        if (!existing.exists()) return@runCatching

        val batch = db.batch()
        batch.delete(followRef)
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "followingCount" to FieldValue.increment(-1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("users").document(targetUserId),
            mapOf(
                "followersCount" to FieldValue.increment(-1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.DELETE, "follows/${uidOrBlank()}_$targetUserId", auth)
        }
    }

    suspend fun removeFollower(followerUserId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val followId = "${followerUserId}_${uid}"
        val followRef = db.collection("follows").document(followId)
        if (!followRef.get().await().exists()) return@runCatching
        followRef.delete().await()
        db.collection("users").document(uid).update(
            mapOf(
                "followersCount" to FieldValue.increment(-1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.DELETE, "follows", auth)
        }
    }

    // --- POSTS & REELS ---
    fun observePosts(): Flow<List<PostItem>> = flow {
        val path = "posts"
        emitAll(
            db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { it.toObjects(PostItem::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun createPost(
        currentUser: UserProfile,
        mediaType: String,
        mediaUrl: String,
        filterName: String,
        caption: String,
        location: String,
        taggedUsers: List<String>,
        musicName: String = "",
        durationSec: Long = 0L
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("posts").document()
        val payload = mapOf(
            "authorId" to uid,
            "authorUsername" to currentUser.username,
            "authorAvatar" to currentUser.avatarData,
            "mediaType" to mediaType,
            "mediaUrl" to mediaUrl,
            "filterName" to filterName,
            "caption" to caption.take(500),
            "location" to location.take(100),
            "taggedUsers" to taggedUsers.take(10),
            "musicName" to musicName.take(80),
            "durationSec" to durationSec,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val batch = db.batch()
        batch.set(docRef, payload)
        batch.update(
            db.collection("users").document(uid),
            mapOf(
                "postsCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        docRef.id
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.CREATE, "posts", auth)
        }
    }

    suspend fun toggleLikePost(post: PostItem, currentUser: UserProfile, currentlyLiked: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val likeRef = db.collection("posts").document(post.id).collection("likes").document(uid)
        val postRef = db.collection("posts").document(post.id)
        val batch = db.batch()
        if (currentlyLiked) {
            batch.delete(likeRef)
            batch.update(
                postRef,
                mapOf(
                    "likesCount" to FieldValue.increment(-1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                likeRef,
                mapOf(
                    "userId" to uid,
                    "username" to currentUser.username,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (post.authorId != uid) {
                val notifRef = db.collection("notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "recipientId" to post.authorId,
                        "actorId" to uid,
                        "actorUsername" to currentUser.username,
                        "actorAvatar" to currentUser.avatarData,
                        "type" to "like",
                        "postId" to post.id,
                        "message" to "${currentUser.name.ifBlank { currentUser.username }} ne Like kiya",
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}/likes", auth)
        }
    }

    suspend fun isPostLikedByUser(postId: String, userId: String): Boolean {
        return try {
            db.collection("posts").document(postId).collection("likes").document(userId).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { it.toObjects(PostComment::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun addComment(post: PostItem, currentUser: UserProfile, text: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val commentRef = db.collection("posts").document(post.id).collection("comments").document()
        val postRef = db.collection("posts").document(post.id)
        val batch = db.batch()
        batch.set(
            commentRef,
            mapOf(
                "authorId" to uid,
                "authorUsername" to currentUser.username,
                "authorAvatar" to currentUser.avatarData,
                "text" to text.trim().take(300),
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            postRef,
            mapOf(
                "commentsCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (post.authorId != uid) {
            val notifRef = db.collection("notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "recipientId" to post.authorId,
                    "actorId" to uid,
                    "actorUsername" to currentUser.username,
                    "actorAvatar" to currentUser.avatarData,
                    "type" to "comment",
                    "postId" to post.id,
                    "message" to "${currentUser.name.ifBlank { currentUser.username }} ne Comment kiya: ${text.take(40)}",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments", auth)
        }
    }

    fun observeSavedPostIds(userId: String): Flow<Set<String>> = flow {
        val path = "users/$userId/saved_posts"
        emitAll(
            db.collection("users").document(userId).collection("saved_posts")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun toggleSavePost(postId: String, currentlySaved: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        val saveRef = db.collection("users").document(uid).collection("saved_posts").document(postId)
        if (currentlySaved) {
            saveRef.delete().await()
        } else {
            saveRef.set(
                mapOf(
                    "postId" to postId,
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.WRITE, "users/${uidOrBlank()}/saved_posts/$postId", auth)
        }
    }

    // --- STORIES (24-HOUR) ---
    fun observeStories(): Flow<List<StoryItem>> = flow {
        val path = "stories"
        emitAll(
            db.collection("stories")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    val now = Date()
                    snap.toObjects(StoryItem::class.java, ServerTimestampBehavior.ESTIMATE)
                        .filter { story ->
                            val exp = story.expiresAt?.toDate()
                            exp == null || exp.after(now)
                        }
                }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun createStory(
        currentUser: UserProfile,
        mediaUrl: String,
        caption: String,
        filterName: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("stories").document()
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L))
        docRef.set(
            mapOf(
                "authorId" to uid,
                "authorUsername" to currentUser.username,
                "authorAvatar" to currentUser.avatarData,
                "mediaUrl" to mediaUrl,
                "caption" to caption.take(200),
                "filterName" to filterName,
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to expiresAt
            )
        ).await()
        docRef.id
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.CREATE, "stories", auth)
        }
    }

    // --- NOTIFICATIONS ---
    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun markNotificationRead(notificationId: String) {
        try {
            db.collection("notifications").document(notificationId).update("isRead", true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId", auth)
        }
    }

    // --- CHAT / DM ---
    fun observeChats(userId: String): Flow<List<ChatThread>> = flow {
        val path = "chats"
        emitAll(
            db.collection("chats")
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatThread::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun getOrCreateChat(otherUserId: String): Result<String> = runCatching {
        val uid = requireUserId()
        val participants = listOf(uid, otherUserId).sorted()
        val chatId = "${participants[0]}_${participants[1]}"
        val docRef = db.collection("chats").document(chatId)
        if (!docRef.get().await().exists()) {
            docRef.set(
                mapOf(
                    "participantIds" to participants,
                    "lastMessage" to "",
                    "lastSenderId" to "",
                    "typingUserIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        chatId
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.WRITE, "chats", auth)
        }
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = flow {
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { it.toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { e ->
                    val fe = unwrapFirestoreException(e)
                    if (fe == null && e is CancellationException) throw e
                    handleFirestoreError(fe ?: e, OperationType.LIST, path, auth)
                    throw e
                }
        )
    }

    suspend fun sendMessage(
        chatId: String,
        participantIds: List<String>,
        currentUser: UserProfile,
        otherUserId: String,
        type: String,
        text: String,
        mediaUrl: String = ""
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val msgRef = db.collection("chats").document(chatId).collection("messages").document()
        val chatRef = db.collection("chats").document(chatId)
        val preview = when (type) {
            "photo" -> "📷 Photo bheja"
            "video" -> "🎥 Video bheja"
            "voice" -> "🎤 Voice message bheja"
            else -> text.take(100)
        }
        val batch = db.batch()
        batch.set(
            msgRef,
            mapOf(
                "senderId" to uid,
                "participantIds" to participantIds,
                "type" to type,
                "text" to text.take(1000),
                "mediaUrl" to mediaUrl,
                "isSeen" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            chatRef,
            mapOf(
                "lastMessage" to preview,
                "lastSenderId" to uid,
                "typingUserIds" to FieldValue.arrayRemove(uid),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val notifRef = db.collection("notifications").document()
        batch.set(
            notifRef,
            mapOf(
                "recipientId" to otherUserId,
                "actorId" to uid,
                "actorUsername" to currentUser.username,
                "actorAvatar" to currentUser.avatarData,
                "type" to "message",
                "message" to "Naya Message aaya: ${currentUser.name.ifBlank { currentUser.username }} - $preview",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Unit
    }.onFailure { e ->
        if (e !is CancellationException) {
            handleFirestoreError(e, OperationType.CREATE, "chats/$chatId/messages", auth)
        }
    }

    suspend fun setTyping(chatId: String, isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val chatRef = db.collection("chats").document(chatId)
        try {
            val update = if (isTyping) {
                mapOf(
                    "typingUserIds" to FieldValue.arrayUnion(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "typingUserIds" to FieldValue.arrayRemove(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            chatRef.update(update).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun markMessagesSeen(chatId: String, messages: List<ChatMessage>) {
        val uid = auth.currentUser?.uid ?: return
        val unseenFromOther = messages.filter { it.senderId != uid && !it.isSeen && it.id.isNotBlank() }
        if (unseenFromOther.isEmpty()) return
        try {
            val batch = db.batch()
            unseenFromOther.forEach { msg ->
                batch.update(
                    db.collection("chats").document(chatId).collection("messages").document(msg.id),
                    "isSeen",
                    true
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun uidOrBlank(): String = auth.currentUser?.uid ?: ""

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }
}
