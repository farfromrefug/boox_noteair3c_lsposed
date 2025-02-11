package com.akylas.noteair3c

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList
import com.akylas.noteair3c.lsposed.R;

class AppsAdapter(aContext: Context?, listData: ArrayList<AppsItem?>) :
    RecyclerView.Adapter<AppsAdapter.ViewHolder?>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var appName: TextView
        var appPackageName: TextView

        init {
            appName = itemView.findViewById<TextView>(R.id.appName)
            appPackageName = itemView.findViewById<TextView>(R.id.appPackageName)
        }
    }

    private val listData: ArrayList<AppsItem?>

    init {
        this.listData = listData
    }

    override fun getItemCount(): Int {
        return listData.size
    }

    fun getItem(position: Int): AppsItem? {
        return listData.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.list_row_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.appName.setText(item!!.getAppName())
        holder.appPackageName.setText(item.getAppPackageName())
    }
}