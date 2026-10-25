package iatheiya.ayana.session

/**
 * Up and down through the commands sent in a session, newest first, the way a shell walks its
 * history. Each command comes up once, where it was last sent: repeats, in a row or from earlier,
 * add nothing to walk through. The draft being typed is kept while the walk is on, and restored
 * when the walk comes back down past the newest command.
 */
class CommandRecall {

    /** How many commands back the walk is; 0 means the live draft is shown. */
    private var steps = 0

    private var liveDraft = ""

    /**
     * The draft to show after one step, or null when the walk cannot move that way.
     * [history] is oldest first.
     */
    fun step(history: List<String>, draft: String, older: Boolean): String? {
        val distinct = distinctByLastUse(history)
        return walk(distinct, draft, older)
    }

    private fun walk(history: List<String>, draft: String, older: Boolean): String? {
        if (older) {
            if (steps == history.size) return null
            if (steps == 0) liveDraft = draft
            steps++
        } else {
            if (steps == 0) return null
            steps--
        }
        return if (steps == 0) liveDraft else history[history.size - steps]
    }

    /** [history] (oldest first) with each command kept only at its newest place, still oldest first. */
    private fun distinctByLastUse(history: List<String>): List<String> {
        val seen = HashSet<String>()
        return history.asReversed().filter { seen.add(it) }.asReversed()
    }

    /** The walk has not started: the field shows the live draft. */
    val atLive: Boolean get() = steps == 0

    /** Ends the walk: the user is editing a draft again. */
    fun reset() {
        steps = 0
    }
}
