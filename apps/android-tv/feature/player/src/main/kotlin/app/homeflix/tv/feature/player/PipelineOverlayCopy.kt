package app.homeflix.tv.feature.player

internal fun progressMessage(progress: PipelineProgress): String {
    val reason = progress.reason
    val activeLabel = progress.stages.firstOrNull { it.status == StageStatus.ACTIVE }?.label
    return when {
        reason != null -> reason
        activeLabel != null -> activeLabel
        else -> "Preparing playback"
    }
}

internal fun attemptLabel(progress: PipelineProgress): String {
    val sourceAttempt = progress.sourceAttempt
    if (sourceAttempt != null) {
        val total = progress.sourceCount?.let { " of $it" }.orEmpty()
        return "Source $sourceAttempt$total"
    }
    return if (progress.attempt > 1) "Playback attempt ${progress.attempt}" else ""
}
