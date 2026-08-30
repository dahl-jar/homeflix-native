package app.homeflix.tv.feature.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TIME_UPDATE_INTERVAL_MS = 1_000L
private const val MILLISECONDS_PER_SECOND = 1_000.0
private const val MINIMUM_PLAYBACK_ADVANCE_SECONDS = 0.1

@androidx.annotation.OptIn(UnstableApi::class)
class ExoPlayerAdapter(
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
) {
    fun bind(callbacks: PlayerCallbacks): PlayerBinding {
        var disposed = false
        var lastPositionSeconds = 0.0

        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (disposed) return
                    when (playbackState) {
                        Player.STATE_READY -> callbacks.onReady(durationSeconds())
                        Player.STATE_ENDED -> callbacks.onEnded()
                        else -> Unit
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (disposed) return
                    callbacks.onPlayingChange(isPlaying)
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (disposed) return
                    callbacks.onError(error.errorCodeName)
                }
            }
        player.addListener(listener)

        val ticker: Job =
            scope.launch {
                while (isActive) {
                    if (!disposed) {
                        val positionSeconds = player.currentPosition / MILLISECONDS_PER_SECOND
                        val advanced = positionSeconds > lastPositionSeconds + MINIMUM_PLAYBACK_ADVANCE_SECONDS
                        lastPositionSeconds = positionSeconds
                        callbacks.onTimeUpdate(
                            positionSeconds = positionSeconds,
                            durationSeconds = durationSeconds(),
                            bufferedSeconds = player.bufferedPosition / MILLISECONDS_PER_SECOND,
                            playbackAdvanced = advanced && player.isPlaying,
                        )
                    }
                    delay(TIME_UPDATE_INTERVAL_MS)
                }
            }

        return object : PlayerBinding {
            override suspend fun load(
                source: PlayerMediaSource,
                startSeconds: Double,
            ) {
                if (disposed) return
                val builder = MediaItem.Builder().setUri(source.url)
                if (source.hls) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                player.setMediaItem(builder.build())
                player.prepare()
                player.seekTo((startSeconds * MILLISECONDS_PER_SECOND).toLong())
                player.play()
                lastPositionSeconds = startSeconds
            }

            override fun play() {
                player.play()
            }

            override fun pause() {
                player.pause()
            }

            override fun seekBy(seconds: Double) {
                val target = player.currentPosition + (seconds * MILLISECONDS_PER_SECOND).toLong()
                player.seekTo(target.coerceAtLeast(0))
            }

            override fun seekTo(seconds: Double) {
                player.seekTo((seconds * MILLISECONDS_PER_SECOND).toLong().coerceAtLeast(0))
            }

            override fun selectNativeAudioTrack(typeOrdinal: Int) {
                selectTrack(C.TRACK_TYPE_AUDIO, typeOrdinal)
            }

            override fun selectNativeSubtitleTrack(typeOrdinal: Int?) {
                if (typeOrdinal == null) {
                    player.trackSelectionParameters =
                        player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                } else {
                    selectTrack(C.TRACK_TYPE_TEXT, typeOrdinal)
                }
            }

            override fun snapshot(): PlayerSnapshot =
                PlayerSnapshot(
                    positionSeconds = player.currentPosition / MILLISECONDS_PER_SECOND,
                    durationSeconds = durationSeconds(),
                    isPaused = !player.isPlaying,
                )

            override fun dispose() {
                if (disposed) return
                disposed = true
                ticker.cancel()
                player.removeListener(listener)
                player.pause()
                player.stop()
                player.clearMediaItems()
            }
        }
    }

    private fun durationSeconds(): Double {
        val duration = player.duration
        return if (duration == C.TIME_UNSET) 0.0 else duration / MILLISECONDS_PER_SECOND
    }

    private fun selectTrack(
        trackType: Int,
        typeOrdinal: Int,
    ) {
        val groups = player.currentTracks.groups.filter { it.type == trackType }
        val group = groups.getOrNull(typeOrdinal)
        if (group != null) {
            player.trackSelectionParameters =
                player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(trackType, false)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                    .build()
        }
    }
}
