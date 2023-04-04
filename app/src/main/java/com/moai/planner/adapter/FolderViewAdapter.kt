package com.moai.planner.adapter

import android.app.Application
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.moai.planner.R
import com.moai.planner.data.user.UserAuthentication
import com.moai.planner.util.FolderItem
import com.google.firebase.database.FirebaseDatabase

class FolderViewAdapter(private val mList: List<FolderItem>) : RecyclerView.Adapter<FolderViewAdapter.ViewHolder>() {

    private lateinit var mListener : OnItemClickListener
    private var auth = UserAuthentication(Application())
    private var database = FirebaseDatabase.getInstance()
    private var ref = database.getReference("users/" + auth.getCurrentUid().toString())

    interface OnItemClickListener {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)

    }
    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_template, parent, false)

        return ViewHolder(view, mListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = mList[position]
        holder.textViewName.text = item.folder_name
        holder.textViewSize.text = item.folder_files
        holder.checkbox.isChecked = item.isFavourite
        holder.icon.setImageResource(item.icon)
        holder.checkbox.isVisible = item.icon != R.drawable.folder

        holder.checkbox.setOnClickListener {
            item.isFavourite = !item.isFavourite
            modifyItemState(item.id, item.isFavourite, "/" + item.path)
        }

    }
    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    fun getFileName(position: Int): String {
        return mList[position].folder_name
    }
    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View, listener: OnItemClickListener) : RecyclerView.ViewHolder(ItemView) {
        val textViewName: TextView = itemView.findViewById(R.id.fileName)
        val textViewSize: TextView = itemView.findViewById(R.id.fileSize)
        val checkbox: CheckBox = itemView.findViewById(R.id.star)
        val icon: ImageView = itemView.findViewById(R.id.fileIcon)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }

            itemView.setOnLongClickListener {
                listener.onItemLongClick(adapterPosition)
                return@setOnLongClickListener true
            }
        }
    }

    private fun modifyItemState(itemObjectId: String, isFavourite: Boolean, path: String = "") {
        val value = ref.child("favourites/${path}").child(itemObjectId)
        value.child("favourite").setValue(isFavourite)
    }

    fun onItemDelete(itemObjectId: String, path: String = "") {
        val value = ref.child("favourites/${path}").child(itemObjectId)
        value.removeValue()
    }

}
