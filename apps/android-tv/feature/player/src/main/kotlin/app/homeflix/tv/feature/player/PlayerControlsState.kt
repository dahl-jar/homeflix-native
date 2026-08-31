package app.homeflix.tv.feature.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class PlayerControlsState {
    private var hidden by mutableStateOf(false)

    var revision by mutableIntStateOf(0)
        private set

    var pinned by mutableStateOf(false)

    fun visible(status: PlaybackStatus): Boolean = status != PlaybackStatus.PLAYING || !hidden

    fun shouldAutoHide(status: PlaybackStatus): Boolean =
        shouldScheduleAutoHide(status, hidden = hidden, pinned = pinned)

    fun show() {
        hidden = false
        revision += 1
    }

    fun hide() {
        hidden = true
    }

    fun toggle(status: PlaybackStatus) {
        if (visible(status)) hide() else show()
    }
}
