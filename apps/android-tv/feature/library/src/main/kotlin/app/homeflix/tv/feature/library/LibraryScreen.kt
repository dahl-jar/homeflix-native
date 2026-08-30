package app.homeflix.tv.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvNavEntry
import app.homeflix.tv.core.designsystem.TvNavProfile
import app.homeflix.tv.core.designsystem.TvNavigationRail
import app.homeflix.tv.core.designsystem.libraryNavIcon

@Composable
fun LibraryScreen(
    gateway: LibraryGateway,
    userId: String,
    library: LibrarySummary,
    libraries: List<LibrarySummary>,
    profile: TvNavProfile,
    onHomeSelected: () -> Unit,
    onLibrarySelected: (LibrarySummary) -> Unit,
    onMediaSelected: (String) -> Unit,
    onProfileSelected: () -> Unit,
) {
    var selection by remember(library.id) { mutableStateOf(LibraryFilterSelection()) }
    var options by remember(library.id) { mutableStateOf(LibraryFilterOptions(emptyList(), emptyList())) }
    var retryToken by remember(library.id) { mutableStateOf(0) }
    var activePicker by remember(library.id) { mutableStateOf<LibraryFilterKind?>(null) }
    val pager = remember(library.id, selection, retryToken) { LibraryPager() }
    var state by remember(pager) { mutableStateOf<LibraryUiState>(LibraryUiState.Loading) }
    val gridState = rememberLazyGridState()
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(library.id) {
        options = loadFilterOptions(gateway, userId, library.id)
    }

    LibraryLoadEffects(
        gateway = gateway,
        userId = userId,
        libraryId = library.id,
        selection = selection,
        pager = pager,
        gridState = gridState,
        onState = { loaded -> state = loaded },
    )

    LaunchedEffect(activePicker) {
        if (activePicker == null) {
            contentFocusRequester.requestFocus()
        }
    }

    LibraryScaffold(
        library = library,
        libraries = libraries,
        profile = profile,
        state = state,
        selection = selection,
        options = options,
        gridState = gridState,
        activePicker = activePicker,
        contentFocusRequester = contentFocusRequester,
        onSelectionChanged = { changed -> selection = changed },
        onOpenPicker = { kind -> activePicker = kind },
        onDismissPicker = { activePicker = null },
        onRetry = { retryToken += 1 },
        onMediaSelected = onMediaSelected,
        onHomeSelected = onHomeSelected,
        onLibrarySelected = onLibrarySelected,
        onProfileSelected = onProfileSelected,
    )
}

@Composable
private fun LibraryScaffold(
    library: LibrarySummary,
    libraries: List<LibrarySummary>,
    profile: TvNavProfile,
    state: LibraryUiState,
    selection: LibraryFilterSelection,
    options: LibraryFilterOptions,
    gridState: LazyGridState,
    activePicker: LibraryFilterKind?,
    contentFocusRequester: FocusRequester,
    onSelectionChanged: (LibraryFilterSelection) -> Unit,
    onOpenPicker: (LibraryFilterKind) -> Unit,
    onDismissPicker: () -> Unit,
    onRetry: () -> Unit,
    onMediaSelected: (String) -> Unit,
    onHomeSelected: () -> Unit,
    onLibrarySelected: (LibrarySummary) -> Unit,
    onProfileSelected: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(HomeflixColors.Background),
    ) {
        LibraryContent(
            library = library,
            state = state,
            selection = selection,
            options = options,
            gridState = gridState,
            contentFocusRequester = contentFocusRequester,
            onOpenPicker = onOpenPicker,
            onClearRefinements = {
                onSelectionChanged(selection.copy(genre = null, decade = null, rating = null, status = null))
            },
            onRetry = onRetry,
            onMediaSelected = onMediaSelected,
        )
        LibraryRail(
            profile = profile,
            libraries = libraries,
            selectedId = library.id,
            contentFocusRequester = contentFocusRequester,
            onHomeSelected = onHomeSelected,
            onLibrarySelected = onLibrarySelected,
            onProfileSelected = onProfileSelected,
            modifier = Modifier.align(Alignment.TopStart),
        )
        activePicker?.let { kind ->
            LibraryFilterPicker(
                title = pickerTitle(kind),
                rows = pickerRows(kind, selection, options),
                onSelect = { key ->
                    onSelectionChanged(applyPickerSelection(kind, key, selection, options))
                    onDismissPicker()
                },
                onDismiss = onDismissPicker,
            )
        }
    }
}

