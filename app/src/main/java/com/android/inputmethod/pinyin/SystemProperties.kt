package com.android.inputmethod.pinyin

object SystemProperties {
    /**
     * Get the value for the given key.
     *
     * @return an empty string if the key isn't found
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    @Throws(IllegalArgumentException::class)
    fun get(key: String): String? {
        var ret = ""
        try {
            val SystemProperties = Class.forName("android.os.SystemProperties")

            //Parameters Types
            val paramTypes = arrayOf<Class<*>>(
                String::class.java
            )
            val get = SystemProperties.getMethod("get", *paramTypes)

            //Parameters
            val params = arrayOf<Any>(key)
            ret = get.invoke(SystemProperties, *params) as String
        } catch (iAE: IllegalArgumentException) {
            throw iAE
        } catch (e: Exception) {
            ret = ""
            //TODO
        }

        return ret
    }

    /**
     * Get the value for the given key.
     *
     * @return if the key isn't found, return def if it isn't null, or an empty
     * string otherwise
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    @Throws(IllegalArgumentException::class)
    fun get(key: String?, def: String?): String? {
        var ret = def

        try {
            val SystemProperties = Class.forName("android.os.SystemProperties")

            //Parameters Types
            val paramTypes = arrayOf<Class<*>>(
                String::class.java,
                String::class.java
            )
            val get = SystemProperties.getMethod("get", *paramTypes)

            //Parameters
            val params = arrayOf<Any?>(key, def)
            ret = get.invoke(SystemProperties, *params) as String
        } catch (iAE: IllegalArgumentException) {
            throw iAE
        } catch (e: Exception) {
            ret = def
            //TODO
        }

        return ret
    }

    /**
     * Get the value for the given key, and return as an integer.
     *
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as an integer, or def if the key isn't found or
     * cannot be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    @Throws(IllegalArgumentException::class)
    fun getInt(key: String, def: Int): Int? {
        var ret = def

        try {
            val SystemProperties = Class.forName("android.os.SystemProperties")

            //Parameters Types
            val paramTypes = arrayOf<Class<*>?>(
                String::class.java,
                Int::class.javaPrimitiveType
            )
            val getInt = SystemProperties.getMethod("getInt", *paramTypes)

            //Parameters
            val params = arrayOf<Any>(key, def)
            ret = getInt.invoke(SystemProperties, *params) as Int
        } catch (IAE: IllegalArgumentException) {
            throw IAE
        } catch (e: Exception) {
            ret = def
            //TODO
        }

        return ret
    }

    /**
     * Get the value for the given key, and return as a long.
     *
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a long, or def if the key isn't found or cannot
     * be parsed
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    @Throws(IllegalArgumentException::class)
    fun getLong(key: String, def: Long): Long? {
        var ret = def

        try {
            val SystemProperties = Class.forName("android.os.SystemProperties")
            //Parameters Types
            val paramTypes = arrayOf<Class<*>?>(
                String::class.java,
                Long::class.javaPrimitiveType
            )
            val getLong = SystemProperties.getMethod("getLong", *paramTypes)

            //Parameters
            val params = arrayOf<Any>(key, def)
            ret = getLong.invoke(SystemProperties, *params) as Long
        } catch (iAE: IllegalArgumentException) {
            throw iAE
        } catch (e: Exception) {
            ret = def
            //TODO
        }

        return ret
    }

    /**
     * Get the value for the given key, returned as a boolean. Values 'n', 'no',
     * '0', 'false' or 'off' are considered false. Values 'y', 'yes', '1', 'true'
     * or 'on' are considered true. (case insensitive). If the key does not exist,
     * or has any other value, then the default result is returned.
     *
     * @param key the key to lookup
     * @param def a default value to return
     * @return the key parsed as a boolean, or def if the key isn't found or is
     * not able to be parsed as a boolean.
     * @throws IllegalArgumentException if the key exceeds 32 characters
     */
    @Throws(IllegalArgumentException::class)
    fun getBoolean(key: String, def: Boolean): Boolean? {
        var ret = def
        try {
            val SystemProperties = Class.forName("android.os.SystemProperties")

            //Parameters Types
            val paramTypes = arrayOf<Class<*>?>(
                String::class.java,
                Boolean::class.javaPrimitiveType
            )
            val getBoolean = SystemProperties.getMethod("getBoolean", *paramTypes)

            //Parameters
            val params = arrayOf<Any>(key, def)
            ret = getBoolean.invoke(SystemProperties, *params) as Boolean
        } catch (iAE: IllegalArgumentException) {
            throw iAE
        } catch (e: Exception) {
            ret = def
            //TODO
        }

        return ret
    }

    /**
     * Set the value for the given key.
     *
     * @throws IllegalArgumentException if the key exceeds 32 characters
     * @throws IllegalArgumentException if the value exceeds 92 characters
     */
    @Throws(IllegalArgumentException::class)
    fun set(key: String, `val`: String) {
        try {
            val SystemProperties = Class.forName("android.os.SystemProperties")

            //Parameters Types
            val paramTypes = arrayOf<Class<*>>(
                String::class.java,
                String::class.java
            )
            val set = SystemProperties.getMethod("set", *paramTypes)

            //Parameters
            val params = arrayOf<Any>(key, `val`)
            set.invoke(SystemProperties, *params)
        } catch (iAE: IllegalArgumentException) {
            throw iAE
        } catch (e: Exception) {
            //TODO
        }
    }
}
