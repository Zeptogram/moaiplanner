package com.example.moaiplanner.ui.main

import FolderViewAdapter
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moaiplanner.R
import com.example.moaiplanner.data.user.AuthRepository
import com.example.moaiplanner.databinding.FileFragmentBinding
import com.example.moaiplanner.util.FolderItem
import com.example.moaiplanner.util.NetworkUtils
import com.example.moaiplanner.util.getFolderSize
import com.example.moaiplanner.util.sizeCache
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream
import java.text.DecimalFormat

class FileFragment: Fragment() {
    lateinit var binding: FileFragmentBinding
    private var files = ArrayList<FolderItem>()
    private var shownFiles = ArrayList<FolderItem>()
    lateinit var firebase: AuthRepository
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var userDirNotes: StorageReference
    private lateinit var favouritesRef: DatabaseReference
    private lateinit var realtimeDb: FirebaseDatabase
    private var currentData = ArrayList<FolderItem>()
    private var init: Boolean = true


    private var currentFolder = ""
    private lateinit var folderPath: String
    private lateinit var toolbar: Toolbar

    // Adapter per la RecyclerView
    private lateinit var adapter: FolderViewAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FileFragmentBinding.inflate(inflater, container, false)

        firebase = AuthRepository(requireActivity().application)
        storage = Firebase.storage
        storageRef = storage.reference
        userDirNotes = storageRef.child("${firebase.getCurrentUid()}/Notes")

        realtimeDb = FirebaseDatabase.getInstance()
        favouritesRef = realtimeDb.getReference("users/" + firebase.getCurrentUid().toString())




