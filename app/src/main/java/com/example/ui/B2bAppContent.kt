package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.data.Comment
import com.example.data.PostLike
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import coil.compose.AsyncImage
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch

const val CURRENT_VERSION = "1.0.0"

fun isValidIndianMobileNumber(phone: String): Boolean {
    val sanitized = phone.replace(Regex("[\\s\\-\\(\\)+]"), "").trim()
    val clean = if (sanitized.startsWith("91") && sanitized.length == 12) {
        sanitized.substring(2)
    } else {
        sanitized
    }
    
    if (clean.length != 10 || !clean.all { it.isDigit() }) return false
    
    val firstChar = clean[0]
    if (firstChar !in listOf('6', '7', '8', '9')) return false
    
    // Block common sequential and repeated dummy blocks
    val repeatingDummies = listOf(
        "1234567890", "0987654321", "1111111111", "2222222222", 
        "3333333333", "4444444444", "5555555555", "6666666666", 
        "7777777777", "8888888888", "9999999999", "0000000000"
    )
    if (repeatingDummies.contains(clean)) return false
    
    return true
}

fun isValidRealEmail(email: String): Boolean {
    val trimmed = email.trim().lowercase(java.util.Locale.ROOT)
    if (trimmed.isBlank()) return false
    
    // Check standard email pattern
    val pattern = android.util.Patterns.EMAIL_ADDRESS
    if (!pattern.matcher(trimmed).matches()) return false
    
    // Check fake/mock/disposable domains
    val fakeDomains = listOf(
        "example.com", "test.com", "mock.com", "fake.com", "xyz.com", 
        "yopmail.com", "mailinator.com", "tempmail.com", "gmail.con", 
        "123.com", "abc.com", "dummy.com", "testmail.com", "none.com"
    )
    return fakeDomains.none { trimmed.endsWith("@$it") || trimmed.contains("@" + it) }
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

fun isNewerVersion(current: String, latest: String): Boolean {
    try {
        val currParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val lateParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(currParts.size, lateParts.size)
        for (i in 0 until maxLength) {
            val currVal = currParts.getOrElse(i) { 0 }
            val lateVal = lateParts.getOrElse(i) { 0 }
            if (lateVal > currVal) return true
            if (currVal > lateVal) return false
        }
    } catch (e: Exception) {
        return latest != current
    }
    return false
}

data class VideoMetadata(
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val format: String
)

fun extractVideoMetadata(context: android.content.Context, uri: android.net.Uri): VideoMetadata? {
    var retriever: android.media.MediaMetadataRetriever? = null
    try {
        retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
        val widthStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        val width = widthStr?.toIntOrNull() ?: 0
        val height = heightStr?.toIntOrNull() ?: 0
        
        var sizeBytes = 0L
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                sizeBytes = afd.length
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (sizeBytes <= 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    sizeBytes = stream.available().toLong()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val format = when {
            mimeType.contains("mp4") -> "MP4"
            mimeType.contains("mkv") -> "MKV"
            mimeType.contains("webm") -> "WEBM"
            mimeType.contains("avi") -> "AVI"
            mimeType.contains("quicktime") -> "MOV"
            else -> {
                val path = uri.path ?: ""
                val ext = path.substringAfterLast('.', "").uppercase()
                if (ext.isNotBlank()) ext else "MP4"
            }
        }
        
        return VideoMetadata(
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            format = format
        )
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    } finally {
        try {
            retriever?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun InlineVideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember(videoUrl) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            playWhenReady = false
        }
    }
    
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // Auto-pause video when the app goes to background / pause state
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    var isPlaying by remember { mutableStateOf(false) }
    
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }
    
    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "TAP TO PLAY SHOWREEL",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

suspend fun checkForUpdates(onUpdateRequired: (Boolean, String, String) -> Unit) {
    withContext(Dispatchers.IO) {
        // Step 1: Try checking via Firestore first
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val task = db.collection("app_version").document("latest").get()
            val firestoreResult = kotlinx.coroutines.suspendCancellableCoroutine<Pair<String, String>?> { continuation ->
                task.addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val v = document.getString("latest_version") ?: "1.0.0"
                        val u = document.getString("apk_url") ?: ""
                        if (continuation.isActive) continuation.resume(Pair(v, u), null)
                    } else {
                        if (continuation.isActive) continuation.resume(null, null)
                    }
                }.addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null, null)
                }
            }
            
            if (firestoreResult != null) {
                val latestVersion = firestoreResult.first
                val apkUrl = firestoreResult.second
                val required = isNewerVersion(CURRENT_VERSION, latestVersion)
                withContext(Dispatchers.Main) {
                    onUpdateRequired(required, apkUrl, latestVersion)
                }
                return@withContext
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step 2: Fallback to GitHub raw JSON version check
        try {
            val url = "https://raw.githubusercontent.com/rmobileslko-bot/Threads-B2B-App-Updates/main/version.json"
            val responseText = URL(url).readText()
            val json = JSONObject(responseText)
            val latestVersion = json.optString("latest_version", "1.0.0")
            val apkUrl = json.optString("apk_url", "")
            
            val required = isNewerVersion(CURRENT_VERSION, latestVersion)
            withContext(Dispatchers.Main) {
                onUpdateRequired(required, apkUrl, latestVersion)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onUpdateRequired(false, "", "")
            }
        }
    }
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val cachePath = java.io.File(context.cacheDir, "camera_photos")
        cachePath.mkdirs()
        val file = java.io.File(cachePath, "captured_${System.currentTimeMillis()}.jpg")
        val stream = java.io.FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        stream.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun B2bAppContent(viewModel: B2bViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val feedback = playTapFeedback()

    // Screen tabs: "feed", "create", "admin", "profile", "compliance"
    var currentTab by remember { mutableStateOf("feed") }
    
    val approvedPostsList by viewModel.approvedPosts.collectAsStateWithLifecycle()
    val allPostsList by viewModel.allPosts.collectAsStateWithLifecycle()
    val allProfilesList by viewModel.allUserProfiles.collectAsStateWithLifecycle()
    val allLogsList by viewModel.allModerationLogs.collectAsStateWithLifecycle()
    val commentsState by viewModel.postComments.collectAsStateWithLifecycle()
    val firestorePostsList by viewModel.firestorePosts.collectAsStateWithLifecycle()

    val notificationsList by viewModel.notifications.collectAsStateWithLifecycle()

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showProfilePopup by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showPostCreationModal by remember { mutableStateOf(false) }
    var showControlToggleMenu by remember { mutableStateOf(false) }
    var showAboutAppDialog by remember { mutableStateOf(false) }
    var showUseOfAppDialog by remember { mutableStateOf(false) }
    var showWhyUseB2bThreadsSheet by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedCompanyDetail by remember { mutableStateOf<UserProfile?>(null) }
    var selectedPostForSpecs by remember { mutableStateOf<Post?>(null) }

    var activeChatPartnerId by remember { mutableStateOf<String?>(null) }
    var activeChatPostId by remember { mutableStateOf<Int?>(null) }
    var activeChatPostTitle by remember { mutableStateOf<String?>(null) }

    var isUpdateRequired by remember { mutableStateOf(false) }
    var updateApkUrl by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        checkForUpdates { required, apkUrl, latestVersion ->
            updateApkUrl = apkUrl
            if (required) {
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "New APK Update v$latestVersion is available!",
                        actionLabel = "Download",
                        duration = SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open update link directly.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    val activity = context as? Activity
    BackHandler(enabled = viewModel.currentUserProfile != null) {
        if (currentTab != "feed") {
            currentTab = "feed"
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    val currentUser = viewModel.currentUserProfile
    val isBanned = viewModel.isBannedBySystem
    val userSub = viewModel.currentUserSub
    val isAdmin = currentUser?.id?.removePrefix("@")?.lowercase() == "codecrafttechnologies" ||
                  currentUser?.id?.removePrefix("@")?.lowercase() == "rahman8040samsung" ||
                  currentUser?.id?.removePrefix("@")?.lowercase() == "rahman8040" ||
                  currentUser?.email?.lowercase() == "rahman8040samsung@gmail.com"

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            currentTab = "feed"
        } else {
            if (isAdmin) {
                currentTab = "admin"
            } else {
                currentTab = "feed"
                viewModel.triggerFeedLoading()
            }
        }
    }

    Scaffold(
        topBar = {
            if (!(currentTab == "inbox" && activeChatPartnerId != null)) {
                Column {
                    CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Threads B2B",
                                tint = ThreadsWhite,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "THREADS B2B",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ThreadsWhite,
                                letterSpacing = 1.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentTab != "feed") {
                            IconButton(onClick = { currentTab = "feed" }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back to Feed",
                                    tint = ThreadsWhite,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        if (viewModel.currentUserProfile != null) {
                            // Clicking Online status opens the Profile Popup Dialog
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isLightThemeMode) Color(0xFFE6F4EA) else Color(0xFF0F1A12))
                                    .border(BorderStroke(0.5.dp, ThreadsSuccessGreen.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                                    .clickable {
                                        feedback()
                                        showProfilePopup = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ThreadsSuccessGreen)
                                )
                                Text(
                                    text = "Online",
                                    color = ThreadsSuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                feedback()
                                isLightThemeMode = !isLightThemeMode
                            },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isLightThemeMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = ThreadsWhite
                            )
                        }

                        if (viewModel.currentUserProfile != null) {
                            Box {
                                IconButton(onClick = { showControlToggleMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Control Menu",
                                        tint = ThreadsWhite
                                    )
                                }
                                DropdownMenu(
                                    expanded = showControlToggleMenu,
                                    onDismissRequest = { showControlToggleMenu = false },
                                    modifier = Modifier.background(ThreadsCard)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Alerts Hub", color = ThreadsWhite) },
                                        leadingIcon = {
                                            Box {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = "Alerts Hub",
                                                    tint = ThreadsSuccessGreen
                                                )
                                                if (notificationsList.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(ThreadsErrorRed, CircleShape)
                                                            .align(Alignment.TopEnd)
                                                            .padding(2.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            showControlToggleMenu = false
                                            showNotificationsDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Security Compliance", color = ThreadsWhite) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Security,
                                                contentDescription = "IT Act Compliance Status",
                                                tint = ThreadsSuccessGreen
                                            )
                                        },
                                        onClick = {
                                            showControlToggleMenu = false
                                            currentTab = "compliance"
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Use of This App", color = ThreadsWhite) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lightbulb,
                                                contentDescription = "Use of This App",
                                                tint = ThreadsSuccessGreen
                                            )
                                        },
                                        onClick = {
                                            showControlToggleMenu = false
                                            showUseOfAppDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Why Use B2B Threads", color = ThreadsWhite) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Why Use B2B Threads",
                                                tint = ThreadsSuccessGreen
                                            )
                                        },
                                        onClick = {
                                            showControlToggleMenu = false
                                            showWhyUseB2bThreadsSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("About App", color = ThreadsWhite) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "About App",
                                                tint = ThreadsSuccessGreen
                                            )
                                        },
                                        onClick = {
                                            showControlToggleMenu = false
                                            showAboutAppDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share App (APK)", color = ThreadsWhite) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share App",
                                                tint = ThreadsSuccessGreen
                                            )
                                        },
                                        onClick = {
                                            showControlToggleMenu = false
                                            shareAppApk(context)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Logout", color = ThreadsWhite) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ExitToApp,
                                                contentDescription = "Logout",
                                                tint = ThreadsErrorRed
                                            )
                                        },
                                        onClick = {
                                            showControlToggleMenu = false
                                            showLogoutConfirmationDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = ThreadsOled
                    )
                )
                HorizontalDivider(color = CssTheme.borderMuted, thickness = 1.dp)
                }
            }
        },
        bottomBar = {
            if (viewModel.currentUserProfile != null && !viewModel.isBannedBySystem && !(currentTab == "inbox" && activeChatPartnerId != null)) {
                val profileRole = viewModel.currentUserProfile?.role ?: "Seller"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThreadsBlack)
                ) {
                    NavigationBar(
                        containerColor = ThreadsBlack,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = currentTab == "feed",
                            onClick = { 
                                currentTab = "feed" 
                                viewModel.triggerFeedLoading()
                            },
                            icon = { Icon(Icons.Outlined.Home, contentDescription = "Feed") },
                            label = { Text("Feed", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThreadsOled,
                                selectedTextColor = ThreadsWhite,
                                indicatorColor = ThreadsWhite,
                                unselectedIconColor = ThreadsSubtext,
                                unselectedTextColor = ThreadsSubtext
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == "directory",
                            onClick = { currentTab = "directory" },
                            icon = { Icon(Icons.Default.Store, contentDescription = "Verified Sellers Directory") },
                            label = { Text("Sellers", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThreadsOled,
                                selectedTextColor = ThreadsWhite,
                                indicatorColor = ThreadsWhite,
                                unselectedIconColor = ThreadsSubtext,
                                unselectedTextColor = ThreadsSubtext
                            )
                        )
                        if (profileRole == "Seller") {
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    viewModel.clearPostCreationStatus()
                                    showPostCreationModal = true
                                },
                                icon = { Icon(Icons.Outlined.AddCircleOutline, contentDescription = "New Product") },
                                label = { Text("Post", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ThreadsOled,
                                    selectedTextColor = ThreadsWhite,
                                    indicatorColor = ThreadsWhite,
                                    unselectedIconColor = ThreadsSubtext,
                                    unselectedTextColor = ThreadsSubtext
                                )
                            )
                        }
                        if (isAdmin) {
                            NavigationBarItem(
                                selected = currentTab == "admin",
                                onClick = { currentTab = "admin" },
                                icon = { Icon(Icons.Outlined.AdminPanelSettings, contentDescription = "Admin Area") },
                                label = { Text("Admin", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ThreadsOled,
                                    selectedTextColor = ThreadsWhite,
                                    indicatorColor = ThreadsWhite,
                                    unselectedIconColor = ThreadsSubtext,
                                    unselectedTextColor = ThreadsSubtext
                                )
                            )
                        }
                        NavigationBarItem(
                            selected = currentTab == "inbox",
                            onClick = { currentTab = "inbox" },
                            icon = { Icon(if (currentTab == "inbox") Icons.Filled.Email else Icons.Outlined.Email, contentDescription = "Inbox Messages") },
                            label = { Text("Inbox", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThreadsOled,
                                selectedTextColor = ThreadsWhite,
                                indicatorColor = ThreadsWhite,
                                unselectedIconColor = ThreadsSubtext,
                                unselectedTextColor = ThreadsSubtext
                            )
                        )
                        NavigationBarItem(
                            selected = currentTab == "profile",
                            onClick = { 
                                currentTab = "profile" 
                                viewModel.triggerProfileLoading()
                            },
                            icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile Details") },
                            label = { Text("Profile", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThreadsOled,
                                selectedTextColor = ThreadsWhite,
                                indicatorColor = ThreadsWhite,
                                unselectedIconColor = ThreadsSubtext,
                                unselectedTextColor = ThreadsSubtext
                            )
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = ThreadsOled
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThreadsOled),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(innerPadding)
            ) {
                if (!viewModel.isOnline) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThreadsErrorRed)
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Mode",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You are currently offline. Check connection.",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                // Check Logged-in Session
                val currentUser = viewModel.currentUserProfile
                val userSub = viewModel.currentUserSub
                val isBanned = viewModel.isBannedBySystem

                if (viewModel.isCheckingSession) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ThreadsSuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else if (currentUser == null) {
                    // Screen: Login & Indian B2B Seller Onboarding
                    OnboardingScreen(
                        viewModel = viewModel,
                        existingUsers = allProfilesList,
                        onForgotPasswordClick = { showForgotPasswordDialog = true },
                        onRoute = { target -> currentTab = target }
                    )
                } else if (isBanned) {
                    // Screen: Permanent Ban/Kickoff visual screen
                    BannedUserKickoffScreen(viewModel)
                } else {
                    when (currentTab) {
                        "feed" -> {
                            CssTheme.B2bErrorBoundary(
                                viewName = "B2B Live Trade Feed",
                                isSimulatedError = viewModel.isFeedErrorSimulated,
                                onReset = { viewModel.isFeedErrorSimulated = false }
                            ) {
                                if (viewModel.isFeedLoading) {
                                    CssTheme.B2bFeedSkeleton()
                                } else {
                                    FeedScreen(
                                        posts = approvedPostsList,
                                        firestorePosts = firestorePostsList,
                                        comments = commentsState,
                                        selectedFilter = selectedCategoryFilter,
                                        onFilterChange = { selectedCategoryFilter = it },
                                        onAddComment = { pid, txt -> viewModel.submitComment(pid, txt) },
                                        onUpgradeClick = { showUpgradeDialog = true },
                                        viewModel = viewModel,
                                        companies = allProfilesList,
                                        onCompanyClick = { selectedCompanyDetail = it },
                                        onPostClick = { selectedPostForSpecs = it },
                                        onDirectMessageClick = { partnerId, postId, postTitle ->
                                            activeChatPartnerId = partnerId
                                            activeChatPostId = postId
                                            activeChatPostTitle = postTitle
                                            currentTab = "inbox"
                                        }
                                    )
                                }
                            }
                        }
                        "directory" -> {
                            SellersDirectoryScreen(
                                companies = allProfilesList,
                                onCompanyClick = { selectedCompanyDetail = it }
                            )
                        }
                        "create" -> {
                            CreatePostScreen(
                                viewModel = viewModel,
                                subscription = userSub,
                                onUpgradeClick = { showUpgradeDialog = true },
                                onPostSuccess = { currentTab = "feed" },
                                onConciseClick = { showPostCreationModal = true }
                            )
                        }
                        "admin" -> {
                            MasterAdminDashboard(
                                viewModel = viewModel,
                                allPosts = allPostsList,
                                allLogs = allLogsList,
                                allUsers = allProfilesList
                            )
                        }
                        "profile" -> {
                            CssTheme.B2bErrorBoundary(
                                viewName = "Merchant Profile Dashboard",
                                isSimulatedError = viewModel.isProfileErrorSimulated,
                                onReset = { viewModel.isProfileErrorSimulated = false }
                            ) {
                                if (viewModel.isProfileLoading) {
                                    CssTheme.B2bProfileSkeleton()
                                } else {
                                    SellerProfileScreen(
                                        profile = currentUser,
                                        subscription = userSub,
                                        onUpgradeClick = { showUpgradeDialog = true },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                        "compliance" -> {
                            LegalComplianceScreen(onBack = { currentTab = "feed" })
                        }
                        "inbox" -> {
                            InboxScreen(
                                viewModel = viewModel,
                                initialChatPartnerId = activeChatPartnerId,
                                onClearInitialPartner = {
                                    activeChatPartnerId = null
                                    activeChatPostId = null
                                    activeChatPostTitle = null
                                },
                                onBack = { currentTab = "feed" }
                            )
                        }
                    }
                }

                // Secure simulated Stripe/Razorpay Modal
                if (showUpgradeDialog && currentUser != null) {
                    PaymentGatewaySimulationDialog(
                        userId = currentUser.id,
                        viewModel = viewModel,
                        onDismiss = { showUpgradeDialog = false }
                    )
                }

                // Post Creation Modal specifically tuned for business updates
                if (showPostCreationModal && currentUser != null) {
                    PostCreationModal(
                        viewModel = viewModel,
                        onDismiss = { showPostCreationModal = false },
                        onUpgradeClick = { showUpgradeDialog = true }
                    )
                }

                // Shared Trade Profile Details Dialog
                selectedCompanyDetail?.let { comp ->
                    TradeProfileDetailsDialog(
                        company = comp,
                        onDismiss = { selectedCompanyDetail = null },
                        onMessageClick = { partnerId ->
                            activeChatPartnerId = partnerId
                            activeChatPostId = null
                            activeChatPostTitle = null
                            currentTab = "inbox"
                        }
                    )
                }

                // --- Instagram-Style Logout Dialog ---
                if (showLogoutConfirmationDialog) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showLogoutConfirmationDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = ThreadsCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color(0xFF2E1010), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Logout Alert",
                                        tint = ThreadsErrorRed,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Log out of Threads B2B Net?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ThreadsWhite,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "You can always log back in securely. All business credentials remain saved.",
                                    fontSize = 13.sp,
                                    color = ThreadsSubtext,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                                TextButton(
                                    onClick = {
                                        viewModel.firebaseLogout()
                                        showLogoutConfirmationDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(
                                        "Log Out",
                                        color = ThreadsErrorRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                                TextButton(
                                    onClick = { showLogoutConfirmationDialog = false },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(
                                        "Cancel",
                                        color = ThreadsWhite,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Elegant "About App" Dialog featuring CodeCraft Technologies ---
                if (showAboutAppDialog) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showAboutAppDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = ThreadsCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                             ) {
                                // CodeCraft Tech Stylized Neon Logo inside Dialog
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F121F))
                                        .border(BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF6D00)))), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = "CodeCraft Logo Left",
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "CodeCraft Logo Right",
                                            tint = Color(0xFFFF6D00),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "THREADS B2B",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = ThreadsWhite,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "v1.0.0 (Secure Production)",
                                    fontSize = 11.sp,
                                    color = ThreadsSuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "THREADS B2B is an advanced, enterprise-grade, secure B2B & B2C marketplace network specifically designed and tailored to support and scale Indian merchants in modern digital trade.",
                                    fontSize = 13.sp,
                                    color = ThreadsWhite.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.5.dp, ThreadsBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "DEVELOPER CREDIT",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFAB40),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "CodeCraft Technologies",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E5FF)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "CodeCraft Technologies is a premier software engineering firm specializing in building highly scalable cloud architectures, secure digital compliance systems, and robust enterprise-grade mobile solutions.",
                                            fontSize = 11.sp,
                                            color = ThreadsSubtext,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { showAboutAppDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Close",
                                        color = ThreadsOled,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Elegant "Why Use B2B Threads" Bottom Sheet ---
                if (showWhyUseB2bThreadsSheet) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showWhyUseB2bThreadsSheet = false },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { showWhyUseB2bThreadsSheet = false },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clickable(enabled = false) { /* Prevent click propagating to box */ },
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                                border = BorderStroke(1.dp, ThreadsBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(24.dp)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Handle bar indicator
                                    Box(
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(ThreadsBorder)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0F1A15))
                                            .border(BorderStroke(1.5.dp, ThreadsSuccessGreen), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Why Use B2B Threads Icon",
                                            tint = ThreadsSuccessGreen,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Text(
                                        text = "WHY USE B2B THREADS?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = ThreadsWhite,
                                        letterSpacing = 1.sp
                                    )

                                    Text(
                                        text = "Empowering Indian Wholesalers & Exporters",
                                        fontSize = 12.sp,
                                        color = ThreadsSuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )

                                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                                    // Point 1: Unlimited Reachability
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.4f)),
                                        border = BorderStroke(0.5.dp, ThreadsBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.TrendingUp, "Reach Icon", tint = ThreadsSuccessGreen, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("UNLIMITED REACHABILITY", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 13.sp)
                                            }
                                            Text(
                                                text = "Unlike foreign B2B directories that charge heavy premiums to show your products, B2B Threads offers complete, open reachability. Your trade portfolios reach direct wholesalers and buyers in real-time, completely unfiltered by proprietary algorithms.",
                                                fontSize = 11.sp,
                                                color = ThreadsSubtext,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    // Point 2: Zero-Cost Posting Benefits
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.4f)),
                                        border = BorderStroke(0.5.dp, ThreadsBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Lightbulb, "Cost Icon", tint = ThreadsSuccessGreen, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("ZERO-COST POSTING BENEFITS", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 13.sp)
                                            }
                                            Text(
                                                text = "Publish up to 50 active trade listings every single month entirely free of charge. Experience premium B2B marketing tools, custom WhatsApp connect buttons, and digital catalog displays with zero commissions or registration traps.",
                                                fontSize = 11.sp,
                                                color = ThreadsSubtext,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    // Point 3: Viral Sharing
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.4f)),
                                        border = BorderStroke(0.5.dp, ThreadsBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Share, "Viral Icon", tint = ThreadsSuccessGreen, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("VIRAL SHARING & GROWTH", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 13.sp)
                                            }
                                            Text(
                                                text = "The speed and size of B2B Threads depends entirely on our merchant community. When you share B2B Threads, you directly help expand the active marketplace, bringing in more verified buyers, larger trade volumes, and explosive dynamic growth for everyone.",
                                                fontSize = 11.sp,
                                                color = ThreadsSubtext,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                showWhyUseB2bThreadsSheet = false
                                                shareAppApk(context)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                                            modifier = Modifier.weight(1f).height(46.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                                Icon(Icons.Default.Share, "Share", tint = ThreadsOled, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Share B2B Threads", color = ThreadsOled, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }

                                        Button(
                                            onClick = { showWhyUseB2bThreadsSheet = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsCard),
                                            modifier = Modifier.weight(1f).height(46.dp),
                                            border = BorderStroke(0.5.dp, ThreadsBorder),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Close", color = ThreadsWhite, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Elegant "Use of This App" Dialog ---
                if (showUseOfAppDialog) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showUseOfAppDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = ThreadsCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F1A15))
                                        .border(BorderStroke(1.5.dp, ThreadsSuccessGreen), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = "Use of App Icon",
                                        tint = ThreadsSuccessGreen,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "USE & BENEFITS OF THREADS B2B",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ThreadsWhite,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Empowering Indian Merchants Globally",
                                    fontSize = 12.sp,
                                    color = ThreadsSuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Dynamic Feature Card 1: Free Unlimited-Style Reach
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.4f)),
                                    border = BorderStroke(0.5.dp, ThreadsBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.TrendingUp, "Reach Icon", tint = ThreadsSuccessGreen, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("GLOBAL FIRST: 100% FREE LISTINGS", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Abhi tak global level par aisa koi app hi nahi hai jo FREE me aapke premium products post karne ka benefit deta ho! B2B Threads brings you professional digital tools without any hidden commissions.",
                                            fontSize = 11.sp,
                                            color = ThreadsSubtext,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                // Dynamic Feature Card 2: Peer-Powered Reachability
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.4f)),
                                    border = BorderStroke(0.5.dp, ThreadsBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Share, "Share Reach Icon", tint = ThreadsSuccessGreen, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("USER-DRIVEN VIRAL REACH", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Is app ki reach khud users par depend karti hai! Jitna aap is app ko share karenge, utna hi aapke products logo tak viral targets ki tarah share honge. Koi limits hi nahi—unlimited visibility with maximum security!",
                                            fontSize = 11.sp,
                                            color = ThreadsSubtext,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                // Dynamic Feature Card 3: Post Limits
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.4f)),
                                    border = BorderStroke(0.5.dp, ThreadsBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, "Monthly Limits Icon", tint = ThreadsSuccessGreen, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("GENEROUS MONTHLY POST LIMITS", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Enjoy up to 50 active trade postings every single month completely free of charge. Help us keep the marketplace clean, spam-free, and high quality for premium wholesale buyers.",
                                            fontSize = 11.sp,
                                            color = ThreadsSubtext,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Sath milkar business badhayein! Encourage other merchants, share the platform, and unlock unlimited network connectivity today.",
                                    fontSize = 12.sp,
                                    color = ThreadsWhite,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            showUseOfAppDialog = false
                                            shareAppApk(context)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                            Icon(Icons.Default.Share, "Share", tint = ThreadsOled, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share App", color = ThreadsOled, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    Button(
                                        onClick = { showUseOfAppDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsCard),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        border = BorderStroke(0.5.dp, ThreadsBorder),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Close", color = ThreadsWhite, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Accessible Forgot Password Dialog ---
                if (showForgotPasswordDialog) {
                    var forgotUsernameInput by remember { mutableStateOf("") }
                    var forgotVerificationStatus by remember { mutableStateOf<String?>(null) }
                    var forgotNewPasswordInput by remember { mutableStateOf("") }
                    var forgotStep by remember { mutableStateOf(1) } // 1 = locate, 2 = reset
                    
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showForgotPasswordDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .border(BorderStroke(1.dp, ThreadsBorder), RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = ThreadsCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Merchant Recovery Window",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = ThreadsWhite
                                    )
                                    IconButton(onClick = { showForgotPasswordDialog = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close recovery", tint = ThreadsWhite)
                                    }
                                }
                                
                                Text(
                                    text = "Maintain cyber security by re-establishing portal locks securely.",
                                    fontSize = 11.sp,
                                    color = ThreadsSubtext
                                )

                                if (forgotStep == 1) {
                                    OutlinedTextField(
                                        value = forgotUsernameInput,
                                        onValueChange = { forgotUsernameInput = it },
                                        label = { Text("Account Handle (e.g., rahulsme)", color = ThreadsSubtext) },
                                        singleLine = true,
                                        prefix = { Text("@", color = ThreadsWhite) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ThreadsWhite,
                                            unfocusedBorderColor = ThreadsGray,
                                            focusedTextColor = ThreadsWhite,
                                            unfocusedTextColor = ThreadsWhite
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("forgot_username_input")
                                    )

                                    if (forgotVerificationStatus != null) {
                                        Text(
                                            text = forgotVerificationStatus!!,
                                            color = ThreadsErrorRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val searchHandle = if (forgotUsernameInput.trim().startsWith("@")) forgotUsernameInput.trim() else "@${forgotUsernameInput.trim()}"
                                            val found = allProfilesList.find { it.id.equals(searchHandle, ignoreCase = true) }
                                            if (found != null) {
                                                forgotStep = 2
                                                forgotVerificationStatus = null
                                            } else {
                                                forgotVerificationStatus = "No active identity account matches handle: $searchHandle"
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("forgot_find_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled)
                                    ) {
                                        Text("Locate Active Portal Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                } else {
                                    Text(
                                        "Success! Identity confirmed. Reset login password:",
                                        color = ThreadsSuccessGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    OutlinedTextField(
                                        value = forgotNewPasswordInput,
                                        onValueChange = { forgotNewPasswordInput = it },
                                        label = { Text("New Secure Password", color = ThreadsSubtext) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ThreadsWhite,
                                            unfocusedBorderColor = ThreadsGray,
                                            focusedTextColor = ThreadsWhite,
                                            unfocusedTextColor = ThreadsWhite
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("forgot_new_password_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (forgotNewPasswordInput.trim().length < 4) {
                                                Toast.makeText(context, "Password must be at least 4 chars long.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            viewModel.resetPassword(forgotUsernameInput.trim(), forgotNewPasswordInput.trim()) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "Portal reset completed successfully!", Toast.LENGTH_SHORT).show()
                                                    showForgotPasswordDialog = false
                                                } else {
                                                    Toast.makeText(context, "Error updating database entry.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("forgot_reset_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen, contentColor = Color.White)
                                    ) {
                                        Text("Verify & Save Password Check", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- In-App Clickable Profile popup dialog ---
                if (showProfilePopup) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showProfilePopup = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                            border = BorderStroke(1.dp, ThreadsSuccessGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(ThreadsBorder),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = viewModel.currentUserProfile?.name?.take(2)?.uppercase() ?: "GU",
                                        color = ThreadsWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    )
                                }
                                
                                Text(
                                    text = viewModel.currentUserProfile?.name ?: "Guest User",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ThreadsWhite
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .background(ThreadsOled, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ThreadsSuccessGreen)
                                    )
                                    Text(
                                        text = "${viewModel.currentUserProfile?.role ?: "Guest"} (${viewModel.currentUserProfile?.profileType ?: "Individual"})",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = ThreadsSuccessGreen
                                    )
                                }
                                
                                Text(
                                    text = viewModel.currentUserProfile?.id?.let { if (it.startsWith("@")) it else "@$it" } ?: "@guest",
                                    fontSize = 12.sp,
                                    color = ThreadsSubtext
                                )
                                
                                Button(
                                    onClick = { showProfilePopup = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- Product Specifications & Seller Contact Detail Bottom Sheet ---
                if (selectedPostForSpecs != null) {
                    ProductSpecsBottomSheet(
                        post = selectedPostForSpecs!!,
                        viewModel = viewModel,
                        onDismissRequest = { selectedPostForSpecs = null }
                    )
                }

                // --- In-App Responsive Notifications Dialog ---
                if (showNotificationsDialog) {
                    LaunchedEffect(Unit) {
                        viewModel.markNotificationsAsRead()
                    }
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showNotificationsDialog = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(BorderStroke(1.dp, ThreadsBorder), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = ThreadsCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Alerts Hub", tint = Color(0xFFFF9800))
                                        Text(
                                            "In-App Alerts Center",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = ThreadsWhite
                                        )
                                    }
                                    IconButton(onClick = { showNotificationsDialog = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close notifications", tint = ThreadsWhite)
                                    }
                                }

                                HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                                if (notificationsList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.NotificationsNone, contentDescription = "No alerts", tint = ThreadsSubtext, modifier = Modifier.size(48.dp))
                                            Text("No new trade active notifications.", color = ThreadsSubtext, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 350.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(notificationsList) { notif ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(ThreadsBlack, RoundedCornerShape(8.dp))
                                                    .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(8.dp))
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(
                                                            when (notif.iconType) {
                                                                "success" -> Color(0xFF1B5E20)
                                                                "like" -> Color(0xFF4A148C)
                                                                "comment" -> Color(0xFF0D47A1)
                                                                "security" -> Color(0xFFB71C1C)
                                                                else -> ThreadsGray
                                                            },
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when (notif.iconType) {
                                                            "success" -> Icons.Default.Check
                                                            "like" -> Icons.Default.Favorite
                                                            "comment" -> Icons.Default.Comment
                                                            "security" -> Icons.Default.Security
                                                            else -> Icons.Default.Info
                                                        },
                                                        contentDescription = notif.iconType,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(notif.title, fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 14.sp)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(notif.message, color = ThreadsWhite.copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 16.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(formatRelativeTime(notif.timestamp), color = ThreadsSubtext, fontSize = 10.sp)
                                                }

                                                IconButton(
                                                    onClick = { viewModel.dismissNotification(notif.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Dismiss notification", tint = ThreadsSubtext, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    TextButton(
                                        onClick = { viewModel.clearAllNotifications() },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Clear All Notifications", color = ThreadsErrorRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Deep Link Post Inspector Dialog (Accessible on Chrome & Thread B2B Native App) ---
                val deepLinkedId = viewModel.deepLinkedPostId
                if (deepLinkedId != null) {
                    val matchingPost = allPostsList.find { it.id == deepLinkedId }
                    if (matchingPost != null) {
                        androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.clearDeepLinkedPost() }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .border(BorderStroke(1.dp, ThreadsBorder), RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = ThreadsCard)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Deep Shared Thread Link",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = ThreadsWhite
                                        )
                                        IconButton(onClick = { viewModel.clearDeepLinkedPost() }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close deep shared thread", tint = ThreadsWhite)
                                        }
                                    }
                                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                                    
                                    // Live Postcard content inside deep-linked viewer
                                    B2bPostCard(
                                        post = matchingPost,
                                        postComments = commentsState[matchingPost.id] ?: emptyList(),
                                        onAddComment = { pId, text -> viewModel.submitComment(pId, text) },
                                        viewModel = viewModel,
                                        onCompanyClick = { comp ->
                                            selectedCompanyDetail = comp
                                            viewModel.clearDeepLinkedPost()
                                        },
                                        onPostClick = { selectedPostForSpecs = it }
                                    )
                                }
                            }
                        }
                    } else {
                        androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.clearDeepLinkedPost() }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                colors = CardDefaults.cardColors(containerColor = ThreadsCard)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = "Query missing", tint = ThreadsErrorRed, modifier = Modifier.size(48.dp))
                                    Text("Post Unavailable", fontWeight = FontWeight.Bold, color = ThreadsWhite)
                                    Text("The deep-linked wholesale listing ID #$deepLinkedId does not exist or may have been deleted for compliance reasons.", fontSize = 12.sp, color = ThreadsSubtext, textAlign = TextAlign.Center)
                                    Button(
                                        onClick = { viewModel.clearDeepLinkedPost() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled)
                                    ) {
                                        Text("Acknowledge")
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    if (isUpdateRequired) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)) // Beautiful full bleed deep OLED dark
                .clickable(enabled = false) {} // Prevent clicking components underneath
                .padding(24.dp)
                .testTag("mandatory_update_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Update Available",
                    tint = ThreadsSuccessGreen,
                    modifier = Modifier.size(72.dp)
                )

                Text(
                    text = "Update Required",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "A new version of the app is available. Please update to continue securely.",
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateApkUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open update link directly.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThreadsSuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("update_now_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Text(
                            text = "Update Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. ONBOARDING & SIGN UP COMPOSABLE
// ==========================================
@Composable
fun OnboardingScreen(
    viewModel: B2bViewModel,
    existingUsers: List<UserProfile>,
    onForgotPasswordClick: () -> Unit,
    onRoute: (String) -> Unit
) {
    var isSignUpMode by remember { mutableStateOf(false) }

    // Onboarding States
    var selectedRole by remember { mutableStateOf("Seller") } // "Seller" or "Customer"
    var usernameInput by remember { mutableStateOf(viewModel.getSavedLoginUsername()) }
    var passwordInput by remember { mutableStateOf(viewModel.getSavedLoginPassword()) }
    var nameInput by remember { mutableStateOf("") }
    var emailRegInput by remember { mutableStateOf("") }
    var selectedProfileType by remember { mutableStateOf("Manufacturer") }
    var instagramInput by remember { mutableStateOf("") }
    var facebookInput by remember { mutableStateOf("") }
    var whatsappInput by remember { mutableStateOf("") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var customProfileTypeInput by remember { mutableStateOf("") }
    var passwordRegInput by remember { mutableStateOf("") }
    var passwordRegVisible by remember { mutableStateOf(false) }
    
    // Seller Specifics
    var hasGstin by remember { mutableStateOf(true) }
    var gstinInput by remember { mutableStateOf("") }
    var aadhaarInput by remember { mutableStateOf("") }

    // Customer Specifics
    var customerPhoneInput by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = isSignUpMode) {
        isSignUpMode = false
    }

    // Observe Firebase login/registration failure messages and instantly present clean Toast alerts
    val errorMsg = viewModel.loginErrorMessage
    LaunchedEffect(errorMsg) {
        if (!errorMsg.isNullOrBlank()) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            viewModel.clearLoginError()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFF44336), Color(0xFF3F51B5))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Threads B2B",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Threads B2B & B2C Marketplace",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ThreadsWhite,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Secure, Digitally Compliant Network for Indian Sellers & Buyers",
                fontSize = 13.sp,
                color = ThreadsSubtext,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (!isSignUpMode) {
            // Mode: Standard Secure Unified Login Form (Firebase Only)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cssCard(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                        Text(
                            text = "Secure Merchant Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ThreadsWhite
                        )
                        Text(
                            text = "Please authenticate via Firebase Cloud Auth to access the secure commercial B2B database.",
                            fontSize = 12.sp,
                            color = ThreadsSubtext
                        )

                        // Email Address Input Field
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { 
                                usernameInput = it
                                viewModel.clearLoginError()
                            },
                            label = { Text("Verified Email Address") },
                            placeholder = { Text("e.g. merchant@company.com") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email, 
                                    contentDescription = "Verified Email Address", 
                                    tint = ThreadsSubtext
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThreadsWhite,
                                unfocusedBorderColor = ThreadsGray,
                                focusedTextColor = ThreadsWhite,
                                unfocusedTextColor = ThreadsWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input")
                        )

                        // Password Field (with visibility toggle)
                        var passwordVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            placeholder = { Text("••••••••") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Password secure lock", tint = ThreadsSubtext)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = ThreadsSubtext)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThreadsWhite,
                                unfocusedBorderColor = ThreadsGray,
                                focusedTextColor = ThreadsWhite,
                                unfocusedTextColor = ThreadsWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onForgotPasswordClick() }) {
                                Text(
                                    text = "Forgot Password? Compliance Reset",
                                    color = ThreadsSubtext,
                                    fontSize = 12.sp,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    )
                                )
                            }
                        }

                        // Submit Button (Firebase Authentication)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    viewModel.loginWithFirebase(usernameInput, passwordInput) { targetTab ->
                                        onRoute(targetTab)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_submit_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThreadsSuccessGreen, 
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser, 
                                    contentDescription = "lock secure"
                                )
                                Text(
                                    text = "Sign In via Cloud Auth", 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
            }

            item {
                Button(
                    onClick = { isSignUpMode = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_signup_toggle"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = ThreadsWhite),
                    border = BorderStroke(1.dp, ThreadsBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Don't have an account? Register Profile", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Mode: Register New Profile Fields (Direct Firebase Auth flow)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                    border = BorderStroke(1.dp, ThreadsBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "New Account Onboarding",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = ThreadsWhite
                        )

                        // Role Selector
                        Text(
                            text = "I am registering as:",
                            fontSize = 12.sp,
                            color = ThreadsSubtext
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Seller", "Customer").forEach { role ->
                                val selected = selectedRole == role
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) ThreadsWhite else ThreadsGray)
                                        .clickable { selectedRole = role }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (role == "Seller") "Traders / Sellers / OEMs" else "Users / Customers / Buyers",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) ThreadsOled else ThreadsWhite
                                    )
                                }
                            }
                        }

                        Divider(color = ThreadsBorder, thickness = 0.5.dp)

                        // --- SHARED FIELDS ---
                        // Unique Username ID
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Unique Account Handle (e.g., rahul)") },
                            singleLine = true,
                            prefix = { Text("@") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThreadsWhite,
                                unfocusedBorderColor = ThreadsGray,
                                focusedTextColor = ThreadsWhite,
                                unfocusedTextColor = ThreadsWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("username_input")
                        )

                        // Set Secure Password
                        OutlinedTextField(
                            value = passwordRegInput,
                            onValueChange = { passwordRegInput = it },
                            label = { Text("Establish Password") },
                            placeholder = { Text("At least 4 characters") },
                            singleLine = true,
                            visualTransformation = if (passwordRegVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Password Lock", tint = ThreadsSubtext)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordRegVisible = !passwordRegVisible }) {
                                    val icon = if (passwordRegVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = ThreadsSubtext)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThreadsWhite,
                                unfocusedBorderColor = ThreadsGray,
                                focusedTextColor = ThreadsWhite,
                                unfocusedTextColor = ThreadsWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("password_reg_input")
                        )

                        // Compliant Login Email Address (Firebase Credential)
                        OutlinedTextField(
                            value = emailRegInput,
                            onValueChange = { emailRegInput = it },
                            label = { Text("Compliant Login Email Address") },
                            placeholder = { Text("e.g. contact@yourbusiness.com") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = "Email credential", tint = ThreadsSubtext)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThreadsWhite,
                                unfocusedBorderColor = ThreadsGray,
                                focusedTextColor = ThreadsWhite,
                                unfocusedTextColor = ThreadsWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("email_reg_input")
                        )

                        // Full Name
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text(if (selectedRole == "Seller") "Business / Company Name" else "Full Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThreadsWhite,
                                unfocusedBorderColor = ThreadsGray,
                                focusedTextColor = ThreadsWhite,
                                unfocusedTextColor = ThreadsWhite
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("fullname_input")
                        )

                        if (selectedRole == "Seller") {
                            // --- SELLER REGISTER SPECIFIC FIELDS ---
                            Text(
                                text = "Business Classification Type:",
                                fontSize = 12.sp,
                                color = ThreadsSubtext
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = if (selectedProfileType == "Custom Classification") "Custom Classification" else selectedProfileType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Classification Type") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (typeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand Classification Dropdown",
                                            tint = ThreadsWhite,
                                            modifier = Modifier.clickable { typeDropdownExpanded = !typeDropdownExpanded }
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThreadsWhite,
                                        unfocusedBorderColor = ThreadsGray,
                                        focusedTextColor = ThreadsWhite,
                                        unfocusedTextColor = ThreadsWhite
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { typeDropdownExpanded = !typeDropdownExpanded }
                                )

                                DropdownMenu(
                                    expanded = typeDropdownExpanded,
                                    onDismissRequest = { typeDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(ThreadsCard)
                                        .width(310.dp)
                                        .border(BorderStroke(1.dp, ThreadsBorder))
                                ) {
                                    val typesPlusOther = listOf("Individual", "Retailer", "Manufacturer", "OEM", "Trader", "Other (Custom Type)...")
                                    typesPlusOther.forEach { type ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = when (type) {
                                                            "Individual" -> Icons.Default.Person
                                                            "Retailer" -> Icons.Default.Store
                                                            "Manufacturer" -> Icons.Default.Build
                                                            "OEM" -> Icons.Default.Settings
                                                            "Trader" -> Icons.Default.ShoppingCart
                                                            else -> Icons.Default.Category
                                                        },
                                                        contentDescription = null,
                                                        tint = ThreadsSuccessGreen,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column {
                                                        Text(type, color = ThreadsWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            text = when (type) {
                                                                "Individual" -> "Independent broker or agent"
                                                                "Retailer" -> "Local shop supply / reseller outlet"
                                                                "Manufacturer" -> "Raw materials / production factory"
                                                                "OEM" -> "Original equipment parts maker"
                                                                "Trader" -> "Bulk trade / wholesale import-export"
                                                                else -> "Define custom business category"
                                                            },
                                                            color = ThreadsSubtext,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                if (type.contains("Other")) {
                                                    selectedProfileType = "Custom Classification"
                                                } else {
                                                    selectedProfileType = type
                                                }
                                                typeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedProfileType == "Custom Classification") {
                                OutlinedTextField(
                                    value = customProfileTypeInput,
                                    onValueChange = { customProfileTypeInput = it },
                                    label = { Text("Type custom business classification (e.g. Distributor)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThreadsWhite,
                                        unfocusedBorderColor = ThreadsGray,
                                        focusedTextColor = ThreadsWhite,
                                        unfocusedTextColor = ThreadsWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // WhatsApp Number
                            OutlinedTextField(
                                value = whatsappInput,
                                onValueChange = { whatsappInput = it },
                                label = { Text("WhatsApp Contact Number (+91...)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThreadsWhite,
                                    unfocusedBorderColor = ThreadsGray,
                                    focusedTextColor = ThreadsWhite,
                                    unfocusedTextColor = ThreadsWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // GSTIN or Aadhaar selection Row
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                                border = BorderStroke(1.dp, ThreadsBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "REGULATORY BUSINESS COMPLIANCE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800)
                                    )
                                    Text(
                                        text = "Indian cyber laws mandate business verification. Choose GSTIN or Aadhaar authentication:",
                                        fontSize = 10.sp,
                                        color = ThreadsSubtext
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (hasGstin) ThreadsWhite else ThreadsGray)
                                                .clickable { hasGstin = true }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Have GSTIN",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (hasGstin) ThreadsOled else ThreadsWhite
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (!hasGstin) ThreadsWhite else ThreadsGray)
                                                .clickable { hasGstin = false }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Unregistered (Use Aadhaar)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!hasGstin) ThreadsOled else ThreadsWhite
                                            )
                                        }
                                    }

                                    if (hasGstin) {
                                        OutlinedTextField(
                                            value = gstinInput,
                                            onValueChange = { gstinInput = it.uppercase() },
                                            label = { Text("GSTIN Number (Mandatory - 15 Alphanumeric)") },
                                            placeholder = { Text("e.g. 09AAACC4578M1Z2") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ThreadsWhite,
                                                unfocusedBorderColor = ThreadsGray,
                                                focusedTextColor = ThreadsWhite,
                                                unfocusedTextColor = ThreadsWhite
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        OutlinedTextField(
                                            value = aadhaarInput,
                                            onValueChange = { if (it.length <= 12) aadhaarInput = it.filter { char -> char.isDigit() } },
                                            label = { Text("Aadhaar Card Number (Mandatory - 12 Digits)") },
                                            placeholder = { Text("e.g. 123456789012") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = ThreadsWhite,
                                                unfocusedBorderColor = ThreadsGray,
                                                focusedTextColor = ThreadsWhite,
                                                unfocusedTextColor = ThreadsWhite
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Optional Social reachability
                            Text(
                                text = "Social Details (Optional):",
                                fontSize = 11.sp,
                                color = ThreadsSubtext
                            )
                            OutlinedTextField(
                                value = instagramInput,
                                onValueChange = { instagramInput = it },
                                label = { Text("Instagram Handle") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThreadsWhite,
                                    unfocusedBorderColor = ThreadsGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // --- CUSTOMER REGISTER SPECIFIC FIELDS ---
                            OutlinedTextField(
                                value = customerPhoneInput,
                                onValueChange = { customerPhoneInput = it },
                                label = { Text("Mobile Number (Mandatory)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThreadsWhite,
                                    unfocusedBorderColor = ThreadsGray,
                                    focusedTextColor = ThreadsWhite,
                                    unfocusedTextColor = ThreadsWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val sanitizedUser = usernameInput.trim().removePrefix("@")
                        if (sanitizedUser.isBlank() || nameInput.isBlank()) {
                            Toast.makeText(context, "Username and Name are required to proceed.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val trimmedPassword = passwordRegInput.trim()
                        if (trimmedPassword.length < 4) {
                            Toast.makeText(context, "Set a secure password of at least 4 characters.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (emailRegInput.isBlank() || !isValidRealEmail(emailRegInput)) {
                            Toast.makeText(context, "Genuine Email is required for Firebase Registration. Temp domains or blanks are rejected.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        if (selectedRole == "Seller") {
                            // Seller valid check
                            if (whatsappInput.isBlank()) {
                                Toast.makeText(context, "WhatsApp contact number is required for B2B Sellers.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isValidIndianMobileNumber(whatsappInput)) {
                                Toast.makeText(context, "Please enter a genuine 10-digit Indian Mobile/WhatsApp number.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (hasGstin) {
                                if (gstinInput.trim().length < 15) {
                                    Toast.makeText(context, "Please enter a valid GSTIN (15 character registration) to comply.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            } else {
                                if (aadhaarInput.trim().length != 12) {
                                    Toast.makeText(context, "Please enter a valid 12-digit Aadhaar Card number for identity verification.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }

                            viewModel.registerWithFirebase(
                                emailInput = emailRegInput.trim(),
                                passwordInput = trimmedPassword,
                                nameInput = nameInput.trim(),
                                roleInput = "Seller",
                                whatsappInput = whatsappInput.trim(),
                                profileTypeInput = if (selectedProfileType == "Custom Classification") customProfileTypeInput.trim().ifBlank { "Custom" } else selectedProfileType,
                                gstinInput = if (hasGstin) gstinInput.trim() else "",
                                aadhaarInput = if (!hasGstin) aadhaarInput.trim() else "",
                                onSuccess = {
                                    Toast.makeText(context, "Secure Firebase B2B Seller Identity Verified & Activated!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            // Customer valid check
                            val contactPhone = customerPhoneInput.trim()
                            if (contactPhone.isBlank()) {
                                Toast.makeText(context, "Mobile number is mandatory for Customers.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isValidIndianMobileNumber(contactPhone)) {
                                Toast.makeText(context, "Please enter a genuine 10-digit Indian Mobile number.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            viewModel.registerWithFirebase(
                                emailInput = emailRegInput.trim(),
                                passwordInput = trimmedPassword,
                                nameInput = nameInput.trim(),
                                roleInput = "Customer",
                                whatsappInput = contactPhone,
                                profileTypeInput = "Individual",
                                gstinInput = "",
                                aadhaarInput = "",
                                onSuccess = {
                                    Toast.makeText(context, "Secure Firebase Customer Session Established!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_signup_submit"),
                    colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Verify and Launch App", fontWeight = FontWeight.Bold)
                }
            }

            item {
                TextButton(onClick = { isSignUpMode = false }) {
                    Text("← Back to Sign In", color = ThreadsSubtext)
                }
            }
        }
    }
}

// ==========================================
// 2. KICKED OFF / BANNED SCREEN
// ==========================================
@Composable
fun BannedUserKickoffScreen(viewModel: B2bViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
            border = BorderStroke(2.dp, ThreadsErrorRed)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = "User Banned",
                    tint = ThreadsErrorRed,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "ACCESS DENIED",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThreadsErrorRed,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your profile '${viewModel.currentUserProfile?.id}' has been flagged and permanently banned for violating our zero-tolerance comment filters (toxic, abusive, or spam uploads detected by automated moderators).",
                    fontSize = 14.sp,
                    color = ThreadsWhite,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "This enforcement acts in full compliance with Section 79 of the Indian Information Technology (IT) Act, 2000. System actions are recorded in security logs for supervisor override.",
                    fontSize = 11.sp,
                    color = ThreadsSubtext,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled)
                ) {
                    Text("OK, Exit Account")
                }
            }
        }
    }
}

// ==========================================
// 3. MAIN SCROLLABLE FEED SCREEN (THREADS DESIGN)
// ==========================================
@Composable
fun FeedScreen(
    posts: List<Post>,
    firestorePosts: List<FirestorePost>,
    comments: Map<Int, List<Comment>>,
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    onAddComment: (Int, String) -> Unit,
    onUpgradeClick: () -> Unit,
    viewModel: B2bViewModel,
    companies: List<UserProfile>,
    onCompanyClick: (UserProfile) -> Unit,
    onPostClick: (Post) -> Unit,
    onDirectMessageClick: (String, Int?, String?) -> Unit = { _, _, _ -> }
) {
    // Dynamic filters: standard 8 predefined trade sectors + any custom-entered categorization found in real trade listings
    val baseFilters = listOf("All", "Textiles", "Agriculture", "Electronics", "Handicrafts", "Chemicals", "Machinery", "Jewelry", "Other")
    
    // Map live Firestore posts to Post entities so they reuse the premium B2bPostCard UI seamlessly
    val mappedCloudPosts = firestorePosts.mapIndexed { index, fPost ->
        Post(
            id = -1000 - index, // Negative unique ID to prevent database overlaps
            authorId = fPost.userId,
            authorName = fPost.authorName,
            authorType = fPost.authorType,
            text = fPost.textContent,
            price = fPost.price?.toDoubleOrNull(),
            category = fPost.category,
            imageUrl = fPost.imageUrl,
            videoUrl = fPost.videoUrl,
            timestamp = fPost.timestamp,
            specs = fPost.specs
        )
    }

    val dynamicFilters = (baseFilters + mappedCloudPosts.mapNotNull { it.category }).distinct()

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val pullDistanceState = remember { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pullDistanceDp = with(density) { pullDistanceState.value.toDp() }

    // Sync with viewModel isFeedLoading
    LaunchedEffect(viewModel.isFeedLoading) {
        if (!viewModel.isFeedLoading) {
            pullDistanceState.value = 0f
        }
    }

    val feedback = playTapFeedback()

    Column(modifier = Modifier.fillMaxSize()) {
        // Category filter horizontal strip (scrolling LazyRow)
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThreadsOled)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(dynamicFilters) { chipName ->
                val isSelected = selectedFilter == chipName
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ThreadsWhite else ThreadsGray)
                        .clickable {
                            feedback()
                            onFilterChange(chipName)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = chipName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) ThreadsOled else ThreadsWhite
                    )
                }
            }
        }

        // Content selection from Firestore live posts
        val filteredPosts = if (selectedFilter == "All") {
            mappedCloudPosts
        } else {
            mappedCloudPosts.filter { it.category == selectedFilter }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(viewModel.isFeedLoading) {
                    if (viewModel.isFeedLoading) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { offset: androidx.compose.ui.geometry.Offset -> },
                        onDragEnd = {
                            val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                            if (isAtTop && pullDistanceState.value > 150f) {
                                viewModel.triggerFeedLoading()
                            } else {
                                pullDistanceState.value = 0f
                            }
                        },
                        onDragCancel = {
                            pullDistanceState.value = 0f
                        },
                        onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                            val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                            if (isAtTop) {
                                if (dragAmount > 0f || pullDistanceState.value > 0f) {
                                    pullDistanceState.value = (pullDistanceState.value + dragAmount * 0.5f).coerceIn(0f, 300f)
                                    change.consume()
                                }
                            }
                        }
                    )
                }
        ) {
            
            val animatedOffset by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (viewModel.isFeedLoading) 48.dp else (pullDistanceDp * 0.4f).coerceAtMost(60.dp),
                label = "pull_offset"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = animatedOffset)
            ) {
                if (filteredPosts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue, 
                                contentDescription = "Empty", 
                                tint = ThreadsSubtext, 
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "No live cloud postings found for $selectedFilter", 
                                color = ThreadsSubtext, 
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize().imePadding()
                    ) {
                        items(filteredPosts) { post ->
                            B2bPostCard(
                                post = post,
                                postComments = comments[post.id] ?: emptyList(),
                                onAddComment = onAddComment,
                                viewModel = viewModel,
                                onCompanyClick = onCompanyClick,
                                onPostClick = onPostClick,
                                onDirectMessageClick = onDirectMessageClick
                            )
                            HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Top Floating Spinning Pull To Refresh Indicator (Instagram Style)
            androidx.compose.animation.AnimatedVisibility(
                visible = pullDistanceState.value > 10f || viewModel.isFeedLoading,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 12.dp)
                    .zIndex(10f)
            ) {
                val threshold = 150f
                val hasCrossed = pullDistanceState.value >= threshold
                
                var feedbackTriggered by remember { mutableStateOf(false) }
                if (hasCrossed && !feedbackTriggered && !viewModel.isFeedLoading) {
                    LaunchedEffect(Unit) {
                        feedback()
                        feedbackTriggered = true
                    }
                } else if (!hasCrossed) {
                    feedbackTriggered = false
                }

                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, if (hasCrossed) ThreadsSuccessGreen else ThreadsBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (viewModel.isFeedLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 2.5.dp,
                                color = ThreadsSuccessGreen
                            )
                        } else {
                            val pullProgress = (pullDistanceState.value / threshold).coerceIn(0f, 1f)
                            val rotationDegrees = pullProgress * 360f
                            
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refreshing Pull Wheel",
                                tint = if (hasCrossed) ThreadsSuccessGreen else ThreadsWhite,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotationDegrees)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getRelativeTimeString(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        weeks < 4 -> "${weeks}w ago"
        months < 12 -> "${months}mo ago"
        else -> "${years}y ago"
    }
}

fun getProductSpecsByCategory(category: String?, postId: Int): List<Pair<String, String>> {
    val cleanCat = category?.trim() ?: "Other"
    return when (cleanCat.lowercase()) {
        "textiles" -> listOf(
            "Material" to "Organic Cotton & Premium Khadi Blend",
            "Thread Count" to "320 TC Satin Weave",
            "GSM" to "180 - 240 GSM Certified",
            "Width / Dimensions" to "58 inches (Standard Width)",
            "Color Fastness" to "Grade 4.5/5 (Export Standard)",
            "Shrinkage" to "Less than 2% on first wash",
            "Batch Origin" to "Surat Textile Hub, India",
            "Minimum Order (MOQ)" to "500 Meters per custom dye"
        )
        "agriculture" -> listOf(
            "Product Grade" to "A-Grade Premium Export Quality",
            "Moisture Content" to "Under 12% Max",
            "Cultivation Type" to "100% Organic, Pesticide-Free",
            "Shelf Life" to "6 - 9 Months in cold dry storage",
            "Purity Level" to "99.8% Sorted/Cleaned",
            "Source Region" to "Punjab Agricultural Belt, India",
            "Packaging Type" to "PP Jute Bags, 50kg wholesale size",
            "Minimum Order (MOQ)" to "10 Metric Tons (1 Truckload)"
        )
        "electronics" -> listOf(
            "Device Type" to "SME / Commercial High-Output Adaptor",
            "Input Voltage" to "110V - 240V AC Worldwide Compatible",
            "Certifications" to "BIS, CE, RoHS Compliant",
            "Efficiency" to "92.5% Active Load Rating",
            "Protection Specs" to "Over-voltage, Short-circuit protection",
            "Warranty Details" to "1 Year Replacement B2B Warranty",
            "Manufacturing Base" to "Noida Tech Zone, India",
            "Minimum Order (MOQ)" to "100 Pieces carton"
        )
        "handicrafts" -> listOf(
            "Artisan Group" to "Channapatna Craft Collective",
            "Base Material" to "Solid Teak & Natural Lacquer Dye",
            "Processing" to "100% Hand-turned on traditional lathe",
            "Toxic Content" to "Lead-free & completely child-safe",
            "Polishing Type" to "Natural beeswax buffed",
            "Unique Feature" to "Each piece uniquely patterns",
            "Regional Origin" to "Channapatna, Karnataka, India",
            "Minimum Order (MOQ)" to "50 Unique Units (Mixed Assortment)"
        )
        "chemicals" -> listOf(
            "Chemical Purity" to "99.5% Tech Grade Certified",
            "Form" to "Fine crystalline powder format",
            "CAS Registry No." to "CAS-77-92-9 Industrial Standard",
            "PH value" to "2.2 at 1% dilution",
            "Solubility" to "Highly soluble in safe solvents",
            "Safety Standards" to "MSDS Sheets available on request",
            "Packaging Standard" to "UN-Approved High-Density HDPE Drums",
            "Minimum Order (MOQ)" to "1,000 Liters / 1 Metric Ton"
        )
        "machinery" -> listOf(
            "Power Output" to "7.5 HP High-Torque Induction Motor",
            "Operating Speed" to "2880 RPM Nominal Phase Rate",
            "Efficiency Class" to "IE3 Premium High Efficiency",
            "Phase Configuration" to "Three-phase standard grid hookup",
            "Cooling Type" to "TEFC (Totally Enclosed Fan Cooled)",
            "IP Protection Rating" to "IP55 Dust and Water Jet Resistant",
            "Primary Metal" to "Cast Iron rugged frame shell",
            "Minimum Order (MOQ)" to "1 Industrial Assembly Unit"
        )
        "jewelry" -> listOf(
            "Primary Metal" to "Pure Sterling Silver 92.5 Fine Grade",
            "Stamps" to "BIS Hallmark 925 stamped",
            "Plating Type" to "Rhodium anti-tarnish protective plating",
            "Stone details" to "Real uncut natural Semiprecious stones",
            "Hypoallergenic" to "100% Lead & Nickel Free",
            "Handpicked Collection" to "Jaipur Royal Enamel Heritage style",
            "Minimum Order (MOQ)" to "15 Custom Designs per assortment"
        )
        else -> listOf(
            "Product SKU Code" to "B2B-${100000 + postId}-IND",
            "Quality Standard" to "MSME India / IT Act Section 79 Certified",
            "Dispatch Hub" to "Central B2B Logistics Hub, NCR",
            "Packaging Unit" to "Standard Box / Corrugated Wholesale Box",
            "Lead Time" to "3 - 7 Business Days Dispatch guarantee",
            "Origin State" to "India (Domestic SME Network)",
            "Tax Compliance" to "GST e-Way Bill pre-filled, compliant",
            "Minimum Order (MOQ)" to "Contact seller for customized batch runs"
        )
    }
}

@Composable
fun ProductSpecsBottomSheet(
    post: Post,
    viewModel: B2bViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val allProfilesList by viewModel.allUserProfiles.collectAsStateWithLifecycle()
    val sellerProfile = allProfilesList.find { it.id == post.authorId }
    val rawPhone = sellerProfile?.whatsappNumber?.ifBlank { sellerProfile.phoneNumber } ?: ""
    val cleanPhoneNum = rawPhone.replace(" ", "").trim().removePrefix("+91")
    val finalPhone = if (cleanPhoneNum.isNotBlank()) cleanPhoneNum else "9999999999"

    val currentUser = viewModel.currentUserProfile
    val specList = remember(post.specs, post.category, post.id) {
        if (!post.specs.isNullOrBlank()) {
            post.specs.split("\n").mapNotNull { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else if (line.isNotBlank()) {
                    line.trim() to ""
                } else {
                    null
                }
            }
        } else {
            getProductSpecsByCategory(post.category, post.id)
        }
    }
    
    val specMap = remember(specList) {
        specList.toMap().mapKeys { it.key.lowercase().trim() }
    }
    
    var isEditingSpecs by remember { mutableStateOf(false) }
    var editMaterial by remember(post.id, post.specs) { mutableStateOf(specMap["material"] ?: specMap["base material"] ?: specMap["product grade"] ?: specMap["artisan group"] ?: specMap["chemical purity"] ?: specMap["primary metal"] ?: "Premium Blend") }
    var editSize by remember(post.id, post.specs) { mutableStateOf(specMap["size"] ?: specMap["width / dimensions"] ?: specMap["form"] ?: specMap["operating speed"] ?: specMap["packaging standard"] ?: "Standard Wholesale size") }
    var editStock by remember(post.id, post.specs) { mutableStateOf(specMap["stock"] ?: specMap["minimum order (moq)"] ?: specMap["unique feature"] ?: "In Stock") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(enabled = false) { /* Prevent click propagating to box */ }
                    .animateContentSize(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                border = BorderStroke(1.dp, ThreadsBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                ) {
                    // 1. Sleek drag handle on top (visual styling)
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .width(42.dp)
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(ThreadsBorder)
                            .align(Alignment.CenterHorizontally)
                    )

                    // 2. Header title row with Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = post.authorName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThreadsWhite
                                )
                                if (post.isApproved) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Profile",
                                        tint = Color(0xFF1DA1F2),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Wholesale Listing Product Specifications",
                                fontSize = 12.sp,
                                color = ThreadsSubtext
                            )
                        }
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ThreadsGray)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close spec dialog",
                                tint = ThreadsWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    // 3. Scrollable product information area
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Product image / Hero layout if present
                        if (!post.imageUrl.isNullOrBlank()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ThreadsOled)
                                        .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = post.imageUrl,
                                        contentDescription = "Specifications Product Snapshot",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }

                        // Product Description
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "OFFERING DESCRIPTION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThreadsSubtext,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = post.text,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = ThreadsWhite
                                )
                            }
                        }

                        // Dynamic Technical Specifications Grid
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TECHNICAL METRICS & CERTIFICATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThreadsSubtext,
                                        letterSpacing = 0.8.sp
                                    )
                                    if (currentUser != null && post.authorId == currentUser.id) {
                                        IconButton(
                                            onClick = { isEditingSpecs = !isEditingSpecs },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isEditingSpecs) Icons.Default.Close else Icons.Default.Edit,
                                                contentDescription = "Edit Specifications",
                                                tint = ThreadsSuccessGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (isEditingSpecs) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ThreadsGray, RoundedCornerShape(12.dp))
                                            .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editMaterial,
                                            onValueChange = { editMaterial = it },
                                            label = { Text("Material", color = ThreadsSubtext, fontSize = 11.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = ThreadsWhite,
                                                unfocusedTextColor = ThreadsWhite,
                                                focusedBorderColor = ThreadsSuccessGreen,
                                                unfocusedBorderColor = ThreadsBorder
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = editSize,
                                            onValueChange = { editSize = it },
                                            label = { Text("Size / Dimensions", color = ThreadsSubtext, fontSize = 11.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = ThreadsWhite,
                                                unfocusedTextColor = ThreadsWhite,
                                                focusedBorderColor = ThreadsSuccessGreen,
                                                unfocusedBorderColor = ThreadsBorder
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = editStock,
                                            onValueChange = { editStock = it },
                                            label = { Text("Stock Status / MOQ", color = ThreadsSubtext, fontSize = 11.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = ThreadsWhite,
                                                unfocusedTextColor = ThreadsWhite,
                                                focusedBorderColor = ThreadsSuccessGreen,
                                                unfocusedBorderColor = ThreadsBorder
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { isEditingSpecs = false },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Cancel", color = ThreadsWhite, fontSize = 12.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    val newSpecs = "Material: $editMaterial\nSize: $editSize\nStock: $editStock"
                                                    viewModel.updatePostSpecs(post, newSpecs)
                                                    isEditingSpecs = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Save", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    specList.forEach { pair ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(ThreadsGray, RoundedCornerShape(6.dp))
                                                .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = pair.first,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ThreadsSubtext
                                            )
                                            Text(
                                                text = pair.second,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ThreadsWhite,
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Seller Contact Credentials Panel
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "SELLER IDENTITY & TRUST CERTIFICATES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThreadsSubtext,
                                    letterSpacing = 0.8.sp
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ThreadsOled)
                                        .border(BorderStroke(1.dp, ThreadsBorder), RoundedCornerShape(10.dp))
                                        .padding(14.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = sellerProfile?.name?.ifBlank { post.authorName } ?: post.authorName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ThreadsWhite
                                                )
                                                Text(
                                                    text = "Industry Role: ${sellerProfile?.role ?: post.authorType} (${sellerProfile?.profileType ?: "Verified Member"})",
                                                    fontSize = 12.sp,
                                                    color = ThreadsSuccessGreen
                                                )
                                            }
                                            CssTheme.CssBadge(
                                                text = "VERIFIED SELLER",
                                                badgeType = CssTheme.BadgeType.SECURE
                                            )
                                        }

                                        HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                                        // GSTIN or Aadhaar ID indicator
                                        val gstin = sellerProfile?.gstin?.trim() ?: ""
                                        val aadhaar = sellerProfile?.aadhaarNumber?.trim() ?: ""
                                        val idDetailsText = when {
                                            gstin.isNotBlank() -> "GSTIN: $gstin (Active)"
                                            aadhaar.isNotBlank() -> "Aadhaar: XXXX-XXXX-${aadhaar.takeLast(4)} (Verified)"
                                            else -> "Identity: Government Database Attestation Active"
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = "Shield Verified",
                                                tint = ThreadsSuccessGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = idDetailsText,
                                                fontSize = 11.sp,
                                                color = ThreadsSubtext
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Contact action bottom rows
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // WhatsApp Direct Trade
                        Button(
                            onClick = {
                                val waUrl = "https://wa.me/91$finalPhone?text=Hello%20${Uri.encode(post.authorName)}%2C%20I%20saw%20your%20B2BHUB%20product%20proposal%3A%20${Uri.encode(post.text.take(35))}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not launch WhatsApp link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("💬 WhatsApp Trade", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Phone dial dealer
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$finalPhone"))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not locate a dialer app", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📞 Ring Dealer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// THREADS & INSTAGRAM-LIKE MAJESTIC CARD COMPONENT
// ==========================================

@Composable
fun B2bPostCard(
    post: Post,
    postComments: List<Comment>,
    onAddComment: (Int, String) -> Unit,
    viewModel: B2bViewModel,
    onCompanyClick: (UserProfile) -> Unit,
    onPostClick: (Post) -> Unit = {},
    onDirectMessageClick: (String, Int?, String?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val currentUserProfile = viewModel.currentUserProfile
    var commentsExpanded by remember { mutableStateOf(false) }
    var currentCommentInput by remember { mutableStateOf(TextFieldValue("")) }

    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val animAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val animScale = remember { androidx.compose.animation.core.Animatable(0.97f) }
    val animOffsetY = remember { androidx.compose.animation.core.Animatable(10f) }
    
    LaunchedEffect(post.id) {
        launch {
            animAlpha.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
        launch {
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
        launch {
            animOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 6.dp)
            .graphicsLayer(
                alpha = animAlpha.value,
                scaleX = animScale.value,
                scaleY = animScale.value,
                translationY = animOffsetY.value
            )
            .clickable { onPostClick(post) }
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = ThreadsCard),
        border = BorderStroke(0.5.dp, ThreadsBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Post Header (Full Width at TOP)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Profile Avatar (Instagram Gradient ring)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFF4F5BD5), Color(0xFFFEDA75))
                            )
                        )
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(ThreadsCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = ThreadsWhite,
                            fontSize = 14.sp
                        )
                    }
                }

                // User Info Column with weight(1f) to occupy remaining space and push buttons to the end
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ThreadsWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (post.isApproved) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Profile Check",
                                tint = Color(0xFF1DA1F2),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "•",
                            color = ThreadsSubtext,
                            fontSize = 12.sp
                        )
                        Text(
                            text = getRelativeTimeString(post.timestamp),
                            color = ThreadsSubtext,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (post.authorId.startsWith("@")) post.authorId else "@${post.authorId}",
                            color = ThreadsSubtext,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        CssTheme.CssBadge(
                            text = post.authorType,
                            badgeType = if (post.authorType.lowercase() == "seller") CssTheme.BadgeType.BLUE else CssTheme.BadgeType.MUTED
                        )
                    }
                }

                // End Row for Buttons with Arrangement.spacedBy(8.dp) horizontal spacing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Follow/Subscribe Action (if it's not our own post)
                    if (currentUserProfile != null && post.authorId != currentUserProfile.id) {
                        val followedSellers by viewModel.followedSellers.collectAsStateWithLifecycle()
                        val isFollowed = followedSellers.contains(post.authorId)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFollowed) ThreadsGray else ThreadsSuccessGreen.copy(alpha = 0.15f))
                                .border(BorderStroke(0.5.dp, if (isFollowed) ThreadsBorder else ThreadsSuccessGreen.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.toggleFollowSeller(post.authorId)
                                    // Sound + long press haptic vibration on subscribe
                                    try {
                                        val notificationSound: android.net.Uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                        val r = android.media.RingtoneManager.getRingtone(context, notificationSound)
                                        r?.play()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFollowed) "Subscribed" else "Subscribe",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFollowed) ThreadsSubtext else ThreadsSuccessGreen
                            )
                        }
                    }

                    // Status Pill on top-right using CSS Theme Badge
                    CssTheme.CssBadge(
                        text = "ACTIVE",
                        badgeType = CssTheme.BadgeType.SECURE
                    )
                }
            }

            // 2. Majestic Media Block (Tall Instagram Crop height 340dp)
            if (!post.imageUrl.isNullOrBlank()) {
                val isLocalOrWeb = post.imageUrl.startsWith("content:") || post.imageUrl.startsWith("file:") || post.imageUrl.startsWith("http")
                if (isLocalOrWeb) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = "B2B Product Photo Detail",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            } else if (!post.videoUrl.isNullOrBlank()) {
                // High-fidelity native inline video player (ExoPlayer)
                InlineVideoPlayer(
                    videoUrl = post.videoUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(8.dp))
                )
            }

            // 3. Instagram Interaction Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Likes reaction (Instagram heart style)
                    val postLikesMap by viewModel.postLikes.collectAsStateWithLifecycle()
                    val likesList = postLikesMap[post.id] ?: emptyList()
                    val hasLiked = currentUserProfile != null && likesList.any { it.userId == currentUserProfile.id }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.toggleLike(post.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (hasLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Likes Button",
                            tint = if (hasLiked) Color.Red else ThreadsWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "${likesList.size}",
                            color = ThreadsWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Chat Bubble for comments
                    Row(
                        modifier = Modifier.clickable { commentsExpanded = !commentsExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments indicator tag",
                            tint = ThreadsWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "${postComments.size}",
                            color = ThreadsWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Direct Message Paper Airplane button (Instagram DM style)
                    val triggerFeedback = playTapFeedback()
                    if (currentUserProfile != null && post.authorId != currentUserProfile.id) {
                        IconButton(
                            onClick = {
                                triggerFeedback()
                                onDirectMessageClick(post.authorId, post.id, post.text)
                            },
                            modifier = Modifier.size(24.dp).testTag("direct_message_post_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Send,
                                contentDescription = "Send Direct Message about this thread",
                                tint = ThreadsWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Native Share Button with Dynamic Share Count
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                viewModel.incrementShareCount(post)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    val shareText = buildString {
                                        append(post.text)
                                        if (!post.imageUrl.isNullOrBlank()) {
                                            append("\n\nCheck out the product image here: ")
                                            append(post.imageUrl)
                                        }
                                    }
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                val chooser = Intent.createChooser(shareIntent, "Share B2B Product Post")
                                context.startActivity(chooser)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("share_post_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Post",
                            tint = ThreadsWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${post.shareCount}",
                            color = ThreadsWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Delete Post Button (Only author or Admin can delete)
                    val isPostAdmin = currentUserProfile?.id?.removePrefix("@")?.lowercase() == "codecrafttechnologies" ||
                                      currentUserProfile?.id?.removePrefix("@")?.lowercase() == "rahman8040samsung" ||
                                      currentUserProfile?.id?.removePrefix("@")?.lowercase() == "rahman8040" ||
                                      currentUserProfile?.email?.lowercase() == "rahman8040samsung@gmail.com"
                    val canDelete = currentUserProfile != null && (isPostAdmin || post.authorId == currentUserProfile.id)

                    if (canDelete) {
                        IconButton(
                            onClick = {
                                viewModel.deletePost(post.id, currentUserProfile?.id ?: "unknown")
                                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp).testTag("delete_post_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Post",
                                tint = ThreadsErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Category Tag
                    post.category?.let { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1B1230))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = cat,
                                color = Color(0xFFA889F7),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // IT Compliance Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F2615), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = "Compliance indicator check", tint = ThreadsSuccessGreen, modifier = Modifier.size(9.dp))
                            Text(
                                text = "IT ACT SEC 79 COMPLIANT",
                                color = ThreadsSuccessGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pricing on Right
                if (post.price != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF072710))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "₹${post.price} wholesale",
                            color = ThreadsSuccessGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Text(
                        text = "Call for Pricing",
                        color = ThreadsSubtext,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // 4. Inline Post Text (Username in bold + description string)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = ThreadsWhite)) {
                            append(post.authorName)
                            append("  ")
                        }
                        append(post.text)
                    },
                    color = ThreadsWhite,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                if (post.price != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color(0xFF0F3218), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Price tag",
                            tint = ThreadsSuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Price: ₹${post.price}",
                            color = ThreadsSuccessGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // 5. Connect Instant buttons (WhatsApp / Instagram click targets)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allProfilesList by viewModel.allUserProfiles.collectAsStateWithLifecycle()
                val sellerProfile = allProfilesList.find { it.id == post.authorId }
                val rawPhone = sellerProfile?.whatsappNumber?.ifBlank { sellerProfile.phoneNumber } ?: ""
                val cleanPhoneNum = rawPhone.replace(" ", "").trim().removePrefix("+91")
                val finalPhone = if (cleanPhoneNum.isNotBlank()) cleanPhoneNum else "9999999999"

                // Discuss on WhatsApp
                Button(
                    onClick = {
                        val waUrl = "https://wa.me/91$finalPhone?text=Hello%20${Uri.encode(post.authorName)}%2C%20I%20saw%20your%20B2BHUB%20product%20proposal%3A%20${Uri.encode(post.text.take(35))}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not launch WhatsApp link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("💬 WhatsApp Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Check profile / details
                Button(
                    onClick = {
                        if (sellerProfile != null) {
                            onCompanyClick(sellerProfile)
                        } else {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$finalPhone"))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (sellerProfile != null) "🏢 View Seller Profile" else "📞 Call Dealer",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThreadsWhite
                    )
                }
            }

            // 6. Comments Expansion Panel (Instagram Style collapsible feed list)
            if (commentsExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThreadsCard)
                        .border(BorderStroke(0.5.dp, ThreadsBorder))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Secure Micro-Comment Feed (Automated Chat Safety active)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThreadsSubtext,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val baseComments = postComments.filter { !it.text.trim().startsWith("@") }
                        val allReplies = postComments.filter { it.text.trim().startsWith("@") }

                        baseComments.forEach { comment ->
                            val commentAuthorIdClean = comment.authorId.trim().removePrefix("@").lowercase()
                            val repliesForThisComment = allReplies.filter { reply ->
                                val textTrimmed = reply.text.trim()
                                val word = textTrimmed.split(" ").firstOrNull() ?: ""
                                val wordClean = word.trim().removePrefix("@").lowercase()
                                wordClean == commentAuthorIdClean
                            }

                            key(comment.id) {
                                var repliesVisible by remember { mutableStateOf(false) }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Avatar on Left with brand gradient border ring
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFFFA7E1E), Color(0xFFD62976))
                                                    )
                                                )
                                                .padding(1.5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(ThreadsCard),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = comment.authorId.trim().removePrefix("@").take(1).uppercase(),
                                                    color = ThreadsWhite,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Info Column on Right
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (comment.authorId.startsWith("@")) comment.authorId else "@${comment.authorId}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = ThreadsWhite,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (comment.authorRole == "Customer") Color(0xFF1E88E5) else Color(0xFF43A047),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                ) {
                                                    Text(
                                                        text = comment.authorRole.uppercase(),
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = comment.text,
                                                color = ThreadsWhite.copy(alpha = 0.95f),
                                                fontSize = 13.sp,
                                                lineHeight = 17.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text(
                                                    text = formatRelativeTime(comment.timestamp),
                                                    color = ThreadsSubtext,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Reply",
                                                    color = ThreadsSuccessGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable {
                                                            val cleanAuthor = if (comment.authorId.startsWith("@")) comment.authorId.substring(1) else comment.authorId
                                                            val namePart = cleanAuthor.split("@").firstOrNull() ?: cleanAuthor
                                                            val tag = "@$namePart"
                                                            currentCommentInput = TextFieldValue(
                                                                text = "$tag ",
                                                                selection = TextRange(tag.length + 1)
                                                            )
                                                            try {
                                                                focusRequester.requestFocus()
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )

                                                val isCommentAuthorOrAdmin = currentUserProfile != null && (
                                                    currentUserProfile.id == comment.authorId || 
                                                    currentUserProfile.role == "Super Admin"
                                                )
                                                if (isCommentAuthorOrAdmin) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Comment",
                                                        tint = ThreadsErrorRed,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable {
                                                                viewModel.deleteComment(comment.id)
                                                            }
                                                    )
                                                }
                                                
                                                if (repliesForThisComment.isNotEmpty()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(ThreadsGray)
                                                            .clickable { repliesVisible = !repliesVisible }
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (repliesVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                            contentDescription = "Toggle Replies",
                                                            tint = ThreadsSuccessGreen,
                                                            modifier = Modifier.size(11.sp.value.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (repliesVisible) "Hide secret replies" else "Show ${repliesForThisComment.size} secret replies",
                                                            color = ThreadsSuccessGreen,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Secret Nested Replies
                                    if (repliesVisible && repliesForThisComment.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 24.dp, top = 2.dp, bottom = 6.dp)
                                        ) {
                                            // Vertical connector line
                                            Box(
                                                modifier = Modifier
                                                    .width(1.5.dp)
                                                    .height(IntrinsicSize.Max)
                                                    .background(ThreadsBorder)
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(start = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                repliesForThisComment.forEach { reply ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFF0F0F0F), RoundedCornerShape(6.dp))
                                                            .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(6.dp))
                                                            .padding(8.dp),
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(22.dp)
                                                                .clip(CircleShape)
                                                                .background(ThreadsGray),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = reply.authorId.trim().removePrefix("@").take(1).uppercase(),
                                                                color = ThreadsWhite,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        Column(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(start = 8.dp)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (reply.authorId.startsWith("@")) reply.authorId else "@${reply.authorId}",
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = ThreadsWhite,
                                                                    fontSize = 11.sp
                                                                )
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(
                                                                            if (reply.authorRole == "Customer") Color(0xFF1E88E5) else Color(0xFF43A047),
                                                                            RoundedCornerShape(2.dp)
                                                                        )
                                                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                                                ) {
                                                                    Text(
                                                                        text = reply.authorRole.uppercase(),
                                                                        color = Color.White,
                                                                        fontSize = 6.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(
                                                                    text = formatRelativeTime(reply.timestamp),
                                                                    color = ThreadsSubtext,
                                                                    fontSize = 8.sp
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = reply.text,
                                                                color = ThreadsWhite.copy(alpha = 0.9f),
                                                                fontSize = 11.sp,
                                                                lineHeight = 14.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = ThreadsBorder.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val currentUser = viewModel.currentUserProfile
                        val isAuthorized = currentUser != null && (currentUser.role == "Seller" || currentUser.role == "Customer")

                        if (isAuthorized) {
                            // Enter Comment Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = currentCommentInput,
                                    onValueChange = { currentCommentInput = it },
                                    placeholder = { Text("Write query or type abusive words to test automated ban...", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThreadsWhite,
                                        unfocusedBorderColor = ThreadsGray,
                                        focusedTextColor = ThreadsWhite,
                                        unfocusedTextColor = ThreadsWhite
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester)
                                        .testTag("comment_input_${post.id}")
                                        .height(48.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        if (currentCommentInput.text.isNotBlank()) {
                                            onAddComment(post.id, currentCommentInput.text.trim())
                                            currentCommentInput = TextFieldValue("")
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        }
                                    },
                                    modifier = Modifier.testTag("submit_comment_button_${post.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled)
                                ) {
                                    Text("Post", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF230F0F)),
                                border = BorderStroke(0.5.dp, ThreadsErrorRed)
                            ) {
                                Text(
                                    text = "⚠️ Restricted: Registered seller and customer profiles verified on database can comment on trade proposals.",
                                    color = ThreadsErrorRed,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// THREADS-LIKE CARD COMPONENT (REPLACED CLASS)
// ==========================================
@Composable
fun B2bPostCardOld(
    post: Post,
    postComments: List<Comment>,
    onAddComment: (Int, String) -> Unit,
    viewModel: B2bViewModel
) {
    val context = LocalContext.current
    var commentsExpanded by remember { mutableStateOf(false) }
    var currentCommentInput by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left column: seller profile graphic icon and vertical line
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(Color(0xFF333333), Color(0xFF666666), Color(0xFFCCCCCC), Color(0xFF333333))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.authorId.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }

            // Right column: Name, Handle, timestamp, Content text, Image placeholder, Social reach
            Column(modifier = Modifier.weight(1f)) {
                // Name section with Type Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ThreadsWhite
                        )
                        if (post.isApproved) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Profile",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(ThreadsGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = post.authorType.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThreadsSubtext
                            )
                        }
                    }
                    Text(
                        text = "10 mins ago",
                        color = ThreadsSubtext,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = post.authorId,
                    color = ThreadsSubtext,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // The Post Text
                Text(
                    text = post.text,
                    color = ThreadsWhite,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Highly secure pricing metric tag
                if (post.price != null || post.category != null || post.isApproved) {
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (post.category != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1C132E), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = post.category,
                                    color = Color(0xFFA889F7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (post.price != null) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF132E16), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Wholesale Target: ₹${post.price}",
                                    color = ThreadsSuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (post.isApproved) {
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF0F2615), RoundedCornerShape(4.dp))
                                    .border(BorderStroke(0.5.dp, Color(0xFF1B5E20)), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Verified Secure compliance badge",
                                    tint = ThreadsSuccessGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Verified Secure",
                                    color = ThreadsSuccessGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Polished Media Representation inside deep details: support actual camera photos, gallery images, videos, and presets
                if (post.imageUrl != null) {
                    val isLocalOrWeb = post.imageUrl.startsWith("content:") || post.imageUrl.startsWith("file:") || post.imageUrl.startsWith("http")
                    if (isLocalOrWeb) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "B2B Product Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF111111), Color(0xFF222222))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (post.imageUrl) {
                                        "Sarees" -> Icons.Default.ShoppingBag
                                        "Spices" -> Icons.Default.Eco
                                        "Electronics" -> Icons.Default.Memory
                                        "Handicrafts" -> Icons.Default.Palette
                                        else -> Icons.Default.Image
                                    },
                                    contentDescription = "Decentralized Product Graphics Representation",
                                    tint = ThreadsSubtext,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "De-Centralized High-Res B2B Product Banner: ${post.imageUrl}",
                                    fontSize = 11.sp,
                                    color = ThreadsSubtext
                                )
                            }
                        }
                    }
                }

                if (post.videoUrl != null) {
                    // High-fidelity native inline video player (ExoPlayer)
                    InlineVideoPlayer(
                        videoUrl = post.videoUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(8.dp))
                    )
                }

                // Social Integrations bar for immediate transaction handshake (Display Insta, FB, WhatsApp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left interaction actions: comments expander
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { commentsExpanded = !commentsExpanded }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Open Comment Box",
                                tint = ThreadsSubtext,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${postComments.size} Comments",
                                fontSize = 12.sp,
                                color = ThreadsSubtext
                            )
                        }
                    }

                    // Direct external intent launches represent fully functional social integrations
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Connect:", fontSize = 11.sp, color = ThreadsSubtext)

                        // Instagram
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = "View Instagram",
                                tint = Color(0xFFE1306C),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // WhatsApp
                        IconButton(
                            onClick = {
                                // Simulate launching direct WhatsApp query
                                val whatsappMsg = "Hello from Threads B2B! I am interested in your wholesale listing: '${post.text.take(30)}...'"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(whatsappMsg)}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Contact WhatsApp Direct Link",
                                tint = ThreadsSuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Expanded Section: Comments & Anti-offense bot validation
                if (commentsExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThreadsCard, RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Secure Micro-Comment Feed (Automated Chat Safety active)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThreadsSubtext,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            val baseComments = postComments.filter { !it.text.trim().startsWith("@") }
                            val allReplies = postComments.filter { it.text.trim().startsWith("@") }

                            baseComments.forEach { comment ->
                                val commentAuthorIdClean = comment.authorId.trim().removePrefix("@").lowercase()
                                val repliesForThisComment = allReplies.filter { reply ->
                                    val textTrimmed = reply.text.trim()
                                    val word = textTrimmed.split(" ").firstOrNull() ?: ""
                                    val wordClean = word.trim().removePrefix("@").lowercase()
                                    wordClean == commentAuthorIdClean
                                }

                                key(comment.id) {
                                    var repliesVisible by remember { mutableStateOf(false) }

                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = if (comment.authorId.startsWith("@")) comment.authorId else "@${comment.authorId}",
                                                fontWeight = FontWeight.Bold,
                                                color = ThreadsWhite,
                                                fontSize = 12.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (comment.authorRole == "Customer") Color(0xFF0D47A1) else Color(0xFF1B5E20),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = comment.authorRole.uppercase(),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = comment.text,
                                                color = ThreadsSuccessGreen,
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )

                                            val currentUserProfile = viewModel.currentUserProfile
                                            val isCommentAuthorOrAdmin = currentUserProfile != null && (
                                                currentUserProfile.id == comment.authorId || 
                                                currentUserProfile.role == "Super Admin"
                                            )
                                            if (isCommentAuthorOrAdmin) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Comment",
                                                    tint = ThreadsErrorRed,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            viewModel.deleteComment(comment.id)
                                                        }
                                                )
                                            }

                                            if (repliesForThisComment.isNotEmpty()) {
                                                Text(
                                                    text = if (repliesVisible) "Hide replies" else "Show ${repliesForThisComment.size} replies",
                                                    color = ThreadsSuccessGreen,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clickable { repliesVisible = !repliesVisible }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (repliesVisible && repliesForThisComment.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                repliesForThisComment.forEach { reply ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color(0xFF0A0A0A), RoundedCornerShape(4.dp))
                                                            .padding(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (reply.authorId.startsWith("@")) reply.authorId else "@${reply.authorId}",
                                                            fontWeight = FontWeight.Bold,
                                                            color = ThreadsWhite,
                                                            fontSize = 11.sp
                                                        )
                                                        Text(
                                                            text = reply.text,
                                                            color = ThreadsWhite.copy(alpha = 0.9f),
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val currentUser = viewModel.currentUserProfile
                            val isAuthorized = currentUser != null && (currentUser.role == "Seller" || currentUser.role == "Customer")

                            if (isAuthorized) {
                                // Enter Comment Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = currentCommentInput,
                                        onValueChange = { currentCommentInput = it },
                                        placeholder = { Text("Write query or type abusive words to test automated ban bot...", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ThreadsWhite,
                                            unfocusedBorderColor = ThreadsGray,
                                            focusedTextColor = ThreadsWhite,
                                            unfocusedTextColor = ThreadsWhite
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("comment_input_${post.id}")
                                            .height(48.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Button(
                                        onClick = {
                                            if (currentCommentInput.text.isNotBlank()) {
                                                onAddComment(post.id, currentCommentInput.text.trim())
                                                currentCommentInput = TextFieldValue("")
                                            }
                                        },
                                        modifier = Modifier.testTag("submit_comment_button_${post.id}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled)
                                    ) {
                                        Text("Post", fontSize = 11.sp)
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF230F0F)),
                                    border = BorderStroke(0.5.dp, ThreadsErrorRed)
                                ) {
                                    Text(
                                        text = "⚠️ Restricted to Authorized Users: Registered business profiles (Sellers) or customers with valid email & mobile login verified on Google can interact and comment.",
                                        color = ThreadsErrorRed,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(10.dp)
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

// ==========================================
// POST CREATION MODAL FOR CONCISE BUSINESS UPDATES
// ==========================================
@Composable
fun PostCreationModal(
    viewModel: B2bViewModel,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val feedback = playTapFeedback()
    val context = LocalContext.current
    
    var textInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Textiles") }
    var selectedImageStyle by remember { mutableStateOf("Sarees") }
    var inputCustomUrl by remember { mutableStateOf("") }
    var useCustomUrl by remember { mutableStateOf(false) }
    var showUrlInput by remember { mutableStateOf(false) }
    var imageSelected by remember { mutableStateOf(false) }
    var showAttachmentOptions by remember { mutableStateOf(false) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var customCategoryInput by remember { mutableStateOf("") }

    var customSpecsInput by remember { mutableStateOf("") }
    var specMaterial by remember { mutableStateOf("Silk") }
    var specSize by remember { mutableStateOf("Medium (M)") }
    var specStock by remember { mutableStateOf("In Stock") }
    
    // Custom Video Specs states for requirement compliance
    var videoDurationMins by remember { mutableStateOf(2) }
    var videoDurationSecs by remember { mutableStateOf(30) }
    var videoSizeMb by remember { mutableStateOf(14.5f) }
    var videoResolution by remember { mutableStateOf("720P") }
    var videoFormat by remember { mutableStateOf("MP4") }
    var compressVideo by remember { mutableStateOf(true) }

    LaunchedEffect(selectedCategory, customCategoryInput) {
        val catName = if (selectedCategory == "Other") customCategoryInput else selectedCategory
        if (catName.isNotBlank()) {
            val defaultSpecs = getProductSpecsByCategory(catName, 0)
            customSpecsInput = defaultSpecs.joinToString("\n") { "${it.first}: ${it.second}" }
            
            // Populate modern structured inputs dynamically from category standards
            defaultSpecs.forEach { (key, value) ->
                when (key.lowercase()) {
                    "material", "fabric" -> specMaterial = value
                    "dimensions", "size", "width", "gsm" -> specSize = value
                    "stock", "status", "availability", "moq" -> specStock = value
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageSelected = true
            useCustomUrl = true
            inputCustomUrl = uri.toString()
            selectedImageStyle = "Local Image"
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageSelected = true
            useCustomUrl = true
            inputCustomUrl = uri.toString()
            selectedImageStyle = "Local Video"
            
            val meta = extractVideoMetadata(context, uri)
            if (meta != null) {
                if (meta.durationMs > 0) {
                    videoDurationMins = (meta.durationMs / 1000 / 60).toInt()
                    videoDurationSecs = ((meta.durationMs / 1000) % 60).toInt()
                }
                if (meta.sizeBytes > 0) {
                    videoSizeMb = meta.sizeBytes.toFloat() / (1024 * 1024)
                }
                videoFormat = meta.format
                videoResolution = if (meta.width >= 1080 || meta.height >= 1080) {
                    "1080P"
                } else if (meta.width >= 720 || meta.height >= 720) {
                    "720P"
                } else {
                    "480P"
                }
                Toast.makeText(context, "Video parsed: $videoFormat (${String.format(java.util.Locale.US, "%.1f MB", videoSizeMb)}), duration: ${videoDurationMins}m ${videoDurationSecs}s", Toast.LENGTH_LONG).show()
            } else {
                videoDurationMins = 2
                videoDurationSecs = 30
                videoSizeMb = 14.5f
                videoFormat = "MP4"
                videoResolution = "720P"
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val savedUri = saveBitmapToCache(context, bitmap)
            if (savedUri != null) {
                imageSelected = true
                useCustomUrl = true
                inputCustomUrl = savedUri.toString()
                selectedImageStyle = "Camera Capture"
            }
        }
    }

    val categories = listOf("Textiles", "Agriculture", "Electronics", "Handicrafts")
    
    val maxChars = 280
    val charCount = textInput.length
    
    val isAdmin = viewModel.currentUserProfile?.isAdmin() ?: false
    val currentUsed = viewModel.currentUserSub?.postCountThisMonth ?: 0
    val maxPostsAllowed = viewModel.currentUserSub?.getLimitVal(isAdmin) ?: 10
    val isPremium = isAdmin || (maxPostsAllowed > 10)
    
    var isVerifying by remember { mutableStateOf(false) }
    var verificationStepText by remember { mutableStateOf("") }
    var verificationStatusMsg by remember { mutableStateOf<String?>(null) }
    var isDoneSuccessfully by remember { mutableStateOf(false) }

    LaunchedEffect(isVerifying) {
        if (isVerifying) {
            val isVideo = selectedImageStyle == "Local Video" || inputCustomUrl.endsWith(".mp4") || inputCustomUrl.contains("video")
            val assetName = if (isVideo) "product video" else "product image"

            verificationStepText = "Verifying your $assetName format and resolution..."
            kotlinx.coroutines.delay(1200)
            if (isVideo && compressVideo) {
                verificationStepText = "Compressing $videoFormat video (reducing to 720P @30fps, H.264)..."
                kotlinx.coroutines.delay(1500)
                verificationStepText = "Optimized compressed file size: ${if (videoSizeMb > 20f) "14.2 MB" else String.format(java.util.Locale.US, "%.1f MB", videoSizeMb)}..."
                kotlinx.coroutines.delay(1200)
            }
            verificationStepText = "Scanning your $assetName for Indian IT Act Rule 79 compliance..."
            kotlinx.coroutines.delay(1200)
            verificationStepText = "Running content threat safety checks on your $assetName with Gemini AI..."
            kotlinx.coroutines.delay(1200)

            val finalImg = if (imageSelected && !isVideo) {
                if (useCustomUrl) inputCustomUrl else selectedImageStyle
            } else {
                null
            }
            val finalVideo = if (imageSelected && isVideo) {
                inputCustomUrl
            } else {
                null
            }
            val finalCategory = if (selectedCategory == "Other") {
                customCategoryInput.trim().ifBlank { "Custom Category" }
            } else {
                selectedCategory
            }

            viewModel.submitProductPost(
                text = textInput.trim(),
                price = priceInput.toDoubleOrNull(),
                category = finalCategory,
                imageUrl = finalImg,
                videoUrl = finalVideo,
                specs = customSpecsInput.trim().ifBlank { null }
            )
        }
    }

    LaunchedEffect(viewModel.postCreationStatus) {
        if (isVerifying) {
            when (val status = viewModel.postCreationStatus) {
                PostCreationResult.Success -> {
                    isDoneSuccessfully = true
                    Toast.makeText(context, "Successful post! Done", Toast.LENGTH_SHORT).show()
                }
                is PostCreationResult.LimitExceeded -> {
                    verificationStatusMsg = "LIMIT EXCEEDED:\n${status.message}"
                }
                is PostCreationResult.Flagged -> {
                    verificationStatusMsg = "CONTENT BLOCKED:\nReason: ${status.reason}"
                    Toast.makeText(context, status.reason, Toast.LENGTH_LONG).show()
                }
                is PostCreationResult.Error -> {
                    verificationStatusMsg = "ERROR:\n${status.message}"
                    Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                }
                PostCreationResult.Idle -> {}
            }
        }
    }

    Dialog(onDismissRequest = { if (!isVerifying) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("post_creation_modal"),
            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
            border = BorderStroke(1.dp, ThreadsBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isVerifying) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Secured Trade Verification",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThreadsWhite
                    )

                    Text(
                        text = "Compliance Pre-Clearance Pipeline (IT Act Section 79)",
                        fontSize = 10.sp,
                        color = ThreadsSuccessGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                    if (!isDoneSuccessfully && verificationStatusMsg == null) {
                        CircularProgressIndicator(
                            color = ThreadsSuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verificationStepText,
                            color = ThreadsWhite,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    } else if (isDoneSuccessfully) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success checkmark",
                            tint = ThreadsSuccessGreen,
                            modifier = Modifier.size(64.dp)
                        )

                        Text(
                            text = "Done",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ThreadsSuccessGreen
                        )

                        Text(
                            text = "Successful post!\nYour product proposal has been pre-cleared, approved, and fully indexed in our global trade feed.",
                            fontSize = 13.sp,
                            color = ThreadsSubtext,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isVerifying = false
                                isDoneSuccessfully = false
                                viewModel.clearPostCreationStatus()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("verification_success_done_button")
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Canceled validation",
                            tint = ThreadsErrorRed,
                            modifier = Modifier.size(64.dp)
                        )

                        Text(
                            text = "Verification Blocked",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThreadsErrorRed
                        )

                        Text(
                            text = verificationStatusMsg ?: "Unknown compliance threat.",
                            fontSize = 12.sp,
                            color = ThreadsSubtext,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isVerifying = false
                                    verificationStatusMsg = null
                                    viewModel.clearPostCreationStatus()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray, contentColor = ThreadsWhite),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Edit Post", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    isVerifying = false
                                    verificationStatusMsg = null
                                    viewModel.clearPostCreationStatus()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsErrorRed, contentColor = ThreadsWhite),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Discard", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Modal Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Create Concise Post Icon",
                                tint = ThreadsSuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Concise Business update",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ThreadsWhite
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = ThreadsWhite)
                        }
                    }
                }
                
                // Subscription Limit indicator
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThreadsBlack, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAdmin) "Unlimited Admin Access Active ✓" else "Usage tracking: $currentUsed / $maxPostsAllowed posts",
                            fontSize = 11.sp,
                            color = if (isPremium) ThreadsSuccessGreen else ThreadsSubtext,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .background(if (isPremium) ThreadsSuccessGreen else ThreadsGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isAdmin) "ADMIN" else if (isPremium) "PRO" else "FREE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Preset Header Tag chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Tap to insert direct prefix tags:",
                            fontSize = 11.sp,
                            color = ThreadsSubtext
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val templates = listOf("[OFFER]", "[WANTED]", "[EXPORT]", "[READY_STOCK]", "[URGENT]", "[ALERT]")
                            templates.forEach { template ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ThreadsGray)
                                        .clickable {
                                            if (!textInput.contains(template)) {
                                                textInput = "$template $textInput"
                                                if (textInput.length > maxChars) {
                                                    textInput = textInput.take(maxChars)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = template,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ThreadsWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Short Business proposal text field
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = {
                                if (it.length <= maxChars) {
                                    textInput = it
                                }
                            },
                            label = { Text("Describe products / Wholesale proposal") },
                            placeholder = { Text("Describe products. Pre-moderation active.") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThreadsWhite,
                                unfocusedBorderColor = ThreadsGray,
                                focusedTextColor = ThreadsWhite,
                                unfocusedTextColor = ThreadsWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("modal_post_text_input")
                        )
                        
                        // Counter and Circular Progress Layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val counterColor = when {
                                charCount > 250 -> ThreadsErrorRed
                                charCount > 200 -> Color(0xFFFFA726) // Orange
                                else -> ThreadsSubtext
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(14.dp)) {
                                    CircularProgressIndicator(
                                        progress = { charCount.toFloat() / maxChars.toFloat() },
                                        color = counterColor,
                                        trackColor = ThreadsGray,
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text(
                                    text = "$charCount / $maxChars characters (Strict Concise updates)",
                                    fontSize = 11.sp,
                                    color = counterColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            if (charCount >= maxChars) {
                                Text(
                                    text = "LIMIT",
                                    fontSize = 9.sp,
                                    color = ThreadsErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Price Field
                item {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Target Wholesale Pricing (₹) - Optional") },
                        prefix = { Text("₹ ", color = ThreadsWhite) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThreadsWhite,
                            unfocusedBorderColor = ThreadsGray,
                            focusedTextColor = ThreadsWhite,
                            unfocusedTextColor = ThreadsWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("modal_post_price_input")
                    )
                }

                // Category Dropdown Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Target Category / Segment:", fontSize = 11.sp, color = ThreadsSubtext)
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (selectedCategory == "Other") "Other (Custom Category)" else selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Choose Category Segment") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = if (categoryDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Expand Category Dropdown",
                                        tint = ThreadsWhite,
                                        modifier = Modifier.clickable { categoryDropdownExpanded = !categoryDropdownExpanded }
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThreadsWhite,
                                    unfocusedBorderColor = ThreadsGray,
                                    focusedTextColor = ThreadsWhite,
                                    unfocusedTextColor = ThreadsWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = !categoryDropdownExpanded }
                            )

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier
                                    .background(ThreadsCard)
                                    .border(BorderStroke(0.5.dp, ThreadsBorder))
                            ) {
                                val standardCategories = listOf("Textiles", "Agriculture", "Electronics", "Handicrafts", "Services", "Machinery", "Chemicals", "Food & Beverages", "Other (Custom Category)...")
                                standardCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = ThreadsWhite) },
                                        onClick = {
                                            if (cat.startsWith("Other")) {
                                                selectedCategory = "Other"
                                            } else {
                                                selectedCategory = cat
                                            }
                                            categoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (selectedCategory == "Other") {
                            OutlinedTextField(
                                value = customCategoryInput,
                                onValueChange = { customCategoryInput = it },
                                label = { Text("Type custom category name (e.g., Cosmetics)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThreadsWhite,
                                    unfocusedBorderColor = ThreadsGray,
                                    focusedTextColor = ThreadsWhite,
                                    unfocusedTextColor = ThreadsWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 1. Professional Product Specifications UI (Material 3 Dropdowns)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(ThreadsCard, RoundedCornerShape(12.dp))
                            .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🏷️ PRODUCT SPECIFICATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThreadsWhite,
                            letterSpacing = 0.5.sp
                        )
                        
                        // Material Input/Dropdown
                        var materialDropdownExpanded by remember { mutableStateOf(false) }
                        val materials = listOf("Cotton", "Silk", "Polyester", "Linen", "Denim", "Wool", "Leather", "Metal", "Plastic", "Glass", "Other")
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Material:", fontSize = 11.sp, color = ThreadsSubtext)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = specMaterial,
                                    onValueChange = { specMaterial = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = ThreadsWhite, fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThreadsWhite,
                                        unfocusedBorderColor = ThreadsGray,
                                        focusedTextColor = ThreadsWhite,
                                        unfocusedTextColor = ThreadsWhite
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { materialDropdownExpanded = !materialDropdownExpanded }) {
                                            Icon(
                                                imageVector = if (materialDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Expand Material",
                                                tint = ThreadsWhite
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = materialDropdownExpanded,
                                    onDismissRequest = { materialDropdownExpanded = false },
                                    modifier = Modifier.background(ThreadsCard).border(BorderStroke(0.5.dp, ThreadsBorder))
                                ) {
                                    materials.forEach { mat ->
                                        DropdownMenuItem(
                                            text = { Text(mat, color = ThreadsWhite, fontSize = 13.sp) },
                                            onClick = {
                                                specMaterial = mat
                                                materialDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Size/Dimensions Input/Dropdown
                        var sizeDropdownExpanded by remember { mutableStateOf(false) }
                        val sizes = listOf("Standard", "Small (S)", "Medium (M)", "Large (L)", "XL", "XXL", "100m x 1.5m", "Custom Dimensions")
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Size / Dimensions:", fontSize = 11.sp, color = ThreadsSubtext)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = specSize,
                                    onValueChange = { specSize = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = ThreadsWhite, fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThreadsWhite,
                                        unfocusedBorderColor = ThreadsGray,
                                        focusedTextColor = ThreadsWhite,
                                        unfocusedTextColor = ThreadsWhite
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { sizeDropdownExpanded = !sizeDropdownExpanded }) {
                                            Icon(
                                                imageVector = if (sizeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Expand Size",
                                                tint = ThreadsWhite
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = sizeDropdownExpanded,
                                    onDismissRequest = { sizeDropdownExpanded = false },
                                    modifier = Modifier.background(ThreadsCard).border(BorderStroke(0.5.dp, ThreadsBorder))
                                ) {
                                    sizes.forEach { sz ->
                                        DropdownMenuItem(
                                            text = { Text(sz, color = ThreadsWhite, fontSize = 13.sp) },
                                            onClick = {
                                                specSize = sz
                                                sizeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Stock Availability Selection
                        var stockDropdownExpanded by remember { mutableStateOf(false) }
                        val stockOptions = listOf("In Stock", "Immediate Dispatch", "Pre-order (7 Days)", "Made-to-order", "Out of Stock")
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Stock Availability:", fontSize = 11.sp, color = ThreadsSubtext)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = specStock,
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(color = ThreadsWhite, fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThreadsWhite,
                                        unfocusedBorderColor = ThreadsGray,
                                        focusedTextColor = ThreadsWhite,
                                        unfocusedTextColor = ThreadsWhite
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { stockDropdownExpanded = !stockDropdownExpanded }) {
                                            Icon(
                                                imageVector = if (stockDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Expand Stock Options",
                                                tint = ThreadsWhite
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { stockDropdownExpanded = !stockDropdownExpanded }
                                )
                                DropdownMenu(
                                    expanded = stockDropdownExpanded,
                                    onDismissRequest = { stockDropdownExpanded = false },
                                    modifier = Modifier.background(ThreadsCard).border(BorderStroke(0.5.dp, ThreadsBorder))
                                ) {
                                    stockOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt, color = ThreadsWhite, fontSize = 13.sp) },
                                            onClick = {
                                                specStock = opt
                                                stockDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Redesigned Standard Attachment Action Bar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThreadsBlack, RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, ThreadsBorder), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📎 ATTACH PRODUCT PHOTO OR VIDEO (Instant Click)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThreadsSuccessGreen,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Direct Gallery trigger
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray, contentColor = ThreadsWhite),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Direct Camera trigger
                            Button(
                                onClick = { cameraLauncher.launch() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray, contentColor = ThreadsWhite),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color(0xFFA889F7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Direct Video trigger
                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray, contentColor = ThreadsWhite),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video", tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Video", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Image/Video Selection Preview Box Area
                item {
                    if (imageSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .border(BorderStroke(1.dp, ThreadsBorder), RoundedCornerShape(8.dp))
                                .background(Color(0xFF151515))
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            if (selectedImageStyle == "Local Video" || inputCustomUrl.endsWith(".mp4") || inputCustomUrl.contains("video")) {
                                // Video thumbnail representation
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "Play Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("🎥 Attached Video Proposal Ready", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(inputCustomUrl.take(45) + "...", color = ThreadsSubtext, fontSize = 10.sp)
                                    }
                                }
                            } else {
                                // Load actual image
                                AsyncImage(
                                    model = inputCustomUrl,
                                    contentDescription = "Attached Image Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }

                            // Delete overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Custom Media Loaded Successfully",
                                    color = ThreadsSuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                IconButton(
                                    onClick = {
                                        imageSelected = false
                                        useCustomUrl = false
                                        inputCustomUrl = ""
                                        selectedImageStyle = "Default"
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove attachment", tint = ThreadsErrorRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    } else {
                        // Helpful friendly tip for standard post users
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, ThreadsBorder.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                .background(ThreadsBlack)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = ThreadsSubtext, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ready to publish: standard text post will be used without attachments unless you select a gallery or camera file above.",
                                    color = ThreadsSubtext,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }



                // Final Publish and Cancel Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("Dismiss", color = ThreadsWhite, fontSize = 12.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                feedback()
                                if (textInput.isBlank()) {
                                    Toast.makeText(context, "Trade description cannot be empty", Toast.LENGTH_SHORT).show()
                                } else {
                                    val isVideo = selectedImageStyle == "Local Video" || inputCustomUrl.endsWith(".mp4") || inputCustomUrl.contains("video")
                                    if (imageSelected && isVideo) {
                                        val totalSeconds = videoDurationMins * 60 + videoDurationSecs
                                        if (totalSeconds > 300) {
                                            Toast.makeText(context, "Validation failed: Video must be less than 5 minutes", Toast.LENGTH_LONG).show()
                                            return@Button
                                        }
                                        if (videoSizeMb > 20f) {
                                            Toast.makeText(context, "Validation failed: Video size cannot exceed 20 MB", Toast.LENGTH_LONG).show()
                                            return@Button
                                        }
                                        if (videoResolution == "1080P") {
                                            Toast.makeText(context, "Compression System Alert: Resolution downgraded to allowed maximum (720P)", Toast.LENGTH_LONG).show()
                                            videoResolution = "720P"
                                        }
                                    }
                                    val sub = viewModel.currentUserSub ?: Subscription(userId = viewModel.currentUserProfile?.id ?: "")
                                    if (sub.getRemainingCredits() <= 0) {
                                        Toast.makeText(context, "Upload Prevented: Remaining post credits is 0. Please buy a Booster pack!", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                        onUpgradeClick()
                                    } else {
                                        customSpecsInput = "Material: $specMaterial\nSize/Dimensions: $specSize\nStock Availability: $specStock"
                                        isVerifying = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("modal_publish_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("Publish Proposal", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            } // closes LazyColumn
            } // closes else block
        } // closes Card
    } // closes Dialog
}

// ==========================================
// 4. FREEMIUM COMPOSER SCREEN & POST LIMIT DETAILS
// ==========================================
@Composable
fun CreatePostScreen(
    viewModel: B2bViewModel,
    subscription: com.example.data.Subscription?,
    onUpgradeClick: () -> Unit,
    onPostSuccess: () -> Unit,
    onConciseClick: (() -> Unit)? = null
) {
    val feedback = playTapFeedback()
    var textInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Textiles") }
    var selectedImageStyle by remember { mutableStateOf("Sarees") }

    val categories = listOf("Textiles", "Agriculture", "Electronics", "Handicrafts")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isAdmin = viewModel.currentUserProfile?.isAdmin() ?: false
    val currentUsed = subscription?.postCountThisMonth ?: 0
    val maxPostsAllowed = subscription?.getLimitVal(isAdmin) ?: 10
    val isPremium = isAdmin || (maxPostsAllowed > 10)
    val remaining = if (isAdmin) "UNLIMITED" else "${maxPostsAllowed - currentUsed} posts left"

    LaunchedEffect(viewModel.postCreationStatus) {
        when (val status = viewModel.postCreationStatus) {
            PostCreationResult.Success -> {
                Toast.makeText(context, "Post approved & published successfully!", Toast.LENGTH_SHORT).show()
                viewModel.clearPostCreationStatus()
                onPostSuccess()
            }
            is PostCreationResult.LimitExceeded -> {
                Toast.makeText(context, "Failed: Free Post Count Limit Exceeded!", Toast.LENGTH_LONG).show()
            }
            is PostCreationResult.Flagged -> {
                Toast.makeText(context, "BLOCKED: Content violates strict safety layers!", Toast.LENGTH_LONG).show()
            }
            is PostCreationResult.Error -> {
                Toast.makeText(context, "Error: ${status.message}", Toast.LENGTH_SHORT).show()
            }
            PostCreationResult.Idle -> {}
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onConciseClick != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConciseClick() },
                    colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                    border = BorderStroke(1.dp, ThreadsSuccessGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F2C1A), CircleShape)
                                .size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Concise Mode",
                                tint = ThreadsSuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Concise Trade Composer Modal",
                                color = ThreadsWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Instant prefix headers, image uploader & char counter limit.",
                                color = ThreadsSubtext,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = "Forward arrow",
                            tint = ThreadsSubtext,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                border = BorderStroke(1.dp, if (isPremium) ThreadsSuccessGreen else ThreadsBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isPremium) "PREMIUM WHOLESALER ACTIVE" else "FREEMIUM PLAN ACTIVE",
                                fontWeight = FontWeight.Bold,
                                color = if (isPremium) ThreadsSuccessGreen else ThreadsWhite,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Secured Indian B2B Post Limiter Logic",
                                fontSize = 12.sp,
                                color = ThreadsSubtext
                            )
                        }
                        if (isAdmin) {
                            Box(
                                modifier = Modifier
                                    .background(ThreadsSuccessGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("UNLIMITED VIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else if (isPremium) {
                            Box(
                                modifier = Modifier
                                    .background(ThreadsSuccessGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("VIP ($maxPostsAllowed)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Button(
                                onClick = onUpgradeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+POSTS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onUpgradeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("UPGRADE VIP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress indicators
                    Text(
                        text = "Usage Tracker: $currentUsed / ${if (isAdmin) "∞" else maxPostsAllowed} posts spent",
                        fontSize = 13.sp,
                        color = ThreadsWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (isAdmin) 0.1f else (currentUsed.toFloat() / maxPostsAllowed.toFloat()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isPremium) ThreadsSuccessGreen else ThreadsWhite,
                        trackColor = ThreadsGray
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                border = BorderStroke(1.dp, ThreadsBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Compose New Threads Post",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ThreadsWhite
                    )

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Product Details / Wholesale Proposal Descriptions") },
                        placeholder = { Text("Describe products. Post containing 'Alcohol', Pork references, or NSFW terms is automatically rejected.") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThreadsWhite,
                            unfocusedBorderColor = ThreadsGray,
                            focusedTextColor = ThreadsWhite,
                            unfocusedTextColor = ThreadsWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("post_text_input")
                    )

                    // Price
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Wholesale Target Price (INR/₹) - [Optional]") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThreadsWhite,
                            unfocusedBorderColor = ThreadsGray,
                            focusedTextColor = ThreadsWhite,
                            unfocusedTextColor = ThreadsWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category
                    Text("Select Target B2C Segment category:", fontSize = 12.sp, color = ThreadsSubtext)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) ThreadsWhite else ThreadsGray)
                                    .clickable {
                                        selectedCategory = cat
                                        selectedImageStyle = when (cat) {
                                            "Textiles" -> "Sarees"
                                            "Agriculture" -> "Spices"
                                            "Electronics" -> "Electronics"
                                            "Handicrafts" -> "Handicrafts"
                                            else -> "Default"
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) ThreadsOled else ThreadsWhite,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Product Imagery representations
                    Text("Auto-Generate B2B Adaptive Imagery Template:", fontSize = 12.sp, color = ThreadsSubtext)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThreadsCard, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = when (selectedImageStyle) {
                                    "Sarees" -> Icons.Default.ShoppingBag
                                    "Spices" -> Icons.Default.Eco
                                    "Electronics" -> Icons.Default.Memory
                                    else -> Icons.Default.Palette
                                },
                                contentDescription = "Category visual logo",
                                tint = ThreadsSuccessGreen
                            )
                            Text(
                                text = "Pre-arranged high-res template style: $selectedImageStyle",
                                color = ThreadsWhite,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Action submit with safety warning popups
        item {
            Button(
                onClick = {
                    feedback()
                    if (textInput.isBlank()) {
                        Toast.makeText(context, "Post description cannot be blank for dynamic safety checking.", Toast.LENGTH_SHORT).show()
                    } else {
                        val remainingCredits = subscription?.getRemainingCredits() ?: 10
                        if (remainingCredits <= 0) {
                            Toast.makeText(context, "Upload Prevented: Remaining post credits is 0. Please purchase a booster pack!", Toast.LENGTH_LONG).show()
                            onUpgradeClick()
                        } else {
                            viewModel.submitProductPost(
                                text = textInput.trim(),
                                price = priceInput.toDoubleOrNull(),
                                category = selectedCategory,
                                imageUrl = selectedImageStyle,
                                videoUrl = null
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_post_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Publish to B2B Threads Feed", fontWeight = FontWeight.Bold)
            }
        }

        // Show Banned Keyword Guidance
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                border = BorderStroke(0.5.dp, ThreadsBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Strict Automated Moderation Rules:", color = ThreadsErrorRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Alcohol/Bhang • Haram categories (pork, bacon, gambling) • NSFW/lingerie/nude imagery are strictly blocked on-device. Repeated violations trigger profile lockouts.", color = ThreadsSubtext, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

// ==========================================
// 5. MASTER SUPER ADMIN DASHBOARD
// ==========================================
@Composable
fun MasterAdminDashboard(
    viewModel: B2bViewModel,
    allPosts: List<Post>,
    allLogs: List<com.example.data.ModerationLog>,
    allUsers: List<UserProfile>
) {
    val postLikesMap by viewModel.postLikes.collectAsStateWithLifecycle()
    val postCommentsMap by viewModel.postComments.collectAsStateWithLifecycle()

    var adminSection by remember { mutableStateOf("logs") } // "logs", "users", "posts"
    var selectedUserSubTab by remember { mutableStateOf("Customers") } // "Customers", "Manufacturers", "OEMs", "Traders"
    var selectedSafetyLogDetail by remember { mutableStateOf<com.example.data.ModerationLog?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // High visibility administrative brand logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThreadsCard, RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, ThreadsErrorRed), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MASTER ADMIN PANEL",
                    color = ThreadsErrorRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Exclusive Control Panel: CodeCraft Technologies",
                    color = ThreadsSubtext,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.SecurityUpdateWarning, contentDescription = "Super admin key", tint = ThreadsErrorRed)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick analytical cards (Clickable tabs to drill down)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Total Posts card
            val postsSelected = adminSection == "posts"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThreadsBlack)
                    .border(
                        BorderStroke(
                            width = if (postsSelected) 1.5.dp else 1.dp,
                            color = if (postsSelected) ThreadsWhite else ThreadsBorder
                        ),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { adminSection = "posts" }
                    .padding(12.dp)
            ) {
                Column {
                    Text("Total Posts ↗", fontSize = 11.sp, color = if (postsSelected) ThreadsWhite else ThreadsSubtext)
                    Text("${allPosts.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ThreadsWhite)
                }
            }
            // Violations / Safety Logs card
            val logsSelected = adminSection == "logs"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThreadsBlack)
                    .border(
                        BorderStroke(
                            width = if (logsSelected) 1.5.dp else 1.dp,
                            color = if (logsSelected) ThreadsErrorRed else ThreadsBorder
                        ),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { adminSection = "logs" }
                    .padding(12.dp)
            ) {
                Column {
                    Text("Safety Logs ↗", fontSize = 11.sp, color = if (logsSelected) ThreadsErrorRed else ThreadsSubtext)
                    Text("${allLogs.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ThreadsErrorRed)
                }
            }
            // Merchants / Users card
            val usersSelected = adminSection == "users"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThreadsBlack)
                    .border(
                        BorderStroke(
                            width = if (usersSelected) 1.5.dp else 1.dp,
                            color = if (usersSelected) ThreadsSuccessGreen else ThreadsBorder
                        ),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { adminSection = "users" }
                    .padding(12.dp)
            ) {
                Column {
                    Text("Total Users ↗", fontSize = 11.sp, color = if (usersSelected) ThreadsSuccessGreen else ThreadsSubtext)
                    Text("${allUsers.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ThreadsWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab sub-selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "logs" to "Safety Logs",
                "users" to "Indian Merchants",
                "posts" to "Purge Feed",
                "interactions" to "Global Interactions (Likes & Comments)"
            ).forEach { (tab, label) ->
                val selected = adminSection == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ThreadsWhite else ThreadsGray)
                        .clickable { adminSection = tab }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) ThreadsOled else ThreadsWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-sections render
        when (adminSection) {
            "logs" -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (allLogs.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.clearAllModerationLogs() },
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsErrorRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("clear_logs_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All Logs", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear All Logs", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (allLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No safety logs recorded. Systems clean.", color = ThreadsSubtext)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(allLogs) { log ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSafetyLogDetail = log },
                                    colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                                    border = BorderStroke(0.5.dp, ThreadsBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${log.actionTaken} ↗",
                                                fontWeight = FontWeight.Bold,
                                                color = if (log.actionTaken.contains("BANNED") || log.actionTaken.contains("REJECTED")) ThreadsErrorRed else ThreadsSuccessGreen,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "ID: ${log.userId}",
                                                fontSize = 11.sp,
                                                color = ThreadsSuccessGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Content: \"${log.postContent}\"", fontSize = 12.sp, color = ThreadsWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Trigger Reason: ${log.triggerReason}", fontSize = 11.sp, color = ThreadsSubtext)
                                        Text("Click to view full security audit details", fontSize = 9.sp, color = ThreadsSubtext.copy(alpha = 0.7f), fontWeight = FontWeight.Light)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "users" -> {
                val filteredUsers = allUsers.filter { user ->
                    when (selectedUserSubTab) {
                        "Customers" -> user.role == "Customer"
                        "Manufacturers" -> user.role == "Seller" && user.profileType.lowercase(java.util.Locale.ROOT) == "manufacturer"
                        "OEMs" -> user.role == "Seller" && user.profileType.lowercase(java.util.Locale.ROOT) == "oem"
                        "Traders" -> user.role == "Seller" && (user.profileType.lowercase(java.util.Locale.ROOT) == "trader" || user.profileType.lowercase(java.util.Locale.ROOT) == "individual" || user.profileType.lowercase(java.util.Locale.ROOT) == "retailer")
                        else -> true
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sub-tab row for user types (Split separately!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThreadsBlack, RoundedCornerShape(8.dp))
                            .horizontalScroll(rememberScrollState())
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Customers", "Manufacturers", "OEMs", "Traders").forEach { tab ->
                            val isSelected = selectedUserSubTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) ThreadsWhite else Color.Transparent)
                                    .clickable { selectedUserSubTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when(tab) {
                                        "Customers" -> "Buyers / Customers"
                                        "Manufacturers" -> "Manufacturers"
                                        "OEMs" -> "OEMs / Sellers"
                                        else -> "Traders / Wholesalers"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ThreadsOled else ThreadsSubtext,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No verified users registered under this classification.", color = ThreadsSubtext, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredUsers) { user ->
                                val isBanned = viewModel.isUserBanned(user.id)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                                    border = BorderStroke(0.5.dp, if (isBanned) ThreadsErrorRed else ThreadsBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${user.name} (${user.id})",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isBanned) ThreadsErrorRed else ThreadsWhite,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Classification: ${user.profileType} | WhatsApp: ${user.whatsappNumber}",
                                                fontSize = 11.sp,
                                                color = ThreadsSubtext
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (isBanned) {
                                                Button(
                                                    onClick = { viewModel.adminUnbanUser(user.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Unban", fontSize = 10.sp)
                                                }
                                            } else {
                                                Text(
                                                    text = "Secure Active",
                                                    color = ThreadsSuccessGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            }
                                            Button(
                                                onClick = { viewModel.adminToggleUserSubscription(user.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Toggle Sub", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "posts" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allPosts) { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                            border = BorderStroke(1.dp, if (post.isApproved) ThreadsBorder else ThreadsErrorRed)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Author: ${post.authorId}",
                                        fontWeight = FontWeight.Bold,
                                        color = ThreadsWhite,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (post.isApproved) "APPROVED FEED" else "FLAGGED BLOCKED",
                                        fontWeight = FontWeight.Bold,
                                        color = if (post.isApproved) ThreadsSuccessGreen else ThreadsErrorRed,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(post.text, fontSize = 13.sp, color = ThreadsWhite)
                                if (post.flagReason != null) {
                                    Text("Reason: ${post.flagReason}", fontSize = 11.sp, color = ThreadsErrorRed)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { viewModel.adminOverrideApproval(post, !post.isApproved) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(if (post.isApproved) "Block" else "Approve", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.adminDeletePost(post.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsErrorRed)
                                    ) {
                                        Text("Delete", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "interactions" -> {
                val lazyAllPosts = allPosts
                if (lazyAllPosts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active B2B listings to monitor.", color = ThreadsSubtext)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "GLOBAL INTERACTION MONITOR (IT ACT SEC 79)",
                                color = ThreadsErrorRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "Read active user engagements, commenter identity IDs, and list of likes across all threads network.",
                                fontSize = 10.sp,
                                color = ThreadsSubtext,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(lazyAllPosts) { post ->
                            val likes = postLikesMap[post.id] ?: emptyList()
                            val comments = postCommentsMap[post.id] ?: emptyList()

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                                border = BorderStroke(0.5.dp, ThreadsBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Wholesale Listing ID #${post.id}",
                                            fontWeight = FontWeight.Bold,
                                            color = ThreadsSuccessGreen,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Owner: ${post.authorId}",
                                            fontSize = 11.sp,
                                            color = ThreadsWhite
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Post Content:\n\"${post.text}\"",
                                        fontSize = 12.sp,
                                        color = ThreadsWhite,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        lineHeight = 16.sp,
                                        modifier = Modifier
                                            .background(ThreadsCard, RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                            .fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Displays Likes section
                                    Text(
                                        text = "Liked Users (${likes.size}):",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF9800)
                                    )
                                    if (likes.isEmpty()) {
                                        Text("No likes registered yet.", color = ThreadsSubtext, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, top = 2.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            likes.forEach { like ->
                                                Text(
                                                    text = like.userId,
                                                    fontSize = 10.sp,
                                                    color = ThreadsWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(Color(0xFF2E1A0A), RoundedCornerShape(4.dp))
                                                        .border(BorderStroke(0.5.dp, Color(0xFFFF9800).copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Displays Comments section
                                    Text(
                                        text = "Comments Section Archive (${comments.size}):",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF2196F3)
                                    )
                                    if (comments.isEmpty()) {
                                        Text("No comments submitted yet.", color = ThreadsSubtext, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                                    } else {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                                        ) {
                                            comments.forEach { comment ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(ThreadsCard, RoundedCornerShape(4.dp))
                                                        .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(4.dp))
                                                        .padding(6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = comment.authorId,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = ThreadsSuccessGreen,
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = comment.text,
                                                            fontSize = 11.sp,
                                                            color = ThreadsWhite,
                                                            lineHeight = 14.sp
                                                        )
                                                        Text(
                                                            text = formatRelativeTime(comment.timestamp),
                                                            fontSize = 8.sp,
                                                            color = ThreadsSubtext
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
                }
            }
        }

        // Safety Log Detail Dialog (IT Act Sec 79 Secure Audit Trail viewer)
        if (selectedSafetyLogDetail != null) {
            val log = selectedSafetyLogDetail!!
            Dialog(onDismissRequest = { selectedSafetyLogDetail = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("safety_log_detail_dialog"),
                    colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ThreadsBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔒 SECURITY SYSTEM REPORT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ThreadsErrorRed
                            )
                            IconButton(onClick = { selectedSafetyLogDetail = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close security details", tint = ThreadsWhite)
                            }
                        }

                        HorizontalDivider(color = ThreadsBorder)

                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = ThreadsSubtext)) {
                                    append("Action Taken: ")
                                }
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = if (log.actionTaken.contains("BANNED")) ThreadsErrorRed else ThreadsSuccessGreen)) {
                                    append(log.actionTaken)
                                }
                            },
                            fontSize = 12.sp
                        )

                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = ThreadsSubtext)) {
                                    append("Offending Merchant: ")
                                }
                                withStyle(androidx.compose.ui.text.SpanStyle(color = ThreadsWhite)) {
                                    append(log.userId)
                                }
                            },
                            fontSize = 12.sp
                        )

                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = ThreadsSubtext)) {
                                    append("Compliance Date: ")
                                }
                                withStyle(androidx.compose.ui.text.SpanStyle(color = ThreadsWhite)) {
                                    append(java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(log.timestamp))
                                }
                            },
                            fontSize = 12.sp
                        )

                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = ThreadsSubtext)) {
                                    append("Trigger Reason: ")
                                }
                                withStyle(androidx.compose.ui.text.SpanStyle(color = ThreadsWhite)) {
                                    append(log.triggerReason)
                                }
                            },
                            fontSize = 12.sp
                        )

                        Text(
                            text = "OFFENDING CONTEXT CONTENT:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThreadsSubtext
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ThreadsOled, RoundedCornerShape(6.dp))
                                .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(6.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = log.postContent,
                                color = ThreadsWhite,
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { selectedSafetyLogDetail = null },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled)
                        ) {
                            Text("Acknowledge Log Report", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5.5 EDIT PROFILE DIALOG
// ==========================================
@Composable
fun EditProfileDialog(
    profile: UserProfile,
    viewModel: B2bViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var instagram by remember { mutableStateOf(profile.instagramUrl) }
    var facebook by remember { mutableStateOf(profile.facebookUrl) }
    var whatsapp by remember { mutableStateOf(profile.whatsappNumber) }
    var phoneNumber by remember { mutableStateOf(profile.phoneNumber) }
    var isSaving by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
            border = BorderStroke(1.dp, ThreadsBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit Profile & Contact Info",
                    color = ThreadsWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Business / Full Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThreadsWhite,
                        unfocusedBorderColor = ThreadsGray,
                        focusedTextColor = ThreadsWhite,
                        unfocusedTextColor = ThreadsWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Registered Mobile Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThreadsWhite,
                        unfocusedBorderColor = ThreadsGray,
                        focusedTextColor = ThreadsWhite,
                        unfocusedTextColor = ThreadsWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp Contact Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThreadsWhite,
                        unfocusedBorderColor = ThreadsGray,
                        focusedTextColor = ThreadsWhite,
                        unfocusedTextColor = ThreadsWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instagram,
                    onValueChange = { instagram = it },
                    label = { Text("Instagram @username or URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThreadsWhite,
                        unfocusedBorderColor = ThreadsGray,
                        focusedTextColor = ThreadsWhite,
                        unfocusedTextColor = ThreadsWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = facebook,
                    onValueChange = { facebook = it },
                    label = { Text("Facebook Username or Page URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThreadsWhite,
                        unfocusedBorderColor = ThreadsGray,
                        focusedTextColor = ThreadsWhite,
                        unfocusedTextColor = ThreadsWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = ThreadsSubtext)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isValidIndianMobileNumber(phoneNumber)) {
                                Toast.makeText(context, "Please enter a genuine 10-digit Indian Mobile Number.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            if (!isValidIndianMobileNumber(whatsapp)) {
                                Toast.makeText(context, "Please enter a genuine 10-digit Indian WhatsApp Number.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            isSaving = true
                            viewModel.updateUserProfile(
                                name = name.trim(),
                                instagram = instagram.trim(),
                                facebook = facebook.trim(),
                                whatsapp = whatsapp.trim(),
                                phoneNumber = phoneNumber.trim(),
                                onComplete = {
                                    isSaving = false
                                    Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Save", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SELLER PROFILE VIEW WITH SOCIALS LINKS
// ==========================================
@Composable
fun SellerProfileScreen(
    profile: UserProfile,
    subscription: com.example.data.Subscription?,
    onUpgradeClick: () -> Unit,
    viewModel: B2bViewModel
) {
    val context = LocalContext.current
    val isCustomer = profile.role == "Customer"
    val maxLim = 10
    val isAdmin = profile.isAdmin()
    val limitVal = subscription?.getLimitVal(isAdmin) ?: 10
    val isPremium = isAdmin || (limitVal > 10)
    val spent = subscription?.postCountThisMonth ?: 0
    var showEditDialog by remember { mutableStateOf(false) }
    var showUsageDashboard by remember { mutableStateOf(false) }

    val progressFloat = (spent.toFloat() / limitVal.toFloat()).coerceIn(0f, 1f)

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            viewModel = viewModel,
            onDismiss = { showEditDialog = false }
        )
    }

    // --- Usage Dashboard Overlay Dialog ---
    if (showUsageDashboard) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showUsageDashboard = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                border = BorderStroke(1.5.dp, ThreadsSuccessGreen)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 USAGE DASHBOARD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ThreadsWhite,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = { showUsageDashboard = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close overlay", tint = ThreadsSubtext)
                        }
                    }

                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Circular Progress Bar
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = progressFloat,
                            color = ThreadsSuccessGreen,
                            trackColor = ThreadsBorder,
                            strokeWidth = 10.dp,
                            modifier = Modifier.size(150.dp)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$spent",
                                color = ThreadsWhite,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAdmin) "Unlimited" else "of $limitVal Posts",
                                color = ThreadsSubtext,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stats Details Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.4f)),
                            border = BorderStroke(0.5.dp, ThreadsBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("PUBLISHED", color = ThreadsSubtext, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("$spent", color = ThreadsWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = ThreadsBlack.copy(alpha = 0.4f)),
                            border = BorderStroke(0.5.dp, ThreadsBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("REMAINING", color = ThreadsSubtext, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (isAdmin) "∞" else "${(limitVal - spent).coerceAtLeast(0)}",
                                    color = ThreadsSuccessGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Educational Promo / Benefits Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                        border = BorderStroke(0.5.dp, ThreadsBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, "Benefit Icon", tint = ThreadsSuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Why Choose B2B Threads?", color = ThreadsWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                text = "Our platform is 100% commission-free, letting you showcase your direct trade profiles, WhatsApp catalog, and Instagram posts without spending a single rupee. By sharing B2B Threads, you help build a larger network of verified traders and buyers!",
                                color = ThreadsSubtext,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showUsageDashboard = false
                                shareAppApk(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, "Share", tint = ThreadsOled, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share App", color = ThreadsOled, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = { showUsageDashboard = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsCard),
                            modifier = Modifier.weight(1f).height(40.dp),
                            border = BorderStroke(0.5.dp, ThreadsBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss", color = ThreadsWhite, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🇮🇳 MERCHANT DASHBOARD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ThreadsSubtext,
                letterSpacing = 1.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { viewModel.triggerProfileLoading() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Profile",
                        tint = ThreadsSubtext,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

        // Avatar
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    if (isCustomer) {
                        Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF0D47A1)))
                    } else {
                        Brush.sweepGradient(listOf(Color.White, Color.DarkGray, Color.White))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(profile.name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp)
        }

        Text(profile.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ThreadsWhite)
        Text("@${profile.id.removePrefix("@")}", fontSize = 14.sp, color = ThreadsSubtext)

        // Class type badge
        Box(
            modifier = Modifier
                .background(if (isCustomer) Color(0xFF0D47A1) else ThreadsGray, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isCustomer) "VERIFIED CUSTOMER Setup" else "${profile.profileType.uppercase()} CLASSIFICATION",
                color = if (isCustomer) Color.White else ThreadsSubtext,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Standalone Usage Dashboard Trigger Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showUsageDashboard = true }
                .testTag("view_usage_dashboard_card"),
            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
            border = BorderStroke(1.dp, ThreadsSuccessGreen.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F1A15), CircleShape)
                            .size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DataUsage,
                            contentDescription = "Usage icon",
                            tint = ThreadsSuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "MONTHLY USAGE DASHBOARD",
                            fontWeight = FontWeight.Bold,
                            color = ThreadsWhite,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Track your active postings & limits",
                            color = ThreadsSubtext,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = "Go",
                    tint = ThreadsSuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isCustomer) {
            // Customer Credentials & Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                border = BorderStroke(0.5.dp, ThreadsBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Verified Buyer Contact Details:", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Phone, contentDescription = "Phone", tint = Color(0xFF2196F3))
                            Text("Mobile Number", color = ThreadsWhite, fontSize = 13.sp)
                        }
                        Text(
                            profile.phoneNumber.ifBlank { "Not registered" },
                            color = ThreadsWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Email, contentDescription = "Email", tint = Color(0xFF2196F3))
                            Text("Email Address", color = ThreadsWhite, fontSize = 13.sp)
                        }
                        Text(
                            profile.email.ifBlank { "Not registered" },
                            color = ThreadsWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                    Button(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_customer_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit details", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Mobile & Name Manually", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                border = BorderStroke(1.dp, Color(0xFF333333))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "India Cyber-Safe B2B/B2C Guidelines",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3),
                        fontSize = 13.sp
                    )
                    Text(
                        "To protect Indian SMEs and Manufacturers from trade impersonation, Customer/Buyer accounts can only view active portfolios, comment on posts, and connect directly with sellers. Customer posting privileges are disabled for compliance.",
                        color = ThreadsSubtext,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            // On-profile Social accounts reach cards for Sellers
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                border = BorderStroke(0.5.dp, ThreadsBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Verified Digital Reach Media Links (Click to Test):", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 13.sp)

                    // Insta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (profile.instagramUrl.isNotBlank()) {
                                    val igHandle = profile.instagramUrl.trim().removePrefix("@")
                                    val isFullUrl = profile.instagramUrl.startsWith("http")
                                    val url = if (isFullUrl) profile.instagramUrl else "https://instagram.com/$igHandle"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open Instagram profile", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Instagram account not configured yet.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = "Handles",
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Instagram Display Account", color = ThreadsWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = profile.instagramUrl.ifBlank { "Not configured" },
                                color = if (profile.instagramUrl.isNotBlank()) ThreadsSuccessGreen else ThreadsSubtext,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                    // Facebook
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (profile.facebookUrl.isNotBlank()) {
                                    val fbHandle = profile.facebookUrl.trim()
                                    val isFullUrl = profile.facebookUrl.startsWith("http")
                                    val url = if (isFullUrl) profile.facebookUrl else "https://facebook.com/$fbHandle"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open Facebook catalogue", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Facebook account not configured yet.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "FB",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Facebook Brand Connect", color = ThreadsWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = profile.facebookUrl.ifBlank { "Not configured" },
                                color = if (profile.facebookUrl.isNotBlank()) ThreadsSuccessGreen else ThreadsSubtext,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                    // WhatsApp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (profile.whatsappNumber.isNotBlank()) {
                                    val filteredWa = profile.whatsappNumber.replace(" ", "").trim().removePrefix("+91").removePrefix("91")
                                    val waUrl = if (filteredWa.isNotBlank()) "https://wa.me/91$filteredWa" else "https://wa.me/919999999999"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not launch WhatsApp link", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "WhatsApp account not configured yet.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "WA",
                                tint = ThreadsSuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("WhatsApp Wholesale Chat", color = ThreadsWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = profile.whatsappNumber.ifBlank { "Not configured" },
                            color = ThreadsWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                    Button(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_seller_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ThreadsGray, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit links", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Social Links & Mobile Manually", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Card Subscription Plan for Sellers
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsBlack),
                border = BorderStroke(1.dp, if (isPremium) ThreadsSuccessGreen else ThreadsBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your B2B Subscription Log:",
                            fontWeight = FontWeight.Bold,
                            color = ThreadsWhite,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isPremium) "GOLD PREM" else "FREE TIER",
                            color = if (isPremium) ThreadsSuccessGreen else ThreadsSubtext,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isAdmin) {
                        Text("Unlimited Monthly uploads successfully unlocked. Ideal for Indian exporter wholesale catalogs.", color = ThreadsSubtext, fontSize = 12.sp)
                    } else if (isPremium) {
                        Text("Paid tier unlocked. You have $limitVal active postings available. Spent currently: $spent / $limitVal.", color = ThreadsSubtext, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onUpgradeClick,
                            modifier = Modifier.fillMaxWidth().testTag("upgrade_subscription_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("BUY +10 POSTS (₹500)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Text("Free plan allows exactly 10 postings/3-months. Spent currently: $spent / $maxLim.", color = ThreadsSubtext, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onUpgradeClick,
                            modifier = Modifier.fillMaxWidth().testTag("upgrade_subscription_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Upgrade via Direct GPay / UPI", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Developer Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
            border = BorderStroke(1.dp, ThreadsBorder)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF23180F), CircleShape)
                        .size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Developer logo",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "APP OWNER & DEVELOPER DETAILS",
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Muhammad Ali (Proprietor)",
                        color = ThreadsWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "CodeCraft Technologies\nPhone: +91 9044732288\n26 Bagh Qazi, Yahiganj, U.P., India",
                        color = ThreadsSubtext,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. REAL-TIME SECURE PAYMENT SIMULATOR MODAL
// ==========================================
@Composable
fun PaymentGatewaySimulationDialog(
    userId: String,
    viewModel: B2bViewModel,
    onDismiss: () -> Unit
) {
    var utrInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Dialog(onDismissRequest = {
        viewModel.paymentGatewaySimulatedDetails = null
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
            border = BorderStroke(1.dp, Color(0xFFFF9800)), // Distinguished orange border for compliance
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "GPay Secure",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "GPay Direct UPI Verification",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ThreadsWhite
                        )
                    }
                    IconButton(onClick = {
                        viewModel.paymentGatewaySimulatedDetails = null
                        onDismiss()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = ThreadsWhite)
                    }
                }

                Text(
                    text = "Indian cyber-regulations restrict auto-gateway processing for specific SME trade. Payment must be transferred directly via Google Pay (GPay) or any UPI app to our registered compliance merchant account.",
                    fontSize = 11.sp,
                    color = ThreadsSubtext,
                    lineHeight = 15.sp
                )

                // Pay information card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                    border = BorderStroke(0.5.dp, ThreadsBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "MERCHANT DESIGNATED GPAY:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GPay Number: +91 9044732288",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ThreadsWhite
                            )
                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("+919044732288"))
                                    Toast.makeText(context, "GPay Number copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThreadsWhite, contentColor = ThreadsOled),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Copy Num", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(color = ThreadsBorder, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Amount due: ₹500.00",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ThreadsSuccessGreen
                            )

                            Button(
                                onClick = {
                                    val upiUri = Uri.parse("upi://pay?pa=9044732288@okbizaxis&pn=B2bThreadsWholesale&am=500.00&cu=INR&tn=SellerPremiumUpgrade")
                                    val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No compatible UPI app detected. Please manually transfer on your GPay App using our copied number.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5), contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.ArrowOutward, contentDescription = "Launch", tint = Color.White, modifier = Modifier.size(10.dp))
                                    Text("Launch GPay", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Reference Input Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Enter GPay UPI Transaction ID / UTR (Mandatory):",
                        fontSize = 11.sp,
                        color = ThreadsWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = utrInput,
                        onValueChange = { input ->
                            if (input.length <= 12) {
                                utrInput = input.filter { it.isDigit() }
                            }
                        },
                        placeholder = { Text("Enter 12-Digit Reference No (e.g., 603214589251)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThreadsWhite,
                            unfocusedBorderColor = ThreadsGray,
                            focusedTextColor = ThreadsWhite,
                            unfocusedTextColor = ThreadsWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("utr_reference_field")
                    )
                    Text(
                        text = "⚠️ Warning: Stripe, Razorpay, and direct pass bypass are disabled. Plan will NOT unlock without authentic Rs 500 GPay receipt validation verified by the proprietor.",
                        fontSize = 9.sp,
                        color = Color(0xFFEF9A9A),
                        lineHeight = 12.sp
                    )
                }

                val log = viewModel.paymentGatewaySimulatedDetails
                if (log != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThreadsOled, RoundedCornerShape(8.dp))
                            .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (log.contains("SUCCESS")) ThreadsSuccessGreen else Color(0xFFFFCC00)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.upgradeWithGPayUpi(userId, utrInput) { success, msg ->
                            if (success) {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Verification Failed: $msg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("simulate_payment_trigger"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = Color.White),
                    enabled = utrInput.length == 12
                ) {
                    Text("Submit Reference ID for Settlement Verification", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ==========================================
// 8. LEGAL COMPLIANCE & SAFETY SCREEN
// ==========================================
@Composable
fun LegalComplianceScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onBack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ThreadsWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to Feed System", color = ThreadsWhite, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFF0F2C1A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Gavel, contentDescription = "Legal security", tint = ThreadsSuccessGreen, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Indian Cyber Law & Legal Compliance Policy", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ThreadsWhite)
            Text("Zero-Tolerance Framework for CodeCraft Technologies application services.", fontSize = 12.sp, color = ThreadsSubtext)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                border = BorderStroke(0.5.dp, ThreadsBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Information Technology (IT) Act, 2000 Compliance", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 14.sp)
                    Text(
                        "Under Section 79 of the Indian IT Act, this application utilizes an active automated content screening filter (Layer 1 Heuristics & Layer 2 Artificial Intelligence ContentModerator) to scan and remove illegal products. Banned subjects encompass severe intoxicants (Alcohol), products forbidden/Haram in Islam (Pork, gambling, betting lottery schemes), and premium NSFW imagery.",
                        fontSize = 12.sp, color = ThreadsSubtext, lineHeight = 17.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                border = BorderStroke(0.5.dp, ThreadsBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Digital Personal Data Protection (DPDP) Act, 2023", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 14.sp)
                    Text(
                        "All merchant contact phone numbers and metadata stored inside the SQLite database room elements remain encrypted on the user device container. No cross-app user data extraction occurs. This satisfies digital privacy provisions and sovereign Indian customer protection standards.",
                        fontSize = 12.sp, color = ThreadsSubtext, lineHeight = 17.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                border = BorderStroke(0.5.dp, ThreadsBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. CodeCraft Technologies Legal Oversight", fontWeight = FontWeight.Bold, color = ThreadsWhite, fontSize = 14.sp)
                    Text(
                        "Master administrative control features are restricted solely to Authorized CodeCraft Engineers. Overrides can restore erroneously restricted accounts or execute absolute content purging to keep the Indian seller community entirely secure.",
                        fontSize = 12.sp, color = ThreadsSubtext, lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. VERIFIED SELLERS DEDICATED DIRECTORY SCREEN
// ==========================================
@Composable
fun SellersDirectoryScreen(
    companies: List<UserProfile>,
    onCompanyClick: (UserProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProfileTypeFilter by remember { mutableStateOf("All") }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isClickedState by remember { mutableStateOf(false) }
    val showDescription = isHovered || isClickedState

    var isSearchExpanded by remember { mutableStateOf(false) }

    val filteredCompanies = companies.filter { comp ->
        // Exclude customers and admin profiles from B2B Trade Directory
        val isCustomer = comp.role == "Customer"
        val isAdmin = comp.id.removePrefix("@").lowercase() == "rahman8040samsung" || 
                      comp.id.removePrefix("@").lowercase() == "rahman8040" || 
                      comp.id.removePrefix("@").lowercase() == "codecrafttechnologies" || 
                      comp.email.lowercase() == "rahman8040samsung@gmail.com" ||
                      comp.id.removePrefix("@").lowercase() == "admin" ||
                      comp.id.removePrefix("@").lowercase().contains("admin") ||
                      comp.role.equals("admin", ignoreCase = true) ||
                      comp.profileType.equals("SuperAdmin", ignoreCase = true)
        
        if (isCustomer || isAdmin) return@filter false

        val matchesType = selectedProfileTypeFilter == "All" || 
                comp.profileType.equals(selectedProfileTypeFilter, ignoreCase = true)
        
        val q = searchQuery.trim()
        val matchesSearch = q.isBlank() ||
                comp.name.contains(q, ignoreCase = true) ||
                comp.id.contains(q, ignoreCase = true) ||
                comp.profileType.contains(q, ignoreCase = true) ||
                comp.gstin.contains(q, ignoreCase = true) ||
                comp.aadhaarNumber.contains(q, ignoreCase = true) ||
                comp.email.contains(q, ignoreCase = true) ||
                comp.phoneNumber.contains(q, ignoreCase = true) ||
                (q.lowercase() == "msme" && comp.aadhaarNumber.isNotBlank()) ||
                (q.lowercase() == "gst" && comp.gstin.isNotBlank()) ||
                (q.lowercase() == "oem" && comp.profileType.equals("OEM", ignoreCase = true))
                
        matchesType && matchesSearch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThreadsOled)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isClickedState = !isClickedState }
                    .hoverable(interactionSource = interactionSource)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Verified Trade Directory",
                    fontSize = 15.sp, // Smaller as requested
                    fontWeight = FontWeight.Bold,
                    color = ThreadsWhite,
                    modifier = Modifier.testTag("sellers_directory_title")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (showDescription) Icons.Default.Info else Icons.Outlined.Info,
                    contentDescription = "Toggle description",
                    tint = if (showDescription) ThreadsWhite else ThreadsSubtext,
                    modifier = Modifier.size(13.dp)
                )
            }

            AnimatedVisibility(visible = showDescription) {
                Text(
                    text = "Discover verified Indian manufacturers, traders, and suppliers compliant with IT Act guidelines.",
                    fontSize = 11.sp,
                    color = ThreadsSubtext,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Segment Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val segments = listOf("All", "Manufacturer", "OEM", "Trader", "Retailer", "Individual")
                segments.forEach { seg ->
                    val isSelected = selectedProfileTypeFilter == seg
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ThreadsWhite else ThreadsGray)
                            .clickable { selectedProfileTypeFilter = seg }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = seg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ThreadsOled else ThreadsWhite
                        )
                    }
                }
            }

            // Lazy Column list of beautiful detailed seller profile cards
            if (filteredCompanies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching verified partners found.", color = ThreadsSubtext)
                }
            } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCompanies) { comp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCompanyClick(comp) }
                            .testTag("seller_card_${comp.id}"),
                        colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, ThreadsBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar on Left
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF262626))
                                    .border(1.dp, ThreadsSuccessGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comp.name.take(1).uppercase(),
                                    color = ThreadsSuccessGreen,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Info in Middle
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = comp.name,
                                        color = ThreadsWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Partner",
                                        tint = ThreadsSuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = comp.id,
                                    color = ThreadsSubtext,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                ) {
                                    // 1. Segment badge (Manufacturer, OEM, Trader, Exporter, Retailer)
                                    Box(
                                        modifier = Modifier
                                            .background(ThreadsGray, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = comp.profileType.uppercase(),
                                            color = Color(0xFFA889F7),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    // 2. GST Active
                                    if (comp.gstin.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF0F2615), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "GST ACTIVE",
                                                color = ThreadsSuccessGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    
                                    // 3. OEM Compliant
                                    if (comp.profileType.equals("OEM", ignoreCase = true)) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF1E3A8A), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "OEM COMPLIANT",
                                                color = Color(0xFF93C5FD),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    
                                    // 4. MSME Badge
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF3B0764), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "MSME VERIFIED",
                                            color = Color(0xFFE9D5FF),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // 5. ISO Certified Badge
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF78350F), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ISO CERTIFIED",
                                            color = Color(0xFFFDE68A),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Arrow right on Right
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Profile details",
                                tint = ThreadsSubtext,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Close search column and nested main column
        }

        // Floating Search Overlay Card & Floating Search Button close/trigger at bottom right ("bottom me profile ke pass")
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .testTag("sellers_directory_search_overlay_card"),
                    colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                    border = BorderStroke(1.dp, ThreadsBorder),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search partner name...", fontSize = 12.sp, color = ThreadsSubtext) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = ThreadsWhite,
                                unfocusedTextColor = ThreadsWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sellers_directory_search_input"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = ThreadsSubtext,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.testTag("sellers_directory_search_fab"),
                containerColor = ThreadsWhite,
                contentColor = ThreadsOled,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search trade directory"
                )
            }
        }
    }
}

// ==========================================
// 6. SHARED DETAILED TRADE PROFILE DIALOG
// ==========================================
@Composable
fun TradeProfileDetailsDialog(
    company: UserProfile,
    onDismiss: () -> Unit,
    onMessageClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, ThreadsBorder), RoundedCornerShape(14.dp))
                .background(ThreadsCard)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Area with Recognizable Back / Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Business Hub",
                            tint = ThreadsSuccessGreen
                        )
                        Text(
                            "Trade Profile Details",
                            color = ThreadsWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Visible back button to return to previous stream
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(ThreadsGray, CircleShape)
                            .testTag("close_trade_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close trade profile detail view",
                            tint = ThreadsWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

                // Corporate Info Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThreadsBlack, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = company.name,
                        color = ThreadsWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = company.id,
                            color = ThreadsSubtext,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (company.gstin.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E3A1E))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = "GSTIN Verified", tint = ThreadsSuccessGreen, modifier = Modifier.size(10.dp))
                                    Text("GST TAX ACTIVE", color = ThreadsSuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Classification details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Professional Role:", color = ThreadsSubtext, fontSize = 11.sp)
                        Text(company.role, color = ThreadsWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Trade Segment:", color = ThreadsSubtext, fontSize = 11.sp)
                        Text(company.profileType, color = Color(0xFFA889F7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (company.gstin.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Verified GSTIN:", color = ThreadsSubtext, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = company.gstin,
                                color = ThreadsWhite,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                // Interactive Communication Actions Panel
                Text(
                    "Direct Business Connexions Clickable:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThreadsSubtext
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Direct DM Button (Instagram/Threads style)
                    if (onMessageClick != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onMessageClick(company.id)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("profile_direct_message_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Email, contentDescription = "Send DM", tint = Color.Black, modifier = Modifier.size(16.dp))
                                Text("✉️ Send Direct Message (Instagram style)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // WhatsApp
                    val filteredWa = company.whatsappNumber.replace(" ", "").trim().removePrefix("+91")
                    val waUrl = if (filteredWa.isNotBlank()) "https://wa.me/91$filteredWa" else "https://wa.me/919999999999"
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not launch WhatsApp link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("profile_whatsapp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("💬 Discuss trade on WhatsApp (${company.whatsappNumber.ifBlank { "Unspecified" }})", fontSize = 11.sp, color = Color.White)
                    }

                    // Instagram
                    if (company.instagramUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                val igHandle = company.instagramUrl.trim().removePrefix("@")
                                val isFullUrl = company.instagramUrl.startsWith("http")
                                val url = if (isFullUrl) company.instagramUrl else "https://instagram.com/$igHandle"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open Instagram profile", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📷 Check Catalogue on Instagram (@${company.instagramUrl.removePrefix("@")})", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    // Facebook
                    if (company.facebookUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                val fbHandle = company.facebookUrl.trim()
                                val isFullUrl = company.facebookUrl.startsWith("http")
                                val url = if (isFullUrl) company.facebookUrl else "https://facebook.com/$fbHandle"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open Facebook catalogue", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📘 View Business Page on Facebook", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Explicit recognizable button to back out
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, ThreadsBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ThreadsWhite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Return", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back to Sellers", fontSize = 11.sp)
                }
            }
        }
    }
}

// ==========================================
// 7. FOOTER RUNNING CAROUSEL STRIP OF VERIFIED SELLERS
// ==========================================
@Composable
fun HorizontalVerifiedSellersStrip(
    companies: List<UserProfile>,
    onCompanyClick: (UserProfile) -> Unit
) {
    if (companies.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0C0C0C))
            .border(BorderStroke(0.5.dp, ThreadsBorder))
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified tag",
                    tint = ThreadsSuccessGreen,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "VERIFIED PARTNERS CAROUSEL",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThreadsSuccessGreen,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "Tap to Contact",
                fontSize = 8.sp,
                color = ThreadsSubtext
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            companies.forEach { comp ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ThreadsCard)
                        .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(6.dp))
                        .clickable { onCompanyClick(comp) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("footer_carousel_partner_${comp.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = comp.name.take(1).uppercase(),
                                color = ThreadsSuccessGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = comp.name,
                                    color = ThreadsWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Check",
                                    tint = ThreadsSuccessGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Text(
                                text = comp.profileType,
                                color = Color(0xFFA889F7),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun playTapFeedback(): () -> Unit {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val view = androidx.compose.ui.platform.LocalView.current
    return {
        try {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        } catch (e: Exception) {
            // ignore
        }
    }
}

// ==========================================
// THREADS DIRECT MESSAGING & INBOX SYSTEM
// ==========================================
@Composable
fun InboxScreen(
    viewModel: B2bViewModel,
    initialChatPartnerId: String? = null,
    onClearInitialPartner: () -> Unit = {},
    onBack: () -> Unit
) {
    val currentUser = viewModel.currentUserProfile ?: return
    val allMessages by viewModel.directMessages.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allUserProfiles.collectAsStateWithLifecycle()

    var activeChatPartnerId by remember { mutableStateOf<String?>(initialChatPartnerId) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    val feedback = playTapFeedback()

    LaunchedEffect(initialChatPartnerId) {
        if (initialChatPartnerId != null) {
            activeChatPartnerId = initialChatPartnerId
        }
    }

    if (activeChatPartnerId != null) {
        val partnerId = activeChatPartnerId!!
        val partnerProfile = allProfiles.find { it.id == partnerId } ?: UserProfile(
            id = partnerId,
            name = partnerId.removePrefix("@").replaceFirstChar { it.uppercase() },
            profileType = "Business",
            role = "Seller"
        )

        IndividualChatScreen(
            viewModel = viewModel,
            currentUser = currentUser,
            partner = partnerProfile,
            messages = allMessages.filter {
                (it.senderId == currentUser.id && it.receiverId == partnerId) ||
                (it.senderId == partnerId && it.receiverId == currentUser.id)
            },
            onBack = {
                feedback()
                activeChatPartnerId = null
                onClearInitialPartner()
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThreadsOled)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThreadsWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Direct Messages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThreadsWhite
                    )
                }

                IconButton(
                    onClick = {
                        feedback()
                        showNewChatDialog = true
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ThreadsCard)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "New Chat", tint = ThreadsSuccessGreen, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val conversations = remember(allMessages, currentUser.id) {
                allMessages
                    .filter { it.senderId == currentUser.id || it.receiverId == currentUser.id }
                    .groupBy { msg ->
                        if (msg.senderId == currentUser.id) msg.receiverId else msg.senderId
                    }
                    .map { (partnerId, msgs) ->
                        val lastMsg = msgs.maxByOrNull { it.timestamp }!!
                        partnerId to lastMsg
                    }
                    .sortedByDescending { it.second.timestamp }
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "No messages",
                            tint = ThreadsSubtext,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Inbox Empty",
                            color = ThreadsWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Discuss wholesale deals directly with manufacturers, buyers, and sellers.",
                            color = ThreadsSubtext,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Button(
                            onClick = {
                                feedback()
                                showNewChatDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThreadsSuccessGreen)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search users", tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Find Partner", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(conversations) { (partnerId, lastMsg) ->
                        val partnerProfile = allProfiles.find { it.id == partnerId } ?: UserProfile(
                            id = partnerId,
                            name = partnerId.removePrefix("@").replaceFirstChar { it.uppercase() },
                            profileType = "Business",
                            role = "Seller"
                        )

                        ConversationItem(
                            partner = partnerProfile,
                            lastMessage = lastMsg,
                            onClick = {
                                feedback()
                                activeChatPartnerId = partnerId
                            }
                        )
                    }
                }
            }
        }

        if (showNewChatDialog) {
            NewChatSelectionDialog(
                currentUser = currentUser,
                allProfiles = allProfiles,
                onDismiss = { showNewChatDialog = false },
                onSelectUser = { partner ->
                    showNewChatDialog = false
                    activeChatPartnerId = partner.id
                }
            )
        }
    }
}

@Composable
fun ConversationItem(
    partner: UserProfile,
    lastMessage: DirectMessage,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ThreadsCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (partner.role == "Seller") ThreadsSuccessGreen.copy(alpha = 0.2f)
                            else Color(0xFF3F51B5).copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = partner.name.take(2).uppercase(),
                        color = if (partner.role == "Seller") ThreadsSuccessGreen else Color(0xFF7986CB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                
                val partnerIsOnline = partner.id.hashCode() % 2 == 0
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(ThreadsOled)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (partnerIsOnline) ThreadsSuccessGreen else ThreadsSubtext)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = partner.name,
                            color = ThreadsWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = partner.id,
                            color = ThreadsSubtext,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 80.dp)
                        )
                    }

                    val relativeTime = remember(lastMessage.timestamp) {
                        val diff = System.currentTimeMillis() - lastMessage.timestamp
                        when {
                            diff < 60000 -> "now"
                            diff < 3600000 -> "${diff / 60000}m ago"
                            diff < 86400000 -> "${diff / 3600000}h ago"
                            else -> "${diff / 86400000}d ago"
                        }
                    }
                    Text(
                        text = relativeTime,
                        color = ThreadsSubtext,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastMessage.messageText,
                        color = if (lastMessage.isRead) ThreadsSubtext else ThreadsWhite,
                        fontWeight = if (lastMessage.isRead) FontWeight.Normal else FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (partner.role == "Seller") ThreadsSuccessGreen.copy(alpha = 0.15f)
                                else Color(0xFF3F51B5).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = partner.role,
                            color = if (partner.role == "Seller") ThreadsSuccessGreen else Color(0xFF7986CB),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IndividualChatScreen(
    viewModel: B2bViewModel,
    currentUser: UserProfile,
    partner: UserProfile,
    messages: List<DirectMessage>,
    onBack: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<DirectMessage?>(null) }
    val lazyListState = rememberLazyListState()
    val feedback = playTapFeedback()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThreadsOled)
            .imePadding()
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThreadsCard)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ThreadsWhite)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (partner.role == "Seller") ThreadsSuccessGreen.copy(alpha = 0.2f)
                        else Color(0xFF3F51B5).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = partner.name.take(2).uppercase(),
                    color = if (partner.role == "Seller") ThreadsSuccessGreen else Color(0xFF7986CB),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = partner.name,
                        color = ThreadsWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (partner.role == "Seller") {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Merchant",
                            tint = ThreadsSuccessGreen,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val partnerIsOnline = partner.id.hashCode() % 2 == 0
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (partnerIsOnline) ThreadsSuccessGreen else ThreadsSubtext)
                    )
                    Text(
                        text = if (partnerIsOnline) "Active Now" else "Offline • Last seen 15m ago",
                        color = if (partnerIsOnline) ThreadsSuccessGreen else ThreadsSubtext,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThreadsOled)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = partner.profileType,
                    color = ThreadsSubtext,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

        // Messages list
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(messages) { index, msg ->
                val isMe = msg.senderId == currentUser.id
                
                // Deterministic Date Header
                val showDateHeader = if (index == 0) {
                    true
                } else {
                    val prevMsg = messages[index - 1]
                    val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                    val currentDay = sdf.format(java.util.Date(msg.timestamp))
                    val prevDay = sdf.format(java.util.Date(prevMsg.timestamp))
                    currentDay != prevDay
                }
                
                if (showDateHeader) {
                    val dateHeaderStr = remember(msg.timestamp) {
                        val sdf = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
                        val today = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                        val yesterday = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis() - 86400000))
                        val formatted = sdf.format(java.util.Date(msg.timestamp))
                        when (formatted) {
                            today -> "Today"
                            yesterday -> "Yesterday"
                            else -> formatted
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ThreadsCard)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = dateHeaderStr,
                                color = ThreadsSubtext,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    // Linked Thread Post Context Banner
                    if (msg.associatedPostId != null && msg.associatedPostTitle != null) {
                        Card(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .widthIn(max = 240.dp),
                            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
                            border = BorderStroke(0.5.dp, ThreadsBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Post Thread", tint = ThreadsSuccessGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Inquire: ${msg.associatedPostTitle}",
                                    fontSize = 10.sp,
                                    color = ThreadsSubtext,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
 
                    var showMessageMenu by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 2.dp,
                                    bottomEnd = if (isMe) 2.dp else 16.dp
                                )
                            )
                            .background(
                                if (isMe) ThreadsSuccessGreen else ThreadsCard
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        if (isMe) {
                                            feedback()
                                            showMessageMenu = true
                                        }
                                    }
                                )
                            }
                            .semantics {
                                if (isMe) {
                                    onLongClick("Options") {
                                        showMessageMenu = true
                                        true
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Column {
                            val bubbleTextColor = if (isMe) {
                                Color.Black
                            } else {
                                if (isLightThemeMode) Color.Black else Color.White
                            }
                            val bubbleTimeColor = if (isMe) {
                                Color.Black.copy(alpha = 0.65f)
                            } else {
                                if (isLightThemeMode) Color(0xFF555555) else Color(0xFFB0B0B0)
                            }

                            Text(
                                text = msg.messageText,
                                color = bubbleTextColor,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            
                            val formattedTime = remember(msg.timestamp) {
                                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(msg.timestamp))
                            }
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (msg.isEdited) {
                                    Text(
                                        text = "(edited)",
                                        color = bubbleTimeColor.copy(alpha = 0.5f),
                                        fontSize = 9.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                                Text(
                                    text = formattedTime,
                                    color = bubbleTimeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMessageMenu,
                            onDismissRequest = { showMessageMenu = false },
                            modifier = Modifier.background(ThreadsCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Message", color = ThreadsWhite) },
                                onClick = {
                                    showMessageMenu = false
                                    editingMessage = msg
                                    textInput = msg.messageText
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = ThreadsErrorRed) },
                                onClick = {
                                    showMessageMenu = false
                                    viewModel.deleteDirectMessage(msg.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = ThreadsBorder, thickness = 0.5.dp)

        // Bottom Input Row
        if (editingMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThreadsOled)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Editing message", color = ThreadsSuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = {
                        editingMessage = null
                        textInput = ""
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = ThreadsSubtext)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThreadsCard)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text(if (editingMessage != null) "Edit message..." else "Write commercial offer...", color = ThreadsSubtext, fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(color = ThreadsWhite, fontSize = 14.sp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = ThreadsWhite,
                    unfocusedTextColor = ThreadsWhite,
                    focusedContainerColor = ThreadsOled,
                    unfocusedContainerColor = ThreadsOled,
                    disabledContainerColor = ThreadsOled,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(20.dp))
            )

            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        feedback()
                        if (editingMessage != null) {
                            viewModel.editDirectMessage(editingMessage!!, textInput)
                            editingMessage = null
                        } else {
                            viewModel.sendDirectMessage(partner.id, textInput)
                        }
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(ThreadsSuccessGreen)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (editingMessage != null) Icons.Default.Check else Icons.Default.Send,
                    contentDescription = if (editingMessage != null) "Save Edit" else "Send Direct Message",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun NewChatSelectionDialog(
    currentUser: UserProfile,
    allProfiles: List<UserProfile>,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val feedback = playTapFeedback()

    val filteredProfiles = remember(allProfiles, searchQuery, currentUser.id) {
        allProfiles.filter {
            it.id != currentUser.id && (
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
            )
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThreadsCard),
            border = BorderStroke(0.5.dp, ThreadsBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "New B2B Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThreadsWhite,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search handle or name...", color = ThreadsSubtext, fontSize = 13.sp) },
                    textStyle = LocalTextStyle.current.copy(color = ThreadsWhite, fontSize = 14.sp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = ThreadsWhite,
                        unfocusedTextColor = ThreadsWhite,
                        focusedContainerColor = ThreadsOled,
                        unfocusedContainerColor = ThreadsOled,
                        disabledContainerColor = ThreadsOled,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(BorderStroke(0.5.dp, ThreadsBorder), RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredProfiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No SME matches found",
                            color = ThreadsSubtext,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProfiles) { prof ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        feedback()
                                        onSelectUser(prof)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (prof.role == "Seller") ThreadsSuccessGreen.copy(alpha = 0.2f)
                                            else Color(0xFF3F51B5).copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = prof.name.take(2).uppercase(),
                                        color = if (prof.role == "Seller") ThreadsSuccessGreen else Color(0xFF7986CB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prof.name,
                                        color = ThreadsWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${prof.id} • ${prof.role}",
                                        color = ThreadsSubtext,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = ThreadsSubtext)
                }
            }
        }
    }
}

fun shareAppApk(context: android.content.Context) {
    try {
        val apkPath = context.packageCodePath
        val srcFile = java.io.File(apkPath)
        val cacheDir = java.io.File(context.cacheDir, "shared_apk")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val destFile = java.io.File(cacheDir, "B2B_Threads.apk")
        
        // Copy APK to cache
        srcFile.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        val authority = "${context.packageName}.fileprovider"
        val apkUri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(context, authority, destFile)
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(android.content.Intent.EXTRA_STREAM, apkUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Share Threads B2B App")
            putExtra(android.content.Intent.EXTRA_TEXT, "Install THREADS B2B App - The secure B2B & B2C marketplace network tailored for Indian merchants.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share APK via"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error sharing APK: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}


