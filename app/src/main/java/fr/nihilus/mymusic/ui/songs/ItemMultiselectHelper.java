package fr.nihilus.mymusic.ui.songs;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;

/**
 * Helper class to use a contextual action mode for a collection of items, such as
 * {@link android.support.v7.widget.ListView} or {@link RecyclerView}.
 * It will create an ActionMode when an item is long pressed, and keep track
 * of items that were selected.
 */
public abstract class ItemMultiselectHelper implements ActionMode.Callback {

    private final AppCompatActivity mActivity;
    private SparseBooleanArray mCheckedItems;
    private ActionMode mActionMode;
    private boolean mSelectEnabled;
    private int mCheckedCount;

    /**
     * Create a new instance of this helper class that can start a selection mode
     * in the specified activity.
     * @param activity activity that hosts the selection mode
     */
    public ItemMultiselectHelper(AppCompatActivity activity) {
        mActivity = activity;
    }

    /**
     * Call this method when an item view react to a click event. To ensure proper behavior,
     * you should trigger your normal {@link AdapterView.OnItemClickListener#onItemClick} code
     * only when this method returns false.
     *
     * @param view     view that was clicked
     * @param position adapter position of the item tht was clicked
     * @return whether the click event was handled
     */
    public boolean onItemClick(View view, int position) {
        if (mSelectEnabled) {
            // Toggle state for this position
            boolean wasChecked = mCheckedItems.get(position, false);
            mCheckedItems.put(position, !wasChecked);
            onItemCheckedStateChanged(mActionMode, position, !wasChecked);

            // Keep count of checked items. If it falls to zero, finish ActionMode
            mCheckedCount += (wasChecked ? -1 : 1);
            if (mCheckedCount == 0) {
                mCheckedItems = null;
                mActionMode.finish();
            }
            return true;
        }
        return false;
    }

    /**
     * Call this method when an item view react to a long click event.
     * @param view view that was clicked
     * @param position adapter position of the item tha was clicked
     * @return whether the long click event was handled
     */
    public boolean onItemLongClick(View view, int position) {
        if (!mSelectEnabled) {
            mSelectEnabled = true;
            mCheckedItems = new SparseBooleanArray();
            mCheckedItems.put(position, true);
            mActionMode = mActivity.startSupportActionMode(this);
            onItemCheckedStateChanged(mActionMode, position, true);
        }
        return false;
    }

    /**
     * Return the number of item that are currently selected.
     * Note that this will always return zero if the selection mode is disabled.
     * @return the number of items currently selected
     */
    public int getCheckedItemCount() {
        return mCheckedCount;
    }

    /**
     * Return a set of checked items in the collection of displayed item.
     * This is only valid when the selection mode is started.
     * @return a SparseBooleanArray which where value is true for a given position
     * if the item is checked and false otherwise, or null if selection mode is disabled.
     */
    public SparseBooleanArray getCheckedItemPositions() {
        return mCheckedItems;
    }

    /**
     * Called when an item is checked or unchecked during selection mode.
     * You can use this method to update display depending on the number of checked items.
     * @param mode the actionmode providing this selection
     * @param position adapter position of the item that is now checked or unchecked
     * @param checked true if the item is now checked, false if the item is now unchecked
     */
    public abstract void onItemCheckedStateChanged(ActionMode mode, int position, boolean checked);
}
