import android.location.GnssAntennaInfo.Listener
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
import com.example.moaiplanner.util.FolderItem

class FolderViewAdapter(private val mList: List<FolderItem>) : RecyclerView.Adapter<FolderViewAdapter.ViewHolder>() {

    private lateinit var mListener : onItemClickListener

    interface onItemClickListener {
        fun onItemClick(position: Int)
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
        if(item.icon == R.drawable.folder)
            holder.checkbox.isVisible = false

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.isFavourite = isChecked
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
        }
    }








}
