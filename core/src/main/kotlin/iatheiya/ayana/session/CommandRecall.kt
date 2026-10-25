package iatheiya.ayana.session

/**
 * Up and down through one session's drafts and the commands sent, newest first, the way a shell
 * walks its history.
 *
 * - Up walks back: first through the drafts set aside here (newest first), then through the
 *   commands sent in any session. Each text comes up once, at its newest place: repeats add
 *   nothing to walk through.
 * - Down walks forward again. On the live draft, a written draft is set aside and the field is
 *   cleared, so several drafts can be written one after another and found again with Up.
 * - Up on a written live draft sets it aside too, so it is never lost by looking back.
 * - An empty field is never set aside, and nothing is kept of text erased by hand.
 * - Sending ends it all: the drafts set aside are dropped.
 *
 * With [setsAside] off, the field is never cleared: the live draft is kept while the walk is on
 * and comes back when the walk returns past the newest command, as a shell does.
 */
class CommandRecall {

    /** Down and Up set written drafts aside (a setting). */
    var setsAside: Boolean = true

    /** How many entries back the walk is; 0 means the live draft is shown. */
    private var steps = 0

    private var liveDraft = ""

    /** Drafts set aside in this session, oldest first. */
    private val aside = ArrayList<String>()

    /** The text to show after Up, or null when there is nothing further back. [history] is oldest first. */
    fun up(history: List<String>, draft: String): String? {
        if (steps == 0) {
            if (setsAside && draft.isNotBlank()) {
                aside.add(draft)
                liveDraft = ""
                // The draft just set aside is the newest entry and already on screen: show the one before it.
                val entries = entries(history)
                if (entries.size < 2) return null
                steps = 2
                return entries[entries.size - 2]
            }
            liveDraft = draft
        }
        val entries = entries(history)
        if (steps >= entries.size) return null
        steps++
        return entries[entries.size - steps]
    }

    /** The text to show after Down, or null when nothing changes. */
    fun down(history: List<String>, draft: String): String? {
        if (steps == 0) {
            if (!setsAside || draft.isBlank()) return null
            aside.add(draft)
            return ""
        }
        steps--
        return if (steps == 0) liveDraft else entries(history).let { it[it.size - steps] }
    }

    /** The user edits the field again: the walk starts over from the live draft. */
    fun reset() {
        steps = 0
    }

    /** Something was sent: the walk starts over and the drafts set aside are dropped. */
    fun sent() {
        steps = 0
        aside.clear()
    }

    /** What the walk goes through, oldest first: history, then the drafts set aside, each once. */
    private fun entries(history: List<String>): List<String> {
        val seen = HashSet<String>()
        return (history + aside).asReversed().filter { seen.add(it) }.asReversed()
    }
}
