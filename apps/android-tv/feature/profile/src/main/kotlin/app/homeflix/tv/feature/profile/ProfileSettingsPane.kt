package app.homeflix.tv.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsPane(
    details: ProfileDetails,
    ioDispatcher: CoroutineDispatcher,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheCleared by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        SettingsSection(label = "SERVER") {
            SettingRow(label = "Address", value = details.serverAddress)
            RowDivider()
            SettingRow(label = "Status", value = "Connected")
        }
        Spacer(Modifier.height(SECTION_SPACING))
        SettingsSection(label = "APP") {
            ActionSettingRow(
                label = "Clear image cache",
                value = if (cacheCleared) "Cleared" else null,
                contentDescription = "Clear image cache",
                onClick = {
                    scope.launch {
                        clearImageCaches(context, ioDispatcher)
                        cacheCleared = true
                    }
                },
            )
            SettingRow(label = "Version", value = details.appVersion)
        }
    }
}

@Composable
private fun SettingsSection(
    label: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = label,
        color = HomeflixColors.Muted,
        fontSize = SECTION_LABEL_FONT_SIZE,
        letterSpacing = SECTION_LABEL_LETTER_SPACING,
        modifier = Modifier.padding(start = SECTION_LABEL_INSET, bottom = SECTION_LABEL_SPACING),
    )
    Column {
        content()
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String?,
) {
    SettingRowContent(
        label = label,
        value = value,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ActionSettingRow(
    label: String,
    value: String?,
    contentDescription: String,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { this.contentDescription = contentDescription }
                .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
    ) {
        SettingRowContent(label = label, value = value)
        FocusUnderline(isFocused)
    }
}

@Composable
private fun FocusUnderline(isFocused: Boolean) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(UNDERLINE_HEIGHT),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(if (isFocused) UNDERLINE_HEIGHT else DIVIDER_HEIGHT)
                    .background(if (isFocused) HomeflixColors.Focus else DividerColor),
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(DIVIDER_HEIGHT)
                .background(DividerColor),
    )
}

@Composable
private fun SettingRowContent(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier.padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = ROW_VERTICAL_PADDING),
    ) {
        Text(
            text = label,
            color = HomeflixColors.OnBackground,
            fontSize = ROW_LABEL_FONT_SIZE,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                color = HomeflixColors.Muted,
                fontSize = ROW_VALUE_FONT_SIZE,
            )
        }
    }
}

private suspend fun clearImageCaches(
    context: PlatformContext,
    ioDispatcher: CoroutineDispatcher,
) {
    val imageLoader = SingletonImageLoader.get(context)
    withContext(ioDispatcher) { imageLoader.diskCache?.clear() }
    imageLoader.memoryCache?.clear()
}

private const val DIVIDER_ALPHA = 0.07f
private val DividerColor = Color.White.copy(alpha = DIVIDER_ALPHA)
private val SECTION_SPACING = 28.dp
private val SECTION_LABEL_INSET = 4.dp
private val SECTION_LABEL_SPACING = 10.dp
private val ROW_HORIZONTAL_PADDING = 20.dp
private val ROW_VERTICAL_PADDING = 16.dp
private val DIVIDER_HEIGHT = 1.dp
private val UNDERLINE_HEIGHT = 2.dp
private val SECTION_LABEL_FONT_SIZE = 13.sp
private val SECTION_LABEL_LETTER_SPACING = 1.sp
private val ROW_LABEL_FONT_SIZE = 18.sp
private val ROW_VALUE_FONT_SIZE = 15.sp
