package com.moai.planner.util


interface ToDoItemListener {
    fun modifyItemState(itemObjectId: String, isDone: Boolean)
    fun onItemDelete(itemObjectId: String, position: Int)
    fun editItemState(itemObjectId: String, task: String, time: String, position: Int)
}