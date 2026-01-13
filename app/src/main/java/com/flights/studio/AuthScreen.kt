@file:OptIn(ExperimentalMaterial3Api::class)

package com.flights.studio

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthMode { Login, SignUp }

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    onLogin: suspend (email: String, password: String) -> Result<Unit>,
    onSignUp: suspend (
        fullName: String,
        phone: String,
        email: String,
        password: String,
        avatarUri: Uri?
    ) -> Result<Unit>,
    onForgotPassword: (prefillEmail: String) -> Unit,
) {
    var mode by remember { mutableStateOf(AuthMode.Login) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()

    val textScale = rememberUiTight()

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        avatarUri = uri
    }

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    var showPass by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val pageBg = if (!dark) Color(0xFFF6F7FB) else Color(0xFF0F1116)
    val fieldBg = if (!dark) Color(0xFFE7EAF2) else Color(0xFF1B1F2A)
    val primaryBtn = if (!dark) Color(0xFF253D73) else Color(0xFF4E7DFF)
    val titleColor = if (!dark) Color(0xFF1C1D22) else Color(0xFFECEEF6)
    val subtleText = if (!dark) Color(0xFF2B2C33) else Color(0xFFB7BECC)

    val canSubmit = when (mode) {
        AuthMode.Login ->
            email.isNotBlank() && password.isNotBlank() && !isLoading

        AuthMode.SignUp ->
            fullName.isNotBlank() && phone.isNotBlank() &&
                    email.isNotBlank() && password.isNotBlank() && confirm.isNotBlank() && !isLoading
    }

    fun switchMode(next: AuthMode) {
        mode = next
        errorText = null
        confirm = ""
        showPass = false
        showConfirm = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                val size = 120.dp
                val shape = RoundedCornerShape(60.dp)

                Surface(
                    modifier = Modifier
                        .size(size)
                        .clip(shape)
                        .clickable(enabled = mode == AuthMode.SignUp) {
                            avatarPicker.launch(arrayOf("image/*"))
                        },
                    shape = shape,
                    color = fieldBg,
                    tonalElevation = 2.dp
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (mode == AuthMode.SignUp) {
                                val s = MaterialTheme.typography.labelLarge
                                Text(
                                    text = "Add photo",
                                    style = s,
                                    fontSize = s.fontSize.us(textScale),
                                    lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(textScale) else s.lineHeight,
                                    color = subtleText
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = subtleText,
                                    modifier = Modifier.size(52.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedModeText(
                mode = mode,
                loginText = "Login",
                signUpText = "Sign up",
                color = titleColor,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier,
                textScale = textScale
            )

            Spacer(Modifier.height(18.dp))

            AnimatedVisibility(
                visible = mode == AuthMode.SignUp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    RoundedField(
                        value = fullName,
                        onValueChange = { fullName = it; errorText = null },
                        placeholder = "Full name",
                        leadingIcon = { Icon(painterResource(R.drawable.ic_oui_contact), null) },
                        fieldBg = fieldBg,
                        keyboardType = KeyboardType.Text,
                        textScale = textScale
                    )
                    Spacer(Modifier.height(14.dp))
                    RoundedField(
                        value = phone,
                        onValueChange = { phone = it; errorText = null },
                        placeholder = "Phone number",
                        leadingIcon = { Icon(Icons.Filled.Phone, null) },
                        fieldBg = fieldBg,
                        keyboardType = KeyboardType.Phone,
                        textScale = textScale
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }

            RoundedField(
                value = email,
                onValueChange = { email = it; errorText = null },
                placeholder = "Email",
                leadingIcon = { Icon(Icons.Filled.Email, null) },
                fieldBg = fieldBg,
                keyboardType = KeyboardType.Email,
                textScale = textScale
            )

            Spacer(Modifier.height(14.dp))

            RoundedField(
                value = password,
                onValueChange = { password = it; errorText = null },
                placeholder = "Password",
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                    }
                },
                fieldBg = fieldBg,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                textScale = textScale
            )

            AnimatedVisibility(
                visible = mode == AuthMode.SignUp,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    RoundedField(
                        value = confirm,
                        onValueChange = { confirm = it; errorText = null },
                        placeholder = "Confirm password",
                        leadingIcon = { Icon(Icons.Filled.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(if (showConfirm) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                            }
                        },
                        fieldBg = fieldBg,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        textScale = textScale
                    )
                }
            }

            if (errorText != null) {
                Spacer(Modifier.height(10.dp))
                val s = MaterialTheme.typography.bodySmall
                Text(
                    errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = s,
                    fontSize = s.fontSize.us(textScale),
                    lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(textScale) else s.lineHeight
                )
            } else {
                Spacer(Modifier.height(18.dp))
            }

            Button(
                onClick = {
                    errorText = null

                    val e = email.trim()
                    val p = password

                    if (e.isBlank() || p.isBlank()) {
                        errorText = "Enter email and password"
                        return@Button
                    }
                    if (mode == AuthMode.SignUp && p != confirm) {
                        errorText = "Passwords do not match"
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        if (mode == AuthMode.SignUp) {
                            if (fullName.trim().isBlank()) {
                                errorText = "Enter your name"
                                isLoading = false
                                return@launch
                            }
                            if (phone.trim().isBlank()) {
                                errorText = "Enter your phone number"
                                isLoading = false
                                return@launch
                            }
                        }

                        val result = if (mode == AuthMode.Login) {
                            onLogin(e, p)
                        } else {
                            onSignUp(
                                fullName.trim(),
                                phone.trim(),
                                e,
                                p,
                                avatarUri
                            )
                        }

                        isLoading = false
                        result.exceptionOrNull()?.let { ex ->
                            when (ex.message) {
                                "ACCOUNT_EXISTS" -> {
                                    mode = AuthMode.Login
                                    errorText = "Account already exists. Please log in."
                                    password = ""
                                    confirm = ""
                                    showPass = false
                                    showConfirm = false
                                }

                                "SIGNUP_NO_SESSION" -> {
                                    mode = AuthMode.Login
                                    errorText = "Account created. Please log in."
                                    password = ""
                                    confirm = ""
                                    showPass = false
                                    showConfirm = false
                                }

                                "WEAK_PASSWORD" -> {
                                    errorText = "Password is too weak. Use at least 6 characters."
                                    password = ""
                                    confirm = ""
                                    showPass = false
                                    showConfirm = false
                                }

                                "SIGNUP_CONFLICT" -> {
                                    mode = AuthMode.Login
                                    errorText = "Email or phone already exists. Please log in."
                                    password = ""
                                    confirm = ""
                                    showPass = false
                                    showConfirm = false
                                }

                                "SIGNUP_FAILED" -> {
                                    errorText = "Sign up failed. Please try again."
                                }

                                "PHONE_TAKEN" -> {
                                    errorText = "Phone already used"
                                }

                                else -> {
                                    val raw = ex.message.orEmpty()
                                    val msg = raw.lowercase()

                                    errorText = when {
                                        msg.contains("invalid login") ||
                                                msg.contains("invalid credentials") ||
                                                (msg.contains("invalid") && msg.contains("password")) ||
                                                (msg.contains("invalid") && msg.contains("email")) ->
                                            "Invalid email or password"

                                        msg.contains("rate") || msg.contains("too many") ->
                                            "Too many attempts. Try again later."

                                        else -> "Something went wrong. Please try again."
                                    }

                                    android.util.Log.e("AUTH_UI_ERR", "raw=$raw", ex)
                                }
                            }
                        }
                    }
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryBtn,
                    contentColor = Color.White,
                    disabledContainerColor = primaryBtn.copy(alpha = 0.45f),
                    disabledContentColor = Color.White.copy(alpha = 0.75f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                }
                AnimatedModeText(
                    mode = mode,
                    loginText = "Login",
                    signUpText = "Create account",
                    color = Color.White,
                    style = TextStyle(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier,
                    textScale = textScale
                )
            }

            Spacer(Modifier.height(14.dp))

            if (mode == AuthMode.Login) {
                var showForgotDialog by remember { mutableStateOf(false) }

                val s = MaterialTheme.typography.bodyMedium
                Text(
                    text = "Forget Password?",
                    color = primaryBtn,
                    style = s,
                    fontSize = s.fontSize.us(textScale),
                    lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(textScale) else s.lineHeight,
                    modifier = Modifier.clickable {
                        val e = email.trim()
                        if (e.isBlank()) {
                            errorText = "Enter your email first"
                            return@clickable
                        }
                        showForgotDialog = true
                    }
                )
                val scales = rememberUiScales()
                ForgotPasswordDialogModern(
                    visible = showForgotDialog,
                    email = email.trim(),
                    dark = dark,
                    primaryBtn = primaryBtn,
                    fieldBg = fieldBg,
                    scales = scales,
                    onSendReset = { targetEmail ->
                        onForgotPassword(targetEmail)
                    },
                    onDismissAndBackToLogin = {
                        showForgotDialog = false
                        mode = AuthMode.Login
                        password = ""
                        confirm = ""
                        showPass = false
                        showConfirm = false
                    },
                    onDismiss = { showForgotDialog = false }
                )
                Spacer(Modifier.height(28.dp))
            } else {
                Spacer(Modifier.height(18.dp))
            }

            AuthBottomSwitch(
                mode = mode,
                subtleText = subtleText,
                primaryBtn = primaryBtn,
                textScale = textScale,
                onSwitch = { next -> switchMode(next) }
            )
        }
    }
}

@Composable
fun AuthBottomSwitch(
    mode: AuthMode,
    subtleText: Color,
    primaryBtn: Color,
    textScale: Float,
    onSwitch: (AuthMode) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            val goingToSignUp = targetState == AuthMode.SignUp

            val inAnim =
                (fadeIn(tween(180)) + slideInVertically(tween(220)) { h ->
                    if (goingToSignUp) h / 2 else -h / 2
                })

            val outAnim =
                (fadeOut(tween(140)) + slideOutVertically(tween(200)) { h ->
                    if (goingToSignUp) -h / 2 else h / 2
                })

            inAnim.togetherWith(outAnim).using(SizeTransform(clip = false))
        },
        label = "AuthBottomSwitch"
    ) { m ->
        val bottom = buildAnnotatedString {
            withStyle(SpanStyle(color = subtleText)) {
                append(if (m == AuthMode.Login) "Not a member? " else "Already have an account? ")
            }
            withStyle(SpanStyle(color = primaryBtn, fontWeight = FontWeight.SemiBold)) {
                append(if (m == AuthMode.Login) "Sign up now!" else "Login")
            }
        }

        val s = MaterialTheme.typography.bodyMedium
        Text(
            text = bottom,
            style = s,
            fontSize = s.fontSize.us(textScale),
            lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(textScale) else s.lineHeight,
            modifier = modifier.clickable {
                onSwitch(if (m == AuthMode.Login) AuthMode.SignUp else AuthMode.Login)
            }
        )
    }
}

private enum class ForgotUiState {
    Confirm,
    Sending,
    CountdownToOpenEmail,
    ReadyToOpenEmail
}

@Composable
fun ForgotPasswordDialogModern(
    visible: Boolean,
    email: String,
    dark: Boolean,
    primaryBtn: Color,
    fieldBg: Color,
    scales: UiScales, // ✅ use your real scaling
    onSendReset: (String) -> Unit,
    onDismissAndBackToLogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val ctx = LocalContext.current

    var checked by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(ForgotUiState.Confirm) }
    var msg by remember { mutableStateOf<String?>(null) }

    // ✅ real countdown seconds
    var secondsLeft by remember { mutableIntStateOf(8) }

    fun openGmail() {
        val gmailInbox = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.google.android.gm")
        }
        runCatching { ctx.startActivity(gmailInbox); return }

        val anyMailApp = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { ctx.startActivity(Intent.createChooser(anyMailApp, "Open email app")); return }

        val web = Intent(Intent.ACTION_VIEW, "https://mail.google.com".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(web) }
    }

    // ✅ countdown logic
    LaunchedEffect(state) {
        if (state == ForgotUiState.CountdownToOpenEmail) {
            secondsLeft = 8
            while (secondsLeft > 0 && state == ForgotUiState.CountdownToOpenEmail) {
                delay(1000)
                secondsLeft -= 1
            }
            if (state == ForgotUiState.CountdownToOpenEmail) {
                state = ForgotUiState.ReadyToOpenEmail
            }
        }
    }

    // Modern-ish container + your colors
    AlertDialog(
        onDismissRequest = {
            // Only allow dismiss on first step (so user doesn’t close while “sending”)
            if (state == ForgotUiState.Confirm) onDismiss()
        },
        containerColor = if (!dark) Color.White else Color(0xFF111526),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(22.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // little icon chip
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(primaryBtn.copy(alpha = if (dark) 0.22f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔒", fontSize = MaterialTheme.typography.titleMedium.fontSize.us(scales.body))
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    val t = MaterialTheme.typography.titleLarge
                    Text(
                        text = "Reset password",
                        style = t,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = t.fontSize.us(scales.body),
                        lineHeight = if (t.lineHeight.isSpecified) t.lineHeight.us(scales.body) else t.lineHeight
                    )
                    val s = MaterialTheme.typography.bodySmall
                    Text(
                        text = "We’ll send a secure reset link to your email.",
                        style = s,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        fontSize = s.fontSize.us(scales.label),
                        lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.label) else s.lineHeight
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Email card
                Surface(
                    color = fieldBg,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 0.dp
                ) {
                    Column(Modifier.padding(12.dp)) {
                        val l = MaterialTheme.typography.labelMedium
                        Text(
                            "Reset link will be sent to:",
                            style = l,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            fontSize = l.fontSize.us(scales.label),
                            lineHeight = if (l.lineHeight.isSpecified) l.lineHeight.us(scales.label) else l.lineHeight
                        )
                        Spacer(Modifier.height(4.dp))
                        val b = MaterialTheme.typography.bodyLarge
                        Text(
                            email.ifBlank { "—" },
                            style = b,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = b.fontSize.us(scales.body),
                            lineHeight = if (b.lineHeight.isSpecified) b.lineHeight.us(scales.body) else b.lineHeight
                        )
                    }
                }

                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "forgot_state"
                ) { st ->
                    when (st) {
                        ForgotUiState.Confirm -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.06f else 0.04f))
                                        .clickable(enabled = email.isNotBlank()) { checked = !checked }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                                    Spacer(Modifier.width(10.dp))
                                    val s = MaterialTheme.typography.bodyMedium
                                    Text(
                                        "I confirm I want to reset this account",
                                        style = s,
                                        fontSize = s.fontSize.us(scales.body),
                                        lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                                    )
                                }

                                val tip = MaterialTheme.typography.bodySmall
                                Text(
                                    "Tip: Check Spam/Junk if you don’t see it.",
                                    style = tip,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                                    fontSize = tip.fontSize.us(scales.label),
                                    lineHeight = if (tip.lineHeight.isSpecified) tip.lineHeight.us(scales.label) else tip.lineHeight
                                )
                            }
                        }

                        ForgotUiState.Sending -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                val s = MaterialTheme.typography.bodyMedium
                                Text(
                                    "Sending reset email…",
                                    style = s,
                                    fontSize = s.fontSize.us(scales.body),
                                    lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                                )
                            }
                        }

                        ForgotUiState.CountdownToOpenEmail -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    val s = MaterialTheme.typography.bodyMedium
                                    Text(
                                        "Email sent. Preparing… $secondsLeft",
                                        style = s,
                                        fontSize = s.fontSize.us(scales.body),
                                        lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                                    )
                                }
                                val s2 = MaterialTheme.typography.bodySmall
                                Text(
                                    "We’ll show the Open Email button in a moment.",
                                    style = s2,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                                    fontSize = s2.fontSize.us(scales.label),
                                    lineHeight = if (s2.lineHeight.isSpecified) s2.lineHeight.us(scales.label) else s2.lineHeight
                                )
                            }
                        }

                        ForgotUiState.ReadyToOpenEmail -> {
                            val s = MaterialTheme.typography.bodyMedium
                            Text(
                                text = "Email sent ✅\nOpen your inbox and tap the reset link.",
                                style = s,
                                fontSize = s.fontSize.us(scales.body),
                                lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                            )
                            msg?.let {
                                Spacer(Modifier.height(6.dp))
                                val s2 = MaterialTheme.typography.bodySmall
                                Text(
                                    it,
                                    style = s2,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                                    fontSize = s2.fontSize.us(scales.label),
                                    lineHeight = if (s2.lineHeight.isSpecified) s2.lineHeight.us(scales.label) else s2.lineHeight
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                ForgotUiState.Confirm -> {
                    Button(
                        enabled = email.isNotBlank() && checked,
                        onClick = {
                            state = ForgotUiState.Sending

                            // ✅ you still control actual send from caller
                            onSendReset(email)

                            // ✅ nicer flow: short sending → countdown → open email
                            // (no fake "waiting", real countdown UI)
                            state = ForgotUiState.CountdownToOpenEmail
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBtn, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        val s = MaterialTheme.typography.labelLarge
                        Text(
                            "Send reset email",
                            style = s,
                            fontSize = s.fontSize.us(scales.body),
                            lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                        )
                    }
                }

                ForgotUiState.Sending, ForgotUiState.CountdownToOpenEmail -> {
                    Button(enabled = false, onClick = {}, shape = RoundedCornerShape(14.dp)) {
                        val s = MaterialTheme.typography.labelLarge
                        Text(
                            "Please wait…",
                            style = s,
                            fontSize = s.fontSize.us(scales.body),
                            lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                        )
                    }
                }

                ForgotUiState.ReadyToOpenEmail -> {
                    Button(
                        onClick = {
                            openGmail()
                            onDismissAndBackToLogin()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBtn, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        val s = MaterialTheme.typography.labelLarge
                        Text(
                            "Open email",
                            style = s,
                            fontSize = s.fontSize.us(scales.body),
                            lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                        )
                    }
                }
            }
        },
        dismissButton = {
            when (state) {
                ForgotUiState.Confirm -> {
                    TextButton(onClick = onDismiss) {
                        val s = MaterialTheme.typography.labelLarge
                        Text(
                            "Cancel",
                            style = s,
                            fontSize = s.fontSize.us(scales.body),
                            lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                        )
                    }
                }

                ForgotUiState.ReadyToOpenEmail -> {
                    TextButton(onClick = onDismissAndBackToLogin) {
                        val s = MaterialTheme.typography.labelLarge
                        Text(
                            "Back to login",
                            style = s,
                            fontSize = s.fontSize.us(scales.body),
                            lineHeight = if (s.lineHeight.isSpecified) s.lineHeight.us(scales.body) else s.lineHeight
                        )
                    }
                }

                else -> Unit
            }
        }
    )
}

@Composable
fun AnimatedModeText(
    mode: AuthMode,
    loginText: String,
    signUpText: String,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
    textScale: Float
) {
    val scaledStyle = remember(style, textScale) { style.scaleText(textScale) }

    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            val goingToSignUp = targetState == AuthMode.SignUp

            val inAnim =
                (fadeIn(tween(180)) + slideInVertically(tween(220)) { h ->
                    if (goingToSignUp) h / 2 else -h / 2
                })

            val outAnim =
                (fadeOut(tween(140)) + slideOutVertically(tween(200)) { h ->
                    if (goingToSignUp) -h / 2 else h / 2
                })

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

@Composable
private fun RoundedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    fieldBg: Color,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textScale: Float
) {
    val base = MaterialTheme.typography.bodyLarge
    val scaled = remember(base, textScale) { base.scaleText(textScale) }

    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(
                placeholder,
                style = scaled
            )
        },
        textStyle = scaled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = fieldBg,
            unfocusedContainerColor = fieldBg,
            disabledContainerColor = fieldBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Color(0xFF253D73)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
    )
}

private fun TextStyle.scaleText(s: Float): TextStyle {
    var out = this
    if (out.fontSize.isSpecified) out = out.copy(fontSize = out.fontSize.us(s))
    if (out.lineHeight.isSpecified) out = out.copy(lineHeight = out.lineHeight.us(s))
    return out
}
