package hu.infokristaly.docustorepictureapp.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import hu.infokristaly.docustorepictureapp.model.Organization


class OrganizationAdapter(private val context: Context, private val data: List<Organization>) : BaseAdapter() {
    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Organization = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_single_choice, parent, false)
        val organization = data[position]
        (view as TextView).text = organization.name
        return view
    }
}
