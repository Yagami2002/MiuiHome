package com.yuk.miuihome

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build

@SuppressLint("StaticFieldLeak")
object HomeContext {
    lateinit var application: Application
    lateinit var context: Context
    lateinit var classLoader: ClassLoader
    lateinit var activity: Activity
    lateinit var resInstance: ResInject
    var isAlpha = false
    var isWidgetLauncher = false
    var versionCode: Long = -1L
}