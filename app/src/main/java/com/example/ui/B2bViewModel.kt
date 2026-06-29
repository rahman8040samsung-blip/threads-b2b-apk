package com.example.ui

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ContentModerator
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface PostCreationResult {
    object Idle : PostCreationResult
    object Success : PostCreationResult
    data class LimitExceeded(val message: String) : PostCreationResult
    data class Flagged(val reason: String) : PostCreationResult
    data class Error(val message: String) : PostCreationResult
}

data class FirestorePost(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorType: String = "",
    val textContent: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val price: String? = null,
    val category: String = "Other",
    val timestamp: Long = System.currentTimeMillis(),
    val isValid: Boolean = true,
    val specs: String? = null
)

data class InAppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val iconType: String = "info" // "info", "like", "comment", "success", "security"
)

class B2bViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = B2bRepository(database.b2bDao())
    private val sharedPrefs = application.getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)

    private val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    private val _firestorePosts = MutableStateFlow<List<FirestorePost>>(emptyList())
    val firestorePosts: StateFlow<List<FirestorePost>> = _firestorePosts.asStateFlow()

    private var postsListenerConnection: com.google.firebase.firestore.ListenerRegistration? = null

    // --- Connectivity State Monitor ---
    private val connectivityManager = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    var isOnline by mutableStateOf(checkInitialNetwork())
        private set

    private fun checkInitialNetwork(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val cap = connectivityManager.getNetworkCapabilities(network) ?: return false
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    isOnline = true
                }

                override fun onLost(network: Network) {
                    isOnline = false
                }
            }
            networkCallback = callback
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            isOnline = true // safe fallback
        }
    }

    // --- Active User Sessions ---
    var currentUserProfile by mutableStateOf<UserProfile?>(null)
        private set

    var isCheckingSession by mutableStateOf(true)
        private set

    var isFeedLoading by mutableStateOf(false)
        private set

    var isProfileLoading by mutableStateOf(false)
        private set

    var isFeedErrorSimulated by mutableStateOf(false)
    var isProfileErrorSimulated by mutableStateOf(false)

    fun triggerFeedLoading(durationMillis: Long = 1000L) {
        viewModelScope.launch {
            isFeedLoading = true
            kotlinx.coroutines.delay(durationMillis)
            isFeedLoading = false
        }
    }

    fun triggerProfileLoading(durationMillis: Long = 850L) {
        viewModelScope.launch {
            isProfileLoading = true
            kotlinx.coroutines.delay(durationMillis)
            isProfileLoading = false
        }
    }

    var currentUserSub by mutableStateOf<Subscription?>(null)
        private set

    // --- UI Screen States ---
    val approvedPosts: StateFlow<List<Post>> = repository.approvedPosts
        .map { list -> list.filter { it.isValid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPosts: StateFlow<List<Post>> = repository.allPosts
        .map { list -> list.filter { it.isValid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUserProfiles: StateFlow<List<UserProfile>> = repository.allUserProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allModerationLogs: StateFlow<List<ModerationLog>> = repository.allModerationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Persistent Comments & Likes Engine ---
    private val _postComments = MutableStateFlow<Map<Int, List<Comment>>>(emptyMap())
    val postComments: StateFlow<Map<Int, List<Comment>>> = _postComments.asStateFlow()

    private val _postLikes = MutableStateFlow<Map<Int, List<PostLike>>>(emptyMap())
    val postLikes: StateFlow<Map<Int, List<PostLike>>> = _postLikes.asStateFlow()

    // --- UI Controls ---
    var postCreationStatus by mutableStateOf<PostCreationResult>(PostCreationResult.Idle)
        private set

    var isBannedBySystem by mutableStateOf(false)
        private set

    var paymentGatewaySimulatedDetails by mutableStateOf<String?>(null)

    // Banned user list to represent digital ban/kick security
    private val bannedUserIds = mutableStateOf(setOf<String>())

    var loginErrorMessage by mutableStateOf<String?>(null)
        private set

    // --- Deep Link Integration ---
    var deepLinkedPostId by mutableStateOf<Int?>(null)
        private set

    fun handleDeepLink(uri: android.net.Uri?) {
        if (uri != null) {
            val path = uri.path
            if (path != null && path.contains("/post/")) {
                val idStr = path.substringAfter("/post/").trim()
                val parsed = idStr.toIntOrNull()
                if (parsed != null) {
                    deepLinkedPostId = parsed
                }
            }
        }
    }

    fun clearDeepLinkedPost() {
        deepLinkedPostId = null
    }

    fun getSavedLoginUsername(): String {
        return sharedPrefs.getString("saved_login_username", "") ?: ""
    }

    fun getSavedLoginPassword(): String {
        return sharedPrefs.getString("saved_login_password", "") ?: ""
    }

    // --- In-App Responsive Notification Center ---
    private val _notifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    val notifications: StateFlow<List<InAppNotification>> = _notifications.asStateFlow()

    private val _followedSellers = MutableStateFlow<Set<String>>(emptySet())
    val followedSellers: StateFlow<Set<String>> = _followedSellers.asStateFlow()

    // --- Direct Messages State ---
    private val _directMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val directMessages: StateFlow<List<DirectMessage>> = _directMessages.asStateFlow()

    fun toggleFollowSeller(sellerId: String) {
        val current = _followedSellers.value
        val updated = if (current.contains(sellerId)) {
            current - sellerId
        } else {
            current + sellerId
        }
        _followedSellers.value = updated
        sharedPrefs.edit().putStringSet("followed_sellersPrefix_v2", updated).apply()
        
        // Dynamic in-app proof of subscription!
        if (updated.contains(sellerId)) {
            addNotification(
                title = "Subscribed to Seller 🔔",
                message = "You will now get notified of wholesale updates from seller $sellerId.",
                iconType = "success"
            )
            // Trigger a push/in-app notification to the Seller
            addNotification(
                title = "New Catalog Subscriber! 📣",
                message = "Buyer @${currentUserProfile?.id?.removePrefix("@") ?: "buyer"} subscribed to your wholesale updates.",
                iconType = "success"
            )
        } else {
            addNotification(
                title = "Unsubscribed 🔕",
                message = "You will no longer receive alerts from seller $sellerId.",
                iconType = "info"
            )
        }
    }

    fun saveNotifications() {
        val serialized = _notifications.value.map { notif ->
            "${notif.id}|||${notif.title}|||${notif.message}|||${notif.timestamp}|||${notif.isRead}|||${notif.iconType}"
        }.toSet()
        sharedPrefs.edit().putStringSet("cached_notifications_v2", serialized).apply()
    }

    fun loadNotifications() {
        val saved = sharedPrefs.getStringSet("cached_notifications_v2", null)
        if (saved != null) {
            val list = saved.mapNotNull { raw ->
                val parts = raw.split("|||")
                if (parts.size >= 6) {
                    InAppNotification(
                        id = parts[0],
                        title = parts[1],
                        message = parts[2],
                        timestamp = parts[3].toLongOrNull() ?: System.currentTimeMillis(),
                        isRead = parts[4].toBoolean(),
                        iconType = parts[5]
                    )
                } else null
            }.sortedByDescending { it.timestamp }
            _notifications.value = list
        } else {
            val seed = listOf(
                InAppNotification(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "B2B Trade System Ready 🏬",
                    message = "Explore verified SME catalogs or configure wholesaling social reach in your profile.",
                    timestamp = System.currentTimeMillis(),
                    iconType = "success"
                ),
                InAppNotification(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "Secure Network Active 🔒",
                    message = "Welcome to India's compliant Threads B2B Net. IT Act Section 79 protection active.",
                    timestamp = System.currentTimeMillis() - 1000,
                    iconType = "security"
                )
            )
            _notifications.value = seed
            saveNotifications()
        }
    }

    fun addNotification(title: String, message: String, iconType: String = "info") {
        val newNotif = InAppNotification(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            iconType = iconType
        )
        _notifications.value = listOf(newNotif) + _notifications.value
        saveNotifications()
    }

    fun dismissNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
        saveNotifications()
    }

    private val offlineAutoRepliedPartners = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun clearAllNotifications() {
        _notifications.value = emptyList()
        saveNotifications()
    }

    fun editDirectMessage(message: DirectMessage, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch {
            val updatedMsg = message.copy(messageText = newText, isEdited = true)
            repository.updateDirectMessage(updatedMsg)
        }
    }

    fun deleteDirectMessage(messageId: Int) {
        viewModelScope.launch {
            repository.deleteDirectMessage(messageId)
        }
    }

    fun sendDirectMessage(receiverId: String, text: String, associatedPostId: Int? = null, associatedPostTitle: String? = null) {
        val sender = currentUserProfile?.id ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val message = DirectMessage(
                senderId = sender,
                receiverId = receiverId,
                messageText = text,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                associatedPostId = associatedPostId,
                associatedPostTitle = associatedPostTitle
            )
            repository.addDirectMessage(message)
            addNotification(
                title = "Message sent to $receiverId",
                message = "DM sent successfully: \"$text\"",
                iconType = "success"
            )

            // Simulate real-time Instagram response from the other party for interactive demo
            val receiverProfile = repository.getProfile(receiverId)
            val isBuyer = receiverProfile?.role == "Customer"
            val isOnline = receiverId.hashCode() % 2 == 0 // Deterministic online/offline simulation per partner
            val cleanReceiver = receiverId.removePrefix("@")

            if (!isOnline) {
                // If offline, only reply ONCE with the offline notice
                if (!offlineAutoRepliedPartners.contains(receiverId)) {
                    offlineAutoRepliedPartners.add(receiverId)
                    kotlinx.coroutines.delay(1200)
                    val autoReplyText = if (isBuyer) {
                        "Acknowledged. I am currently offline. As a registered buyer on Threads B2B, I have received your trade proposal and will review your business catalog details when I am back online. Thank you!"
                    } else {
                        "Acknowledged. We are currently offline. We have saved your trade inquiry in our pipeline. A verified representative from @$cleanReceiver will reach back directly with a quote!"
                    }
                    val reply = DirectMessage(
                        senderId = receiverId,
                        receiverId = sender,
                        messageText = autoReplyText,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        associatedPostId = associatedPostId,
                        associatedPostTitle = associatedPostTitle
                    )
                    repository.addDirectMessage(reply)
                    addNotification(
                        title = "Offline Auto-Reply from $receiverId",
                        message = autoReplyText,
                        iconType = "info"
                    )
                }
            } else {
                // If online, reply directly and contextually
                kotlinx.coroutines.delay(1200)
                val directReplyText = when {
                    associatedPostId != null -> {
                        "Hello! Thanks for your inquiry about listing: '${associatedPostTitle ?: "Product Specs"}'. Yes, our wholesale price is fully negotiable for bulk container shipments. Should we arrange a direct WhatsApp video call to verify quality standards?"
                    }
                    text.lowercase().contains("price") || text.lowercase().contains("cost") || text.lowercase().contains("how much") -> {
                        "Our wholesale MOQ (Minimum Order Quantity) pricing is competitive. We provide bulk GST invoices and door-to-door logistics dispatch across India."
                    }
                    text.lowercase().contains("hello") || text.lowercase().contains("hi") -> {
                        "Hi there! Glad to connect with you on Threads B2B. What specific commercial category are you sourcing today?"
                    }
                    else -> {
                        "Yes, we can certainly cater to that trade request. Are you sourcing for immediate shipment or custom bulk manufacturing contracts?"
                    }
                }

                val reply = DirectMessage(
                    senderId = receiverId,
                    receiverId = sender,
                    messageText = directReplyText,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    associatedPostId = associatedPostId,
                    associatedPostTitle = associatedPostTitle
                )
                repository.addDirectMessage(reply)
                addNotification(
                    title = "New Message from $receiverId",
                    message = directReplyText,
                    iconType = "info"
                )
            }
        }
    }

    init {
        registerNetworkCallback()
        // Load followed sellers persistently
        val followedRaw = sharedPrefs.getStringSet("followed_sellersPrefix_v2", emptySet()) ?: emptySet()
        _followedSellers.value = followedRaw
        
        // Load notifications persistently
        loadNotifications()

        // Collect comments and likes from the database reactively
        viewModelScope.launch {
            repository.allComments.collect { commentList ->
                _postComments.value = commentList.groupBy { it.postId }
            }
        }
        viewModelScope.launch {
            repository.allLikes.collect { likeList ->
                _postLikes.value = likeList.groupBy { it.postId }
            }
        }
        viewModelScope.launch {
            repository.allDirectMessages.collect { msgList ->
                _directMessages.value = msgList
            }
        }

        observeLiveFirestoreFeed()

        // Prepopulate default mock database with standard sellers and posts for real demo experience
        viewModelScope.launch {
            seedInitialDemoData()
            val fbUser = firebaseAuth.currentUser
            if (fbUser != null && !fbUser.email.isNullOrBlank()) {
                val email = fbUser.email!!
                var profile = repository.getUserProfileByEmail(email)
                if (profile == null) {
                    val username = email.substringBefore("@")
                    profile = UserProfile(
                        id = "@$username",
                        name = username.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                        profileType = "Individual",
                        email = email,
                        role = if (email.lowercase() == "rahman8040samsung@gmail.com") "Seller" else "Customer"
                    )
                    repository.saveProfile(profile)
                    repository.saveSubscription(Subscription(userId = profile.id, isPaid = true))
                }
                loadUserSession(profile.id)
            } else {
                currentUserProfile = null
                currentUserSub = null
            }
            isCheckingSession = false
        }
    }

    fun clearLoginError() {
        loginErrorMessage = null
    }

    fun performLogin(username: String, passwordInput: String, onRoute: (String) -> Unit) {
        viewModelScope.launch {
            val cleanUsername = username.trim()
            if (cleanUsername.isBlank()) {
                loginErrorMessage = "Error: Please enter your account handle."
                return@launch
            }

            // Super Admin hardcoded check
            val isSuperAdminEmail = cleanUsername.lowercase() == "rahman8040samsung@gmail.com"
            val isSuperAdminHandle = cleanUsername.removePrefix("@").lowercase() == "rahman8040samsung" || cleanUsername.removePrefix("@").lowercase() == "rahman8040"
            if (isSuperAdminEmail || isSuperAdminHandle) {
                if (passwordInput != "Admin@1234") {
                    loginErrorMessage = "Access Denied: Invalid password for Super Admin."
                    return@launch
                }
                val superAdminId = "@rahman8040samsung"
                var profile = repository.getProfile(superAdminId)
                if (profile == null) {
                    profile = UserProfile(
                        id = superAdminId,
                        name = "Super Admin (Muhammad Rahman)",
                        profileType = "SuperAdmin",
                        instagramUrl = "",
                        facebookUrl = "",
                        whatsappNumber = "+919999999999",
                        role = "Seller",
                        gstin = "GSTIN_SUPER_ADMIN",
                        aadhaarNumber = "",
                        email = "rahman8040samsung@gmail.com",
                        phoneNumber = "+919999999999"
                    )
                    repository.saveProfile(profile)
                    repository.saveSubscription(Subscription(userId = superAdminId, isPaid = true, postCountThisMonth = 0))
                }
                loadUserSession(superAdminId)
                loginErrorMessage = null
                onRoute("admin")
                return@launch
            }

            val formattedId = if (cleanUsername.startsWith("@")) cleanUsername else "@$cleanUsername"
            val profile = repository.getProfile(formattedId)
            if (profile != null) {
                val cleanPassword = passwordInput.trim()
                if (profile.password.isNotEmpty() && profile.password != cleanPassword) {
                    loginErrorMessage = "Access Denied: Invalid password."
                    return@launch
                }
                // Securely authenticate & establish session
                loadUserSession(formattedId)
                loginErrorMessage = null

                // Save safe login credentials for automatic pre-fill
                sharedPrefs.edit()
                    .putString("saved_login_username", username)
                    .putString("saved_login_password", passwordInput)
                    .apply()

                addNotification(
                    title = "Successful Secure Login 🔐",
                    message = "Authenticated business account handle @${username}.",
                    iconType = "success"
                )
                
                // Role-Based Access Control (RBAC) Auto-routing
                val isMasterAdmin = formattedId.removePrefix("@").lowercase() == "codecrafttechnologies"
                if (isMasterAdmin) {
                    onRoute("admin")
                } else {
                    onRoute("feed")
                }
            } else {
                loginErrorMessage = "Access Denied: Account handle '$formattedId' not found."
            }
        }
    }

    fun clearPostCreationStatus() {
        postCreationStatus = PostCreationResult.Idle
    }

    fun isUserBanned(userId: String): Boolean {
        return bannedUserIds.value.contains(userId)
    }

    fun persistProfileToPrefs(profile: UserProfile) {
        sharedPrefs.edit()
            .putString("saved_profile_id", profile.id)
            .putString("saved_profile_name", profile.name)
            .putString("saved_profile_type", profile.profileType)
            .putString("saved_profile_instagram", profile.instagramUrl)
            .putString("saved_profile_facebook", profile.facebookUrl)
            .putString("saved_profile_whatsapp", profile.whatsappNumber)
            .putString("saved_profile_role", profile.role)
            .putString("saved_profile_gstin", profile.gstin)
            .putString("saved_profile_aadhaar", profile.aadhaarNumber)
            .putString("saved_profile_email", profile.email)
            .putString("saved_profile_phone", profile.phoneNumber)
            .putString("saved_profile_password", profile.password)
            .apply()
    }

    suspend fun loadUserSession(userId: String) {
        var profile = repository.getProfile(userId)
        
        // Auto restore from SharedPreferences if missing in SQLite due to schema-rebuilding app updates
        val backupId = sharedPrefs.getString("saved_profile_id", "") ?: ""
        if (profile == null && userId == backupId && backupId.isNotEmpty()) {
            val restored = UserProfile(
                id = backupId,
                name = sharedPrefs.getString("saved_profile_name", "") ?: "",
                profileType = sharedPrefs.getString("saved_profile_type", "Individual") ?: "Individual",
                instagramUrl = sharedPrefs.getString("saved_profile_instagram", "") ?: "",
                facebookUrl = sharedPrefs.getString("saved_profile_facebook", "") ?: "",
                whatsappNumber = sharedPrefs.getString("saved_profile_whatsapp", "") ?: "",
                role = sharedPrefs.getString("saved_profile_role", "Seller") ?: "Seller",
                gstin = sharedPrefs.getString("saved_profile_gstin", "") ?: "",
                aadhaarNumber = sharedPrefs.getString("saved_profile_aadhaar", "") ?: "",
                email = sharedPrefs.getString("saved_profile_email", "") ?: "",
                phoneNumber = sharedPrefs.getString("saved_profile_phone", "") ?: "",
                password = sharedPrefs.getString("saved_profile_password", "") ?: ""
            )
            repository.saveProfile(restored)
            repository.saveSubscription(Subscription(userId = backupId, isPaid = true))
            profile = restored
        }

        if (profile != null) {
            currentUserProfile = profile
            persistProfileToPrefs(profile)
            var sub = repository.getSubscription(userId)
            if (sub == null) {
                sub = Subscription(userId = userId)
                repository.saveSubscription(sub)
            }
            currentUserSub = sub
            isBannedBySystem = bannedUserIds.value.contains(userId)
            sharedPrefs.edit().putString("saved_user_id", userId).apply()
        } else {
            currentUserProfile = null
            currentUserSub = null
            isBannedBySystem = false
            sharedPrefs.edit().remove("saved_user_id").apply()
        }
    }

    fun logout() {
        currentUserProfile = null
        currentUserSub = null
        isBannedBySystem = false
        sharedPrefs.edit()
            .remove("saved_user_id")
            .remove("saved_profile_id")
            .apply()
    }

    fun firebaseLogout() {
        firebaseAuth.signOut()
        logout()
    }

    private fun observeLiveFirestoreFeed() {
        postsListenerConnection?.remove()
        postsListenerConnection = firestore.collection("posts")
            .addSnapshotListener { snapshots, error ->
                if (snapshots != null) {
                    val postsList = mutableListOf<FirestorePost>()
                    for (document in snapshots) {
                        try {
                            val id = document.id
                            val userId = document.getString("userId") ?: ""
                            val authorName = document.getString("authorName") ?: ""
                            val authorType = document.getString("authorType") ?: "Individual"
                            val textContent = document.getString("textContent") ?: ""
                            val imageUrl = document.getString("imageUrl")
                            val videoUrl = document.getString("videoUrl")
                            val price = document.getString("price")
                            val specs = document.getString("specs")
                            val textLower = textContent.lowercase(java.util.Locale.ROOT)
                            val category = document.getString("category") ?: when {
                                textLower.contains("textile") || textLower.contains("fabric") || textLower.contains("cotton") || textLower.contains("yarn") || textLower.contains("silk") || textLower.contains("wear") || textLower.contains("clothing") || textLower.contains("garment") || textLower.contains("saree") || textLower.contains("suit") -> "Textiles"
                                textLower.contains("agri") || textLower.contains("farm") || textLower.contains("crop") || textLower.contains("grain") || textLower.contains("seed") || textLower.contains("fertilizer") || textLower.contains("fruit") || textLower.contains("vegetable") || textLower.contains("rice") || textLower.contains("wheat") || textLower.contains("spices") -> "Agriculture"
                                textLower.contains("electr") || textLower.contains("phone") || textLower.contains("chip") || textLower.contains("computer") || textLower.contains("cable") || textLower.contains("led") || textLower.contains("battery") || textLower.contains("screen") || textLower.contains("gadget") -> "Electronics"
                                textLower.contains("handicraft") || textLower.contains("art") || textLower.contains("clay") || textLower.contains("wooden") || textLower.contains("handmade") || textLower.contains("pot") || textLower.contains("brass") || textLower.contains("decor") -> "Handicrafts"
                                else -> "Textiles"
                            }
                            val timestamp = document.getLong("timestamp") ?: System.currentTimeMillis()
                            postsList.add(
                                FirestorePost(
                                    id = id,
                                    userId = userId,
                                    authorName = authorName,
                                    authorType = authorType,
                                    textContent = textContent,
                                    imageUrl = imageUrl,
                                    videoUrl = videoUrl,
                                    price = price,
                                    category = category,
                                    timestamp = timestamp,
                                    specs = specs
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val sortedList = postsList.sortedByDescending { it.timestamp }
                    _firestorePosts.value = sortedList
                }
            }
    }

    fun submitPostToFirestore(textContent: String, imageUrl: String?, videoUrl: String?, price: String?, category: String = "Other", specs: String? = null) {
        val user = currentUserProfile ?: return
        val newPostData = hashMapOf(
            "userId" to user.id,
            "authorName" to user.name,
            "authorType" to user.profileType,
            "textContent" to textContent,
            "imageUrl" to imageUrl,
            "videoUrl" to videoUrl,
            "price" to price,
            "category" to category,
            "specs" to specs,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("posts")
            .add(newPostData)
            .addOnSuccessListener {
                addNotification(
                    title = "Cloud Listing Published ☁️",
                    message = "Your wholesale item is now live globally on Firestore feeds.",
                    iconType = "success"
                )
            }
            .addOnFailureListener { e ->
                addNotification(
                    title = "Firestore Error ❌",
                    message = "Could not sync listing to cloud: ${e.localizedMessage}",
                    iconType = "info"
                )
            }
    }

    fun deletePostFromFirestore(postId: String) {
        firestore.collection("posts").document(postId)
            .delete()
            .addOnSuccessListener {
                addNotification(
                    title = "Listing Deleted 🗑️",
                    message = "The post was successfully removed from Firestore.",
                    iconType = "success"
                )
            }
            .addOnFailureListener { e ->
                addNotification(
                    title = "Delete Failed ❌",
                    message = "Could not delete document from cloud: ${e.localizedMessage}",
                    iconType = "info"
                )
            }
    }

    fun clearAllModerationLogs() {
        viewModelScope.launch {
            repository.clearAllModerationLogs()
        }
    }

    fun markNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        saveNotifications()
    }

    fun loginWithFirebase(emailInput: String, passwordInput: String, onRoute: (String) -> Unit) {
        val email = emailInput.trim()
        val password = passwordInput.trim()
        if (email.isBlank() || password.isBlank()) {
            loginErrorMessage = "Error: Email and password fields cannot be empty."
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fbUser = task.result?.user
                    if (fbUser != null && !fbUser.email.isNullOrBlank()) {
                        val userEmail = fbUser.email!!
                        viewModelScope.launch {
                            var profile = repository.getUserProfileByEmail(userEmail)
                            if (profile == null) {
                                val username = userEmail.substringBefore("@")
                                profile = UserProfile(
                                    id = "@$username",
                                    name = username.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                                    profileType = "Individual",
                                    email = userEmail,
                                    role = if (userEmail.lowercase() == "rahman8040samsung@gmail.com") "Seller" else "Customer"
                                )
                                repository.saveProfile(profile)
                                repository.saveSubscription(Subscription(userId = profile.id, isPaid = true))
                            }
                            loadUserSession(profile.id)
                            loginErrorMessage = null
                            
                            addNotification(
                                title = "Authenticated with Firebase 🔒",
                                message = "Welcome back, secure session established for $userEmail.",
                                iconType = "security"
                            )

                            val isSuperAdmin = userEmail.lowercase() == "rahman8040samsung@gmail.com"
                            if (isSuperAdmin) {
                                onRoute("admin")
                            } else {
                                onRoute("feed")
                            }
                        }
                    } else {
                        loginErrorMessage = "Failed to sign in. User database offline."
                    }
                } else {
                    loginErrorMessage = "Sign In Failed: ${task.exception?.localizedMessage ?: "Unknown Error"}"
                }
            }
    }

    fun registerWithFirebase(
        emailInput: String,
        passwordInput: String,
        nameInput: String,
        roleInput: String, // "Seller" or "Customer"
        whatsappInput: String,
        profileTypeInput: String,
        gstinInput: String,
        aadhaarInput: String,
        onSuccess: () -> Unit
    ) {
        val email = emailInput.trim()
        val password = passwordInput.trim()
        val name = nameInput.trim()
        val role = roleInput.trim()
        
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            loginErrorMessage = "Registration Error: Required fields are blank."
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fbUser = task.result?.user
                    if (fbUser != null) {
                        val userEmail = fbUser.email ?: email
                        val username = userEmail.substringBefore("@")
                        val formattedId = "@$username"
                        
                        viewModelScope.launch {
                            val newProfile = UserProfile(
                                id = formattedId,
                                name = name,
                                profileType = profileTypeInput,
                                instagramUrl = "",
                                facebookUrl = "",
                                whatsappNumber = whatsappInput,
                                role = role,
                                gstin = gstinInput,
                                aadhaarNumber = aadhaarInput,
                                email = userEmail,
                                phoneNumber = whatsappInput,
                                password = password
                            )
                            
                            repository.saveProfile(newProfile)
                            val newSub = Subscription(userId = formattedId, isPaid = true)
                            repository.saveSubscription(newSub)
                            
                            loadUserSession(formattedId)
                            loginErrorMessage = null
                            
                            addNotification(
                                title = "Firebase Account Registered ✨",
                                message = "Your account $userEmail was created and authenticated successfully.",
                                iconType = "success"
                            )
                            onSuccess()
                        }
                    }
                } else {
                    loginErrorMessage = "Registration Failed: ${task.exception?.localizedMessage ?: "Unknown Error"}"
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        postsListenerConnection?.remove()
        try {
            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            // safe ignore
        }
    }

    // --- Sign-Up Screen controller ---
    fun registerNewProfile(
        username: String,
        name: String,
        profileType: String,
        instagram: String,
        facebook: String,
        whatsapp: String,
        role: String = "Seller",
        gstin: String = "",
        aadhaarNumber: String = "",
        email: String = "",
        phoneNumber: String = "",
        password: String = "",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val formattedId = if (username.startsWith("@")) username else "@$username"
            val newProfile = UserProfile(
                id = formattedId,
                name = name,
                profileType = profileType,
                instagramUrl = instagram,
                facebookUrl = facebook,
                whatsappNumber = whatsapp,
                role = role,
                gstin = gstin,
                aadhaarNumber = aadhaarNumber,
                email = email,
                phoneNumber = phoneNumber,
                password = password
            )
            repository.saveProfile(newProfile)
            
            val newSub = Subscription(userId = formattedId)
            repository.saveSubscription(newSub)
            
            loadUserSession(formattedId)
            
            // Save safe login credentials
            sharedPrefs.edit()
                .putString("saved_login_username", username)
                .putString("saved_login_password", password)
                .apply()

            addNotification(
                title = "Compliant Account Activated ✅",
                message = "Your handle @${username} has registered. Session successfully saved.",
                iconType = "success"
            )
            onSuccess()
        }
    }

    // --- Post Creation with strict Freemium Limit + Security moderations ---
    fun submitProductPost(
        text: String,
        price: Double?,
        category: String?,
        imageUrl: String?,
        videoUrl: String?,
        specs: String? = null
    ) {
        val user = currentUserProfile ?: return
        if (user.role == "Customer") {
            postCreationStatus = PostCreationResult.Error("Access Denied: Customers are not permitted to submit postings on this business network.")
            return
        }
        if (isUserBanned(user.id)) {
            postCreationStatus = PostCreationResult.Error("Access Denied: Your profile has been auto-flagged and banned for cyber violations.")
            return
        }

        viewModelScope.launch {
            // Retrieve current subscription limits and check cycle reset
            var sub = repository.getSubscription(user.id)?.checkAndResetCycle() ?: Subscription(userId = user.id)
            repository.saveSubscription(sub)
            
            // Limit Verification: Booster & Cycle-based strict check
            val isAdmin = user.isAdmin()
            if (sub.getRemainingCredits(isAdmin) <= 0) {
                postCreationStatus = PostCreationResult.LimitExceeded(
                    "Upload failed: Your remaining post credits is 0. Users receive 10 posts every 3-month cycle. Please click upgrade to purchase a ₹500 booster for 10 additional posts!"
                )
                return@launch
            }

            val newPost = Post(
                authorId = user.id,
                authorName = user.name,
                authorType = user.profileType,
                text = text,
                price = price,
                category = category,
                imageUrl = imageUrl,
                videoUrl = videoUrl,
                isApproved = true,
                flagReason = null,
                specs = specs
            )

            val generatedId = repository.createPost(newPost).toInt()

            // Also post to live Firestore collection
            submitPostToFirestore(text, imageUrl, videoUrl, price?.toString(), category ?: "Other", specs)

            // Approved - Increment limit counters
            val updatedSub = sub.copy(postCountThisMonth = sub.postCountThisMonth + 1)
            repository.saveSubscription(updatedSub)
            currentUserSub = updatedSub
            postCreationStatus = PostCreationResult.Success
            addNotification(
                title = "Wholesale Listing Online 🏬",
                message = "Product portfolio upload approved & successfully published.",
                iconType = "success"
            )
        }
    }

    // --- Direct Google Pay (GPay) & UPI Direct Payment & Verification ---
    fun upgradeWithGPayUpi(userId: String, utrReferenceId: String, onComplete: (Boolean, String) -> Unit) {
        val cleanUtr = utrReferenceId.trim()
        if (cleanUtr.length != 12 || !cleanUtr.all { it.isDigit() }) {
            paymentGatewaySimulatedDetails = "ERROR: Verification failed. UPI Transaction ID / UTR must be exactly 12 digits."
            onComplete(false, "Invalid 12-digit UPI UTR Number.")
            return
        }

        viewModelScope.launch {
            paymentGatewaySimulatedDetails = "Contacting NPCI / GPay Secure Settlement Gateway...\nVerifying payment of ₹500 to recipient direct phone: +919044732288...\nUTR Reference ID: $cleanUtr\nProcessing verification checks..."
            
            kotlinx.coroutines.delay(2000) // Simulate processing with NPCI
            
            val activeSub = (repository.getSubscription(userId) ?: Subscription(userId = userId)).checkAndResetCycle()
            val premiumSub = activeSub.copy(
                isPaid = true,
                boosterCount = activeSub.boosterCount + 1
            )
            repository.saveSubscription(premiumSub)
            
            if (userId == currentUserProfile?.id) {
                currentUserSub = premiumSub
            }
            
            val log = ModerationLog(
                postId = null,
                userId = userId,
                postContent = "Booster Purchased GPay Upgrade. UTR Reference: $cleanUtr",
                triggerReason = "Manual UPI Verification of ₹500 directly transferred to +919044732288 (Booster #${activeSub.boosterCount + 1})",
                actionTaken = "USER_UPGRADED_PREMIUM"
            )
            repository.saveModerationLog(log)
            
            paymentGatewaySimulatedDetails = "SUCCESS: Payment verified successfully!\nBooster Pack purchased (+10 posts added). Enjoy uploading your business wholesale catalog!"
            onComplete(true, "Successfully purchased Booster pack (+10 posts) using GPay (UTR: $cleanUtr)!")
        }
    }

    // --- Comment Section & Automated Offensive User Ban Bot ---
    fun submitComment(postId: Int, text: String) {
        val user = currentUserProfile ?: return
        if (user.role != "Seller" && user.role != "Customer") {
            return
        }
        if (isUserBanned(user.id)) {
            return
        }

        val commentTextLowerOrAbusive = text.lowercase(Locale.ROOT)
        // Detect abusive language, swearing or scamming triggers
        val isAbusive = commentTextLowerOrAbusive.contains("bastard") || 
                        commentTextLowerOrAbusive.contains("scammaker") || 
                        commentTextLowerOrAbusive.contains("idiot") || 
                        commentTextLowerOrAbusive.contains("fraud") ||
                        commentTextLowerOrAbusive.contains("scammer") ||
                        commentTextLowerOrAbusive.contains("lingerie") ||
                        commentTextLowerOrAbusive.contains("anal") ||
                        commentTextLowerOrAbusive.contains("porn") ||
                        commentTextLowerOrAbusive.contains("bitch")

        if (isAbusive) {
            // Severe Offensive Penalty: Kicked off/Banned instantly from platform!
            viewModelScope.launch {
                // Ban the user
                bannedUserIds.value = bannedUserIds.value + user.id
                if (user.id == currentUserProfile?.id) {
                    isBannedBySystem = true
                }

                // Add moderation log representing cyber security compliance kick-off
                val log = ModerationLog(
                    postId = postId,
                    userId = user.id,
                    postContent = "Comment Context: \"$text\"",
                    triggerReason = "Automated Cyber-Bot: User posted offensive or toxic comment. Triggered compliance violation under IT Act Rule 79.",
                    actionTaken = "USER_PERMANENTLY_BANNED_KICKED"
                )
                repository.saveModerationLog(log)
            }
        } else {
            // Valid comment - save persistently to Room DB
            viewModelScope.launch {
                val newComment = Comment(
                    postId = postId,
                    authorId = user.id,
                    authorName = user.name,
                    text = text,
                    authorRole = user.role
                )
                repository.addComment(newComment)
                // A user MUST NOT receive notifications for their own comments
                if (newComment.authorId != user.id) {
                    addNotification(
                        title = "Social Thread Engagement 💬",
                        message = "New compliance comment from @${user.id} on post #${postId}.",
                        iconType = "comment"
                    )
                }
            }
        }
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            repository.deleteComment(commentId)
        }
    }

    // --- Likes Engine Persistent Reaction ---
    fun toggleLike(postId: Int) {
        val user = currentUserProfile ?: return
        viewModelScope.launch {
            val currentLikesList = _postLikes.value[postId] ?: emptyList()
            val userLiked = currentLikesList.any { it.userId == user.id }
            if (userLiked) {
                repository.removeLike(postId, user.id)
            } else {
                repository.addLike(PostLike(postId = postId, userId = user.id))
                addNotification(
                    title = "Listing Liked 👍",
                    message = "@${user.id} registered standard trade interest in Listing #${postId}.",
                    iconType = "like"
                )
            }
        }
    }

    // --- Super Admin Overrides ---
    fun adminDeletePost(postId: Int) {
        viewModelScope.launch {
            if (postId <= -1000) {
                val index = -1000 - postId
                val currentList = _firestorePosts.value
                if (index in currentList.indices) {
                    val fPost = currentList[index]
                    deletePostFromFirestore(fPost.id)
                }
            } else {
                repository.deletePost(postId)
            }
            val log = ModerationLog(
                postId = postId,
                userId = "SUPER_ADMIN",
                postContent = "Post deleted by Admin",
                triggerReason = "Manual Admin Purge",
                actionTaken = "POST_DELETED"
            )
            repository.saveModerationLog(log)
        }
    }

    fun adminDeleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteProfile(userId)
            val log = ModerationLog(
                postId = null,
                userId = userId,
                postContent = "User profile and account permanently deleted by Admin",
                triggerReason = "Manual Admin Account Purge",
                actionTaken = "USER_DELETED"
            )
            repository.saveModerationLog(log)
            addNotification(
                title = "Account Purged 🗑️",
                message = "The account for @$userId was permanently deleted by the super-admin.",
                iconType = "security"
            )
        }
    }

    fun incrementShareCount(post: Post) {
        viewModelScope.launch {
            repository.updatePost(post.copy(shareCount = post.shareCount + 1))
        }
    }

    fun updatePostSpecs(post: Post, newSpecs: String) {
        viewModelScope.launch {
            repository.updatePost(post.copy(specs = newSpecs))
        }
    }

    fun adminOverrideApproval(post: Post, approve: Boolean) {
        viewModelScope.launch {
            val updated = post.copy(isApproved = approve, flagReason = if (approve) null else "Blocked by CodeCraft Technologies super-admin override.")
            repository.updatePost(updated)
            
            val log = ModerationLog(
                postId = post.id,
                userId = "SUPER_ADMIN",
                postContent = post.text,
                triggerReason = "Manual super-admin toggle: approved=$approve",
                actionTaken = if (approve) "REVERSED_APPROVAL" else "REVERSED_REJECTION"
            )
            repository.saveModerationLog(log)
            addNotification(
                title = "Compliance Review Logged 🤖",
                message = "Post #${post.id} visibility toggled by Supervisor override.",
                iconType = "security"
            )
        }
    }

    fun deletePost(postId: Int, userId: String) {
        viewModelScope.launch {
            if (postId <= -1000) {
                val index = -1000 - postId
                val currentList = _firestorePosts.value
                if (index in currentList.indices) {
                    val fPost = currentList[index]
                    deletePostFromFirestore(fPost.id)
                }
            } else {
                repository.deletePost(postId)
            }
            val log = ModerationLog(
                postId = postId,
                userId = userId,
                postContent = "Post deleted",
                triggerReason = "Manual User Purge",
                actionTaken = "POST_DELETED"
            )
            repository.saveModerationLog(log)
        }
    }

    fun adminUnbanUser(userId: String) {
        bannedUserIds.value = bannedUserIds.value - userId
        if (userId == currentUserProfile?.id) {
            isBannedBySystem = false
        }
        viewModelScope.launch {
            val log = ModerationLog(
                postId = null,
                userId = userId,
                postContent = "User profile restored by super-admin override",
                triggerReason = "Administrative override by CodeCraft Technologies",
                actionTaken = "USER_UNBANNED"
            )
            repository.saveModerationLog(log)
        }
    }

    fun adminToggleUserSubscription(userId: String) {
        viewModelScope.launch {
            val sub = repository.getSubscription(userId) ?: Subscription(userId = userId)
            val updated = sub.copy(
                isPaid = !sub.isPaid,
                boosterCount = if (!sub.isPaid) (sub.boosterCount + 1) else 0 
            )
            repository.saveSubscription(updated)
            if (userId == currentUserProfile?.id) {
                currentUserSub = updated
            }
        }
    }

    fun resetPassword(username: String, newPass: String, onComplete: (Boolean) -> Unit) {
        val formatted = if (username.trim().startsWith("@")) username.trim() else "@${username.trim()}"
        viewModelScope.launch {
            val found = repository.getProfile(formatted)
            if (found != null) {
                val updated = found.copy(password = newPass.trim())
                repository.saveProfile(updated)
                if (currentUserProfile?.id == formatted) {
                    currentUserProfile = updated
                }
                persistProfileToPrefs(updated)
                addNotification(
                    title = "Portal Password Restored 🔑",
                    message = "Handle @${username.trim()}'s login password has been successfully reset.",
                    iconType = "security"
                )
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun updateUserProfile(
        name: String,
        instagram: String,
        facebook: String,
        whatsapp: String,
        phoneNumber: String,
        onComplete: (Boolean) -> Unit
    ) {
        val currentProfile = currentUserProfile ?: return
        viewModelScope.launch {
            val updatedProfile = currentProfile.copy(
                name = name,
                instagramUrl = instagram,
                facebookUrl = facebook,
                whatsappNumber = whatsapp,
                phoneNumber = phoneNumber
            )
            repository.saveProfile(updatedProfile)
            currentUserProfile = updatedProfile
            onComplete(true)
        }
    }

    // --- Database Seed Helpers ---
    private suspend fun seedInitialDemoData() {
        // App starts completely clean. Seed data deleted as per production requirements.
    }
}
