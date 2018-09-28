/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.media

import org.mockito.exceptions.base.MockitoException
import org.mockito.stubbing.Answer

/**
 * Defines [Answer]s that can be used to stub getter and setter of the property of a mocked object.
 */
class PropertyStub<T>(initialValue: T) {
    private var value: T = initialValue

    /**
     * The answer to use when stubbing the property's getter.
     * Returns the saved value.
     */
    val getValue: Answer<T> = Answer {
        value
    }

    /**
     * The answer to use when stubbing the property's setter.
     * Saves the value of the first method argument.
     *
     * This should only be applied when stubbing a method that has exactly 1 parameter of type [T].
     */
    val setValue: Answer<Unit> = Answer {
        if (it.arguments.size != 1) {
           throw MockitoException("Usage of PropertyStub getValue requires the stubbed method to have exactly 1 argument.")
        }

        value = it.getArgument(0)
    }
}