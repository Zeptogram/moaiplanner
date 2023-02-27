package com.example.moaiplanner.ui.main

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ToDoListFragment : Fragment() {

    // Lista di elementi della to-do list
    private val toDoList = mutableListOf<ToDoItem>()

    // Adapter per la RecyclerView
    private lateinit var adapter: ToDoListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla il layout del Fragment
        val view = inflater.inflate(R.layout.todo_fragment, container, false)

        val toolbar = activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, true)

        toolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    findNavController().navigate(R.id.optionsFragment, null,
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
        adapter = ToDoListAdapter(toDoList)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        // Aggiungi un listener al bottone "Aggiungi" per aggiungere un nuovo elemento alla lista
        val addButton = view.findViewById<FloatingActionButton>(R.id.addButton)
        addButton.setOnClickListener {
            showAddItemDialog()
            /*val text = view.findViewById<EditText>(R.id.editText).text.toString()
            if (text.isNotBlank()) {
                toDoList.add(ToDoItem(text))
                adapter.notifyItemInserted(toDoList.lastIndex)
            }*/
        }

        // Aggiungi un item touch helper per eliminare gli elementi tramite lo swipe
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                toDoList.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        return view
    }

    // Classe per rappresentare un elemento della to-do list
    private data class ToDoItem(val text: String, var isDone: Boolean = false)

    // Adapter per la RecyclerView
    private inner class ToDoListAdapter(private val items: List<ToDoItem>) : RecyclerView.Adapter<ToDoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.todo_element, parent, false)
            return ToDoViewHolder(view)
        }

        override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = item.text
            holder.checkBox.isChecked = item.isDone
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.isDone = isChecked
            }
        }

        override fun getItemCount(): Int = items.size
    }

    // ViewHolder per gli elementi della RecyclerView
    private inner class ToDoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    private fun showAddItemDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)

        val editTextItem = dialogView.findViewById<EditText>(R.id.editTextItem)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Aggiungi elemento")
            .setView(dialogView)
            .setPositiveButton("Aggiungi") { dialog, which ->
                val text = editTextItem.text.toString()
                if (text.isNotBlank()) {
                    toDoList.add(ToDoItem(text))
                    adapter.notifyItemInserted(toDoList.lastIndex)
                }
                // Do something with the new item
            }
            .setNegativeButton("Annulla", null)
            .create()

        dialog.show()

    }



}
