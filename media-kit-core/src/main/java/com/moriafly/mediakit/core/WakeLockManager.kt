@file:Suppress("unused")

package com.moriafly.mediakit.core

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.annotation.MainThread

/**
 * # 唤醒锁管理
 *
 * Also see Media3 WakeLockManager.
 */
class WakeLockManager(
    context: Context
) {
    private val applicationContext: Context = context.applicationContext

    private var wakeLock: WakeLock? = null
    private var enabled = false
    private var stayAwake = false

    /**
     * 初始化和销毁时使用的“主开关”，服务启动时候初始化并设置为 true，销毁的时候设置为 false
     *
     * Sets whether to enable the acquiring and releasing of the [WakeLock].
     *
     * By default, wake lock handling is not enabled. Enabling this will acquire the wake lock if
     * necessary. Disabling this will release the wake lock if it is held.
     *
     * Enabling [WakeLock] requires the [android.Manifest.permission.WAKE_LOCK].
     *
     * @param enabled True if the player should handle a [WakeLock], false otherwise.
     */
    @MainThread
    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            if (wakeLock == null) {
                val powerManager =
                    applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager?
                if (powerManager == null) {
                    Log.w(TAG, "PowerManager is null, therefore not creating the WakeLock")
                    return
                }
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                wakeLock!!.setReferenceCounted(false)
            }
        }

        this.enabled = enabled
        updateWakeLock()
    }

    /**
     * 播放和暂停等操作过程中使用的“操作开关”，一般仅需要在 Player 的 onIsPlaying 回调中处理
     *
     * Sets whether to acquire or release the [WakeLock].
     *
     * Please note this method requires wake lock handling to be enabled through setEnabled(boolean
     * enable) to actually have an impact on the [WakeLock].
     *
     * @param stayAwake True if the player should acquire the [WakeLock]. False if the player
     * should release.
     */
    @MainThread
    fun setStayAwake(stayAwake: Boolean) {
        this.stayAwake = stayAwake
        updateWakeLock()
    }

    // WakelockTimeout suppressed because the time the wake lock is needed for is unknown (could be
    // listening to radio with screen off for multiple hours), therefore we can not determine a
    // reasonable timeout that would not affect the user.
    @SuppressLint("WakelockTimeout", "Wakelock")
    private fun updateWakeLock() {
        wakeLock?.let {
            if (enabled && stayAwake) {
                if (!it.isHeld) {
                    it.acquire()
                }
            } else {
                if (it.isHeld) {
                    it.release()
                }
            }
        }
    }

    companion object {
        private const val TAG = "WakeLockManager"
        private const val WAKE_LOCK_TAG = "MediaKit:WakeLockManager"
    }
}
