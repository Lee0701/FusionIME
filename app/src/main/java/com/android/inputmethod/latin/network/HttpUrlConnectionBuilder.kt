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

import android.text.TextUtils
import com.android.inputmethod.annotations.UsedForTesting
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Builder for [HttpURLConnection]s.
 *
 * TODO: Remove @UsedForTesting after this is actually used.
 */
@UsedForTesting
class HttpUrlConnectionBuilder {
    private val mHeaderMap = HashMap<String, String>()

    private var mUrl: URL? = null
    private var mConnectTimeoutMillis = DEFAULT_TIMEOUT_MILLIS
    private var mReadTimeoutMillis = DEFAULT_TIMEOUT_MILLIS
    private var mContentLength = -1
    private var mUseCache = false
    private var mMode = 0

    /**
     * Sets the URL that'll be used for the request.
     * This *must* be set before calling [.build]
     *
     * TODO: Remove @UsedForTesting after this method is actually used.
     */
    @UsedForTesting
    @Throws(MalformedURLException::class)
    fun setUrl(url: String?): HttpUrlConnectionBuilder {
        require(!TextUtils.isEmpty(url)) { "URL must not be empty" }
        mUrl = URL(url)
        return this
    }

    /**
     * Sets the connect timeout. Defaults to {@value #DEFAULT_TIMEOUT_MILLIS} milliseconds.
     *
     * TODO: Remove @UsedForTesting after this method is actually used.
     */
    @UsedForTesting
    fun setConnectTimeout(timeoutMillis: Int): HttpUrlConnectionBuilder {
        require(timeoutMillis >= 0) {
            ("connect-timeout must be >= 0, but was "
                    + timeoutMillis)
        }
        mConnectTimeoutMillis = timeoutMillis
        return this
    }

    /**
     * Sets the read timeout. Defaults to {@value #DEFAULT_TIMEOUT_MILLIS} milliseconds.
     *
     * TODO: Remove @UsedForTesting after this method is actually used.
     */
    @UsedForTesting
    fun setReadTimeout(timeoutMillis: Int): HttpUrlConnectionBuilder {
        require(timeoutMillis >= 0) {
            ("read-timeout must be >= 0, but was "
                    + timeoutMillis)
        }
        mReadTimeoutMillis = timeoutMillis
        return this
    }

    /**
     * Adds an entry to the request header.
     *
     * TODO: Remove @UsedForTesting after this method is actually used.
     */
    @UsedForTesting
    fun addHeader(key: String, value: String): HttpUrlConnectionBuilder {
        mHeaderMap[key] = value
        return this
    }

    /**
     * Sets an authentication token.
     *
     * TODO: Remove @UsedForTesting after this method is actually used.
     */
    @UsedForTesting
    fun setAuthToken(value: String): HttpUrlConnectionBuilder {
        mHeaderMap[HTTP_HEADER_AUTHORIZATION] = value
        return this
    }

    /**
     * Sets the request to be executed such that the input is not buffered.
     * This may be set when the request size is known beforehand.
     *
     * TODO: Remove @UsedForTesting after this method is actually used.
     */
    @UsedForTesting
    fun setFixedLengthForStreaming(length: Int): HttpUrlConnectionBuilder {
        mContentLength = length
        return this
    }

    /**
     * Indicates if the request can use cached responses or not.
     *
     * TODO: Remove @UsedForTesting after this method is actually used.
     */
    @UsedForTesting
    fun setUseCache(useCache: Boolean): HttpUrlConnectionBuilder {
        mUseCache = useCache
        return this
    }

    /**
     * The request mode.
     * Sets the request mode to be one of: upload-only, download-only or bidirectional.
     *
     * @see .MODE_UPLOAD_ONLY
     *
     * @see .MODE_DOWNLOAD_ONLY
     *
     * @see .MODE_BI_DIRECTIONAL
     *
     * TODO: Remove @UsedForTesting after this method is actually used
     */
    @UsedForTesting
    fun setMode(mode: Int): HttpUrlConnectionBuilder {
        require(!(mode != MODE_UPLOAD_ONLY && mode != MODE_DOWNLOAD_ONLY && mode != MODE_BI_DIRECTIONAL)) { "Invalid mode specified:$mode" }
        mMode = mode
        return this
    }

    /**
     * Builds the [HttpURLConnection] instance that can be used to execute the request.
     *
     * TODO: Remove @UsedForTesting after this method is actually used.
     */
    @UsedForTesting
    @Throws(IOException::class)
    fun build(): HttpURLConnection {
        requireNotNull(mUrl) { "A URL must be specified!" }
        val connection = mUrl!!.openConnection() as HttpURLConnection
        connection.connectTimeout = mConnectTimeoutMillis
        connection.readTimeout = mReadTimeoutMillis
        connection.useCaches = mUseCache
        when (mMode) {
            MODE_UPLOAD_ONLY -> {
                connection.doInput = true
                connection.doOutput = false
            }

            MODE_DOWNLOAD_ONLY -> {
                connection.doInput = false
                connection.doOutput = true
            }

            MODE_BI_DIRECTIONAL -> {
                connection.doInput = true
                connection.doOutput = true
            }
        }
        for ((key, value) in mHeaderMap) {
            connection.addRequestProperty(key, value)
        }
        if (mContentLength >= 0) {
            connection.setFixedLengthStreamingMode(mContentLength)
        }
        return connection
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MILLIS = 5 * 1000

        /**
         * Request header key for authentication.
         */
        const val HTTP_HEADER_AUTHORIZATION: String = "Authorization"

        /**
         * Request header key for cache control.
         */
        const val KEY_CACHE_CONTROL: String = "Cache-Control"

        /**
         * Request header value for cache control indicating no caching.
         * @see .KEY_CACHE_CONTROL
         */
        const val VALUE_NO_CACHE: String = "no-cache"

        /**
         * Indicates that the request is unidirectional - upload-only.
         * TODO: Remove @UsedForTesting after this is actually used.
         */
        @UsedForTesting
        const val MODE_UPLOAD_ONLY: Int = 1

        /**
         * Indicates that the request is unidirectional - download only.
         * TODO: Remove @UsedForTesting after this is actually used.
         */
        @UsedForTesting
        const val MODE_DOWNLOAD_ONLY: Int = 2

        /**
         * Indicates that the request is bi-directional.
         * TODO: Remove @UsedForTesting after this is actually used.
         */
        @UsedForTesting
        const val MODE_BI_DIRECTIONAL: Int = 3
    }
}