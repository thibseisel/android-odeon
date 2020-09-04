/*
 * Copyright 2020 Thibault Seisel
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

import androidx.media.MediaBrowserServiceCompat.BrowserRoot

/**
 * Define [android.os.Bundle] extra keys that are recognized by Android Auto
 * to apply custom behavior when displaying and browsing media items.
 */
internal object AutomotiveExtras {

    /**
     * Declares that custom styling for media items is supported.
     * This should be used as the key for a [Boolean] extra of [BrowserRoot].
     *
     * More info:
     * [Build media apps for cars - Apply content styling](https://developer.android.com/training/cars/media#default-content-style)
     *
     * @see CONTENT_STYLE_BROWSABLE_HINT
     * @see CONTENT_STYLE_PLAYABLE_HINT
     */
    const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"

    /**
     * Declares that the app supports browsing voice search results from the Android Auto UI.
     * This should be used as the key for a [Boolean] extra of [BrowserRoot].
     *
     * More info:
     * [Build media apps for cars - Display browsable search results](https://developer.android.com/training/cars/media#display_search)
     */
    const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

    /**
     * Bundle extra indicating the presentation hint for playable media items.
     * When set as a [BrowserRoot] extra, this defines the default content style for all playable items.
     * When set on an individual item, this overrides the default value for that item's direct children.
     *
     * @see CONTENT_STYLE_LIST_ITEM_HINT_VALUE
     * @see CONTENT_STYLE_GRID_ITEM_HINT_VALUE
     * @see CONTENT_STYLE_CATEGORY_LIST_ITEM_HINT_VALUE
     * @see CONTENT_STYLE_CATEGORY_GRID_ITEM_HINT_VALUE
     */
    const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"

    /**
     * Bundle extra indicating the presentation hint for browsable media items.
     * When set as a [BrowserRoot] extra, this defines the default content style for all browsable items.
     * When set on an individual item, this overrides the default value for that item's direct children.
     *
     * @see CONTENT_STYLE_LIST_ITEM_HINT_VALUE
     * @see CONTENT_STYLE_GRID_ITEM_HINT_VALUE
     * @see CONTENT_STYLE_CATEGORY_LIST_ITEM_HINT_VALUE
     * @see CONTENT_STYLE_CATEGORY_GRID_ITEM_HINT_VALUE
     */
    const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"

    /**
     * Specifies the corresponding items should be presented as lists.
     *
     * @see CONTENT_STYLE_BROWSABLE_HINT
     * @see CONTENT_STYLE_PLAYABLE_HINT
     */
    const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1

    /**
     * Specifies that the corresponding items should be presented as grids.
     *
     * @see CONTENT_STYLE_BROWSABLE_HINT
     * @see CONTENT_STYLE_PLAYABLE_HINT
     */
    const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2

    /**
     * Specifies that the corresponding items should be presented as lists and are
     * represented by a vector icon. This adds a small margin around the icons
     * instead of filling the full available area.
     *
     * @see CONTENT_STYLE_BROWSABLE_HINT
     * @see CONTENT_STYLE_PLAYABLE_HINT
     */
    const val CONTENT_STYLE_CATEGORY_LIST_ITEM_HINT_VALUE = 3

    /**
     * Specifies that the corresponding items should be presented as grids and are
     * represented by a vector icon. This adds a small margin around the icons
     * instead of filling the full available area.
     *
     * @see CONTENT_STYLE_BROWSABLE_HINT
     * @see CONTENT_STYLE_PLAYABLE_HINT
     */
    const val CONTENT_STYLE_CATEGORY_GRID_ITEM_HINT_VALUE = 4
}
