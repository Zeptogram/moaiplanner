package com.example.moaiplanner.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.NotelistFragmentBinding
import okhttp3.internal.notifyAll

class FileFragment: Fragment() {
    lateinit var binding: NotelistFragmentBinding
    private var notes = ArrayList<Note>()
    private var shownNotes = ArrayList<Note>()

    private lateinit var toolbar: Toolbar

    // Adapter per la RecyclerView
    private lateinit var adapter: FileFragment.FileAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = NotelistFragmentBinding.inflate(inflater, container, false)

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

        // Inizializza la RecyclerView
        adapter = FileAdapter(notes)
        val recyclerView = binding.files
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        notes.add(Note("IShowSpeed.md", "104 Kb", true))
        adapter.notifyItemInserted(notes.lastIndex)
        notes.add(Note("Marco Bianchi.md", "1 Mb" ,false))
        adapter.notifyItemInserted(notes.lastIndex)


        shownNotes.addAll(notes)

        binding.buttonShowall.setOnClickListener {
            if(!shownNotes.equals(notes)) {
                shownNotes.clear()
                shownNotes.addAll(notes)
                adapter.notifyDataSetChanged()
            }

        }

        binding.buttonFavourites.setOnClickListener {
            shownNotes.clear()
            for(note in notes){
                if(note.isFavourite) {
                    shownNotes.add(note)
                }
            }
            adapter.notifyDataSetChanged()


        }




        // Inflate il layout per il fragment
        return binding.root
    }

    data class Note(val name: String, val size: String, val isFavourite: Boolean)

    // Adapter per la RecyclerView
    private inner class FileAdapter(private val items: List<Note>) : RecyclerView.Adapter<FileViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.note_template, parent, false)
            return FileViewHolder(view)
        }
        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            val item = shownNotes[position]
            holder.textViewName.text = item.name
            holder.textViewSize.text = item.size
            holder.checkbox.isChecked = item.isFavourite



        }
        override fun getItemCount(): Int = shownNotes.size

    }


    // ViewHolder per gli elementi della RecyclerView
    private inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewName: TextView = itemView.findViewById(R.id.fileName)
        val textViewSize: TextView = itemView.findViewById(R.id.file_size)
        val checkbox: CheckBox = itemView.findViewById(R.id.star)

    }
}