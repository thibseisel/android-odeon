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

package fr.nihilus.music.spotify

import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.nihilus.music.spotify.service.SpotifyAccountsService
import fr.nihilus.music.spotify.service.SpotifyAccountsServiceImpl
import fr.nihilus.music.spotify.service.SpotifyService
import fr.nihilus.music.spotify.service.SpotifyServiceImpl
import io.ktor.client.engine.HttpClientEngine
import javax.inject.Named

/**
 * Provides declarations used internally by the Spotify Client feature
 * such as REST web services, the HTTP client and JSON (de)serialization.
 *
 * This module requires the following dependencies:
 * - [HttpClientEngine]: the engine that should execute HTTP requests,
 * - a [String] named `SPOTIFY_CLIENT_SECRET`: the Spotify secret key for this application.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SpotifyClientModule {

    @Binds
    abstract fun bindsSpotifyAccountsService(impl: SpotifyAccountsServiceImpl): SpotifyAccountsService

    @Binds
    abstract fun bindsSpotifyApiService(impl: SpotifyServiceImpl): SpotifyService

    companion object {

        @Provides @Named("SPOTIFY_CLIENT_KEY")
        fun providesClientKey() = BuildConfig.SPOTIFY_CLIENT_ID

        @Provides @Named("APP_USER_AGENT")
        fun providesUserAgent(
            @Named("APP_VERSION_NAME") versionName: String
        ): String = "Odeon/$versionName OkHttp"

        @Provides @Reusable
        fun providesMoshi(): Moshi = Moshi.Builder()
            // Additional custom converters should be placed before the Kotlin JSON Adapter.
            .build()
    }

}
