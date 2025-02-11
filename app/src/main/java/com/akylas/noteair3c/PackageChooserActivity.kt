package com.akylas.noteair3c

import android.app.ListActivity
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.akylas.noteair3c.lsposed.R;
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageChooserActivity : ListActivity() {
    private var adapter: AppAdapter? = null
    lateinit var pm: PackageManager

    public override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.package_chooser_layout)
        pm = getPackageManager()

        GlobalScope.launch {
            val main = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
            main.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            val launchables: MutableList<ResolveInfo> =
                pm.queryIntentActivities(main, 0)
            java.util.Collections.sort<ResolveInfo>(
                launchables,
                ResolveInfo.DisplayNameComparator(pm)
            )
            Log.d("launchables ${launchables}")
            adapter = AppAdapter(pm, launchables)
            withContext(Dispatchers.Main) {
                setListAdapter(adapter)
            }
        }
    }

    override fun onListItemClick(
        l: android.widget.ListView?,
        v: android.view.View?,
        position: kotlin.Int,
        id: kotlin.Long
    ) {
        val launchable: ResolveInfo? = adapter?.getItem(position)
        val activity: ActivityInfo? = launchable?.activityInfo
        if (activity != null) {
            val name: ComponentName = ComponentName(
                activity.applicationInfo.packageName,
                activity.name
            )

            val intentMessage = android.content.Intent()
            intentMessage.putExtra("package_name", name.packageName)
            setResult(1, intentMessage)
            finish()
        }
    }

    internal inner class AppAdapter(
        pm: PackageManager,
        apps: kotlin.collections.MutableList<ResolveInfo>
    ) : ArrayAdapter<ResolveInfo>(
        this@PackageChooserActivity,
        R.layout.package_chooser_row,
        apps
    ) {
        private val pm: PackageManager

        init {
            this.pm = pm
        }


        override fun getView(
            position: Int, convertView: View?, parent: ViewGroup
        ): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = newView(parent)
            }

            bindView(position, convertView!!)
            return (convertView)
        }

        private fun newView(parent: android.view.ViewGroup?): android.view.View? {
            return (getLayoutInflater().inflate(R.layout.package_chooser_row, parent, false))
        }

        private fun bindView(position: kotlin.Int, row: android.view.View) {
            val label = row.findViewById<android.view.View?>(R.id.label) as android.widget.TextView
            label.setText(getItem(position)?.loadLabel(pm))
            val icon = row.findViewById<android.view.View?>(R.id.icon) as android.widget.ImageView
            icon.setImageDrawable(getItem(position)?.loadIcon(pm))
        }
    }

}