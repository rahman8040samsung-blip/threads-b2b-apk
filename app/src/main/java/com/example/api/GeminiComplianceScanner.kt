package com.example.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiComplianceScanner {
    private const val TAG = "GeminiComplianceScanner"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun scanPost(text: String, imageUrl: String? = null): ContentModerator.ModerationResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is not set or is using placeholder. Skipping Gemini scan layer.")
            return@withContext ContentModerator.ModerationResult(isApproved = true)
        }

        val systemInstruction = """
            You are a strict B2B trade network content scanner.
            Analyze the following product post to check compliance with community guidelines.
            Guidelines strictly prohibit:
            - Severe intoxicants / Alcohol
            - Non-halal forbidden items (Pork / Pork-byproducts)
            - Gambling, betting, lottery, scams, Ponzi schemes, fraudulent claims
            - NSFW, explicit imagery, weapons, hate speech, or illicit substances.
            
            You MUST return a JSON object with EXACTLY these two keys:
            - "isApproved": boolean (true if content is safe and compliant, false if prohibited)
            - "reason": string (a concise explanation of why it is flagged, or null if approved)
            
            Return ONLY the valid JSON block. No markdown, no prose, no formatting tags.
        """.trimIndent()

        val promptText = if (imageUrl != null) {
            "Product text description: \"$text\"\nProduct attached image URL reference: \"$imageUrl\""
        } else {
            "Product text description: \"$text\""
        }

        try {
            val requestBodyJson = JSONObject()
            
            val contentArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", promptText)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentArray.put(contentObj)
            requestBodyJson.put("contents", contentArray)

            val systemInstructionObj = JSONObject()
            val systemPartsArray = JSONArray()
            val systemPartObj = JSONObject()
            systemPartObj.put("text", systemInstruction)
            systemPartsArray.put(systemPartObj)
            systemInstructionObj.put("parts", systemPartsArray)
            requestBodyJson.put("systemInstruction", systemInstructionObj)

            val generationConfig = JSONObject()
            generationConfig.put("responseMimeType", "application/json")
            requestBodyJson.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed with code: ${response.code}.")
                    return@withContext ContentModerator.ModerationResult(isApproved = true)
                }

                val responseBodyStr = response.body?.string() ?: return@withContext ContentModerator.ModerationResult(isApproved = true)
                Log.d(TAG, "Gemini Compliance Response: $responseBodyStr")

                val jo = JSONObject(responseBodyStr)
                val candidates = jo.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentResponseObj = firstCandidate.optJSONObject("content")
                    if (contentResponseObj != null) {
                        val parts = contentResponseObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val partText = parts.getJSONObject(0).optString("text")
                            if (!partText.isNullOrBlank()) {
                                val cleanedText = partText.trim()
                                    .removePrefix("```json")
                                    .removePrefix("```")
                                    .removeSuffix("```")
                                    .trim()
                                val responseJson = JSONObject(cleanedText)
                                val isApproved = responseJson.optBoolean("isApproved", true)
                                val reason = responseJson.optString("reason", "Contains prohibited materials.")
                                return@withContext ContentModerator.ModerationResult(
                                    isApproved = isApproved,
                                    reason = if (isApproved) null else reason
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini compliance scan: ", e)
        }

        return@withContext ContentModerator.ModerationResult(isApproved = true)
    }
}
