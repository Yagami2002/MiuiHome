package com.yuk.miuihome.module

import android.content.Context
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.yuk.miuihome.R
import com.yuk.miuihome.utils.LogUtil
import com.yuk.miuihome.utils.OwnSP.ownSP
import com.yuk.miuihome.utils.dip2px
import com.yuk.miuihome.utils.ktx.findClass
import com.yuk.miuihome.utils.px2dip
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class DockHook {

    private fun readStream(input: InputStream) {
        val reader = BufferedReader(InputStreamReader(input))
        var read = ""
        while (true) {
            val temp: String = reader.readLine() ?: break
            read += temp
        }
    }

    fun init() {
        if (ownSP.getBoolean("dockSettings", false)) {
            try {
                readStream(
                    Runtime.getRuntime()
                        .exec("su -c settings put system key_home_screen_search_bar_show_initiate 1").inputStream
                )
            } catch (ignore: Exception) {
            }
            val deviceConfigClass = "com.miui.home.launcher.DeviceConfig".findClass()
            val launcherClass = "com.miui.home.launcher.Launcher".findClass()
            XposedHelpers.findAndHookMethod(
                deviceConfigClass,
                "calcHotSeatsMarginTop",
                Context::class.java,
                Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.args[1] = false
                        super.beforeHookedMethod(param)
                    }
                })
            // 图标区域底部边距
            XposedHelpers.findAndHookMethod(
                deviceConfigClass,
                "calcHotSeatsMarginBottom",
                Context::class.java,
                Boolean::class.java,
                Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = dip2px((ownSP.getFloat("dockIconBottom", -1f) * 10).toInt())
                    }
                })
            // 搜索框宽度
            XposedHelpers.findAndHookMethod(
                deviceConfigClass,
                "calcSearchBarWidth",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val context = param.args[0] as Context
                        val deviceWidth = px2dip(context.resources.displayMetrics.widthPixels)
                        param.result =
                            dip2px(deviceWidth - (ownSP.getFloat("dockSide", -1f) * 10).toInt())
                    }
                })
            // Dock底部边距
            XposedHelpers.findAndHookMethod(
                deviceConfigClass,
                "calcSearchBarMarginBottom",
                Context::class.java,
                Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = dip2px((ownSP.getFloat("dockBottom", -1f) * 10).toInt())
                    }
                })
            // 宽度变化量
            XposedHelpers.findAndHookMethod(
                deviceConfigClass,
                "getSearchBarWidthDelta",
                XC_MethodReplacement.returnConstant(0)
            )
            XposedHelpers.findAndHookMethod(
                launcherClass,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        super.afterHookedMethod(param)
                        val searchBarObject = XposedHelpers.callMethod(
                            param.thisObject,
                            "getSearchBar"
                        ) as FrameLayout
                        val searchBarDesktop = searchBarObject.getChildAt(0) as RelativeLayout
                        val searchBarDrawer = searchBarObject.getChildAt(1) as RelativeLayout
                        val searchBarContainer = searchBarObject.parent as FrameLayout
                        val searchEdgeLayout = searchBarContainer.parent as FrameLayout
                        // 重新给搜索框容器排序
                        searchEdgeLayout.removeView(searchBarContainer)
                        searchEdgeLayout.addView(searchBarContainer, 0)
                        // 清空搜索图标和小爱同学
                        searchBarDesktop.removeAllViews()
                        // 修改高度
                        searchBarObject.layoutParams.height =
                            dip2px((ownSP.getFloat("dockHeight", -1f) * 10).toInt())
                        // 修改应用列表搜索框
                        val mAllAppViewField = launcherClass.getDeclaredField("mAppsView")
                        mAllAppViewField.isAccessible = true
                        val mAllAppView = mAllAppViewField.get(param.thisObject) as RelativeLayout
                        val mAllAppSearchView =
                            mAllAppView.getChildAt(mAllAppView.childCount - 1) as FrameLayout
                        searchBarObject.removeView(searchBarDrawer)
                        mAllAppSearchView.addView(searchBarDrawer)
                        searchBarDrawer.bringToFront()
                        val layoutParams = searchBarDrawer.layoutParams as FrameLayout.LayoutParams
                        searchBarDrawer.layoutParams.height = dip2px(45)
                        layoutParams.leftMargin = dip2px(15)
                        layoutParams.rightMargin = dip2px(15)
                        searchBarDrawer.layoutParams = layoutParams
                    }
                })
        }
    }
}