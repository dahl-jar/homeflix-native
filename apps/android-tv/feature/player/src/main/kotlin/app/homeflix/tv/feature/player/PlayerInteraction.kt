package app.homeflix.tv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private val PlayerMenuBackground = Color(0xF7100E0F)

@Stable
internal class PlayerFocusState {
    var isFocused by mutableStateOf(false)
        private set

    fun update(isFocused: Boolean) {
        this.isFocused = isFocused
    }
}

@Composable
internal fun rememberPlayerFocusState(): PlayerFocusState = remember { PlayerFocusState() }

@Composable
internal fun Modifier.playerFocusableClick(
    state: PlayerFocusState,
    contentDescription: String,
    onClick: () -> Unit,
): Modifier =
    semantics { this.contentDescription = contentDescription }
        .onFocusChanged { state.update(it.isFocused) }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )

@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.playerMenuSurface(): Modifier =
    fillMaxSize()
        .background(PlayerMenuBackground)
        .focusGroup()
        .focusProperties { onExit = { FocusRequester.Cancel } }
