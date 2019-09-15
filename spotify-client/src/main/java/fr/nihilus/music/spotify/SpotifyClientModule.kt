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

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.Reusable
import fr.nihilus.music.spotify.service.SpotifyAccountsService
import fr.nihilus.music.spotify.service.SpotifyAuthentication
import fr.nihilus.music.spotify.service.SpotifyErrorInterceptor
import fr.nihilus.music.spotify.service.SpotifyService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provides declarations used internally by the Spotify Client feature
 * such as REST web services, the HTTP client and JSON (de)serialization.
 *
 * This module requires the following dependencies:
 * - [String] named `SPOTIFY_AUTH_BASE`: the base URL of the Spotify Authorization service.
 * - [String] named `SPOTIFY_API_BASE`: the base URL of the Spotify API.
 * - [String] named `SPOTIFY_CLIENT_KEY`: the Spotify client key registered for this application.
 * - [String] named `SPOTIFY_CLIENT_SECRET`: the Spotify secret key for this application.
 */
@Module
internal object SpotifyClientModule {

    @JvmStatic
    @Provides @Singleton
    fun providesBaseOkHttp(): OkHttpClient = OkHttpClient()

    @JvmStatic
    @Provides @Singleton
    fun providesSpotifyAuthService(
        baseClient: OkHttpClient,
        moshi: Moshi,
        @Named("SPOTIFY_AUTH_BASE") authBaseUrl: String
    ): SpotifyAccountsService {
        val restAdapter = Retrofit.Builder()
            .client(baseClient)
            .baseUrl(authBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .validateEagerly(BuildConfig.DEBUG)
            .build()
        return restAdapter.create()
    }

    @JvmStatic
    @Provides @Singleton
    fun providesSpotifyApiService(
        baseClient: OkHttpClient,
        authService: SpotifyAccountsService,
        @Named("SPOTIFY_API_BASE") apiBaseUrl: String,
        @Named("SPOTIFY_CLIENT_KEY") clientKey: String,
        @Named("SPOTIFY_CLIENT_SECRET") clientSecret: String,
        moshi: Moshi
    ): SpotifyService {
        val modifiedClient = baseClient.newBuilder()
            .addInterceptor(SpotifyErrorInterceptor(moshi))
            .addInterceptor(
                SpotifyAuthentication(
                    authService,
                    clientKey,
                    clientSecret
                )
            )
            .build()

        val restAdapter = Retrofit.Builder()
            .client(modifiedClient)
            .baseUrl(apiBaseUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .validateEagerly(BuildConfig.DEBUG)
            .build()

        return restAdapter.create()
    }

    @JvmStatic
    @Provides @Reusable
    fun providesMoshi(): Moshi = Moshi.Builder()
        // Additional custom converters should be placed before the Kotlin JSON Adapter.
        .build()
}