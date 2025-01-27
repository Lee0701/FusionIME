/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.inputmethod.keyboard.internal

import android.util.Log

class PointerTrackerQueue {
    interface Element {
        fun isModifier(): Boolean
        fun isInDraggingFinger(): Boolean
        fun onPhantomUpEvent(eventTime: Long)
        fun cancelTrackingForAction()
    }

    // Note: {@link #mExpandableArrayOfActivePointers} and {@link #mArraySize} are synchronized by
    // {@link #mExpandableArrayOfActivePointers}
    private val mExpandableArrayOfActivePointers: ArrayList<Element> = ArrayList(
        INITIAL_CAPACITY
    )
    private var mArraySize: Int = 0

    fun size(): Int {
        synchronized(mExpandableArrayOfActivePointers) {
            return mArraySize
        }
    }

    fun add(pointer: Element) {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) {
                Log.d(TAG, "add: " + pointer + " " + this)
            }
            val expandableArray: ArrayList<Element> = mExpandableArrayOfActivePointers
            val arraySize: Int = mArraySize
            if (arraySize < expandableArray.size) {
                expandableArray.set(arraySize, pointer)
            } else {
                expandableArray.add(pointer)
            }
            mArraySize = arraySize + 1
        }
    }

    fun remove(pointer: Element) {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) {
                Log.d(TAG, "remove: " + pointer + " " + this)
            }
            val expandableArray: ArrayList<Element> = mExpandableArrayOfActivePointers
            val arraySize: Int = mArraySize
            var newIndex: Int = 0
            for (index in 0 until arraySize) {
                val element: Element = expandableArray.get(index)
                if (element === pointer) {
                    if (newIndex != index) {
                        Log.w(TAG, "Found duplicated element in remove: " + pointer)
                    }
                    continue  // Remove this element from the expandableArray.
                }
                if (newIndex != index) {
                    // Shift this element toward the beginning of the expandableArray.
                    expandableArray.set(newIndex, element)
                }
                newIndex++
            }
            mArraySize = newIndex
        }
    }

    fun getOldestElement(): Element? {
        synchronized(mExpandableArrayOfActivePointers) {
            return if ((mArraySize == 0)) null else mExpandableArrayOfActivePointers.get(0)
        }
    }

    fun releaseAllPointersOlderThan(pointer: Element, eventTime: Long) {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) {
                Log.d(TAG, "releaseAllPointerOlderThan: " + pointer + " " + this)
            }
            val expandableArray: ArrayList<Element> = mExpandableArrayOfActivePointers
            val arraySize: Int = mArraySize
            var newIndex: Int
            var index: Int
            newIndex = 0.also { index = it }
            while (index < arraySize) {
                val element: Element = expandableArray.get(index)
                if (element === pointer) {
                    break // Stop releasing elements.
                }
                if (!element.isModifier()) {
                    element.onPhantomUpEvent(eventTime)
                    index++
                    continue  // Remove this element from the expandableArray.
                }
                if (newIndex != index) {
                    // Shift this element toward the beginning of the expandableArray.
                    expandableArray.set(newIndex, element)
                }
                newIndex++
                index++
            }
            // Shift rest of the expandableArray.
            var count: Int = 0
            while (index < arraySize) {
                val element: Element = expandableArray.get(index)
                if (element === pointer) {
                    count++
                    if (count > 1) {
                        Log.w(
                            TAG, "Found duplicated element in releaseAllPointersOlderThan: "
                                    + pointer
                        )
                    }
                }
                if (newIndex != index) {
                    // Shift this element toward the beginning of the expandableArray.
                    expandableArray.set(newIndex, expandableArray.get(index))
                }
                newIndex++
                index++
            }
            mArraySize = newIndex
        }
    }

    fun releaseAllPointers(eventTime: Long) {
        releaseAllPointersExcept(null, eventTime)
    }

    fun releaseAllPointersExcept(pointer: Element?, eventTime: Long) {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) {
                if (pointer == null) {
                    Log.d(TAG, "releaseAllPointers: " + this)
                } else {
                    Log.d(TAG, "releaseAllPointerExcept: " + pointer + " " + this)
                }
            }
            val expandableArray: ArrayList<Element> = mExpandableArrayOfActivePointers
            val arraySize: Int = mArraySize
            var newIndex: Int = 0
            var count: Int = 0
            for (index in 0 until arraySize) {
                val element: Element = expandableArray.get(index)
                if (element === pointer) {
                    count++
                    if (count > 1) {
                        Log.w(
                            TAG, "Found duplicated element in releaseAllPointersExcept: "
                                    + pointer
                        )
                    }
                } else {
                    element.onPhantomUpEvent(eventTime)
                    continue  // Remove this element from the expandableArray.
                }
                if (newIndex != index) {
                    // Shift this element toward the beginning of the expandableArray.
                    expandableArray.set(newIndex, element)
                }
                newIndex++
            }
            mArraySize = newIndex
        }
    }

    fun hasModifierKeyOlderThan(pointer: Element): Boolean {
        synchronized(mExpandableArrayOfActivePointers) {
            val expandableArray: ArrayList<Element> = mExpandableArrayOfActivePointers
            val arraySize: Int = mArraySize
            for (index in 0 until arraySize) {
                val element: Element = expandableArray.get(index)
                if (element === pointer) {
                    return false // Stop searching modifier key.
                }
                if (element.isModifier()) {
                    return true
                }
            }
            return false
        }
    }

    fun isAnyInDraggingFinger(): Boolean {
        synchronized(mExpandableArrayOfActivePointers) {
            val expandableArray: ArrayList<Element> = mExpandableArrayOfActivePointers
            val arraySize: Int = mArraySize
            for (index in 0 until arraySize) {
                val element: Element = expandableArray.get(index)
                if (element.isInDraggingFinger()) {
                    return true
                }
            }
            return false
        }
    }

    fun cancelAllPointerTrackers() {
        synchronized(mExpandableArrayOfActivePointers) {
            if (DEBUG) {
                Log.d(TAG, "cancelAllPointerTracker: " + this)
            }
            val expandableArray: ArrayList<Element> = mExpandableArrayOfActivePointers
            val arraySize: Int = mArraySize
            for (index in 0 until arraySize) {
                val element: Element = expandableArray.get(index)
                element.cancelTrackingForAction()
            }
        }
    }

    override fun toString(): String {
        synchronized(mExpandableArrayOfActivePointers) {
            val sb: StringBuilder = StringBuilder()
            val expandableArray: ArrayList<Element> = mExpandableArrayOfActivePointers
            val arraySize: Int = mArraySize
            for (index in 0 until arraySize) {
                val element: Element = expandableArray.get(index)
                if (sb.length > 0) {
                    sb.append(" ")
                }
                sb.append(element.toString())
            }
            return "[" + sb.toString() + "]"
        }
    }

    companion object {
        private val TAG: String = PointerTrackerQueue::class.java.getSimpleName()
        private const val DEBUG: Boolean = false

        private const val INITIAL_CAPACITY: Int = 10
    }
}
