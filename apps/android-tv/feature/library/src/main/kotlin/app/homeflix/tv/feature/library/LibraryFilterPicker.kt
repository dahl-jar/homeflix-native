package app.homeflix.tv.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface

@Composable
internal fun LibraryFilterPicker(
    title: String,
    rows: List<PickerRow>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedRowFocusRequester = remember { FocusRequester() }
    val focusIndex = rows.indexOfFirst(PickerRow::selected).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = focusIndex)

    BackHandler(onBack = onDismiss)
    LaunchedEffect(title) {
        selectedRowFocusRequester.requestFocus()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .zIndex(PICKER_Z_INDEX)
                .background(HomeflixColors.Background.copy(alpha = SCRIM_ALPHA)),
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = PICKER_START_PADDING)
                    .width(PICKER_WIDTH)
                    .fillMaxHeight(),
        ) {
            Spacer(Modifier.height(PICKER_TOP_SPACING))
            Text(
                text = title,
                color = HomeflixColors.OnBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(PICKER_TITLE_SPACING))
            LazyColumn(state = listState) {
                items(
                    items = rows,
                    key = { row -> row.key ?: CLEAR_ROW_KEY },
                ) { row ->
                    PickerRowSurface(
                        row = row,
                        focusRequester = selectedRowFocusRequester.takeIf { row == rows[focusIndex] },
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRowSurface(
    row: PickerRow,
    focusRequester: FocusRequester?,
    onSelect: (String?) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    TvFocusSurface(
        contentDescription = "${row.label} option",
        onClick = { onSelect(row.key) },
        appearance =
            TvFocusAppearance(
                shape = RoundedCornerShape(ROW_CORNER_RADIUS),
                backgroundColor = if (focused) HomeflixColors.OnBackground else Color.Transparent,
                showFocusBorder = false,
            ),
        modifier =
            Modifier
                .padding(bottom = ROW_SPACING)
                .onFocusChanged { focusState -> focused = focusState.isFocused }
                .let { surfaceModifier ->
                    if (focusRequester != null) surfaceModifier.focusRequester(focusRequester) else surfaceModifier
                },
    ) {
        Text(
            text = row.label,
            color = rowLabelColor(focused, row.selected),
            fontSize = 15.sp,
            fontWeight = if (row.selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = ROW_VERTICAL_PADDING),
        )
    }
}

private fun rowLabelColor(
    focused: Boolean,
    selected: Boolean,
): Color =
    when {
        selected -> HomeflixColors.Focus
        focused -> HomeflixColors.Background
        else -> HomeflixColors.OnBackground
    }

private const val CLEAR_ROW_KEY = "clear"
private const val SCRIM_ALPHA = 0.94f
private const val PICKER_Z_INDEX = 2f
private val PICKER_START_PADDING = 72.dp
private val PICKER_WIDTH = 320.dp
private val PICKER_TOP_SPACING = 68.dp
private val PICKER_TITLE_SPACING = 18.dp
private val ROW_CORNER_RADIUS = 8.dp
private val ROW_SPACING = 6.dp
private val ROW_HORIZONTAL_PADDING = 14.dp
private val ROW_VERTICAL_PADDING = 9.dp
