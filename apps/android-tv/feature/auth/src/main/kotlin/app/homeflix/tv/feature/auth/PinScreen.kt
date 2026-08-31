package app.homeflix.tv.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val PIN_LENGTH = 4
private const val BACKSPACE_KEY = -1
private val PIN_VERTICAL_BIAS = 48.dp
private val PIN_BOX_WIDTH = 48.dp
private val PIN_BOX_HEIGHT = 58.dp
private val PIN_BOX_SPACING = 14.dp
private val KEYPAD_KEY_WIDTH = 88.dp
private val KEYPAD_KEY_HEIGHT = 72.dp
private val KeypadRows =
    listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
        listOf(null, 0, BACKSPACE_KEY),
    )

@Composable
fun PinScreen(
    onBack: () -> Unit,
    onPinSubmitted: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val firstKeyFocusRequester = remember { FocusRequester() }
    val pinEntry = rememberPinEntry(onPinSubmitted)

    LaunchedEffect(firstKeyFocusRequester) {
        firstKeyFocusRequester.requestFocus()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(HomeflixColors.Background),
    ) {
        PinBackButton(
            onClick = onBack,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 62.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(top = PIN_VERTICAL_BIAS),
        ) {
            Text(
                text = "Enter your PIN to access this profile.",
                color = HomeflixColors.OnBackground,
                fontSize = 20.sp,
            )
            Spacer(Modifier.height(32.dp))
            PinIndicators(state = pinEntry.state)
            Spacer(Modifier.height(48.dp))
            PinKeypad(
                firstKeyFocusRequester = firstKeyFocusRequester,
                onKeySelected = pinEntry.onKeySelected,
            )
        }
    }
}

private data class PinEntry(
    val state: PinInputState,
    val onKeySelected: (Int) -> Unit,
)

@Composable
private fun rememberPinEntry(onPinSubmitted: suspend (String) -> Boolean): PinEntry {
    var state by remember { mutableStateOf(PinInputState()) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    return PinEntry(
        state = state,
        onKeySelected = { key ->
            when {
                key == BACKSPACE_KEY -> state = PinInputReducer.backspace(state)
                !isSubmitting -> {
                    val result = PinInputReducer.append(state, key)
                    state = result.state
                    result.pin?.let { pin ->
                        isSubmitting = true
                        scope.launch {
                            val accepted =
                                try {
                                    onPinSubmitted(pin)
                                } catch (failure: CancellationException) {
                                    throw failure
                                } catch (_: Exception) {
                                    false
                                }
                            if (!accepted) {
                                state = PinInputReducer.authenticationFailed(state)
                            }
                            isSubmitting = false
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PinKeypad(
    firstKeyFocusRequester: FocusRequester,
    onKeySelected: (Int) -> Unit,
) {
    Column {
        KeypadRows.forEachIndexed { rowIndex, row ->
            Row {
                row.forEachIndexed { columnIndex, key ->
                    if (key == null) {
                        Spacer(Modifier.size(width = KEYPAD_KEY_WIDTH, height = KEYPAD_KEY_HEIGHT))
                    } else {
                        KeypadButton(
                            key = key,
                            onClick = { onKeySelected(key) },
                            modifier =
                                if (rowIndex == 0 && columnIndex == 0) {
                                    Modifier.focusRequester(firstKeyFocusRequester)
                                } else {
                                    Modifier
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinIndicators(state: PinInputState) {
    Row(horizontalArrangement = Arrangement.spacedBy(PIN_BOX_SPACING)) {
        repeat(PIN_LENGTH) { index ->
            val borderColor = if (state.hasError) HomeflixColors.Focus else HomeflixColors.PinBorder
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(width = PIN_BOX_WIDTH, height = PIN_BOX_HEIGHT)
                        .border(width = 1.dp, color = borderColor),
            ) {
                if (index < state.digits.length) {
                    Box(
                        modifier =
                            Modifier
                                .size(13.dp)
                                .background(Color.White, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun PinBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusSurface(
        contentDescription = "Back",
        onClick = onClick,
        appearance =
            TvFocusAppearance(
                shape = CircleShape,
                backgroundColor = HomeflixColors.GlassBackground,
            ),
        modifier = modifier.size(38.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .border(1.dp, HomeflixColors.GlassBorder, CircleShape),
        ) {
            Text(
                text = "‹",
                color = HomeflixColors.OnBackground,
                fontSize = 24.sp,
            )
        }
    }
}

@Composable
private fun KeypadButton(
    key: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (key == BACKSPACE_KEY) "⌫" else key.toString()
    val description = if (key == BACKSPACE_KEY) "Backspace" else label
    TvFocusSurface(
        contentDescription = description,
        onClick = onClick,
        modifier = modifier.size(width = KEYPAD_KEY_WIDTH, height = KEYPAD_KEY_HEIGHT),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = label,
                color = HomeflixColors.OnBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
