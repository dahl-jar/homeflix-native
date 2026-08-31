package app.homeflix.tv.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.TvMediaCard

private const val GRID_COLUMNS = 7
private const val POSTER_ASPECT_RATIO = 2f / 3f
private val CARD_SPACING = 14.dp

@Composable
internal fun LibraryGrid(
    items: List<MediaItem>,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    onMediaSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        items(
            items = items,
            key = MediaItem::id,
        ) { item ->
            LibraryCard(
                item = item,
                onClick = { onMediaSelected(item.id) },
            )
        }
    }
}

@Composable
private fun LibraryCard(
    item: MediaItem,
    onClick: () -> Unit,
) {
    TvMediaCard(
        name = item.name,
        imageUrl = item.primaryImageUrl ?: item.backdropImageUrl,
        playedPercentage = item.playedPercentage,
        contentDescription = "${item.name} card",
        onClick = onClick,
        modifier =
            Modifier
                .aspectRatio(POSTER_ASPECT_RATIO)
                .zIndex(1f),
    )
}
