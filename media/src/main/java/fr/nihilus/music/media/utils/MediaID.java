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

package fr.nihilus.music.media.utils;

import androidx.annotation.NonNull;

/**
 * @deprecated Use MediaId class instead.
 * It provides safe media id instances with utility for extracting its parts.
 */
@Deprecated
public final class MediaID {

    /**
     * The root of the MediaBrowser displayed to untrusted packages.
     * This is only used for debug builds to allow interaction with the MediaControllerTest app.
     */
    public static final String ID_EMPTY_ROOT = "EMPTY";

    private static final char CATEGORY_SEPARATOR = '/';
    private static final char LEAF_SEPARATOR = '|';

    /**
     * Extracts category and categoryValue from the mediaID. mediaID is, by this sample's
     * convention, a concatenation of category (eg "by_genre"), categoryValue (eg "Classical") and
     * mediaID. This is necessary so we know where the user selected the music from, when the music
     * exists in more than one music list, and thus we are able to correctly build the playing queue.
     *
     * @param mediaID that contains a category and categoryValue.
     */
    @NonNull
    private static String[] getHierarchy(@NonNull String mediaID) {
        int pos = mediaID.indexOf(LEAF_SEPARATOR);
        if (pos >= 0) {
            mediaID = mediaID.substring(0, pos);
        }
        return mediaID.split(String.valueOf(CATEGORY_SEPARATOR));
    }

    public static String categoryValueOf(@NonNull String mediaID) {
        String[] hierarchy = getHierarchy(mediaID);
        if (hierarchy.length == 2) {
            return hierarchy[1];
        }
        return null;
    }

    @NonNull
    public static String getIdRoot(@NonNull String mediaId) {
        int indexOfCategory = mediaId.indexOf(CATEGORY_SEPARATOR);
        int indexOfLeaf = mediaId.indexOf(LEAF_SEPARATOR);

        if (indexOfCategory > 0) {
            // Media ID has categories. Root is before the first category separator.
            return mediaId.substring(0, indexOfCategory);
        } else if (indexOfLeaf > 0) {
            // Media ID has only a music id. Root is before the leaf separator.
            return mediaId.substring(0, indexOfLeaf);
        } else {
            // Media Id is only composed of the root.
            return mediaId;
        }
    }
}
