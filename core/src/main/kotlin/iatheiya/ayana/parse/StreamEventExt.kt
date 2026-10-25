package iatheiya.ayana.parse

import iatheiya.ayana.transcript.TranscriptBuilder

fun StreamEvent.applyTo(builder: TranscriptBuilder) {
    when (this) {
        is StreamEvent.Text -> builder.applyOutput(styled)
        is StreamEvent.OutputBegin -> Unit
        is StreamEvent.CommandEnd -> builder.applyCommandEnd(exitCode)
        is StreamEvent.WorkingDirectory -> builder.applyWorkingDirectory(path)
    }
}
