package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import hu.infokristaly.docustorepictureapp.model.Organization


class OrganizationAdapter(private val context: Context, private val data: List<Pair<Int, Organization>>) : BaseAdapter() {
    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Pair<Int, Organization> = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val (key, value) = data[position]
        (view as TextView).text = "${value.name}"
        return view
    }
}

class CustomStringStringPairAdapter(private val context: Context, private val data: List<Pair<String, String>>) : BaseAdapter() {
    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Pair<String, String> = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_single_choice, parent, false)
        val (key, value) = data[position]
        (view as TextView).text = "$key: $value"
        return view
    }
}