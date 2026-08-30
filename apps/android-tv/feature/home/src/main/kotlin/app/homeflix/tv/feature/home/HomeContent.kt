package app.homeflix.tv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.catalog.MediaItem
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.TvNavEntry
import app.homeflix.tv.core.designsystem.TvNavProfile
import app.homeflix.tv.core.designsystem.TvNavigationRail
import app.homeflix.tv.core.designsystem.libraryNavIcon
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

internal val HOME_HORIZONTAL_PADDING = 72.dp
internal val HOME_NAV_GUTTER = 60.dp
internal val RAIL_START = 300.dp
private const val HERO_SETTLE_MILLIS = 220L

@OptIn(FlowPreview::class)
@Composable
fun HomeCatalog(
    content: HomeContent,
    viewer: HomeViewer,
    onMediaSelected: (String) -> Unit,
    onProfileSelected: () -> Unit,
    libraries: List<LibrarySummary> = emptyList(),
    onLibrarySelected: (LibrarySummary) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val firstCardFocusRequester = remember { FocusRequester() }
    val railEntryFocusRequester = remember { FocusRequester() }
    var focused by remember(content) { mutableStateOf(HomePolicy.initialHero(content)) }
    var hero by remember(content) { mutableStateOf(HomePolicy.initialHero(content)) }

    LaunchedEffect(content) {
        if (hero != null) {
            firstCardFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(content) {
        snapshotFlow { focused }
            .debounce(HERO_SETTLE_MILLIS)
            .collect { settled -> hero = settled }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(HomeflixColors.Background),
    ) {
        HomeHero(item = hero)
        HomeRails(
            content = content,
            railFocus = HomeRailFocus(firstCardFocusRequester, railEntryFocusRequester),
            onFocused = { focused = it },
            onMediaSelected = onMediaSelected,
        )
        TvNavigationRail(
            profile = TvNavProfile(name = viewer.name, avatarUrl = viewer.avatarUrl),
            entries = homeNavEntries(libraries),
            contentFocusRequester = railEntryFocusRequester,
            onEntrySelected = { entryId ->
                libraries.firstOrNull { library -> library.id == entryId }?.let(onLibrarySelected)
            },
            onProfileSelected = onProfileSelected,
            modifier = Modifier.align(Alignment.TopStart),
        )
        HomeHeader(modifier = Modifier.align(Alignment.TopStart))
    }
}

private const val HOME_ENTRY_ID = "home"

private fun homeNavEntries(libraries: List<LibrarySummary>): List<TvNavEntry> =
    listOf(
        TvNavEntry(
            id = HOME_ENTRY_ID,
            label = "Home",
            icon = Icons.Filled.Home,
            selected = true,
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

private fun Modifier.railsFocusContainment(): Modifier =
    focusProperties {
        onExit = {
            if (requestedFocusDirection == FocusDirection.Up) {
                cancelFocusChange()
            }
        }
    }.focusGroup()

private fun featuredRail(items: List<MediaItem>): HomeRail =
    HomeRail(
        id = "featured",
        title = "Featured",
        items = items,
        variant = HomeRailVariant.Landscape,
    )

@Composable
private fun HomeRails(
    content: HomeContent,
    railFocus: HomeRailFocus,
    onFocused: (MediaItem) -> Unit,
    onMediaSelected: (String) -> Unit,
) {
    HomeVerticalFocusPositioning {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = RAIL_START)
                    .railsFocusContainment(),
        ) {
            HomeRailsColumn(
                content = content,
                railFocus = railFocus,
                onFocused = onFocused,
                onMediaSelected = onMediaSelected,
            )
        }
    }
}

@Composable
private fun HomeRailsColumn(
    content: HomeContent,
    railFocus: HomeRailFocus,
    onFocused: (MediaItem) -> Unit,
    onMediaSelected: (String) -> Unit,
) {
    val continueRail = HomePolicy.continueRail(content)
    val laterRails = content.rails.filter { rail -> rail.id != HomePolicy.CONTINUE_RAIL_ID }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 44.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(railFocus.entry)
                .focusRestorer(),
    ) {
        if (continueRail != null) {
            item(key = continueRail.id) {
                HomeMediaRail(
                    rail = continueRail,
                    railFocus = railFocus,
                    onFocused = onFocused,
                    onMediaSelected = onMediaSelected,
                )
                Spacer(Modifier.height(22.dp))
            }
        }
        if (content.featured.isNotEmpty()) {
            item(key = "featured") {
                HomeMediaRail(
                    rail = featuredRail(content.featured),
                    railFocus = railFocus.takeIf { continueRail == null },
                    onFocused = onFocused,
                    onMediaSelected = onMediaSelected,
                    featured = true,
                )
                Spacer(Modifier.height(22.dp))
            }
        }
        items(
            count = laterRails.size,
            key = { index -> laterRails[index].id },
        ) { index ->
            val rail = laterRails[index]
            HomeMediaRail(
                rail = rail,
                railFocus =
                    railFocus.takeIf {
                        continueRail == null && content.featured.isEmpty() && index == 0
                    },
                onFocused = onFocused,
                onMediaSelected = onMediaSelected,
            )
            Spacer(Modifier.height(22.dp))
        }
    }
}
