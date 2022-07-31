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

package fr.nihilus.music.ui.library.search

/**
 * State of the "search" screen.
 */
internal data class SearchScreenUiState(
    /**
     * User-defined text used to filter search results.
     */
    val query: String,
    /**
     * List of media that matched the query.
     * Media are organized in sections of the same type and separated by section titles.
     */
    val results: List<SearchResult>
)
