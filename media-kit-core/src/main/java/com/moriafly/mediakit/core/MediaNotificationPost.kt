/**
 * Media Kit
 * Copyright (C) 2025 Moriafly
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

@file:Suppress("unused")

package com.moriafly.mediakit.core

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.MainThread
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat

/**
 * # 媒体通知推送
 *
 * **所有方法必须在主线程调用**
 *
 * 自带前台服务管理，参考 Media3 Notification
 *
 * ## UI（Activity）和 Service 应该通过 bindService 通信
 *
 * 这样会更稳定且避免了最开始通过 startForegroundService 启动必须显示通知的情况，
 * 更多问题参考 [MediaKit.tryStartService] 的说明。同时参考 Google Media 注释：
 * - 防止服务所有者的 `stopSelf()` 销毁服务：
 * 当使用 `startForegroundService()` 时，如果服务调用 `stopSelf()`，即使另一个线程中的 `onConnect()` 仍在运行，
 * 也会立即在主线程上触发 `onDestroy()` 调用。这可能导致服务在不恰当的时机被销毁。
 * 使用 `bindService()` 可以避免这种情况，因为服务的生命周期会更稳定，不会因为 `stopSelf()` 而立即终止。
 * - 最小化开发者需要处理的 API：
 * 使用 `bindService()` 时，开发者只需要关注 `Service.onBind()` 方法。
 * 而如果使用 `startForegroundService()`，开发者还需要处理 `Service.onStartCommand()`，增加了复杂性。
 * - 未来支持无 UI 播放：
 * 如果服务需要持续运行，它必须是前台服务或绑定服务。
 * 对于系统应用，可能会有无 UI 播放的需求，使用 `bindService()` 更适合这种场景，因为它不需要像前台服务那样显示通知。
 *
 * @param service 关联的服务
 * @param notificationId The identifier for this notification as per
 * NotificationManager.notify(int, Notification); must not be 0.
 */
@UnstableMediaKitApi
class MediaNotificationPost(
    val service: Service,
    val notificationId: Int
) {
    private val notificationManagerCompat = NotificationManagerCompat.from(service)

    /**
     * 服务是否在前台
     *
     * 仅表示内部逻辑，它不能完全翻译服务是否在前台的完全真实状态，详见 [stopInForeground] 注释
     */
    var isInForeground: Boolean = false
        private set

    /**
     * 在前台服务启动失败时回调（Android 12+），可以不处理
     */
    var onForegroundServiceStartNotAllowedException: () -> Unit = {}

    /**
     * 推送通知
     *
     * @param notification 通知
     * @param strategy [Strategy]
     */
    @MainThread
    fun postNotification(
        notification: Notification,
        strategy: Strategy
    ) {
        fun postNotificationNone() {
            // 对于媒体通知不需要权限
            @SuppressLint("MissingPermission")
            notificationManagerCompat.notify(notificationId, notification)
        }

        when (strategy) {
            Strategy.InForeground -> {
                if (!isInForeground) {
                    val intent = Intent(service, service::class.java)
                    try {
                        // 向系统发送合约
                        MediaKit.startForegroundService(service, intent)
                        // 此时候服务本身就在运行，直接 startForeground 完成合约
                        ServiceCompat.startForeground(
                            service,
                            notificationId,
                            notification,
                            @SuppressLint("InlinedApi")
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                        // 完成前台提升
                        isInForeground = true
                    } catch (e: IllegalStateException) {
                        // ForegroundServiceStartNotAllowedException 继承自 IllegalStateException
                        // 提升前台失败
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            e is ForegroundServiceStartNotAllowedException
                        ) {
                            onForegroundServiceStartNotAllowedException()

                            // 继续发送通知，确保通知发送
                            postNotificationNone()
                        } else {
                            // 抛出异常，此需要开发者处理
                            throw e
                        }
                    }
                } else {
                    postNotificationNone()
                }
            }

            Strategy.StopForeground -> {
                // 先更新通知，确保内容显示正确
                postNotificationNone()
                // 如果当前是前台服务，则将其降级
                if (isInForeground) {
                    stopInForeground(false)
                }
            }

            Strategy.None -> postNotificationNone()
        }
    }

    /**
     * 移除通知，如果服务当前是前台服务，此方法会将其停止并移除通知
     */
    @MainThread
    fun removeNotification() {
        if (isInForeground) {
            stopInForeground(true)
        } else {
            notificationManagerCompat.cancel(notificationId)
        }
    }

    /**
     * 停止前台状态，必须在 [isInForeground] 为 true 时调用
     *
     * @param removeNotification 是否停止前台状态的同时也移除当前通知
     */
    private fun stopInForeground(removeNotification: Boolean) {
        // ServiceCompat.stopForeground 是一个同步调用，它会向 ActivityManagerService 发送一个请求
        // 然而，系统处理这个请求是异步的，在极端的时机（例如，系统负载极高），服务的实际前台状态可能不会立即改变
        // 当前实现是标准且可接受的, 这更多是一个理论上的考量点而不是一个实际的 Bug
        // Android Media 3 的 PlayerNotificationManager 采用了相同的逻辑
        ServiceCompat.stopForeground(
            service,
            if (removeNotification) {
                ServiceCompat.STOP_FOREGROUND_REMOVE
            } else {
                ServiceCompat.STOP_FOREGROUND_DETACH
            }
        )
        isInForeground = false
    }

    /**
     * 更新通知策略
     */
    enum class Strategy {
        /**
         * 要求在前台服务的情况下更新，如果当前不是前台服务则需要提升
         */
        InForeground,

        /**
         * 若是前台服务的情况下，要求停止前台服务
         */
        StopForeground,

        /**
         * 无所谓
         */
        None
    }
}
