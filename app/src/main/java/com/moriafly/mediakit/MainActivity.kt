package com.moriafly.mediakit

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.moriafly.mediakit.core.MediaKit
import com.moriafly.mediakit.core.UnstableMediaKitApi

class MainActivity : Activity() {
    @OptIn(UnstableMediaKitApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    @OptIn(UnstableMediaKitApi::class)
    fun tryStartService() {
        val intent = Intent(this, TestService::class.java)
        val componentName = MediaKit.tryStartService(this, intent)
        Log.d(TAG, "componentName: $componentName")
    }

    fun startService() {
        val intent = Intent(this, TestService::class.java)
        val componentName = this.startService(intent)
        Log.d(TAG, "componentName: $componentName")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startForegroundService() {
        val intent = Intent(this, TestService::class.java)
        val componentName = this.startForegroundService(intent)
        Log.d(TAG, "componentName: $componentName")
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
