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

package fr.nihilus.music

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.base.BaseActivity
import fr.nihilus.music.databinding.ActivityHomeBinding
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.nowplaying.NowPlayingFragment
import fr.nihilus.music.service.MusicService
import timber.log.Timber

@AndroidEntryPoint
class HomeActivity : BaseActivity() {
    private val viewModel: MusicLibraryViewModel by viewModels()

    private lateinit var binding: ActivityHomeBinding
    private lateinit var bottomSheet: BottomSheetBehavior<*>
    private lateinit var playerFragment: NowPlayingFragment

    private val sheetCollapsingCallback = BottomSheetCollapsingCallback()

    private val requestReadPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted ->
        // Whether it has permission or not, load fragment into interface
        handleIntent(intent)

        // Show an informative dialog message if permission is not granted
        // and user has not checked "Don't ask again".
        if (!permissionGranted &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )) {
            ConfirmDialogFragment.open(
                this,
                "storage_permission_rationale",
                message = getString(R.string.external_storage_permission_rationale),
                positiveButton = fr.nihilus.music.core.ui.R.string.core_ok
            )
        }
    }

    init {
        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            if (fragment is NowPlayingFragment) {
                playerFragment = fragment
                fragment.setOnRequestPlayerExpansionListener { shouldCollapse ->
                    bottomSheet.state =
                        if (shouldCollapse) BottomSheetBehavior.STATE_COLLAPSED
                        else BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPlayerView()
        requestReadPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    override fun onResume() {
        super.onResume()
        bottomSheet.addBottomSheetCallback(sheetCollapsingCallback)
    }

    override fun onPause() {
        bottomSheet.removeBottomSheetCallback(sheetCollapsingCallback)
        super.onPause()
    }

    private fun setupPlayerView() {
        bottomSheet = BottomSheetBehavior.from(binding.playerContainer)

        // Show / hide BottomSheet on startup without an animation
        setInitialBottomSheetVisibility(viewModel.playerSheetVisible.value == true)
        viewModel.playerSheetVisible.observe(this, this::onSheetVisibilityChanged)

        viewModel.playerError.observe(this) { playerErrorEvent ->
            playerErrorEvent.handle { errorMessage ->
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Called when the back button is pressed.
     * This will close the navigation drawer if open, collapse the player view if expanded,
     * or otherwise follow the default behavior (pop fragment back stack or finish activity).
     */
    override fun onBackPressed() {
        if (bottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Called when receiving an intent while the Activity is alive.
     * This is intended to handle actions relative to launcher shortcuts (API25+).
     *
     * @param intent the new intent that was started for the activity
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Show or hide the player view depending on the passed playback state.
     * This method is meant to be called only once to show or hide player view without animation.
     */
    private fun setInitialBottomSheetVisibility(shouldShow: Boolean) {
        bottomSheet.peekHeight = if (shouldShow) {
            resources.getDimensionPixelSize(R.dimen.playerview_height)
        } else {
            resources.getDimensionPixelSize(R.dimen.playerview_hidden_height)
        }
    }

    private fun onSheetVisibilityChanged(sheetVisible: Boolean) = when {
        sheetVisible -> {
            val collapsedSheetHeight = resources.getDimensionPixelSize(R.dimen.playerview_height)
            bottomSheet.setPeekHeight(collapsedSheetHeight, true)
        }

        bottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED -> {
            val hiddenSheetHeight = resources.getDimensionPixelSize(R.dimen.playerview_hidden_height)
            bottomSheet.setPeekHeight(hiddenSheetHeight, true)
        }

        else -> {
            val hiddenSheetHeight = resources.getDimensionPixelSize(R.dimen.playerview_hidden_height)
            bottomSheet.setPeekHeight(hiddenSheetHeight, false)
            bottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    /**
     * Perform an action depending on the received intent.
     * This is intended to handle actions relative to launcher shortcuts (API 25+).
     *
     * @param intent the intent that started this activity, or was received later
     * @return true if intent was handled, false otherwise
     */
    private fun handleIntent(intent: Intent?) {
        Timber.d("Received intent: %s", intent)
        when (intent?.action) {
            MusicService.ACTION_PLAYER_UI -> {
                binding.playerContainer.postDelayed({
                    bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                }, 300L)
            }
        }
    }

    /**
     * Toggle the visibility of elements in [NowPlayingFragment]
     * depending on the collapsing state of the bottom sheet.
     */
    private inner class BottomSheetCollapsingCallback : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_SETTLING ||
                newState == BottomSheetBehavior.STATE_DRAGGING) {
                return
            }

            val isExpandedOrExpanding = newState != BottomSheetBehavior.STATE_COLLAPSED
                    && newState != BottomSheetBehavior.STATE_HIDDEN
            playerFragment.setCollapsed(!isExpandedOrExpanding)
        }
    }
}
