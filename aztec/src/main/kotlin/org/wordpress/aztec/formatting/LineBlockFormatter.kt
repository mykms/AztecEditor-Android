package org.wordpress.aztec.formatting

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.text.Spanned
import android.text.TextUtils
import org.wordpress.aztec.AztecText
import org.wordpress.aztec.R
import org.wordpress.aztec.TextChangedEvent
import org.wordpress.aztec.TextFormat
import org.wordpress.aztec.spans.*
import java.util.*


class LineBlockFormatter(editor: AztecText) : AztecFormatter(editor) {

    fun applyHeading(textFormat: TextFormat) {
        headingClear()

        if (textFormat != TextFormat.FORMAT_PARAGRAPH) {
            headingFormat(textFormat)
        }
    }

    fun applyMoreComment() {
        applyComment(AztecCommentSpan.Comment.MORE)
    }

    fun applyPageComment() {
        applyComment(AztecCommentSpan.Comment.PAGE)
    }

    fun handleLineBlockStyling(textChangedEvent: TextChangedEvent) {
        if (textChangedEvent.isAddingCharacters && textChangedEvent.isNewLine()) {
            val spanAtNewLIne = editableText.getSpans(textChangedEvent.inputStart, textChangedEvent.inputStart, AztecHeadingSpan::class.java).getOrNull(0)
            if (spanAtNewLIne != null) {
                val spanStart = editableText.getSpanStart(spanAtNewLIne)
                val spanEnd = editableText.getSpanEnd(spanAtNewLIne)

                val isHeadingSplitRequired = spanStart <= textChangedEvent.inputStart && spanEnd > textChangedEvent.inputEnd
                //split heading span
                if (isHeadingSplitRequired && editableText[textChangedEvent.inputStart - 1] != '\n') {
                    editableText.setSpan(spanAtNewLIne, spanStart, textChangedEvent.inputStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    editableText.setSpan(spanAtNewLIne.clone(), textChangedEvent.inputStart + 1, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                //avoid applying heading span to newline breaks
                else if (isHeadingSplitRequired && editableText[textChangedEvent.inputStart - 1] == '\n') {
                    editableText.setSpan(spanAtNewLIne, spanStart + 1, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if (!isHeadingSplitRequired && editableText.length > textChangedEvent.inputStart + 1 && editableText[textChangedEvent.inputStart + 1] == '\n') {
                    editableText.setSpan(spanAtNewLIne, spanStart, spanEnd - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else if(!isHeadingSplitRequired && textChangedEvent.inputStart + 1 == spanEnd && editableText[textChangedEvent.inputStart] == '\n'){
                    editableText.setSpan(spanAtNewLIne, spanStart, spanEnd - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        } else if (!textChangedEvent.isAddingCharacters && editableText.length > textChangedEvent.inputEnd && textChangedEvent.inputEnd > 0) {
            val charBeforeInputEnd = textChangedEvent.text[textChangedEvent.inputEnd - 1]
            if (charBeforeInputEnd == '\n') return

            val spanOnTheLeft = editableText.getSpans(textChangedEvent.inputEnd - 1, textChangedEvent.inputEnd, AztecHeadingSpan::class.java).getOrNull(0)
            val spanOnTheRight = editableText.getSpans(textChangedEvent.inputEnd, textChangedEvent.inputEnd + 1, AztecHeadingSpan::class.java).getOrNull(0)

            //remove heading span if we move it up to line without another heading style applied
            if (spanOnTheLeft == null && spanOnTheRight != null) {
                editableText.removeSpan(spanOnTheRight)
            } else if (spanOnTheLeft != null && spanOnTheRight != null) {
                //change the heading span style if we move it up to line with diferent heading style applied
                if (spanOnTheLeft != spanOnTheRight) {
                    val leftSpanStart = editableText.getSpanStart(spanOnTheLeft)
                    val rightSpanEnd = editableText.getSpanEnd(spanOnTheRight)
                    editableText.removeSpan(spanOnTheRight)
                    editableText.setSpan(spanOnTheLeft, leftSpanStart, rightSpanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    fun headingClear() {
        val lines = TextUtils.split(editableText.toString(), "\n")

        for (i in lines.indices) {
            if (!containsHeading(i)) {
                continue
            }

            var lineStart = 0

            for (j in 0..i - 1) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length

            if (lineStart >= lineEnd) {
                continue
            }

            var headingStart = 0
            var headingEnd = 0

            if ((lineStart <= selectionStart && selectionEnd <= lineEnd) ||
                    (lineStart >= selectionStart && selectionEnd >= lineEnd) ||
                    (lineStart <= selectionStart && selectionEnd >= lineEnd && selectionStart <= lineEnd) ||
                    (lineStart >= selectionStart && selectionEnd <= lineEnd && selectionEnd >= lineStart)) {
                headingStart = lineStart
                headingEnd = lineEnd
            }

            if (headingStart < headingEnd) {
                val spans = editableText.getSpans(headingStart, headingEnd, AztecHeadingSpan::class.java)

                for (span in spans) {
                    editableText.removeSpan(span)
                }
            }
        }

        editor.refreshText()
    }

    fun headingFormat(textFormat: TextFormat) {
        val lines = TextUtils.split(editableText.toString(), "\n")

        for (i in lines.indices) {
            var lineStart = 0

            for (j in 0..i - 1) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length

            if (lineStart >= lineEnd) {
                continue
            }

            var headingStart = 0
            var headingEnd = 0

            if ((lineStart <= selectionStart && selectionEnd <= lineEnd) ||
                    (lineStart >= selectionStart && selectionEnd >= lineEnd) ||
                    (lineStart <= selectionStart && selectionEnd >= lineEnd && selectionStart <= lineEnd) ||
                    (lineStart >= selectionStart && selectionEnd <= lineEnd && selectionEnd >= lineStart)) {
                headingStart = lineStart
                headingEnd = lineEnd
            }

            if (headingStart < headingEnd) {
                editableText.setSpan(AztecHeadingSpan(textFormat), headingStart, headingEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        editor.refreshText()
    }

    fun containsHeading(textFormat: TextFormat, selStart: Int, selEnd: Int): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")
        val list = ArrayList<Int>()

        for (i in lines.indices) {
            var lineStart = 0
            for (j in 0..i - 1) {
                lineStart += lines[j].length + 1
            }

            val lineEnd = lineStart + lines[i].length
            if (lineStart >= lineEnd) {
                continue
            }

            if (lineStart <= selStart && selEnd <= lineEnd) {
                list.add(i)
            } else if (selStart <= lineStart && lineEnd <= selEnd) {
                list.add(i)
            }
        }

        if (list.isEmpty()) return false

        for (i in list) {
            if (!containHeadingType(textFormat, i)) {
                return false
            }
        }

        return true
    }

    fun containsHeading(index: Int): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")

        if (index < 0 || index >= lines.size) {
            return false
        }

        var start = 0

        for (i in 0..index - 1) {
            start += lines[i].length + 1
        }

        val end = start + lines[index].length

        if (start >= end) {
            return false
        }

        val spans = editableText.getSpans(start, end, AztecHeadingSpan::class.java)
        return spans.size > 0
    }

    private fun containHeadingType(textFormat: TextFormat, index: Int): Boolean {
        val lines = TextUtils.split(editableText.toString(), "\n")

        if (index < 0 || index >= lines.size) {
            return false
        }

        var start = 0

        for (i in 0..index - 1) {
            start += lines[i].length + 1
        }

        val end = start + lines[index].length

        if (start >= end) {
            return false
        }

        val spans = editableText.getSpans(start, end, AztecHeadingSpan::class.java)

        for (span in spans) {
            when (textFormat) {
                TextFormat.FORMAT_HEADING_1 ->
                    return span.heading == AztecHeadingSpan.Heading.H1
                TextFormat.FORMAT_HEADING_2 ->
                    return span.heading == AztecHeadingSpan.Heading.H2
                TextFormat.FORMAT_HEADING_3 ->
                    return span.heading == AztecHeadingSpan.Heading.H3
                TextFormat.FORMAT_HEADING_4 ->
                    return span.heading == AztecHeadingSpan.Heading.H4
                TextFormat.FORMAT_HEADING_5 ->
                    return span.heading == AztecHeadingSpan.Heading.H5
                TextFormat.FORMAT_HEADING_6 ->
                    return span.heading == AztecHeadingSpan.Heading.H6
                else -> return false
            }
        }

        return false
    }

    private fun applyComment(comment: AztecCommentSpan.Comment) {
        //check if we add a comment into a block element, at the end of the line, but not at the end of last line
        var applyingOnTheEndOfBlockLine = false
        editableText.getSpans(selectionStart, selectionEnd, AztecBlockSpan::class.java).forEach {
            if (editableText.getSpanEnd(it) > selectionEnd && editableText[selectionEnd] == '\n') {
                applyingOnTheEndOfBlockLine = true
                return@forEach
            }
        }

        val commentStartIndex = selectionStart + 1
        val commentEndIndex = selectionStart + comment.html.length + 1

        editor.disableTextChangedListener()
        editableText.replace(selectionStart, selectionEnd, "\n" + comment.html + if (applyingOnTheEndOfBlockLine) "" else "\n")

        editor.removeBlockStylesFromRange(commentStartIndex, commentEndIndex + 1, true)
        editor.removeHeadingStylesFromRange(commentStartIndex, commentEndIndex + 1)
        editor.removeInlineStylesFromRange(commentStartIndex, commentEndIndex + 1)

        val span = AztecCommentSpan(
                editor.context,
                when (comment) {
                    AztecCommentSpan.Comment.MORE -> ContextCompat.getDrawable(editor.context, R.drawable.img_more)
                    AztecCommentSpan.Comment.PAGE -> ContextCompat.getDrawable(editor.context, R.drawable.img_page)
                }
        )

        editableText.setSpan(
                span,
                commentStartIndex,
                commentEndIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        editor.setSelection(commentEndIndex + 1)
    }

    fun insertMedia(context: Context, drawable: Drawable, source: String) {
        val span = AztecMediaSpan(context, drawable, source)

        //check if we add media into a block element, at the end of the line, but not at the end of last line
        var applyingOnTheEndOfBlockLine = false
        editableText.getSpans(selectionStart, selectionEnd, AztecBlockSpan::class.java).forEach {
            if (editableText.getSpanEnd(it) > selectionEnd && editableText[selectionEnd] == '\n') {
                applyingOnTheEndOfBlockLine = true
                return@forEach
            }
        }

        val mediaStartIndex = selectionStart + 1
        val mediaEndIndex = selectionStart + span.getHtml().length + 1

        editor.disableTextChangedListener()
        editableText.replace(selectionStart, selectionEnd, "\n" + span.getHtml() + if (applyingOnTheEndOfBlockLine) "" else "\n")

        editor.removeBlockStylesFromRange(mediaStartIndex, mediaEndIndex + 1, true)
        editor.removeHeadingStylesFromRange(mediaStartIndex, mediaEndIndex + 1)
        editor.removeInlineStylesFromRange(mediaStartIndex, mediaEndIndex + 1)

        editableText.setSpan(
                span,
                mediaStartIndex,
                mediaEndIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        editableText.setSpan(
                AztecMediaClickableSpan(span),
                mediaStartIndex,
                mediaEndIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        editor.setSelection(mediaEndIndex + 1)
        editor.isMediaAdded = true
    }
}
