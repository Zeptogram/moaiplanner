package com.example.moaiplanner.data.notes

import FolderViewAdapter
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.documentfile.provider.DocumentFile
import com.example.moaiplanner.R
import com.example.moaiplanner.data.user.UserAuthentication
import com.example.moaiplanner.util.FolderItem
import com.example.moaiplanner.util.getFolderSize
import com.example.moaiplanner.util.sizeCache
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.io.FileInputStream
import java.text.DecimalFormat

class FolderManager(private val activity: Activity, private val view: View) {


    private var firebase = UserAuthentication(activity.application)
    private var storage = Firebase.storage
    private var storageRef = storage.reference
    private var realtimeDb = FirebaseDatabase.getInstance()
    private var favouritesRef = realtimeDb.getReference("users/" + firebase.getCurrentUid().toString())
    private lateinit var folderPath: String
    private var currentFolder = ""


    fun getCollections(data: ArrayList<FolderItem>, adapter: FolderViewAdapter, folderName: String, shownFiles: ArrayList<FolderItem>, currentData: ArrayList<FolderItem>){
        data.clear()
        shownFiles.clear()
        adapter.notifyDataSetChanged()
        folderPath = "/${firebase.getCurrentUid()}/Notes/${currentFolder}"
        val folder = storageRef.child("${firebase.getCurrentUid()}/Notes/${folderName}")
        Log.d("collectionNotesRef", folder.toString())
        folder.listAll()
            .addOnSuccessListener { (items, prefixes) ->
                prefixes.forEach { prefix ->

                    Log.d("FIRESTORAGE-PREFIX", prefix.toString())
                    var fileItem = FolderItem(prefix.toString().split("/").last().replace("%20", " "), "", false, R.drawable.folder)
                    fileItem.userId = firebase.getCurrentUid().toString()
                    fileItem.path = currentFolder.substringBeforeLast("/")
                    val value: FolderItem? = checkItemPresence(currentData, fileItem)
                    if(value == null) {
                        //var dbItem = favouritesRef.child("favourites/$currentFolder").push()
                        //fileItem.id = dbItem.key.toString()
                        data.add(fileItem)
                        getFolderSize(prefix) { bytes, files ->
                            val df = DecimalFormat("#,##0.##")
                            df.maximumFractionDigits = 2
                            var kb = bytes.toDouble() / 1024
                            val info = df.format(kb) + "KB - " + files.toString() + " Notes"
                            fileItem.folder_files = info
                           // dbItem.setValue(fileItem)
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        data.add(value)
                        adapter.notifyDataSetChanged()

                    }

                    //data.add(fileItem)

                    prefix.listAll()
                }

                items.forEach { item ->
                    Log.d("FIRESTORAGE-ITEM", item.toString())
                    if (item.toString().split("/").last().contains("^[^.]*\$|.*\\.md\$".toRegex())){
                        var fileItem = FolderItem(item.toString().split("/").last().replace("%20", " "), "", false, R.drawable.baseline_insert_drive_file_24)
                        fileItem.userId = firebase.getCurrentUid().toString()
                        fileItem.path = currentFolder.substringBeforeLast("/")
                        Log.d("PRESENZA", checkItemPresence(currentData, fileItem).toString())
                        val value: FolderItem? = checkItemPresence(currentData, fileItem)
                        if(value == null) {
                            var dbItem = favouritesRef.child("favourites/$currentFolder").push()
                            fileItem.id = dbItem.key.toString()
                            data.add(fileItem)
                            item.metadata.addOnSuccessListener {
                                val df = DecimalFormat("#,##0.##")
                                df.maximumFractionDigits = 2
                                var kbytes: Double = it.sizeBytes.toDouble() / 1024
                                val size = df.format(kbytes) + "kB"
                                fileItem.folder_files = size
                                dbItem.setValue(fileItem)
                                adapter.notifyDataSetChanged()
                            }
                        }
                        else {
                            data.add(value)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.d("FIRESTORAGE-ERROR", "Error getting file list")
                view?.let { it1 ->
                    Snackbar.make(it1, "Error Getting Files", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                        .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                        .show()
                }
            }
            .addOnSuccessListener {
                shownFiles.clear()
                shownFiles.addAll(data)
                adapter.notifyDataSetChanged()
            }
    }

    fun getCurrentFolder(): String{
        return currentFolder
    }

    fun setCurrentFolder(folder: String) {
        currentFolder = folder
    }

    fun getFolderPath(): String{
        return folderPath
    }

    fun getUserData(): UserAuthentication{
        return firebase
    }

    fun getStorageReference(): StorageReference{
        return storageRef
    }


    suspend fun createFile(fileName: EditText, isFolder: Boolean = false) {
        val noteDir: StorageReference
        var text = " "
        if(!isFolder) {
            noteDir = getStorageReference().child("${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${fileName.text}.md")
            text = "# Note created with Moai Planner"
            try {
                noteDir.putBytes(text.toByteArray()).await()
                Log.d("NOTE-CREATION", "Nota creata")
                view.let {
                    Snackbar.make(it, "Note created", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                        .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                        .show()
                }
            } catch (e: Exception) {
                view.let {
                    Snackbar.make(it, "Note error", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                        .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                        .show()
                }
            }
        } else {
            noteDir = getStorageReference().child("${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${fileName.text}/temp.tmp")
            try {
                noteDir.putBytes(text.toByteArray()).await()
                Log.d("FOLDER-CREATION", "Folder creato")
                view?.let {
                    Snackbar.make(it, "Folder created", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                        .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                        .show()
                }
            } catch (e: Exception) {
                view.let {
                    Snackbar.make(it, "Folder error", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                        .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                        .show()
                }
            }
        }
    }

    suspend fun uploadNote(data: Intent){
        val uri = data?.data
        Log.d("NOTE URI", uri.toString())
        var fileName = "null.md"
        activity?.let {
            if (uri != null) {
                val document = DocumentFile.fromSingleUri(it, uri)
                if (document != null) {
                    fileName = document.name.toString()
                }
            }
        }
        Log.d("NOTE URI", fileName)
        val stream = FileInputStream(uri?.let {
            activity?.contentResolver?.openFileDescriptor(
                it,
                "r"
            )?.fileDescriptor ?: FileDescriptor()
        })
        try {
            val noteDir = getStorageReference().child("${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${fileName}")
            noteDir.putStream(stream).await()
            // Register observers to listen for when the download is done or if it fails
            view.let {
                Snackbar.make(it, "Note uploaded successfully", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    //.setBackgroundTint(resources.getColor(R.color.pr))
                    .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                    .show()
            }
            withContext(Dispatchers.IO) {
                stream.close()
            }
        } catch (e: Exception) {
            view.let { it1 ->
                Snackbar.make(it1, "Note upload failed", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    //.setBackgroundTint(resources.getColor(R.color.pr))
                    .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                    .show()
            }
            withContext(Dispatchers.IO) {
                stream.close()
            }
        }
    }


    suspend fun deleteNote(adapter: FolderViewAdapter, shownFiles: ArrayList<FolderItem>, files: ArrayList<FolderItem>, currentData: ArrayList<FolderItem>, position: Int): Boolean {
        val noteDir = getStorageReference().child(
            "${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${
                adapter.getFileName(position)
            }"
        )
        Log.d("NOTE-DIR", noteDir.toString())
        try {
            val id = shownFiles[position].id
            adapter.onItemDelete(id)
            noteDir.delete().await()
            currentData.remove(shownFiles[position])
            view.let { it1 ->
                Snackbar.make(it1, "File deleted", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                    .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                    .show()
            }

            return true;

        } catch(e: Exception) {
        view.let { it1 ->
            Snackbar.make(it1, "Delete failed", Snackbar.LENGTH_SHORT)
                .setAction("OK") {
                    // Responds to click on the action
                }
                .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                .show()
            }
            return false
        }
    }

    suspend fun deleteDirectory(adapter: FolderViewAdapter, shownFiles: ArrayList<FolderItem>, files: ArrayList<FolderItem>, currentData: ArrayList<FolderItem>, position: Int): Boolean {
        val noteDir = getStorageReference().child("${getUserData().getCurrentUid()}/Notes/${getCurrentFolder()}${adapter.getFileName(position)}")
        Log.d("NOTE-DIR", noteDir.toString())
        try {
            val result = noteDir.listAll().await()
            result.items.forEach { item ->
                item.delete().await()
            }
            deleteFolder(result.prefixes, adapter, shownFiles, files, currentData)
            view.let { it1 ->
                Snackbar.make(it1, "Folder deleted", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    //.setBackgroundTint(resources.getColor(R.color.pr))
                    .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                    .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                    .show()
            }
            return true

        } catch(e: java.lang.Exception) {
            view.let { it1 ->
                Snackbar.make(it1, "Delete failed", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    //.setBackgroundTint(resources.getColor(R.color.pr))
                    .setActionTextColor(activity.resources.getColor(R.color.primary, null))
                    .setAnchorView(activity.findViewById(R.id.bottom_navigation))
                    .show()
            }
            return false
        }
    }


    private fun checkItemPresence(currentData: ArrayList<FolderItem>, item: FolderItem): FolderItem? {
        for(i in currentData) {
            if(i.folder_name == item.folder_name &&
                i.path == item.path &&
                i.icon == item.icon &&
                i.userId == item.userId)
                return i
        }
        return null
    }

    fun updateFolderNotesCache(folder: String) {
        var path = folder
        while(path != "/${firebase.getCurrentUid()}/Notes") {
            path = path.substringBeforeLast("/")
            sizeCache.remove(path)
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
                         if(folderItem.id.isNotBlank()) {
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

    private suspend fun deleteFolder(prefixes: List<StorageReference>, adapter: FolderViewAdapter, shownFiles: ArrayList<FolderItem>, files: ArrayList<FolderItem>, currentData: ArrayList<FolderItem>) {
        prefixes.forEach { prefix ->
            val result = prefix.listAll().await()
            result.items.forEach { item ->
                item.delete().await()
            }
            deleteFolder(result.prefixes, adapter, shownFiles, files, currentData)
        }
    }


}