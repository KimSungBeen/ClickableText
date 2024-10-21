package com.sbeen.clickabletext.ui.theme


import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.sbeen.clickabletext.R

object ClickableText {
    data class Button(
        val indexRange: IntRange,
        val onClick: () -> Unit,
        val textStyle: TextStyle = TextStyle(
            color = Color.Blue,
            fontSize = 18.sp
        ),
        val leading: @Composable (BoxScope.() -> Unit)? = null,
        val trailing: @Composable (BoxScope.() -> Unit)? = null,
        val statusProvider: () -> Status = { Status.Default }
    )

    enum class Status {
        Default,
        Disabled
    }
}

/**
 *
 * @param fullText Full text in text area
 * @param defaultTextStyle default text style
 * @param buttons List of buttons to include in text
 * @param onClick Button click event, clickedButtonIndex: Index of the clicked button
 */
@Composable
fun ClickableText(
    fullText: String,
    defaultTextStyle: TextStyle,
    buttons: List<ClickableText.Button>,
    onClick: (clickedButtonIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // State value when no buttons in the text are pressed
    val NONPRESSED_INDEX: Int = -1

    // Utility to find the index of the pressed text when a press event occurs in the text
    var layoutResult: TextLayoutResult? by remember { mutableStateOf(null) }

    // MutableState that stores the index of the pressed text
    var pressedIndex: Int by remember { mutableIntStateOf(NONPRESSED_INDEX) }

    // Map to store image resources
    val inlineContent = mutableMapOf<String, InlineTextContent>()

    // If there is leading or trailing, the button range is misaligned,
    // so the value for correction of the click area
    var shiftIndex = 0

    // List of click ranges for buttons with index correction
    // (to calculate the receiving factor of the click event lambda)
    val clickIndexRanges = mutableListOf<IntRange>()

    // Create AnnotatedString
    val annotatedText = buildAnnotatedString {
        buttons.foldIndexed(
            buttons.firstOrNull() ?: return@buildAnnotatedString
        ) { i: Int, prevButton: ClickableText.Button, currentButton: ClickableText.Button ->
            // If leading or trailing exists, the index correction value is increased by 1.
            val additional: List<@Composable BoxScope.() -> Unit> = listOfNotNull(
                currentButton.leading,
                currentButton.trailing
            )

            val count: Int = additional.count()

            // Clickable IndexRange
            val clickIndexRange: IntRange = with(currentButton.indexRange) {
                (first + if (i == 0) 0 else shiftIndex)..last + shiftIndex + count
            }.also { shiftIndex += count }

            // Save Clickable IndexRange list
            clickIndexRanges.add(clickIndexRange)


            val buttonStatus: ClickableText.Status = currentButton.statusProvider()
            val alphaValue: Float = when {
                buttonStatus == ClickableText.Status.Disabled -> 0.25f
                pressedIndex in clickIndexRange -> 0.5f
                else -> 1f
            }

            // Save the button's leading, trailing image to inlineContent
            listOfNotNull(
                currentButton.leading?.let { currentButton.indexRange.first to it },
                currentButton.trailing?.let { currentButton.indexRange.last to it }
            ).forEach { (key, content) ->
                inlineContent[key.toString()] = InlineTextContent(
                    placeholder = Placeholder(
                        width = currentButton.textStyle.fontSize,
                        height = currentButton.textStyle.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Box(
                        modifier = Modifier.alpha(alphaValue),
                        content = content
                    )
                }
            }

            // From the first button to the last button, the preceding text is output and the button is output.
            if (i < buttons.lastIndex) {
                append(
                    fullText.substring(
                        startIndex = if (i == 0) 0 else prevButton.indexRange.last,
                        endIndex = currentButton.indexRange.first
                    )
                )

                // 클릭 가능한 텍스트 영역 추가
                ButtonUnit(
                    fullText = fullText,
                    button = currentButton,
                    pressedIndex = pressedIndex,
                    clickIndexRange = clickIndexRange
                )
            }
            // If it is the last button, the preceding text, button, and remaining text are output.
            else {
                append(
                    fullText.substring(
                        prevButton.indexRange.last until currentButton.indexRange.first
                    )
                )

                // Add a clickable text area
                ButtonUnit(
                    fullText = fullText,
                    button = currentButton,
                    pressedIndex = pressedIndex,
                    clickIndexRange = clickIndexRange
                )

                append(
                    fullText.substring(
                        currentButton.indexRange.last until fullText.length
                    )
                )
            }

            currentButton
        }
    }

    // Text where AnnotatedString will be input
    Text(
        text = annotatedText,
        modifier = modifier.pointerInput(buttons) {
            this.detectTapGestures(
                onPress = { offset: Offset ->
                    try {
                        layoutResult?.let { pressedIndex = it.getOffsetForPosition(offset) }

                        awaitRelease() // Wait until the user releases their hand
                    } finally {
                        clickIndexRanges.indexOfFirst { pressedIndex in it }
                            .takeIf { it >= 0 && buttons[it].statusProvider() != ClickableText.Status.Disabled }
                            ?.let(onClick)

                        pressedIndex = NONPRESSED_INDEX
                    }
                }
            )
        },
        inlineContent = inlineContent,
        onTextLayout = { layoutResult = it },
        style = defaultTextStyle
    )
}

/**
 * [AnnotatedString] Composable of the part that will be displayed as a button inside
 * @param fullText Full text in text area
 * @param button Information about the portion of the text area occupied by the button
 * @param pressedIndex Index of the pressed area in the text area
 * @param clickIndexRange Range for correcting click areas that are misaligned due to leading or trailing
 */
@Composable
private fun AnnotatedString.Builder.ButtonUnit(
    fullText: String,
    button: ClickableText.Button,
    pressedIndex: Int,
    clickIndexRange: IntRange
) {
    // TextButton Style
    val style: TextStyle = button.textStyle.copy(
        color = button.textStyle.color.copy(
            alpha = when {
                button.statusProvider() == ClickableText.Status.Disabled -> 0.25f
                pressedIndex in clickIndexRange -> 0.5f
                else -> 1f
            }
        )
    )

    // Leading Image
    if (button.leading != null) {
        appendInlineContent(button.indexRange.first.toString())
    }

    // Add a clickable text area
    withStyle(style = style.toSpanStyle()) {
        with(button.indexRange) {
            append(fullText.substring(startIndex = first, endIndex = last))
        }
    }

    // Trailing Image
    if (button.trailing != null) {
        appendInlineContent(button.indexRange.last.toString())
    }
}

@Preview(showBackground = true)
@Composable
private fun TextButtonPreview() {
    var status by remember { mutableStateOf(ClickableText.Status.Default) }

    val buttons: List<ClickableText.Button> = listOf(
        ClickableText.Button(
            indexRange = 0..5,
            onClick = { status = ClickableText.Status.Default },
            trailing = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null
                )
            }
        ),
        ClickableText.Button(
            indexRange = 17..21,
            onClick = { status = ClickableText.Status.Disabled },
            leading = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null
                )
            }
        ),
        ClickableText.Button(
            indexRange = 30..33,
            textStyle = TextStyle(
                fontSize = 28.sp,
                color = Color.Blue
            ),
            onClick = {},
            leading = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null
                )
            },
            trailing = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null
                )
            },
            statusProvider = { status }
        )
    )

    ClickableText(
        fullText = "Hello, I am 000. Nice to meet you :)",
        defaultTextStyle = TextStyle(
            color = Color.Black,
            fontSize = 18.sp
        ),
        buttons = buttons,
        onClick = { clickedButtonIndex: Int ->
            buttons[clickedButtonIndex].onClick()
        }
    )
}
