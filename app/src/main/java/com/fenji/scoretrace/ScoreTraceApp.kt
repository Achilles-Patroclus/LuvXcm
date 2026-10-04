package com.fenji.scoretrace

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.fenji.scoretrace.data.player.MusicPlayerManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** 应用入口：Hilt 依赖注入的宿主 */
@HiltAndroidApp
class ScoreTraceApp : Application() {

    @Inject
    lateinit var musicPlayerManager: MusicPlayerManager

    /** 处于「已 started」状态的 Activity 数量：归 0 表示应用退到后台，升到 1 表示回到前台 */
    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {

            override fun onActivityStarted(activity: Activity) {
                activityCount++
                if (activityCount == 1) musicPlayerManager.onAppForeground()
            }

            override fun onActivityStopped(activity: Activity) {
                // 计数必须先减：转屏时下面会 return，若在这里不减，这个 Activity 的份额会
                // 永久留在计数里，之后退后台就永远等不到 activityCount == 0，后台暂停会失效。
                activityCount--

                // isChangingConfigurations 为 true 表示马上会重建（转屏等），不算退后台
                if (activity.isChangingConfigurations) return

                if (activityCount == 0) musicPlayerManager.onAppBackground()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

            override fun onActivityResumed(activity: Activity) = Unit

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
