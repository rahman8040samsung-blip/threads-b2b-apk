package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface B2bDao {

    // --- User Profile ---
    @Query("SELECT * FROM user_profiles")
    fun getAllUserProfilesFlow(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getUserProfileById(id: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE email = :email LIMIT 1")
    suspend fun getUserProfileByEmail(email: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profiles WHERE id = :id")
    suspend fun deleteUserProfile(id: String)


    // --- Posts ---
    @Query("SELECT * FROM posts WHERE isApproved = 1 ORDER BY timestamp DESC")
    fun getApprovedPostsFlow(): Flow<List<Post>>

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY timestamp DESC")
    fun getPostsByAuthorFlow(authorId: String): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post): Long

    @Update
    suspend fun updatePost(post: Post)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deletePostById(id: Int)


    // --- Subscriptions ---
    @Query("SELECT * FROM subscriptions WHERE userId = :userId LIMIT 1")
    suspend fun getSubscriptionByUserId(userId: String): Subscription?

    @Query("SELECT * FROM subscriptions WHERE userId = :userId LIMIT 1")
    fun getSubscriptionFlow(userId: String): Flow<Subscription?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Query("UPDATE subscriptions SET postCountThisMonth = postCountThisMonth + 1 WHERE userId = :userId")
    suspend fun incrementPostCount(userId: String)


    // --- Moderation Logs ---
    @Query("SELECT * FROM moderation_logs ORDER BY timestamp DESC")
    fun getAllModerationLogsFlow(): Flow<List<ModerationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModerationLog(log: ModerationLog)

    @Query("DELETE FROM moderation_logs")
    suspend fun deleteAllModerationLogs()

    // --- Comments ---
    @Query("SELECT * FROM comments ORDER BY timestamp ASC")
    fun getAllCommentsFlow(): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment): Long

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteCommentById(id: Int)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteCommentsByPostId(postId: Int)

    // --- Likes ---
    @Query("SELECT * FROM post_likes ORDER BY timestamp ASC")
    fun getAllLikesFlow(): Flow<List<PostLike>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: PostLike)

    @Query("DELETE FROM post_likes WHERE postId = :postId AND userId = :userId")
    suspend fun deleteLike(postId: Int, userId: String)

    @Query("DELETE FROM post_likes WHERE postId = :postId")
    suspend fun deleteLikesByPostId(postId: Int)

    // --- Direct Messages ---
    @Query("SELECT * FROM direct_messages ORDER BY timestamp ASC")
    fun getAllDirectMessagesFlow(): Flow<List<DirectMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDirectMessage(message: DirectMessage): Long

    @Update
    suspend fun updateDirectMessage(message: DirectMessage)

    @Query("DELETE FROM direct_messages WHERE id = :id")
    suspend fun deleteDirectMessageById(id: Int)
}
