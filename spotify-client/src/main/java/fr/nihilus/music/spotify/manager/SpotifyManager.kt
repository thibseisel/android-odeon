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

package fr.nihilus.music.spotify.manager

import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.spotify.manager.FeatureFilter

/**
 * Main entry point for tagging and classifying media stored on the device.
 *
 * In order to provide audio classification capabilities even when device is offline,
 * this manager maintains a local cache of audio features that should periodically be refreshed by
 * [syncing it with the Spotify REST API][sync].
 */
interface SpotifyManager {

    /**
     * Among all tracks stored locally on the device, list those whose audio features
     * match a given set of [filters].
     *
     * Tracks that have not been linked with a track on the Spotify API are not listed
     * due to having no audio features.
     *
     * @param filters Set of constraints on the value of track features.
     * Having no filters will return all tracks (except unlinked ones).
     * @return Tracks whose audio features match all provided filters.
     */
    suspend fun findTracksHavingFeatures(filters: List<FeatureFilter>): List<Track>

    /**
     * Fetch media metadata from the Spotify API and store them locally for offline use.
     *
     * The synchronization performs the following steps:
     * 1. Identify tracks that have not been synced yet.
     * 2. For each track, search for that track on the Spotify API.
     * If it is found, that track is considered linked to its remote counterpart.
     * 3. Download audio features for each newly linked track and save them locally.
     */
    suspend fun sync()
}