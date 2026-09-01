package app.homeflix.tv.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import app.homeflix.tv.core.designsystem.HomeflixColors
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign as OverlayTextAlign

private const val BACKDROP_ZOOM_FROM = 1.05f
private const val BACKDROP_ZOOM_MS = 7_000
private const val BLOCK_ENTER_MS = 450
private const val BLOCK_STAGGER_MS = 60L
private const val BLOCK_RISE_PX = 8f
private const val BLOCK_COUNT = 4
private const val EYEBROW_BLOCK = 0
private const val TITLE_BLOCK = 1
private const val STAGES_BLOCK = 2
private const val STATUS_BLOCK = 3
private const val EXIT_MS = 300
private const val REDUCED_ENTER_MS = 200
private const val WAIT_TICK_MS = 1_000L
private const val EYEBROW_FONT_SIZE = 13
private const val TITLE_FONT_SIZE = 30
private const val MESSAGE_FONT_SIZE = 16
private const val ATTEMPT_FONT_SIZE = 13
private const val EYEBROW_LETTER_SPACING = 2.1
private val CONTENT_MAX_WIDTH = 820.dp
private val STAGE_ROW_TOP_SPACING = 34.dp
private val MESSAGE_TOP_SPACING = 20.dp
private val ATTEMPT_TOP_SPACING = 4.dp
private val REASSURANCE_TOP_SPACING = 10.dp
private val TITLE_TOP_SPACING = 10.dp
private val HORIZONTAL_PADDING = 80.dp
private val CONTENT_HORIZONTAL_INSET = HORIZONTAL_PADDING * 2
private val ScrimTop = Color(0xFF080708).copy(alpha = 0.7f)
private val ScrimMiddle = Color(0xFF080708).copy(alpha = 0.82f)
private val ScrimBottom = Color(0xFF080708).copy(alpha = 0.96f)
private const val SCRIM_MIDDLE_STOP = 0.52f
private val ReassuranceColor = Color(0xFFEEECEB).copy(alpha = 0.5f)

@Composable
fun PipelineOverlay(
    itemName: String,
    backdropUrl: String?,
    progress: PipelineProgress,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = progress.visible,
        enter = fadeIn(tween(REDUCED_ENTER_MS)),
        exit = fadeOut(tween(EXIT_MS)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            OverlayBackdrop(backdropUrl)
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to ScrimTop,
                                SCRIM_MIDDLE_STOP to ScrimMiddle,
                                1f to ScrimBottom,
                            ),
                        ),
            )
            OverlayContent(itemName = itemName, progress = progress)
        }
    }
}

@Composable
private fun OverlayBackdrop(backdropUrl: String?) {
    if (backdropUrl == null) return
    val reduceMotion = rememberReducedMotion()
    val scale = remember { Animatable(if (reduceMotion) 1f else BACKDROP_ZOOM_FROM) }
    LaunchedEffect(reduceMotion) {
        if (!reduceMotion && scale.value > 1f) {
            scale.animateTo(1f, tween(BACKDROP_ZOOM_MS, easing = EaseOutQuad))
        }
    }
    AsyncImage(
        model = backdropUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    )
}

@Composable
private fun OverlayContent(
    itemName: String,
    progress: PipelineProgress,
) {
    val reduceMotion = rememberReducedMotion()
    val blocks = remember { List(BLOCK_COUNT) { Animatable(0f) } }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            blocks.forEach { it.snapTo(1f) }
        } else {
            blocks.forEachIndexed { index, block ->
                if (index > 0) delay(BLOCK_STAGGER_MS)
                block.animateTo(1f, tween(BLOCK_ENTER_MS, easing = EaseOutCubic))
            }
        }
    }
    val reassurance = rememberStepReassurance(progress)
    val configuration = LocalConfiguration.current
    val viewportWidth =
        (configuration.screenWidthDp.dp - CONTENT_HORIZONTAL_INSET)
            .coerceAtMost(CONTENT_MAX_WIDTH - CONTENT_HORIZONTAL_INSET)
    val layout = pipelineStageLayout(viewportWidth.value, progress.stages.size)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = HORIZONTAL_PADDING),
    ) {
        EntranceBlock(blocks[EYEBROW_BLOCK]) { OverlayEyebrow() }
        EntranceBlock(blocks[TITLE_BLOCK]) { OverlayTitle(itemName) }
        EntranceBlock(blocks[STAGES_BLOCK]) {
            StageRow(
                progress = progress,
                stageWidth = layout.stageWidth,
                centered = layout.centered,
                modifier = Modifier.padding(top = STAGE_ROW_TOP_SPACING),
            )
        }
        EntranceBlock(blocks[STATUS_BLOCK]) {
            OverlayStatusColumn(progress = progress, reassurance = reassurance)
        }
    }
}

@Composable
private fun OverlayEyebrow() {
    Text(
        text = "PREPARING PLAYBACK",
        color = HomeflixColors.Muted,
        fontSize = EYEBROW_FONT_SIZE.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = EYEBROW_LETTER_SPACING.sp,
    )
}

@Composable
private fun OverlayTitle(itemName: String) {
    Text(
        text = itemName,
        color = HomeflixColors.OnBackground,
        fontSize = TITLE_FONT_SIZE.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = OverlayTextAlign.Center,
        maxLines = 2,
        modifier = Modifier.padding(top = TITLE_TOP_SPACING).widthIn(max = CONTENT_MAX_WIDTH),
    )
}

@Composable
private fun OverlayStatusColumn(
    progress: PipelineProgress,
    reassurance: String?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CrossfadeText(
            value = progressMessage(progress),
            color = if (progress.reason != null) HomeflixColors.Error else HomeflixColors.OnBackground,
            fontSize = MESSAGE_FONT_SIZE,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = MESSAGE_TOP_SPACING),
        )
        CrossfadeText(
            value = attemptLabel(progress),
            color = HomeflixColors.Muted,
            fontSize = ATTEMPT_FONT_SIZE,
            modifier = Modifier.padding(top = ATTEMPT_TOP_SPACING),
        )
        CrossfadeText(
            value = reassurance.orEmpty(),
            color = ReassuranceColor,
            fontSize = ATTEMPT_FONT_SIZE,
            modifier = Modifier.padding(top = REASSURANCE_TOP_SPACING),
        )
    }
}

@Composable
private fun StageRow(
    progress: PipelineProgress,
    stageWidth: Float,
    centered: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (centered) Modifier else Modifier.horizontalScroll(rememberScrollState())),
    ) {
        progress.stages.forEachIndexed { index, stage ->
            PipelineStageCell(
                stage = stage,
                isLast = index == progress.stages.lastIndex,
                nextStatus = progress.stages.getOrNull(index + 1)?.status,
                width = stageWidth.dp,
            )
        }
    }
}

@Composable
private fun EntranceBlock(
    animation: Animatable<Float, *>,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier.graphicsLayer {
                alpha = animation.value
                translationY = BLOCK_RISE_PX * (1f - animation.value)
            },
    ) {
        content()
    }
}

@Composable
private fun rememberStepReassurance(progress: PipelineProgress): String? {
    val activeStageId = progress.stages.firstOrNull { it.status == StageStatus.ACTIVE }?.id
    val stepKey = "$activeStageId:${progress.sourceAttempt}:${progress.attempt}"
    var stepStartedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(stepKey) {
        stepStartedAt = System.currentTimeMillis()
        message = null
        while (true) {
            delay(WAIT_TICK_MS)
            message = pipelineWaitReassurance(System.currentTimeMillis() - stepStartedAt)
        }
    }
    return message
}
