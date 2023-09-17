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

package fr.nihilus.music.core.ui.base

import androidx.annotation.ContentView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

/**
 * Base Fragment class.
 * Makes it easier to add common behavior to all fragments.
 */
abstract class BaseFragment : Fragment {

    /** No-arg Fragment constructor. */
    constructor() : super()

    /**
     * Alternate constructor that can be used to provide a default layout that will be inflated by [onCreateView].
     * @param contentLayoutId identifier of a layout resource to be inflated as this fragment's view.
     */
    @ContentView
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)
}