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
import com.example.moaiplanner.data.notes.FolderManager
import com.example.moaiplanner.databinding.FileFragmentBinding
import com.example.moaiplanner.util.FolderItem
import com.example.moaiplanner.util.NetworkUtils
import com.example.moaiplanner.util.sizeCache
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream

class FileFragment: Fragment() {
    lateinit var binding: FileFragmentBinding
    private var files = ArrayList<FolderItem>()
    private var shownFiles = ArrayList<FolderItem>()
    private var currentData = ArrayList<FolderItem>()
    private var init: Boolean = true
    private lateinit var fm: FolderManager
    private lateinit var toolbar: Toolbar


    // Adapter per la RecyclerView
    private lateinit var adapter: FolderViewAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FileFragmentBinding.inflate(inflater, container, false)

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



        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fm =  FolderManager(requireActivity(), requireView())

        NetworkUtils.notifyMissingNetwork(requireContext(), view)
        var bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(0).isChecked = true;

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
            if (fm.getCurrentFolder() == "") {
                findNavController().navigate(
                    R.id.homeFragment, null,
                    navOptions {
                        anim {
                            enter = android.R.anim.fade_in
                            popEnter = android.R.anim.fade_in
                        }
                    }
                )
            } else {
                if (fm.getCurrentFolder().split("/").size == 2) {
                    fm.setCurrentFolder("")
                    toolbar.title = "My Notes"
                } else {
                    fm.setCurrentFolder(
                        fm.getCurrentFolder().substringBeforeLast("/").substringBeforeLast("/")
                            .plus("/")
                    )
                    toolbar.title = fm.getCurrentFolder()
                }
                binding.buttonFavourites.isEnabled = true
                binding.buttonShowall.isEnabled = false
                lifecycleScope.launch(Dispatchers.Main) {
                    fm.fetchFavouritesFromFirebase(currentData)
                    resetFolderView()
                }
            }
        }


    }

    override fun onStart() {
        super.onStart()

        if (!fm.getUserData().isUserAuthenticated()) {
            findNavController().navigate(R.id.welcomeActivity)
        }
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
                    bundle.putString("noteDir", fm.getCurrentFolder().plus(adapter.getFileName(position)))
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
                    fm.setCurrentFolder(fm.getCurrentFolder().plus(adapter.getFileName(position).plus("/")))
                    toolbar.title = fm.getCurrentFolder()
                    files.clear()
                    shownFiles.clear()
                    binding.buttonFavourites.isEnabled = true
                    binding.buttonShowall.isEnabled = false
                    adapter.notifyDataSetChanged()
                    fm.fetchFavouritesFromFirebase(currentData)
                    lifecycleScope.launch(Dispatchers.IO) {
                        fm.getCollections(files, adapter, fm.getCurrentFolder(), shownFiles, currentData)
                    }
                }
            }

            override fun onItemLongClick(position: Int) {
                showDeleteNoteFolderDialog(position)
            }
        })
        if(init) {
            init = false
            fm.fetchFavouritesFromFirebase(currentData)
        }
        lifecycleScope.launch(Dispatchers.IO) {
            fm.getCollections(files, adapter, fm.getCurrentFolder(), shownFiles, currentData)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            lifecycleScope.launch(Dispatchers.IO) {
                if (data != null) {
                    fm.uploadNote(data)
                }
            }.invokeOnCompletion {
                lifecycleScope.launch(Dispatchers.Main) {
                    sizeCache.remove(
                        "/${
                            fm.getUserData().getCurrentUid()
                        }/Notes/${fm.getCurrentFolder()}".substringBeforeLast("/")
                    )
                    fm.updateFolderNotesCache(fm.getFolderPath())
                    resetFolderView()
                }
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
                        fm.createFile(editTextNoteFolder)
                    }.invokeOnCompletion {
                        lifecycleScope.launch(Dispatchers.Main){
                            fm.updateFolderNotesCache(fm.getFolderPath())
                            resetFolderView()
                        }
                    }

                } else if (radioButtonFolder.isChecked) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        fm.createFile(editTextNoteFolder, true)
                    }.invokeOnCompletion {
                        lifecycleScope.launch(Dispatchers.Main){
                            fm.updateFolderNotesCache(fm.getFolderPath())
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
        lifecycleScope.launch(Dispatchers.IO) {
            fm.getCollections(files, adapter, fm.getCurrentFolder(), shownFiles, currentData)
        }

    }

    private fun showDeleteNoteFolderDialog(position: Int) {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_note_folder, null)
        var deleted = false
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete note or folder")
            .setView(dialogView)
            .setPositiveButton("Delete") { dialog, which ->
                lifecycleScope.launch(Dispatchers.IO) {
                    Log.d("NOTE-DIR", adapter.getFileName(position))
                    if (adapter.getFileName(position).endsWith(".md")) {
                        deleted = fm.deleteNote(adapter, shownFiles, files, currentData, position)
                        deleteItem(position)
                    } else {
                        deleted = fm.deleteDirectory(adapter, shownFiles, files, currentData, position)
                        deleteFolderItem(position)
                    }
                }.invokeOnCompletion {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Log.d("DELETED", deleted.toString())
                        if (!deleted) {
                            fm.updateFolderNotesCache(fm.getFolderPath())
                            resetFolderView()
                        } else {
                            fm.updateFolderNotesCache(fm.getFolderPath())
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }



    private fun initFolderView() {
        files.clear()
        shownFiles.clear()
    }

    private fun deleteItem(position: Int){
        lifecycleScope.launch(Dispatchers.Main) {
            adapter.onItemDelete(shownFiles[position].id, fm.getCurrentFolder())
            currentData.remove(shownFiles[position])
            shownFiles.removeAt(position)
            adapter.notifyDataSetChanged()
        }
    }
    private fun deleteFolderItem(position: Int){
        lifecycleScope.launch(Dispatchers.Main) {
            val name = shownFiles[position].folder_name
            adapter.onItemDelete(name, fm.getCurrentFolder())
            currentData.remove(shownFiles[position])
            shownFiles.removeAt(position)
            adapter.notifyDataSetChanged()
        }


    }



}