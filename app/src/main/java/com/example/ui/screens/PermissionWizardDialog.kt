package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.InstaMagenta

data class AppPermissionStep(
    val id: String,
    val title: String,
    val reason: String,
    val fallbackTip: String,
    val icon: ImageVector,
    val runtimePermission: String? = null,
    val settingsAction: String? = null
)

val MERA_INSTA_PERMISSIONS: List<AppPermissionStep> = listOf(
    AppPermissionStep(
        id = "camera",
        title = "Camera Permission",
        reason = "Photo aur Video banane ke liye",
        fallbackTip = "Koi baat nahi! Aap Gallery se Photo/Video upload kar sakte ho.",
        icon = Icons.Default.CameraAlt,
        runtimePermission = Manifest.permission.CAMERA
    ),
    AppPermissionStep(
        id = "mic",
        title = "Microphone Permission",
        reason = "Reels me awaaz aur Voice Message ke liye",
        fallbackTip = "Koi baat nahi! Aap bina awaaz ke Reels aur Text/Photo Chat kar sakte ho.",
        icon = Icons.Default.Mic,
        runtimePermission = Manifest.permission.RECORD_AUDIO
    ),
    AppPermissionStep(
        id = "storage",
        title = "Storage / Photos & Videos",
        reason = "Photo Video save aur upload karne ke liye",
        fallbackTip = "Koi baat nahi! Android Photo Picker bina permission ke surakshit tarike se Photo chunne deta hai.",
        icon = Icons.Default.PhotoLibrary
    ),
    AppPermissionStep(
        id = "notifications",
        title = "Notifications Permission",
        reason = "Like, Comment, Follow, Message ka batane ke liye",
        fallbackTip = "Koi baat nahi! Aap app ke andar Bell icon par saare Notifications dekh sakte ho.",
        icon = Icons.Default.Notifications,
        runtimePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null
    ),
    AppPermissionStep(
        id = "location",
        title = "Location Permission",
        reason = "Post pe Location lagane aur aas-paas ke dost dhoondhne ke liye",
        fallbackTip = "Koi baat nahi! Aap Post karte samay shehar ka naam khud likh sakte ho.",
        icon = Icons.Default.LocationOn,
        runtimePermission = Manifest.permission.ACCESS_COARSE_LOCATION
    ),
    AppPermissionStep(
        id = "contacts",
        title = "Contacts Permission",
        reason = "Apne Contacts se dost dhoondhne aur Follow karne ke liye",
        fallbackTip = "Koi baat nahi! Aap Search tab me naam likh kar dosto ko dhoondh sakte ho.",
        icon = Icons.Default.Contacts,
        runtimePermission = Manifest.permission.READ_CONTACTS
    ),
    AppPermissionStep(
        id = "accessibility",
        title = "Accessibility Permission",
        reason = "App ko aasani se chalane aur dusre app se Post Share karne ke liye",
        fallbackTip = "Koi baat nahi! Aap Share button se sidhe WhatsApp/apps me post bhej sakte ho.",
        icon = Icons.Default.AccessibilityNew,
        settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
    ),
    AppPermissionStep(
        id = "overlay",
        title = "Display Over Other Apps",
        reason = "Kisi bhi app ke upar Notification aur Chat Bubble dikhane ke liye",
        fallbackTip = "Koi baat nahi! App kholte hi aapko naye messages aur notifications dikh jayenge.",
        icon = Icons.Default.Layers,
        settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    ),
    AppPermissionStep(
        id = "internet",
        title = "Internet / Online Permission",
        reason = "Online Post, Follow, Chat ke liye",
        fallbackTip = "Koi baat nahi! Internet chalu hote hi aapke saare posts aur chats sync ho jayenge.",
        icon = Icons.Default.Wifi
    ),
    AppPermissionStep(
        id = "phone_state",
        title = "Phone State Permission",
        reason = "Call aane pe App ko rokne ke liye",
        fallbackTip = "Koi baat nahi! Call aane par aap khud Reels ko pause kar sakte ho.",
        icon = Icons.Default.Call,
        runtimePermission = Manifest.permission.READ_PHONE_STATE
    )
)

@Composable
fun PermissionWizardDialog(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var declinedFeedback by remember { mutableStateOf<String?>(null) }

    val step = MERA_INSTA_PERMISSIONS[currentIndex]

    fun advanceOrFinish() {
        declinedFeedback = null
        if (currentIndex + 1 < MERA_INSTA_PERMISSIONS.size) {
            currentIndex++
        } else {
            onFinished()
        }
    }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            advanceOrFinish()
        } else {
            declinedFeedback = step.fallbackTip
        }
    }

    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(22.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / MERA_INSTA_PERMISSIONS.size },
                    modifier = Modifier.fillMaxWidth(),
                    color = InstaMagenta
                )
                Spacer(modifier = Modifier.height(14.dp))
                Icon(
                    imageVector = step.icon,
                    contentDescription = step.title,
                    tint = InstaMagenta,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${currentIndex + 1} / ${MERA_INSTA_PERMISSIONS.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = step.reason,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Du permission?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = InstaMagenta
                )
                if (declinedFeedback != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = declinedFeedback!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            if (declinedFeedback != null) {
                Button(
                    onClick = { advanceOrFinish() },
                    colors = ButtonDefaults.buttonColors(containerColor = InstaMagenta),
                    modifier = Modifier.testTag("perm_next_button")
                ) {
                    Text("Samajh gaya, Aage Badhein", color = Color.White)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { declinedFeedback = step.fallbackTip },
                        modifier = Modifier.weight(1f).testTag("perm_no_button")
                    ) {
                        Text("Nahi")
                    }
                    Button(
                        onClick = {
                            when {
                                step.runtimePermission != null -> {
                                    runtimeLauncher.launch(step.runtimePermission)
                                }
                                step.settingsAction != null -> {
                                    try {
                                        val intent = Intent(step.settingsAction).apply {
                                            if (step.settingsAction == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                    advanceOrFinish()
                                }
                                else -> {
                                    advanceOrFinish()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = InstaMagenta),
                        modifier = Modifier.weight(1f).testTag("perm_yes_button")
                    ) {
                        Text("Haan", color = Color.White)
                    }
                }
            }
        }
    )
}
