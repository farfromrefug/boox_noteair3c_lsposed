package com.akylas.noteair3c

import android.content.Context
import com.akylas.noteair3c.lsposed.R;
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akylas.noteair3c.lsposed.BuildConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.ArrayList

class WhitelistAppsActivity : AppCompatActivity() {
    var recyclerView: RecyclerView? = null
    var sharedPreferences: SharedPreferences? = null
    var whitelistAppsAdapter: AppsAdapter? = null
    var listData: ArrayList<AppsItem?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whitelist_apps)

        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.setLayoutManager(LinearLayoutManager(this))
        listData = ArrayList<AppsItem?>()
        sharedPreferences = this.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_WORLD_READABLE)
        whitelistAppsAdapter = AppsAdapter(this, listData!!)
        recyclerView!!.setAdapter(whitelistAppsAdapter)
        loadPackagesFromWhitelist()

        val simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                return false // true if moved, false otherwise
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                verifyAndRemovePackage(
                    listData!!.get(viewHolder.getLayoutPosition())!!.getAppPackageName()
                )
            }
        }
        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.whitelist_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.getItemId()
        if (id == R.id.action_add_whitelist) {
            startActivityForResult(
                Intent(
                    this@WhitelistAppsActivity,
                    PackageChooserActivity::class.java
                ), 999
            )
        } else if (id == R.id.action_add_whitelist_package) {
            showManuallyAddPackageDialog();
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            if (requestCode == 999) {
                val pkg = data.getStringExtra("package_name")
                if (pkg != null) {
                    verifyAndAddPackage(pkg)
                }
            } else if (requestCode == 998) {
                val pkg = data.getStringExtra("package_name")
                verifyAndRemovePackage(pkg)
            }
        }
    }

    fun loadPackagesFromWhitelist() {
        val apps = sharedPreferences!!.getString("reader_apps", "")!!.split(",").toList()
            .filter { it.length > 0 }
        if (!listData!!.isEmpty()) {
            listData!!.clear()
        }
        if (!apps.isEmpty()) {
            for (r in apps) {
                Log.d("loading apps $r")
                val appItem = AppsItem()
                appItem.setAppPackageName(r)
                try {
                    appItem.setAppName(
                        getPackageManager().getApplicationLabel(
                            getPackageManager().getApplicationInfo(
                                r,
                                PackageManager.GET_META_DATA
                            )
                        ).toString()
                    )
                    listData!!.add(appItem)
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
        whitelistAppsAdapter!!.notifyDataSetChanged()
    }


    fun verifyAndAddPackage(packageName: String) {
        val apps = sharedPreferences!!.getString("reader_apps", "")!!.split(",").toMutableSet()
        apps.add(packageName)
        with(sharedPreferences!!.edit() ?: return) {
            putString("reader_apps", apps.filter { it.length > 0 }.distinct().joinToString(","))
            commit()
        }
        loadPackagesFromWhitelist()
    }

    fun verifyAndRemovePackage(packageName: String?) {
        val apps = sharedPreferences!!.getString("reader_apps", "")!!.split(",").toMutableSet()
        apps.remove(packageName)
        with(sharedPreferences!!.edit() ?: return) {
            putString("reader_apps", apps.filter { it.length > 0 }.distinct().joinToString(","))
            commit()
        }
        loadPackagesFromWhitelist()
    }
    fun showManuallyAddPackageDialog() {
        val customLayout = layoutInflater.inflate(R.layout.dialog_input, null)
        val alertDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Whitelist apps")
            .setMessage("Please enter the package name of the app you want to add")
            .setPositiveButton("OK") { dialog, _ ->
                // Handle the positive button click, e.g., get the input text and process it
                val input = customLayout.findViewById<TextInputEditText>(R.id.textInput)?.text?.toString()
                if (!input.isNullOrEmpty()) {
                    verifyAndAddPackage(input)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        alertDialog.setView(customLayout)

        // Show the alert dialog
        alertDialog.show()

    }
}
