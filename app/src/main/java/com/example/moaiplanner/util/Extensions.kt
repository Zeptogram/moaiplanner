package com.example.moaiplanner.util
import android.app.NotificationManager
import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.commonsware.cwac.anddown.AndDown
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Reader

fun View.showKeyboard() {
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    requestFocus()
}

fun View.hideKeyboard() =
    (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        .hideSoftInputFromWindow(windowToken, 0)


fun enableLight(enable: Boolean) {
    if(enable)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    else
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
}

fun disableNotifications(enable: Boolean, context: Context) {
    if(!enable){
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.cancelAll()
    }
}


suspend fun AssetManager.readAssetToString(asset: String): String? {
    return withContext(Dispatchers.IO) {
        open(asset).reader().use(Reader::readText)
    }
}

const val HOEDOWN_FLAGS = AndDown.HOEDOWN_EXT_STRIKETHROUGH or AndDown.HOEDOWN_EXT_TABLES or
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

fun getFolderSize(folder: StorageReference, callback: (Long, Int) -> Unit) {
    var totalSize = 0L
    var fileCount = 0
    folder.listAll()
        .addOnSuccessListener { (items, prefixes) ->
            val fileTasks = items.map { it.metadata }
            Tasks.whenAllSuccess<StorageMetadata>(fileTasks)
                .addOnSuccessListener { metadatas ->
                    metadatas.forEach { metadata ->
                        totalSize += metadata.sizeBytes
                        fileCount++
                    }
                    prefixes.forEach { prefix ->
                        getFolderSize(prefix) { size, count ->
                            totalSize += size
                            fileCount += count
                        }
                    }
                    callback(totalSize, fileCount)
                }
                .addOnFailureListener {
                    callback(-1, 0)
                }
        }
        .addOnFailureListener {
            callback(-1, 0)
        }
}