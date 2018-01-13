/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.glide

import android.graphics.drawable.Drawable
import android.widget.ImageSwitcher
import com.bumptech.glide.request.target.ViewTarget
import com.bumptech.glide.request.transition.Transition

class SwitcherTarget(switcher: ImageSwitcher) : ViewTarget<ImageSwitcher, Drawable>(switcher) {

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        view.setImageDrawable(resource)
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        view.setImageDrawable(errorDrawable)
    }
}