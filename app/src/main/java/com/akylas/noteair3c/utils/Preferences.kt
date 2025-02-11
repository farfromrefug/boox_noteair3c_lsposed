package com.akylas.noteair3c.utils

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.content.Context
import com.akylas.noteair3c.Log
import com.akylas.noteair3c.ModuleMain.Companion.usingKreader
import com.akylas.noteair3c.lsposed.BuildConfig
import de.robv.android.xposed.XSharedPreferences

@SuppressLint("CommitPrefEdits")
class Preferences {
    companion object {
        const val SP_NAME = "Note3CLSposed"

        private const val default_str = ""
        private const val default_bool = true
    }
        /**
         * To read preference of user.
         */
        private val prefs by lazy {
            val pref = XSharedPreferences(BuildConfig.APPLICATION_ID)
            pref.makeWorldReadable()
            pref
        }

        fun getString(key: String, defaultValue: String = default_str): String {
            return prefs.getString(key, defaultValue) ?: defaultValue
        }

        fun setString(key: String, value: String) {
            with(prefs.edit() ?: return) {
                putString(key, value)
//                apply()
                commit()
            }
        }

        fun getBoolean(key: String, defaultValue: Boolean = default_bool): Boolean {
            return prefs.getBoolean(key, defaultValue)
        }

        fun setBoolean(key: String, value: Boolean) {
            with(prefs.edit() ?: return) {
                putBoolean(key, value)
//                apply()
                commit()
            }
        }

        fun getInt(key: String, defaultValue: Int = 0): Int {
            return prefs.getInt(key, defaultValue)
        }

        fun setInt(key: String, value: Int) {
            with(prefs.edit() ?: return) {
                putInt(key, value)
                commit()
            }
        }

        fun getLong(key: String, defaultValue: Long = 0): Long {
            return prefs.getLong(key, defaultValue)
        }

        fun setLong(key: String, value: Long) {
            with(prefs.edit() ?: return) {
                putLong(key, value)
                commit()
            }
        }

        fun getFloat(key: String, defaultValue: Float = 0F): Float {
            return prefs.getFloat(key, defaultValue)
        }

        fun setFloat(key: String, value: Float) {
            with(prefs.edit() ?: return) {
                putFloat(key, value)
                commit()
            }
        }

//    }
}