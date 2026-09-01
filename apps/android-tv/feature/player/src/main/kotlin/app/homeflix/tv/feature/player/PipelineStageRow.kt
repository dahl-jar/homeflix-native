package app.homeflix.tv.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import app.homeflix.tv.core.designsystem.HomeflixColors

private val MARKER_SIZE = 28.dp
private val CONNECTOR_GAP = 6.dp
private val CONNECTOR_HEIGHT = 2.dp
private val PENDING_DOT_SIZE = 6.dp
private val ACTIVE_DOT_SIZE = 7.dp
private val ICON_SIZE = 15.dp
private val LABEL_TOP_SPACING = 9.dp
private val LABEL_MIN_HEIGHT = 32.dp
private val SWEEP_WIDTH = 28.dp
private const val ARC_ROTATION_MS = 1_100
private const val SWEEP_MS = 1_400
private const val ENTER_MS = 260
private const val ENTER_SCALE_FROM = 0.9f
private const val ARC_SWEEP_DEGREES = 90f
private const val RING_WIDTH_DP = 2
private const val LABEL_FONT_SIZE = 11
private const val LABEL_LINE_HEIGHT = 15
private val TrackColor = Color.White.copy(alpha = 0.22f)
private val ConnectorColor = Color.White.copy(alpha = 0.16f)
private val ConnectorSettledColor = Color.White.copy(alpha = 0.35f)
private val PendingDotColor = Color.White.copy(alpha = 0.45f)
private val LabelColor = Color(0xFFEEECEB).copy(alpha = 0.58f)
private val MarkerBackground = Color(0xFF141213)
private val MarkerBorder = Color.White.copy(alpha = 0.3f)

@Composable
fun PipelineStageCell(
    stage: PipelineStage,
    isLast: Boolean,
    nextStatus: StageStatus?,
    width: Dp,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReducedMotion()
    val enter = remember(stage.id) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(stage.id, reduceMotion) {
        if (!reduceMotion && enter.value < 1f) {
            enter.animateTo(1f, tween(ENTER_MS, easing = EaseOutQuad))
        }
    }

    Box(
        modifier =
            modifier
                .width(width)
                .graphicsLayer {
                    alpha = enter.value
                    val scale = ENTER_SCALE_FROM + (1f - ENTER_SCALE_FROM) * enter.value
                    scaleX = scale
                    scaleY = scale
                }.semantics { contentDescription = "${stage.label}: ${stage.status.name.lowercase()}" },
    ) {
        if (!isLast) {
            StageConnector(
                width = width,
                settled = stage.status == StageStatus.COMPLETE,
                sweeping = nextStatus == StageStatus.ACTIVE && !reduceMotion,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            StageMarker(status = stage.status, reduceMotion = reduceMotion)
            Text(
                text = stage.label,
                color =
                    when (stage.status) {
                        StageStatus.ACTIVE -> HomeflixColors.OnBackground
                        StageStatus.FAILED -> HomeflixColors.Focus
                        else -> LabelColor
                    },
                fontSize = LABEL_FONT_SIZE.sp,
                lineHeight = LABEL_LINE_HEIGHT.sp,
                fontWeight =
                    if (stage.status == StageStatus.ACTIVE ||
                        stage.status == StageStatus.FAILED
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier =
                    Modifier
                        .padding(top = LABEL_TOP_SPACING, start = 2.dp, end = 2.dp)
                        .heightIn(min = LABEL_MIN_HEIGHT),
            )
        }
    }
}

@Composable
private fun StageMarker(
    status: StageStatus,
    reduceMotion: Boolean,
) {
    val backgroundColor =
        when (status) {
            StageStatus.FAILED -> HomeflixColors.Focus
            StageStatus.COMPLETE -> HomeflixColors.Surface
            else -> MarkerBackground
        }
    val borderColor =
        when (status) {
            StageStatus.ACTIVE -> Color.Transparent
            StageStatus.COMPLETE -> HomeflixColors.GlassBorder
            StageStatus.FAILED -> HomeflixColors.Focus
            StageStatus.PENDING -> MarkerBorder
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(MARKER_SIZE)
                .clip(CircleShape)
                .background(backgroundColor)
                .drawBehind {
                    if (status != StageStatus.ACTIVE) {
                        drawCircle(
                            color = borderColor,
                            style = Stroke(width = RING_WIDTH_DP.dp.toPx()),
                        )
                    }
                },
    ) {
        when (status) {
            StageStatus.PENDING ->
                Box(Modifier.size(PENDING_DOT_SIZE).clip(CircleShape).background(PendingDotColor))
            StageStatus.ACTIVE -> {
                PipelineActiveArc(reduceMotion = reduceMotion)
                Box(Modifier.size(ACTIVE_DOT_SIZE).clip(CircleShape).background(HomeflixColors.OnBackground))
            }
            StageStatus.COMPLETE ->
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = HomeflixColors.OnBackground,
                    modifier = Modifier.size(ICON_SIZE),
                )
            StageStatus.FAILED ->
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = HomeflixColors.OnBackground,
                    modifier = Modifier.size(ICON_SIZE),
                )
        }
    }
}

