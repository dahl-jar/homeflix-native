package app.homeflix.tv.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.designsystem.HomeflixStatusScreen
import kotlinx.coroutines.CancellationException

@Composable
fun HomeScreen(
    gateway: HomeGateway,
    viewer: HomeViewer,
    onMediaSelected: (String) -> Unit,
    onProfileSelected: () -> Unit,
    libraries: List<LibrarySummary> = emptyList(),
    onLibrarySelected: (LibrarySummary) -> Unit = {},
) {
    var state by remember(gateway, viewer.id) { mutableStateOf<HomeUiState>(HomeUiState.Loading) }

    LaunchedEffect(gateway, viewer.id) {
        state = loadHome(gateway, viewer.id)
    }

    when (val current = state) {
        HomeUiState.Loading -> HomeSkeleton()
        HomeUiState.Empty ->
            HomeflixStatusScreen("Your library is ready.", "Add media to start watching")
        HomeUiState.Error ->
            HomeflixStatusScreen("Can’t load Homeflix.", "Check the server connection")
        is HomeUiState.Content ->
            HomeCatalog(
                content = current.value,
                viewer = viewer,
                onMediaSelected = onMediaSelected,
                onProfileSelected = onProfileSelected,
                libraries = libraries,
                onLibrarySelected = onLibrarySelected,
            )
    }
}

private suspend fun loadHome(
    gateway: HomeGateway,
    userId: String,
): HomeUiState =
    try {
        val content = gateway.fetchHome(userId)
        if (content.featured.isEmpty() && content.rails.isEmpty()) {
            HomeUiState.Empty
        } else {
            HomeUiState.Content(content)
        }
    } catch (failure: CancellationException) {
        throw failure
    } catch (_: Exception) {
        HomeUiState.Error
    }
