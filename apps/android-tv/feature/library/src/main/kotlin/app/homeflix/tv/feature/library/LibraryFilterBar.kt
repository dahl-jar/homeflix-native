package app.homeflix.tv.feature.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface

@Composable
internal fun LibraryFilterBar(
    selection: LibraryFilterSelection,
    options: LibraryFilterOptions,
    onOpenPicker: (LibraryFilterKind) -> Unit,
    onClearRefinements: () -> Unit,
    startPadding: Dp = 0.dp,
    endPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PILL_SPACING),
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = startPadding, end = endPadding),
    ) {
        if (selection.hasRefinements) {
            FilterPill(
                label = "Clear",
                refined = true,
                onClick = onClearRefinements,
            )
        }
        FilterPill(
            label = selection.sort.label,
            refined = false,
            onClick = { onOpenPicker(LibraryFilterKind.Sort) },
        )
        if (options.genres.isNotEmpty()) {
            FilterPill(
                label = selection.genre ?: "Genre",
                refined = selection.genre != null,
                onClick = { onOpenPicker(LibraryFilterKind.Genre) },
            )
        }
        if (options.decades.isNotEmpty()) {
            FilterPill(
                label = selection.decade?.label ?: "Decade",
                refined = selection.decade != null,
                onClick = { onOpenPicker(LibraryFilterKind.Decade) },
            )
        }
        FilterPill(
            label = selection.rating?.label ?: "Rating",
            refined = selection.rating != null,
            onClick = { onOpenPicker(LibraryFilterKind.Rating) },
        )
        FilterPill(
            label = selection.status?.label ?: "Watched",
            refined = selection.status != null,
            onClick = { onOpenPicker(LibraryFilterKind.Status) },
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    refined: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    TvFocusSurface(
        contentDescription = "$label filter",
        onClick = onClick,
        appearance =
            TvFocusAppearance(
                shape = RoundedCornerShape(PILL_CORNER_RADIUS),
                backgroundColor = if (focused) HomeflixColors.OnBackground else HomeflixColors.GlassBackground,
                showFocusBorder = false,
            ),
        modifier = modifier.onFocusChanged { focusState -> focused = focusState.isFocused },
    ) {
        Text(
            text = label,
            color = pillLabelColor(focused, refined),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = PILL_HORIZONTAL_PADDING, vertical = PILL_VERTICAL_PADDING),
        )
    }
}

private fun pillLabelColor(
    focused: Boolean,
    refined: Boolean,
): Color =
    when {
        focused -> HomeflixColors.Background
        refined -> HomeflixColors.Focus
        else -> HomeflixColors.OnBackground
    }

internal fun pickerTitle(kind: LibraryFilterKind): String =
    when (kind) {
        LibraryFilterKind.Sort -> "Sort"
        LibraryFilterKind.Genre -> "Genre"
        LibraryFilterKind.Decade -> "Decade"
        LibraryFilterKind.Rating -> "Rating"
        LibraryFilterKind.Status -> "Watched"
    }

internal fun pickerRows(
    kind: LibraryFilterKind,
    selection: LibraryFilterSelection,
    options: LibraryFilterOptions,
): List<PickerRow> =
    when (kind) {
        LibraryFilterKind.Sort ->
            LibraryFilters.sortOptions.map { sort ->
                PickerRow(key = sort.key, label = sort.label, selected = sort.key == selection.sort.key)
            }

        LibraryFilterKind.Genre ->
            clearRow("All genres", selection.genre == null) +
                options.genres.map { genre ->
                    PickerRow(key = genre, label = genre, selected = genre == selection.genre)
                }

        LibraryFilterKind.Decade ->
            clearRow("Any decade", selection.decade == null) +
                options.decades.map { decade ->
                    PickerRow(key = decade.key, label = decade.label, selected = decade.key == selection.decade?.key)
                }

        LibraryFilterKind.Rating ->
            clearRow("Any rating", selection.rating == null) +
                LibraryFilters.ratingOptions.map { rating ->
                    PickerRow(key = rating.key, label = rating.label, selected = rating.key == selection.rating?.key)
                }

        LibraryFilterKind.Status ->
            clearRow("All", selection.status == null) +
                LibraryFilters.statusOptions.map { status ->
                    PickerRow(key = status.key, label = status.label, selected = status.key == selection.status?.key)
                }
    }

internal fun applyPickerSelection(
    kind: LibraryFilterKind,
    key: String?,
    selection: LibraryFilterSelection,
    options: LibraryFilterOptions,
): LibraryFilterSelection =
    when (kind) {
        LibraryFilterKind.Sort ->
            selection.copy(sort = LibraryFilters.sortOptions.firstOrNull { it.key == key } ?: selection.sort)

        LibraryFilterKind.Genre -> selection.copy(genre = key)

        LibraryFilterKind.Decade -> selection.copy(decade = options.decades.firstOrNull { it.key == key })

        LibraryFilterKind.Rating -> selection.copy(rating = LibraryFilters.ratingOptions.firstOrNull { it.key == key })

        LibraryFilterKind.Status -> selection.copy(status = LibraryFilters.statusOptions.firstOrNull { it.key == key })
    }

private fun clearRow(
    label: String,
    selected: Boolean,
): List<PickerRow> = listOf(PickerRow(key = null, label = label, selected = selected))

private val PILL_SPACING = 8.dp
private val PILL_CORNER_RADIUS = 17.dp
private val PILL_HORIZONTAL_PADDING = 14.dp
private val PILL_VERTICAL_PADDING = 7.dp
