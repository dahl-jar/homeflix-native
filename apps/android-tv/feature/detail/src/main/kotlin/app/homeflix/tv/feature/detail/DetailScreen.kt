package app.homeflix.tv.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvNavEntry
import app.homeflix.tv.core.designsystem.TvNavProfile
import app.homeflix.tv.core.designsystem.TvNavigationRail
import app.homeflix.tv.core.designsystem.libraryNavIcon
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    gateway: DetailGateway,
    userId: String,
    itemId: String,
    profile: TvNavProfile,
    libraries: List<LibrarySummary>,
    onHomeSelected: () -> Unit,
    onLibrarySelected: (LibrarySummary) -> Unit,
    onProfileSelected: () -> Unit,
    onMediaSelected: (String) -> Unit,
    onPlaySelected: (String) -> Unit,
) {
    var state by remember(gateway, itemId) { mutableStateOf<DetailUiState>(DetailUiState.Loading) }
    var similar by remember(gateway, itemId) { mutableStateOf(emptyList<MediaItem>()) }
    var seasons by remember(gateway, itemId) { mutableStateOf(emptyList<DetailSeason>()) }
    var seasonIndex by remember(gateway, itemId) { mutableIntStateOf(0) }
    var episodes by remember(gateway, itemId) { mutableStateOf(emptyList<MediaItem>()) }
    var retryToken by remember(gateway, itemId) { mutableIntStateOf(0) }

    LaunchedEffect(gateway, itemId, retryToken) {
        state = DetailUiState.Loading
        coroutineScope {
            launch { similar = optionalList { gateway.fetchSimilar(userId, itemId) } }
            state =
                try {
                    DetailUiState.Content(gateway.fetchDetail(userId, itemId))
                } catch (failure: CancellationException) {
                    throw failure
                } catch (_: Exception) {
                    DetailUiState.Error
                }
        }
    }

    LaunchedEffect(state) {
        val content = state as? DetailUiState.Content
        if (content != null && content.value.item.type == SERIES_TYPE) {
            seasons = optionalList { gateway.fetchSeasons(userId, itemId) }
            seasonIndex = defaultSeasonIndex(seasons)
        }
    }

    LaunchedEffect(seasons, seasonIndex) {
        val season = seasons.getOrNull(seasonIndex)
        if (season != null) {
            episodes = optionalList { gateway.fetchEpisodes(userId, itemId, season.id) }
        }
    }

    DetailStateViews(
        state = state,
        similar = similar,
        seasons = seasons,
        seasonIndex = seasonIndex,
        episodes = episodes,
        onRetry = { retryToken += 1 },
        onSeasonSelected = { index -> seasonIndex = index },
        onMediaSelected = onMediaSelected,
        onPlaySelected = onPlaySelected,
        profile = profile,
        libraries = libraries,
        onHomeSelected = onHomeSelected,
        onLibrarySelected = onLibrarySelected,
        onProfileSelected = onProfileSelected,
    )
}

