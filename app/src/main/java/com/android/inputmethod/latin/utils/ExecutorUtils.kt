/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.inputmethod.latin.utils

import android.util.Log
import com.android.inputmethod.annotations.UsedForTesting
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * Utilities to manage executors.
 */
object ExecutorUtils {
    private const val TAG = "ExecutorUtils"

    const val KEYBOARD: String = "Keyboard"
    const val SPELLING: String = "Spelling"

    private var sKeyboardExecutorService = newExecutorService(KEYBOARD)
    private var sSpellingExecutorService = newExecutorService(SPELLING)

    private fun newExecutorService(name: String): ScheduledExecutorService {
        return Executors.newSingleThreadScheduledExecutor(ExecutorFactory(name))
    }

    @UsedForTesting
    private var sExecutorServiceForTests: ScheduledExecutorService? = null

    @UsedForTesting
    fun setExecutorServiceForTests(
        executorServiceForTests: ScheduledExecutorService?
    ) {
        sExecutorServiceForTests = executorServiceForTests
    }

    //
    // Public methods used to schedule a runnable for execution.
    //
    /**
     * @param name Executor's name.
     * @return scheduled executor service used to run background tasks
     */
    fun getBackgroundExecutor(name: String): ScheduledExecutorService? {
        if (sExecutorServiceForTests != null) {
            return sExecutorServiceForTests
        }
        return when (name) {
            KEYBOARD -> sKeyboardExecutorService
            SPELLING -> sSpellingExecutorService
            else -> throw IllegalArgumentException("Invalid executor: $name")
        }
    }

    fun killTasks(name: String) {
        val executorService = getBackgroundExecutor(name)
        executorService!!.shutdownNow()
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Log.wtf(TAG, "Failed to shut down: $name")
        }
        if (executorService === sExecutorServiceForTests) {
            // Don't do anything to the test service.
            return
        }
        when (name) {
            KEYBOARD -> sKeyboardExecutorService = newExecutorService(KEYBOARD)
            SPELLING -> sSpellingExecutorService = newExecutorService(SPELLING)
            else -> throw IllegalArgumentException("Invalid executor: $name")
        }
    }

    @UsedForTesting
    fun chain(vararg runnables: Runnable): Runnable {
        return RunnableChain(*runnables)
    }

    private class ExecutorFactory(name: String) : ThreadFactory {
        private val mName = name

        override fun newThread(runnable: Runnable): Thread {
            val thread = Thread(runnable, TAG)
            thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, ex ->
                Log.w(
                    mName + "-" + runnable.javaClass.simpleName,
                    ex
                )
            }
            return thread
        }
    }

    @UsedForTesting
    class RunnableChain(vararg runnables: Runnable) : Runnable {
        @get:UsedForTesting
        var runnables: List<Runnable>

        init {
            require(runnables.isNotEmpty()) { "Attempting to construct an empty chain" }
            this.runnables = runnables.toList()
        }

        override fun run() {
            for (runnable in runnables) {
                if (Thread.interrupted()) {
                    return
                }
                runnable.run()
            }
        }
    }
}
