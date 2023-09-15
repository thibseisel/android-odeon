/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.service

/**
 * Define the parameters for paginating media items.
 *
 * @param page The index of the page of results to return, `0` being the first page.
 * @param size The number of items returned per page.
 */
internal class PaginationOptions(page: Int, size: Int) {
    val page = page.coerceAtLeast(MINIMUM_PAGE_NUMBER)
    val size = size.coerceAtLeast(MINIMUM_PAGE_SIZE)

    companion object {

        /**
         * The default index of the returned page of media children when none is specified.
         * This is the index of the first page.
         */
        const val DEFAULT_PAGE_NUMBER = 0

        /**
         * The default number of media items to return in a page when none is specified.
         * All children will be returned in the same page.
         */
        const val DEFAULT_PAGE_SIZE = Int.MAX_VALUE

        /**
         * The minimum accepted value for [PaginationOptions.page].
         * This is the index of the first page.
         */
        private const val MINIMUM_PAGE_NUMBER = 0

        /**
         * The minimum accepted value for [PaginationOptions.size].
         * This is the minimum of items that can be displayed in a page.
         */
        private const val MINIMUM_PAGE_SIZE = 1
    }
}
