package hu.infokristaly.docustorepictureapp.utils

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hu.infokristaly.docustorepictureapp.R
import hu.infokristaly.docustorepictureapp.model.DocInfo
import java.text.SimpleDateFormat

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val itemTextView: TextView = itemView.findViewById(R.id.item_text)

    fun bind(docInfo: DocInfo) {
        val formatedCreatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm").format(docInfo.createdAt!!)
        itemTextView.text = "${formatedCreatedAt} - ${docInfo.organization?.name} - ${docInfo.subject?.value} - ${docInfo.comment}"
    }
}