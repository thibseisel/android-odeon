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

package fr.nihilus.music.fileviewer

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseActivity
import fr.nihilus.music.ui.ProgressAutoUpdater
import fr.nihilus.music.view.PlayPauseButton

class FileViewerActivity : BaseActivity() {
    private lateinit var seekUpdater: ProgressAutoUpdater

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[FileViewerViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_viewer)

        with(intent) {
            check(action == Intent.ACTION_VIEW) {
                "This activity should only be started by an Intent with action ACTION_VIEW."
            }

            viewModel.playMediaFromUri(data)
        }

        val albumArt = findViewById<ImageView>(R.id.album_art_view)
        val titleView = findViewById<TextView>(R.id.title_view)
        val subtitleView = findViewById<TextView>(R.id.subtitle_view)
        val playPauseButton = findViewById<PlayPauseButton>(R.id.play_pause_button)
        val seekBar = findViewById<SeekBar>(R.id.progress)

        // Configure seekBar auto-updates
        seekUpdater = ProgressAutoUpdater(seekBar) { position ->
            viewModel.seekToPosition(position)
        }

        viewModel.nowPlaying.observe(this, Observer { metadata ->
            seekUpdater.setMetadata(metadata)
            if (metadata != null) {
                albumArt.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                titleView.text = metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE)
                subtitleView.text = metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST)
            } else {
                albumArt.setImageResource(R.drawable.ic_audiotrack_24dp)
                titleView.text = null
                subtitleView.text = null
            }
        })

        viewModel.playbackState.observe(this, Observer { state ->
            if (state != null && state.state < 4) {
                seekUpdater.setPlaybackState(state)
                playPauseButton.isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
            }
        })
    }
}
