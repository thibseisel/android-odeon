/*
 * Copyright 2018 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.media

import android.database.AbstractCursor
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import java.io.InputStream
import java.io.Reader

/**
 * A [Cursor] implementation backed by the content of a CSV file.
 *
 * @constructor
 * Initialize this cursor by the data read from the provided [source].
 * @param source A Reader pointing to a file or stream of data formatted as CSV.
 *
 */
class CSVCursor(source: Reader) : AbstractCursor() {

    private val columnHeaders: List<String>
    private val values: List<List<String>>

    init {
        val lines = source.buffered().lineSequence()
        columnHeaders = lines.first().split(';')
        values = lines.map { it.split(';') }.toList()
    }

    constructor(source: InputStream) : this(source.reader())

    override fun getCount(): Int = values.size
    override fun getColumnNames(): Array<String> = columnHeaders.toTypedArray()

    override fun getString(column: Int): String? = get(column)
    override fun getShort(column: Int): Short = get(column)?.toShort() ?: 0
    override fun getInt(column: Int): Int = get(column)?.toInt() ?: 0
    override fun getLong(column: Int): Long = get(column)?.toLong() ?: 0L
    override fun getFloat(column: Int): Float = get(column)?.toFloat() ?: 0f
    override fun getDouble(column: Int): Double = get(column)?.toDouble() ?: 0.0
    override fun isNull(column: Int): Boolean = get(column) == null

    private fun get(column: Int): String? {
        if (column !in columnHeaders.indices) {
            throw CursorIndexOutOfBoundsException(column, columnHeaders.size)
        }

        if (position < 0) {
            throw CursorIndexOutOfBoundsException("Before first row.")
        }

        if (position >= columnHeaders.size) {
            throw CursorIndexOutOfBoundsException("After last row.")
        }

        return values[position][column]
    }
}