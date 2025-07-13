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

package com.moriafly.mediakit.core.android

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import com.moriafly.mediakit.core.UnstableMediaKitApi
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Field

/**
 * # AudioSystem
 *
 * 用以获取 Android 系统的一些音频配置属性，使用了 ART 内部函数
 */
@UnstableMediaKitApi
object AudioSystem {
    private const val ANDROID_MEDIA_AUDIO_SYSTEM_CLASS_NAME = "android.media.AudioSystem"

    @SuppressLint("PrivateApi")
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSampleRateHzMax(): Int? {
        val allStaticFields = HiddenApiBypass
            .getStaticFields(Class.forName(ANDROID_MEDIA_AUDIO_SYSTEM_CLASS_NAME))
        val sampleRateHzMax = allStaticFields
            .filterIsInstance<Field>()
            .find { it.name == "SAMPLE_RATE_HZ_MAX" }?.get(null)
        return sampleRateHzMax as Int?
    }

    @SuppressLint("PrivateApi")
    @RequiresApi(Build.VERSION_CODES.S)
    fun getSampleRateHzMin(): Int? {
        val allStaticFields = HiddenApiBypass
            .getStaticFields(Class.forName(ANDROID_MEDIA_AUDIO_SYSTEM_CLASS_NAME))
        val sampleRateHzMax = allStaticFields
            .filterIsInstance<Field>()
            .find { it.name == "SAMPLE_RATE_HZ_MIN" }?.get(null)
        return sampleRateHzMax as Int?
    }

    /**
     * Parameters 在 Android 源码的 includes/audio.h，如 A2dpSuspended=1
     */
    @SuppressLint("PrivateApi")
    @RequiresApi(Build.VERSION_CODES.P)
    fun setParameters(keyValuePairs: String) {
        val clazz = Class.forName(ANDROID_MEDIA_AUDIO_SYSTEM_CLASS_NAME)
        HiddenApiBypass.invoke(clazz, null, "setParameters", keyValuePairs)
    }

    @SuppressLint("PrivateApi")
    @RequiresApi(Build.VERSION_CODES.P)
    fun getParameters(keys: String): String {
        val clazz = Class.forName(ANDROID_MEDIA_AUDIO_SYSTEM_CLASS_NAME)
        return HiddenApiBypass.invoke(clazz, null, "getParameters", keys) as String
    }
}
