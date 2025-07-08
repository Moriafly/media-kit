package com.moriafly.mediakit

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaKitInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var activityScenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        activityScenario.close()
    }

    @Test
    fun testMediaKit() {
        assertEquals(Lifecycle.State.RESUMED, activityScenario.state)

        activityScenario.moveToState(Lifecycle.State.CREATED)

        // 4. 验证 Activity 的状态确实已经变为 CREATED，即进入了后台
        assertEquals(Lifecycle.State.CREATED, activityScenario.state)

        activityScenario.onActivity {
            it.startService()
            it.startForegroundService()
        }
    }
}
