package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PostComment
import com.example.data.model.PostItem
import com.example.data.model.StoryItem
import com.example.ui.theme.InstaBlue
import com.example.ui.theme.InstaMagenta
import com.example.ui.theme.InstaOrange
import com.example.ui.theme.InstaPurple
import com.example.ui.theme.InstaYellow
import com.example.ui.theme.OnlineGreen
import com.example.ui.util.MediaUtils
import kotlinx.coroutines.delay

@Composable
fun AvatarView(
    avatarData: String,
    name: String,
    size: Dp = 44.dp,
    hasStoryRing: Boolean = false,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val imageBitmap = remember(avatarData) { MediaUtils.decodeBase64ToImageBitmap(avatarData) }
    val ringModifier = if (hasStoryRing) {
        Modifier
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(listOf(InstaYellow, InstaOrange, InstaMagenta, InstaPurple)),
                shape = CircleShape
            )
            .padding(3.dp)
    } else Modifier

    Box(
        modifier = Modifier
            .size(size + if (hasStoryRing) 6.dp else 0.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .then(ringModifier)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(InstaPurple, InstaMagenta))),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "$name profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = name.take(1).uppercase().ifBlank { "M" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.4f).sp
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
            )
        }
    }
}

@Composable
fun FilteredMediaBox(
    mediaUrl: String,
    mediaType: String,
    filterName: String,
    modifier: Modifier = Modifier,
    overlayText: String = ""
) {
    val bitmap = remember(mediaUrl) { MediaUtils.decodeBase64ToImageBitmap(mediaUrl) }
    val colorFilter = remember(filterName) { MediaUtils.colorFilterFor(filterName) }
    val blurMod = if (filterName == "Blur Background") Modifier.blur(6.dp) else Modifier

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                when (filterName) {
                    "Warm" -> listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B))
                    "Cool" -> listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))
                    "Black & White" -> listOf(Color(0xFF232526), Color(0xFF414345))
                    "HDR" -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                    else -> listOf(InstaPurple, InstaMagenta, InstaOrange)
                }
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Post media",
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize().then(blurMod)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                if (mediaType == "video" || mediaType == "reel") {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Video",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = if (mediaUrl.startsWith("studio://")) mediaUrl.removePrefix("studio://") else "Mera Insta Studio",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        if (overlayText.isNotBlank()) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            ) {
                Text(
                    text = overlayText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        if (filterName != "Normal") {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Text(
                    text = filterName,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun PostCard(
    post: PostItem,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowingAuthor: Boolean,
    isOwnPost: Boolean,
    onAuthorClick: () -> Unit,
    onFollowClick: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val context = LocalContext.current
    var showHeartAnim by remember { mutableStateOf(false) }

    LaunchedEffect(showHeartAnim) {
        if (showHeartAnim) {
            delay(650)
            showHeartAnim = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                avatarData = post.authorAvatar,
                name = post.authorUsername,
                size = 36.dp,
                onClick = onAuthorClick
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAuthorClick() }
            ) {
                Text(
                    text = post.authorUsername,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (post.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = post.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!isOwnPost && !isFollowingAuthor) {
                TextButton(
                    onClick = onFollowClick,
                    modifier = Modifier.testTag("post_follow_${post.id}")
                ) {
                    Text("Follow", color = InstaBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Media with Double-Tap Heart Animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(post.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            showHeartAnim = true
                            onDoubleTapLike()
                        }
                    )
                }
        ) {
            FilteredMediaBox(
                mediaUrl = post.mediaUrl,
                mediaType = post.mediaType,
                filterName = post.filterName,
                modifier = Modifier.fillMaxSize()
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = showHeartAnim,
                enter = scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Dil Animation",
                    tint = Color.White,
                    modifier = Modifier.size(96.dp)
                )
            }
        }

        // Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLikeClick, modifier = Modifier.testTag("like_btn_${post.id}")) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onCommentClick, modifier = Modifier.testTag("comment_btn_${post.id}")) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment"
                )
            }
            IconButton(
                onClick = { sharePost(context, post) },
                modifier = Modifier.testTag("share_btn_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "Share"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onSaveClick, modifier = Modifier.testTag("save_btn_${post.id}")) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save"
                )
            }
        }

        // Likes & Caption
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
            Text(
                text = "${post.likesCount} Likes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (post.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(post.authorUsername)
                            append(" ")
                        }
                        append(post.caption)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (post.taggedUsers.isNotEmpty()) {
                Text(
                    text = "Tagged: ${post.taggedUsers.joinToString(", ") { "@$it" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = InstaBlue
                )
            }
            if (post.commentsCount > 0) {
                Text(
                    text = "Saare ${post.commentsCount} comments dekhein",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { onCommentClick() }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}

fun sharePost(context: Context, post: PostItem) {
    val shareText = "Mera Insta par ${post.authorUsername} ki post dekhein: ${post.caption}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Post Share Karein"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    comments: List<PostComment>,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (comments.isEmpty()) {
                Text(
                    text = "Abhi koi comment nahi hai. Pehla comment likhein!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp).align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(verticalAlignment = Alignment.Top) {
                            AvatarView(avatarData = comment.authorAvatar, name = comment.authorUsername, size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = comment.authorUsername,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Comment likhein...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            onSendComment(commentText)
                            commentText = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Bhejein", tint = InstaBlue)
                }
            }
        }
    }
}

@Composable
fun StoryViewerOverlay(
    story: StoryItem,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FilteredMediaBox(
                mediaUrl = story.mediaUrl,
                mediaType = "photo",
                filterName = story.filterName,
                overlayText = story.caption,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AvatarView(avatarData = story.authorAvatar, name = story.authorUsername, size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(story.authorUsername, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("24 Ghante Story", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                    ) {
                        Text("Band Karein", color = Color.White)
                    }
                }
            }
        }
    }
}
