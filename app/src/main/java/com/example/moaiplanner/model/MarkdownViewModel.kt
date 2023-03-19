package com.example.moaiplanner.model


import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.moaiplanner.ui.main.NoteFragment
import com.example.moaiplanner.util.getName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.Reader
import java.util.concurrent.atomic.AtomicBoolean

const val PREF_KEY_AUTOSAVE_URI = "autosave.uri"

class MarkdownViewModel : ViewModel() {
    val fileName = MutableLiveData("Untitled.md")
    val markdownUpdates = MutableLiveData<String>()
    val editorActions = MutableLiveData<EditorAction>()
    val uri = MutableLiveData<Uri?>()
    var currentDir = MutableLiveData("Notes/")
    private val isDirty = AtomicBoolean(false)
    private val saveMutex = Mutex()

    fun updateMarkdown(markdown: String?) {
        this.markdownUpdates.postValue(markdown ?: "")
        isDirty.set(true)
    }

    suspend fun load(
        context: Context,
        uri: Uri?,
        sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    ): Boolean {
        if (uri == null) {
            sharedPrefs.getString(PREF_KEY_AUTOSAVE_URI, null)
                ?.let {
                    return load(context, Uri.parse(it), sharedPrefs)
                } ?: return false
        }
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    val fileInput = FileInputStream(it.fileDescriptor)
                    val fileName = uri.getName(context)
                    val content = fileInput.reader().use(Reader::readText)
                    /*if (content.isBlank()) {
                        // Non ho aperto nulla
                        return@withContext false
                    }*/
                    editorActions.postValue(EditorAction.Load(content))
                    markdownUpdates.postValue(content)
                    this@MarkdownViewModel.fileName.postValue(fileName)
                    this@MarkdownViewModel.uri.postValue(uri)
                    // Aperto, quindi no dirty
                    isDirty.set(false)
                    sharedPrefs.edit()
                        .putString(PREF_KEY_AUTOSAVE_URI, uri.toString())
                        .apply()
                    true
                } ?: run {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun save(
        context: Context,
        givenUri: Uri? = null,
        sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    ): Boolean = saveMutex.withLock {
        val uri = givenUri?.let {
            it
        } ?: this.uri.value?.let {
            it
        } ?: run {
            return@save false
        }
        return withContext(Dispatchers.IO) {
            try {
                val fileName = uri.getName(context)
                context.contentResolver.openOutputStream(uri, "rwt")
                    ?.writer()
                    ?.use {
                        it.write(markdownUpdates.value ?: "")
                        it.close()
                        it.flush()
                    }
                    ?: run {
                        return@withContext false
                    }

                this@MarkdownViewModel.fileName.postValue(fileName)
                this@MarkdownViewModel.uri.postValue(uri)
                isDirty.set(false)
                //timber.i("Saved file $fileName to uri $uri")
                //timber.i("Persisting autosave uri in shared prefs: $uri")
                sharedPrefs.edit()
                    .putString(PREF_KEY_AUTOSAVE_URI, uri.toString())
                    .apply()
                true
            } catch (e: Exception) {
                //timber.e(e, "Failed to save file at uri: $uri")
                false
            }
        }
    }

    suspend fun autosave(context: Context, sharedPrefs: SharedPreferences) {
        if (saveMutex.isLocked) {
            //timber.i("Ignoring autosave since manual save is already in progress")
            return
        }
        val isAutoSaveEnabled = sharedPrefs.getBoolean(NoteFragment.KEY_AUTOSAVE, true)
        //timber.d("Autosave called. isEnabled? $isAutoSaveEnabled")
        if (!isDirty.get() || !isAutoSaveEnabled) {
            //timber.i("Ignoring call to autosave. Contents haven't changed or autosave not enabled")
            return
        }

        if (save(context)) {
            //timber.i("Autosave with cached uri succeeded: ${uri.value}")
        } else {
            // The user has left the app, with autosave enabled, and we don't already have a
            // Uri for them or for some reason we were unable to save to the original Uri. In
            // this case, we need to just save to internal file storage so that we can recover
            val fileUri = Uri.fromFile(File(context.filesDir, fileName.value ?: "Untitled.md"))
            // timber.i("No cached uri for autosave, saving to $fileUri instead")
            save(context, fileUri)
        }
    }

    fun reset(untitledFileName: String, sharedPrefs: SharedPreferences) {
        // timber.i("Resetting view model to default state")
        fileName.postValue(untitledFileName)
        uri.postValue(null)
        markdownUpdates.postValue("")
        editorActions.postValue(EditorAction.Load(""))
        isDirty.set(false)
        //timber.i("Removing autosave uri from shared prefs")
        sharedPrefs.edit {
            remove(PREF_KEY_AUTOSAVE_URI)
        }
    }

    fun shouldPromptSave() = isDirty.get()

    fun loadDir(
        context: Context,
        sharedPrefs: SharedPreferences = context.getSharedPreferences("Note", Context.MODE_PRIVATE)
    ): String?
    {
        return sharedPrefs.getString("currentdir", "")
    }

    fun saveDir(
        context: Context,
        sharedPrefs: SharedPreferences = context.getSharedPreferences("Note", Context.MODE_PRIVATE)
    ): Boolean
    {
        sharedPrefs.edit().apply {
            putString("currentdir", currentDir.value.toString())
            apply()
        }
        return true
    }
    sealed class EditorAction {
        val consumed = AtomicBoolean(false)
        data class Load(val markdown: String) : EditorAction()
    }
}
