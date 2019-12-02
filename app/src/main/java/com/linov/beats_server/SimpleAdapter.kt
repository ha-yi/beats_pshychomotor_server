package com.linov.beats_server

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recycler_item.view.*

/**
 * Created by Hayi Nukman at 2019-11-27
 * https://github.com/ha-yi
 */

class SimpleAdapter: RecyclerView.Adapter<SimpleAdapter.SimpleVH>() {
    private var listItem: List<Clients> = listOf()

    fun updateItems(newData: List<Clients>) {
        listItem = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleVH = SimpleVH(
        LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
    )

    override fun getItemCount(): Int = listItem.size

    override fun onBindViewHolder(holder: SimpleVH, position: Int) {
        holder.setData(listItem[position])
    }

    class SimpleVH(view: View): RecyclerView.ViewHolder(view) {
        fun setData(data: Clients) {
            var text = "${if (data.groupReady) "[G]" else ""} ${data.name} (${data.ip})"

            itemView.setBackgroundColor(Color.TRANSPARENT)
            if (data.groupReady) {
                itemView.setBackgroundColor(Color.CYAN)
            }

            if (data.onGroupBoard) {
                text = "[G] ${data.name} (${data.ip}) running test"
            }

            itemView.apply {
                txtContent.text = text
            }
        }
    }
}

