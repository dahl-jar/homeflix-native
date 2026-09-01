package app.homeflix.tv.feature.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
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
        var timeline = PlaybackTimeline(TimelineMode.ABSOLUTE_ITEM, originSeconds = 0.0)

        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (disposed) return
                    callbacks.onPlaybackStateChange(playbackEngineState(playbackState), player.playWhenReady)
                    when (playbackState) {
                        Player.STATE_READY -> callbacks.onReady(timeline.itemDurationSeconds(durationSeconds()))
                        Player.STATE_ENDED -> callbacks.onEnded()
                        else -> Unit
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (disposed) return
                    callbacks.onPlayingChange(isPlaying)
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    if (disposed) return
                    callbacks.onPlaybackStateChange(playbackEngineState(player.playbackState), playWhenReady)
                }

                override fun onRenderedFirstFrame() {
                    if (disposed) return
                    callbacks.onFirstFrame()
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (disposed) return
                    callbacks.onError(playerErrorDetails(error))
                }
            }
        val analyticsListener =
            object : AnalyticsListener {
                override fun onVideoInputFormatChanged(
                    eventTime: AnalyticsListener.EventTime,
                    format: androidx.media3.common.Format,
                    decoderReuseEvaluation: DecoderReuseEvaluation?,
                ) {
                    if (disposed) return
                    callbacks.onFormatSelected("video", playbackFormatTelemetry("video", format))
                }

                override fun onAudioInputFormatChanged(
                    eventTime: AnalyticsListener.EventTime,
                    format: androidx.media3.common.Format,
                    decoderReuseEvaluation: DecoderReuseEvaluation?,
                ) {
                    if (disposed) return
                    callbacks.onFormatSelected("audio", playbackFormatTelemetry("audio", format))
                }

                override fun onVideoDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    decoderName: String,
                    initializedTimestampMs: Long,
                    initializationDurationMs: Long,
                ) {
                    if (disposed) return
                    callbacks.onDecoderInitialized("video", decoderName)
                }

                override fun onAudioDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    decoderName: String,
                    initializedTimestampMs: Long,
                    initializationDurationMs: Long,
                ) {
                    if (disposed) return
                    callbacks.onDecoderInitialized("audio", decoderName)
                }
            }
        player.addListener(listener)
        player.addAnalyticsListener(analyticsListener)

        val ticker: Job =
            scope.launch {
                while (isActive) {
                    if (!disposed) {
                        val positionSeconds =
                            timeline.itemPositionSeconds(player.currentPosition / MILLISECONDS_PER_SECOND)
                        val advanced = positionSeconds > lastPositionSeconds + MINIMUM_PLAYBACK_ADVANCE_SECONDS
                        lastPositionSeconds = positionSeconds
                        callbacks.onTimeUpdate(
                            positionSeconds = positionSeconds,
                            durationSeconds = timeline.itemDurationSeconds(durationSeconds()),
                            bufferedSeconds =
                                timeline.itemPositionSeconds(player.bufferedPosition / MILLISECONDS_PER_SECOND),
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
                timeline = source.timeline(startSeconds)
                val builder = MediaItem.Builder().setUri(source.url)
                if (source.hls) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
                player.setMediaItem(builder.build())
                player.prepare()
                player.seekTo((timeline.playerPositionSeconds(startSeconds) * MILLISECONDS_PER_SECOND).toLong())
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
                val playerPositionSeconds = timeline.playerPositionSeconds(seconds)
                player.seekTo((playerPositionSeconds * MILLISECONDS_PER_SECOND).toLong().coerceAtLeast(0))
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
                    positionSeconds =
                        timeline.itemPositionSeconds(player.currentPosition / MILLISECONDS_PER_SECOND),
                    durationSeconds = timeline.itemDurationSeconds(durationSeconds()),
                    isPaused = !player.isPlaying,
                )

            override fun dispose() {
                if (disposed) return
                disposed = true
                ticker.cancel()
                player.removeListener(listener)
                player.removeAnalyticsListener(analyticsListener)
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

    private fun playbackEngineState(playbackState: Int): PlaybackEngineState =
        when (playbackState) {
            Player.STATE_BUFFERING -> PlaybackEngineState.BUFFERING
            Player.STATE_READY -> PlaybackEngineState.READY
            Player.STATE_ENDED -> PlaybackEngineState.ENDED
            else -> PlaybackEngineState.IDLE
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
