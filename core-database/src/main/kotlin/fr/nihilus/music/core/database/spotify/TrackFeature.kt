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

package fr.nihilus.music.core.database.spotify

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Audio features of a specific track.
 */
@Entity(tableName = "track_feature")
data class TrackFeature(

    /**
     * The unique identifier of the analyzed track.
     */
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = false)
    val id: String,

    /**
     * The estimated overall key of the track.
     * The value is `null` if the key is unknown.
     */
    @ColumnInfo(name = "key")
    val key: Pitch?,

    /**
     * Mode indicates the modality (major or minor) of a track,
     * the type of scale from which its melodic content is derived.
     */
    @ColumnInfo(name = "mode")
    val mode: MusicalMode,

    /**
     * The overall estimated tempo of a track in beats per minute (BPM).
     * In musical terminology, tempo is the speed or pace of a given piece
     * and derives directly from the average beat duration.
     */
    @ColumnInfo(name = "tempo")
    val tempo: Float,

    /**
     * An estimated overall time signature of a track.
     * The time signature (meter) is a notational convention to specify how many beats are in each bar (or measure).
     */
    @ColumnInfo(name = "time_signature")
    val signature: Int,

    /**
     * The overall loudness of a track in decibels (dB).
     * Loudness values are averaged across the entire track and are useful for comparing relative loudness of tracks.
     * Loudness is the quality of a sound that is the primary psychological correlate of physical strength (amplitude).
     * Values typical range between `-60` and `0` dB.
     */
    @ColumnInfo(name = "loudness")
    val loudness: Float,

    /**
     * A confidence measure from `0.0` to `1.0` of whether the track is acoustic.
     * `1.0` represents high confidence the track is acoustic.
     */
    @ColumnInfo(name = "acousticness")
    val acousticness: Float,

    /**
     * Danceability describes how suitable a track is for dancing based on a combination of musical elements
     * including tempo, rhythm stability, beat strength, and overall regularity.
     * A value of `0.0` is least danceable and `1.0` is most danceable.
     */
    @ColumnInfo(name = "danceability")
    val danceability: Float,

    /**
     * Energy is a measure from `0.0` to `1.0` and represents a perceptual measure of intensity and activity.
     * Typically, energetic tracks feel fast, loud, and noisy.
     *
     * For example, death metal has high energy, while a Bach prelude scores low on the scale.
     * Perceptual features contributing to this attribute include dynamic range, perceived loudness, timbre, onset rate,
     * and general entropy.
     */
    @ColumnInfo(name = "energy")
    val energy: Float,

    /**
     * Predicts whether a track contains no vocals.
     * “Ooh” and “aah” sounds are treated as instrumental in this context.
     * Rap or spoken word tracks are clearly “vocal”.
     * The closer the instrumentalness value is to `1.0`, the greater likelihood the track contains no vocal content. Values above 0.5 are intended to represent instrumental tracks,
     * but confidence is higher as the value approaches `1.0`.
     */
    @ColumnInfo(name = "instrumentalness")
    val instrumentalness: Float,

    /**
     * Detects the presence of an audience in the recording.
     * Higher liveness values represent an increased probability that the track was performed live.
     * A value above `0.8` provides strong likelihood that the track is live.
     */
    @ColumnInfo(name = "liveness")
    val liveness: Float,

    /**
     * Speechiness detects the presence of spoken words in a track.
     * The more exclusively speech-like the recording (e.g. talk show, audio book, poetry),
     * the closer to `1.0` the attribute value.
     * Values above `0.66` describe tracks that are probably made entirely of spoken words.
     * Values between `0.33` and `0.66` describe tracks that may contain both music and speech,
     * either in sections or layered, including such cases as rap music.
     * Values below `0.33` most likely represent music and other non-speech-like tracks.
     */
    @ColumnInfo(name = "speechiness")
    val speechiness: Float,

    /**
     * A measure from `0.0` to `1.0` describing the musical positiveness conveyed by a track.
     * Tracks with high valence sound more positive (e.g. happy, cheerful, euphoric),
     * while tracks with low valence sound more negative (e.g. sad, depressed, angry).
     */
    @ColumnInfo(name = "valence")
    val valence: Float
)

/**
 * Audio features associated with a track stored locally on the device.
 */
class LocalizedTrackFeature(

    /**
     * The unique identifier of a track stored on the device's storage.
     */
    @ColumnInfo(name = "local_id")
    val trackId: Long,

    /**
     * The audio features that have been linked to that local track.
     */
    @Embedded
    val features: TrackFeature
) {
    operator fun component1() = trackId
    operator fun component2() = features
}

/**
 * Enumeration of musical modes.
 */
enum class MusicalMode {
    MINOR, MAJOR;
}

/**
 * Values map to pitches using standard Pitch Class notation.
 * E.g. 0 = C, 1 = C♯/D♭, 2 = D, and so on.
 *
 * For enharmonic tones such as C♯/D♭ or G♯/A♭, the name of the associated constant
 * is chosen from the tone with the simplest key signature.
 * For example, C♯ has 7♯ while D♭ has 5♭, therefore we pick D♭ (D FLAT) as the constant name.
 */
enum class Pitch {
    C, D_FLAT, D, E_FLAT, E, F, F_SHARP, G, A_FLAT, A, B_FLAT, B;
}

/**
 * Parse the raw code for the `mode` property from the Spotify AudioFeature object.
 */
fun decodeMusicalMode(mode: Int): MusicalMode = when (mode) {
    0 -> MusicalMode.MINOR
    1 -> MusicalMode.MAJOR
    else -> error("Invalid encoded value for MusicalMode: $mode")
}

/**
 * Parse the raw code for the `key` property from the Spotify AudioFeature object.
 */
fun decodePitch(key: Int?): Pitch? = when (key) {
    0 -> Pitch.C
    1 -> Pitch.D_FLAT
    2 -> Pitch.D
    3 -> Pitch.E_FLAT
    4 -> Pitch.E
    5 -> Pitch.F
    6 -> Pitch.F_SHARP
    7 -> Pitch.G
    8 -> Pitch.A_FLAT
    9 -> Pitch.A
    10 -> Pitch.B_FLAT
    11 -> Pitch.B
    else -> null
}
