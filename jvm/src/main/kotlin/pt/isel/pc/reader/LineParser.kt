package pt.isel.pc.reader

import pt.isel.pc.utils.NodeLinkedList
import java.nio.CharBuffer

/**
 * Receives CharBuffers and provides Strings, partitioned by line breaks
 */
class LineParser {

    // Holds the line being parsed
    private val stringBuilder = StringBuilder()

    // Holds the already parsed lines
    private val lines = NodeLinkedList<String>()

    // The previous char, if it is a terminator
    private var lastTerminator: Char? = null

    // Provide a sequence of chars to the parser
    fun offer(chars: CharBuffer) {
        while (chars.position() != chars.limit()) {
            offer(chars.get())
        }
    }

    /**
     * Checks if a string is available, and returns it if so.
     */
    fun poll(): String? = if (lines.notEmpty) {
        lines.pull().value
    } else {
        null
    }

    private fun offer(char: Char) {
        if (isTerminator(char)) {
            if (lastTerminator == null || lastTerminator == char) {
                // assume that this terminator is ending a line
                extractLine()
                lastTerminator = char
            } else {
                // merge this with last terminator
                lastTerminator = null
            }
        } else {
            stringBuilder.append(char)
            lastTerminator = null
        }
    }

    private fun extractLine() {
        lines.enqueue(stringBuilder.toString())
        stringBuilder.clear()
    }

    companion object {
        fun isTerminator(c: Char) = c == '\n' || c == '\r'
    }
}