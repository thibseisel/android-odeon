/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.core.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.core.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.playback.RepeatMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import javax.inject.Provider
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class SharedPreferencesSettingsTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @BeforeTest
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("test", Context.MODE_PRIVATE)
    }

    @Test
    fun `When reading queueIdentifier, then return the saved value or 0`() {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.queueIdentifier shouldBe 0L

        prefs.edit().putLong(PREF_KEY_QUEUE_IDENTIFIER, 42).commit()

        settings.queueIdentifier shouldBe 42L
    }

    @Test
    fun `When reading lastQueueMediaId, then return the latest written value or null`() {
        val allTracks = MediaId(TYPE_TRACKS, CATEGORY_ALL)

        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.lastQueueMediaId shouldBe null

        prefs.edit().putString(PREF_KEY_LAST_PLAYED, allTracks.encoded).commit()
        settings.lastQueueMediaId shouldBe allTracks
    }

    @Test
    fun `When updating lastQueueMediaId, then also increment queueIdentifier`() {
        val allTracks = MediaId(TYPE_TRACKS, CATEGORY_ALL)

        prefs.edit().putLong(PREF_KEY_QUEUE_IDENTIFIER, 12L).commit()

        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.lastQueueMediaId = allTracks

        prefs.getString(PREF_KEY_LAST_PLAYED, null) shouldBe allTracks.encoded
        prefs.getLong(PREF_KEY_QUEUE_IDENTIFIER, 0L) shouldBe 13L
    }

    @Test
    fun `When reading lastQueueIndex, then return the saved value or 0`() {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.lastQueueIndex shouldBe 0

        prefs.edit().putInt(PREF_KEY_QUEUE_INDEX, 42).commit()

        settings.lastQueueIndex shouldBe 42
    }

    @Test
    fun `When setting lastQueueIndex, then update the saved value`() {
        prefs.edit().putInt(PREF_KEY_QUEUE_INDEX, 34).commit()

        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.lastQueueIndex = 12

        prefs.getInt(PREF_KEY_QUEUE_INDEX, 0) shouldBe 12
    }

    @Test
    fun `When reading lastPlayedPosition, then return the saved value or -1`() {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.lastPlayedPosition shouldBe -1L

        prefs.edit().putLong(PREF_KEY_QUEUE_POSITION, 12345L).commit()

        settings.lastPlayedPosition shouldBe 12345L
    }

    @Test
    fun `When setting lastPlayedPosition, then update the saved value`() {
        prefs.edit().putLong(PREF_KEY_QUEUE_POSITION, Long.MAX_VALUE).commit()

        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.lastPlayedPosition = 12345L

        prefs.getLong(PREF_KEY_QUEUE_POSITION, Long.MIN_VALUE) shouldBe 12345L
    }

    @Test
    fun `When reading shuffleModeEnabled, then return the saved value or false`() {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.shuffleModeEnabled shouldBe false

        prefs.edit().putBoolean(PREF_KEY_SHUFFLE_MODE_ENABLED, true).commit()

        settings.shuffleModeEnabled shouldBe true
    }

    @Test
    fun `When setting shuffleModeEnabled, then update the saved value`() {
        prefs.edit().putBoolean(PREF_KEY_SHUFFLE_MODE_ENABLED, true).commit()

        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.shuffleModeEnabled = false

        prefs.getBoolean(PREF_KEY_SHUFFLE_MODE_ENABLED, true) shouldBe false
    }

    @Test
    fun `When reading queueReload, then return the saved value as an enum or FROM_TRACK`() {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.queueReload shouldBe QueueReloadStrategy.FROM_TRACK

        fun enumFor(@StringRes prefValueResId: Int): QueueReloadStrategy {
            prefs.edit().putString(
                context.getString(R.string.pref_key_reload_queue),
                context.getString(prefValueResId)
            ).commit()
            return settings.queueReload
        }

        enumFor(R.string.pref_reload_queue_start_value) shouldBe QueueReloadStrategy.FROM_START
        enumFor(R.string.pref_reload_queue_track_value) shouldBe QueueReloadStrategy.FROM_TRACK
        enumFor(R.string.pref_reload_queue_position_value) shouldBe QueueReloadStrategy.AT_POSITION
    }

    @Test
    fun `When reading prepareQueueOnStartup, then return saved value or true`() {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.prepareQueueOnStartup shouldBe true

        val prefKey = context.getString(R.string.pref_key_prepare_on_startup)
        prefs.edit().putBoolean(prefKey, false).commit()
        settings.prepareQueueOnStartup shouldBe false

        prefs.edit().putBoolean(prefKey, true).commit()
        settings.prepareQueueOnStartup shouldBe true
    }

    @Test
    fun `When reading repeatMode, then return the saved value as an enum or DISABLED`() {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.repeatMode shouldBe RepeatMode.DISABLED

        prefs.edit().putInt(PREF_KEY_REPEAT_MODE, RepeatMode.ONE.code).commit()
        settings.repeatMode shouldBe RepeatMode.ONE

        prefs.edit().putInt(PREF_KEY_REPEAT_MODE, RepeatMode.ALL.code).commit()
        settings.repeatMode shouldBe RepeatMode.ALL

        prefs.edit().putInt(PREF_KEY_REPEAT_MODE, RepeatMode.DISABLED.code).commit()
        settings.repeatMode shouldBe RepeatMode.DISABLED
    }

    @Test
    fun `When setting repeatMode, then update prefs with the corresponding code`() {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))

        settings.repeatMode = RepeatMode.ONE
        prefs.getInt(PREF_KEY_REPEAT_MODE, -1) shouldBe RepeatMode.ONE.code

        settings.repeatMode = RepeatMode.ALL
        prefs.getInt(PREF_KEY_REPEAT_MODE, -1) shouldBe RepeatMode.ALL.code

        settings.repeatMode = RepeatMode.DISABLED
        prefs.getInt(PREF_KEY_REPEAT_MODE, -1) shouldBe RepeatMode.DISABLED.code
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `When running API 28 or earlier, then the default theme should be BATTERY_SAVER`() = runBlockingTest {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.currentTheme.first() shouldBe Settings.AppTheme.BATTERY_SAVER_ONLY
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    fun `When running API 29+, then the default theme should be SYSTEM`() = runBlockingTest {
        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        settings.currentTheme.first() shouldBe Settings.AppTheme.SYSTEM
    }

    @Test
    fun `When observing currentTheme then emit the latest saved value`() = runBlockingTest {
        val themePrefKey = context.getString(R.string.pref_key_theme)
        prefs.edit().putString(themePrefKey, "light").commit()

        val settings = SharedPreferencesSettings(context, providerOf(prefs))
        val updates = settings.currentTheme.produceIn(this)
        updates.receive() shouldBe Settings.AppTheme.LIGHT

        prefs.edit().putString(themePrefKey, "dark").commit()
        updates.receive() shouldBe Settings.AppTheme.DARK

        prefs.edit().putString(themePrefKey, "battery").commit()
        updates.receive() shouldBe Settings.AppTheme.BATTERY_SAVER_ONLY

        prefs.edit().putString(themePrefKey, "system").commit()
        updates.receive() shouldBe Settings.AppTheme.SYSTEM

        updates.cancel()
    }

    private fun <T> providerOf(value: T) = Provider { value }
}