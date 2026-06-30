package com.example.update

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreUpdateRepository {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getLatestVersion(): VersionInfo? {
        return try {
            val snapshot = firestore
                .collection("app_config")
                .document("version_info")
                .get()
                .await()

            if (!snapshot.exists()) {
                return null
            }

            VersionInfo(
                latestVersion = snapshot.getString("latestVersion") ?: "",
                versionCode = snapshot.getLong("versionCode") ?: 0,
                minVersion = snapshot.getString("minVersion") ?: "",
                forceUpdate = snapshot.getBoolean("forceUpdate") ?: false,
                apkUrl = snapshot.getString("apkUrl") ?: "",
                releaseNotes = snapshot.getString("releaseNotes") ?: "",
                published = snapshot.getBoolean("published") ?: true
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
