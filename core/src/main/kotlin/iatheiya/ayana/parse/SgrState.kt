package iatheiya.ayana.parse

import iatheiya.ayana.text.Rgb

/**
 * Current SGR (Select Graphic Rendition) attributes.
 *
 * Every consumer of the byte stream must interpret colour codes identically:
 * a 256-colour index resolves to the same RGB wherever it is rendered. So the
 * interpretation lives here once; each projection maps the resulting
 * attributes to its own representation.
 */
class SgrState {

    var foreground: Rgb? = null
        private set
    var background: Rgb? = null
        private set
    var bold: Boolean = false
        private set
    var dim: Boolean = false
        private set
    var italic: Boolean = false
        private set
    var underline: Boolean = false
        private set
    var reverse: Boolean = false
        private set
    var strikethrough: Boolean = false
        private set

    /** Returns to the unstyled state. */
    fun reset() {
        foreground = null
        background = null
        bold = false
        dim = false
        italic = false
        underline = false
        reverse = false
        strikethrough = false
    }

    /**
     * Applies one SGR sequence body, e.g. `"1;31"` or `"38;5;196"`. An empty
     * body is the SGR shorthand for a full reset (`ESC[m`).
     */
    fun applySgr(parameters: String) {
        if (parameters.isEmpty()) {
            reset()
            return
        }
        val codes = parameters.split(';').mapNotNull { it.toIntOrNull() }
        var i = 0
        while (i < codes.size) {
            val code = codes[i]
            when (code) {
                0 -> reset()
                1 -> bold = true
                2 -> dim = true
                3 -> italic = true
                4 -> underline = true
                7 -> reverse = true
                9 -> strikethrough = true
                21, 22 -> {
                    bold = false
                    dim = false
                }
                23 -> italic = false
                24 -> underline = false
                27 -> reverse = false
                29 -> strikethrough = false
                39 -> foreground = null
                49 -> background = null
                in 30..37 -> foreground = BASIC[code - 30]
                in 90..97 -> foreground = BRIGHT[code - 90]
                in 40..47 -> background = BASIC[code - 40]
                38, 48 -> i = applyExtendedColor(codes, i)
            }
            i++
        }
    }

    /** Handles `38;5;n` and `38;2;r;g;b` (and the 48 background forms). Returns the index of the last code consumed. */
    private fun applyExtendedColor(codes: List<Int>, i: Int): Int {
        val foreground = codes[i] == 38
        if (i + 2 < codes.size && codes[i + 1] == 5) {
            setColor(foreground, fromPalette256(codes[i + 2]))
            return i + 2
        }
        if (i + 4 < codes.size && codes[i + 1] == 2) {
            setColor(foreground, rgb(codes[i + 2], codes[i + 3], codes[i + 4]))
            return i + 4
        }
        return i
    }

    private fun setColor(isForeground: Boolean, color: Rgb) {
        if (isForeground) foreground = color else background = color
    }

    companion object {
        /** Standard colours 0-7, shared by foreground (30-37) and background (40-47). */
        private val BASIC = arrayOf(
            Rgb(0xFF000000), Rgb(0xFFCC0000), Rgb(0xFF00CC00), Rgb(0xFFCCCC00),
            Rgb(0xFF0000CC), Rgb(0xFFCC00CC), Rgb(0xFF00CCCC), Rgb(0xFFCCCCCC),
        )

        /** Bright foreground colours 8-15 (90-97). */
        private val BRIGHT = arrayOf(
            Rgb(0xFF777777), Rgb(0xFFFF5555), Rgb(0xFF55FF55), Rgb(0xFFFFFF55),
            Rgb(0xFF5555FF), Rgb(0xFFFF55FF), Rgb(0xFF55FFFF), Rgb(0xFFFFFFFF),
        )

        /** xterm's default 0-15 palette, used by the palette lookup below. */
        private val SYSTEM_PALETTE = longArrayOf(
            0xFF000000, 0xFF800000, 0xFF008000, 0xFF808000,
            0xFF000080, 0xFF800080, 0xFF008080, 0xFFC0C0C0,
            0xFF808080, 0xFFFF0000, 0xFF00FF00, 0xFFFFFF00,
            0xFF0000FF, 0xFFFF00FF, 0xFF00FFFF, 0xFFFFFFFF,
        )

        /** xterm's six intensity levels for the 6x6x6 colour cube. */
        private val CUBE = intArrayOf(0, 95, 135, 175, 215, 255)

        /**
         * Resolves an xterm-256 palette index: 0-15 system colours, 16-231 the
         * 6x6x6 cube, 232-255 the greyscale ramp.
         */
        fun fromPalette256(n: Int): Rgb {
            val index = n.coerceIn(0, 255)
            return when {
                index < 16 -> Rgb(SYSTEM_PALETTE[index])
                index < 232 -> {
                    val cube = index - 16
                    rgb(CUBE[cube / 36], CUBE[(cube % 36) / 6], CUBE[cube % 6])
                }
                else -> {
                    val gray = (index - 232) * 10 + 8
                    rgb(gray, gray, gray)
                }
            }
        }

        private fun rgb(r: Int, g: Int, b: Int): Rgb =
            Rgb(
                0xFF000000L or
                    (r.coerceIn(0, 255).toLong() shl 16) or
                    (g.coerceIn(0, 255).toLong() shl 8) or
                    b.coerceIn(0, 255).toLong(),
            )
    }
}
