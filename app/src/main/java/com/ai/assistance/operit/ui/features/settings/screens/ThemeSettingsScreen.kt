package com.ai.assistance.operit.ui.features.settings.screens

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material3.*
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.preferences.UserPreferencesManager
import com.ai.assistance.operit.ui.features.settings.components.ColorPickerDialog
import com.ai.assistance.operit.ui.features.settings.components.ColorSelectionItem
import com.ai.assistance.operit.ui.features.settings.components.MediaTypeOption
import com.ai.assistance.operit.ui.features.settings.components.ThemeModeOption
import com.ai.assistance.operit.ui.features.settings.components.ChatStyleOption
import com.ai.assistance.operit.ui.features.settings.components.AvatarPicker
import com.ai.assistance.operit.ui.theme.getTextColorForBackground
import com.ai.assistance.operit.util.FileUtils
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.launch

// Add utility function to calculate the luminance of a color
private fun calculateLuminance(color: Color): Float {
    return 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { UserPreferencesManager(context) }
    val scope = rememberCoroutineScope()

    // Collect theme settings
    val themeMode =
            preferencesManager.themeMode.collectAsState(
                            initial = UserPreferencesManager.THEME_MODE_LIGHT
                    )
                    .value
    val useSystemTheme = preferencesManager.useSystemTheme.collectAsState(initial = true).value
    val customPrimaryColor =
            preferencesManager.customPrimaryColor.collectAsState(initial = null).value
    val customSecondaryColor =
            preferencesManager.customSecondaryColor.collectAsState(initial = null).value
    val useCustomColors = preferencesManager.useCustomColors.collectAsState(initial = false).value

    // Collect background image settings
    val useBackgroundImage =
            preferencesManager.useBackgroundImage.collectAsState(initial = false).value
    val backgroundImageUri =
            preferencesManager.backgroundImageUri.collectAsState(initial = null).value
    val backgroundImageOpacity =
            preferencesManager.backgroundImageOpacity.collectAsState(initial = 0.3f).value

    // Collect background media type and video settings
    val backgroundMediaType =
            preferencesManager.backgroundMediaType.collectAsState(
                            initial = UserPreferencesManager.MEDIA_TYPE_IMAGE
                    )
                    .value
    val videoBackgroundMuted =
            preferencesManager.videoBackgroundMuted.collectAsState(initial = true).value
    val videoBackgroundLoop =
            preferencesManager.videoBackgroundLoop.collectAsState(initial = true).value

    // Collect toolbar transparency setting
    val toolbarTransparent =
            preferencesManager.toolbarTransparent.collectAsState(initial = false).value

    // Collect status bar color settings
    val useCustomStatusBarColor =
            preferencesManager.useCustomStatusBarColor.collectAsState(initial = false).value
    val customStatusBarColor =
            preferencesManager.customStatusBarColor.collectAsState(initial = null).value
    val statusBarTransparent =
            preferencesManager.statusBarTransparent.collectAsState(initial = false).value
    val chatHeaderTransparent =
            preferencesManager.chatHeaderTransparent.collectAsState(initial = false).value
    val chatInputTransparent =
            preferencesManager.chatInputTransparent.collectAsState(initial = false).value
    val chatHeaderOverlayMode =
            preferencesManager.chatHeaderOverlayMode.collectAsState(initial = false).value

    // Collect AppBar content color settings
    val forceAppBarContentColor =
            preferencesManager.forceAppBarContentColor.collectAsState(initial = false).value
    val appBarContentColorMode =
            preferencesManager.appBarContentColorMode.collectAsState(
                            initial = UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_LIGHT
                    )
                    .value

    // Collect ChatHeader icon color settings
    val chatHeaderHistoryIconColor =
            preferencesManager.chatHeaderHistoryIconColor.collectAsState(initial = null).value
    val chatHeaderPipIconColor =
            preferencesManager.chatHeaderPipIconColor.collectAsState(initial = null).value

    // Collect background blur settings
    val useBackgroundBlur =
            preferencesManager.useBackgroundBlur.collectAsState(initial = false).value
    val backgroundBlurRadius =
            preferencesManager.backgroundBlurRadius.collectAsState(initial = 10f).value

    // Collect chat style setting
    val chatStyle = preferencesManager.chatStyle.collectAsState(initial = UserPreferencesManager.CHAT_STYLE_CURSOR).value

    // Collect new display settings
    val showThinkingProcess = preferencesManager.showThinkingProcess.collectAsState(initial = true).value
    val showStatusTags = preferencesManager.showStatusTags.collectAsState(initial = true).value
    val showInputProcessingStatus = preferencesManager.showInputProcessingStatus.collectAsState(initial = true).value

    // Collect avatar settings
    val userAvatarUri = preferencesManager.customUserAvatarUri.collectAsState(initial = null).value
    val aiAvatarUri = preferencesManager.customAiAvatarUri.collectAsState(initial = null).value
    val avatarShape = preferencesManager.avatarShape.collectAsState(initial = UserPreferencesManager.AVATAR_SHAPE_CIRCLE).value
    val avatarCornerRadius = preferencesManager.avatarCornerRadius.collectAsState(initial = 8f).value

    // Collect on color mode
    val onColorMode = preferencesManager.onColorMode.collectAsState(initial = UserPreferencesManager.ON_COLOR_MODE_AUTO).value

    // Collect custom chat title
    val customChatTitle = preferencesManager.customChatTitle.collectAsState(initial = null).value


    // Default color definitions
    val defaultPrimaryColor = Color.Magenta.toArgb()
    val defaultSecondaryColor = Color.Blue.toArgb()

    // Mutable state
    var themeModeInput by remember { mutableStateOf(themeMode) }
    var useSystemThemeInput by remember { mutableStateOf(useSystemTheme) }
    var primaryColorInput by remember { mutableStateOf(customPrimaryColor ?: defaultPrimaryColor) }
    var secondaryColorInput by remember {
        mutableStateOf(customSecondaryColor ?: defaultSecondaryColor)
    }
    var useCustomColorsInput by remember { mutableStateOf(useCustomColors) }

    // Background image state
    var useBackgroundImageInput by remember { mutableStateOf(useBackgroundImage) }
    var backgroundImageUriInput by remember { mutableStateOf(backgroundImageUri) }
    var backgroundImageOpacityInput by remember { mutableStateOf(backgroundImageOpacity) }

    // Background media type and video settings state
    var backgroundMediaTypeInput by remember { mutableStateOf(backgroundMediaType) }
    var videoBackgroundMutedInput by remember { mutableStateOf(videoBackgroundMuted) }
    var videoBackgroundLoopInput by remember { mutableStateOf(videoBackgroundLoop) }

    // Toolbar transparency state
    var toolbarTransparentInput by remember { mutableStateOf(toolbarTransparent) }

    // Status bar color state
    var useCustomStatusBarColorInput by remember { mutableStateOf(useCustomStatusBarColor) }
    var customStatusBarColorInput by remember { mutableStateOf(customStatusBarColor ?: defaultPrimaryColor) }
    var statusBarTransparentInput by remember { mutableStateOf(statusBarTransparent) }
    var chatHeaderTransparentInput by remember { mutableStateOf(chatHeaderTransparent) }
    var chatInputTransparentInput by remember { mutableStateOf(chatInputTransparent) }
    var chatHeaderOverlayModeInput by remember { mutableStateOf(chatHeaderOverlayMode) }

    // AppBar content color state
    var forceAppBarContentColorInput by remember { mutableStateOf(forceAppBarContentColor) }
    var appBarContentColorModeInput by remember { mutableStateOf(appBarContentColorMode) }

    // ChatHeader icon color state
    var chatHeaderHistoryIconColorInput by remember {
        mutableStateOf(chatHeaderHistoryIconColor ?: Color.Gray.toArgb())
    }
    var chatHeaderPipIconColorInput by remember {
        mutableStateOf(chatHeaderPipIconColor ?: Color.Gray.toArgb())
    }

    // Background blur state
    var useBackgroundBlurInput by remember { mutableStateOf(useBackgroundBlur) }
    var backgroundBlurRadiusInput by remember { mutableStateOf(backgroundBlurRadius) }

    // Chat style state
    var chatStyleInput by remember { mutableStateOf(chatStyle) }

    // New display settings state
    var showThinkingProcessInput by remember { mutableStateOf(showThinkingProcess) }
    var showStatusTagsInput by remember { mutableStateOf(showStatusTags) }
    var showInputProcessingStatusInput by remember { mutableStateOf(showInputProcessingStatus) }

    // Avatar state
    var userAvatarUriInput by remember { mutableStateOf(userAvatarUri) }
    var aiAvatarUriInput by remember { mutableStateOf(aiAvatarUri) }
    var avatarShapeInput by remember { mutableStateOf(avatarShape) }
    var avatarCornerRadiusInput by remember { mutableStateOf(avatarCornerRadius) }

    // On color mode state
    var onColorModeInput by remember { mutableStateOf(onColorMode) }

    // Custom chat title state
    var customChatTitleInput by remember(customChatTitle) { mutableStateOf(customChatTitle ?: "") }

    var showColorPicker by remember { mutableStateOf(false) }
    var currentColorPickerMode by remember { mutableStateOf("primary") }
    var showSaveSuccessMessage by remember { mutableStateOf(false) }

    // Video player state
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
                // Add stricter memory limits
                .setLoadControl(
                        DefaultLoadControl.Builder()
                                .setBufferDurationsMs(
                                        5000, // Minimum buffer time reduced to 5 seconds
                                        10000, // Maximum buffer time reduced to 10 seconds
                                        500, // Minimum buffer for playback
                                        1000 // Minimum buffer for playback after rebuffering
                                )
                                .setTargetBufferBytes(5 * 1024 * 1024) // Limit buffer to 5MB
                                .setPrioritizeTimeOverSizeThresholds(true)
                                .build()
                )
                .build()
                .apply {
                    // Set loop playback
                    repeatMode = Player.REPEAT_MODE_ALL
                    // Set mute
                    volume = if (videoBackgroundMutedInput) 0f else 1f
                    playWhenReady = true

                    // If there's a background video URI, load it
                    if (backgroundImageUriInput != null &&
                                    backgroundMediaTypeInput ==
                                            UserPreferencesManager.MEDIA_TYPE_VIDEO
                    ) {
                        try {
                            val mediaItem = MediaItem.fromUri(Uri.parse(backgroundImageUriInput))
                            setMediaItem(mediaItem)
                            prepare()
                        } catch (e: Exception) {
                            Log.e("ThemeSettings", "Video loading error", e)
                        }
                    }
                }
    }

    // Free ExoPlayer resources when component is destroyed
    DisposableEffect(Unit) {
        onDispose {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            } catch (e: Exception) {
                Log.e("ThemeSettings", "ExoPlayer release error", e)
            }
        }
    }

    // Handle video URI changes
    LaunchedEffect(backgroundImageUriInput, backgroundMediaTypeInput) {
        if (backgroundImageUriInput != null &&
                        backgroundMediaTypeInput == UserPreferencesManager.MEDIA_TYPE_VIDEO
        ) {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(backgroundImageUriInput)))
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                Log.e("ThemeSettings", "更新视频来源错误", e)
            }
        }
    }

    // Handle video settings changes - add error handling
    LaunchedEffect(videoBackgroundMutedInput, videoBackgroundLoopInput) {
        try {
            exoPlayer.volume = if (videoBackgroundMutedInput) 0f else 1f
            exoPlayer.repeatMode =
                    if (videoBackgroundLoopInput) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        } catch (e: Exception) {
            Log.e("ThemeSettings", "更新视频设置错误", e)
        }
    }

    // Image crop launcher
    val cropImageLauncher =
            rememberLauncherForActivityResult(CropImageContract()) { result ->
                if (result.isSuccessful) {
                    val croppedUri = result.uriContent
                    if (croppedUri != null) {
                        scope.launch {
                            val internalUri =
                                    FileUtils.copyFileToInternalStorage(context, croppedUri, "background")
                            if (internalUri != null) {
                                Log.d("ThemeSettings", "Background image saved to: $internalUri")
                                backgroundImageUriInput = internalUri.toString()
                                backgroundMediaTypeInput = UserPreferencesManager.MEDIA_TYPE_IMAGE
                                preferencesManager.saveThemeSettings(
                                        backgroundImageUri = internalUri.toString(),
                                        backgroundMediaType =
                                                UserPreferencesManager.MEDIA_TYPE_IMAGE
                                )
                                showSaveSuccessMessage = true
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.theme_image_saved),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            } else {
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.theme_copy_failed),
                                                Toast.LENGTH_LONG
                                        )
                                        .show()
                            }
                        }
                    }
                } else if (result.error != null) {
                    Toast.makeText(
                                    context,
                                    context.getString(
                                            R.string.theme_image_crop_failed,
                                            result.error!!.message
                                    ),
                                    Toast.LENGTH_LONG
                            )
                            .show()
                }
            }

    // Launch image crop function
    fun launchImageCrop(uri: Uri) {
        // Use safe way to get system colors
        var primaryColor: Int
        var onPrimaryColor: Int
        var surfaceColor: Int
        var statusBarColor: Int

        // Check system dark theme
        val isNightMode =
                context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES

        try {
            // Try to use theme colors - this is a fallback option
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            primaryColor = typedValue.data

            // Try to get system status bar color (API 23+)
            try {
                context.theme.resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true)
                statusBarColor = typedValue.data
            } catch (e: Exception) {
                // If unable to get, use theme color
                statusBarColor = primaryColor
            }

            context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
            surfaceColor = typedValue.data

            onPrimaryColor =
                    if (isNightMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        } catch (e: Exception) {
            // Use fallback colors
            primaryColor = if (isNightMode) 0xFF9C27B0.toInt() else 0xFF6200EE.toInt() // Purple
            statusBarColor =
                    if (isNightMode) 0xFF7B1FA2.toInt() else 0xFF3700B3.toInt() // Dark purple
            surfaceColor =
                    if (isNightMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            onPrimaryColor =
                    if (isNightMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        }

        val cropOptions =
                CropImageContractOptions(
                        uri,
                        CropImageOptions().apply {
                            guidelines = com.canhub.cropper.CropImageView.Guidelines.ON
                            outputCompressFormat = android.graphics.Bitmap.CompressFormat.JPEG
                            outputCompressQuality = 90
                            fixAspectRatio = false
                            cropMenuCropButtonTitle = context.getString(R.string.theme_crop_done)
                            activityTitle = context.getString(R.string.theme_crop_image)

                            // Set theme colors
                            toolbarColor = primaryColor
                            toolbarBackButtonColor = onPrimaryColor
                            toolbarTitleColor = onPrimaryColor
                            activityBackgroundColor = surfaceColor
                            backgroundColor = surfaceColor

                            // Status bar color
                            statusBarColor = statusBarColor

                            // Use light/dark theme
                            activityMenuIconColor = onPrimaryColor

                            // Improve user experience
                            showCropOverlay = true
                            showProgressBar = true
                            multiTouchEnabled = true
                            autoZoomEnabled = true
                        }
                )
        cropImageLauncher.launch(cropOptions)
    }

    // Image/video picker launcher
    val mediaPickerLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
                    uri: Uri? ->
                if (uri != null) {
                    // Check if it's a video file
                    val isVideo = FileUtils.isVideoFile(context, uri)

                    if (isVideo) {
                        // Video file check size
                        val isVideoSizeAcceptable =
                                FileUtils.checkVideoSize(context, uri, 30) // Limit to 30MB

                        if (!isVideoSizeAcceptable) {
                            // Video too large, show warning
                            Toast.makeText(
                                            context,
                                            context.getString(R.string.theme_video_too_large),
                                            Toast.LENGTH_LONG
                                    )
                                    .show()
                            return@rememberLauncherForActivityResult
                        }

                        // Video file size acceptable, directly save
                        scope.launch {
                            val internalUri = FileUtils.copyFileToInternalStorage(context, uri, "background_video")

                            if (internalUri != null) {
                                Log.d("ThemeSettings", "Background video saved to: $internalUri")
                                backgroundImageUriInput = internalUri.toString()
                                backgroundMediaTypeInput = UserPreferencesManager.MEDIA_TYPE_VIDEO
                                preferencesManager.saveThemeSettings(
                                        backgroundImageUri = internalUri.toString(),
                                        backgroundMediaType =
                                                UserPreferencesManager.MEDIA_TYPE_VIDEO
                                )
                                showSaveSuccessMessage = true
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.theme_video_saved),
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                            } else {
                                Toast.makeText(
                                                context,
                                                context.getString(R.string.theme_copy_failed),
                                                Toast.LENGTH_LONG
                                        )
                                        .show()
                            }
                        }
                    } else {
                        // Image file first launch crop
                        launchImageCrop(uri)
                    }
                }
            }

    // Migrate existing background image if needed (on first load)
    LaunchedEffect(Unit) {
        // Check if we have a background image URI that starts with content://
        backgroundImageUri?.let { uriString ->
            if (uriString.startsWith("content://")) {
                try {
                    // Try to copy to internal storage
                    val uri = Uri.parse(uriString)
                    scope.launch {
                        val internalUri = FileUtils.copyFileToInternalStorage(context, uri, "migrated_background")
                        if (internalUri != null) {
                            Log.d("ThemeSettings", "Migrated background image to: $internalUri")
                            // Update the URI in preferences
                            preferencesManager.saveThemeSettings(
                                    backgroundImageUri = internalUri.toString()
                            )
                            // Update the local state
                            backgroundImageUriInput = internalUri.toString()
                            Toast.makeText(context, context.getString(R.string.background_image_migrated), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ThemeSettings", "Error migrating background image", e)
                    // If migration fails, disable background image to prevent
                    // crashes
                    scope.launch {
                        preferencesManager.saveThemeSettings(useBackgroundImage = false)
                        useBackgroundImageInput = false
                        Toast.makeText(context, context.getString(R.string.background_image_access_failed), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // When settings change, update local state
    LaunchedEffect(
            themeMode,
            useSystemTheme,
            customPrimaryColor,
            customSecondaryColor,
            useCustomColors,
            useBackgroundImage,
            backgroundImageUri,
            backgroundImageOpacity,
            backgroundMediaType,
            videoBackgroundMuted,
            videoBackgroundLoop,
            toolbarTransparent,
            useCustomStatusBarColor,
            customStatusBarColor,
            statusBarTransparent,
            chatHeaderTransparent,
            chatInputTransparent,
            chatHeaderOverlayMode,
            forceAppBarContentColor,
            appBarContentColorMode,
            chatHeaderHistoryIconColor,
            chatHeaderPipIconColor,
            useBackgroundBlur,
            backgroundBlurRadius,
            chatStyle,
            showThinkingProcess,
            showStatusTags,
            showInputProcessingStatus,
            userAvatarUri,
            aiAvatarUri,
            avatarShape,
            avatarCornerRadius,
            onColorMode,
            customChatTitle
    ) {
        themeModeInput = themeMode
        useSystemThemeInput = useSystemTheme
        if (customPrimaryColor != null) primaryColorInput = customPrimaryColor
        if (customSecondaryColor != null) secondaryColorInput = customSecondaryColor
        useCustomColorsInput = useCustomColors
        useBackgroundImageInput = useBackgroundImage
        backgroundImageUriInput = backgroundImageUri
        backgroundImageOpacityInput = backgroundImageOpacity
        backgroundMediaTypeInput = backgroundMediaType
        videoBackgroundMutedInput = videoBackgroundMuted
        videoBackgroundLoopInput = videoBackgroundLoop
        toolbarTransparentInput = toolbarTransparent
        useCustomStatusBarColorInput = useCustomStatusBarColor
        if (customStatusBarColor != null) customStatusBarColorInput = customStatusBarColor
        statusBarTransparentInput = statusBarTransparent
        chatHeaderTransparentInput = chatHeaderTransparent
        chatInputTransparentInput = chatInputTransparent
        chatHeaderOverlayModeInput = chatHeaderOverlayMode
        forceAppBarContentColorInput = forceAppBarContentColor
        appBarContentColorModeInput = appBarContentColorMode
        if (chatHeaderHistoryIconColor != null) {
            chatHeaderHistoryIconColorInput = chatHeaderHistoryIconColor
        }
        if (chatHeaderPipIconColor != null) {
            chatHeaderPipIconColorInput = chatHeaderPipIconColor
        }
        useBackgroundBlurInput = useBackgroundBlur
        backgroundBlurRadiusInput = backgroundBlurRadius
        chatStyleInput = chatStyle
        showThinkingProcessInput = showThinkingProcess
        showStatusTagsInput = showStatusTags
        showInputProcessingStatusInput = showInputProcessingStatus
        userAvatarUriInput = userAvatarUri
        aiAvatarUriInput = aiAvatarUri
        avatarShapeInput = avatarShape
        avatarCornerRadiusInput = avatarCornerRadius
        onColorModeInput = onColorMode
        customChatTitleInput = customChatTitle ?: ""
    }

    // Avatar picker and cropper launcher
    var avatarPickerMode by remember { mutableStateOf("user") }

    val cropAvatarLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedUri = result.uriContent
            if (croppedUri != null) {
                scope.launch {
                    val uniqueName = if (avatarPickerMode == "user") "user_avatar" else "ai_avatar"
                    val internalUri = FileUtils.copyFileToInternalStorage(context, croppedUri, uniqueName)
                    if (internalUri != null) {
                        if (avatarPickerMode == "user") {
                            Log.d("ThemeSettings", "User avatar saved to: $internalUri")
                            userAvatarUriInput = internalUri.toString()
                            preferencesManager.saveThemeSettings(customUserAvatarUri = internalUri.toString())
                        } else { // "ai"
                            Log.d("ThemeSettings", "AI avatar saved to: $internalUri")
                            aiAvatarUriInput = internalUri.toString()
                            preferencesManager.saveThemeSettings(customAiAvatarUri = internalUri.toString())
                        }
                        showSaveSuccessMessage = true
                        Toast.makeText(context, context.getString(R.string.avatar_updated), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.theme_copy_failed), Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else if (result.error != null) {
            Toast.makeText(context, context.getString(R.string.avatar_crop_failed, result.error!!.message), Toast.LENGTH_LONG).show()
        }
    }

    fun launchAvatarCrop(uri: Uri) {
        val cropOptions = CropImageContractOptions(
            uri,
            CropImageOptions().apply {
                guidelines = com.canhub.cropper.CropImageView.Guidelines.ON
                outputCompressFormat = android.graphics.Bitmap.CompressFormat.PNG
                outputCompressQuality = 90
                fixAspectRatio = true
                aspectRatioX = 1
                aspectRatioY = 1
                cropMenuCropButtonTitle = context.getString(R.string.theme_crop_done)
                activityTitle = "裁剪头像"
                // Basic theming, can be expanded later
                toolbarColor = Color.Gray.toArgb()
                toolbarTitleColor = Color.White.toArgb()
            }
        )
        cropAvatarLauncher.launch(cropOptions)
    }

    val avatarImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            launchAvatarCrop(uri)
        }
    }


    // Get background image state to check if we need opaque cards
    val hasBackgroundImage =
            preferencesManager.useBackgroundImage.collectAsState(initial = false).value

    // Color surface modifier based on whether background image is used
    val cardModifier =
            if (hasBackgroundImage) {
                // Make cards fully opaque when background image is used
                CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
                )
            } else {
                CardDefaults.cardColors()
            }

    // Add a scroll state that we can control
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        // ======= SECTION 1: THEME MODE =======
        ThemeSectionTitle(
                title = stringResource(id = R.string.theme_title_mode),
                icon = Icons.Default.Brightness4
        )

        // System theme settings
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_system_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )

                // Follow system theme
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(id = R.string.theme_follow_system),
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                text = stringResource(id = R.string.theme_follow_system_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                            checked = useSystemThemeInput,
                            onCheckedChange = {
                                useSystemThemeInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(useSystemTheme = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }

                // Only show theme selection when not following system
                if (!useSystemThemeInput) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                            text = stringResource(id = R.string.theme_select),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Theme mode selection
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeModeOption(
                                title = stringResource(id = R.string.theme_light),
                                selected =
                                        themeModeInput == UserPreferencesManager.THEME_MODE_LIGHT,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    themeModeInput = UserPreferencesManager.THEME_MODE_LIGHT
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(
                                                themeMode = UserPreferencesManager.THEME_MODE_LIGHT
                                        )
                                        showSaveSuccessMessage = true
                                    }
                                }
                        )

                        ThemeModeOption(
                                title = stringResource(id = R.string.theme_dark),
                                selected = themeModeInput == UserPreferencesManager.THEME_MODE_DARK,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    themeModeInput = UserPreferencesManager.THEME_MODE_DARK
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(
                                                themeMode = UserPreferencesManager.THEME_MODE_DARK
                                        )
                                        showSaveSuccessMessage = true
                                    }
                                }
                        )
                    }
                }
            }
        }

        // ======= SECTION 2: COLOR CUSTOMIZATION =======
        ThemeSectionTitle(
                title = stringResource(id = R.string.theme_title_color),
                icon = Icons.Default.ColorLens
        )

        // Add status bar color settings
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_statusbar_color),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )

                // Status bar transparent switch
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(id = R.string.theme_statusbar_transparent),
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                text = stringResource(id = R.string.theme_statusbar_transparent_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = statusBarTransparentInput,
                            onCheckedChange = {
                                statusBarTransparentInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(statusBarTransparent = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Use custom status bar color switch
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(id = R.string.theme_use_custom_statusbar_color),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (statusBarTransparentInput) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = stringResource(id = R.string.theme_use_custom_statusbar_color_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (statusBarTransparentInput) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = useCustomStatusBarColorInput,
                            enabled = !statusBarTransparentInput,
                            onCheckedChange = {
                                useCustomStatusBarColorInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(useCustomStatusBarColor = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }

                if (useCustomStatusBarColorInput) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    // Status bar color selection
                    ColorSelectionItem(
                            title = stringResource(id = R.string.theme_statusbar_color),
                            color = Color(customStatusBarColorInput),
                            enabled = !statusBarTransparentInput,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!statusBarTransparentInput) {
                                    currentColorPickerMode = "statusBar"
                                    showColorPicker = true
                                }
                            }
                    )
                }
            }
        }


        // Add toolbar transparency settings
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_toolbar_transparent),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )

                // Toolbar transparency
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.theme_toolbar_transparent_desc), style = MaterialTheme.typography.bodyMedium)
                        Text(
                                text = stringResource(id = R.string.theme_toolbar_transparent_desc_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = toolbarTransparentInput,
                            onCheckedChange = {
                                toolbarTransparentInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(toolbarTransparent = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }
            }
        }

        // Chat header transparency
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_chat_header_transparent_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.theme_chat_header_transparent), style = MaterialTheme.typography.bodyMedium)
                        Text(
                                text = stringResource(id = R.string.theme_chat_header_transparent_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = chatHeaderTransparentInput,
                            onCheckedChange = {
                                chatHeaderTransparentInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(chatHeaderTransparent = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }

                if (chatHeaderTransparentInput) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                    text = stringResource(id = R.string.theme_chat_header_overlay_mode),
                                    style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                    text = stringResource(id = R.string.theme_chat_header_overlay_mode_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                                checked = chatHeaderOverlayModeInput,
                                onCheckedChange = {
                                    chatHeaderOverlayModeInput = it
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(
                                                chatHeaderOverlayMode = it
                                        )
                                        showSaveSuccessMessage = true
                                    }
                                }
                        )
                    }
                }
            }
        }

        // Chat input transparency
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_chat_input_transparent_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.theme_chat_input_transparent), style = MaterialTheme.typography.bodyMedium)
                        Text(
                                text = stringResource(id = R.string.theme_chat_input_transparent_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = chatInputTransparentInput,
                            onCheckedChange = {
                                chatInputTransparentInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(chatInputTransparent = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }
            }
        }

        // Add AppBar content color settings
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_appbar_content_color_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )

                // Force AppBar content color
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(id = R.string.theme_force_appbar_content_color),
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                text = stringResource(id = R.string.theme_force_appbar_content_color_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = forceAppBarContentColorInput,
                            onCheckedChange = {
                                forceAppBarContentColorInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(forceAppBarContentColor = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }

                if (forceAppBarContentColorInput) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                            text = stringResource(id = R.string.theme_appbar_content_color_mode),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeModeOption(
                                title = stringResource(id = R.string.theme_appbar_content_color_light),
                                selected = appBarContentColorModeInput == UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_LIGHT,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    appBarContentColorModeInput = UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_LIGHT
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(appBarContentColorMode = UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_LIGHT)
                                        showSaveSuccessMessage = true
                                    }
                                }
                        )
                        ThemeModeOption(
                                title = stringResource(id = R.string.theme_appbar_content_color_dark),
                                selected = appBarContentColorModeInput == UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_DARK,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    appBarContentColorModeInput = UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_DARK
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(appBarContentColorMode = UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_DARK)
                                        showSaveSuccessMessage = true
                                    }
                                }
                        )
                    }
                }
            }
        }

        // ChatHeader icon color settings
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_chat_header_icons_color_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
                ColorSelectionItem(
                        title = stringResource(id = R.string.theme_chat_header_history_icon_color),
                        color = Color(chatHeaderHistoryIconColorInput),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            currentColorPickerMode = "historyIcon"
                            showColorPicker = true
                        }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ColorSelectionItem(
                        title = stringResource(id = R.string.theme_chat_header_pip_icon_color),
                        color = Color(chatHeaderPipIconColorInput),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            currentColorPickerMode = "pipIcon"
                            showColorPicker = true
                        }
                )
            }
        }

        // Custom color settings
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_custom_color),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )

                // Whether to use custom colors
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(id = R.string.theme_use_custom_color),
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                text = stringResource(id = R.string.theme_custom_color_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                            checked = useCustomColorsInput,
                            onCheckedChange = {
                                useCustomColorsInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(useCustomColors = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }

                // Only show color selection when custom colors are enabled
                if (useCustomColorsInput) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                            text = stringResource(id = R.string.theme_select_color),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Color selection
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Primary color selection
                        ColorSelectionItem(
                                title = stringResource(id = R.string.theme_primary_color),
                                color = Color(primaryColorInput),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    currentColorPickerMode = "primary"
                                    showColorPicker = true
                                }
                        )

                        // Secondary color selection
                        ColorSelectionItem(
                                title = stringResource(id = R.string.theme_secondary_color),
                                color = Color(secondaryColorInput),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    currentColorPickerMode = "secondary"
                                    showColorPicker = true
                                }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = stringResource(id = R.string.theme_on_color_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeModeOption(
                            title = stringResource(id = R.string.theme_on_color_auto),
                            selected = onColorModeInput == UserPreferencesManager.ON_COLOR_MODE_AUTO,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onColorModeInput = UserPreferencesManager.ON_COLOR_MODE_AUTO
                                scope.launch {
                                    preferencesManager.saveThemeSettings(onColorMode = UserPreferencesManager.ON_COLOR_MODE_AUTO)
                                    showSaveSuccessMessage = true
                                }
                            }
                        )
                        ThemeModeOption(
                            title = stringResource(id = R.string.theme_on_color_light),
                            selected = onColorModeInput == UserPreferencesManager.ON_COLOR_MODE_LIGHT,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onColorModeInput = UserPreferencesManager.ON_COLOR_MODE_LIGHT
                                scope.launch {
                                    preferencesManager.saveThemeSettings(onColorMode = UserPreferencesManager.ON_COLOR_MODE_LIGHT)
                                    showSaveSuccessMessage = true
                                }
                            }
                        )
                        ThemeModeOption(
                            title = stringResource(id = R.string.theme_on_color_dark),
                            selected = onColorModeInput == UserPreferencesManager.ON_COLOR_MODE_DARK,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onColorModeInput = UserPreferencesManager.ON_COLOR_MODE_DARK
                                scope.launch {
                                    preferencesManager.saveThemeSettings(onColorMode = UserPreferencesManager.ON_COLOR_MODE_DARK)
                                    showSaveSuccessMessage = true
                                }
                                }
                        )
                    }

                    // Add a color preview section to show how colors will look
                    Text(
                            text = stringResource(id = R.string.theme_preview),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Create a mini-preview of how the selected colors will look
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        // Primary color demo
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            val primaryColor = Color(primaryColorInput)
                            val onPrimaryColor = getTextColorForBackground(primaryColor)

                            // Primary button preview
                            Surface(
                                    modifier =
                                            Modifier.weight(1f).height(40.dp).padding(end = 8.dp),
                                    color = primaryColor,
                                    shape = RoundedCornerShape(4.dp)
                            ) {
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                            stringResource(id = R.string.theme_primary_button),
                                            color = onPrimaryColor,
                                            style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            // Secondary button preview
                            val secondaryColor = Color(secondaryColorInput)
                            val onSecondaryColor = getTextColorForBackground(secondaryColor)

                            Surface(
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    color = secondaryColor,
                                    shape = RoundedCornerShape(4.dp)
                            ) {
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                            stringResource(id = R.string.theme_secondary_button),
                                            color = onSecondaryColor,
                                            style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        Text(
                                text = stringResource(id = R.string.theme_contrast_tip),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Save custom colors button
                    Button(
                            onClick = {
                                scope.launch {
                                    preferencesManager.saveThemeSettings(
                                            customPrimaryColor = primaryColorInput,
                                            customSecondaryColor = secondaryColorInput
                                    )
                                    showSaveSuccessMessage = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) { Text(stringResource(id = R.string.theme_save_colors)) }
                }
            }
        }

        // ======= SECTION 2: CHAT STYLE =======
        ThemeSectionTitle(
            title = stringResource(id = R.string.chat_style_title),
            icon = Icons.Default.ColorLens // 您可以根据需要更改图标
        )

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.chat_style_desc),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChatStyleOption(
                        title = stringResource(id = R.string.chat_style_cursor),
                        selected = chatStyleInput == UserPreferencesManager.CHAT_STYLE_CURSOR,
                        modifier = Modifier.weight(1f)
                    ) {
                        chatStyleInput = UserPreferencesManager.CHAT_STYLE_CURSOR
                        scope.launch {
                            preferencesManager.saveThemeSettings(chatStyle = UserPreferencesManager.CHAT_STYLE_CURSOR)
                            showSaveSuccessMessage = true
                        }
                    }

                    ChatStyleOption(
                        title = stringResource(id = R.string.chat_style_bubble),
                        selected = chatStyleInput == UserPreferencesManager.CHAT_STYLE_BUBBLE,
                        modifier = Modifier.weight(1f)
                    ) {
                        chatStyleInput = UserPreferencesManager.CHAT_STYLE_BUBBLE
                        scope.launch {
                            preferencesManager.saveThemeSettings(chatStyle = UserPreferencesManager.CHAT_STYLE_BUBBLE)
                            showSaveSuccessMessage = true
                        }
                    }
                }
            }
        }

        // ======= SECTION 4: DISPLAY OPTIONS =======
        ThemeSectionTitle(
            title = stringResource(id = R.string.display_options_title),
            icon = Icons.Default.ColorLens // Replace with a more appropriate icon if available
        )
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Show thinking process switch
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.show_thinking_process), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(id = R.string.show_thinking_process_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showThinkingProcessInput,
                        onCheckedChange = {
                            showThinkingProcessInput = it
                            scope.launch {
                                preferencesManager.saveThemeSettings(showThinkingProcess = it)
                                showSaveSuccessMessage = true
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Show status tags switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.show_status_tags), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(id = R.string.show_status_tags_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showStatusTagsInput,
                        onCheckedChange = {
                            showStatusTagsInput = it
                            scope.launch {
                                preferencesManager.saveThemeSettings(showStatusTags = it)
                                showSaveSuccessMessage = true
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Show input processing status switch
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.show_input_processing_status), style = MaterialTheme.typography.bodyMedium)
                        Text(
                                text = stringResource(id = R.string.show_input_processing_status_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                            checked = showInputProcessingStatusInput,
                            onCheckedChange = {
                                showInputProcessingStatusInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(showInputProcessingStatus = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }
            }
        }

        // ======= SECTION 5: AVATAR CUSTOMIZATION =======
        ThemeSectionTitle(
            title = stringResource(id = R.string.avatar_customization_title),
            icon = Icons.Default.Person // Replace with a more appropriate icon if available
        )
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User Avatar Picker
                    AvatarPicker(
                        label = stringResource(id = R.string.user_avatar_label),
                        avatarUri = userAvatarUriInput,
                        onAvatarChange = {
                            avatarPickerMode = "user"
                            avatarImagePicker.launch("image/*")
                        },
                        onAvatarReset = {
                            userAvatarUriInput = null
                            scope.launch {
                                preferencesManager.saveThemeSettings(customUserAvatarUri = "")
                            }
                        }
                    )

                    // AI Avatar Picker
                    AvatarPicker(
                        label = stringResource(id = R.string.ai_avatar_label),
                        avatarUri = aiAvatarUriInput,
                        onAvatarChange = {
                            avatarPickerMode = "ai"
                            avatarImagePicker.launch("image/*")
                        },
                        onAvatarReset = {
                            aiAvatarUriInput = null
                            scope.launch {
                                preferencesManager.saveThemeSettings(customAiAvatarUri = "")
                            }
                        }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = stringResource(id = R.string.avatar_shape_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChatStyleOption(
                        title = stringResource(id = R.string.avatar_shape_circle),
                        selected = avatarShapeInput == UserPreferencesManager.AVATAR_SHAPE_CIRCLE,
                        modifier = Modifier.weight(1f)
                    ) {
                        avatarShapeInput = UserPreferencesManager.AVATAR_SHAPE_CIRCLE
                        scope.launch {
                            preferencesManager.saveThemeSettings(avatarShape = UserPreferencesManager.AVATAR_SHAPE_CIRCLE)
                            showSaveSuccessMessage = true
                        }
                    }
                    ChatStyleOption(
                        title = stringResource(id = R.string.avatar_shape_square),
                        selected = avatarShapeInput == UserPreferencesManager.AVATAR_SHAPE_SQUARE,
                        modifier = Modifier.weight(1f)
                    ) {
                        avatarShapeInput = UserPreferencesManager.AVATAR_SHAPE_SQUARE
                        scope.launch {
                            preferencesManager.saveThemeSettings(avatarShape = UserPreferencesManager.AVATAR_SHAPE_SQUARE)
                            showSaveSuccessMessage = true
                        }
                    }
                }

                AnimatedVisibility(visible = avatarShapeInput == UserPreferencesManager.AVATAR_SHAPE_SQUARE) {
                    Column {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = stringResource(id = R.string.avatar_corner_radius),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newValue = (avatarCornerRadiusInput - 1f).coerceIn(0f, 16f)
                                    avatarCornerRadiusInput = newValue
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(avatarCornerRadius = newValue)
                                        showSaveSuccessMessage = true
                                    }
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = stringResource(id = R.string.avatar_corner_decrease))
                            }

                            Text(
                                text = "${avatarCornerRadiusInput.toInt()} dp",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            OutlinedButton(
                                onClick = {
                                    val newValue = (avatarCornerRadiusInput + 1f).coerceIn(0f, 16f)
                                    avatarCornerRadiusInput = newValue
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(avatarCornerRadius = newValue)
                                        showSaveSuccessMessage = true
                                    }
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.avatar_corner_increase))
                            }
                        }
                    }
                }
            }
        }


        // ======= SECTION 3: BACKGROUND CUSTOMIZATION =======
        ThemeSectionTitle(
                title = stringResource(id = R.string.theme_title_background),
                icon = Icons.Default.Image
        )

        // Background media settings
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(id = R.string.theme_bg_media),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                )

                // Whether to use background image
                Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = stringResource(id = R.string.theme_use_custom_bg),
                                style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                                text = stringResource(id = R.string.theme_custom_bg_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                            checked = useBackgroundImageInput,
                            onCheckedChange = {
                                useBackgroundImageInput = it
                                scope.launch {
                                    preferencesManager.saveThemeSettings(useBackgroundImage = it)
                                    showSaveSuccessMessage = true
                                }
                            }
                    )
                }

                // Only show image selection when background image is enabled
                if (useBackgroundImageInput) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Media type selection
                    Text(
                            text = stringResource(id = R.string.theme_media_type),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MediaTypeOption(
                                title = stringResource(id = R.string.theme_media_image),
                                icon = Icons.Default.Image,
                                selected =
                                        backgroundMediaTypeInput ==
                                                UserPreferencesManager.MEDIA_TYPE_IMAGE,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    backgroundMediaTypeInput =
                                            UserPreferencesManager.MEDIA_TYPE_IMAGE
                                    if (backgroundImageUriInput != null) {
                                        // If there's already a background, save the media type
                                        scope.launch {
                                            preferencesManager.saveThemeSettings(
                                                    backgroundMediaType =
                                                            UserPreferencesManager.MEDIA_TYPE_IMAGE
                                            )
                                            showSaveSuccessMessage = true
                                        }
                                    }
                                }
                        )

                        MediaTypeOption(
                                title = stringResource(id = R.string.theme_media_video),
                                icon = Icons.Default.Videocam,
                                selected =
                                        backgroundMediaTypeInput ==
                                                UserPreferencesManager.MEDIA_TYPE_VIDEO,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    backgroundMediaTypeInput =
                                            UserPreferencesManager.MEDIA_TYPE_VIDEO
                                    if (backgroundImageUriInput != null) {
                                        // If there's already a background, save the media type
                                        scope.launch {
                                            preferencesManager.saveThemeSettings(
                                                    backgroundMediaType =
                                                            UserPreferencesManager.MEDIA_TYPE_VIDEO
                                            )
                                            showSaveSuccessMessage = true
                                        }
                                    }
                                }
                        )
                    }

                    // Current selected media preview
                    if (backgroundImageUriInput != null) {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(200.dp)
                                                .padding(bottom = 16.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline,
                                                        RoundedCornerShape(8.dp)
                                                )
                                                .background(Color.Black.copy(alpha = 0.1f))
                        ) {
                            if (backgroundMediaTypeInput == UserPreferencesManager.MEDIA_TYPE_IMAGE
                            ) {
                                // Image preview
                                Image(
                                        painter =
                                                rememberAsyncImagePainter(
                                                        Uri.parse(backgroundImageUriInput)
                                                ),
                                        contentDescription = "背景图片预览",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                )

                                // Crop button
                                IconButton(
                                        onClick = {
                                            backgroundImageUriInput?.let {
                                                launchImageCrop(Uri.parse(it))
                                            }
                                        },
                                        modifier =
                                                Modifier.align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .background(
                                                                MaterialTheme.colorScheme.surface
                                                                        .copy(alpha = 0.7f),
                                                                CircleShape
                                                        )
                                ) {
                                    Icon(
                                            imageVector = Icons.Default.Crop,
                                            contentDescription = "重新裁剪",
                                            tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                // Video preview
                                // Capture the background color from the Composable context
                                val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
                                // Determine if it's a light theme
                                val isLightTheme =
                                        calculateLuminance(MaterialTheme.colorScheme.background) >
                                                0.5f

                                AndroidView(
                                        factory = { ctx ->
                                            StyledPlayerView(ctx).apply {
                                                player = exoPlayer
                                                useController = false
                                                layoutParams =
                                                        ViewGroup.LayoutParams(
                                                                MATCH_PARENT,
                                                                MATCH_PARENT
                                                        )
                                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                // Use the captured background color
                                                setBackgroundColor(backgroundColor)
                                                // Create a semi-transparent overlay on the player
                                                // itself for opacity control
                                                foreground =
                                                        android.graphics.drawable.ColorDrawable(
                                                                android.graphics.Color.argb(
                                                                        ((1f -
                                                                                        backgroundImageOpacityInput) *
                                                                                        255)
                                                                                .toInt(),
                                                                        // Use white for light
                                                                        // theme, black for dark
                                                                        // theme
                                                                        if (isLightTheme) 255
                                                                        else 0,
                                                                        if (isLightTheme) 255
                                                                        else 0,
                                                                        if (isLightTheme) 255 else 0
                                                                )
                                                        )
                                            }
                                        },
                                        update = { view ->
                                            // Update the foreground transparency when opacity
                                            // changes
                                            view.foreground =
                                                    android.graphics.drawable.ColorDrawable(
                                                            android.graphics.Color.argb(
                                                                    ((1f -
                                                                                    backgroundImageOpacityInput) *
                                                                                    255)
                                                                            .toInt(),
                                                                    // Use white for light theme,
                                                                    // black for dark theme
                                                                    if (isLightTheme) 255 else 0,
                                                                    if (isLightTheme) 255 else 0,
                                                                    if (isLightTheme) 255 else 0
                                                            )
                                                    )
                                        },
                                        modifier = Modifier.fillMaxSize()
                                )

                                // Video control buttons
                                Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                                    // Mute button
                                    IconButton(
                                            onClick = {
                                                videoBackgroundMutedInput =
                                                        !videoBackgroundMutedInput
                                                scope.launch {
                                                    preferencesManager.saveThemeSettings(
                                                            videoBackgroundMuted =
                                                                    videoBackgroundMutedInput
                                                    )
                                                    showSaveSuccessMessage = true
                                                }
                                            },
                                            modifier =
                                                    Modifier.padding(end = 8.dp)
                                                            .background(
                                                                    MaterialTheme.colorScheme
                                                                            .surface.copy(
                                                                            alpha = 0.7f
                                                                    ),
                                                                    CircleShape
                                                            )
                                    ) {
                                        Icon(
                                                imageVector =
                                                        if (videoBackgroundMutedInput)
                                                                Icons.Default.VolumeOff
                                                        else Icons.Default.VolumeUp,
                                                contentDescription =
                                                        if (videoBackgroundMutedInput)
                                                                stringResource(
                                                                        id = R.string.theme_unmute
                                                                )
                                                        else
                                                                stringResource(
                                                                        id = R.string.theme_mute
                                                                ),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Loop button
                                    IconButton(
                                            onClick = {
                                                videoBackgroundLoopInput = !videoBackgroundLoopInput
                                                scope.launch {
                                                    preferencesManager.saveThemeSettings(
                                                            videoBackgroundLoop =
                                                                    videoBackgroundLoopInput
                                                    )
                                                    showSaveSuccessMessage = true

                                                    // Show Toast notification about status change
                                                    Toast.makeText(
                                                                    context,
                                                                    if (videoBackgroundLoopInput)
                                                                            context.getString(
                                                                                    R.string
                                                                                            .theme_loop_enabled
                                                                            )
                                                                    else
                                                                            context.getString(
                                                                                    R.string
                                                                                            .theme_loop_disabled
                                                                            ),
                                                                    Toast.LENGTH_SHORT
                                                            )
                                                            .show()
                                                }
                                            },
                                            modifier =
                                                    Modifier.background(
                                                            // Show different background color based
                                                            // on loop state
                                                            if (videoBackgroundLoopInput)
                                                                    MaterialTheme.colorScheme
                                                                            .primary.copy(
                                                                            alpha = 0.3f
                                                                    )
                                                            else
                                                                    MaterialTheme.colorScheme
                                                                            .surface.copy(
                                                                            alpha = 0.7f
                                                                    ),
                                                            CircleShape
                                                    )
                                    ) {
                                        Icon(
                                                if (videoBackgroundLoopInput) Icons.Default.Loop
                                                else Icons.Outlined.Loop,
                                                contentDescription =
                                                        if (videoBackgroundLoopInput)
                                                                stringResource(
                                                                        id = R.string.theme_loop_on
                                                                )
                                                        else
                                                                stringResource(
                                                                        id = R.string.theme_loop_off
                                                                ),
                                                tint =
                                                        if (videoBackgroundLoopInput)
                                                                MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(150.dp)
                                                .padding(bottom = 16.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outline,
                                                        RoundedCornerShape(8.dp)
                                                )
                                                .background(
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                            Text(
                                    text = stringResource(id = R.string.theme_no_bg_selected),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                            onClick = {
                                if (backgroundMediaTypeInput ==
                                                UserPreferencesManager.MEDIA_TYPE_VIDEO
                                ) {
                                    mediaPickerLauncher.launch("video/*")
                                } else {
                                    mediaPickerLauncher.launch("image/*")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                                if (backgroundMediaTypeInput ==
                                                UserPreferencesManager.MEDIA_TYPE_VIDEO
                                )
                                        stringResource(id = R.string.theme_select_video)
                                else stringResource(id = R.string.theme_select_image)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Opacity adjustment
                    Text(
                            text =
                                    stringResource(
                                            id = R.string.theme_bg_opacity,
                                            (backgroundImageOpacityInput * 100).toInt()
                                    ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Remember last saved value for debounce save operation
                    var lastSavedOpacity by remember { mutableStateOf(backgroundImageOpacityInput) }

                    // Use rememberUpdatedState to stabilize callback function
                    val currentScope = rememberCoroutineScope()

                    // Create a simpler disableScrollWhileDragging state
                    var isDragging by remember { mutableStateOf(false) }

                    // Custom interaction source, helps us monitor drag state
                    val interactionSource = remember { MutableInteractionSource() }

                    // Monitor drag state
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is DragInteraction.Start -> isDragging = true
                                is DragInteraction.Stop -> isDragging = false
                                is DragInteraction.Cancel -> isDragging = false
                            }
                        }
                    }

                    // If in drag state, temporarily lock scrolling
                    if (isDragging) {
                        DisposableEffect(Unit) {
                            val previousScrollValue = scrollState.value
                            onDispose {
                                // Nothing to do on dispose
                            }
                        }
                    }

                    // Create a fixed update callback and finish callback
                    val updateOpacity = remember {
                        { value: Float -> backgroundImageOpacityInput = value }
                    }

                    val onValueChangeFinished = remember {
                        {
                            if (kotlin.math.abs(lastSavedOpacity - backgroundImageOpacityInput) >
                                            0.01f
                            ) {
                                currentScope.launch {
                                    preferencesManager.saveThemeSettings(
                                            backgroundImageOpacity = backgroundImageOpacityInput
                                    )
                                    lastSavedOpacity = backgroundImageOpacityInput
                                    showSaveSuccessMessage = true
                                }
                            }
                        }
                    }

                    // Use Box to wrap slider, solve drag issue
                    Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 8.dp)) {
                        Slider(
                                value = backgroundImageOpacityInput,
                                onValueChange = updateOpacity,
                                onValueChangeFinished = onValueChangeFinished,
                                valueRange = 0.1f..1f,
                                interactionSource = interactionSource,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                        SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor =
                                                        MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                        )
                        )
                    }

                    // Add a gap, ensure slider below has enough space
                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Background blur settings
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                    text = stringResource(id = R.string.theme_background_blur),
                                    style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                    text = stringResource(id = R.string.theme_background_blur_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                                checked = useBackgroundBlurInput,
                                onCheckedChange = {
                                    useBackgroundBlurInput = it
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(useBackgroundBlur = it)
                                        showSaveSuccessMessage = true
                                    }
                                }
                        )
                    }

                    if (useBackgroundBlurInput) {
                        Text(
                                text =
                                        stringResource(id = R.string.theme_background_blur_radius) +
                                                ": ${backgroundBlurRadiusInput.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )

                        // Remember last saved value for debounce save operation
                        var lastSavedBlurRadius by remember { mutableStateOf(backgroundBlurRadiusInput) }
                        val blurInteractionSource = remember { MutableInteractionSource() }

                        val onBlurValueChangeFinished = remember {
                            {
                                if (kotlin.math.abs(lastSavedBlurRadius - backgroundBlurRadiusInput) >
                                                0.1f
                                ) {
                                    scope.launch {
                                        preferencesManager.saveThemeSettings(
                                                backgroundBlurRadius = backgroundBlurRadiusInput
                                        )
                                        lastSavedBlurRadius = backgroundBlurRadiusInput
                                        showSaveSuccessMessage = true
                                    }
                                }
                            }
                        }

                        // Use Box to wrap slider, solve drag issue
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth().height(56.dp).padding(vertical = 8.dp)
                        ) {
                            Slider(
                                    value = backgroundBlurRadiusInput,
                                    onValueChange = { backgroundBlurRadiusInput = it },
                                    onValueChangeFinished = onBlurValueChangeFinished,
                                    valueRange = 1f..30f,
                                    interactionSource = blurInteractionSource,
                                    modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Reset button
        OutlinedButton(
                onClick = {
                    scope.launch {
                        preferencesManager.resetThemeSettings()
                        // Reset local state after reset
                        themeModeInput = UserPreferencesManager.THEME_MODE_LIGHT
                        useSystemThemeInput = true
                        primaryColorInput = defaultPrimaryColor
                        secondaryColorInput = defaultSecondaryColor
                        useCustomColorsInput = false
                        useBackgroundImageInput = false
                        backgroundImageUriInput = null
                        backgroundImageOpacityInput = 0.3f
                        backgroundMediaTypeInput = UserPreferencesManager.MEDIA_TYPE_IMAGE
                        videoBackgroundMutedInput = true
                        videoBackgroundLoopInput = true
                        toolbarTransparentInput = false
                        useCustomStatusBarColorInput = false
                        customStatusBarColorInput = defaultPrimaryColor
                        statusBarTransparentInput = false
                        chatHeaderTransparentInput = false
                        chatInputTransparentInput = false
                        chatHeaderOverlayModeInput = false
                        forceAppBarContentColorInput = false
                        appBarContentColorModeInput = UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_LIGHT
                        chatHeaderHistoryIconColorInput = Color.Gray.toArgb()
                        chatHeaderPipIconColorInput = Color.Gray.toArgb()
                        useBackgroundBlurInput = false
                        backgroundBlurRadiusInput = 10f
                        chatStyleInput = UserPreferencesManager.CHAT_STYLE_CURSOR
                        showThinkingProcessInput = true
                        showStatusTagsInput = true
                        showInputProcessingStatusInput = true
                        userAvatarUriInput = null
                        aiAvatarUriInput = null
                        avatarShapeInput = UserPreferencesManager.AVATAR_SHAPE_CIRCLE
                        avatarCornerRadiusInput = 8f
                        onColorModeInput = UserPreferencesManager.ON_COLOR_MODE_AUTO
                        customChatTitleInput = ""
                        showSaveSuccessMessage = true
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) { Text(stringResource(id = R.string.theme_reset)) }

        // Show save success message
        if (showSaveSuccessMessage) {
            LaunchedEffect(key1 = showSaveSuccessMessage) {
                kotlinx.coroutines.delay(2000)
                showSaveSuccessMessage = false
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                        text = stringResource(id = R.string.theme_saved),
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Color picker dialog
        if (showColorPicker) {
            ColorPickerDialog(
                    showColorPicker = showColorPicker,
                    currentColorPickerMode = currentColorPickerMode,
                    primaryColorInput = primaryColorInput,
                    secondaryColorInput = secondaryColorInput,
                    statusBarColorInput = customStatusBarColorInput,
                    historyIconColorInput = chatHeaderHistoryIconColorInput,
                    pipIconColorInput = chatHeaderPipIconColorInput,
                    onColorSelected = {
                        primaryColor,
                        secondaryColor,
                        statusBarColor,
                        historyIconColor,
                        pipIconColor ->
                        primaryColor?.let { primaryColorInput = it }
                        secondaryColor?.let { secondaryColorInput = it }
                        statusBarColor?.let { customStatusBarColorInput = it }
                        historyIconColor?.let { chatHeaderHistoryIconColorInput = it }
                        pipIconColor?.let { chatHeaderPipIconColorInput = it }

                        // Save the colors
                        scope.launch {
                            when (currentColorPickerMode) {
                                "primary" ->
                                        primaryColor?.let {
                                            preferencesManager.saveThemeSettings(
                                                    customPrimaryColor = it
                                            )
                                        }
                                "secondary" ->
                                        secondaryColor?.let {
                                            preferencesManager.saveThemeSettings(
                                                    customSecondaryColor = it
                                            )
                                        }
                                "statusBar" ->
                                        statusBarColor?.let {
                                            preferencesManager.saveThemeSettings(
                                                    customStatusBarColor = it
                                            )
                                        }
                                "historyIcon" ->
                                        historyIconColor?.let {
                                            preferencesManager.saveThemeSettings(
                                                    chatHeaderHistoryIconColor = it
                                            )
                                        }
                                "pipIcon" ->
                                        pipIconColor?.let {
                                            preferencesManager.saveThemeSettings(
                                                    chatHeaderPipIconColor = it
                                            )
                                        }
                            }
                            showSaveSuccessMessage = true
                        }
                    },
                    onDismiss = { showColorPicker = false }
            )
        }

        // ======= SECTION: UI TEXT =======
        ThemeSectionTitle(
            title = stringResource(id = R.string.ui_text_title),
            icon = Icons.Default.TextFields
        )
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = cardModifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = customChatTitleInput,
                    onValueChange = { customChatTitleInput = it },
                    label = { Text(stringResource(id = R.string.custom_chat_title_label)) },
                    placeholder = { Text(stringResource(id = R.string.custom_chat_title_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                preferencesManager.saveThemeSettings(customChatTitle = customChatTitleInput)
                                showSaveSuccessMessage = true
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = stringResource(id = R.string.save_title_desc))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeSectionTitle(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
        )
    }
    Divider(modifier = Modifier.padding(bottom = 8.dp))
}
