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

package fr.nihilus.music.spotify.service

import fr.nihilus.music.core.test.fail
import io.kotlintest.matchers.numerics.shouldBeLessThanOrEqual
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData

internal fun dummySpotifyBackend(): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
    { request ->
        val ids = request.url.parameters["ids"]?.split(',') ?: emptyList()
        val jsonString = when (request.url.encodedPath) {
            "/v1/artists" -> {
                ids.size shouldBeLessThanOrEqual 50
                ids.joinToString(",", """{"artists": [""", "]}") { artistId ->
                    """{
                          "external_urls": {
                            "spotify": "https://open.spotify.com/artist/12Chz98pHFMPJEknJQMWvI"
                          },
                          "followers": {
                            "href": null,
                            "total": 4961259
                          },
                          "genres": [
                            "modern rock",
                            "permanent wave",
                            "piano rock",
                            "post-grunge",
                            "rock"
                          ],
                          "href": "https://api.spotify.com/v1/artists/12Chz98pHFMPJEknJQMWvI",
                          "id": "$artistId",
                          "images": [
                            {
                              "height": 320,
                              "url": "https://i.scdn.co/image/17f00ec7613d733f2dd88de8f2c1628ea5f9adde",
                              "width": 320
                            }
                          ],
                          "name": "Muse",
                          "popularity": 82,
                          "type": "artist",
                          "uri": "spotify:artist:12Chz98pHFMPJEknJQMWvI"
                        }""".trimIndent()
                }
            }

            "/v1/albums" -> {
                ids.size shouldBeLessThanOrEqual 20
                ids.joinToString(",", """{"albums": [""", "]}") { albumId ->
                    """{
                          "album_type": "album",
                          "artists": [
                            {
                              "external_urls": {
                                "spotify": "https://open.spotify.com/artist/7jy3rLJdDQY21OgRLCZ9sD"
                              },
                              "href": "https://api.spotify.com/v1/artists/7jy3rLJdDQY21OgRLCZ9sD",
                              "id": "$albumId",
                              "name": "Foo Fighters",
                              "type": "artist",
                              "uri": "spotify:artist:7jy3rLJdDQY21OgRLCZ9sD"
                            }
                          ],
                          "copyrights": [
                            {
                              "text": "(P) 2017 Roswell Records, Inc. under license to RCA Records, a unit of Sony Music Entertainment",
                              "type": "P"
                            }
                          ],
                          "external_ids": {
                            "upc": "886446547176"
                          },
                          "external_urls": {
                            "spotify": "https://open.spotify.com/album/6KMkuqIwKkwUhUYRPL6dUc"
                          },
                          "genres": [],
                          "href": "https://api.spotify.com/v1/albums/6KMkuqIwKkwUhUYRPL6dUc",
                          "id": "6KMkuqIwKkwUhUYRPL6dUc",
                          "images": [
                            {
                              "height": 300,
                              "url": "https://i.scdn.co/image/466a21e8c6f72e540392ae76a94e01c876a8f193",
                              "width": 300
                            }
                          ],
                          "label": "RCA Records Label",
                          "name": "Concrete and Gold",
                          "popularity": 68,
                          "release_date": "2017-09-15",
                          "release_date_precision": "day",
                          "total_tracks": 11,
                          "tracks": {
                            "href": "https://api.spotify.com/v1/albums/6KMkuqIwKkwUhUYRPL6dUc/tracks?offset=0&limit=50&market=FR",
                            "items": [
                              {
                                "artists": [
                                  {
                                    "external_urls": {
                                      "spotify": "https://open.spotify.com/artist/7jy3rLJdDQY21OgRLCZ9sD"
                                    },
                                    "href": "https://api.spotify.com/v1/artists/7jy3rLJdDQY21OgRLCZ9sD",
                                    "id": "7jy3rLJdDQY21OgRLCZ9sD",
                                    "name": "Foo Fighters",
                                    "type": "artist",
                                    "uri": "spotify:artist:7jy3rLJdDQY21OgRLCZ9sD"
                                  }
                                ],
                                "disc_number": 1,
                                "duration_ms": 320880,
                                "explicit": false,
                                "external_urls": {
                                  "spotify": "https://open.spotify.com/track/5lnsL7pCg0fQKcWnlkD1F0"
                                },
                                "href": "https://api.spotify.com/v1/tracks/5lnsL7pCg0fQKcWnlkD1F0",
                                "id": "5lnsL7pCg0fQKcWnlkD1F0",
                                "is_local": false,
                                "is_playable": true,
                                "name": "Dirty Water",
                                "preview_url": "https://p.scdn.co/mp3-preview/165855a9cf8021df6e4d8dbf51cb92c5939ea8cd?cid=774b29d4f13844c495f206cafdad9c86",
                                "track_number": 6,
                                "type": "track",
                                "uri": "spotify:track:5lnsL7pCg0fQKcWnlkD1F0"
                              }
                            ],
                            "limit": 50,
                            "next": null,
                            "offset": 0,
                            "previous": null,
                            "total": 1
                          },
                          "type": "album",
                          "uri": "spotify:album:6KMkuqIwKkwUhUYRPL6dUc"
                        }""".trimIndent()
                }
            }

            "/v1/tracks" -> {
                ids.size shouldBeLessThanOrEqual 50
                ids.joinToString(",", """{"tracks": [""", "]}") { trackId ->
                    """{
                      "album": {
                        "album_type": "album",
                        "artists": [
                          {
                            "external_urls": {
                              "spotify": "https://open.spotify.com/artist/12Chz98pHFMPJEknJQMWvI"
                            },
                            "href": "https://api.spotify.com/v1/artists/12Chz98pHFMPJEknJQMWvI",
                            "id": "12Chz98pHFMPJEknJQMWvI",
                            "name": "Muse",
                            "type": "artist",
                            "uri": "spotify:artist:12Chz98pHFMPJEknJQMWvI"
                          }
                        ],
                        "external_urls": {
                          "spotify": "https://open.spotify.com/album/5OZgDtx180ZZPMpm36J2zC"
                        },
                        "href": "https://api.spotify.com/v1/albums/5OZgDtx180ZZPMpm36J2zC",
                        "id": "5OZgDtx180ZZPMpm36J2zC",
                        "images": [
                          {
                            "height": 300,
                            "url": "https://i.scdn.co/image/0b2a261f7bec0ed109a149316d116c15ca72e5ef",
                            "width": 300
                          }
                        ],
                        "name": "Simulation Theory (Super Deluxe)",
                        "release_date": "2018-11-09",
                        "release_date_precision": "day",
                        "total_tracks": 21,
                        "type": "album",
                        "uri": "spotify:album:5OZgDtx180ZZPMpm36J2zC"
                      },
                      "artists": [
                        {
                          "external_urls": {
                            "spotify": "https://open.spotify.com/artist/12Chz98pHFMPJEknJQMWvI"
                          },
                          "href": "https://api.spotify.com/v1/artists/12Chz98pHFMPJEknJQMWvI",
                          "id": "12Chz98pHFMPJEknJQMWvI",
                          "name": "Muse",
                          "type": "artist",
                          "uri": "spotify:artist:12Chz98pHFMPJEknJQMWvI"
                        }
                      ],
                      "disc_number": 1,
                      "duration_ms": 245960,
                      "explicit": false,
                      "external_ids": {
                        "isrc": "GBAHT1800406"
                      },
                      "external_urls": {
                        "spotify": "https://open.spotify.com/track/$trackId"
                      },
                      "href": "https://api.spotify.com/v1/tracks/$trackId",
                      "id": "$trackId",
                      "is_local": false,
                      "is_playable": true,
                      "name": "Algorithm",
                      "popularity": 60,
                      "preview_url": "https://p.scdn.co/mp3-preview/00d386644c07673f05878e337f977911c0ba740b?cid=774b29d4f13844c495f206cafdad9c86",
                      "track_number": 1,
                      "type": "track",
                      "uri": "spotify:track:$trackId"
                    }""".trimIndent()
                }
            }

            "/v1/audio-features" -> {
                ids.size shouldBeLessThanOrEqual 100
                ids.joinToString(",", """{"audio_features": [""", "]}") { trackId ->
                    """{
                      "danceability": 0.522,
                      "energy": 0.923,
                      "key": 2,
                      "loudness": -4.56,
                      "mode": 1,
                      "speechiness": 0.0539,
                      "acousticness": 0.0125,
                      "instrumentalness": 0.017,
                      "liveness": 0.0854,
                      "valence": 0.595,
                      "tempo": 170.057,
                      "type": "audio_features",
                      "id": "$trackId",
                      "uri": "spotify:track:$trackId",
                      "track_href": "https://api.spotify.com/v1/tracks/7f0vVL3xi4i78Rv5Ptn2s1",
                      "analysis_url": "https://api.spotify.com/v1/audio-analysis/7f0vVL3xi4i78Rv5Ptn2s1",
                      "duration_ms": 245960,
                      "time_signature": 4
                    }""".trimIndent()
                }
            }

            else -> fail("Unexpected request on endpoint: ${request.url}")
        }

        respondJson(jsonString)
    }