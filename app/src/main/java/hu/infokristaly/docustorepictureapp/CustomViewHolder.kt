package hu.infokristaly.docustorepictureapp

import android.graphics.Color
import android.view.View
import android.widget.TextView
import com.amrdeveloper.treeview.TreeNode
import com.amrdeveloper.treeview.TreeViewHolder
import java.text.SimpleDateFormat

class CustomViewHolder(itemView: View) : TreeViewHolder(itemView) {
    private val itemTextView: TextView = itemView.findViewById(R.id.node_text)

    override fun bindTreeNode(node: TreeNode) {
        super.bindTreeNode(node)
        if (node.isSelected()) {
            itemView.setBackgroundColor(Color.WHITE);
            itemTextView.setTextColor(Color.BLACK);
        } else {
            itemView.setBackgroundColor(Color.BLACK);
            itemTextView.setTextColor(Color.WHITE);
        }

        itemTextView.text = node.value.toString()
    }
}