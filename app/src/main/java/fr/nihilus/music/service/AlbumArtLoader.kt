package fr.nihilus.music.service

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v7.content.res.AppCompatResources
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import fr.nihilus.music.R
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import io.reactivex.Single
import javax.inject.Inject

private const val TAG = "AlbumArtLoader"
private const val ART_MAX_SIZE = 320

@ServiceScoped
class AlbumArtLoader
@Inject internal constructor(service: MusicService) {

    private val mGlide: GlideRequest<Bitmap>

    init {
        val errorDrawable = AppCompatResources.getDrawable(service, R.drawable.dummy_album_art)
        mGlide = GlideApp.with(service).asBitmap()
                .downsample(DownsampleStrategy.AT_MOST)
                .override(ART_MAX_SIZE)
                .error(errorDrawable)
    }

    fun loadIntoMetadata(metadata: MediaMetadataCompat): Single<MediaMetadataCompat> {
        return Single.create { emitter ->
            val uriString = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            if (uriString != null) {
                val artUri = Uri.parse(uriString)
                mGlide.load(artUri).into(object : SimpleTarget<Bitmap>() {

                    override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                        // Emits a new metadata with an album art
                        val newMeta = MediaMetadataCompat.Builder(metadata)
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
                                .build()
                        emitter.onSuccess(newMeta)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        // Emits the same metadata if album art loading fails
                        emitter.onSuccess(metadata)
                    }
                })
            }
        }
    }
}