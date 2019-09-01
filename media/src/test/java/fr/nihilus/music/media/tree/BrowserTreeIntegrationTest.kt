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

package fr.nihilus.music.media.tree

import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.assertOn
import fr.nihilus.music.media.database.AppDatabase
import fr.nihilus.music.media.provider.MediaStoreSurrogate
import io.kotlintest.inspectors.forAll
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.should
import io.kotlintest.shouldBe
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validate the behavior of the whole "media" module, using [BrowserTree] as its facade.
 * Those tests instantiate [BrowserTree] and all its dependencies using [BrowserTreeComponent].
 */
@RunWith(AndroidJUnit4::class)
class BrowserTreeIntegrationTest {

    private lateinit var component: BrowserTreeComponent
    private lateinit var browserTree: BrowserTree

    private val fakeMediaStore: MediaStoreSurrogate get() = component.inMemoryMediaStore
    private val database: AppDatabase get() = component.database
    private val scope: TestCoroutineScope get() = component.scope

    @Before
    fun setUp() {
        component = DaggerBrowserTreeComponent.builder().build()
        browserTree = component.createBrowserTree()
    }

    @After
    fun cleanup() {
        scope.cleanupTestCoroutines()
        fakeMediaStore.release()
        database.close()
    }

    @Test
    fun `When requesting all tracks then retrieve them as items from MediaStore`() = run {
        val children = browserTree.getChildren(MediaId(TYPE_TRACKS, CATEGORY_ALL), null)
        children.shouldNotBeNull()

        // Check that all tracks from the MediaStore are present, and in alphabetical order.
        children shouldHaveSize 10
        children.map { it.mediaId }.shouldContainExactly(
            "$TYPE_TRACKS/$CATEGORY_ALL|161",
            "$TYPE_TRACKS/$CATEGORY_ALL|309",
            "$TYPE_TRACKS/$CATEGORY_ALL|481",
            "$TYPE_TRACKS/$CATEGORY_ALL|48",
            "$TYPE_TRACKS/$CATEGORY_ALL|125",
            "$TYPE_TRACKS/$CATEGORY_ALL|294",
            "$TYPE_TRACKS/$CATEGORY_ALL|219",
            "$TYPE_TRACKS/$CATEGORY_ALL|75",
            "$TYPE_TRACKS/$CATEGORY_ALL|464",
            "$TYPE_TRACKS/$CATEGORY_ALL|477"
        )

        children.forAll {
            it.isPlayable shouldBe true
            it.isBrowsable shouldBe false
        }

        // Check that track metadata are correctly mapped as a MediaItem.
        children[4].should { jailbreak ->
            jailbreak.description.title shouldBe "Jailbreak"
            jailbreak.description.subtitle shouldBe "AC/DC"

            assertOn(jailbreak.description.extras) {
                longInt(MediaItems.EXTRA_DURATION).isEqualTo(276668L)
                integer(MediaItems.EXTRA_DISC_NUMBER).isEqualTo(2)
                integer(MediaItems.EXTRA_TRACK_NUMBER).isEqualTo(14)
            }
        }
    }

    private fun run(testBody: suspend TestCoroutineScope.() -> Unit) {
        scope.runBlockingTest(testBody)
    }
}