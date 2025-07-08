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

import android.app.ForegroundServiceStartNotAllowedException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Media Kit
 */
@UnstableMediaKitApi
object MediaKit {
    /**
     * 尝试启动服务
     *
     * “贪婪”式启动服务，不保证是启动普通服务或者前台服务，该功能的目的是尽量启动服务
     *
     * 它会优先使用 startService 启动服务，如果启动失败，则尝试使用 startForegroundService 继续启动服务
     *
     * 以此方法启动的服务需要以前台服务的逻辑进行处理，需要尽快调用 ServiceCompat.startForeground
     *
     * 此函数使用前提参考：https://developer.android.google.cn/develop/background-work/services/fgs/changes?hl=zh-cn
     *
     * @param context 环境
     * @param intent 启动服务意图
     *
     * @throws SecurityException If the caller does not have permission to access the service or the
     * service can not be found.
     * @throws ForegroundServiceStartNotAllowedException If the caller app's targeting API is
     * Build.VERSION_CODES.S or later, and the foreground service is restricted from start due to
     * background restriction.
     *
     * @return If the service is being started or is already running, the ComponentName of the
     * actual service that was started is returned; else if the service does not exist null is
     * returned.
     */
    fun tryStartService(context: Context, intent: Intent): ComponentName? {
        // 如果是 Android 8.0 以下，则直接启动后台服务
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return context.startService(intent)
        }

        // Android 8.0+ 不允许后台应用创建后台服务

        // 官方推荐：如果用户会注意到该服务，请将其设为前台服务，例如播放音频的服务应始终是前台服务
        // 使用 startForegroundService() 方法（而非 startService()）创建服务
        // 但是前台服务有些地方存在问题需要避免，所以尝试以下方法

        // 如果以 Android 8.0 为目标平台的应用尝试在不允许其创建后台服务的情况下使用 startService() 方法，
        // 则该方法将引发一个 IllegalStateException
        // 在 Android 8.0 以上需要在被系统视为前台应用的情况下才能通过 startService() 启动服务
        // 此出前台的判断比较复杂，参考：https://developer.android.google.cn/about/versions/oreo/background?hl=zh-cn#services
        // 既然会引发 IllegalStateException，这直接尝试使用 startService() 启动服务，如果引发 Exception，
        // 则说明当前应用不是前台应用，从而进行回退逻辑
        try {
            return context.startService(intent)
        } catch (_: IllegalStateException) {
            // 启动失败，说明当前应用不是前台应用

            // 启动一个服务，并向系统承诺这个服务会很快转为前台服务
            // 系统会为该服务创建一个进程（如果尚不存在）并启动它
            // 系统会给予这个服务一个短暂的时间窗口：
            // Android 8.0+ 限制时间 5 秒
            // Android 12.0+ 限制时间 10 秒
            // 在这个时间窗口内，服务必须调用 startForeground(int, Notification) 方法，将自己提升为前台服务
            // 如果服务没有在规定时间内调用 startForeground，系统会认为该服务行为异常，并强制终止应用的进程（ANR）
            // 异常：
            // Android 8.0+ RemoteServiceException
            // Android 12+ ForegroundServiceDidNotStartInTimeException

            // 对于 Android 8.0+ 的应用，即便在后台也可以通过 startForegroundService 启动前台服务
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return context.startForegroundService(intent)
            }

            // 但是 Android 12+ 不允许后台应用启动前台服务
            // 虽然这个地方已经确认当前应用不是前台应用，但是存在豁免情况，所以继续尝试
            // 参考：https://developer.android.google.cn/develop/background-work/services/fgs/restrictions-bg-start?hl=zh-cn#background-start-restriction-exemptions
            return context.startForegroundService(intent)
        }
    }
}
