package com.athea.app.screen

/** What a full-screen program shows now: its rows as plain text, and where the cursor is. */
data class ScreenSnapshot(
    val rows: List<String>,
    val cursorRow: Int,
    val cursorCol: Int,
    val cursorVisible: Boolean,
)

/**
 * The screen a program draws over the whole terminal, such as vim, top or less. The shell output
 * is shown as a transcript; while a program uses the alternate screen, its grid is shown instead.
 *
 * It implements the part of VT100 and xterm that such programs rely on: cursor movement, erasing,
 * scrolling regions, inserting and deleting lines, wrapping and the alternate screen. Colours and
 * text attributes are dropped, because the grid shows plain characters.
 */
class TerminalScreen(rows: Int, cols: Int) {

    private var rowCount = rows.coerceAtLeast(1)
    private var colCount = cols.coerceAtLeast(1)
    private var grid = blankGrid(rowCount, colCount)
    private var mainGrid: Array<CharArray>? = null
    private var mainCursor = 0 to 0
    private var savedCursor = 0 to 0
    private var cursorRow = 0
    private var cursorCol = 0
    private var wrapPending = false
    private var scrollTop = 0
    private var scrollBottom = rowCount - 1
    private var cursorVisible = true
    private var state = State.TEXT
    private val params = StringBuilder()
    private var carry = ByteArray(0)

    /** True while a program uses the alternate screen. */
    var altActive = false
        private set

    fun feed(bytes: ByteArray) {
        val input = if (carry.isEmpty()) bytes else carry + bytes
        val complete = completeLength(input)
        carry = input.copyOfRange(complete, input.size)
        for (ch in String(input, 0, complete, Charsets.UTF_8)) step(ch)
    }

    fun resize(rows: Int, cols: Int) {
        rowCount = rows.coerceAtLeast(1)
        colCount = cols.coerceAtLeast(1)
        grid = resized(grid)
        mainGrid = mainGrid?.let { resized(it) }
        scrollTop = 0
        scrollBottom = rowCount - 1
        moveTo(cursorRow, cursorCol)
    }

    fun snapshot(): ScreenSnapshot = ScreenSnapshot(
        rows = grid.map { String(it).trimEnd() },
        cursorRow = cursorRow,
        cursorCol = cursorCol,
        cursorVisible = cursorVisible,
    )

    // ------------------------------------------------------------- decoding

    /** The length of the prefix that ends on a whole UTF-8 character; the rest waits for the next chunk. */
    private fun completeLength(bytes: ByteArray): Int {
        var index = bytes.size - 1
        var continuation = 0
        while (index >= 0 && continuation < MAX_UTF8_BYTES) {
            val b = bytes[index].toInt() and 0xFF
            if ((b and 0xC0) == 0x80) {
                index--
                continuation++
                continue
            }
            val needed = when {
                b >= 0xF0 -> 4
                b >= 0xE0 -> 3
                b >= 0xC0 -> 2
                else -> 1
            }
            return if (bytes.size - index < needed) index else bytes.size
        }
        return bytes.size
    }

    // ------------------------------------------------------------ parsing

    private fun step(ch: Char) {
        when (state) {
            State.TEXT -> text(ch)
            State.ESC -> escape(ch)
            State.CSI -> csi(ch)
            State.OSC -> when (ch) {
                BEL -> state = State.TEXT
                ESC -> state = State.OSC_ESC
                else -> Unit
            }
            State.OSC_ESC, State.CHARSET -> state = State.TEXT
        }
    }

    private fun text(ch: Char) {
        when (ch) {
            ESC -> state = State.ESC
            '\r' -> {
                cursorCol = 0
                wrapPending = false
            }
            '\n', '\u000B', '\u000C' -> lineFeed()
            '\b' -> {
                if (cursorCol > 0) cursorCol--
                wrapPending = false
            }
            '\t' -> {
                cursorCol = (((cursorCol / TAB_WIDTH) + 1) * TAB_WIDTH).coerceAtMost(colCount - 1)
                wrapPending = false
            }
            else -> if (ch >= ' ' && ch != DEL) put(ch)
        }
    }

