package iatheiya.ayana.session

/**
 * The part of [suggestion] that comes next after [draft]: the rest of the line the draft ends
 * in, or, when the draft ends a line, the line break and the line after it. A suggestion of
 * several lines is shown and taken one line at a time; null when it does not extend the draft.
 */
fun suggestionStep(draft: String, suggestion: String?): String? {
    if (suggestion == null || draft.isEmpty() || suggestion.length <= draft.length || !suggestion.startsWith(draft)) {
        return null
    }
    val tail = suggestion.substring(draft.length)
    return if (tail.startsWith("\n")) "\n" + tail.substring(1).substringBefore('\n') else tail.substringBefore('\n')
}

/** Whether more of [suggestion] remains after [draft] and its next [step]. */
fun suggestionHasMore(draft: String, suggestion: String, step: String): Boolean =
    suggestion.length > draft.length + step.length
