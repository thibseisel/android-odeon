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

package fr.nihilus.music.ui.cleanup

import android.os.Bundle
import fr.nihilus.music.core.ui.base.BaseActivity
import fr.nihilus.music.core.ui.extensions.isDrawnEdgeToEdge
import fr.nihilus.music.ui.cleanup.databinding.ActivityCleanupBinding

/**
 * A simple activity that only displays the content of [CleanupFragment].
 * This is required to avoid displaying the player controls on the cleanup screen
 * where controlling playback becomes secondary.
 */
class CleanupActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.isDrawnEdgeToEdge = true

        val binding = ActivityCleanupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
    }
}
