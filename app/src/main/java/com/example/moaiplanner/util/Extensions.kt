package com.example.moaiplanner.util
import android.app.NotificationManager
import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
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
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
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

val sizeCache = HashMap<String, Pair<Long, Int>>() // cache per le dimensioni

suspend fun getTotalSize(folder: StorageReference): Pair<Long, Int> {
    val folderPath = folder.path
    val home = folderPath.split("/")
    if (sizeCache.containsKey(folderPath)  && home.size != 3) {
        // Se la cartella è già stata processata in precedenza, ritorniamo le dimensioni dalla cache
        return sizeCache[folderPath]!!
    }

    var totalSize = 0L
    var noteCount = 0
    val (items, prefixes) = folder.listAll().await()
    val fileTasks = items.map { it.metadata }
    val metadatas = Tasks.whenAllSuccess<StorageMetadata>(fileTasks).await()
    metadatas.forEach { metadata ->
        if(metadata.name?.endsWith(".md") == true) {
            totalSize += metadata.sizeBytes
            noteCount++
        }
    }

    // Creiamo un array di coroutine job per processare le sotto-cartelle in parallelo
    val subFolderJobs = prefixes.map { prefix ->
        GlobalScope.async { getTotalSize(prefix) }
    }

    // Attendo che tutte le sotto-cartelle siano state processate e sommo le dimensioni e i conteggi
    val subFolderResults = subFolderJobs.awaitAll()
    subFolderResults.forEach { (subTotalSize, subNoteCount) ->
        totalSize += subTotalSize
        noteCount += subNoteCount
    }

    // Salviamo le dimensioni nella cache
    sizeCache[folderPath] = Pair(totalSize, noteCount)

    return Pair(totalSize, noteCount)
}

// La funzione principale adesso è asincrona e utilizza la coroutine appena definita
@OptIn(DelicateCoroutinesApi::class)
fun getFolderSize(
    folder: StorageReference,
    callback: (Long, Int) -> Unit
) {
    GlobalScope.launch(Dispatchers.Main) {
        try {
            val totalSize = getTotalSize(folder).first
            val fileCount = getTotalSize(folder).second
            callback(totalSize, fileCount)
        } catch (e: Exception) {
            callback(-1, 0)
        }
    }
}
