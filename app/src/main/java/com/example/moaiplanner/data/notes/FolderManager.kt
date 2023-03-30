package com.example.moaiplanner.data.notes

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.moaiplanner.R
import com.example.moaiplanner.adapter.FolderViewAdapter
import com.example.moaiplanner.data.user.UserAuthentication
import com.example.moaiplanner.model.MarkdownViewModel
import com.example.moaiplanner.util.FolderItem
import com.example.moaiplanner.util.Utils
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.FileDescriptor
import java.io.FileInputStream
import java.text.DecimalFormat

@SuppressLint("NotifyDataSetChanged")
class FolderManager(private val activity: Activity, private val view: View) {


    private var firebase = UserAuthentication(activity.application)
    private var storage = Firebase.storage
    private var storageRef = storage.reference
    private var realtimeDb = FirebaseDatabase.getInstance()
    private var favouritesRef = realtimeDb.getReference("users/" + firebase.getCurrentUid().toString())
    private lateinit var folderPath: String
    private var currentFolder = ""


    fun getCollections(
        data: ArrayList<FolderItem>,
        adapter: FolderViewAdapter,
        folderName: String,
        shownFiles: ArrayList<FolderItem>,
        currentData: ArrayList<FolderItem>
    ) {
        data.clear()
        shownFiles.clear()
        folderPath = "/${firebase.getCurrentUid()}/Notes/${currentFolder}"
        val folder = storageRef.child("${firebase.getCurrentUid()}/Notes/${folderName}")
        Log.d("collectionNotesRef", folder.toString())
        folder.listAll()
            .addOnSuccessListener { (items, prefixes) ->
                prefixes.forEach { prefix ->
                    Log.d("FIRESTORAGE-PREFIX", prefix.toString())
                    val fileItem = FolderItem(
                        prefix.toString().split("/").last().replace("%20", " "),
                        "",
                        false,
                        R.drawable.folder
                    )
                    fileItem.userId = firebase.getCurrentUid().toString()
                    fileItem.path = currentFolder.substringBeforeLast("/")
                    val value: FolderItem? = checkItemPresence(currentData, fileItem)
                    if (value == null) {
                        data.add(fileItem)
                        getFolderSize(prefix) { bytes, files ->
                            val df = DecimalFormat("#,##0.##")
                            df.maximumFractionDigits = 2
                            val kb = bytes.toDouble() / 1024
                            val info = df.format(kb) + "KB - " + files.toString() + " Notes"
                            fileItem.folder_files = info
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        data.add(value)
                    }
                }
                items.forEach { item ->
                    Log.d("FIRESTORAGE-ITEM", item.toString())
                    if (item.toString().split("/").last()
                            .contains("^[^.]*\$|.*\\.md\$".toRegex())
                    ) {
                        val fileItem = FolderItem(
                            item.toString().split("/").last().replace("%20", " "),
                            "",
                            false,
                            R.drawable.baseline_insert_drive_file_24
                        )
                        fileItem.userId = firebase.getCurrentUid().toString()
                        fileItem.path = currentFolder.substringBeforeLast("/")
                        Log.d("PRESENZA", checkItemPresence(currentData, fileItem).toString())
                        val value: FolderItem? = checkItemPresence(currentData, fileItem)
                        if (value == null) {
                            val dbItem = favouritesRef.child("favourites/$currentFolder").push()
                            fileItem.id = dbItem.key.toString()
                            data.add(fileItem)
                            item.metadata.addOnSuccessListener {
                                val df = DecimalFormat("#,##0.##")
                                df.maximumFractionDigits = 2
                                val kbytes: Double = it.sizeBytes.toDouble() / 1024
                                val size = df.format(kbytes) + "kB"
                                fileItem.folder_files = size
                                dbItem.setValue(fileItem)
                                adapter.notifyDataSetChanged()
                            }
                        } else {
                            data.add(value)
                        }
                    }
                }
                shownFiles.clear()
                shownFiles.addAll(data)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.d("FIRESTORAGE-ERROR", "Error getting file list")
                Utils.showPopup(view, activity, activity.getString(R.string.file_load_error))
            }
    }

    fun getCurrentFolder(): String {
        return currentFolder
    }

    fun setCurrentFolder(folder: String) {
        currentFolder = folder
    }

    fun getFolderPath(): String {
        return folderPath
    }

    fun getUserData(): UserAuthentication {
        return firebase
    }

    private fun getStorageReference(): StorageReference {
        return storageRef
    }


    suspend fun createFile(fileName: EditText, isFolder: Boolean = false) {
        val noteDir: StorageReference
        var text = " "
        if (!isFolder) {
            noteDir = getStorageReference().child("${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${fileName.text}.md")
            text = "# Note created with Moai Planner"
            try {
                noteDir.putBytes(text.toByteArray()).await()
                Log.d("NOTE-CREATION", "Nota creata")
                Utils.showPopup(view, activity, activity.getString(R.string.note_create))

            } catch (e: Exception) {
                Utils.showPopup(view, activity, activity.getString(R.string.note_error))
            }
        } else {
            noteDir = getStorageReference().child("${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${fileName.text}/temp.tmp")
            try {
                noteDir.putBytes(text.toByteArray()).await()
                Log.d("FOLDER-CREATION", "Folder creato")
                Utils.showPopup(view, activity, activity.getString(R.string.folder_create))
            } catch (e: Exception) {
                Utils.showPopup(view, activity, activity.getString(R.string.folder_error))
            }
        }
    }

    @SuppressLint("Recycle")
    suspend fun uploadNote(data: Intent) {
        val uri = data.data
        Log.d("NOTE URI", uri.toString())
        var fileName = "null.md"
        if (uri != null) {
            val document = DocumentFile.fromSingleUri(activity, uri)
            if (document != null) {
                fileName = document.name.toString()
            }
        }
        Log.d("NOTE URI", fileName)
        val stream = FileInputStream(uri?.let {
            activity.contentResolver?.openFileDescriptor(
                it,
                "r"
            )?.fileDescriptor ?: FileDescriptor()
        })
        try {
            val noteDir = getStorageReference().child("${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${fileName}")
            noteDir.putStream(stream).await()
            Utils.showPopup(view, activity, activity.getString(R.string.note_uploaded_successfully))
            withContext(Dispatchers.IO) {
                stream.close()
            }
        } catch (e: Exception) {
            Utils.showPopup(view, activity, activity.getString(R.string.note_upload_failed))
            withContext(Dispatchers.IO) {
                stream.close()
            }
        }
    }


    suspend fun deleteNote(
        adapter: FolderViewAdapter,
        shownFiles: ArrayList<FolderItem>,
        currentData: ArrayList<FolderItem>,
        position: Int
    ): Boolean {
        val noteDir = getStorageReference().child(
            "${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${
                adapter.getFileName(position)
            }"
        )
        Log.d("NOTE-DIR", noteDir.toString())
        return try {
            val id = shownFiles[position].id
            adapter.onItemDelete(id)
            noteDir.delete().await()
            currentData.remove(shownFiles[position])
            Utils.showPopup(view, activity, activity.getString(R.string.note_deleted))
            true

        } catch (e: Exception) {
            Utils.showPopup(view, activity, activity.getString(R.string.delete_fail))
            false
        }
    }

