package com.tomash.testapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun fail(message: String = "Test failed"): Nothing = throw AssertionError(message)

fun wrapWithLatch(latchFun: CountDownLatch.(UiDevice) -> Unit) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val latch = CountDownLatch(1)
    latch.latchFun(device)
    latch.awaitWithDefaultDelay()
}

fun CountDownLatch.awaitWithDefaultDelay() {
    val isFinishedSuccessfully = await(maxAwaitDelay, TimeUnit.MILLISECONDS)
    if (!isFinishedSuccessfully)
        fail("Timeout!")
}

val maxAwaitDelay = 10000L

val appContext: Context
    get() = ApplicationProvider.getApplicationContext<Context>()