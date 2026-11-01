package com.khuyutalarab.game

import android.content.Context

/** إحصاءات اللاعب محفوظة محليًا على الجهاز فقط (SharedPreferences). */
class Stats(context: Context) {
    private val prefs = context.getSharedPreferences("khuyut_stats", Context.MODE_PRIVATE)

    var played: Int
        get() = prefs.getInt("played", 0)
        private set(v) = prefs.edit().putInt("played", v).apply()
    var won: Int
        get() = prefs.getInt("won", 0)
        private set(v) = prefs.edit().putInt("won", v).apply()
    var streak: Int
        get() = prefs.getInt("streak", 0)
        private set(v) = prefs.edit().putInt("streak", v).apply()
    var best: Int
        get() = prefs.getInt("best", 0)
        private set(v) = prefs.edit().putInt("best", v).apply()

    var puzzleIndex: Int
        get() = prefs.getInt("puzzle_index", 0)
        set(v) = prefs.edit().putInt("puzzle_index", v).apply()
    /** تقدم اللغز الحالي كنص مبسّط (يُحفظ محليًا). null = لا يوجد تقدم محفوظ. */
    var saved: String?
        get() = prefs.getString("saved", null)
        set(v) = prefs.edit().putString("saved", v).apply()

    /** هل عُرضت شاشة «كيف تلعب» أول مرة؟ */
    var seenHelp: Boolean
        get() = prefs.getBoolean("seen_help", false)
        set(v) = prefs.edit().putBoolean("seen_help", v).apply()

    /** هل اللغز الحالي أُعيد للتدريب؟ (يُحفظ حتى لا تُحتسب النتيجة بعد إغلاق التطبيق) */
    var practice: Boolean
        get() = prefs.getBoolean("practice", false)
        set(v) = prefs.edit().putBoolean("practice", v).apply()

    /** الصوت مفعّل؟ (الافتراضي نعم) */
    var soundOn: Boolean
        get() = prefs.getBoolean("sound_on", true)
        set(v) = prefs.edit().putBoolean("sound_on", v).apply()

    var lastDay: Long
        get() = prefs.getLong("last_day", -1L)
        set(v) = prefs.edit().putLong("last_day", v).apply()

    fun record(win: Boolean) {
        played += 1
        if (win) {
            won += 1
            streak += 1
            if (streak > best) best = streak
        } else {
            streak = 0
        }
    }
}
