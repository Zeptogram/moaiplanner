package com.example.moaiplanner.adapter

import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.moaiplanner.R
import com.example.moaiplanner.data.calendar.CalendarData
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.databinding.RowCalendarDateBinding
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.model.SettingsViewModelFactory


class CalendarAdapter(
    private val calendarInterface: CalendarInterface,
    private val list: ArrayList<CalendarData>,

) :
    RecyclerView.Adapter<CalendarAdapter.MyViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val binding =
            RowCalendarDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun updateList(calendarList: ArrayList<CalendarData>) {
        list.clear()
        list.addAll(calendarList)
        notifyDataSetChanged()
    }


    inner class MyViewHolder(private val binding: RowCalendarDateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(calendarDateModel: CalendarData) {

            val nightModeFlags = binding.root.context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK

            val calendarDay = binding.tvCalendarDay
            val calendarDate = binding.tvCalendarDate
            val cardView = binding.root

            if (calendarDateModel.isSelected) {
                calendarDay.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.white
                    )
                )
                calendarDate.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.white
                    )
                )
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.primary
                    )
                )
            } else {
                when (nightModeFlags) {
                    Configuration.UI_MODE_NIGHT_NO -> {
                        calendarDay.setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.black
                            )
                        )
                        calendarDate.setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.black
                            )
                        )
                        cardView.setCardBackgroundColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.light_background_variant
                            )
                        )
                    }
                    Configuration.UI_MODE_NIGHT_YES -> {
                        calendarDay.setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.white
                            )
                        )
                        calendarDate.setTextColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.white
                            )
                        )
                        cardView.setCardBackgroundColor(
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.gray
                            )
                        )
                    }
                }
            }

            calendarDay.text = calendarDateModel.calendarDay
            calendarDate.text = calendarDateModel.calendarDate
            cardView.setOnClickListener {
                calendarInterface.onSelect(calendarDateModel, adapterPosition)
            }
        }

    }


    interface CalendarInterface {
        fun onSelect(calendarData: CalendarData, position: Int)
    }

}
