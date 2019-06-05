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

package fr.nihilus.music.library.albums

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseActivity

class AlbumDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_detail)

        postponeEnterTransition()

        if (savedInstanceState == null) {
            val pickedAlbum = intent?.getParcelableExtra<MediaItem>(ARG_PICKED_ALBUM)
                ?: error("Missing required intent extra: $ARG_PICKED_ALBUM")

            val albumPalette = intent?.getParcelableExtra<AlbumPalette>(ARG_PALETTE)
                ?: error("Missing required intent extra: $ARG_PALETTE")

            val albumFragment = AlbumDetailFragment.newInstance(pickedAlbum, albumPalette)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_host, albumFragment)
                .commit()
        }
    }

    companion object {
        const val ARG_PALETTE = "palette"
        const val ARG_PICKED_ALBUM = "pickedAlbum"
    }
}
