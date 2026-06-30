package com.example.update

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppUpdateManager {

    fun checkForUpdate(context: Context) {

        CoroutineScope(Dispatchers.Main).launch {

            try {

                val repository = FirestoreUpdateRepository()
                val latest = repository.getLatestVersion() ?: return@launch

                val currentVersion = BuildConfig.VERSION_CODE.toLong()

                if (!latest.published) return@launch

                if (latest.versionCode > currentVersion) {

                    AlertDialog.Builder(context)
                        .setTitle("New Update Available")
                        .setMessage(
                            "Version ${latest.latestVersion} is available.\n\n" +
                                    latest.releaseNotes
                        )
                        .setCancelable(!latest.forceUpdate)
                        .setPositiveButton("Update Now") { _, _ ->

                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(latest.apkUrl)
                            )

                            context.startActivity(intent)

                        }
                        .apply {

                            if (!latest.forceUpdate) {

                                setNegativeButton("Later", null)

                            }

                        }
                        .show()

                }

            } catch (e: Exception) {

                Log.e("AppUpdate", e.toString())

            }

        }

    }

}
