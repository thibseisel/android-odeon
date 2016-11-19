package fr.nihilus.mymusic.widget;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.util.SparseIntArray;
import android.widget.SectionIndexer;

import java.text.Collator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class AlphabetIndexer implements SectionIndexer {

    private static final String ALPHABET = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final Set<String> mFigures;

    private List<MediaBrowserCompat.MediaItem> mItems;

    /**
     * Cached length of the alphabet array.
     */
    private int mAlphabetLength;

    /**
     * This contains a cache of the computed indices so far. It will get reset whenever
     * the dataset changes or the cursor changes.
     */
    private SparseIntArray mAlphaMap;

    /**
     * Use a collator to compare strings in a localized manner.
     */
    private Collator mCollator;

    /**
     * The section array converted from the alphabet string.
     */
    private String[] mAlphabetArray;

    public AlphabetIndexer(List<MediaBrowserCompat.MediaItem> items) {
        mItems = items;
        mAlphabetArray = ALPHABET.split("");
        mAlphabetLength = ALPHABET.length();
        mAlphaMap = new SparseIntArray(mAlphabetLength);
        mCollator = Collator.getInstance();
        mCollator.setStrength(Collator.PRIMARY);
        mFigures = new HashSet<>();
        mFigures.addAll(Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
    }

    public void setItems(List<MediaBrowserCompat.MediaItem> newItems) {
        mItems = newItems;
        mAlphaMap.clear();
    }

    /**
     * Default implementation compares the first character of word with letter.
     */
    private int compare(String word, String letter) {
        final String firstLetter;
        if (word.length() == 0) {
            firstLetter = " ";
        } else {
            firstLetter = word.substring(0, 1);
        }
        return mCollator.compare(firstLetter, letter);
    }

    @Override
    public Object[] getSections() {
        return mAlphabetArray;
    }

    /**
     * Performs a binary search or cache lookup to find the first row that
     * matches a given section's starting letter.
     *
     * @param sectionIndex the section to search for
     * @return the row index of the first occurrence, or the nearest next letter.
     * For instance, if searching for "T" and no "T" is found, then the first
     * row starting with "U" or any higher letter is returned. If there is no
     * data following "T" at all, then the list size is returned.
     */
    @Override
    public int getPositionForSection(int sectionIndex) {
        final SparseIntArray alphaMap = mAlphaMap;
        final List<MediaBrowserCompat.MediaItem> items = mItems;

        // FIXME Décalage : fastscroll sur E donne F...

        if (items == null) {
            return 0;
        }

        // Check bounds
        if (sectionIndex <= 0) {
            return 0;
        }
        if (sectionIndex >= mAlphabetLength) {
            sectionIndex = mAlphabetLength - 1;
        }

        int count = items.size();
        int start = 0;
        int end = count;
        int pos;

        char letter = ALPHABET.charAt(sectionIndex);
        String targetLetter = Character.toString(letter);
        int key = (int) letter;
        // Check map
        if (Integer.MIN_VALUE != (pos = alphaMap.get(key, Integer.MIN_VALUE))) {
            // Is it approximate? Using negative value to indicate that it's
            // an approximation and positive value when it is the accurate
            // position.
            if (pos < 0) {
                pos = -pos;
                end = pos;
            } else {
                // Not approximate, this is the confirmed start of section, return it
                return pos;
            }
        }

        // Do we have the position of the previous section?
        if (sectionIndex > 0) {
            int prevLetter =
                    ALPHABET.charAt(sectionIndex - 1);
            int prevLetterPos = alphaMap.get(prevLetter, Integer.MIN_VALUE);
            if (prevLetterPos != Integer.MIN_VALUE) {
                start = Math.abs(prevLetterPos);
            }
        }

        // Now that we have a possibly optimized start and end, let's binary search

        pos = (end + start) / 2;

        while (pos < end) {
            // Get letter at pos
            MediaDescriptionCompat currentItem = items.get(pos).getDescription();
            CharSequence curName = currentItem.getTitle();
            if (curName == null) {
                if (pos == 0) {
                    break;
                } else {
                    pos--;
                    continue;
                }
            }
            int diff = compare(curName.toString(), targetLetter);
            if (diff != 0) {
                // TODO: Commenting out approximation code because it doesn't work for certain
                // lists with custom comparators
                // Enter approximation in hash if a better solution doesn't exist
                // String startingLetter = Character.toString(getFirstLetter(curName));
                // int startingLetterKey = startingLetter.charAt(0);
                // int curPos = alphaMap.get(startingLetterKey, Integer.MIN_VALUE);
                // if (curPos == Integer.MIN_VALUE || Math.abs(curPos) > pos) {
                //     Negative pos indicates that it is an approximation
                //     alphaMap.put(startingLetterKey, -pos);
                // }
                // if (mCollator.compare(startingLetter, targetLetter) < 0) {
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
                // They're the same, but that doesn't mean it's the start
                if (start == pos) {
                    // This is it
                    break;
                } else {
                    // Need to go further lower to find the starting row
                    end = pos;
                }
            }
            pos = (start + end) / 2;
        }
        alphaMap.put(key, pos);
        return pos;
    }

    /**
     * Returns the section index for a given position in the list by querying the item
     * and comparing it with all items in the section array.
     */
    public int getSectionForPosition(int position) {
        MediaDescriptionCompat currentItem = mItems.get(position).getDescription();
        CharSequence curName = currentItem.getTitle();

        if (curName == null) return 0;

        if (mFigures.contains(curName.subSequence(0, 1).toString())) return 0;

        // Linear search, as there are only a few items in the section index
        // Could speed this up later if it actually gets used.
        for (int i = 1; i < mAlphabetLength; i++) {
            char letter = ALPHABET.charAt(i);
            String targetLetter = Character.toString(letter);
            if (compare(curName.toString(), targetLetter) == 0) {
                return i;
            }
        }
        return 0; // Don't recognize the letter - falls under zero'th section
    }
}