@Composable
fun PipelineActiveArc(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = MARKER_SIZE,
) {
    val rotation =
        if (reduceMotion) {
            0f
        } else {
            val transition = rememberInfiniteTransition(label = "pipelineArc")
            val animated by
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(ARC_ROTATION_MS, easing = LinearEasing)),
                    label = "pipelineArcRotation",
                )
            animated
        }
    Box(
        modifier =
            modifier
                .size(size)
                .rotate(rotation)
                .drawBehind {
                    val stroke = Stroke(width = RING_WIDTH_DP.dp.toPx())
                    val inset = stroke.width / 2
                    val arcSize =
                        androidx.compose.ui.geometry
                            .Size(this.size.width - stroke.width, this.size.height - stroke.width)
                    if (reduceMotion) {
                        drawCircle(color = HomeflixColors.Focus, style = stroke)
                    } else {
                        drawArc(
                            color = TrackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft =
                                androidx.compose.ui.geometry
                                    .Offset(inset, inset),
                            size = arcSize,
                            style = stroke,
                        )
                        drawArc(
                            color = HomeflixColors.Focus,
                            startAngle = -90f,
                            sweepAngle = ARC_SWEEP_DEGREES,
                            useCenter = false,
                            topLeft =
                                androidx.compose.ui.geometry
                                    .Offset(inset, inset),
                            size = arcSize,
                            style = stroke,
                        )
                    }
                },
    )
}

@Composable
private fun StageConnector(
    width: Dp,
    settled: Boolean,
    sweeping: Boolean,
) {
    val connectorLength = (width - MARKER_SIZE - CONNECTOR_GAP * 2).coerceAtLeast(0.dp)
    Box(
        modifier =
            Modifier
                .offset(x = width / 2 + MARKER_SIZE / 2 + CONNECTOR_GAP, y = MARKER_SIZE / 2 - CONNECTOR_HEIGHT / 2)
                .width(connectorLength)
                .height(CONNECTOR_HEIGHT)
                .clip(CircleShape)
                .background(if (settled) ConnectorSettledColor else ConnectorColor),
    ) {
        if (sweeping) {
            val transition = rememberInfiniteTransition(label = "connectorSweep")
            val travel by
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            tween(SWEEP_MS, easing = EaseInOutQuad),
                            repeatMode = RepeatMode.Restart,
                        ),
                    label = "connectorSweepTravel",
                )
            Box(
                modifier =
                    Modifier
                        .offset(x = (connectorLength + SWEEP_WIDTH) * travel - SWEEP_WIDTH)
                        .width(SWEEP_WIDTH)
                        .height(CONNECTOR_HEIGHT)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.75f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
            )
        }
    }
}

@Composable
fun CrossfadeText(
    value: String,
    color: Color,
    fontSize: Int,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    minHeight: Dp = 0.dp,
) {
    androidx.compose.animation.Crossfade(
        targetState = value,
        label = "crossfadeText",
        modifier = modifier,
    ) { shown ->
        Text(
            text = shown,
            color = color,
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = minHeight).alpha(if (shown.isEmpty()) 0f else 1f),
        )
    }
}
