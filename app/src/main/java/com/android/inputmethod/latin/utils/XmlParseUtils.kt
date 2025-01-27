/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.res.TypedArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

object XmlParseUtils {
    @Throws(XmlPullParserException::class, IOException::class)
    fun checkEndTag(tag: String, parser: XmlPullParser) {
        if (parser.next() == XmlPullParser.END_TAG && tag == parser.name) return
        throw NonEmptyTag(parser, tag)
    }

    @Throws(XmlPullParserException::class)
    fun checkAttributeExists(
        attr: TypedArray, attrId: Int,
        attrName: String, tag: String, parser: XmlPullParser
    ) {
        if (attr.hasValue(attrId)) {
            return
        }
        throw ParseException(
            "No $attrName attribute found in <$tag/>", parser
        )
    }

    open class ParseException(msg: String, parser: XmlPullParser) :
        XmlPullParserException(msg + " at " + parser.positionDescription)

    class IllegalStartTag(parser: XmlPullParser, tag: String, parent: String) :
        ParseException("Illegal start tag $tag in $parent", parser)

    class IllegalEndTag(parser: XmlPullParser, tag: String, parent: String) :
        ParseException("Illegal end tag $tag in $parent", parser)

    class IllegalAttribute(
        parser: XmlPullParser, tag: String,
        attribute: String
    ) : ParseException("Tag $tag has illegal attribute $attribute", parser)

    class NonEmptyTag(parser: XmlPullParser, tag: String) :
        ParseException("$tag must be empty tag", parser)
}