    private fun escape(ch: Char) {
        state = State.TEXT
        when (ch) {
            '[' -> {
                params.setLength(0)
                state = State.CSI
            }
            ']' -> state = State.OSC
            '(', ')' -> state = State.CHARSET
            '7' -> savedCursor = cursorRow to cursorCol
            '8' -> moveTo(savedCursor.first, savedCursor.second)
            'D' -> lineFeed()
            'E' -> {
                cursorCol = 0
                lineFeed()
            }
            'M' -> reverseIndex()
            else -> Unit
        }
    }

    private fun csi(ch: Char) {
        when (ch) {
            in '0'..'?' -> params.append(ch)
            in ' '..'/' -> Unit
            in '@'..'~' -> {
                state = State.TEXT
                command(ch, params.toString())
            }
            else -> state = State.TEXT
        }
    }

    private fun command(finalChar: Char, raw: String) {
        val isPrivate = raw.startsWith('?')
        val args = raw.removePrefix("?").split(';').map { it.toIntOrNull() }
        fun arg(index: Int, default: Int): Int = args.getOrNull(index)?.takeIf { it > 0 } ?: default

        when (finalChar) {
            'A' -> moveTo(cursorRow - arg(0, 1), cursorCol)
            'B', 'e' -> moveTo(cursorRow + arg(0, 1), cursorCol)
            'C', 'a' -> moveTo(cursorRow, cursorCol + arg(0, 1))
            'D' -> moveTo(cursorRow, cursorCol - arg(0, 1))
            'E' -> moveTo(cursorRow + arg(0, 1), 0)
            'F' -> moveTo(cursorRow - arg(0, 1), 0)
            'G', '`' -> moveTo(cursorRow, arg(0, 1) - 1)
            'd' -> moveTo(arg(0, 1) - 1, cursorCol)
            'H', 'f' -> moveTo(arg(0, 1) - 1, arg(1, 1) - 1)
            'J' -> eraseDisplay(args.getOrNull(0) ?: 0)
            'K' -> eraseLine(args.getOrNull(0) ?: 0)
            'L' -> if (cursorRow in scrollTop..scrollBottom) {
                scrollDown(cursorRow, scrollBottom, arg(0, 1))
                cursorCol = 0
            }
            'M' -> if (cursorRow in scrollTop..scrollBottom) {
                scrollUp(cursorRow, scrollBottom, arg(0, 1))
                cursorCol = 0
            }
            'S' -> scrollUp(scrollTop, scrollBottom, arg(0, 1))
            'T' -> scrollDown(scrollTop, scrollBottom, arg(0, 1))
            '@' -> insertBlanks(arg(0, 1))
            'P' -> deleteChars(arg(0, 1))
            'X' -> clearCells(cursorRow, cursorCol, cursorCol + arg(0, 1))
            'r' -> setScrollRegion(arg(0, 1) - 1, arg(1, rowCount) - 1)
            's' -> savedCursor = cursorRow to cursorCol
            'u' -> moveTo(savedCursor.first, savedCursor.second)
            'h', 'l' -> if (isPrivate) args.forEach { setPrivateMode(it, finalChar == 'h') }
            else -> Unit
        }
    }

    private fun setPrivateMode(mode: Int?, on: Boolean) {
        when (mode) {
            1047, 1049, 47 -> if (on) enterAltScreen() else leaveAltScreen()
            25 -> cursorVisible = on
            else -> Unit
        }
    }

    // ------------------------------------------------------------ editing

    private fun put(ch: Char) {
        if (wrapPending) {
            wrapPending = false
            cursorCol = 0
            lineFeed()
        }
        grid[cursorRow][cursorCol] = ch
        if (cursorCol == colCount - 1) wrapPending = true else cursorCol++
    }

    private fun lineFeed() {
        when {
            cursorRow == scrollBottom -> scrollUp(scrollTop, scrollBottom, 1)
            cursorRow < rowCount - 1 -> cursorRow++
        }
        wrapPending = false
    }

