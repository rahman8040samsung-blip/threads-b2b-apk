package com.example.data

import kotlinx.coroutines.flow.Flow

class B2bRepository(private val b2bDao: B2bDao) {

    val approvedPosts: Flow<List<Post>> = b2bDao.getApprovedPostsFlow()
    val allPosts: Flow<List<Post>> = b2bDao.getAllPostsFlow()
    val allUserProfiles: Flow<List<UserProfile>> = b2bDao.getAllUserProfilesFlow()
    val allModerationLogs: Flow<List<ModerationLog>> = b2bDao.getAllModerationLogsFlow()
    val allComments: Flow<List<Comment>> = b2bDao.getAllCommentsFlow()
    val allLikes: Flow<List<PostLike>> = b2bDao.getAllLikesFlow()

    fun getPostsByAuthor(authorId: String): Flow<List<Post>> = b2bDao.getPostsByAuthorFlow(authorId)

    suspend fun getProfile(id: String): UserProfile? = b2bDao.getUserProfileById(id)

    suspend fun getUserProfileByEmail(email: String): UserProfile? = b2bDao.getUserProfileByEmail(email)

    suspend fun saveProfile(profile: UserProfile) {
        b2bDao.insertUserProfile(profile)
    }

    suspend fun deleteProfile(id: String) {
        b2bDao.deleteUserProfile(id)
    }

    suspend fun createPost(post: Post): Long {
        return b2bDao.insertPost(post)
    }

    suspend fun updatePost(post: Post) {
        b2bDao.updatePost(post)
    }

    suspend fun deletePost(id: Int) {
        b2bDao.deletePostById(id)
        b2bDao.deleteCommentsByPostId(id)
        b2bDao.deleteLikesByPostId(id)
    }

    suspend fun addComment(comment: Comment): Long {
        return b2bDao.insertComment(comment)
    }

    suspend fun deleteComment(id: Int) {
        b2bDao.deleteCommentById(id)
    }

    suspend fun addLike(like: PostLike) {
        b2bDao.insertLike(like)
    }

    suspend fun removeLike(postId: Int, userId: String) {
        b2bDao.deleteLike(postId, userId)
    }

    suspend fun getSubscription(userId: String): Subscription? {
        return b2bDao.getSubscriptionByUserId(userId)
    }

    fun getSubscriptionFlow(userId: String): Flow<Subscription?> {
        return b2bDao.getSubscriptionFlow(userId)
    }

    suspend fun saveSubscription(subscription: Subscription) {
        b2bDao.insertSubscription(subscription)
    }

    suspend fun incrementPostCount(userId: String) {
        b2bDao.incrementPostCount(userId)
    }

    suspend fun saveModerationLog(log: ModerationLog) {
        b2bDao.insertModerationLog(log)
    }

    suspend fun clearAllModerationLogs() {
        b2bDao.deleteAllModerationLogs()
    }

    // --- Direct Messages ---
    val allDirectMessages: Flow<List<DirectMessage>> = b2bDao.getAllDirectMessagesFlow()

    suspend fun addDirectMessage(message: DirectMessage): Long {
        return b2bDao.insertDirectMessage(message)
    }

    suspend fun updateDirectMessage(message: DirectMessage) {
        b2bDao.updateDirectMessage(message)
    }

    suspend fun deleteDirectMessage(id: Int) {
        b2bDao.deleteDirectMessageById(id)
    }
}