@Composable
private fun DetailStateViews(
    state: DetailUiState,
    similar: List<MediaItem>,
    seasons: List<DetailSeason>,
    seasonIndex: Int,
    episodes: List<MediaItem>,
    onRetry: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onMediaSelected: (String) -> Unit,
    onPlaySelected: (String) -> Unit,
    profile: TvNavProfile,
    libraries: List<LibrarySummary>,
    onHomeSelected: () -> Unit,
    onLibrarySelected: (LibrarySummary) -> Unit,
    onProfileSelected: () -> Unit,
) {
    val contentFocusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            DetailUiState.Loading -> DetailSkeleton()
            DetailUiState.Error ->
                DetailError(
                    retryFocusRequester = contentFocusRequester,
                    onRetry = onRetry,
                )
            is DetailUiState.Content ->
                DetailContentScaffold(
                    content = state.value,
                    similar = similar,
                    seasons = seasons,
                    seasonIndex = seasonIndex,
                    episodes = episodes,
                    playFocusRequester = contentFocusRequester,
                    onSeasonSelected = onSeasonSelected,
                    onMediaSelected = onMediaSelected,
                    onPlaySelected = onPlaySelected,
                )
        }
        TvNavigationRail(
            profile = profile,
            entries = detailNavEntries(libraries),
            contentFocusRequester = contentFocusRequester,
            onEntrySelected = { entryId ->
                when {
                    entryId == HOME_ENTRY_ID -> onHomeSelected()
                    else ->
                        libraries.firstOrNull { candidate -> candidate.id == entryId }?.let(onLibrarySelected)
                }
            },
            onProfileSelected = onProfileSelected,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

private fun detailNavEntries(libraries: List<LibrarySummary>): List<TvNavEntry> =
    listOf(
        TvNavEntry(
            id = HOME_ENTRY_ID,
            label = "Home",
            icon = Icons.Filled.Home,
            selected = false,
        ),
    ) +
        libraries.map { library ->
            TvNavEntry(
                id = library.id,
                label = library.name,
                icon = libraryNavIcon(library.collectionType),
                selected = false,
            )
        }

@Composable
private fun DetailContentScaffold(
    content: DetailContent,
    similar: List<MediaItem>,
    seasons: List<DetailSeason>,
    seasonIndex: Int,
    episodes: List<MediaItem>,
    playFocusRequester: FocusRequester,
    onSeasonSelected: (Int) -> Unit,
    onMediaSelected: (String) -> Unit,
    onPlaySelected: (String) -> Unit,
) {
    LaunchedEffect(content) {
        playFocusRequester.requestFocus()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(HomeflixColors.Background),
    ) {
        DetailBackdrop(content.item.backdropImageUrl)
        DetailSections(
            content = content,
            similar = similar,
            seasons = seasons,
            seasonIndex = seasonIndex,
            episodes = episodes,
            playFocusRequester = playFocusRequester,
            onSeasonSelected = onSeasonSelected,
            onMediaSelected = onMediaSelected,
            onPlaySelected = onPlaySelected,
        )
    }
}

@Composable
private fun DetailSections(
    content: DetailContent,
    similar: List<MediaItem>,
    seasons: List<DetailSeason>,
    seasonIndex: Int,
    episodes: List<MediaItem>,
    playFocusRequester: FocusRequester,
    onSeasonSelected: (Int) -> Unit,
    onMediaSelected: (String) -> Unit,
    onPlaySelected: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(
                start = CONTENT_START_PADDING,
                end = CONTENT_END_PADDING,
                bottom = CONTENT_BOTTOM_PADDING,
            ),
        modifier =
            Modifier
                .fillMaxSize()
                .focusGroup(),
    ) {
        item(key = "summary") {
            Spacer(Modifier.height(SUMMARY_TOP_SPACING))
            DetailSummary(
                content = content,
                playFocusRequester = playFocusRequester,
                onPlaySelected = onPlaySelected,
                onPlayFocused = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
            )
        }
        if (seasons.isNotEmpty()) {
            item(key = "seasons") {
                Spacer(Modifier.height(SECTION_SPACING))
                DetailSeasonsRow(
                    seasons = seasons,
                    seasonIndex = seasonIndex,
                    episodes = episodes,
                    onSeasonSelected = onSeasonSelected,
                    onEpisodeSelected = onPlaySelected,
                )
            }
        }
        if (similar.isNotEmpty()) {
            item(key = "similar") {
                Spacer(Modifier.height(SECTION_SPACING))
                DetailPosterRail(
                    title = "More Like This",
                    items = similar,
                    onMediaSelected = onMediaSelected,
                )
            }
        }
        if (content.cast.isNotEmpty()) {
            item(key = "cast") {
                Spacer(Modifier.height(SECTION_SPACING))
                DetailCastRow(cast = content.cast)
            }
        }
    }
}

@Composable
private fun DetailBackdrop(backdropUrl: String?) {
    if (backdropUrl != null) {
        SubcomposeAsyncImage(
            model = backdropUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to HomeflixColors.Background.copy(alpha = TOP_SCRIM_ALPHA),
                        TOP_SCRIM_STOP to Color.Transparent,
                        BOTTOM_SCRIM_STOP to HomeflixColors.Background.copy(alpha = BOTTOM_SCRIM_ALPHA),
                        1f to HomeflixColors.Background,
                    ),
                ),
    )
}

@Composable
private fun DetailError(
    retryFocusRequester: FocusRequester,
    onRetry: () -> Unit,
) {
    LaunchedEffect(Unit) {
        retryFocusRequester.requestFocus()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(HomeflixColors.Background),
    ) {
        Text(
            text = "Can’t load this title.",
            color = HomeflixColors.OnBackground,
            fontSize = ERROR_FONT_SIZE,
            modifier = Modifier.padding(bottom = ERROR_TEXT_SPACING),
        )
        DetailRetryButton(
            onRetry = onRetry,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(top = ERROR_TEXT_SPACING)
                    .focusRequester(retryFocusRequester),
        )
    }
}

private suspend fun <T> optionalList(block: suspend () -> List<T>): List<T> =
    try {
        block()
    } catch (failure: CancellationException) {
        throw failure
    } catch (_: Exception) {
        emptyList()
    }

private const val SERIES_TYPE = "Series"
private const val HOME_ENTRY_ID = "home"
private const val TOP_SCRIM_ALPHA = 0.45f
private const val TOP_SCRIM_STOP = 0.28f
private const val BOTTOM_SCRIM_STOP = 0.5f
private const val BOTTOM_SCRIM_ALPHA = 0.92f
private val CONTENT_START_PADDING = 72.dp
private val CONTENT_END_PADDING = 48.dp
private val CONTENT_BOTTOM_PADDING = 44.dp
private val SUMMARY_TOP_SPACING = 180.dp
private val SECTION_SPACING = 28.dp
private val ERROR_TEXT_SPACING = 40.dp
private val ERROR_FONT_SIZE = 20.sp
