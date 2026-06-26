package com.example.api

import android.content.Context

object ContentModerator {
    
    data class ModerationResult(
        val isApproved: Boolean,
        val reason: String? = null
    )

    fun moderatePost(context: Context, text: String, imageUrl: String?): ModerationResult {
        val lowercase = text.lowercase(java.util.Locale.ROOT)
        // Compliance filtering matching community guidelines
        val prohibitedKeywords = listOf("scam", "fraud", "fake", "spam", "drugs", "weapons", "illegal", "pimp", "porn")
        
        for (word in prohibitedKeywords) {
            if (lowercase.contains(word)) {
                return ModerationResult(
                    isApproved = false,
                    reason = "Upload failed: Content contains prohibited term '$word' violating community compliance guidelines."
                )
            }
        }
        
        return ModerationResult(isApproved = true)
    }

    fun deleteImageFromCache(context: Context, imageUrl: String?) {
        // Hook to clean up temporary photo cash storage if post is flagged
    }
}
