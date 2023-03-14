import android.app.Application
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.util.FolderItem
import com.google.firebase.database.FirebaseDatabase

class FolderViewAdapter(private val mList: List<FolderItem>) : RecyclerView.Adapter<FolderViewAdapter.ViewHolder>() {

    private lateinit var mListener : onItemClickListener
    private var auth = AuthRepository(Application())
    private var database = FirebaseDatabase.getInstance()
    private var ref = database.getReference("users/" + auth.getCurrentUid().toString())


    interface onItemClickListener {
        fun onItemClick(position: Int)
        fun onItemLongClick(position: Int)

    }


    fun setOnItemClickListener(listener: onItemClickListener) {
        mListener = listener
    }


    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.note_template, parent, false)

        return ViewHolder(view, mListener)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = mList[position]
        // sets the image to the imageview from our itemHolder class
        // sets the text to the textview from our itemHolder class
        holder.textViewName.text = item.folder_name
        holder.textViewSize.text = item.folder_files
        holder.checkbox.isChecked = item.isFavourite
        holder.icon.setImageResource(item.icon)
        holder.checkbox.isVisible = item.icon != R.drawable.folder

        holder.checkbox.setOnClickListener {
            item.isFavourite = !item.isFavourite
            modifyItemState(item.id, item.isFavourite)
            Log.d("TEST", "Called Checked: " + position.toString())
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
    class ViewHolder(ItemView: View, listener: onItemClickListener) : RecyclerView.ViewHolder(ItemView) {
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

    private fun modifyItemState(itemObjectId: String, isFavourite: Boolean) {
        val value = ref.child("favourites").child(itemObjectId)
        value.child("favourite").setValue(isFavourite)
    }

    fun onItemDelete(itemObjectId: String) {
        val value = ref.child("favourites").child(itemObjectId)
        value.removeValue()
    }





}
