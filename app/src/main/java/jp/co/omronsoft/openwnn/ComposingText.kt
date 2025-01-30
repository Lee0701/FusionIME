/*
 * Copyright (C) 2008-2012  OMRON SOFTWARE Co., Ltd.
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
package jp.co.omronsoft.openwnn

import android.util.Log

/**
 * The container class of composing string.
 *
 * This interface is for the class includes information about the
 * input string, the converted string and its decoration.
 * [LetterConverter] and [WnnEngine] get the input string from it, and
 * store the converted string into it.
 *
 * @author Copyright (C) 2009 OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
class ComposingText {
    /** Composing text's layer data  */
    protected var mStringLayer: Array<ArrayList<StrSegment?>>

    /** Cursor position  */
    protected var mCursor: IntArray

    /**
     * Constructor
     */
    init {
        mStringLayer = Array(MAX_LAYER) { ArrayList() }
        mCursor = IntArray(MAX_LAYER)
    }

    /**
     * Output internal information to the log.
     */
    fun debugout() {
        for (i in 0 until MAX_LAYER) {
            Log.d("OpenWnn", "ComposingText[$i]")
            Log.d("OpenWnn", "  cur = " + mCursor[i])
            var tmp = ""
            val it: Iterator<StrSegment?> = mStringLayer[i].iterator()
            while (it.hasNext()) {
                val ss = it.next()
                tmp += "(" + ss!!.string + "," + ss.from + "," + ss.to + ")"
            }
            Log.d("OpenWnn", "  str = $tmp")
        }
    }

    /**
     * Get a [StrSegment] at the position specified.
     *
     * @param layer     Layer
     * @param pos       Position (<0 : the tail segment)
     *
     * @return          The segment; `null` if error occurs.
     */
    fun getStrSegment(layer: Int, pos: Int): StrSegment? {
        var pos = pos
        try {
            val strLayer = mStringLayer[layer]
            if (pos < 0) {
                pos = strLayer.size - 1
            }
            if (pos >= strLayer.size || pos < 0) {
                return null
            }
            return strLayer[pos]
        } catch (ex: Exception) {
            return null
        }
    }

    /**
     * Convert the range of segments to a string.
     *
     * @param layer     Layer
     * @param from      Convert range from
     * @param to        Convert range to
     * @return          The string converted; `null` if error occurs.
     */
    fun toString(layer: Int, from: Int, to: Int): String? {
        try {
            val buf = StringBuffer()
            val strLayer = mStringLayer[layer]

            for (i in from..to) {
                val ss = strLayer[i]
                buf.append(ss!!.string)
            }
            return buf.toString()
        } catch (ex: Exception) {
            return null
        }
    }

    /**
     * Convert segments of the layer to a string.
     *
     * @param layer     Layer
     * @return          The string converted; `null` if error occurs.
     */
    fun toString(layer: Int): String? {
        return this.toString(layer, 0, mStringLayer[layer].size - 1)
    }

    /**
     * Update the upper layer's data.
     *
     * @param layer         The base layer
     * @param mod_from      Modified from
     * @param mod_len       Length after modified (# of StrSegments from `mod_from`)
     * @param org_len       Length before modified (# of StrSegments from `mod_from`)
     */
    private fun modifyUpper(layer: Int, mod_from: Int, mod_len: Int, org_len: Int) {
        if (layer >= MAX_LAYER - 1) {
            /* no layer above */
            return
        }

        val uplayer = layer + 1
        val strUplayer = mStringLayer[uplayer]
        if (strUplayer.size <= 0) {
            /* 
             * if there is no element on above layer,
             * add a element includes whole elements of the lower layer.
             */
            strUplayer.add(StrSegment(toString(layer), 0, mStringLayer[layer].size - 1))
            modifyUpper(uplayer, 0, 1, 0)
            return
        }

        val mod_to = mod_from + (if ((mod_len == 0)) 0 else (mod_len - 1))
        val org_to = mod_from + (if ((org_len == 0)) 0 else (org_len - 1))
        val last = strUplayer[strUplayer.size - 1]
        if (last!!.to < mod_from) {
            /* add at the tail */
            last.to = mod_to
            last.string = toString(layer, last.from, last.to)
            modifyUpper(uplayer, strUplayer.size - 1, 1, 1)
            return
        }

        var uplayer_mod_from = -1
        var uplayer_org_to = -1
        for (i in strUplayer.indices) {
            val ss = strUplayer[i]
            if (ss!!.from > mod_from) {
                if (ss.to <= org_to) {
                    /* the segment is included */
                    if (uplayer_mod_from < 0) {
                        uplayer_mod_from = i
                    }
                    uplayer_org_to = i
                } else {
                    /* included in this segment */
                    uplayer_org_to = i
                    break
                }
            } else {
                if (org_len == 0 && ss.from == mod_from) {
                    /* when an element is added */
                    uplayer_mod_from = i - 1
                    uplayer_org_to = i - 1
                    break
                } else {
                    /* start from this segment */
                    uplayer_mod_from = i
                    uplayer_org_to = i
                    if (ss.to >= org_to) {
                        break
                    }
                }
            }
        }

        val diff = mod_len - org_len
        if (uplayer_mod_from >= 0) {
            /* update an element */
            var ss = strUplayer[uplayer_mod_from]
            var last_to = ss!!.to
            val next = uplayer_mod_from + 1
            for (i in next..uplayer_org_to) {
                ss = strUplayer[next]
                if (last_to > ss!!.to) {
                    last_to = ss.to
                }
                strUplayer.removeAt(next)
            }
            ss!!.to = if ((last_to < mod_to)) mod_to else (last_to + diff)

            ss.string = toString(layer, ss.from, ss.to)

            for (i in next until strUplayer.size) {
                ss = strUplayer[i]
                ss!!.from += diff
                ss!!.to += diff
            }

            modifyUpper(uplayer, uplayer_mod_from, 1, uplayer_org_to - uplayer_mod_from + 1)
        } else {
            /* add an element at the head */
            var ss: StrSegment? = StrSegment(
                toString(layer, mod_from, mod_to),
                mod_from, mod_to
            )
            strUplayer.add(0, ss)
            for (i in 1 until strUplayer.size) {
                ss = strUplayer[i]
                ss!!.from += diff
                ss!!.to += diff
            }
            modifyUpper(uplayer, 0, 1, 0)
        }

        return
    }

    /**
     * Insert a [StrSegment] at the cursor position.
     *
     * @param layer Layer to insert
     * @param str   String
     */
    fun insertStrSegment(layer: Int, str: StrSegment?) {
        val cursor = mCursor[layer]
        mStringLayer[layer].add(cursor, str)
        modifyUpper(layer, cursor, 1, 0)
        setCursor(layer, cursor + 1)
    }

    /**
     * Insert a [StrSegment] at the cursor position(without merging to the previous segment).
     *
     *
     * @param layer1        Layer to insert
     * @param layer2        Never merge to the previous segment from `layer1` to `layer2`.
     * @param str           String
     */
    fun insertStrSegment(layer1: Int, layer2: Int, str: StrSegment) {
        mStringLayer[layer1].add(mCursor[layer1], str)
        mCursor[layer1]++

        for (i in layer1 + 1..layer2) {
            val pos = mCursor[i - 1] - 1
            val tmp = StrSegment(str.string, pos, pos)
            val strLayer = mStringLayer[i]
            strLayer.add(mCursor[i], tmp)
            mCursor[i]++
            for (j in mCursor[i] until strLayer.size) {
                val ss = strLayer[j]
                ss!!.from++
                ss.to++
            }
        }
        val cursor = mCursor[layer2]
        modifyUpper(layer2, cursor - 1, 1, 0)
        setCursor(layer2, cursor)
    }

    /**
     * Replace segments at the range specified.
     *
     * @param layer     Layer
     * @param str       String segment array to replace
     * @param from      Replace from
     * @param to        Replace to
     */
    protected fun replaceStrSegment0(layer: Int, str: Array<StrSegment?>, from: Int, to: Int) {
        var from = from
        var to = to
        val strLayer = mStringLayer[layer]

        if (from < 0 || from > strLayer.size) {
            from = strLayer.size
        }
        if (to < 0 || to > strLayer.size) {
            to = strLayer.size
        }
        for (i in from..to) {
            strLayer.removeAt(from)
        }
        for (i in str.indices.reversed()) {
            strLayer.add(from, str[i])
        }

        modifyUpper(layer, from, str.size, to - from + 1)
    }

    /**
     * Replace segments at the range specified.
     *
     * @param layer     Layer
     * @param str       String segment array to replace
     * @param num       Size of string segment array
     */
    fun replaceStrSegment(layer: Int, str: Array<StrSegment?>, num: Int) {
        val cursor = mCursor[layer]
        replaceStrSegment0(layer, str, cursor - num, cursor - 1)
        setCursor(layer, cursor + str.size - num)
    }

    /**
     * Replace the segment at the cursor.
     *
     * @param layer     Layer
     * @param str       String segment to replace
     */
    fun replaceStrSegment(layer: Int, str: Array<StrSegment?>) {
        val cursor = mCursor[layer]
        replaceStrSegment0(layer, str, cursor - 1, cursor - 1)
        setCursor(layer, cursor + str.size - 1)
    }

    /**
     * Delete segments.
     *
     * @param layer Layer
     * @param from  Delete from
     * @param to    Delete to
     */
    fun deleteStrSegment(layer: Int, from: Int, to: Int) {
        val fromL = intArrayOf(-1, -1, -1)
        val toL = intArrayOf(-1, -1, -1)

        val strLayer2 = mStringLayer[2]
        val strLayer1 = mStringLayer[1]

        if (layer == 2) {
            fromL[2] = from
            toL[2] = to
            fromL[1] = strLayer2[from]!!.from
            toL[1] = strLayer2[to]!!.to
            fromL[0] = strLayer1[fromL[1]]!!.from
            toL[0] = strLayer1[toL[1]]!!.to
        } else if (layer == 1) {
            fromL[1] = from
            toL[1] = to
            fromL[0] = strLayer1[from]!!.from
            toL[0] = strLayer1[to]!!.to
        } else {
            fromL[0] = from
            toL[0] = to
        }

        var diff = to - from + 1
        for (lv in 0 until MAX_LAYER) {
            if (fromL[lv] >= 0) {
                deleteStrSegment0(lv, fromL[lv], toL[lv], diff)
            } else {
                var boundary_from = -1
                var boundary_to = -1
                val strLayer = mStringLayer[lv]
                for (i in strLayer.indices) {
                    val ss = strLayer[i]
                    if ((ss!!.from >= fromL[lv - 1] && ss.from <= toL[lv - 1]) ||
                        (ss.to >= fromL[lv - 1] && ss.to <= toL[lv - 1])
                    ) {
                        if (fromL[lv] < 0) {
                            fromL[lv] = i
                            boundary_from = ss.from
                        }
                        toL[lv] = i
                        boundary_to = ss.to
                    } else if (ss.from <= fromL[lv - 1] && ss.to >= toL[lv - 1]) {
                        boundary_from = ss.from
                        boundary_to = ss.to
                        fromL[lv] = i
                        toL[lv] = i
                        break
                    } else if (ss.from > toL[lv - 1]) {
                        break
                    }
                }
                if (boundary_from != fromL[lv - 1] || boundary_to != toL[lv - 1]) {
                    deleteStrSegment0(lv, fromL[lv] + 1, toL[lv], diff)
                    boundary_to -= diff
                    val tmp = arrayOf<StrSegment?>(
                        (StrSegment(toString(lv - 1), boundary_from, boundary_to))
                    )
                    replaceStrSegment0(lv, tmp, fromL[lv], fromL[lv])
                    return
                } else {
                    deleteStrSegment0(lv, fromL[lv], toL[lv], diff)
                }
            }
            diff = toL[lv] - fromL[lv] + 1
        }
    }

    /**
     * Delete segments (internal method).
     *
     * @param layer     Layer
     * @param from      Delete from
     * @param to        Delete to
     * @param diff      Differential
     */
    private fun deleteStrSegment0(layer: Int, from: Int, to: Int, diff: Int) {
        val strLayer = mStringLayer[layer]
        if (diff != 0) {
            for (i in to + 1 until strLayer.size) {
                val ss = strLayer[i]
                ss!!.from -= diff
                ss!!.to -= diff
            }
        }
        for (i in from..to) {
            strLayer.removeAt(from)
        }
    }

    /**
     * Delete a segment at the cursor.
     *
     * @param layer         Layer
     * @param rightside     `true` if direction is rightward at the cursor, `false` if direction is leftward at the cursor
     * @return              The number of string segments in the specified layer
     */
    fun delete(layer: Int, rightside: Boolean): Int {
        val cursor = mCursor[layer]
        val strLayer = mStringLayer[layer]

        if (!rightside && cursor > 0) {
            deleteStrSegment(layer, cursor - 1, cursor - 1)
            setCursor(layer, cursor - 1)
        } else if (rightside && cursor < strLayer.size) {
            deleteStrSegment(layer, cursor, cursor)
            setCursor(layer, cursor)
        }
        return strLayer.size
    }

    /**
     * Get the string layer.
     *
     * @param layer     Layer
     * @return          [ArrayList] of [StrSegment]; `null` if error.
     */
    fun getStringLayer(layer: Int): ArrayList<StrSegment?>? {
        return try {
            mStringLayer[layer]
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Get upper the segment which includes the position.
     *
     * @param layer     Layer
     * @param pos       Position
     * @return      Index of upper segment
     */
    private fun included(layer: Int, pos: Int): Int {
        if (pos == 0) {
            return 0
        }
        val uplayer = layer + 1
        val strLayer = mStringLayer[uplayer]
        var i = 0
        while (i < strLayer.size) {
            val ss = strLayer[i]
            if (ss!!.from <= pos && pos <= ss.to) {
                break
            }
            i++
        }
        return i
    }

    /**
     * Set the cursor.
     *
     * @param layer     Layer
     * @param pos       Position of cursor
     * @return      New position of cursor
     */
    fun setCursor(layer: Int, pos: Int): Int {
        var pos = pos
        if (pos > mStringLayer[layer].size) {
            pos = mStringLayer[layer].size
        }
        if (pos < 0) {
            pos = 0
        }
        if (layer == 0) {
            mCursor[0] = pos
            mCursor[1] = included(0, pos)
            mCursor[2] = included(1, mCursor[1])
        } else if (layer == 1) {
            mCursor[2] = included(1, pos)
            mCursor[1] = pos
            mCursor[0] = if ((pos > 0)) mStringLayer[1][pos - 1]!!.to + 1 else 0
        } else {
            mCursor[2] = pos
            mCursor[1] = if ((pos > 0)) mStringLayer[2][pos - 1]!!.to + 1 else 0
            mCursor[0] = if ((mCursor[1] > 0)) mStringLayer[1][mCursor[1] - 1]!!.to + 1 else 0
        }
        return pos
    }

    /**
     * Move the cursor.
     *
     * @param layer     Layer
     * @param diff      Relative position from current cursor position
     * @return      New position of cursor
     */
    fun moveCursor(layer: Int, diff: Int): Int {
        val c = mCursor[layer] + diff

        return setCursor(layer, c)
    }

    /**
     * Get the cursor position.
     *
     * @param layer     Layer
     * @return cursor   Current position of cursor
     */
    fun getCursor(layer: Int): Int {
        return mCursor[layer]
    }

    /**
     * Get the number of segments.
     *
     * @param layer     Layer
     * @return          Number of segments
     */
    fun size(layer: Int): Int {
        return mStringLayer[layer].size
    }

    /**
     * Clear all information.
     */
    fun clear() {
        for (i in 0 until MAX_LAYER) {
            mStringLayer[i].clear()
            mCursor[i] = 0
        }
    }

    companion object {
        /**
         * Text layer 0.
         * <br></br>
         * This text layer holds key strokes.<br></br>
         * (ex) Romaji in Japanese.  Parts of Hangul in Korean.
         */
        const val LAYER0: Int = 0

        /**
         * Text layer 1.
         * <br></br>
         * This text layer holds the result of the letter converter.<br></br>
         * (ex) Hiragana in Japanese. Pinyin in Chinese. Hangul in Korean.
         */
        const val LAYER1: Int = 1

        /**
         * Text layer 2.
         * <br></br>
         * This text layer holds the result of the consecutive clause converter.<br></br>
         * (ex) the result of Kana-to-Kanji conversion in Japanese,
         * Pinyin-to-Kanji conversion in Chinese, Hangul-to-Hanja conversion in Korean language.
         */
        const val LAYER2: Int = 2

        /** Maximum number of layers  */
        const val MAX_LAYER: Int = 3
    }
}
