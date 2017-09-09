package fr.nihilus.music.settings

import android.app.Activity
import android.content.Context
import android.content.DialogInterface.OnClickListener
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceFragmentCompat
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.R
import fr.nihilus.music.utils.PermissionUtil
import fr.nihilus.music.utils.SimpleDialog
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
            val nightModeConfig = mPrefs.nightMode
            AppCompatDelegate.setDefaultNightMode(nightModeConfig)
            (activity as AppCompatActivity).delegate.applyDayNight()
            activity.setResult(Activity.RESULT_OK)
            onNightModeChanged(nightModeConfig)
        }
    }

    private fun onNightModeChanged(@AppCompatDelegate.NightMode newMode: Int) {
        if (newMode == AppCompatDelegate.MODE_NIGHT_AUTO) {
            // Show a dialog to request the permission
            if (!PermissionUtil.hasLocationPermission(context)) {
                SimpleDialog.newInstance(R.string.location_rationnale_title,
                        R.string.location_rationnale_message)
                        .setPositiveAction(R.string.ok, OnClickListener { _, _ ->
                            PermissionUtil.requestLocationPermission(activity)
                        })
                        .setNegativeAction(R.string.no_thanks, OnClickListener { _, _ -> })
                        .show(fragmentManager, null)
            }
        }
    }
}
