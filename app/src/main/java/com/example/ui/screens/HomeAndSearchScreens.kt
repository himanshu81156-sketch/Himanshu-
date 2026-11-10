package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PostItem
import com.example.data.model.StoryItem
import com.example.data.model.UserProfile
import com.example.ui.components.AvatarView
import com.example.ui.components.FilteredMediaBox
import com.example.ui.components.PostCard
import com.example.ui.theme.InstaBlue
import com.example.ui.theme.InstaMagenta
import com.example.ui.theme.OnlineGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    currentUser: UserProfile?,
    stories: List<StoryItem>,
    posts: List<PostItem>,
    followingIds: Set<String>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    unreadNotificationCount: Int,
    isDarkMode: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenChats: () -> Unit,
    onAddStoryClick: () -> Unit,
    onStoryClick: (StoryItem) -> Unit,
    onUserClick: (String) -> Unit,
    onFollowAuthor: (String) -> Unit,
    onLikePost: (PostItem, Boolean) -> Unit,
    onCommentPost: (PostItem) -> Unit,
    onSavePost: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mera Insta",
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (currentUser?.isOnline == true) OnlineGreen else Color.Gray)
                    )
                }
            },
            actions = {
                IconButton(onClick = onToggleDarkMode, modifier = Modifier.testTag("theme_toggle_btn")) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme Badlein"
                    )
                }
                IconButton(onClick = onOpenNotifications, modifier = Modifier.testTag("notifications_btn")) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge { Text(unreadNotificationCount.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                }
                IconButton(onClick = onOpenChats, modifier = Modifier.testTag("chat_btn")) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Messages / Chat")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Stories Row
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onAddStoryClick() }
                                    .testTag("my_story_add")
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    AvatarView(
                                        avatarData = currentUser?.avatarData ?: "",
                                        name = currentUser?.username ?: "Me",
                                        size = 62.dp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(InstaBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Story Lagayein",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Meri Story",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        items(stories, key = { it.id }) { story ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(70.dp)
                                    .clickable { onStoryClick(story) }
                            ) {
                                AvatarView(
                                    avatarData = story.authorAvatar,
                                    name = story.authorUsername,
                                    size = 62.dp,
                                    hasStoryRing = true
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = story.authorUsername,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }

                if (posts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Abhi koi Post nahi hai!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Neeche + Post button dabakar apna pehla Photo ya Reel upload karein, ya Search me jaakar Dosto ko Follow karein.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            isLiked = post.id in likedPostIds,
                            isSaved = post.id in savedPostIds,
                            isFollowingAuthor = post.authorId in followingIds,
                            isOwnPost = post.authorId == currentUser?.userId,
                            onAuthorClick = { onUserClick(post.authorId) },
                            onFollowClick = { onFollowAuthor(post.authorId) },
                            onDoubleTapLike = { onLikePost(post, true) },
                            onLikeClick = { onLikePost(post, false) },
                            onCommentClick = { onCommentPost(post) },
                            onSaveClick = { onSavePost(post.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    currentUserId: String,
    allUsers: List<UserProfile>,
    allPosts: List<PostItem>,
    followingIds: Set<String>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    onFollowToggle: (UserProfile) -> Unit,
    onUserClick: (String) -> Unit,
    onLikePost: (PostItem, Boolean) -> Unit,
    onCommentPost: (PostItem) -> Unit,
    onSavePost: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedPost by remember { mutableStateOf<PostItem?>(null) }

    val matchingUsers = remember(query, allUsers) {
        val q = query.trim().lowercase()
        allUsers.filter { it.userId != currentUserId }.filter {
            q.isEmpty() || it.name.lowercase().contains(q) || it.username.lowercase().contains(q)
        }
    }

    val matchingPosts = remember(query, allPosts) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allPosts
        else allPosts.filter {
            it.caption.lowercase().contains(q) ||
                it.location.lowercase().contains(q) ||
                it.authorUsername.lowercase().contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Dost ka naam ya post khojein...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("search_input")
        )

        if (matchingUsers.isNotEmpty()) {
            Text(
                text = if (query.isBlank()) "Dosto ko Follow karein" else "Users",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matchingUsers, key = { it.userId }) { user ->
                    val isFollowing = user.userId in followingIds
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onUserClick(user.userId) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AvatarView(
                                avatarData = user.avatarData,
                                name = user.name.ifBlank { user.username },
                                size = 54.dp,
                                isOnline = user.isOnline
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = user.username,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${user.followersCount} Followers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isFollowing) {
                                OutlinedButton(
                                    onClick = { onFollowToggle(user) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Following", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onFollowToggle(user) },
                                    colors = ButtonDefaults.buttonColors(containerColor = InstaBlue),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Follow", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Text(
            text = "Explore Photos & Reels",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(matchingPosts, key = { it.id }) { post ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { selectedPost = post }
                ) {
                    FilteredMediaBox(
                        mediaUrl = post.mediaUrl,
                        mediaType = post.mediaType,
                        filterName = post.filterName,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (post.mediaType == "reel" || post.mediaType == "video") {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (selectedPost != null) {
        val post = selectedPost!!
        Dialog(onDismissRequest = { selectedPost = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    PostCard(
                        post = post,
                        isLiked = post.id in likedPostIds,
                        isSaved = post.id in savedPostIds,
                        isFollowingAuthor = post.authorId in followingIds,
                        isOwnPost = post.authorId == currentUserId,
                        onAuthorClick = {
                            selectedPost = null
                            onUserClick(post.authorId)
                        },
                        onFollowClick = {
                            allUsers.find { it.userId == post.authorId }?.let(onFollowToggle)
                        },
                        onDoubleTapLike = { onLikePost(post, true) },
                        onLikeClick = { onLikePost(post, false) },
                        onCommentClick = {
                            selectedPost = null
                            onCommentPost(post)
                        },
                        onSaveClick = { onSavePost(post.id) }
                    )
                }
            }
        }
    }
}
