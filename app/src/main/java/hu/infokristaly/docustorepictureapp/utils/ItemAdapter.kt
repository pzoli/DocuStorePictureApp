package hu.infokristaly.docustorepictureapp.utils

import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import hu.infokristaly.docustorepictureapp.R
import hu.infokristaly.docustorepictureapp.model.DocInfo

class ItemAdapter(private var items: List<DocInfo>, private val isItemSelected: (position: Int) -> Boolean) : RecyclerView.Adapter<ItemViewHolder>() {
    var onItemClickListener: ((item: DocInfo, position: Int) -> Boolean)? = null
    var onItemLongClickListener: ((item: DocInfo, position: Int) -> Boolean)? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            val selected = onItemClickListener?.invoke(item, position)
            holder.itemView.isActivated = selected!!
        }
        holder.itemView.setOnLongClickListener {
            val selected = onItemLongClickListener?.invoke(item, position)
            holder.itemView.isActivated = selected!!
            selected
        }
         holder.itemView.isActivated = isItemSelected(position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(newItems: List<DocInfo>) {
        this.items = newItems
        notifyDataSetChanged()
        // Nagyobb listák esetén hatékonyabb lenne a DiffUtil használata
    }

}