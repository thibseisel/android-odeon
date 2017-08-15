package fr.nihilus.mymusic.playback

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat

fun queueOf(vararg mediaIds: String?): List<MediaSessionCompat.QueueItem> {
    val queue = ArrayList<MediaSessionCompat.QueueItem>(mediaIds.size)
    val builder = MediaDescriptionCompat.Builder()

    mediaIds.forEachIndexed { index, mediaId ->
        val description = builder.setTitle("Item $index")
                .setSubtitle("The queue item N°$index")
                .setMediaId(mediaId)
                .setMediaUri(Uri.parse("content://path/to/music/$index"))
                .build()
        queue.add(MediaSessionCompat.QueueItem(description, index.toLong()))
    }

    return queue
}

fun queueItemWith(mediaId: String?,
                  queueId: Long = 1L,
                  title: String = "Item $queueId",
                  mediaUri: String? = "content://path/to/file")
        : MediaSessionCompat.QueueItem {
    val description = MediaDescriptionCompat.Builder()
            .setMediaId(mediaId)
            .setTitle(title)
            .setMediaUri(if (mediaUri != null) Uri.parse(mediaUri) else null)
            .build()
    return MediaSessionCompat.QueueItem(description, queueId)
}