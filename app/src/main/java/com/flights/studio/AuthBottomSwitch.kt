package com.flights.studio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import androidx.compose.material3.Text

@Composable
fun AnimatedModeText(
    mode: AuthMode,
    loginText: String,
    signUpText: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val s = rememberUiTight()

    val scaledStyle = remember(style, s) {
        style.copy(
            fontSize = if (style.fontSize.isSpecified) style.fontSize.us(s) else style.fontSize,
            lineHeight = if (style.lineHeight.isSpecified) style.lineHeight.us(s) else style.lineHeight
        )
    }

    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            val goingToSignUp = targetState == AuthMode.SignUp

            val inAnim =
                fadeIn(tween(180)) + slideInVertically(tween(220)) { h ->
                    if (goingToSignUp) h / 2 else -h / 2
                }

            val outAnim =
                fadeOut(tween(140)) + slideOutVertically(tween(200)) { h ->
                    if (goingToSignUp) -h / 2 else h / 2
                }

            inAnim.togetherWith(outAnim).using(SizeTransform(clip = false))
        },
        label = "AnimatedModeText"
    ) { m ->
        Text(
            text = if (m == AuthMode.Login) loginText else signUpText,
            color = color,
            style = scaledStyle,
            modifier = modifier
        )
    }
}
