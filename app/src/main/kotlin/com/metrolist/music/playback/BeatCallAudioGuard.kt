/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Tells when the phone is using the Bluetooth "hands-free" link (HFP/SCO) for a call or a voice message.
 *
 * While an app records or plays voice through the car's microphone (WhatsApp voice notes, calls), Android
 * moves the Bluetooth link from A2DP (stereo, full quality) to SCO (mono, 8–16 kHz, no bass). Any music
 * still playing goes through that link and sounds like a phone call. This cannot be avoided from a music
 * app: A2DP and SCO cannot carry audio at the same time on almost every car stereo. What the music app can
 * do is not play during that time and come back only once the link is back to A2DP.
 *
 * [onChange] is called on the main thread: `true` as soon as the hands-free link starts, `false` a moment
 * after it ends (the car needs about a second to switch back to A2DP).
 */
class BeatCallAudioGuard(
    private val context: Context,
    private val audioManager: AudioManager,
    private val onChange: (active: Boolean) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var scoConnected = false
    private var started = false

    var isActive: Boolean = false
        private set

    /** When the current hands-free period started, in [System.currentTimeMillis] time. */
    var activeSince: Long = 0L
        private set

    private val scoReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)
                scoConnected = state == AudioManager.SCO_AUDIO_STATE_CONNECTED || state == AudioManager.SCO_AUDIO_STATE_CONNECTING
                evaluate()
            }
        }

    private val modeListener: Any? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AudioManager.OnModeChangedListener { evaluate() } else null

    private val communicationDeviceListener: Any? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioManager.OnCommunicationDeviceChangedListener { evaluate() }
        } else {
            null
        }

    private val releaseCheck =
        Runnable {
            if (isActive && !computeActive()) {
                isActive = false
                Timber.tag(TAG).d("Bluetooth hands-free link ended")
                onChange(false)
            }
        }

    fun start() {
        if (started) return
        started = true
        runCatching {
            ContextCompat.registerReceiver(
                context,
                scoReceiver,
                IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.onFailure { Timber.tag(TAG).w(it, "Cannot listen to SCO state") }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val executor = ContextCompat.getMainExecutor(context)
            runCatching {
                audioManager.addOnModeChangedListener(executor, modeListener as AudioManager.OnModeChangedListener)
                audioManager.addOnCommunicationDeviceChangedListener(
                    executor,
                    communicationDeviceListener as AudioManager.OnCommunicationDeviceChangedListener,
                )
            }.onFailure { Timber.tag(TAG).w(it, "Cannot listen to audio mode") }
        }
        evaluate()
    }

    fun stop() {
        if (!started) return
        started = false
        handler.removeCallbacks(releaseCheck)
        runCatching { context.unregisterReceiver(scoReceiver) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                audioManager.removeOnModeChangedListener(modeListener as AudioManager.OnModeChangedListener)
                audioManager.removeOnCommunicationDeviceChangedListener(
                    communicationDeviceListener as AudioManager.OnCommunicationDeviceChangedListener,
                )
            }
        }
    }

    private fun computeActive(): Boolean {
        if (scoConnected) return true
        val mode = audioManager.mode
        val communicationMode =
            mode == AudioManager.MODE_IN_COMMUNICATION ||
                mode == AudioManager.MODE_IN_CALL ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mode == AudioManager.MODE_CALL_SCREENING)
        if (!communicationMode) return false
        // Only the Bluetooth hands-free route degrades the music; a call on the phone speaker does not.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type?.let { it in HandsFreeTypes } == true
        } else {
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn
        }
    }

    private fun evaluate() {
        val active = computeActive()
        handler.removeCallbacks(releaseCheck)
        if (active && !isActive) {
            isActive = true
            activeSince = System.currentTimeMillis()
            Timber.tag(TAG).d("Bluetooth hands-free link started (mode=${audioManager.mode}, sco=$scoConnected)")
            onChange(true)
        } else if (!active && isActive) {
            handler.postDelayed(releaseCheck, RELEASE_DELAY_MS)
        }
    }

    private companion object {
        const val TAG = "BeatCallAudio"
        const val RELEASE_DELAY_MS = 1500L
        val HandsFreeTypes =
            buildSet {
                add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(AudioDeviceInfo.TYPE_BLE_HEADSET)
            }
    }
}
