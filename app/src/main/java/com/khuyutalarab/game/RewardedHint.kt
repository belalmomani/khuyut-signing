package com.khuyutalarab.game

import android.app.Activity
import android.content.pm.ApplicationInfo
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener
import com.startapp.sdk.adsbase.adlisteners.AdEventListener

/**
 * إعلان فيديو مكافأة (Start.io) مقابل تلميح. يُحمَّل مسبقًا، ويُمنح التلميح فقط
 * إذا أكمل اللاعب الفيديو. بدون App ID حقيقي في res/values/ads.xml لا يعمل الإعلان
 * ويظهر للاعب «الإعلان غير متاح».
 */
class RewardedHint(private val activity: Activity) {
    private var ad: StartAppAd? = null
    private var ready = false
    private var loading = false

    private val appId = activity.getString(R.string.startio_app_id)
    private val configured: Boolean
        get() = appId.isNotBlank() && !appId.startsWith("REPLACE")

    fun init() {
        if (!configured) return
        // إعلانات اختبار في النسخ التجريبية (debug) فقط، لتجنب الضغط على إعلانات حقيقية بالخطأ
        val debuggable = (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        StartAppSDK.setTestAdsEnabled(debuggable)
        StartAppSDK.init(activity, appId, false) // false: بدون إعلانات الإرجاع التلقائية
        StartAppAd.disableSplash()
        load()
    }

    private fun load() {
        if (!configured || loading || ready) return
        loading = true
        val newAd = StartAppAd(activity)
        ad = newAd
        newAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, object : AdEventListener {
            override fun onReceiveAd(received: Ad) {
                ready = true
                loading = false
            }

            override fun onFailedToReceiveAd(failed: Ad?) {
                ready = false
                loading = false
            }
        })
    }

    /** onReward: بعد إكمال الفيديو وإغلاق الإعلان. onUnavailable: لا يوجد إعلان جاهز أو فشل عرضه. */
    fun show(onReward: () -> Unit, onUnavailable: () -> Unit) {
        val current = ad
        if (!ready || current == null) {
            load()
            onUnavailable()
            return
        }
        var rewarded = false
        current.setVideoListener { rewarded = true }
        current.showAd(object : AdDisplayListener {
            override fun adHidden(shown: Ad?) {
                ready = false
                ad = null
                load()
                if (rewarded) onReward()
            }

            override fun adDisplayed(shown: Ad?) {}
            override fun adClicked(shown: Ad?) {}

            override fun adNotDisplayed(shown: Ad?) {
                ready = false
                ad = null
                load()
                onUnavailable()
            }
        })
    }
}
