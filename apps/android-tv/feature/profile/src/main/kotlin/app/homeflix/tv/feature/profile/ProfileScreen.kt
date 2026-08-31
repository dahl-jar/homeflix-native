package app.homeflix.tv.feature.profile

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.designsystem.HomeflixScreenBackground
import app.homeflix.tv.core.designsystem.TvNavEntry
import app.homeflix.tv.core.designsystem.TvNavProfile
import app.homeflix.tv.core.designsystem.TvNavigationRail
import app.homeflix.tv.core.designsystem.libraryNavIcon
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Composable
fun ProfileScreen(
    details: ProfileDetails,
    profile: TvNavProfile,
    libraries: List<LibrarySummary>,
    onHomeSelected: () -> Unit,
    onLibrarySelected: (LibrarySummary) -> Unit,
    onSwitchProfile: () -> Unit,
    onChangeServer: () -> Unit = {},
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val contentFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeflixScreenBackground()
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = CONTENT_START_PADDING,
                        top = CONTENT_TOP_PADDING,
                        end = CONTENT_END_PADDING,
                        bottom = CONTENT_END_PADDING,
                    ).focusGroup(),
        ) {
            IdentityPane(
                details = details,
                switchFocusRequester = contentFocusRequester,
                onSwitchProfile = onSwitchProfile,
            )
            Spacer(Modifier.width(PANE_SPACING))
            SettingsPane(
                details = details,
                ioDispatcher = ioDispatcher,
                onChangeServer = onChangeServer,
                modifier = Modifier.weight(1f),
            )
        }
        TvNavigationRail(
            profile = profile,
            entries = profileNavEntries(libraries),
            contentFocusRequester = contentFocusRequester,
            onEntrySelected = { entryId ->
                when {
                    entryId == HOME_ENTRY_ID -> onHomeSelected()
                    else ->
                        libraries.firstOrNull { candidate -> candidate.id == entryId }?.let(onLibrarySelected)
                }
            },
            onProfileSelected = {},
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

private fun profileNavEntries(libraries: List<LibrarySummary>): List<TvNavEntry> =
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

private const val HOME_ENTRY_ID = "home"
private val CONTENT_START_PADDING = 100.dp
private val CONTENT_TOP_PADDING = 80.dp
private val CONTENT_END_PADDING = 48.dp
private val PANE_SPACING = 64.dp
