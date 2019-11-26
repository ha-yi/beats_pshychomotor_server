package com.linov.beats_server

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Hayi Nukman at 2019-11-27
 * https://github.com/ha-yi
 */

class SimpleAdapter: RecyclerView.Adapter<SimpleAdapter.SimpleVH>() {
    private var listItem: List<String> = listOf()

    fun updateItems(newData: List<String>) {
        listItem = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleVH = SimpleVH(TextView(parent.context))

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: SimpleVH, position: Int) {
        holder.setData(listItem[position])
    }

    class SimpleVH(view: View): RecyclerView.ViewHolder(view) {
        fun setData(data: String) {
            (itemView as TextView).text = data
        }
    }
}

