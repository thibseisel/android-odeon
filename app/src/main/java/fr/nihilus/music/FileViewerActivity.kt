/*
 * Copyright 2017 Thibault Seisel
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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.ViewModelFactory
import fr.nihilus.music.view.PlayPauseButton
import javax.inject.Inject

class FileViewerActivity : AppCompatActivity() {

    @Inject lateinit var mFactory: ViewModelFactory
    lateinit var mViewModel: BrowserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_viewer)

        mViewModel = ViewModelProviders.of(this, mFactory).get(BrowserViewModel::class.java)
        mViewModel.connect()

        with(intent) {
            check(action == Intent.ACTION_VIEW) {
                "This activity should only be started by an Intent with action ACTION_VIEW."
            }

            mViewModel.post { controller ->
                controller.transportControls.playFromUri(data, null)
            }
        }

        val albumArt = findViewById<ImageView>(R.id.cover)
        val titleView = findViewById<TextView>(R.id.title)
        val subtitleView = findViewById<TextView>(R.id.subtitle)
        val playPauseButton = findViewById<PlayPauseButton>(R.id.btn_play_pause)
        val seekBar = findViewById<SeekBar>(R.id.progress)

        mViewModel.currentMetadata.observe(this, Observer { metadata ->
            if (metadata != null) {
                albumArt.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
                titleView.text = metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE)
                subtitleView.text = metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST)
                seekBar.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
            } else {
                albumArt.setImageBitmap(null)
                titleView.text = null
                subtitleView.text = null
                seekBar.max = 0
            }
        })

        mViewModel.playbackState.observe(this, Observer { state ->
            if (state != null && state.state < 4) {
                seekBar.progress = state.position.toInt()
                playPauseButton.isPlaying = state.state == PlaybackStateCompat.STATE_PLAYING
            }
        })

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mViewModel.post { controller ->
                    controller.transportControls.seekTo(seekBar.progress.toLong())
                }
            }

        })
    }
}
