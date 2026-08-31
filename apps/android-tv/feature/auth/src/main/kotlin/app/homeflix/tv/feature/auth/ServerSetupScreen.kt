package app.homeflix.tv.feature.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.homeflix.tv.core.designsystem.HomeflixColors
import app.homeflix.tv.core.designsystem.HomeflixDimensions
import app.homeflix.tv.core.designsystem.HomeflixScreenBackground
import app.homeflix.tv.core.designsystem.HomeflixWordmark
import app.homeflix.tv.core.designsystem.TvFocusAppearance
import app.homeflix.tv.core.designsystem.TvFocusSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ServerSetupScreen(
    initialUrl: String,
    onConnect: suspend (String) -> ServerConnectError?,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val entry = rememberServerEntry(initialUrl, onConnect)
    val fieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(fieldFocusRequester) {
        fieldFocusRequester.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeflixScreenBackground()
        HomeflixWordmark(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(HomeflixDimensions.WordmarkEdgePadding),
        )
        if (onBack != null) {
            AuthBackButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = BACK_BUTTON_START_PADDING, top = BACK_BUTTON_TOP_PADDING),
            )
        }
        ServerEntryContent(
            entry = entry,
            fieldFocusRequester = fieldFocusRequester,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

private class ServerEntry(
    val url: String,
    val error: ServerConnectError?,
    val isConnecting: Boolean,
    val onUrlChange: (String) -> Unit,
    val onSubmit: () -> Unit,
)

@Composable
private fun rememberServerEntry(
    initialUrl: String,
    onConnect: suspend (String) -> ServerConnectError?,
): ServerEntry {
    var url by remember(initialUrl) { mutableStateOf(initialUrl) }
    var error by remember { mutableStateOf<ServerConnectError?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    return ServerEntry(
        url = url,
        error = error,
        isConnecting = isConnecting,
        onUrlChange = { value ->
            url = value
            error = null
        },
        onSubmit = {
            if (!isConnecting) {
                isConnecting = true
                error = null
                scope.launch {
                    error =
                        try {
                            onConnect(url)
                        } catch (failure: CancellationException) {
                            throw failure
                        } catch (_: Exception) {
                            ServerConnectError.Unreachable
                        }
                    isConnecting = false
                }
            }
        },
    )
}

@Composable
private fun ServerEntryContent(
    entry: ServerEntry,
    fieldFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = "Connect to your server",
            color = HomeflixColors.OnBackground,
            fontSize = TITLE_FONT_SIZE,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(HINT_SPACING))
        Text(
            text = "Enter the full address",
            color = HomeflixColors.Muted,
            fontSize = HINT_FONT_SIZE,
        )
        Spacer(Modifier.height(FIELD_SPACING))
        ServerUrlField(
            url = entry.url,
            onUrlChange = entry.onUrlChange,
            onSubmit = entry.onSubmit,
            focusRequester = fieldFocusRequester,
        )
        Spacer(Modifier.height(ERROR_SPACING))
        Text(
            text = entry.error?.let(::errorMessage).orEmpty(),
            color = HomeflixColors.Error,
            fontSize = ERROR_FONT_SIZE,
            modifier = Modifier.height(ERROR_LINE_HEIGHT),
        )
        Spacer(Modifier.height(BUTTON_SPACING))
        ConnectButton(
            isConnecting = entry.isConnecting,
            onClick = entry.onSubmit,
        )
    }
}

@Composable
private fun ServerUrlField(
    url: String,
    onUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) HomeflixColors.Focus else HomeflixColors.PinBorder

    BasicTextField(
        value = url,
        onValueChange = onUrlChange,
        singleLine = true,
        textStyle =
            TextStyle(
                color = HomeflixColors.OnBackground,
                fontSize = FIELD_FONT_SIZE,
            ),
        cursorBrush = SolidColor(HomeflixColors.Focus),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
            ),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        decorationBox = { innerField ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier =
                    Modifier
                        .size(width = FIELD_WIDTH, height = FIELD_HEIGHT)
                        .border(FIELD_BORDER_WIDTH, borderColor, FieldShape)
                        .padding(horizontal = FIELD_HORIZONTAL_PADDING),
            ) {
                if (url.isEmpty()) {
                    Text(
                        text = "https://192.168.1.10:8096",
                        color = HomeflixColors.Muted,
                        fontSize = FIELD_FONT_SIZE,
                    )
                }
                innerField()
            }
        },
        modifier =
            Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                .semantics { contentDescription = "Server address" },
    )
}

@Composable
private fun ConnectButton(
    isConnecting: Boolean,
    onClick: () -> Unit,
) {
    TvFocusSurface(
        contentDescription = "Connect",
        onClick = { if (!isConnecting) onClick() },
        appearance =
            TvFocusAppearance(
                shape = FieldShape,
                backgroundColor = Color.White,
            ),
        modifier = Modifier.size(width = BUTTON_WIDTH, height = BUTTON_HEIGHT),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = if (isConnecting) "Connecting…" else "Connect",
                color = PrimaryButtonTextColor,
                fontSize = BUTTON_FONT_SIZE,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun errorMessage(error: ServerConnectError): String =
    when (error) {
        ServerConnectError.InvalidUrl -> "Enter the full address, starting with http:// or https://"
        ServerConnectError.Unreachable -> "Can’t reach a server at this address"
    }

private val PrimaryButtonTextColor = Color(0xFF141414)
private val FieldShape = RoundedCornerShape(8.dp)
private val BACK_BUTTON_START_PADDING = 16.dp
private val BACK_BUTTON_TOP_PADDING = 62.dp
private val HINT_SPACING = 12.dp
private val FIELD_SPACING = 36.dp
private val ERROR_SPACING = 14.dp
private val ERROR_LINE_HEIGHT = 20.dp
private val BUTTON_SPACING = 10.dp
private val FIELD_WIDTH = 420.dp
private val FIELD_HEIGHT = 52.dp
private val FIELD_BORDER_WIDTH = 1.dp
private val FIELD_HORIZONTAL_PADDING = 16.dp
private val BUTTON_WIDTH = 180.dp
private val BUTTON_HEIGHT = 46.dp
private val TITLE_FONT_SIZE = 28.sp
private val HINT_FONT_SIZE = 15.sp
private val FIELD_FONT_SIZE = 17.sp
private val ERROR_FONT_SIZE = 14.sp
private val BUTTON_FONT_SIZE = 16.sp