    suspend fun deleteDirectory(
        adapter: FolderViewAdapter,
        shownFiles: ArrayList<FolderItem>,
        files: ArrayList<FolderItem>,
        currentData: ArrayList<FolderItem>,
        position: Int
    ): Boolean {
        val noteDir = getStorageReference().child(
            "${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${
                adapter.getFileName(position)
            }"
        )
        Log.d("NOTE-DIR", noteDir.toString())
        return try {
            val result = noteDir.listAll().await()
            result.items.forEach { item ->
                item.delete().await()
            }
            deleteFolder(result.prefixes, adapter, shownFiles, files, currentData)
            Utils.showPopup(view, activity, activity.getString(R.string.folder_delete))
            true

        } catch (e: java.lang.Exception) {
            Utils.showPopup(view, activity, activity.getString(R.string.delete_fail))
            false
        }
    }


    private fun checkItemPresence(
        currentData: ArrayList<FolderItem>,
        item: FolderItem
    ): FolderItem? {
        for (i in currentData) {
            if (i.folder_name == item.folder_name &&
                i.path == item.path &&
                i.icon == item.icon &&
                i.userId == item.userId
            )
                return i
        }
        return null
    }

    fun updateFolderNotesCache(folder: String) {
        var path = folder
        while(path != "/${firebase.getCurrentUid()}") {
            Utils.sizeCache.remove(path)
            path = path.substringBeforeLast("/")
        }
    }


    fun fetchFavouritesFromFirebase(currentData: ArrayList<FolderItem>) {
        val favouritesListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentData.clear()
                val favouritesData = snapshot.child("favourites/${currentFolder}").children
                for (item in favouritesData) {
                    val folderItem = item.getValue(FolderItem::class.java)
                    Log.d("PRESENZA", folderItem.toString())
                    if (folderItem != null && folderItem.icon != R.drawable.folder) {
                        if (folderItem.id.isNotBlank()) {
                            currentData.add(folderItem)
                        }
                    }
                }
                Log.d("CURRENT", currentData.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FileFragment", "loadFavouritesList:onCancelled", error.toException())
            }

        }
        favouritesRef.addValueEventListener(favouritesListListener)
    }

    private suspend fun deleteFolder(
        prefixes: List<StorageReference>,
        adapter: FolderViewAdapter,
        shownFiles: ArrayList<FolderItem>,
        files: ArrayList<FolderItem>,
        currentData: ArrayList<FolderItem>
    ) {
        prefixes.forEach { prefix ->
            val result = prefix.listAll().await()
            result.items.forEach { item ->
                item.delete().await()
            }
            deleteFolder(result.prefixes, adapter, shownFiles, files, currentData)
        }
    }

    fun loadHome(userDir: StorageReference, adapter: FolderViewAdapter, data: ArrayList<FolderItem>) {
        userDir.listAll()
            .addOnSuccessListener { (_, prefixes) ->
                if (prefixes.isEmpty()) {
                    val noteDir = storageRef.child("${firebase.getCurrentUid()}/Notes/temp.tmp")
                    val text = " "
                    val uploadFile = noteDir.putBytes(text.toByteArray())
                    uploadFile.addOnSuccessListener {
                        Log.d("FOLDER-CREATION", "Folder creato")
                        loadHome(userDir, adapter, data)
                    }
                } else {
                    prefixes.forEach { prefix ->
                        Log.d("FIRESTORAGE-PREFIX", prefix.toString())
                        val fileItem = FolderItem(
                            prefix.toString().split("/").last().replace("%20", " "),
                            "",
                            false,
                            R.drawable.folder
                        )
                        getFolderSize(prefix) { bytes, files ->
                            val df = DecimalFormat("#,##0.##")
                            df.maximumFractionDigits = 2
                            val kb = bytes.toDouble() / 1024
                            val info = df.format(kb) + "KB - " + files.toString() + " Notes"
                            fileItem.folder_files = info
                            data.add(fileItem)
                            adapter.notifyItemInserted(data.lastIndex)
                        }
                        prefix.listAll()
                    }
                }
            }
            .addOnFailureListener {
                Log.d("FIRESTORAGE-ERROR", "Error getting file list")
                Utils.showPopup(view, activity, activity.getString(R.string.file_load_error))
            }

    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun getTotalSize(folder: StorageReference): Pair<Long, Int> {
        val folderPath = folder.path
        if (Utils.sizeCache.containsKey(folderPath)  /*&& home.size != 3*/) {
            // Se la cartella è già stata processata in precedenza, ritorniamo le dimensioni dalla cache
            return Utils.sizeCache[folderPath]!!
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
        Utils.sizeCache[folderPath] = Pair(totalSize, noteCount)

        return Pair(totalSize, noteCount)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getFolderSize(
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

    fun saveOnFirebase(noteDir: String, markdownViewModel: MarkdownViewModel){
        var folder = "/${firebase.getCurrentUid()}/${noteDir.substringBeforeLast("/")}"
        var dir = storageRef.child("${firebase.getCurrentUid()}/${noteDir.substringBeforeLast("/")}/${markdownViewModel.fileName.value}")
        if(!dir.toString().contains("Notes")) {
            folder = "/${firebase.getCurrentUid()}/Notes"
            dir = storageRef.child("${firebase.getCurrentUid()}/Notes/${markdownViewModel.fileName.value}")
        }
        val uri : Uri = markdownViewModel.uri.value.toString().toUri()
        val uploadTask = dir.putFile(uri)
        uploadTask.addOnFailureListener {
            Utils.showPopup(view, activity, activity.getString(R.string.note_upload_failed))
            Log.d("Note", "Failed")
        }.addOnSuccessListener {
            updateFolderNotesCache(folder)
            Utils.showPopup(view, activity, activity.getString(R.string.note_uploaded_successfully))
            Log.d("Note", "Successful")
        }
    }


}



