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

package fr.nihilus.music.spotify.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import io.kotlintest.matchers.types.shouldBeInstanceOf
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.intellij.lang.annotations.Language
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.Test

private const val TEST_CLIENT_ID = "client_id"
private const val TEST_CLIENT_SECRET = "client_secret"
private const val CLIENT_BASE64_KEY = "Y2xpZW50X2lkOmNsaWVudF9zZWNyZXQ="
private const val TEST_USER_AGENT = "SpotifyAccountsService/1.0.0 KtorHttpClient/1.2.4"

@Language("JSON")
private val AUTH_TOKEN = """{
    "access_token": "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
    "token_type": "Bearer",
    "expires_in": 3600
}""".trimIndent()

/**
 * Checks the behavior of the Spotify Accounts API client.
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class SpotifyAccountsServiceTest {

    private val moshi = Moshi.Builder().build()

    private fun accountsService(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): SpotifyAccountsService {
        val simulatedServer = MockEngine(handler)
        return SpotifyAccountsServiceImpl(
            simulatedServer,
            moshi,
            TEST_USER_AGENT
        )
    }

    @Test
    fun `Given bad credentials, when authenticating then fail with AuthenticationException`() = runBlockingTest {
        val failingAuthService = accountsService {
            respondJson(
                """{
                "error": "invalid_client",
                "error_description": "Invalid client"
            }""".trimIndent(), HttpStatusCode.BadRequest
            )
        }

        val exception = shouldThrow<AuthenticationException> {
            failingAuthService.authenticate(TEST_CLIENT_ID, "wrong_client_secret")
        }

        exception.error shouldBe "invalid_client"
        exception.description shouldBe "Invalid client"
    }

    @Test
    fun `Given valid credentials, when authenticating then POST them to Accounts service as Base64`() = runBlockingTest {
        val authService = accountsService { request ->
            request.method shouldBe HttpMethod.Post
            request.url.host shouldBe "accounts.spotify.com"
            request.url.encodedPath shouldBe "api/token"
            request.headers[HttpHeaders.Authorization] shouldBe "Basic $CLIENT_BASE64_KEY"

            request.body.shouldBeInstanceOf<FormDataContent> {
                it.formData["grant_type"] shouldBe "client_credentials"
            }

            respondJson(AUTH_TOKEN)
        }

        val token = authService.authenticate(
            TEST_CLIENT_ID,
            TEST_CLIENT_SECRET
        )
        token.token shouldBe TEST_TOKEN_STRING
        token.type shouldBe "Bearer"
        token.expiresIn shouldBe 3600
    }

    @Test
    fun `When authenticating then perform the request with the specified User Agent`() = runBlockingTest {
        val authService = accountsService { request ->
            request.headers[HttpHeaders.UserAgent] shouldBe TEST_USER_AGENT
            respondJson(AUTH_TOKEN)
        }

        authService.authenticate(
            TEST_CLIENT_ID,
            TEST_CLIENT_SECRET
        )
    }
}