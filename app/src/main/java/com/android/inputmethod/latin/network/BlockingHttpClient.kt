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
package com.android.inputmethod.latin.network

import android.util.Log
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection

/**
 * A client for executing HTTP requests synchronously.
 * This must never be called from the main thread.
 */
class BlockingHttpClient(connection: HttpURLConnection) {
    private val mConnection = connection

    /**
     * Interface that handles processing the response for a request.
     */
    interface ResponseProcessor<T> {
        /**
         * Called when the HTTP request finishes successfully.
         * The [InputStream] is closed by the client after the method finishes,
         * so any processing must be done in this method itself.
         *
         * @param response An input stream that can be used to read the HTTP response.
         */
        @Throws(IOException::class)
        fun onSuccess(response: InputStream?): T
    }

    /**
     * Executes the request on the underlying [HttpURLConnection].
     *
     * @param request The request payload, if any, or null.
     * @param responseProcessor A processor for the HTTP response.
     */
    @Throws(IOException::class, AuthException::class, HttpException::class)
    fun <T> execute(request: ByteArray?, responseProcessor: ResponseProcessor<T>): T {
        if (DEBUG) {
            Log.d(TAG, "execute: " + mConnection.url)
        }
        try {
            if (request != null) {
                if (DEBUG) {
                    Log.d(TAG, "request size: " + request.size)
                }
                val out: OutputStream = BufferedOutputStream(mConnection.outputStream)
                out.write(request)
                out.flush()
                out.close()
            }

            val responseCode = mConnection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(
                    TAG, ("Response error: " + responseCode + ", Message: "
                            + mConnection.responseMessage)
                )
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    throw AuthException(mConnection.responseMessage)
                }
                throw HttpException(responseCode)
            }
            if (DEBUG) {
                Log.d(TAG, "request executed successfully")
            }
            return responseProcessor.onSuccess(mConnection.inputStream)
        } finally {
            mConnection.disconnect()
        }
    }

    companion object {
        private const val DEBUG = false
        private val TAG: String = BlockingHttpClient::class.java.simpleName
    }
}
