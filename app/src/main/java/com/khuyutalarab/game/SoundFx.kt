package com.khuyutalarab.game

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * نغمات قصيرة من مولّد نغمات النظام (بلا ملفات صوتية). أي فشل في الجهاز يُتجاهل بصمت
 * حتى لا تتعطل اللعبة بسبب الصوت.
 */
class SoundFx {
    var enabled: Boolean = true
    private var tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    } catch (e: Exception) {
        null
    }

    private fun play(type: Int, ms: Int) {
        if (!enabled) return
        try {
            tone?.startTone(type, ms)
        } catch (e: Exception) {
            // تجاهل
        }
    }

    fun correct() = play(ToneGenerator.TONE_PROP_ACK, 150)
    fun wrong() = play(ToneGenerator.TONE_PROP_NACK, 200)
    fun win() = play(ToneGenerator.TONE_CDMA_CONFIRM, 600)
    fun lose() = play(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 500)

    fun release() {
        try {
            tone?.release()
        } catch (e: Exception) {
            // تجاهل
        }
        tone = null
    }
}
