package app.homeflix.tv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.catalog.LibrarySummary
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixDimensions
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
        HomeUiState.Empty -> HomeStatus("Your library is ready.", "Add media to start watching")
        HomeUiState.Error -> HomeStatus("Can’t load Homeflix.", "Check the server connection")
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

@Composable
private fun HomeStatus(
    title: String,
    detail: String?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(HomeflixColors.Background),
    ) {
        Text(
            text = "HOMEFLIX",
            color = HomeflixColors.Focus,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(HomeflixDimensions.WordmarkEdgePadding),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = HomeflixColors.OnBackground,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
            )
            if (detail != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = detail,
                    color = HomeflixColors.Muted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
