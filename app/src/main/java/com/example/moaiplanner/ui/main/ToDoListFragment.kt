package com.example.moaiplanner.ui.main

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.adapter.CalendarAdapter
import com.example.moaiplanner.data.calendar.CalendarData
import com.example.moaiplanner.databinding.TodoElementBinding
import com.example.moaiplanner.databinding.TodoFragmentBinding
import com.example.moaiplanner.databinding.TomatoFragmentBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate

import java.util.*

class ToDoListFragment : Fragment(), CalendarAdapter.CalendarInterface{

    // Lista di elementi della to-do list
    private val toDoList = mutableListOf<ToDoItem>()
    private val sdf = SimpleDateFormat("MMMM yyyy", Locale.ITALIAN)
    private val cal = Calendar.getInstance(Locale.ITALIAN)
    private lateinit var toolbar: Toolbar
    private lateinit var binding: TodoFragmentBinding
    private val calendarAdapter = CalendarAdapter(this, arrayListOf())
    private val calendarList = ArrayList<CalendarData>()

    // Adapter per la RecyclerView
    private lateinit var adapter: ToDoListAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla il layout del Fragment
        binding = TodoFragmentBinding.inflate(inflater, container, false)

        toolbar = activity?.findViewById<Toolbar>(R.id.topAppBar)!!
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

        init()
        binding.monthYearPicker.setOnClickListener {
            displayDatePicker()
        }

        getDates()
        onSelect(CalendarData(Date(), true), (cal.get(Calendar.DAY_OF_MONTH) - 1))



        // Inizializza la RecyclerView
        adapter = ToDoListAdapter(toDoList)
        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        // Aggiungi un listener al bottone "Aggiungi" per aggiungere un nuovo elemento alla lista
        val addButton = binding.addButton
        addButton.setOnClickListener {
            showAddItemDialog()
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

        return binding.root
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


    private fun init() {
        binding.apply {

            toolbar.title = sdf.format(cal.time)
            calendarView.setHasFixedSize(true)
            calendarView.adapter = calendarAdapter

        }
    }

    private fun displayDatePicker() {

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()
        //val materialDatePicker = materialDateBuilder.build()

        datePicker.show(parentFragmentManager, "DatePicker");
        datePicker.addOnPositiveButtonClickListener {

        try {
            toolbar.title = sdf.format(it)
            cal.time = Date(it)
            getDates()
            onSelect(CalendarData(Date(it), true), (cal.get(Calendar.DAY_OF_MONTH) - 1))
        } catch (e: ParseException) { }

        }

    }


    /*------------------------------ Get Dates of Month ------------------------------*/

    private fun getDates() {

        val dateList = ArrayList<CalendarData>() // For our Calendar Data Class
        val dates = ArrayList<Date>() // For Date
        val monthCalendar = cal.clone() as Calendar
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)

        while (dates.size < maxDaysInMonth) {
            dates.add(monthCalendar.time)
            dateList.add(CalendarData(monthCalendar.time))
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1)   // Increment Day By 1
        }

        calendarList.clear()
        calendarList.addAll(dateList)
        calendarAdapter.updateList(dateList)

    }



    override fun onSelect(calendarData: CalendarData, position: Int) {

        // You can get Selected date here....
        calendarList.forEachIndexed { index, calendarModel ->
            calendarModel.isSelected = index == position
        }
        calendarAdapter.updateList(calendarList)
        //Log.d("CIAO", "vamos")
    }


}
