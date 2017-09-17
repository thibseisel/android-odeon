/*
 * Copyright (C) 2008 The Android Open Source Project
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
package fr.nihilus.music.utils;

import android.database.DataSetObserver;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.SectionIndexer;

import java.text.Collator;
import java.util.List;

/**
 * A {@link SectionIndexer} that allows FastScrolling and indexing in a list of {@link MediaItem}.
 * Sort categories are built upon the first letter of the item's title.
 * Titles that does not start with a letter A-Z falls under the # category.
 */
public class MediaItemIndexer extends DataSetObserver implements SectionIndexer {

    private static final String TAG = "MediaItemIndexer";
    private static final String[] ALPHABET = {"#", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    private List<MediaItem> mItems;
    private final Collator mCollator;
    private final SparseIntArray mAlphaMap;

    public MediaItemIndexer(List<MediaItem> items) {
        mItems = items;
        mCollator = Collator.getInstance();
        mCollator.setStrength(Collator.PRIMARY);
        mAlphaMap = new SparseIntArray(ALPHABET.length);
    }

    @Override
    public String[] getSections() {
        return ALPHABET;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        final SparseIntArray alphaMap = mAlphaMap;
        final List<MediaItem> items = mItems;
        // Check items and bounds
        if (items == null || sectionIndex <= 0) {
            return 0;
        }
        if (sectionIndex >= ALPHABET.length) {
            sectionIndex = ALPHABET.length - 1;
        }

        int count = items.size();
        int start = 0;
        int end = count;
        int pos;

        String targetLetter = ALPHABET[sectionIndex];
        int key = targetLetter.charAt(0);

        // Is it a cached value ?
        if (Integer.MIN_VALUE != (pos = alphaMap.get(key, Integer.MIN_VALUE))) {
            // Is that position an approximation ?
            // Negative position for an approximation, positive for an exact position.
            if (pos < 0) {
                pos = -pos;
                end = pos;
            } else {
                // This is the exact position, return it
                return pos;
            }
        }

        // Do we know the position of the previous section ? (optimisation)
        if (sectionIndex > 0) {
            int prevLetter = ALPHABET[sectionIndex - 1].charAt(0);
            int prevLetterPos = alphaMap.get(prevLetter, Integer.MIN_VALUE);
            if (prevLetterPos != Integer.MIN_VALUE) {
                start = Math.abs(prevLetterPos);
            }
        }

        // Let's binary search
        pos = (start + end) / 2;

        while (pos < end) {
            CharSequence name = items.get(pos).getDescription().getTitle();
            if (name == null) {
                if (pos == 0) {
                    break;
                } else {
                    pos--;
                    continue;
                }
            }
            int diff = compare(name.toString(), targetLetter);
            if (diff != 0) {
                if (diff < 0) {
                    start = pos + 1;
                    if (start >= count) {
                        pos = count;
                        break;
                    }
                } else {
                    end = pos;
                }
            } else {
                if (start == pos) {
                    // This is it
                    break;
                } else {
                    // We need to go further
                    end = pos;
                }
            }
            pos = (start + end) / 2;
        }
        alphaMap.put(key, pos);
        return pos;
    }

    @Override
    public int getSectionForPosition(int position) {
        final MediaItem item = mItems.get(position);
        CharSequence name = item.getDescription().getTitle();
        if (name == null) {
            Log.w(TAG, "getSectionForPosition: mediaItem has no name.");
            return 0;
        }
        // Linear search, since there are only a few items in th section index
        for (int i = 0; i < ALPHABET.length; i++) {
            String targetLetter = ALPHABET[i];
            if (compare(name.toString(), targetLetter) == 0) {
                return i;
            }
        }
        // Does not recognize the letter - put in the zero'th section
        return 0;
    }

    public void setItems(List<MediaItem> items) {
        mItems = items;
        mAlphaMap.clear();
    }

    private int compare(String word, String letter) {
        word = keyFor(word);
        final String firstLetter;
        if (word.length() == 0) {
            firstLetter = " ";
        } else {
            firstLetter = word.substring(0, 1);
        }

        return mCollator.compare(firstLetter, letter);
    }

    @Override
    public void onChanged() {
        super.onChanged();
        mAlphaMap.clear();
    }

    @Override
    public void onInvalidated() {
        super.onInvalidated();
        mAlphaMap.clear();
    }

    /**
     * Removes common english prefixes such as "The", "An", "A" from a word.
     * This ensures coherence with MediaItem sorting that depends on the title key.
     * @param word the word from which remove prefixes
     * @return the word without its prefixes
     */
    private static String keyFor(String word) {
        word = word.trim().toLowerCase();
        if (word.startsWith("the ")) {
            word = word.substring(4);
        }
        if (word.startsWith("an ")) {
            word = word.substring(3);
        }
        if (word.startsWith("a ")) {
            word = word.substring(2);
        }
        return word;
    }
}
