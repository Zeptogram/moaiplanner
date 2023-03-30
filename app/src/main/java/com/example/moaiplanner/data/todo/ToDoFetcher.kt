package com.example.moaiplanner.data.todo

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.widget.TextView
import com.example.moaiplanner.ui.main.HomeFragment
import com.example.moaiplanner.ui.main.ToDoListFragment
import com.example.moaiplanner.util.ToDoItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("NotifyDataSetChanged")
class ToDoFetcher {

    fun fetchToDoListHomeFromFirebase(toDoList: MutableList<ToDoItem>, currentDate: String, toDoAdapter: HomeFragment.ToDoViewerAdapter, todoListRef: DatabaseReference, text: TextView) {
        val toDoListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                toDoList.clear()
                val date = currentDate.split("/")
                val toDoListData = snapshot.child("todolist").child(date[0]).child(date[1]).child(date[2]).children
                for (item in toDoListData) {
                    val toDoItem = item.getValue(ToDoItem::class.java)
                    if (toDoItem != null) {
                        if(toDoItem.id.isNotBlank() && !toDoItem.isDone) {
                            toDoList.add(toDoItem)
                        }
                    }
                }
                if (toDoList.isEmpty())
                    text.visibility = View.VISIBLE
                else
                    text.visibility = View.GONE

                toDoList.sortBy { item ->
                    val format = SimpleDateFormat("HH:mm",  Locale.getDefault())
                    val dateFormatted = format.parse(item.time)
                    dateFormatted?.time?.div((60 * 1000))
                }
                toDoAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("TODO-ERROR", "Error:", error.toException())
            }
        }
        todoListRef.addValueEventListener(toDoListListener)
    }



    fun fetchToDoListFragmentFromFirebase(toDoList: MutableList<ToDoItem>, currentDate: String, toDoAdapter: ToDoListFragment.ToDoListAdapter, todoListRef: DatabaseReference) {
        val toDoListListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                toDoList.clear()
                val date = currentDate.split("/")
                val toDoListData = snapshot.child("todolist").child(date[0]).child(date[1]).child(date[2]).children
                for (item in toDoListData) {
                    val toDoItem = item.getValue(ToDoItem::class.java)
                    if (toDoItem != null) {
                        if(toDoItem.id.isNotBlank()) {
                            toDoList.add(toDoItem)
                        }
                    }
                }
                toDoList.sortBy { item ->
                    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val dateFormmated = format.parse(item.time)
                    dateFormmated?.time?.div((60 * 1000))
                }
                toDoAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("TODO-ERROR", "Error:", error.toException())
            }
        }
        todoListRef.addValueEventListener(toDoListListener)
    }
}