@Composable
private fun LibraryLoadEffects(
    gateway: LibraryGateway,
    userId: String,
    libraryId: String,
    selection: LibraryFilterSelection,
    pager: LibraryPager,
    gridState: LazyGridState,
    onState: (LibraryUiState) -> Unit,
) {
    LaunchedEffect(pager) {
        val loaded = loadPage(gateway, userId, libraryId, selection, pager)
        onState(loaded ?: LibraryUiState.Error)
        if (loaded != null) {
            gridState.scrollToItem(0)
        }
    }

    LaunchedEffect(pager, gridState) {
        observeLoadMore(gridState, pager) {
            loadPage(gateway, userId, libraryId, selection, pager)?.let(onState)
        }
    }
}

@Composable
private fun LibraryRail(
    profile: TvNavProfile,
    libraries: List<LibrarySummary>,
    selectedId: String,
    contentFocusRequester: FocusRequester,
    onHomeSelected: () -> Unit,
    onLibrarySelected: (LibrarySummary) -> Unit,
    onProfileSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvNavigationRail(
        profile = profile,
        entries = libraryNavEntries(libraries, selectedId),
        contentFocusRequester = contentFocusRequester,
        onEntrySelected = { entryId ->
            when {
                entryId == HOME_ENTRY_ID -> onHomeSelected()
                entryId != selectedId ->
                    libraries.firstOrNull { candidate -> candidate.id == entryId }?.let(onLibrarySelected)
            }
        },
        onProfileSelected = onProfileSelected,
        modifier = modifier,
    )
}

@Composable
private fun LibraryContent(
    library: LibrarySummary,
    state: LibraryUiState,
    selection: LibraryFilterSelection,
    options: LibraryFilterOptions,
    gridState: LazyGridState,
    contentFocusRequester: FocusRequester,
    onOpenPicker: (LibraryFilterKind) -> Unit,
    onClearRefinements: () -> Unit,
    onRetry: () -> Unit,
    onMediaSelected: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = CONTENT_TOP_PADDING)
                .focusRequester(contentFocusRequester)
                .focusRestorer()
                .focusGroup(),
    ) {
        LibraryHeader(
            name = library.name,
            total = (state as? LibraryUiState.Content)?.total,
            modifier = Modifier.padding(start = CONTENT_START_PADDING, end = CONTENT_END_PADDING),
        )
        Spacer(Modifier.height(HEADER_SPACING))
        LibraryFilterBar(
            selection = selection,
            options = options,
            onOpenPicker = onOpenPicker,
            onClearRefinements = onClearRefinements,
            startPadding = CONTENT_START_PADDING,
            endPadding = CONTENT_END_PADDING,
        )
        when (state) {
            LibraryUiState.Loading ->
                LibrarySkeleton(
                    libraryName = library.name,
                    modifier =
                        Modifier.padding(
                            start = CONTENT_START_PADDING,
                            top = FILTER_SPACING,
                            end = CONTENT_END_PADDING,
                        ),
                )

            LibraryUiState.Error ->
                LibraryError(
                    onRetry = onRetry,
                    modifier = Modifier.padding(start = CONTENT_START_PADDING),
                )

            is LibraryUiState.Content ->
                LibraryGrid(
                    items = state.items,
                    gridState = gridState,
                    contentPadding =
                        PaddingValues(
                            start = CONTENT_START_PADDING,
                            top = FILTER_SPACING,
                            end = CONTENT_END_PADDING,
                            bottom = GRID_BOTTOM_PADDING,
                        ),
                    onMediaSelected = onMediaSelected,
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

private fun libraryNavEntries(
    libraries: List<LibrarySummary>,
    selectedId: String,
): List<TvNavEntry> =
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
                selected = library.id == selectedId,
            )
        }

private const val HOME_ENTRY_ID = "home"
private val CONTENT_START_PADDING = 100.dp
private val CONTENT_END_PADDING = 48.dp
private val CONTENT_TOP_PADDING = 32.dp
private val HEADER_SPACING = 12.dp
private val FILTER_SPACING = 16.dp
private val GRID_BOTTOM_PADDING = 44.dp
