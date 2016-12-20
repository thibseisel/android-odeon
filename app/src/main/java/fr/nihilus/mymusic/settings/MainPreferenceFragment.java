package fr.nihilus.mymusic.settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.PreferenceFragmentCompat;

import fr.nihilus.mymusic.R;

/**
 * Created by Thib on 20/12/2016.
 */
public class MainPreferenceFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private String mKeyNightMode;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.prefs_main);
        mKeyNightMode = getString(R.string.pref_night_mode);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (mKeyNightMode.equals(key)) {
            int newMode = Prefs.getNightMode(getContext());
            AppCompatDelegate.setDefaultNightMode(newMode);
            getActivity().setResult(Activity.RESULT_OK);
        }
    }
}
