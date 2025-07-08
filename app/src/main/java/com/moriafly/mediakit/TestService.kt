package com.moriafly.mediakit

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class TestService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "TestService"
    }
}
