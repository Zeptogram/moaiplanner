package com.moai.planner.util

data class ToDoItem(
    var task: String,
    var time: String,
    var isDone: Boolean = false,
    var id: String = "",
    var userId: String = ""
){
    constructor() : this("", "", false, "", "")
}