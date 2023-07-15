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

package fr.nihilus.music.spotify

import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Named

/**
 * Module for the Spotify Client feature meant to be used in production.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SpotifyApiModule {

    @Provides @Reusable
    fun providesOkHttpEngine(): HttpClientEngine = OkHttp.create()

    @Provides @Named("SPOTIFY_CLIENT_SECRET")
    fun providesClientSecret() = BuildConfig.SPOTIFY_CLIENT_SECRET
}