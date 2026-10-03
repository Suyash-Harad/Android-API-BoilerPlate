package com.bartech.api_usage_boilerplate

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * App-wide singleton wrapper around Android's TextToSpeech engine, used to speak out
 * scan/dispatch result messages from showResult() across every activity.
 *
 * Initialized once per process using the APPLICATION context — never pass an Activity
 * context here, since TextToSpeech holds onto whatever context it's given for its
 * lifetime, and an Activity context would leak the destroyed activity.
 *
 * Enable/disable state is read fresh from Prefs on every speak() call, so flipping the
 * toggle on the Settings screen takes effect immediately for the next result shown on
 * any screen — no need to notify every activity manually.
 */
object VoiceAnnouncer {

    private var tts: TextToSpeech? = null
    private var isReady = false

    // If speak() is called before the engine finishes starting up (e.g. the very first
    // result right after a cold app start), we remember the latest message and speak it
    // once init completes rather than silently dropping it.
    private var pendingText: String? = null

    fun init(context: Context) {
        if (tts != null) return // already initializing/initialized for this process

        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                tts?.language = Locale.getDefault()
                pendingText?.let { text ->
                    speakInternal(text)
                    pendingText = null
                }
            }
        }
    }

    /**
     * Speaks [text] aloud if the user has "Result Voice Setting" enabled. Safe to call
     * from any activity, any time — silently does nothing if voice is disabled.
     */
    fun speak(context: Context, text: String) {
        if (!Prefs(context).getVoiceEffect()) return
        if (text.isBlank()) return

        if (tts == null) init(context)

        if (isReady) {
            speakInternal(text)
        } else {
            pendingText = text
        }
    }

    private fun speakInternal(text: String) {
        // QUEUE_FLUSH: a new result interrupts whatever was still being read out, so
        // rapid consecutive scans don't queue up a backlog of stale announcements.
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "result_utterance")
    }

    /** Stops speech immediately — e.g. call when the user flips the setting off mid-utterance. */
    fun stop() {
        tts?.stop()
    }

    /**
     * Releases the TTS engine. Optional — Android reclaims this fine on process death, so
     * you only need this if you want strict cleanup (e.g. from a custom Application's
     * onTerminate(), which itself is rarely called on real devices).
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}