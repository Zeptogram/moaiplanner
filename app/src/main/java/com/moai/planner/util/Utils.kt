package com.moai.planner.util
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.commonsware.cwac.anddown.AndDown
import com.moai.planner.R
import com.moai.planner.data.user.UserAuthentication
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.component1
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*

@Suppress("DEPRECATION")
class Utils {
    companion object {

        val sizeCache = HashMap<String, Pair<Long, Int>>() // cache per le dimensioni

        fun View.showKeyboard() {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                0
            )
            requestFocus()
        }

        fun View.hideKeyboard() =
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(windowToken, 0)


        fun enableLight(enable: Boolean) {
            if (enable)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        fun disableNotifications(enable: Boolean, context: Context) {
            if (!enable) {
                val notificationManager =
                    ContextCompat.getSystemService(context, NotificationManager::class.java)
                notificationManager?.cancelAll()
            }
        }


        private const val HOEDOWN_FLAGS =
            AndDown.HOEDOWN_EXT_STRIKETHROUGH or AndDown.HOEDOWN_EXT_TABLES or
                    AndDown.HOEDOWN_EXT_UNDERLINE or AndDown.HOEDOWN_EXT_SUPERSCRIPT or
                    AndDown.HOEDOWN_EXT_FENCED_CODE

        suspend fun String.toHtml(): String {
            return withContext(Dispatchers.IO) {
                AndDown().markdownToHtml(this@toHtml, HOEDOWN_FLAGS, 0)
            }
        }

        suspend fun Uri.getName(context: Context): String {
            var fileName: String? = null
            try {
                if ("content" == scheme) {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.query(
                            this@getName,
                            null,
                            null,
                            null,
                            null
                        )?.use {
                            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            it.moveToFirst()
                            fileName = it.getString(nameIndex)
                        }
                    }
                } else if ("file" == scheme) {
                    fileName = lastPathSegment
                }
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
            return fileName ?: "Untitled.md"
        }

        private fun downloadImage(avatar: StorageReference, picture: ImageView) {
            avatar.downloadUrl.addOnSuccessListener { uri ->
                Picasso.get()
                    .load(uri.toString())
                    .priority(Picasso.Priority.HIGH)
                    .into(picture)
            }.addOnFailureListener { exception ->
                if (exception is StorageException &&
                    (exception).errorCode == StorageException.ERROR_OBJECT_NOT_FOUND
                ) {
                    Log.e("IMAGE", "File not found in storage")
                } else {
                    Log.e("IMAGE", "StorageException: ${exception.message}")
                }
            }
        }

        fun loadImage(
            userDir: StorageReference,
            firebase: UserAuthentication,
            avatar: StorageReference,
            picture: ImageView
        ) {
            userDir.listAll().addOnSuccessListener { (items) ->
                items.forEach { item ->
                    Log.d("IMAGE", item.toString())
                    if (item.toString().substringAfterLast("/") == "avatar.png") {
                        downloadImage(avatar, picture)
                    }
                }
                if (firebase.getProvider() == "google.com") {
                    val uri = firebase.getGoogleImage().toString()
                    Picasso.get().load(uri).priority(Picasso.Priority.HIGH).into(picture)
                }
            }.addOnFailureListener {
                Log.e("IMAGE", "Using default picture")
            }
        }

        fun showPopup(view: View, activity: Activity, title: String){
            Snackbar.make(view, title, Snackbar.LENGTH_SHORT)
                .setAction(activity.getString(R.string.ok)) {}
                .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                .show()
        }

    }
}


