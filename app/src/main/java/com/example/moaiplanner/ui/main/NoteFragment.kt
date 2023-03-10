package com.example.moaiplanner.ui.main

import android.Manifest
import android.R.attr.data
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.*
import androidx.lifecycle.lifecycleScope
import com.example.moaiplanner.R
import com.example.moaiplanner.adapter.EditPagerAdapter
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.model.MarkdownViewModel
import com.example.moaiplanner.util.DisableableViewPager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import java.io.File


class NoteFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val viewModel: MarkdownViewModel by viewModels()
    private lateinit var firebase: AuthRepository
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var userDir: StorageReference
    private var noteDir = "Notes/"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is Activity) return
        lifecycleScope.launch {
            viewModel.load(context, context.intent?.data)
            context.intent?.data = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity?.findViewById<DrawerLayout>(R.id.drawerLayout)?.open()

        return inflater.inflate(R.layout.note_fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toolbar = activity?.findViewById<Toolbar>(R.id.topAppBar)

        toolbar?.menu?.setGroupVisible(R.id.edit, true)
        toolbar?.menu?.setGroupVisible(R.id.sett, false)

        val adapter = EditPagerAdapter(childFragmentManager, view.context)
        val pager = activity?.findViewById<DisableableViewPager>(R.id.pager)
        pager?.adapter = adapter
        pager?.addOnPageChangeListener(adapter)
        pager?.pageMargin = 1
        val tabLayout = activity?.findViewById<TabLayout>(R.id.tabLayout)


        tabLayout?.setupWithViewPager(pager)

        firebase = AuthRepository(requireActivity().application)
        storage = Firebase.storage
        storageRef = storage.reference
        userDir = storageRef.child("${firebase.getCurretUid()}")

        /*tabLayout?.getTabAt(0)?.setIcon(R.drawable.ic_baseline_edit_note_24)
        tabLayout?.getTabAt(1)?.setIcon(R.drawable.ic_baseline_remove_red_eye_24)*/

        viewModel.fileName.observe(viewLifecycleOwner) {
            toolbar?.title = it
        }

        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {

                android.R.id.home -> {
                    activity?.findViewById<DrawerLayout>(R.id.drawerLayout)?.open()
                    true
                }

                R.id.action_save -> {
                    //Timber.d("Save clicked")
                    lifecycleScope.launch {
                        if (!viewModel.save(requireContext())) {
                            requestFileOp(REQUEST_SAVE_FILE)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.file_saved, viewModel.fileName.value),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        val noteDir = storageRef.child("${firebase.getCurretUid()}/${noteDir}")
                        val uri : Uri = viewModel.uri.value.toString().toUri()
                        val uploadTask = noteDir.putFile(uri)

                        // Register observers to listen for when the download is done or if it fails
                        uploadTask.addOnFailureListener {
                            Toast.makeText(context, "Note upload failed", Toast.LENGTH_SHORT).show()
                            Log.d("Note", "Failed")
                        }.addOnSuccessListener { taskSnapshot ->
                            Toast.makeText(context, "Note uploaded successful", Toast.LENGTH_SHORT).show()
                            Log.d("Note", "Successful")
                        }
                    }
                    true
                }
                R.id.action_save_as -> {
                    requestFileOp(REQUEST_SAVE_FILE)
                    true
                }
                R.id.action_share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.putExtra(Intent.EXTRA_TEXT, viewModel.markdownUpdates.value)
                    shareIntent.type = "text/plain"
                    startActivity(Intent.createChooser(
                        shareIntent,
                        getString(R.string.share_file)
                    ))
                    true
                }
                R.id.action_load -> {
                    requestFileOp(REQUEST_OPEN_FILE)
                    true
                }
                R.id.action_new -> {
                    promptSaveOrDiscardChanges()
                    true
                }
                R.id.action_lock_swipe -> {
                    //Timber.d("Lock swiping clicked")
                    it.isChecked = !it.isChecked
                    pager!!.setSwipeLocked(it.isChecked)
                    true
                }
                else -> true
            }
        }

        setFragmentResultListener("noteDirFromHome") { requestKey, bundle ->
            noteDir =  noteDir.plus(bundle.getString("noteDir").toString())
            Log.d("noteNameFromHome", noteDir.toString())
            val noteRef = storageRef.child("${firebase.getCurretUid()}/${noteDir}")
            Log.d("noteStorageRef", noteRef.toString())
            val noteName = noteDir?.substringAfterLast("/")
            val localFile = File(activity?.cacheDir, noteName.toString());
            noteRef.getFile(localFile).addOnSuccessListener {
                lifecycleScope.launch {
                    context?.let { it1 -> viewModel.load(it1, localFile.toUri()) }
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed loading file from database", Toast.LENGTH_SHORT).show()
            }
            localFile.delete()
        }
    }

    override fun onStop() {
        super.onStop()
        val context = context?.applicationContext ?: return
        lifecycleScope.launch {
            viewModel.autosave(context, PreferenceManager.getDefaultSharedPreferences(context))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_SAVE_FILE, REQUEST_OPEN_FILE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, open file save dialog
                    //Timber.d("Storage permissions granted")
                    requestFileOp(requestCode)
                } else {
                    // Permission denied, do nothing
                    //Timber.d("Storage permissions denied, unable to save or load files")
                    context?.let {
                        Toast.makeText(it, R.string.no_permissions, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_OPEN_FILE -> {
                if (resultCode != Activity.RESULT_OK || data?.data == null) {
                    return
                }

                lifecycleScope.launch {
                    context?.let {
                        if (!viewModel.load(it, data.data)) {
                            Toast.makeText(it, R.string.file_load_error, Toast.LENGTH_SHORT).show()
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
                        viewModel.save(it, data.data)
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun promptSaveOrDiscardChanges() {
        if (!viewModel.shouldPromptSave()) {
            viewModel.reset(
                "Untitled.md",
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            )
            return
        }
        val context = context ?: run {
            //Timber.w("Context is null, unable to show prompt for save or discard")
            return
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.save_changes)
            .setMessage(R.string.prompt_save_changes)
            .setNegativeButton(R.string.action_discard) { _, _ ->
                //Timber.d("Discarding changes")
                viewModel.reset(
                    "Untitled.md",
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                )
            }
            .setPositiveButton(R.string.action_save) { _, _ ->
                //Timber.d("Saving changes")
                requestFileOp(REQUEST_SAVE_FILE)
            }
            .create()
            .show()
    }

    private fun requestFileOp(requestType: Int) {
        val context = context ?: run {
            //Timber.w("File op requested but context was null, aborting")
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            //Timber.i("Storage permission not granted, requesting")
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestType
            )
            return
        }
        val intent = when (requestType) {
            REQUEST_SAVE_FILE -> {
                //Timber.d("Requesting save op")
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/markdown"
                    putExtra(Intent.EXTRA_TITLE, viewModel.fileName.value)
                }
            }
            REQUEST_OPEN_FILE -> {
                //Timber.d("Requesting open op")
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    if (MimeTypeMap.getSingleton().hasMimeType("md")) {
                        // If the device doesn't recognize markdown files then we're not going to be
                        // able to open them at all, so there's no sense in filtering them out.
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/markdown"))
                    }
                }
            }
            else -> {
                //Timber.w("Ignoring unknown file op request: $requestType")
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