        toolbar = activity?.findViewById<Toolbar>(R.id.topAppBar)!!
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, false)

        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    findNavController().navigate(
                        R.id.optionsFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }
                    )
                }

            }
            true
        }

        binding.buttonShowall.setOnClickListener {
            binding.buttonShowall.isEnabled = false
            shownFiles.clear()
            shownFiles.addAll(files)
            adapter.notifyDataSetChanged()
            binding.buttonFavourites.isEnabled = true
        }
        binding.buttonFavourites.setOnClickListener {
            binding.buttonFavourites.isEnabled = false
            shownFiles.clear()
            for(file in files) {
                if(file.isFavourite){
                    shownFiles.add(file)
                }
            }
            adapter.notifyDataSetChanged()
            binding.buttonShowall.isEnabled = true
        }

        binding.addNoteFolderButton.setOnClickListener {
            showAddNoteFolderDialog()
        }

        binding.uploadNoteButton.setOnClickListener {
            Intent(Intent.ACTION_OPEN_DOCUMENT).also {
                it.type = "text/markdown"
                startActivityForResult(it, 0)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if(currentFolder == "") {
                findNavController().navigate(
                    R.id.homeFragment, null,
                    navOptions {
                        anim {
                            enter = android.R.anim.fade_in
                            popEnter = android.R.anim.fade_in
                        }
                    }
                )
            }
            else if(currentFolder.split("/").size == 2) {
                currentFolder = ""
                binding.buttonFavourites.isEnabled = true
                binding.buttonShowall.isEnabled = false
                getCollections(files, adapter, currentFolder)
                toolbar.title = "My Notes"
            }
            else {
                Log.d("AAA", currentFolder)
                currentFolder = currentFolder.substringBeforeLast("/").substringBeforeLast("/").plus("/")
                Log.d("AAA", currentFolder)
                binding.buttonFavourites.isEnabled = true
                binding.buttonShowall.isEnabled = false
                getCollections(files, adapter, currentFolder)
                toolbar.title = currentFolder

            }
        }





        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NetworkUtils.notifyMissingNetwork(requireContext(), view)
        var bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(1).isChecked = true;
    }

    override fun onStart() {
        super.onStart()

        firebase = AuthRepository(requireActivity().application)
        if (!firebase.isUserAuthenticated()) {
            findNavController().navigate(R.id.welcomeActivity)
        }

        // initializing variables of grid view with their ids.
        // Inizializza la RecyclerView
        initFolderView()
        adapter = FolderViewAdapter(shownFiles)
        val recyclerView = binding.files
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter


        // OnClick on Recycler elements
        adapter.setOnItemClickListener(object : FolderViewAdapter.onItemClickListener {

            override fun onItemClick(position: Int) {
                val bundle = Bundle()
                // Se è un file, allora navigation al note fragmnet passando nome file nel bundle
                if (adapter.getFileName(position).endsWith(".md")) {
                    val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    bottomNav.menu.getItem(1).isChecked = true
                    bundle.putString("noteDir", currentFolder.plus(adapter.getFileName(position)))
                    setFragmentResult("noteDirFromHome", bundle)
                    navHostFragment.findNavController().popBackStack()
                    navHostFragment.findNavController().navigate(R.id.noteFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }, null)
                } else {
                    currentFolder = currentFolder.plus(adapter.getFileName(position).plus("/"))
                    toolbar.title = currentFolder
                    files.clear()
                    shownFiles.clear()
                    binding.buttonFavourites.isEnabled = true
                    binding.buttonShowall.isEnabled = false
                    adapter.notifyDataSetChanged()
                    getCollections(files, adapter, currentFolder)
                }
            }

            override fun onItemLongClick(position: Int) {
                showDeleteNoteFolderDialog(position)

            }
        })
        if(init) {
            init = false
            fetchFavouritesFromFirebase()
            Log.d("HEY", "x")
        }
        getCollections(files, adapter, currentFolder)

    }



    fun getCollections(data: ArrayList<FolderItem>, adapter: FolderViewAdapter, folderName: String) {
        // Get list of files from Firestore
        files.clear()
        shownFiles.clear()
        adapter.notifyDataSetChanged()
        folderPath = "/${firebase.getCurrentUid()}/Notes/${currentFolder}"
        lifecycleScope.launch(Dispatchers.IO) {
            val folder = storageRef.child("${firebase.getCurrentUid()}/Notes/${folderName}")
            Log.d("collectionNotesRef", folder.toString())
            folder.listAll()
                .addOnSuccessListener { (items, prefixes) ->
                    prefixes.forEach { prefix ->

                        Log.d("FIRESTORAGE-PREFIX", prefix.toString())
                        var fileItem = FolderItem(prefix.toString().split("/").last().replace("%20", " "), "", false, R.drawable.folder)
                        fileItem.userId = firebase.getCurrentUid().toString()
                        fileItem.path = currentFolder.substringBeforeLast("/")
                        val value: FolderItem? = checkItemPresence(fileItem)
                        if(value == null) {
                            var dbItem = favouritesRef.child("favourites").push()
                            fileItem.id = dbItem.key.toString()
                            data.add(fileItem)
                            getFolderSize(prefix) { bytes, files ->
                                val df = DecimalFormat("#,##0.##")
                                df.maximumFractionDigits = 2
                                var kb = bytes.toDouble() / 1024
                                val info = df.format(kb) + "KB - " + files.toString() + " Notes"
                                fileItem.folder_files = info
                                dbItem.setValue(fileItem)
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
                            Log.d("PROVA", checkItemPresence(fileItem).toString())
                            val value: FolderItem? = checkItemPresence(fileItem)
                            if(value == null) {
                                var dbItem = favouritesRef.child("favourites").push()
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
                            .setActionTextColor(resources.getColor(R.color.primary, null))
                            .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                            .show()
                    }
                }
                .addOnSuccessListener {
                    shownFiles.clear()
                    shownFiles.addAll(files)
                    adapter.notifyDataSetChanged()
                }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            val uri = data?.data
            Log.d("NOTE URI", uri.toString())
            var fileName = "null.md"
            context?.let {
                if (uri != null) {
                    val document = DocumentFile.fromSingleUri(it, uri)
                    if (document != null) {
                        fileName = document.name.toString()
                    }
                }
            }
            Log.d("NOTE URI", fileName)

            val stream = FileInputStream(uri?.let { context?.contentResolver?.openFileDescriptor(it, "r")?.fileDescriptor ?: FileDescriptor() })

            val noteDir = storageRef.child("${firebase.getCurrentUid()}/Notes/${currentFolder}${fileName}")
            val uploadTask = noteDir.putStream(stream)

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener {
                view?.let { it1 ->
                    Snackbar.make(it1, "Note upload failed", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(resources.getColor(R.color.primary, null))
                        .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                        .show()
                }
                stream.close()
                resetFolderView()
            }.addOnSuccessListener { taskSnapshot ->
                view?.let {
                    Snackbar.make(it, "Note uploaded successfully", Snackbar.LENGTH_SHORT)
                        .setAction("OK") {
                            // Responds to click on the action
                        }
                        //.setBackgroundTint(resources.getColor(R.color.pr))
                        .setActionTextColor(resources.getColor(R.color.primary, null))
                        .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                        .show()
                }
                stream.close()
                sizeCache.remove("/${firebase.getCurrentUid()}/Notes/${currentFolder}".substringBeforeLast("/"))
                updateFolderNotesCache(folderPath)
                resetFolderView()

            }
        }
    }

    private fun showAddNoteFolderDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_note_folder, null)

        val radioButtonNote = dialogView.findViewById<RadioButton>(R.id.radio_button_note)
        val radioButtonFolder = dialogView.findViewById<RadioButton>(R.id.radio_button_folder)
        val editTextNoteFolder = dialogView.findViewById<EditText>(R.id.textNoteFolder)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add note or folder")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, which ->
                if (radioButtonNote.isChecked) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val noteDir = storageRef.child("${firebase.getCurrentUid()}/Notes/${currentFolder}${editTextNoteFolder.text}.md")
                        val text = "# Note created with Moai Planner"
                        val uploadFile = noteDir.putBytes(text.toByteArray())
                        uploadFile.addOnFailureListener {
                            resetFolderView()
                            // Handle unsuccessful uploads
                        }.addOnSuccessListener { taskSnapshot ->
                            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                            // ...
                            Log.d("NOTE-CREATION", "Nota creata")
                            view?.let {
                                Snackbar.make(it, "Note created", Snackbar.LENGTH_SHORT)
                                    .setAction("OK") {
                                        // Responds to click on the action
                                    }
                                    //.setBackgroundTint(resources.getColor(R.color.pr))
                                    .setActionTextColor(resources.getColor(R.color.primary, null))
                                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                                    .show()
                            }
                            updateFolderNotesCache(folderPath)

                            /*while(path != "/${firebase.getCurretUid()}/Notes") {
                                path = path.substringBeforeLast("/")
                                sizeCache.remove(path)
                            }*/

                            resetFolderView()

                        }
                    }
                } else if (radioButtonFolder.isChecked) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val noteDir = storageRef.child("${firebase.getCurrentUid()}/Notes/${currentFolder}${editTextNoteFolder.text}/temp.tmp")
                        val text = " "
                        val uploadFile = noteDir.putBytes(text.toByteArray())
                        uploadFile.addOnFailureListener {
                            resetFolderView()
                            // Handle unsuccessful uploads
                        }.addOnSuccessListener { taskSnapshot ->
                            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                            // ...
                            Log.d("FOLDER-CREATION", "Folder creato")
                            view?.let {
                                Snackbar.make(it, "Folder created", Snackbar.LENGTH_SHORT)
                                    .setAction("OK") {
                                        // Responds to click on the action
                                    }
                                    //.setBackgroundTint(resources.getColor(R.color.pr))
                                    .setActionTextColor(resources.getColor(R.color.primary, null))
                                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                                    .show()
                            }
                            updateFolderNotesCache(folderPath)
                            resetFolderView()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }


    private fun resetFolderView(){
        binding.buttonFavourites.isEnabled = true
        binding.buttonShowall.isEnabled = false
        files.clear()
        shownFiles.clear()
        getCollections(files, adapter, currentFolder)

    }

    private fun updateFolderNotesCache(folder: String) {
        var path = folder
        while(path != "/${firebase.getCurrentUid()}/Notes") {
            path = path.substringBeforeLast("/")
            sizeCache.remove(path)
        }
    }

    private fun showDeleteNoteFolderDialog(position: Int) {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_note_folder, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete note or folder")
            .setView(dialogView)
            .setPositiveButton("Delete") { dialog, which ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val noteDir: StorageReference
                    Log.d("NOTE-DIR", adapter.getFileName(position))
                    if (adapter.getFileName(position).endsWith(".md")) {
                        noteDir = storageRef.child("${firebase.getCurrentUid()}/Notes/${currentFolder}${adapter.getFileName(position)}")
                        Log.d("NOTE-DIR", noteDir.toString())
                        var id = shownFiles[position].id
                        adapter.onItemDelete(id)
                        noteDir.delete().addOnSuccessListener {

                            currentData.remove(shownFiles[position])

                            view?.let { it1 ->
                                Snackbar.make(it1, "File deleted", Snackbar.LENGTH_SHORT)
                                    .setAction("OK") {
                                        // Responds to click on the action
                                    }
                                    //.setBackgroundTint(resources.getColor(R.color.pr))
                                    .setActionTextColor(resources.getColor(R.color.primary, null))
                                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                                    .show()
                            }
                            updateFolderNotesCache(folderPath)
                            resetFolderView()

                        }.addOnFailureListener {
                            view?.let { it1 ->
                                Snackbar.make(it1, "Delete failed", Snackbar.LENGTH_SHORT)
                                    .setAction("OK") {
                                        // Responds to click on the action
                                    }
                                    //.setBackgroundTint(resources.getColor(R.color.pr))
                                    .setActionTextColor(resources.getColor(R.color.primary, null))
                                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                                    .show()
                            }
                        }
                    } else {
                        noteDir = storageRef.child("${firebase.getCurrentUid()}/Notes/${currentFolder}${adapter.getFileName(position)}")
                        Log.d("NOTE-DIR", noteDir.toString())
                        lifecycleScope.launch(Dispatchers.Main) {
                            adapter.onItemDelete(shownFiles[position].id)
                            currentData.remove(shownFiles[position])
                            shownFiles.removeAt(position)
                            binding.buttonFavourites.isEnabled = true
                            binding.buttonShowall.isEnabled = false
                            adapter.notifyDataSetChanged()
                        }


                        noteDir.listAll().addOnSuccessListener { (items, prefixes) ->
                            items.forEach { item ->
                                item.delete()
                            }
                            deleteFolder(prefixes)

                        }.addOnFailureListener {
                            view?.let { it1 ->
                                Snackbar.make(it1, "Delete failed", Snackbar.LENGTH_SHORT)
                                    .setAction("OK") {
                                        // Responds to click on the action
                                    }
                                    //.setBackgroundTint(resources.getColor(R.color.pr))
                                    .setActionTextColor(resources.getColor(R.color.primary, null))
                                    .setAnchorView(activity?.findViewById(R.id.bottom_navigation))
                                    .show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun deleteFolder(prefixes: List<StorageReference>) {
        prefixes.forEach { prefix ->
            prefix.listAll().addOnSuccessListener { (items, prefixes) ->
                items.forEach { item ->
                    item.delete()
                }
                deleteFolder(prefixes)
            }
        }
    }

    private fun initFolderView() {
        files.clear()
        shownFiles.clear()
    }

    private fun fetchFavouritesFromFirebase() {
        val favouritesListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val favouritesData = snapshot.child("favourites").children
                for (item in favouritesData) {
                    val folderItem = item.getValue(FolderItem::class.java)
                    if (folderItem != null) {
                        if(folderItem.id.isNotBlank()) {
                            currentData.add(folderItem)
                            //shownFiles.add(folderItem)
                        }
                    }
                }
                //adapter.notifyDataSetChanged()
                Log.d("CURRENT", currentData.toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FileFragment", "loadFavouritesList:onCancelled", error.toException())
            }

        }
        favouritesRef.addValueEventListener(favouritesListListener)
    }

    private fun checkItemPresence(item: FolderItem): FolderItem? {
        for(i in currentData) {
            if(i.folder_name == item.folder_name &&
                i.path == item.path &&
                i.icon == item.icon &&
                i.userId == item.userId)
                return i
        }
        return null
    }





}