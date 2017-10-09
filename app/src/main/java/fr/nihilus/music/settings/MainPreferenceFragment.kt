package fr.nihilus.music.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceFragmentCompat
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import javax.inject.Inject

class MainPreferenceFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject lateinit var mPrefs: PreferenceDao
    private lateinit var mKeyNightMode: String

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_main)
        mKeyNightMode = context.getString(R.string.pref_night_mode)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        if (mKeyNightMode == key) {
            onNightModeChanged(mPrefs.nightMode)
        }
    }

    private fun onNightModeChanged(@AppCompatDelegate.NightMode newMode: Int) {
        AppCompatDelegate.setDefaultNightMode(newMode)
        (activity as AppCompatActivity).delegate.applyDayNight()

        val intent = Intent()
        intent.putExtra("night_mode", newMode)

        activity.setResult(Activity.RESULT_OK, intent)
    }
}
