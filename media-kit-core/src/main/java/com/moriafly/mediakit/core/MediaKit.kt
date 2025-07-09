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
     * 启动前台服务
     */
    fun startForegroundService(
        context: Context,
        intent: Intent
    ): ComponentName? =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            context.startService(intent)
        } else {
            context.startForegroundService(intent)
        }

    /**
     * # 尝试启动服务
     *
     * **贪婪**式启动服务，不保证是启动普通服务或者前台服务，该功能的目的是尽量启动服务。
     * 它会优先使用 `startService()` 启动服务，如果启动失败，则尝试使用 `startForegroundService()` 继续启动服务。
     * 以此方法启动的服务需要以前台服务的逻辑进行处理，需要尽快调用 ServiceCompat.startForeground。
     *
     * 此函数使用前提参考：https://developer.android.google.cn/develop/background-work/services/fgs/changes?hl=zh-cn
     *
     * ## 注意 1
     *
     * 如果以前台服务启动，那么 `startForeground()` 是强制性的，即便在启动后立刻触发销毁服务，
     * 类似与 Android 系统的**“严格合约” (Strict Contract)**——向系统做出了一个绝对的、不可撤销的承诺：
     * “我即将启动一个 Service，并且这个 Service 必须、一定会调用 `startForeground()` 方法来显示一个通知，从而转变为前台服务”
     * 这个合约只有一种方式可以“履行”：成功调用 `startForeground()`，不存在“中途取消”或“提前终止”的选项。
     * 此情况在 Android 文档中未提及，issue 参考：
     * - https://issuetracker.google.com/issues/64142050
     * - https://issuetracker.google.com/issues/76112072
     * - Google 官方人员说明 Won't Fix (Intended Behavior)：https://issuetracker.google.com/issues/76112072#comment36
     *
     * ## 注意 2
     *
     * Service 的 `onCreate()` 方法只会在服务首次创建时调用一次，
     * `onStartCommand()` 方法会在每次客户端通过 `startService()` 或 `startForegroundService()` 请求启动服务时被调用，
     * `startForegroundService()` 的承诺和此方法本身绑定，无论服务是否已经运行，
     * 所以需要在 `onStartCommand()` 中调用 `startForeground()` 方法来提升到前台。
     *
     * ## 注意 3
     *
     * 警惕耗时的 `Application.onCreate()`：这是一个非常隐蔽的崩溃陷阱！当系统因内存不足等原因杀死应用进程后，
     * 若服务当前是**前台服务**的同时被设定为自动重启（`START_STICKY`），系统会先执行 `Application.onCreate()`，
     * 然后才执行服务的生命周期方法。如果 `Application.onCreate()` 耗时过长（例如超过 5 秒），它将耗尽前台服务启动的全部超时时间，
     * 导致服务还未执行到 `onStartCommand()` 就已超时，从而引发崩溃。
     * **必须确保 Application 的 onCreate 方法是轻量且迅速的！**
     *
     * ## 注意 4
     *
     * 在设备启动时 (`BOOT_COMPLETED`) 启动的风险：在接收到 `BOOT_COMPLETED` 广播后立即启动前台服务风险极高。
     * 设备刚启动的前几十秒是系统资源竞争最激烈的时候，即使你的代码本身很快，你的服务进程也可能因为 CPU 调度繁忙而被阻塞，
     * 无法及时执行 `onStartCommand`，从而导致超时。这个问题在某些厂商的设备上（如三星）尤为常见。
     *
     * ## 其他参考
     *
     * - 服务恢复时存在的问题：https://issuetracker.google.com/issues/76112072#comment122
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
     * @return 如果服务正在启动或已在运行，则返回实际启动服务的 [ComponentName]；否则若服务不存在，则返回 null。
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
            // 但是 Android 12+ 不允许后台应用启动前台服务，虽然这个地方已经确认当前应用不是前台应用，但是存在豁免情况，所以继续尝试
            // 参考：https://developer.android.google.cn/develop/background-work/services/fgs/restrictions-bg-start?hl=zh-cn#background-start-restriction-exemptions
            return context.startForegroundService(intent)
        }
    }
}
