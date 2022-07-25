/*
 *  Copyright 2016-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package com.amplifyframework.analytics.pinpoint.internal.core.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Integer.min
import java.lang.StringBuilder
import java.nio.charset.Charset
import kotlin.Throws

/**
 * String utility methods.
 */
const val UTF_8 = "UTF-8"

/**
 * Determines if a string is null or zero-length.
 *
 * @return true if this string is null or zero-length, otherwise false
 */
fun String.isNullOrEmpty(): Boolean {
    return this.isEmpty()
}

/**
 * Determines if a string is null, zero-length or entirely whitespace.
 *
 * @return true if this is null, zero-length or whitespace, otherwise false
 */
fun String?.isBlank(): Boolean {
    return this == null || this.trim { it <= ' ' }.isEmpty()
}

/**
 * Converts an input stream into a string value.
 *
 * @return a string containing the content of the input stream
 * @throws IOException upon a failure reading the input stream
 */
@Throws(IOException::class)
fun InputStream.convertToString(charset: Charset?): String {
    val br = BufferedReader(InputStreamReader(this, charset))
    val sb = StringBuilder()
    var line: String?
    while (br.readLine().also { line = it } != null) {
        sb.append("$line\n")
    }
    br.close()
    return sb.toString()
}

/**
 * Converts an input stream into a string value.
 *
 * @return a string containing the content of the input stream
 * @throws IOException upon a failure reading the input stream
 */
@Throws(IOException::class)
fun InputStream.convertToUTF8String(): String {
    return this.convertToString(Charset.forName(UTF_8))
}

/**
 * Reduces the input string to the number of chars, or its length if the
 * number of chars exceeds the input string's length
 *
 * @param numChars the number of leading chars to keep (all others will be
 * removed)
 * @return: the clipped string
 */
fun String.clip(numChars: Int, appendEllipses: Boolean): String {
    val end = min(numChars, this.length)
    var output = this.substring(0, end)
    if (appendEllipses) {
        output = if (output.length < this.length) "$output..." else output
    }
    return output
}

fun Array<String>?.convertToString(): String {
    if (this == null) {
        return ""
    }
    val ret = StringBuilder()
    for (i in this.indices) {
        if (i > 0) {
            ret.append(",")
        }
        ret.append("'${this[i]}'")
    }
    return ret.toString()
}

fun Set<String>?.convertToString(): String {
    if (this == null) {
        return ""
    }
    val ret = StringBuilder()
    for (str in this) {
        if (ret.isNotEmpty()) {
            ret.append(",")
        }
        ret.append("'$str'")
    }
    return ret.toString()
}

/**
 * Trims string to its last X characters. If string is too short, is padded
 * at the front with given char
 *
 * @param len - length of desired string. (must be positive)
 * @param pad - character to pad with
 */
fun String?.trimOrPadString(length: Int, pad: Char): String {
    var str = this
    var len = length
    if (len < 0) {
        len = 0
    }
    if (str == null) {
        str = ""
    }
    val s = StringBuffer()
    if (str.length > len - 1) {
        s.append(str.substring(str.length - len))
    } else {
        for (i in 0 until len - str.length) {
            s.append(pad)
        }
        s.append(str)
    }
    return s.toString()
}

/**
 * Checks if a CharSequence is whitespace, empty ("") or null.
 *
 * @return if the CharSequence is null, empty or whitespace
 */
fun CharSequence?.isBlank(): Boolean {
    val strLen = this?.length ?: 0
    if (this == null || strLen == 0) {
        return true
    }
    for (i in 0 until strLen) {
        if (!Character.isWhitespace(this[i])) {
            return false
        }
    }
    return true
}