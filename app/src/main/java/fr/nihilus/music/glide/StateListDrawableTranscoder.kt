package fr.nihilus.music.glide

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.Log
import android.util.StateSet
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder

class StateListDrawableTranscoder(
        private val context: Context
) : ResourceTranscoder<Drawable, StateListDrawable> {

    override fun transcode(toTranscode: Resource<Drawable>, options: Options): Resource<StateListDrawable> {
        val stateList = StateListDrawable().apply {
            addState(ACTIVATED_STATE, options[ACTIVATED_DRAWABLE])
            addState(StateSet.WILD_CARD, toTranscode.get())
        }

        Log.d("SLDTranscoder", "StateListDrawable: $stateList")

        return StateListDrawableResource(stateList)
    }

    companion object {
        private val ACTIVATED_STATE = intArrayOf(android.R.attr.state_activated)

        /**
         * The drawable to display when the containing View is in the "activated" state.
         */
        @JvmField val ACTIVATED_DRAWABLE: Option<Drawable?> =
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