package com.example.moaiplanner.ui.main

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.adapter.CalendarAdapter
import com.example.moaiplanner.data.calendar.CalendarData
import com.example.moaiplanner.data.todo.ToDoFetcher
import com.example.moaiplanner.data.user.UserAuthentication
import com.example.moaiplanner.databinding.TodoFragmentBinding
import com.example.moaiplanner.util.NavigationHelper
import com.example.moaiplanner.util.NetworkUtils
import com.example.moaiplanner.util.ToDoItem
import com.example.moaiplanner.util.ToDoItemListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.database.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


@Suppress("NAME_SHADOWING")
class ToDoListFragment : Fragment(), CalendarAdapter.CalendarInterface, ToDoItemListener{

    // Lista di elementi della to-do list
    private val toDoList = mutableListOf<ToDoItem>()
    private val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val cal = Calendar.getInstance(Locale.getDefault())
    private lateinit var toolbar: Toolbar
    private lateinit var binding: TodoFragmentBinding
    private val calendarAdapter = CalendarAdapter(this, arrayListOf())
    private val calendarList = ArrayList<CalendarData>()
    private lateinit var database: FirebaseDatabase
    private lateinit var todoListRef: DatabaseReference
    private lateinit var auth: UserAuthentication
    private lateinit var currentDate: String
    private var recyclerView: RecyclerView? = null
    private var manualUpdateUI: Boolean = false
    //private var todoListener: ToDoItemListener = context as ToDoItemListener

    // Adapter per la RecyclerView
    private var adapter = ToDoListAdapter(toDoList)


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate il layout del Fragment
        binding = TodoFragmentBinding.inflate(inflater, container, false)

        auth = UserAuthentication(requireActivity().application)
        if (!auth.isUserAuthenticated()) {
            findNavController().navigate(R.id.welcomeActivity)
        }

        database = FirebaseDatabase.getInstance()
        todoListRef = database.getReference("users/" + auth.getCurrentUid().toString())

        binding.monthYearPicker.setOnClickListener {
            displayDatePicker()
        }

        getDates()
        onSelect(CalendarData(Date(), true), (cal.get(Calendar.DAY_OF_MONTH) - 1))

        val icon = resources.getDrawable(R.drawable.baseline_delete_forever_24, null)
        val iconEdit = resources.getDrawable(R.drawable.ic_baseline_edit_note_24, null)
        val background = ColorDrawable(Color.RED)

