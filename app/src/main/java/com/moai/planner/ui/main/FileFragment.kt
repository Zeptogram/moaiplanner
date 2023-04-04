package com.moai.planner.ui.main

import android.annotation.SuppressLint
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.moai.planner.R
import com.moai.planner.adapter.FolderViewAdapter
import com.moai.planner.data.notes.FolderManager
import com.moai.planner.databinding.FileFragmentBinding
import com.moai.planner.ui.welcome.WelcomeActivity
import com.moai.planner.util.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
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
        // Inflate il layout per il fragment
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fm =  FolderManager(requireActivity(), requireView())

        NetworkUtils.notifyMissingNetwork(requireContext(), requireView(), requireActivity())
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(0).isChecked = true

        toolbar = activity?.findViewById(R.id.topAppBar)!!
        toolbar.menu?.setGroupVisible(R.id.edit, false)
        toolbar.menu?.setGroupVisible(R.id.sett, false)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    NavigationHelper.navigateTo(view, R.id.optionsFragment)
                }
            }
            true
        }
        // Filtro ShowAll
        binding.buttonShowall.setOnClickListener {
            binding.buttonShowall.isEnabled = false
            shownFiles.clear()
            shownFiles.addAll(files)
            adapter.notifyDataSetChanged()
            binding.buttonFavourites.isEnabled = true
        }
        // Filtro Preferiti
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
        // Bottone per create note/folder
        binding.addNoteFolderButton.setOnClickListener {
            showAddNoteFolderDialog()
        }
        //Bottone upload note
        binding.uploadNoteButton.setOnClickListener {
            Intent(Intent.ACTION_OPEN_DOCUMENT).also {
                it.type = "text/markdown"
                startActivityForResult(it, 0)
            }
        }
        // Backbutton custom
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (fm.getCurrentFolder() == "") {
                NavigationHelper.navigateTo(view, R.id.homeFragment)
            } else {
                if (fm.getCurrentFolder().split("/").size == 2) {
                    fm.setCurrentFolder("")
                    toolbar.title = getString(R.string.my_notes)
                } else {
                    fm.setCurrentFolder(fm.getCurrentFolder().substringBeforeLast("/").substringBeforeLast("/").plus("/"))
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
            NavigationHelper.changeActivity(requireActivity(), WelcomeActivity::class.java)
        }
        // Inizializza la RecyclerView
        initFolderView()
        adapter = FolderViewAdapter(shownFiles)
        val recyclerView = binding.files
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        // OnClick on Recycler elements
        adapter.setOnItemClickListener(object : FolderViewAdapter.OnItemClickListener {

            @SuppressLint("NotifyDataSetChanged")
            override fun onItemClick(position: Int) {
                val bundle = Bundle()
                // Se è un file, allora navigation al note fragment passando nome file nel bundle
                if (adapter.getFileName(position).endsWith(".md")) {
                    val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    bottomNav.menu.getItem(1).isChecked = true
                    bundle.putString("noteDir", fm.getCurrentFolder().plus(adapter.getFileName(position)))
                    setFragmentResult("noteDirFromHome", bundle)
                    view?.let { NavigationHelper.navigateToAndPop(it, R.id.noteFragment) }
                } else {
                    // Altrimenti è un folder
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
            // Delete
            override fun onItemLongClick(position: Int) {
                showDeleteNoteFolderDialog(position)
            }
        })
        // Init dei favourites
        if(init) {
            init = false
            fm.fetchFavouritesFromFirebase(currentData)
        }
        // Init dei files
        lifecycleScope.launch(Dispatchers.IO) {
            fm.getCollections(files, adapter, fm.getCurrentFolder(), shownFiles, currentData)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            // Upload della nota
            lifecycleScope.launch(Dispatchers.IO) {
                if (data != null) {
                    fm.uploadNote(data)
                }
            }.invokeOnCompletion {
                // Devo aggiornare la cache e la recyclerview
                lifecycleScope.launch(Dispatchers.Main) {
                    Utils.sizeCache.remove("/${fm.getUserData().getCurrentUid()}/Notes/${fm.getCurrentFolder()}".substringBeforeLast("/"))
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
        // Dialog per creazione di folder e notes
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_note_or_folder))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
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
            .setNegativeButton(getString(R.string.cancel), null)
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
        // Dialog per il delete di note
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_note_or_folder))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    Log.d("NOTE-DIR", adapter.getFileName(position))
                    if (adapter.getFileName(position).endsWith(".md")) {
                        deleted = fm.deleteNote(adapter, shownFiles, currentData, position)
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
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
        dialog.show()
    }



    private fun initFolderView() {
        files.clear()
        shownFiles.clear()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteItem(position: Int){
        lifecycleScope.launch(Dispatchers.Main) {
            adapter.onItemDelete(shownFiles[position].id, fm.getCurrentFolder())
            currentData.remove(shownFiles[position])
            shownFiles.removeAt(position)
            adapter.notifyDataSetChanged()
        }
    }
    @SuppressLint("NotifyDataSetChanged")
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