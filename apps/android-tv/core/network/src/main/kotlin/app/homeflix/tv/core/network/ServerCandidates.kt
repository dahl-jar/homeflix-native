package app.homeflix.tv.core.network

fun parseServerCandidates(value: String?): List<String> =
    value
        .orEmpty()
        .split(',')
        .map { candidate -> candidate.trim().trimEnd('/') }
        .filter(String::isNotEmpty)

fun resolveServer(
    candidates: List<String>,
    probe: (String) -> Boolean,
): String? = candidates.firstOrNull(probe)
