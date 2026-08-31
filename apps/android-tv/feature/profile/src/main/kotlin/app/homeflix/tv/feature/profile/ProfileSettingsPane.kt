package app.homeflix.tv.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class RowAction(
    val label: String,
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Composable
internal fun SettingsPane(
    details: ProfileDetails,
    ioDispatcher: CoroutineDispatcher,
    onChangeServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheCleared by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        SettingsSection(label = "SERVER") {
            SettingRow(
                label = "Address",
                value = details.serverAddress,
                action =
                    RowAction(
                        label = "Change",
                        contentDescription = "Change server",
                        onClick = onChangeServer,
                    ),
            )
            RowDivider()
            SettingRow(label = "Status", value = "Connected")
        }
        Spacer(Modifier.height(SECTION_SPACING))
        SettingsSection(label = "APP") {
            SettingRow(label = "Version", value = details.appVersion)
            RowDivider()
            SettingRow(
                label = "Image cache",
                value = if (cacheCleared) "Cleared" else null,
                action =
                    RowAction(
                        label = "Clear",
                        contentDescription = "Clear image cache",
                        onClick = {
                            scope.launch {
                                clearImageCaches(context, ioDispatcher)
                                cacheCleared = true
                            }
                        },
                    ),
            )
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
    action: RowAction? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ROW_HORIZONTAL_PADDING,
                    vertical = if (action == null) ROW_VERTICAL_PADDING else ACTION_ROW_VERTICAL_PADDING,
                ),
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
        if (action != null) {
            Spacer(Modifier.width(ACTION_SPACING))
            RowActionButton(action)
        }
    }
}

@Composable
private fun RowActionButton(action: RowAction) {
    TvFocusSurface(
        contentDescription = action.contentDescription,
        onClick = action.onClick,
        appearance =
            TvFocusAppearance(
                shape = ActionButtonShape,
                backgroundColor = HomeflixColors.GlassBackground,
                unfocusedBorderColor = HomeflixColors.GlassBorder,
            ),
    ) {
        Text(
            text = action.label,
            color = HomeflixColors.OnBackground,
            fontSize = ACTION_FONT_SIZE,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(
                        horizontal = ACTION_HORIZONTAL_PADDING,
                        vertical = ACTION_VERTICAL_PADDING,
                    ),
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
private val ActionButtonShape = RoundedCornerShape(8.dp)
private val SECTION_SPACING = 28.dp
private val SECTION_LABEL_INSET = 4.dp
private val SECTION_LABEL_SPACING = 10.dp
private val ROW_HORIZONTAL_PADDING = 20.dp
private val ROW_VERTICAL_PADDING = 16.dp
private val ACTION_ROW_VERTICAL_PADDING = 8.dp
private val ACTION_SPACING = 20.dp
private val DIVIDER_HEIGHT = 1.dp
private val ACTION_HORIZONTAL_PADDING = 20.dp
private val ACTION_VERTICAL_PADDING = 8.dp
private val SECTION_LABEL_FONT_SIZE = 13.sp
private val SECTION_LABEL_LETTER_SPACING = 1.sp
private val ROW_LABEL_FONT_SIZE = 18.sp
private val ROW_VALUE_FONT_SIZE = 15.sp
private val ACTION_FONT_SIZE = 14.sp
