@file:OptIn(ExperimentalMaterial3Api::class)

package com.flights.studio

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import androidx.core.net.toUri

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

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { }
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

    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()

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
//        password = ""
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
                                Text(
                                    text = "Add photo",
                                    style = MaterialTheme.typography.labelLarge,
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
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
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
                        leadingIcon = { Icon(painterResource(R.drawable.ic_oui_contact), null)
                        },
                        fieldBg = fieldBg,
                        keyboardType = KeyboardType.Text
                    )
                    Spacer(Modifier.height(14.dp))
                    RoundedField(
                        value = phone,
                        onValueChange = { phone = it; errorText = null },
                        placeholder = "Phone number",
                        leadingIcon = { Icon(Icons.Filled.Phone, null) },
                        fieldBg = fieldBg,
                        keyboardType = KeyboardType.Phone
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
                keyboardType = KeyboardType.Email
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
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation()
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
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation()
                    )
                }
            }

            if (errorText != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                                    // optional: keep them in SignUp mode and clear only the phone field if you want
                                    // phone = ""
                                }



                                else -> {
                                    val raw = ex.message.orEmpty()
                                    val msg = raw.lowercase()

                                    errorText = when {
                                        // ✅ login invalid creds
                                        msg.contains("invalid login") ||
                                                msg.contains("invalid credentials") ||
                                                (msg.contains("invalid") && msg.contains("password")) ||
                                                (msg.contains("invalid") && msg.contains("email")) ->
                                            "Invalid email or password"

                                        // ✅ rate limit / too many attempts
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
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp), color = Color.White)
                    Spacer(Modifier.width(10.dp))
                }
                AnimatedModeText(
                    mode = mode,
                    loginText = "Login",
                    signUpText = "Create account",
                    color = Color.White,
                    style = TextStyle(fontWeight = FontWeight.SemiBold)
                )

            }

            Spacer(Modifier.height(14.dp))

            if (mode == AuthMode.Login) {
                var showForgotDialog by remember { mutableStateOf(false) }

                Text(
                    text = "Forget Password?",
                    color = primaryBtn,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        val e = email.trim()
                        if (e.isBlank()) {
                            errorText = "Enter your email first"
                            return@clickable
                        }
                        showForgotDialog = true
                    }
                )

                ForgotPasswordDialog(
                    visible = showForgotDialog,
                    email = email.trim(),
                    dark = dark,
                    primaryBtn = primaryBtn,
                    fieldBg = fieldBg,
                    onSendReset = { targetEmail ->
                        // Call your existing callback
                        onForgotPassword(targetEmail)
                    },
                    onDismissAndBackToLogin = {
                        showForgotDialog = false
                        // Bring them back to login flow
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

        Text(
            text = bottom,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.clickable {
                onSwitch(if (m == AuthMode.Login) AuthMode.SignUp else AuthMode.Login)
            }
        )
    }
}


@Suppress("AssignedValueIsNeverRead")
@Composable
private fun ForgotPasswordDialog(
    visible: Boolean,
    email: String,
    dark: Boolean,
    primaryBtn: Color,
    fieldBg: Color,
    onSendReset: (String) -> Unit,
    onDismissAndBackToLogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var checked by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(ForgotUiState.Confirm) }
    var msg by remember { mutableStateOf<String?>(null) }


    fun openGmail() {
        // 1) Try open Gmail inbox directly
        val gmailInbox = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.google.android.gm") // try Gmail first
        }

        try {
            ctx.startActivity(gmailInbox)
            return
        } catch (_: Exception) {
            // ignore → fallback
        }

        // 2) Fallback: open any email app inbox
        val anyMailApp = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            ctx.startActivity(Intent.createChooser(anyMailApp, "Open email app"))
            return
        } catch (_: Exception) {
            // ignore → fallback
        }

        // 3) Last resort: open Gmail web inbox
        val web = Intent(
            Intent.ACTION_VIEW,
            "https://mail.google.com".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ctx.startActivity(web)
    }


    AlertDialog(
        onDismissRequest = {
            // Prevent dismiss while "sending/waiting" if you want:
            if (state == ForgotUiState.Confirm) onDismiss()
        },
        containerColor = if (!dark) Color.White else Color(0xFF121522),
        tonalElevation = 6.dp,
        title = {
            Text(
                text = "Reset password",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Email "pill"
                Surface(
                    color = fieldBg,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "We will send a reset link to:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            email.ifBlank { "—" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                when (state) {
                    ForgotUiState.Confirm -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(enabled = email.isNotBlank()) { checked = !checked }
                                .padding(10.dp)
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { checked = it }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "I’m 100% sure I want to reset this email",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = "Tip: If you don’t receive it, check Spam/Junk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    ForgotUiState.Sending -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Sending reset email…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    ForgotUiState.WaitingToOpenEmail -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Done  Waiting 8 seconds…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    ForgotUiState.ReadyToOpenEmail -> {
                        Text(
                            text = "Email sent \nOpen your inbox to click the reset link.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        msg?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
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
                            msg = null
                            state = ForgotUiState.Sending

                            onSendReset(email)

                            scope.launch {
                                kotlinx.coroutines.delay(700)
                                state = ForgotUiState.WaitingToOpenEmail
                                kotlinx.coroutines.delay(8000)
                                state = ForgotUiState.ReadyToOpenEmail
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBtn, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Send reset email")
                    }
                }

                ForgotUiState.Sending, ForgotUiState.WaitingToOpenEmail -> {
                    Button(
                        enabled = false,
                        onClick = {},
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Please wait…")
                    }
                }

                ForgotUiState.ReadyToOpenEmail -> {
                    Button(
                        onClick = {
                            openGmail()
                            // Close dialog + go back to login screen
                            onDismissAndBackToLogin()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBtn, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Open email")
                    }
                }
            }
        },
        dismissButton = {
            when (state) {
                ForgotUiState.Confirm -> {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
                ForgotUiState.ReadyToOpenEmail -> {
                    TextButton(
                        onClick = {
                            // user wants to close but still go back to login
                            onDismissAndBackToLogin()
                        }
                    ) { Text("Back to login") }
                }
                else -> {
                    // disable cancel while waiting
                }
            }
        }
    )
}

private enum class ForgotUiState {
    Confirm,
    Sending,
    WaitingToOpenEmail,
    ReadyToOpenEmail
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
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
    )
}
