package fr.nihilus.music.glide

import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.StateSet
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder

class StateListDrawableTranscoder : ResourceTranscoder<Drawable, StateListDrawable> {

    override fun transcode(toTranscode: Resource<Drawable>, options: Options): Resource<StateListDrawable> {
        val stateList = StateListDrawable().apply {
            addState(ACTIVATED_STATE, options[ACTIVATED_DRAWABLE])
            addState(StateSet.NOTHING, toTranscode.get())
        }

        return StateListDrawableResource(stateList)
    }

    companion object {
        private val ACTIVATED_STATE = intArrayOf(android.R.attr.state_activated)

        /**
         * The drawable to display when the containing View is in the "activated" state.
         */
        @JvmStatic
        val ACTIVATED_DRAWABLE: Option<Drawable> =
                Option.memory("fr.nihilus.music.StateListDrawableTranscoder.activatedDrawable")
    }
}

private class StateListDrawableResource(
        private val drawable: StateListDrawable
) : Resource<StateListDrawable> {

    override fun getResourceClass() = StateListDrawable::class.java

    override fun recycle() {
        // Drawable are not recycled
    }

    override fun get() = drawable

    override fun getSize(): Int = 0
}