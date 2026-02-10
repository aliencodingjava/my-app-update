@file:Suppress("DEPRECATION")

package com.flights.studio

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay
import java.util.WeakHashMap

object FancyPillToast {

    private val activeHosts = WeakHashMap<FragmentActivity, FrameLayout>()

    private fun removeHostSafely(
        activity: FragmentActivity,
        root: ViewGroup,
        host: FrameLayout
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (host.parent != null) {
            try {
                root.removeView(host)
            } catch (_: Throwable) {
            }
        }
        activeHosts.remove(activity)
    }

    fun show(
        activity: FragmentActivity,
        text: String,
        durationMs: Long = 3000L
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        activity.runOnUiThread {
            val root =
                activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runOnUiThread

            activeHosts.remove(activity)?.let { old ->
                try {
                    root.removeView(old)
                } catch (_: Throwable) {
                }
            }

            val host = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

            activeHosts[activity] = host

            val composeView = ComposeView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setContent {
                    FlightsTheme {
                        val ui = rememberUiScale()
                        val uiTight = rememberUiTight()

                        val visible = remember { mutableStateOf(true) }
                        val shadowOn = remember { mutableStateOf(true) }

                        val navBottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding()

                        val shadowDp = animateDpAsState(
                            targetValue = if (shadowOn.value) 5.dp else 0.dp,
                            animationSpec = tween(140),
                            label = "pillShadow"
                        )

                        val surfaceAlpha = animateFloatAsState(
                            targetValue = if (shadowOn.value) 0.96f else 0.92f,
                            animationSpec = tween(140),
                            label = "pillAlpha"
                        )

                        LaunchedEffect(Unit) {
                            delay(durationMs)
                            shadowOn.value = false
                            delay(150)
                            visible.value = false
                            delay(260)

                            activity.runOnUiThread {
                                removeHostSafely(activity, root, host)
                            }
                        }

                        Box(Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = visible.value,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = navBottom + (28.dp * ui)),
                                enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
                                exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 2 }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = surfaceAlpha.value),
                                    tonalElevation = 0.dp,
                                    shadowElevation = shadowDp.value
                                ) {
                                    val base = MaterialTheme.typography.labelLarge
                                    val scaledStyle = remember(base, uiTight) {
                                        var out = base
                                        if (base.fontSize.isSpecified) out =
                                            out.copy(fontSize = base.fontSize.us(uiTight))
                                        if (base.lineHeight.isSpecified) out =
                                            out.copy(lineHeight = base.lineHeight.us(uiTight))
                                        out
                                    }

                                    Text(
                                        text = text,
                                        style = scaledStyle,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(
                                            horizontal = (22.dp * ui),
                                            vertical = (10.dp * ui)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            host.addView(composeView)
            root.addView(host)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    fun showUndo(
        activity: FragmentActivity,
        text: String,
        actionText: String = "UNDO",
        durationMs: Long = 3500L,
        onAction: () -> Unit,
        onTimeout: (() -> Unit)? = null
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        activity.runOnUiThread {
            val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@runOnUiThread

            activeHosts.remove(activity)?.let { old ->
                try { root.removeView(old) } catch (_: Throwable) {}
            }

            val host = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            activeHosts[activity] = host

            val composeView = ComposeView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setContent {
                    FlightsTheme {
                        val ui = rememberUiScale()
                        val uiTight = rememberUiTight()

                        val visible = remember { mutableStateOf(true) }
                        val shadowOn = remember { mutableStateOf(true) }
                        val actionUsed = remember { mutableStateOf(false) }

                        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                        val shadowDp = animateDpAsState(
                            targetValue = if (shadowOn.value) 4.dp else 0.dp,
                            animationSpec = tween(140),
                            label = "pillShadow"
                        )

                        val surfaceAlpha = animateFloatAsState(
                            targetValue = if (shadowOn.value) 0.96f else 0.92f,
                            animationSpec = tween(140),
                            label = "pillAlpha"
                        )

                        fun dismissNow() {
                            shadowOn.value = false
                            visible.value = false
                            activity.runOnUiThread {
                                removeHostSafely(activity, root, host)
                            }
                        }

                        LaunchedEffect(Unit) {
                            delay(durationMs)
                            if (!actionUsed.value) {
                                try { onTimeout?.invoke() } catch (_: Throwable) {}
                            }
                            dismissNow()
                        }

                        // ---- STYLE (text scaled) ----
                        val base = MaterialTheme.typography.labelLarge
                        val scaledStyle = remember(base, uiTight) {
                            var out = base
                            if (base.fontSize.isSpecified) out = out.copy(fontSize = base.fontSize.us(uiTight))
                            if (base.lineHeight.isSpecified) out = out.copy(lineHeight = base.lineHeight.us(uiTight))
                            out
                        }

                        Box(Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = visible.value,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = navBottom + (24.dp * ui)),
                                enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
                                exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { it / 2 }
                            ) {
                                // ✅ SINGLE SURFACE (the pill)
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = surfaceAlpha.value),
                                    tonalElevation = 0.dp,
                                    shadowElevation = shadowDp.value
                                ) {
                                    // ✅ make split buttons NOT paint their own container
                                    val transparentColors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )

                                    // ✅ smaller padding, keeps pill slim
                                    val leadPad = androidx.compose.foundation.layout.PaddingValues(
                                        start = (16.dp * ui),
                                        end = (10.dp * ui),
                                        top = (8.dp * ui),
                                        bottom = (8.dp * ui)
                                    )
                                    val trailPad = androidx.compose.foundation.layout.PaddingValues(
                                        start = (10.dp * ui),
                                        end = (14.dp * ui),
                                        top = (8.dp * ui),
                                        bottom = (8.dp * ui)
                                    )

                                    androidx.compose.material3.SplitButtonLayout(
                                        leadingButton = {
                                            androidx.compose.material3.SplitButtonDefaults.LeadingButton(
                                                onClick = {},
                                                colors = transparentColors,
                                                contentPadding = leadPad
                                            ) {
                                                Text(
                                                    text = text,
                                                    style = scaledStyle,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        },
                                        trailingButton = {
                                            // ✅ real trailing button (not checked toggle look)
                                            androidx.compose.material3.SplitButtonDefaults.TrailingButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    actionUsed.value = true
                                                    try { onAction() } catch (_: Throwable) {}
                                                    dismissNow()
                                                },
                                                colors = transparentColors,
                                                contentPadding = trailPad
                                            ) {
                                                Text(
                                                    text = actionText,
                                                    style = scaledStyle.copy(
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            host.addView(composeView)
            root.addView(host)
        }
    }
}