    private fun reverseIndex() {
        if (cursorRow == scrollTop) {
            scrollDown(scrollTop, scrollBottom, 1)
        } else if (cursorRow > 0) {
            cursorRow--
        }
        wrapPending = false
    }

    private fun moveTo(row: Int, col: Int) {
        cursorRow = row.coerceIn(0, rowCount - 1)
        cursorCol = col.coerceIn(0, colCount - 1)
        wrapPending = false
    }

    private fun setScrollRegion(top: Int, bottom: Int) {
        if (top in 0..bottom && bottom < rowCount) {
            scrollTop = top
            scrollBottom = bottom
            moveTo(0, 0)
        }
    }

    private fun scrollUp(top: Int, bottom: Int, n: Int) {
        val count = n.coerceAtMost(bottom - top + 1)
        for (row in top..bottom - count) grid[row] = grid[row + count]
        for (row in bottom - count + 1..bottom) grid[row] = blankRow()
    }

    private fun scrollDown(top: Int, bottom: Int, n: Int) {
        val count = n.coerceAtMost(bottom - top + 1)
        for (row in bottom downTo top + count) grid[row] = grid[row - count]
        for (row in top until top + count) grid[row] = blankRow()
    }

    private fun insertBlanks(n: Int) {
        val row = grid[cursorRow]
        for (col in colCount - 1 downTo cursorCol + n) row[col] = row[col - n]
        clearCells(cursorRow, cursorCol, cursorCol + n)
    }

    private fun deleteChars(n: Int) {
        val row = grid[cursorRow]
        for (col in cursorCol until colCount - n) row[col] = row[col + n]
        clearCells(cursorRow, (colCount - n).coerceAtLeast(cursorCol), colCount)
    }

    private fun eraseDisplay(mode: Int) {
        when (mode) {
            0 -> {
                clearCells(cursorRow, cursorCol, colCount)
                for (row in cursorRow + 1 until rowCount) clearCells(row, 0, colCount)
            }
            1 -> {
                for (row in 0 until cursorRow) clearCells(row, 0, colCount)
                clearCells(cursorRow, 0, cursorCol + 1)
            }
            else -> for (row in 0 until rowCount) clearCells(row, 0, colCount)
        }
    }

    private fun eraseLine(mode: Int) {
        when (mode) {
            0 -> clearCells(cursorRow, cursorCol, colCount)
            1 -> clearCells(cursorRow, 0, cursorCol + 1)
            else -> clearCells(cursorRow, 0, colCount)
        }
    }

    private fun clearCells(row: Int, from: Int, to: Int) {
        for (col in from until to.coerceAtMost(colCount)) grid[row][col] = ' '
    }

    private fun enterAltScreen() {
        if (altActive) return
        mainGrid = grid
        mainCursor = cursorRow to cursorCol
        grid = blankGrid(rowCount, colCount)
        altActive = true
        scrollTop = 0
        scrollBottom = rowCount - 1
        moveTo(0, 0)
    }

    private fun leaveAltScreen() {
        val main = mainGrid ?: return
        grid = main
        mainGrid = null
        altActive = false
        scrollTop = 0
        scrollBottom = rowCount - 1
        moveTo(mainCursor.first, mainCursor.second)
    }

    private fun blankRow(): CharArray = CharArray(colCount) { ' ' }

    private fun resized(old: Array<CharArray>): Array<CharArray> =
        Array(rowCount) { row -> CharArray(colCount) { col -> old.getOrNull(row)?.getOrNull(col) ?: ' ' } }

    private enum class State { TEXT, ESC, CSI, OSC, OSC_ESC, CHARSET }

    private companion object {
        const val ESC = '\u001B'
        const val BEL = '\u0007'
        const val DEL = '\u007F'
        const val TAB_WIDTH = 8
        const val MAX_UTF8_BYTES = 4

        fun blankGrid(rows: Int, cols: Int): Array<CharArray> = Array(rows) { CharArray(cols) { ' ' } }
    }
}
