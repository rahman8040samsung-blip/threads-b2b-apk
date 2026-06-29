package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String, // format: @username
    val name: String,
    val profileType: String, // Individual, Manufacturer, Wholesaler, Retailer, Trader
    val instagramUrl: String = "",
    val facebookUrl: String = "",
    val whatsappNumber: String = "",
    val role: String = "Customer", // Customer, Seller
    val gstin: String = "",
    val aadhaarNumber: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = ""
)

fun UserProfile.isAdmin(): Boolean {
    val cleanId = id.removePrefix("@").lowercase()
    return cleanId == "codecrafttechnologies" ||
           cleanId == "rahman8040samsung" ||
           cleanId == "rahman8040" ||
           cleanId == "admin" ||
           cleanId.contains("admin") ||
           email.lowercase() == "rahman8040samsung@gmail.com" ||
           role.equals("admin", ignoreCase = true) ||
           profileType.equals("SuperAdmin", ignoreCase = true)
}

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: String,
    val authorName: String,
    val authorType: String,
    val text: String,
    val price: Double? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val isApproved: Boolean = true,
    val flagReason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isValid: Boolean = true,
    val specs: String? = null,
    val shareCount: Int = 0
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val userId: String,
    val isPaid: Boolean = false,
    val postCountThisMonth: Int = 0,
    val boosterCount: Int = 0,
    val startDate: Long = System.currentTimeMillis()
)

fun Subscription.getRemainingCredits(isAdmin: Boolean = false): Int {
    if (isAdmin) return 10000 // Unlimited for Admin
    val baseLimit = 10 + (boosterCount * 10)
    val remaining = baseLimit - postCountThisMonth
    return if (remaining < 0) 0 else remaining
}

fun Subscription.getLimitVal(isAdmin: Boolean = false): Int {
    if (isAdmin) return 10000
    return 10 + (boosterCount * 10)
}

fun Subscription.checkAndResetCycle(): Subscription {
    val threeMonthsInMillis = 90L * 24 * 60 * 60 * 1000 // 3 months
    val now = System.currentTimeMillis()
    if (now - startDate >= threeMonthsInMillis) {
        return this.copy(postCountThisMonth = 0, boosterCount = 0, startDate = now)
    }
    return this
}

@Entity(tableName = "moderation_logs")
data class ModerationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int?,
    val userId: String,
    val postContent: String,
    val triggerReason: String,
    val actionTaken: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val authorId: String,
    val authorName: String,
    val authorRole: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "post_likes", primaryKeys = ["postId", "userId"])
data class PostLike(
    val postId: Int,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "direct_messages")
data class DirectMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,       // format: @username
    val receiverId: String,     // format: @username
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val associatedPostId: Int? = null,
    val associatedPostTitle: String? = null,
    val isEdited: Boolean = false
)