        // Inizializza la RecyclerView
        recyclerView = binding.recyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(requireContext())
        recyclerView!!.adapter = adapter
        recyclerView!!.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))

        // Aggiungi un listener al bottone "Aggiungi" per aggiungere un nuovo elemento alla lista
        val addButton = binding.addButton
        addButton.setOnClickListener {
            showAddItemDialog()
        }

        // Aggiungi un item touch helper per eliminare gli elementi tramite lo swipe
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                viewHolder.itemView.elevation = 16F

                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                Collections.swap(toDoList, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                // Swipe verso destra
                if(direction == ItemTouchHelper.RIGHT) {
                    showEditItemDialog(position)
                }
                // Swipe verso sinistra
                else {
                    val id = toDoList[position].id
                    onItemDelete(id, position)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val backgroundCornerOffset = 20

                if (dX > 0) { // Swipe to right
                    val iconMargin = (itemView.height - iconEdit.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - iconEdit.intrinsicHeight) / 2
                    val iconBottom = iconTop + iconEdit.intrinsicHeight
                    val iconRight = itemView.left + iconMargin + iconEdit.intrinsicWidth
                    val iconLeft = itemView.left + iconMargin
                    iconEdit.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.color = resources.getColor(R.color.primary, null)
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt() + backgroundCornerOffset,
                        itemView.bottom
                    )
                } else if (dX < 0) { // Swipe to left
                    val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                    val iconBottom = iconTop + icon.intrinsicHeight
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.color = Color.RED
                    background.setBounds(
                        itemView.right + dX.toInt() - backgroundCornerOffset,
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                } else { // view is unSwiped
                    background.setBounds(0, 0, 0, 0)
                }
                background.draw(c)
                if(dX <= -itemView.width * 0.1f) icon.draw(c)
                if(dX >= itemView.width * 0.1f) iconEdit.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                viewHolder?.itemView?.elevation = 0F
            }

        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NetworkUtils.notifyMissingNetwork(requireContext(), requireActivity())
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        toolbar = activity?.findViewById(R.id.topAppBar)!!
        toolbar.menu?.setGroupVisible(R.id.edit, false)
        toolbar.menu?.setGroupVisible(R.id.sett, true)

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.settings -> {
                    NavigationHelper.navigateTo(view, R.id.optionsFragment)
                }
            }
            true
        }
        init()
        // Mette la home come main
        bottomNav.menu.getItem(2).isChecked = true

    }
    // Adapter per la RecyclerView
    inner class ToDoListAdapter(private val items: List<ToDoItem>) : RecyclerView.Adapter<ToDoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToDoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.todo_element, parent, false)
            return ToDoViewHolder(view)
        }
        override fun onBindViewHolder(holder: ToDoViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = item.task
            holder.checkBox.isChecked = item.isDone
            holder.timeView.text = item.time
            holder.checkBox.setOnClickListener {
                item.isDone = !item.isDone
                modifyItemState(item.id, item.isDone)
            }
        }
        override fun getItemCount(): Int = items.size
    }

    // ViewHolder per gli elementi della RecyclerView
    inner class ToDoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        var timeView: TextView = itemView.findViewById(R.id.timeView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }
    private fun showAddItemDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)

        val editTextItem = dialogView.findViewById<EditText>(R.id.textItem)
        val editTimeItem = dialogView.findViewById<EditText>(R.id.timeItem)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_item))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val text = editTextItem.text.toString()
                val time = editTimeItem.text.toString()
                if (text.isNotBlank() && time.isNotBlank()) {
                    val todoItem = ToDoItem(text, time)
                    manualUpdateUI = false
                    val dbItem = todoListRef.child("todolist/$currentDate").push()
                    todoItem.id = dbItem.key.toString()
                    todoItem.userId = auth.getCurrentUid().toString()
                    dbItem.setValue(todoItem)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        pickTimeListener(editTimeItem)
        dialog.show()
    }

    private fun showEditItemDialog(index: Int) {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val editTextItem = dialogView.findViewById<EditText>(R.id.textItem)
        val editTimeItem = dialogView.findViewById<EditText>(R.id.timeItem)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_item))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.edit)) { _, _ ->
                val text = editTextItem.text.toString()
                val time = editTimeItem.text.toString()

                if (text.isNotBlank()) {
                    editItemState(toDoList[index].id, text, time, index)
                }
                else{
                    adapter.notifyItemChanged(index)
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                adapter.notifyItemChanged(index)
            }
            .create()
        pickTimeListener(editTimeItem)
        dialog.show()
    }
    private fun init() {
        binding.apply {
            toolbar.title = sdf.format(cal.time).replaceFirstChar { it.uppercase() }
            calendarView.setHasFixedSize(true)
            calendarView.adapter = calendarAdapter
        }
    }
    private fun displayDatePicker() {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

        datePicker.show(parentFragmentManager, "DatePicker")
        datePicker.addOnPositiveButtonClickListener {

        try {
            toolbar.title = sdf.format(it)
            cal.time = Date(it)
            getDates()
            onSelect(CalendarData(Date(it), true), (cal.get(Calendar.DAY_OF_MONTH) - 1))
        } catch (_: ParseException) {

            }
        }
    }
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
        binding.calendarView.scrollToPosition(position)
        calendarList.forEachIndexed { index, calendarModel ->
            calendarModel.isSelected = index == position
        }
        calendarAdapter.updateList(calendarList)
        currentDate = calendarData.calendarYear + "/" + calendarData.calendarMonth + "/" + calendarData.calendarDate
        manualUpdateUI = false
        val todo = ToDoFetcher()
        todo.fetchToDoListFragmentFromFirebase(toDoList, currentDate, adapter, todoListRef)

    }

    private fun pickTimeListener(timeText: EditText){

        timeText.setOnClickListener {
            Log.d("TEST", "x")
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .build()

            picker.addOnPositiveButtonClickListener {
                val hour = picker.hour
                val minute = picker.minute
                val time = String.format("%02d:%02d", hour, minute)
                timeText.setText(time)
            }

            picker.show(parentFragmentManager, "TimePicker")

        }
    }

    override fun modifyItemState(itemObjectId: String, isDone: Boolean) {
        manualUpdateUI = true
        val date = currentDate.split("/")
        val ref = todoListRef.child("todolist").child(date[0]).child(date[1]).child(date[2]).child(itemObjectId)
        ref.child("done").setValue(isDone)
    }

    override fun onItemDelete(itemObjectId: String, position: Int) {
        manualUpdateUI = true
        toDoList.removeAt(position)
        adapter.notifyItemRemoved(position)
        val date = currentDate.split("/")
        val ref = todoListRef.child("todolist").child(date[0]).child(date[1]).child(date[2]).child(itemObjectId)
        ref.removeValue()
    }

    override fun editItemState(itemObjectId: String, task: String, time: String, position: Int) {
        manualUpdateUI = true
        toDoList[position].task = task
        toDoList[position].time = time
        val date = currentDate.split("/")
        val ref = todoListRef.child("todolist").child(date[0]).child(date[1]).child(date[2]).child(itemObjectId)
        ref.child("task").setValue(task)
        ref.child("time").setValue(time)
        adapter.notifyItemChanged(position)
    }


}
