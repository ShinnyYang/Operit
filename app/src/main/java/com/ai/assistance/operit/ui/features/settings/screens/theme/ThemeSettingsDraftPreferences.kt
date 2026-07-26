package com.ai.assistance.operit.ui.features.settings.screens.theme

import android.net.Uri
import com.ai.assistance.operit.data.preferences.ThemePreferenceSnapshot
import com.ai.assistance.operit.data.preferences.ThemePreferenceValues
import com.ai.assistance.operit.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Mirrors the theme preference API for one editor session without mutating the active projection.
 * The editor commits [values] through ActivePromptManager only after the user explicitly saves.
 */
internal class ThemeSettingsDraftPreferences(
    private val persistentPreferences: UserPreferencesManager,
    initialSnapshot: ThemePreferenceSnapshot,
) {
    private val _snapshot = MutableStateFlow(initialSnapshot)
    private val _hasUnsavedChanges = MutableStateFlow(false)
    private var baselineSnapshot = initialSnapshot
    private var resetRequested = false
    private val stagedAssetUris = mutableSetOf<String>()
    private var inFlightSavedValues: ThemePreferenceValues? = null
    private var disposed = false

    val snapshot: StateFlow<ThemePreferenceSnapshot> = _snapshot.asStateFlow()
    val hasUnsavedChangesFlow: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    val values: ThemePreferenceValues
        get() = _snapshot.value.values

    val hasUnsavedChanges: Boolean
        get() = resetRequested || _snapshot.value.values != baselineSnapshot.values

    val isResetRequested: Boolean
        get() = resetRequested

    private fun string(name: String): Flow<String> =
        snapshot.map { requireNotNull(it.values.string(name)) }.distinctUntilChanged()

    private fun optionalString(name: String): Flow<String?> =
        snapshot.map { it.values.string(name) }.distinctUntilChanged()

    private fun boolean(name: String): Flow<Boolean> =
        snapshot.map { requireNotNull(it.values.boolean(name)) }.distinctUntilChanged()

    private fun optionalInt(name: String): Flow<Int?> =
        snapshot.map { it.values.int(name) }.distinctUntilChanged()

    private fun float(name: String): Flow<Float> =
        snapshot.map { requireNotNull(it.values.float(name)) }.distinctUntilChanged()

    val themeMode: Flow<String> = string("theme_mode")
    val useSystemTheme: Flow<Boolean> = boolean("use_system_theme")
    val customPrimaryColor: Flow<Int?> = optionalInt("custom_primary_color")
    val customSecondaryColor: Flow<Int?> = optionalInt("custom_secondary_color")
    val useCustomColors: Flow<Boolean> = boolean("use_custom_colors")
    val useBackgroundImage: Flow<Boolean> = boolean("use_background_image")
    val backgroundImageUri: Flow<String?> = optionalString("background_image_uri")
    val backgroundImageOpacity: Flow<Float> = float("background_image_opacity")
    val backgroundMediaType: Flow<String> = string("background_media_type")
    val videoBackgroundMuted: Flow<Boolean> = boolean("video_background_muted")
    val videoBackgroundLoop: Flow<Boolean> = boolean("video_background_loop")
    val toolbarTransparent: Flow<Boolean> = boolean("toolbar_transparent")
    val navigationDrawerWaterGlass: Flow<Boolean> = boolean("navigation_drawer_water_glass")
    val navigationDrawerButtonLiquidGlass: Flow<Boolean> =
        boolean("navigation_drawer_button_liquid_glass")
    val useCustomNavigationDrawerBackgroundColor: Flow<Boolean> =
        boolean("use_custom_navigation_drawer_background_color")
    val customNavigationDrawerBackgroundColor: Flow<Int?> =
        optionalInt("custom_navigation_drawer_background_color")
    val useCustomNavigationDrawerAccentColor: Flow<Boolean> =
        boolean("use_custom_navigation_drawer_accent_color")
    val customNavigationDrawerAccentColor: Flow<Int?> =
        optionalInt("custom_navigation_drawer_accent_color")
    val useCustomAppBarColor: Flow<Boolean> = boolean("use_custom_app_bar_color")
    val customAppBarColor: Flow<Int?> = optionalInt("custom_app_bar_color")
    val useCustomStatusBarColor: Flow<Boolean> = boolean("use_custom_status_bar_color")
    val customStatusBarColor: Flow<Int?> = optionalInt("custom_status_bar_color")
    val statusBarTransparent: Flow<Boolean> = boolean("status_bar_transparent")
    val statusBarHidden: Flow<Boolean> = boolean("status_bar_hidden")
    val chatHeaderTransparent: Flow<Boolean> = boolean("chat_header_transparent")
    val chatInputTransparent: Flow<Boolean> = boolean("chat_input_transparent")
    val chatInputFloating: Flow<Boolean> = boolean("chat_input_floating")
    val chatInputLiquidGlass: Flow<Boolean> = boolean("chat_input_liquid_glass")
    val chatInputWaterGlass: Flow<Boolean> = boolean("chat_input_water_glass")
    val forceAppBarContentColor: Flow<Boolean> = boolean("force_app_bar_content_color_enabled")
    val appBarContentColorMode: Flow<String> = string("app_bar_content_color_mode")
    val chatHeaderHistoryIconColor: Flow<Int?> = optionalInt("chat_header_history_icon_color")
    val chatHeaderPipIconColor: Flow<Int?> = optionalInt("chat_header_pip_icon_color")
    val chatHeaderOverlayMode: Flow<Boolean> = boolean("chat_header_overlay_mode")
    val useBackgroundBlur: Flow<Boolean> = boolean("use_background_blur")
    val backgroundBlurRadius: Flow<Float> = float("background_blur_radius")
    val chatStyle: Flow<String> = string("chat_style")
    val inputStyle: Flow<String> = string("input_style")
    val bubbleShowAvatar: Flow<Boolean> = boolean("bubble_show_avatar")
    val bubbleWideLayoutEnabled: Flow<Boolean> = boolean("bubble_wide_layout_enabled")
    val cursorUserBubbleFollowTheme: Flow<Boolean> = boolean("cursor_user_bubble_follow_theme")
    val cursorUserBubbleLiquidGlass: Flow<Boolean> = boolean("cursor_user_bubble_liquid_glass")
    val cursorUserBubbleWaterGlass: Flow<Boolean> = boolean("cursor_user_bubble_water_glass")
    val cursorUserBubbleColor: Flow<Int?> = optionalInt("cursor_user_bubble_color")
    val bubbleUserBubbleLiquidGlass: Flow<Boolean> = boolean("bubble_user_bubble_liquid_glass")
    val bubbleUserBubbleWaterGlass: Flow<Boolean> = boolean("bubble_user_bubble_water_glass")
    val bubbleUserBubbleColor: Flow<Int?> = optionalInt("bubble_user_bubble_color")
    val bubbleAiBubbleLiquidGlass: Flow<Boolean> = boolean("bubble_ai_bubble_liquid_glass")
    val bubbleAiBubbleWaterGlass: Flow<Boolean> = boolean("bubble_ai_bubble_water_glass")
    val bubbleAiBubbleColor: Flow<Int?> = optionalInt("bubble_ai_bubble_color")
    val bubbleUserTextColor: Flow<Int?> = optionalInt("bubble_user_text_color")
    val bubbleAiTextColor: Flow<Int?> = optionalInt("bubble_ai_text_color")
    val bubbleUserUseCustomFont: Flow<Boolean> = boolean("bubble_user_use_custom_font")
    val bubbleUserFontType: Flow<String> = string("bubble_user_font_type")
    val bubbleUserSystemFontName: Flow<String> = string("bubble_user_system_font_name")
    val bubbleUserCustomFontPath: Flow<String?> = optionalString("bubble_user_custom_font_path")
    val bubbleAiUseCustomFont: Flow<Boolean> = boolean("bubble_ai_use_custom_font")
    val bubbleAiFontType: Flow<String> = string("bubble_ai_font_type")
    val bubbleAiSystemFontName: Flow<String> = string("bubble_ai_system_font_name")
    val bubbleAiCustomFontPath: Flow<String?> = optionalString("bubble_ai_custom_font_path")
    val bubbleUserUseImage: Flow<Boolean> = boolean("bubble_user_use_image")
    val bubbleAiUseImage: Flow<Boolean> = boolean("bubble_ai_use_image")
    val bubbleUserImageUri: Flow<String?> = optionalString("bubble_user_image_uri")
    val bubbleAiImageUri: Flow<String?> = optionalString("bubble_ai_image_uri")
    val bubbleUserImageCropLeft: Flow<Float> = float("bubble_user_image_crop_left")
    val bubbleUserImageCropTop: Flow<Float> = float("bubble_user_image_crop_top")
    val bubbleUserImageCropRight: Flow<Float> = float("bubble_user_image_crop_right")
    val bubbleUserImageCropBottom: Flow<Float> = float("bubble_user_image_crop_bottom")
    val bubbleUserImageRepeatStart: Flow<Float> = float("bubble_user_image_repeat_start")
    val bubbleUserImageRepeatEnd: Flow<Float> = float("bubble_user_image_repeat_end")
    val bubbleUserImageRepeatYStart: Flow<Float> = float("bubble_user_image_repeat_y_start")
    val bubbleUserImageRepeatYEnd: Flow<Float> = float("bubble_user_image_repeat_y_end")
    val bubbleUserImageScale: Flow<Float> = float("bubble_user_image_scale")
    val bubbleAiImageCropLeft: Flow<Float> = float("bubble_ai_image_crop_left")
    val bubbleAiImageCropTop: Flow<Float> = float("bubble_ai_image_crop_top")
    val bubbleAiImageCropRight: Flow<Float> = float("bubble_ai_image_crop_right")
    val bubbleAiImageCropBottom: Flow<Float> = float("bubble_ai_image_crop_bottom")
    val bubbleAiImageRepeatStart: Flow<Float> = float("bubble_ai_image_repeat_start")
    val bubbleAiImageRepeatEnd: Flow<Float> = float("bubble_ai_image_repeat_end")
    val bubbleAiImageRepeatYStart: Flow<Float> = float("bubble_ai_image_repeat_y_start")
    val bubbleAiImageRepeatYEnd: Flow<Float> = float("bubble_ai_image_repeat_y_end")
    val bubbleAiImageScale: Flow<Float> = float("bubble_ai_image_scale")
    val bubbleImageRenderMode: Flow<String> = string("bubble_image_render_mode")
    val bubbleUserRoundedCornersEnabled: Flow<Boolean> =
        boolean("bubble_rounded_corners_enabled")
    val bubbleAiRoundedCornersEnabled: Flow<Boolean> =
        boolean("bubble_ai_rounded_corners_enabled")
    val bubbleUserContentPaddingLeft: Flow<Float> = float("bubble_content_padding_left")
    val bubbleUserContentPaddingRight: Flow<Float> = float("bubble_content_padding_right")
    val bubbleAiContentPaddingLeft: Flow<Float> = float("bubble_ai_content_padding_left")
    val bubbleAiContentPaddingRight: Flow<Float> = float("bubble_ai_content_padding_right")
    val showThinkingProcess: Flow<Boolean> = boolean("show_thinking_process")
    val showStatusTags: Flow<Boolean> = boolean("show_status_tags")
    val showModelProvider: Flow<Boolean> = boolean("show_model_provider")
    val showModelName: Flow<Boolean> = boolean("show_model_name")
    val showRoleName: Flow<Boolean> = boolean("show_role_name")
    val showUserName: Flow<Boolean> = boolean("show_user_name")
    val showMessageTokenStats: Flow<Boolean> = boolean("show_message_token_stats")
    val showMessageTimingStats: Flow<Boolean> = boolean("show_message_timing_stats")
    val showMessageTimestamp: Flow<Boolean> = boolean("show_message_timestamp")
    val customUserAvatarUri: Flow<String?> = optionalString("custom_user_avatar_uri")
    val customAiAvatarUri: Flow<String?> = optionalString("custom_ai_avatar_uri")
    val avatarShape: Flow<String> = string("avatar_shape")
    val avatarCornerRadius: Flow<Float> = float("avatar_corner_radius")
    val onColorMode: Flow<String> = string("on_color_mode")
    val customChatTitle: Flow<String?> = optionalString("custom_chat_title")
    val showInputProcessingStatus: Flow<Boolean> = boolean("show_input_processing_status")
    val showChatFloatingDotsAnimation: Flow<Boolean> = boolean("show_chat_floating_dots_animation")
    val useCustomFont: Flow<Boolean> = boolean("use_custom_font")
    val fontType: Flow<String> = string("font_type")
    val systemFontName: Flow<String> = string("system_font_name")
    val customFontPath: Flow<String?> = optionalString("custom_font_path")
    val fontScale: Flow<Float> = float("font_scale")
    val recentColorsFlow: Flow<List<Int>> = persistentPreferences.recentColorsFlow

    fun reset() {
        val previous = _snapshot.value
        val resetValues = ThemePreferenceValues.defaultVisual()
            .withString("custom_ai_avatar_uri", previous.values.string("custom_ai_avatar_uri"))
            .withString("custom_chat_title", previous.values.string("custom_chat_title"))
        _snapshot.value = previous.copy(values = resetValues)
        resetRequested = true
        deleteUnreferencedStagedAssets(resetValues)
        updateDirtyState()
    }

    fun discard() {
        deleteStagedAssets(stagedAssetUris.toSet())
        _snapshot.value = baselineSnapshot
        resetRequested = false
        updateDirtyState()
    }

    fun beginSave(savedValues: ThemePreferenceValues) {
        inFlightSavedValues = savedValues
    }

    fun markSaved(savedValues: ThemePreferenceValues) {
        stagedAssetUris.removeAll(savedValues.strings.values)
        inFlightSavedValues = null
        baselineSnapshot = _snapshot.value.copy(values = savedValues)
        resetRequested = false
        if (disposed) {
            deleteStagedAssets(stagedAssetUris.toSet())
        } else {
            deleteUnreferencedStagedAssets(_snapshot.value.values)
        }
        updateDirtyState()
    }

    fun cancelSave() {
        inFlightSavedValues = null
        if (disposed) {
            deleteStagedAssets(stagedAssetUris.toSet())
        } else {
            deleteUnreferencedStagedAssets(_snapshot.value.values)
        }
    }

    fun dispose() {
        disposed = true
        if (inFlightSavedValues == null) {
            deleteStagedAssets(stagedAssetUris.toSet())
        }
    }

    fun registerStagedAsset(uri: String) {
        if (disposed) {
            deleteStagedAssets(setOf(uri))
            return
        }
        stagedAssetUris += uri
    }

    suspend fun addRecentColor(color: Int) {
        persistentPreferences.addRecentColor(color)
    }

    suspend fun saveThemeSettings(
        themeMode: String? = null,
        useSystemTheme: Boolean? = null,
        customPrimaryColor: Int? = null,
        customSecondaryColor: Int? = null,
        useCustomColors: Boolean? = null,
        useBackgroundImage: Boolean? = null,
        backgroundImageUri: String? = null,
        backgroundImageOpacity: Float? = null,
        backgroundMediaType: String? = null,
        videoBackgroundMuted: Boolean? = null,
        videoBackgroundLoop: Boolean? = null,
        toolbarTransparent: Boolean? = null,
        navigationDrawerWaterGlass: Boolean? = null,
        navigationDrawerButtonLiquidGlass: Boolean? = null,
        useCustomNavigationDrawerBackgroundColor: Boolean? = null,
        customNavigationDrawerBackgroundColor: Int? = null,
        useCustomNavigationDrawerAccentColor: Boolean? = null,
        customNavigationDrawerAccentColor: Int? = null,
        useCustomAppBarColor: Boolean? = null,
        customAppBarColor: Int? = null,
        useCustomStatusBarColor: Boolean? = null,
        customStatusBarColor: Int? = null,
        statusBarTransparent: Boolean? = null,
        statusBarHidden: Boolean? = null,
        chatHeaderTransparent: Boolean? = null,
        chatInputTransparent: Boolean? = null,
        chatInputFloating: Boolean? = null,
        chatInputLiquidGlass: Boolean? = null,
        chatInputWaterGlass: Boolean? = null,
        forceAppBarContentColor: Boolean? = null,
        appBarContentColorMode: String? = null,
        chatHeaderHistoryIconColor: Int? = null,
        chatHeaderPipIconColor: Int? = null,
        chatHeaderOverlayMode: Boolean? = null,
        useBackgroundBlur: Boolean? = null,
        backgroundBlurRadius: Float? = null,
        chatStyle: String? = null,
        bubbleShowAvatar: Boolean? = null,
        bubbleWideLayoutEnabled: Boolean? = null,
        cursorUserBubbleFollowTheme: Boolean? = null,
        cursorUserBubbleLiquidGlass: Boolean? = null,
        cursorUserBubbleWaterGlass: Boolean? = null,
        cursorUserBubbleColor: Int? = null,
        bubbleUserBubbleLiquidGlass: Boolean? = null,
        bubbleUserBubbleWaterGlass: Boolean? = null,
        bubbleUserBubbleColor: Int? = null,
        bubbleAiBubbleLiquidGlass: Boolean? = null,
        bubbleAiBubbleWaterGlass: Boolean? = null,
        bubbleAiBubbleColor: Int? = null,
        bubbleUserTextColor: Int? = null,
        bubbleAiTextColor: Int? = null,
        bubbleUserUseCustomFont: Boolean? = null,
        bubbleUserFontType: String? = null,
        bubbleUserSystemFontName: String? = null,
        bubbleUserCustomFontPath: String? = null,
        bubbleAiUseCustomFont: Boolean? = null,
        bubbleAiFontType: String? = null,
        bubbleAiSystemFontName: String? = null,
        bubbleAiCustomFontPath: String? = null,
        bubbleUserUseImage: Boolean? = null,
        bubbleAiUseImage: Boolean? = null,
        bubbleUserImageUri: String? = null,
        bubbleAiImageUri: String? = null,
        bubbleUserImageCropLeft: Float? = null,
        bubbleUserImageCropTop: Float? = null,
        bubbleUserImageCropRight: Float? = null,
        bubbleUserImageCropBottom: Float? = null,
        bubbleUserImageRepeatStart: Float? = null,
        bubbleUserImageRepeatEnd: Float? = null,
        bubbleUserImageRepeatYStart: Float? = null,
        bubbleUserImageRepeatYEnd: Float? = null,
        bubbleUserImageScale: Float? = null,
        bubbleAiImageCropLeft: Float? = null,
        bubbleAiImageCropTop: Float? = null,
        bubbleAiImageCropRight: Float? = null,
        bubbleAiImageCropBottom: Float? = null,
        bubbleAiImageRepeatStart: Float? = null,
        bubbleAiImageRepeatEnd: Float? = null,
        bubbleAiImageRepeatYStart: Float? = null,
        bubbleAiImageRepeatYEnd: Float? = null,
        bubbleAiImageScale: Float? = null,
        bubbleImageRenderMode: String? = null,
        bubbleUserRoundedCornersEnabled: Boolean? = null,
        bubbleAiRoundedCornersEnabled: Boolean? = null,
        bubbleUserContentPaddingLeft: Float? = null,
        bubbleUserContentPaddingRight: Float? = null,
        bubbleAiContentPaddingLeft: Float? = null,
        bubbleAiContentPaddingRight: Float? = null,
        showThinkingProcess: Boolean? = null,
        showStatusTags: Boolean? = null,
        showModelProvider: Boolean? = null,
        showModelName: Boolean? = null,
        showRoleName: Boolean? = null,
        showUserName: Boolean? = null,
        showMessageTokenStats: Boolean? = null,
        showMessageTimingStats: Boolean? = null,
        showMessageTimestamp: Boolean? = null,
        customUserAvatarUri: String? = null,
        customAiAvatarUri: String? = null,
        avatarShape: String? = null,
        avatarCornerRadius: Float? = null,
        onColorMode: String? = null,
        customChatTitle: String? = null,
        showInputProcessingStatus: Boolean? = null,
        showChatFloatingDotsAnimation: Boolean? = null,
        inputStyle: String? = null,
        useCustomFont: Boolean? = null,
        fontType: String? = null,
        systemFontName: String? = null,
        customFontPath: String? = null,
        fontScale: Float? = null,
    ) {
        if (disposed) return

        fun normalizedPath(value: String?): String? = value?.takeIf { it.isNotBlank() }

        var updated = _snapshot.value.values
        themeMode?.let { updated = updated.withString("theme_mode", it) }
        useSystemTheme?.let { updated = updated.withBoolean("use_system_theme", it) }
        customPrimaryColor?.let { updated = updated.withInt("custom_primary_color", it) }
        customSecondaryColor?.let { updated = updated.withInt("custom_secondary_color", it) }
        useCustomColors?.let { updated = updated.withBoolean("use_custom_colors", it) }
        useBackgroundImage?.let { updated = updated.withBoolean("use_background_image", it) }
        backgroundImageUri?.let { updated = updated.withString("background_image_uri", normalizedPath(it)) }
        backgroundImageOpacity?.let { updated = updated.withFloat("background_image_opacity", it) }
        backgroundMediaType?.let { updated = updated.withString("background_media_type", it) }
        videoBackgroundMuted?.let { updated = updated.withBoolean("video_background_muted", it) }
        videoBackgroundLoop?.let { updated = updated.withBoolean("video_background_loop", it) }
        toolbarTransparent?.let { updated = updated.withBoolean("toolbar_transparent", it) }
        navigationDrawerWaterGlass?.let {
            updated = updated.withBoolean("navigation_drawer_water_glass", it)
        }
        navigationDrawerButtonLiquidGlass?.let {
            updated = updated.withBoolean("navigation_drawer_button_liquid_glass", it)
        }
        useCustomNavigationDrawerBackgroundColor?.let {
            updated = updated.withBoolean("use_custom_navigation_drawer_background_color", it)
        }
        customNavigationDrawerBackgroundColor?.let {
            updated = updated.withInt("custom_navigation_drawer_background_color", it)
        }
        useCustomNavigationDrawerAccentColor?.let {
            updated = updated.withBoolean("use_custom_navigation_drawer_accent_color", it)
        }
        customNavigationDrawerAccentColor?.let {
            updated = updated.withInt("custom_navigation_drawer_accent_color", it)
        }
        useCustomAppBarColor?.let { updated = updated.withBoolean("use_custom_app_bar_color", it) }
        customAppBarColor?.let { updated = updated.withInt("custom_app_bar_color", it) }
        useCustomStatusBarColor?.let {
            updated = updated.withBoolean("use_custom_status_bar_color", it)
        }
        customStatusBarColor?.let { updated = updated.withInt("custom_status_bar_color", it) }
        statusBarTransparent?.let { updated = updated.withBoolean("status_bar_transparent", it) }
        statusBarHidden?.let { updated = updated.withBoolean("status_bar_hidden", it) }
        chatHeaderTransparent?.let { updated = updated.withBoolean("chat_header_transparent", it) }
        chatInputTransparent?.let { updated = updated.withBoolean("chat_input_transparent", it) }
        chatInputFloating?.let { updated = updated.withBoolean("chat_input_floating", it) }
        chatInputLiquidGlass?.let { updated = updated.withBoolean("chat_input_liquid_glass", it) }
        chatInputWaterGlass?.let { updated = updated.withBoolean("chat_input_water_glass", it) }
        forceAppBarContentColor?.let {
            updated = updated.withBoolean("force_app_bar_content_color_enabled", it)
        }
        appBarContentColorMode?.let { updated = updated.withString("app_bar_content_color_mode", it) }
        chatHeaderHistoryIconColor?.let {
            updated = updated.withInt("chat_header_history_icon_color", it)
        }
        chatHeaderPipIconColor?.let { updated = updated.withInt("chat_header_pip_icon_color", it) }
        chatHeaderOverlayMode?.let { updated = updated.withBoolean("chat_header_overlay_mode", it) }
        useBackgroundBlur?.let { updated = updated.withBoolean("use_background_blur", it) }
        backgroundBlurRadius?.let { updated = updated.withFloat("background_blur_radius", it) }
        chatStyle?.let { updated = updated.withString("chat_style", it) }
        bubbleShowAvatar?.let { updated = updated.withBoolean("bubble_show_avatar", it) }
        bubbleWideLayoutEnabled?.let { updated = updated.withBoolean("bubble_wide_layout_enabled", it) }
        cursorUserBubbleFollowTheme?.let {
            updated = updated.withBoolean("cursor_user_bubble_follow_theme", it)
        }
        cursorUserBubbleLiquidGlass?.let {
            updated = updated.withBoolean("cursor_user_bubble_liquid_glass", it)
        }
        cursorUserBubbleWaterGlass?.let {
            updated = updated.withBoolean("cursor_user_bubble_water_glass", it)
        }
        cursorUserBubbleColor?.let { updated = updated.withInt("cursor_user_bubble_color", it) }
        bubbleUserBubbleLiquidGlass?.let {
            updated = updated.withBoolean("bubble_user_bubble_liquid_glass", it)
        }
        bubbleUserBubbleWaterGlass?.let {
            updated = updated.withBoolean("bubble_user_bubble_water_glass", it)
        }
        bubbleUserBubbleColor?.let { updated = updated.withInt("bubble_user_bubble_color", it) }
        bubbleAiBubbleLiquidGlass?.let {
            updated = updated.withBoolean("bubble_ai_bubble_liquid_glass", it)
        }
        bubbleAiBubbleWaterGlass?.let {
            updated = updated.withBoolean("bubble_ai_bubble_water_glass", it)
        }
        bubbleAiBubbleColor?.let { updated = updated.withInt("bubble_ai_bubble_color", it) }
        bubbleUserTextColor?.let { updated = updated.withInt("bubble_user_text_color", it) }
        bubbleAiTextColor?.let { updated = updated.withInt("bubble_ai_text_color", it) }
        bubbleUserUseCustomFont?.let {
            updated = updated.withBoolean("bubble_user_use_custom_font", it)
        }
        bubbleUserFontType?.let { updated = updated.withString("bubble_user_font_type", it) }
        bubbleUserSystemFontName?.let {
            updated = updated.withString("bubble_user_system_font_name", it)
        }
        bubbleUserCustomFontPath?.let {
            updated = updated.withString("bubble_user_custom_font_path", normalizedPath(it))
        }
        bubbleAiUseCustomFont?.let {
            updated = updated.withBoolean("bubble_ai_use_custom_font", it)
        }
        bubbleAiFontType?.let { updated = updated.withString("bubble_ai_font_type", it) }
        bubbleAiSystemFontName?.let {
            updated = updated.withString("bubble_ai_system_font_name", it)
        }
        bubbleAiCustomFontPath?.let {
            updated = updated.withString("bubble_ai_custom_font_path", normalizedPath(it))
        }
        bubbleUserUseImage?.let { updated = updated.withBoolean("bubble_user_use_image", it) }
        bubbleAiUseImage?.let { updated = updated.withBoolean("bubble_ai_use_image", it) }
        bubbleUserImageUri?.let {
            updated = updated.withString("bubble_user_image_uri", normalizedPath(it))
        }
        bubbleAiImageUri?.let { updated = updated.withString("bubble_ai_image_uri", normalizedPath(it)) }
        bubbleUserImageCropLeft?.let { updated = updated.withFloat("bubble_user_image_crop_left", it) }
        bubbleUserImageCropTop?.let { updated = updated.withFloat("bubble_user_image_crop_top", it) }
        bubbleUserImageCropRight?.let { updated = updated.withFloat("bubble_user_image_crop_right", it) }
        bubbleUserImageCropBottom?.let {
            updated = updated.withFloat("bubble_user_image_crop_bottom", it)
        }
        bubbleUserImageRepeatStart?.let {
            updated = updated.withFloat("bubble_user_image_repeat_start", it)
        }
        bubbleUserImageRepeatEnd?.let { updated = updated.withFloat("bubble_user_image_repeat_end", it) }
        bubbleUserImageRepeatYStart?.let {
            updated = updated.withFloat("bubble_user_image_repeat_y_start", it)
        }
        bubbleUserImageRepeatYEnd?.let {
            updated = updated.withFloat("bubble_user_image_repeat_y_end", it)
        }
        bubbleUserImageScale?.let { updated = updated.withFloat("bubble_user_image_scale", it) }
        bubbleAiImageCropLeft?.let { updated = updated.withFloat("bubble_ai_image_crop_left", it) }
        bubbleAiImageCropTop?.let { updated = updated.withFloat("bubble_ai_image_crop_top", it) }
        bubbleAiImageCropRight?.let { updated = updated.withFloat("bubble_ai_image_crop_right", it) }
        bubbleAiImageCropBottom?.let { updated = updated.withFloat("bubble_ai_image_crop_bottom", it) }
        bubbleAiImageRepeatStart?.let {
            updated = updated.withFloat("bubble_ai_image_repeat_start", it)
        }
        bubbleAiImageRepeatEnd?.let { updated = updated.withFloat("bubble_ai_image_repeat_end", it) }
        bubbleAiImageRepeatYStart?.let {
            updated = updated.withFloat("bubble_ai_image_repeat_y_start", it)
        }
        bubbleAiImageRepeatYEnd?.let {
            updated = updated.withFloat("bubble_ai_image_repeat_y_end", it)
        }
        bubbleAiImageScale?.let { updated = updated.withFloat("bubble_ai_image_scale", it) }
        bubbleImageRenderMode?.let { updated = updated.withString("bubble_image_render_mode", it) }
        bubbleUserRoundedCornersEnabled?.let {
            updated = updated.withBoolean("bubble_rounded_corners_enabled", it)
        }
        bubbleAiRoundedCornersEnabled?.let {
            updated = updated.withBoolean("bubble_ai_rounded_corners_enabled", it)
        }
        bubbleUserContentPaddingLeft?.let {
            updated = updated.withFloat("bubble_content_padding_left", it)
        }
        bubbleUserContentPaddingRight?.let {
            updated = updated.withFloat("bubble_content_padding_right", it)
        }
        bubbleAiContentPaddingLeft?.let {
            updated = updated.withFloat("bubble_ai_content_padding_left", it)
        }
        bubbleAiContentPaddingRight?.let {
            updated = updated.withFloat("bubble_ai_content_padding_right", it)
        }
        showThinkingProcess?.let { updated = updated.withBoolean("show_thinking_process", it) }
        showStatusTags?.let { updated = updated.withBoolean("show_status_tags", it) }
        showModelProvider?.let { updated = updated.withBoolean("show_model_provider", it) }
        showModelName?.let { updated = updated.withBoolean("show_model_name", it) }
        showRoleName?.let { updated = updated.withBoolean("show_role_name", it) }
        showUserName?.let { updated = updated.withBoolean("show_user_name", it) }
        showMessageTokenStats?.let {
            updated = updated.withBoolean("show_message_token_stats", it)
        }
        showMessageTimingStats?.let {
            updated = updated.withBoolean("show_message_timing_stats", it)
        }
        showMessageTimestamp?.let {
            updated = updated.withBoolean("show_message_timestamp", it)
        }
        customUserAvatarUri?.let {
            updated = updated.withString("custom_user_avatar_uri", normalizedPath(it))
        }
        customAiAvatarUri?.let {
            updated = updated.withString("custom_ai_avatar_uri", normalizedPath(it))
        }
        avatarShape?.let { updated = updated.withString("avatar_shape", it) }
        avatarCornerRadius?.let { updated = updated.withFloat("avatar_corner_radius", it) }
        onColorMode?.let { updated = updated.withString("on_color_mode", it) }
        customChatTitle?.let {
            updated = updated.withString("custom_chat_title", normalizedPath(it))
        }
        showInputProcessingStatus?.let {
            updated = updated.withBoolean("show_input_processing_status", it)
        }
        showChatFloatingDotsAnimation?.let {
            updated = updated.withBoolean("show_chat_floating_dots_animation", it)
        }
        inputStyle?.let { updated = updated.withString("input_style", it) }
        useCustomFont?.let { updated = updated.withBoolean("use_custom_font", it) }
        fontType?.let { updated = updated.withString("font_type", it) }
        systemFontName?.let { updated = updated.withString("system_font_name", it) }
        customFontPath?.let { updated = updated.withString("custom_font_path", normalizedPath(it)) }
        fontScale?.let { updated = updated.withFloat("font_scale", it) }

        if (chatInputLiquidGlass == true) {
            updated = updated.withBoolean("chat_input_water_glass", false)
        }
        if (chatInputWaterGlass == true) {
            updated = updated.withBoolean("chat_input_liquid_glass", false)
        }
        if (cursorUserBubbleLiquidGlass == true) {
            updated = updated.withBoolean("cursor_user_bubble_water_glass", false)
        }
        if (cursorUserBubbleWaterGlass == true) {
            updated = updated.withBoolean("cursor_user_bubble_liquid_glass", false)
        }
        if (bubbleUserBubbleLiquidGlass == true || bubbleUserBubbleWaterGlass == true) {
            updated = updated.withBoolean("bubble_user_use_image", false)
        }
        if (bubbleUserBubbleLiquidGlass == true) {
            updated = updated.withBoolean("bubble_user_bubble_water_glass", false)
        }
        if (bubbleUserBubbleWaterGlass == true) {
            updated = updated.withBoolean("bubble_user_bubble_liquid_glass", false)
        }
        if (bubbleAiBubbleLiquidGlass == true || bubbleAiBubbleWaterGlass == true) {
            updated = updated.withBoolean("bubble_ai_use_image", false)
        }
        if (bubbleAiBubbleLiquidGlass == true) {
            updated = updated.withBoolean("bubble_ai_bubble_water_glass", false)
        }
        if (bubbleAiBubbleWaterGlass == true) {
            updated = updated.withBoolean("bubble_ai_bubble_liquid_glass", false)
        }

        _snapshot.value = _snapshot.value.copy(values = updated)
        resetRequested = false
        deleteUnreferencedStagedAssets(updated)
        updateDirtyState()
    }

    private fun deleteUnreferencedStagedAssets(values: ThemePreferenceValues) {
        val referencedUris = buildSet {
            addAll(values.strings.values)
            inFlightSavedValues?.strings?.values?.let(::addAll)
        }
        deleteStagedAssets(stagedAssetUris.filterNot(referencedUris::contains).toSet())
    }

    private fun deleteStagedAssets(uris: Set<String>) {
        uris.forEach { uriString ->
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                uri.path?.let { path -> File(path).delete() }
            }
            stagedAssetUris.remove(uriString)
        }
    }

    private fun updateDirtyState() {
        _hasUnsavedChanges.value = hasUnsavedChanges
    }
}
