/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.devmenu

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.observe
import fr.nihilus.music.core.ui.base.BaseActivity
import fr.nihilus.music.devmenu.features.ComposerViewModel
import fr.nihilus.music.devmenu.features.MixComposerFragment

class SpotifyDebugActivity : BaseActivity() {
    private val viewModel by viewModels<ComposerViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spotify_debug)

        viewModel.events.observe(this) { event ->
            event.handle { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, MixComposerFragment())
                .commitNow()
        }
    }
}