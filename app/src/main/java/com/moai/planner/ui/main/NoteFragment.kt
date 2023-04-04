@file:Suppress("DEPRECATION")

package com.moai.planner.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import com.moai.planner.R
import com.moai.planner.adapter.EditPagerAdapter
import com.moai.planner.data.notes.FolderManager
import com.moai.planner.data.user.UserAuthentication
import com.moai.planner.model.MarkdownViewModel
import com.moai.planner.ui.welcome.WelcomeActivity
import com.moai.planner.util.DisableableViewPager
import com.moai.planner.util.NavigationHelper
import com.moai.planner.util.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import java.io.File


class NoteFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val markdownViewModel: MarkdownViewModel by viewModels()
    private lateinit var firebase: UserAuthentication
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var userDir: StorageReference
    private var noteDir = "Notes/"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is Activity) return
        lifecycleScope.launch {
            markdownViewModel.load(context, context.intent?.data)
            context.intent?.data = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.findViewById<DrawerLayout>(R.id.drawerLayout)?.open()
        markdownViewModel.currentDir.value = markdownViewModel.loadDir(requireContext()).toString()
        noteDir = markdownViewModel.currentDir.value!!
        return inflater.inflate(R.layout.note_fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = activity?.findViewById<Toolbar>(R.id.topAppBar)
        val fm = FolderManager(requireActivity(), requireView())

        toolbar?.menu?.setGroupVisible(R.id.edit, true)
        toolbar?.menu?.setGroupVisible(R.id.sett, false)
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(1).isChecked = true

        val adapter = EditPagerAdapter(childFragmentManager, view.context)
        val pager = activity?.findViewById<DisableableViewPager>(R.id.pager)
        pager?.adapter = adapter
        pager?.addOnPageChangeListener(adapter)
        pager?.pageMargin = 1
        val tabLayout = activity?.findViewById<TabLayout>(R.id.tabLayout)
        tabLayout?.setupWithViewPager(pager)

        firebase = UserAuthentication(requireActivity().application)
        if (!firebase.isUserAuthenticated()) {
            NavigationHelper.changeActivity(requireActivity(), WelcomeActivity::class.java)
        }
        storage = Firebase.storage
        storageRef = storage.reference
        userDir = storageRef.child("${firebase.getCurrentUid()}")
        markdownViewModel.fileName.observe(viewLifecycleOwner) {
            toolbar?.title = it
        }

        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                android.R.id.home -> {
                    activity?.findViewById<DrawerLayout>(R.id.drawerLayout)?.open()
                    true
                }

                R.id.action_save -> {
                    lifecycleScope.launch {
                        if (!markdownViewModel.save(requireContext())) {
                            requestFileOp(REQUEST_SAVE_FILE)
                        } else {
                            Utils.showPopup(view, requireActivity(), getString(R.string.file_saved, markdownViewModel.fileName.value))
                        }
                    }
                    true
                }
                R.id.action_save_as -> {
                    requestFileOp(REQUEST_SAVE_FILE)
                    true
                }
                R.id.action_save_firebase -> {
                    lifecycleScope.launch {
                        markdownViewModel.autosave(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))
                        markdownViewModel.saveDir(requireContext())
                    }.invokeOnCompletion {
                        fm.saveOnFirebase(noteDir, markdownViewModel)
                    }
                    true
                }
                R.id.action_share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.putExtra(Intent.EXTRA_TEXT, markdownViewModel.markdownUpdates.value)
                    shareIntent.type = "text/plain"
                    startActivity(Intent.createChooser(
                        shareIntent,
                        getString(R.string.share_file)
                    ))
                    true
                }
                R.id.action_load -> {
                    noteDir = "Notes/"
                    requestFileOp(REQUEST_OPEN_FILE)
                    true
                }
                R.id.action_new -> {
                    noteDir = "Notes/"
                    promptSaveOrDiscardChanges()
                    true
                }
                R.id.action_lock_swipe -> {
                    it.isChecked = !it.isChecked
                    pager!!.setSwipeLocked(it.isChecked)
                    true
                }
                else -> true
            }
        }

        setFragmentResultListener("noteDirFromHome") { _, bundle ->
            noteDir = "Notes/"
            noteDir =  noteDir.plus(bundle.getString("noteDir").toString())
            markdownViewModel.currentDir.value = noteDir
            Log.d("noteNameFromHome", noteDir)
            val noteRef = storageRef.child("${firebase.getCurrentUid()}/${noteDir}")
            Log.d("noteStorageRef", noteRef.toString())
            val noteName = noteDir.substringAfterLast("/")
            val localFile = File(activity?.cacheDir, noteName)
            noteRef.getFile(localFile).addOnSuccessListener {
                lifecycleScope.launch {
                    context?.let { it1 -> markdownViewModel.load(it1, localFile.toUri()) }
                }
            }.addOnFailureListener {
                Utils.showPopup(view, requireActivity(), getString(R.string.file_fail_database))
            }
            localFile.delete()
        }
    }

    override fun onStop() {
        super.onStop()
        val context = context?.applicationContext ?: return
        lifecycleScope.launch {
            markdownViewModel.autosave(context, PreferenceManager.getDefaultSharedPreferences(context))
            markdownViewModel.saveDir(requireContext())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_SAVE_FILE, REQUEST_OPEN_FILE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestFileOp(requestCode)
                } else {
                    context?.let {
                        view?.let { it1 ->
                            Utils.showPopup(it1, requireActivity(), getString(R.string.no_permissions))
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_OPEN_FILE -> {
                if (resultCode != Activity.RESULT_OK || data?.data == null) {
                    return
                }
                lifecycleScope.launch {
                    context?.let {
                        if (!markdownViewModel.load(it, data.data)) {
                            view?.let { it1 ->
                                Utils.showPopup(it1, requireActivity(), getString(R.string.file_load_error))
                            }
                        }
                    }
                }
            }
            REQUEST_SAVE_FILE -> {
                if (resultCode != Activity.RESULT_OK || data?.data == null) {
                    data?.data?.toString()
                    return
                }
                lifecycleScope.launch {
                    context?.let {
                        markdownViewModel.save(it, data.data)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun promptSaveOrDiscardChanges() {
        if (!markdownViewModel.shouldPromptSave()) {
            markdownViewModel.reset(
                "Untitled.md",
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            )
            return
        }
        val context = context ?: run {
            return
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.save_changes)
            .setMessage(R.string.prompt_save_changes)
            .setNegativeButton(R.string.action_discard) { _, _ ->
                markdownViewModel.reset(
                    "Untitled.md",
                    PreferenceManager.getDefaultSharedPreferences(context)
                )
            }
            .setPositiveButton(R.string.action_save) { _, _ ->
                requestFileOp(REQUEST_SAVE_FILE)
            }
            .create()
            .show()
    }

    private fun requestFileOp(requestType: Int) {
        context ?: run {
            return
        }

        val intent = when (requestType) {
            REQUEST_SAVE_FILE -> {
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/markdown"
                    putExtra(Intent.EXTRA_TITLE, markdownViewModel.fileName.value)
                }
            }
            REQUEST_OPEN_FILE -> {
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    if (MimeTypeMap.getSingleton().hasMimeType("md")) {
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/markdown"))
                    }
                }
            }
            else -> {
                null
            }
        } ?: return
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        startActivityForResult(
            intent,
            requestType
        )
    }

    companion object {
        // Request codes
        const val REQUEST_OPEN_FILE = 1
        const val REQUEST_SAVE_FILE = 2
        const val KEY_AUTOSAVE = "autosave"
    }






}
