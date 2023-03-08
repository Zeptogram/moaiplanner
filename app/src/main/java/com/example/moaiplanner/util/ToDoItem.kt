package com.example.moaiplanner.util

data class ToDoItem(
    val task: String,
    var time: String,
    var isDone: Boolean = false,
    var id: String = "",
    var userId: String = ""
) {
    constructor() : this("", "", false, "", "")
}