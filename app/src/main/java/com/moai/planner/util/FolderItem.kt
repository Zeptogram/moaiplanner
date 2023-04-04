package com.moai.planner.util

data class FolderItem(
    val folder_name: String,
    var folder_files: String,
    var isFavourite: Boolean = false,
    var icon: Int,
    var path: String = "",
    var id: String = "",
    var userId: String = "",
){
    constructor() : this("", "", false, 0, "", "", "")
}