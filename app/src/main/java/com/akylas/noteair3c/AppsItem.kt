package com.akylas.noteair3c

import java.io.Serializable

class AppsItem : Serializable {
    private var appName: String? = null
    private var appPackageName: String? = null

    fun getAppName(): String? {
        return appName
    }

    fun setAppName(appName: String?) {
        this.appName = appName
    }

    fun getAppPackageName(): String? {
        return appPackageName
    }

    fun setAppPackageName(appPackageName: String?) {
        this.appPackageName = appPackageName
    }
}