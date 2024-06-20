package hu.infokristaly.docustorepictureapp.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import hu.infokristaly.forrasimageserver.entity.Subject

class SubjectAdapter(private val context: Context, private val data: List<Subject>): BaseAdapter() {
    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Subject = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_single_choice, parent, false)
        val subject = data[position]
        (view as TextView).text = subject.value
        return view
    }

}