package app.homeflix.tv.feature.player

private const val MINIMUM_SEGMENT_TICKS = 10_000_000L

enum class SegmentType {
    INTRO,
    RECAP,
    OUTRO,
}

data class SkipSegment(
    val id: String,
    val type: SegmentType,
    val startTicks: Long,
    val endTicks: Long,
)

private fun segmentType(name: String?): SegmentType? =
    when (name) {
        "Intro" -> SegmentType.INTRO
        "Recap" -> SegmentType.RECAP
        "Outro" -> SegmentType.OUTRO
        else -> null
    }

fun normalizeSegments(segments: List<SegmentDto>): List<SkipSegment> =
    segments.mapNotNull(::validSegment).sortedBy(SkipSegment::startTicks)

private fun validSegment(dto: SegmentDto): SkipSegment? {
    val type = segmentType(dto.type)
    val id = dto.id?.takeIf(String::isNotEmpty)
    val bounds = segmentBounds(dto)
    return if (type != null && id != null && bounds != null) {
        SkipSegment(id = id, type = type, startTicks = bounds.first, endTicks = bounds.second)
    } else {
        null
    }
}

private fun segmentBounds(dto: SegmentDto): Pair<Long, Long>? {
    val startTicks = dto.startTicks
    val endTicks = dto.endTicks
    return when {
        startTicks == null || endTicks == null -> null
        startTicks < 0 || endTicks - startTicks < MINIMUM_SEGMENT_TICKS -> null
        else -> startTicks to endTicks
    }
}

fun activeSegment(
    segments: List<SkipSegment>,
    positionTicks: Long,
    dismissedIds: Set<String>,
): SkipSegment? =
    segments.firstOrNull { segment ->
        segment.id !in dismissedIds &&
            positionTicks >= segment.startTicks &&
            positionTicks < segment.endTicks
    }
