package com.athea.app.ui.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.athea.app.text.Rgb
import com.athea.app.text.Style
import com.athea.app.text.StyledText

/** The one place the pure core text model meets Compose's [AnnotatedString]. */
fun StyledText.toAnnotated(): AnnotatedString =
    AnnotatedString(
        text = text,
        spanStyles = spans.map { AnnotatedString.Range(it.style.toSpanStyle(), it.start, it.end) },
    )

private fun Style.toSpanStyle(): SpanStyle =
    SpanStyle(
        color = foreground?.toColor() ?: Color.Unspecified,
        background = background?.toColor() ?: Color.Unspecified,
        fontWeight = if (bold) FontWeight.Bold else null,
    )

private fun Rgb.toColor(): Color = Color(argb)
