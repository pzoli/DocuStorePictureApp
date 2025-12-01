package hu.infokristaly.docustorepictureapp.utils

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hu.infokristaly.docustorepictureapp.R
import hu.infokristaly.docustorepictureapp.model.DocInfo
import java.text.SimpleDateFormat

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val itemTextView: TextView = itemView.findViewById(R.id.item_text)
    private val itemDateView: TextView = itemView.findViewById(R.id.item_date)
    private val itemCommentView: TextView = itemView.findViewById(R.id.item_comment)
    private val itemLocationView: TextView = itemView.findViewById(R.id.item_location)
    fun bind(docInfo: DocInfo) {
        val formatedCreatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm").format(docInfo.createdAt!!)
        itemDateView.text = formatedCreatedAt
        itemTextView.text = "${docInfo.organization?.name} - ${docInfo.subject?.value}"
        itemCommentView.text = docInfo.comment
        itemLocationView.text = docInfo.docLocation?.getLocatoinPath()
    }
}