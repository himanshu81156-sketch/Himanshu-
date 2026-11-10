package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.ui.components.AvatarView
import com.example.ui.components.FilteredMediaBox
import com.example.ui.components.sharePost
import com.example.ui.theme.InstaBlue
import com.example.ui.theme.InstaMagenta
import com.example.ui.util.MediaUtils

@Composable
fun CreatePostScreen(
    allUsers: List<UserProfile>,
    initialMode: String = "photo", // "photo", "story", "reel"
    onCreatePost: (
        mediaType: String,
        mediaUrl: String,
        filterName: String,
        caption: String,
        location: String,
        taggedUsers: List<String>,
        musicName: String,
        durationSec: Long
    ) -> Unit,
    onCreateStory: (mediaUrl: String, caption: String, filterName: String) -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(initialMode) }
    var mediaUrl by remember { mutableStateOf("studio://Mera Insta Moment") }
    var selectedFilter by remember { mutableStateOf("Normal") }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var taggedText by remember { mutableStateOf("") }
    var reelDuration by remember { mutableLongStateOf(15L) }
    var musicName by remember { mutableStateOf("Bollywood Lo-Fi Beat") }
    var overlayText by remember { mutableStateOf("") }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = MediaUtils.uriToCompressedBase64(context, uri, maxDim = 540, quality = 65)
            if (encoded != null) mediaUrl = encoded
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            mediaUrl = MediaUtils.bitmapToBase64(bitmap, maxDim = 540, quality = 65)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Naya Post / Reel / Story Banayein",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Mode Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("photo" to "Post", "reel" to "Reel (9:16)", "story" to "24h Story").forEach { (key, label) ->
                FilterChip(
                    selected = mode == key,
                    onClick = { mode = key },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Preview Box
        val previewAspect = if (mode == "reel" || mode == "story") 9f / 16f else 1f
        Box(
            modifier = Modifier
                .fillMaxWidth(if (mode == "reel" || mode == "story") 0.65f else 1f)
                .aspectRatio(previewAspect)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(16.dp))
        ) {
            FilteredMediaBox(
                mediaUrl = mediaUrl,
                mediaType = mode,
                filterName = selectedFilter,
                overlayText = overlayText,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Camera & Gallery buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    try {
                        cameraLauncher.launch(null)
                    } catch (_: Exception) {
                    }
                },
                modifier = Modifier.weight(1f).testTag("open_camera_btn")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Camera")
            }
            OutlinedButton(
                onClick = {
                    galleryPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier.weight(1f).testTag("open_gallery_btn")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gallery")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("Filter Chunein", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(MediaUtils.FILTERS) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        if (mode == "reel") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Reel Time (Seconds)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15L, 30L, 60L).forEach { sec ->
                    FilterChip(
                        selected = reelDuration == sec,
                        onClick = { reelDuration = sec },
                        label = { Text("${sec} sec") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = musicName,
                onValueChange = { musicName = it },
                label = { Text("Music Name Add Karein") },
                leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = overlayText,
                onValueChange = { overlayText = it },
                label = { Text("Video ke upar Text likhein") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Caption likhein...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("post_caption_input")
        )

        if (mode != "story") {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location add karein (jaise Mumbai, Delhi)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("post_location_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = taggedText,
                onValueChange = { taggedText = it },
                label = { Text("Tag People (comma se usernames likhein)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (allUsers.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(allUsers.take(8)) { user ->
                        FilterChip(
                            selected = taggedText.contains(user.username),
                            onClick = {
                                if (!taggedText.contains(user.username)) {
                                    taggedText = if (taggedText.isBlank()) user.username else "$taggedText, ${user.username}"
                                }
                            },
                            label = { Text("@${user.username}") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = {
                if (mode == "story") {
                    onCreateStory(mediaUrl, caption, selectedFilter)
                } else {
                    val tags = taggedText.split(",").map { it.trim().removePrefix("@") }.filter { it.isNotBlank() }
                    val finalCaption = if (overlayText.isNotBlank() && mode == "reel") "$overlayText • $caption".trim() else caption
                    onCreatePost(
                        mode,
                        mediaUrl,
                        selectedFilter,
                        finalCaption,
                        location,
                        tags,
                        if (mode == "reel") musicName else "",
                        if (mode == "reel") reelDuration else 0L
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = InstaMagenta),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("publish_post_button")
        ) {
            Text(
                text = when (mode) {
                    "story" -> "24 Ghante ki Story Lagayein"
                    "reel" -> "Reel Upload Karein (9:16)"
                    else -> "Post Karein"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ReelsScreen(
    currentUserId: String,
    reels: List<PostItem>,
    followingIds: Set<String>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    onCreateReelClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onFollowAuthor: (String) -> Unit,
    onLikeReel: (PostItem) -> Unit,
    onCommentReel: (PostItem) -> Unit,
    onSaveReel: (String) -> Unit
) {
    val context = LocalContext.current

    if (reels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Abhi koi Reel nahi hai!",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "15s, 30s ya 60s ki apni pehli 9:16 Reel banayein!",
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateReelClick,
                    colors = ButtonDefaults.buttonColors(containerColor = InstaMagenta)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reel Banayein", color = Color.White)
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val reel = reels[page]
            val isLiked = reel.id in likedPostIds
            val isSaved = reel.id in savedPostIds
            val isFollowing = reel.authorId in followingIds

            Box(modifier = Modifier.fillMaxSize()) {
                FilteredMediaBox(
                    mediaUrl = reel.mediaUrl,
                    mediaType = "reel",
                    filterName = reel.filterName,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom gradient for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                // Right side actions
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { onLikeReel(reel) }) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Color(0xFFE91E63) else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text("${reel.likesCount}", color = Color.White, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { onCommentReel(reel) }) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comment",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text("${reel.commentsCount}", color = Color.White, fontSize = 12.sp)
                    }
                    IconButton(onClick = { sharePost(context, reel) }) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { onSaveReel(reel.id) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Music",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Bottom Left Info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 80.dp, bottom = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarView(
                            avatarData = reel.authorAvatar,
                            name = reel.authorUsername,
                            size = 38.dp,
                            onClick = { onAuthorClick(reel.authorId) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reel.authorUsername,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onAuthorClick(reel.authorId) }
                        )
                        if (reel.authorId != currentUserId && !isFollowing) {
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = { onFollowAuthor(reel.authorId) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text("Follow", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                    if (reel.caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = reel.caption, color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${reel.musicName.ifBlank { "Original Audio" }} • ${if (reel.durationSec > 0) "${reel.durationSec}s" else "15s"}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Top Bar overlay for Reels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reels", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Surface(
                color = InstaBlue,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.clickable { onCreateReelClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reel Banayein", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
