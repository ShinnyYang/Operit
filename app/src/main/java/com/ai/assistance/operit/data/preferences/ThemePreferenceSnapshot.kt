package com.ai.assistance.operit.data.preferences

data class ThemePreferenceValues(
    val strings: Map<String, String> = emptyMap(),
    val booleans: Map<String, Boolean> = emptyMap(),
    val ints: Map<String, Int> = emptyMap(),
    val floats: Map<String, Float> = emptyMap(),
) {
    fun string(name: String): String? = strings[name]

    fun boolean(name: String): Boolean? = booleans[name]

    fun int(name: String): Int? = ints[name]

    fun float(name: String): Float? = floats[name]

    fun withString(name: String, value: String?): ThemePreferenceValues {
        val updated = strings.toMutableMap()
        if (value == null) {
            updated.remove(name)
        } else {
            updated[name] = value
        }
        return copy(strings = updated)
    }

    fun withBoolean(name: String, value: Boolean): ThemePreferenceValues =
        copy(booleans = booleans + (name to value))

    fun withInt(name: String, value: Int?): ThemePreferenceValues {
        val updated = ints.toMutableMap()
        if (value == null) {
            updated.remove(name)
        } else {
            updated[name] = value
        }
        return copy(ints = updated)
    }

    fun withFloat(name: String, value: Float): ThemePreferenceValues =
        copy(floats = floats + (name to value))

    companion object {
        fun defaultVisual(): ThemePreferenceValues =
            ThemePreferenceValues(
                strings = mapOf(
                    "theme_mode" to UserPreferencesManager.THEME_MODE_LIGHT,
                    "background_media_type" to UserPreferencesManager.MEDIA_TYPE_IMAGE,
                    "app_bar_content_color_mode" to
                        UserPreferencesManager.APP_BAR_CONTENT_COLOR_MODE_LIGHT,
                    "chat_style" to UserPreferencesManager.CHAT_STYLE_CURSOR,
                    "avatar_shape" to UserPreferencesManager.AVATAR_SHAPE_CIRCLE,
                    "on_color_mode" to UserPreferencesManager.ON_COLOR_MODE_AUTO,
                    "input_style" to UserPreferencesManager.INPUT_STYLE_AGENT,
                    "font_type" to UserPreferencesManager.FONT_TYPE_SYSTEM,
                    "system_font_name" to UserPreferencesManager.SYSTEM_FONT_DEFAULT,
                    "bubble_user_font_type" to UserPreferencesManager.FONT_TYPE_SYSTEM,
                    "bubble_user_system_font_name" to UserPreferencesManager.SYSTEM_FONT_DEFAULT,
                    "bubble_ai_font_type" to UserPreferencesManager.FONT_TYPE_SYSTEM,
                    "bubble_ai_system_font_name" to UserPreferencesManager.SYSTEM_FONT_DEFAULT,
                    "bubble_image_render_mode" to
                        UserPreferencesManager.BUBBLE_IMAGE_RENDER_MODE_TILED_NINE_SLICE,
                ),
                booleans = mapOf(
                    "use_system_theme" to true,
                    "use_custom_colors" to false,
                    "use_background_image" to false,
                    "video_background_muted" to true,
                    "video_background_loop" to true,
                    "toolbar_transparent" to false,
                    "navigation_drawer_water_glass" to false,
                    "navigation_drawer_button_liquid_glass" to false,
                    "use_custom_navigation_drawer_background_color" to false,
                    "use_custom_navigation_drawer_accent_color" to false,
                    "use_custom_app_bar_color" to false,
                    "use_custom_status_bar_color" to false,
                    "status_bar_transparent" to false,
                    "status_bar_hidden" to false,
                    "chat_header_transparent" to false,
                    "chat_input_transparent" to false,
                    "chat_input_floating" to false,
                    "chat_input_liquid_glass" to false,
                    "chat_input_water_glass" to false,
                    "force_app_bar_content_color_enabled" to false,
                    "chat_header_overlay_mode" to false,
                    "use_background_blur" to false,
                    "bubble_show_avatar" to true,
                    "bubble_wide_layout_enabled" to false,
                    "cursor_user_bubble_follow_theme" to true,
                    "cursor_user_bubble_liquid_glass" to false,
                    "cursor_user_bubble_water_glass" to false,
                    "bubble_user_bubble_liquid_glass" to false,
                    "bubble_user_bubble_water_glass" to false,
                    "bubble_ai_bubble_liquid_glass" to false,
                    "bubble_ai_bubble_water_glass" to false,
                    "bubble_user_use_image" to false,
                    "bubble_ai_use_image" to false,
                    "bubble_rounded_corners_enabled" to true,
                    "bubble_ai_rounded_corners_enabled" to true,
                    "show_thinking_process" to true,
                    "show_status_tags" to true,
                    "show_model_provider" to false,
                    "show_model_name" to false,
                    "show_role_name" to true,
                    "show_user_name" to true,
                    "show_message_token_stats" to false,
                    "show_message_timing_stats" to false,
                    "show_message_timestamp" to false,
                    "show_input_processing_status" to true,
                    "show_chat_floating_dots_animation" to true,
                    "use_custom_font" to false,
                    "bubble_user_use_custom_font" to false,
                    "bubble_ai_use_custom_font" to false,
                ),
                floats = mapOf(
                    "background_image_opacity" to 0.3f,
                    "background_blur_radius" to 10f,
                    "avatar_corner_radius" to 8f,
                    "font_scale" to 1f,
                    "bubble_user_image_crop_left" to 0f,
                    "bubble_user_image_crop_top" to 0f,
                    "bubble_user_image_crop_right" to 0f,
                    "bubble_user_image_crop_bottom" to 0f,
                    "bubble_user_image_repeat_start" to 0.35f,
                    "bubble_user_image_repeat_end" to 0.65f,
                    "bubble_user_image_repeat_y_start" to 0.35f,
                    "bubble_user_image_repeat_y_end" to 0.65f,
                    "bubble_user_image_scale" to 1f,
                    "bubble_ai_image_crop_left" to 0f,
                    "bubble_ai_image_crop_top" to 0f,
                    "bubble_ai_image_crop_right" to 0f,
                    "bubble_ai_image_crop_bottom" to 0f,
                    "bubble_ai_image_repeat_start" to 0.35f,
                    "bubble_ai_image_repeat_end" to 0.65f,
                    "bubble_ai_image_repeat_y_start" to 0.35f,
                    "bubble_ai_image_repeat_y_end" to 0.65f,
                    "bubble_ai_image_scale" to 1f,
                    "bubble_content_padding_left" to 12f,
                    "bubble_content_padding_right" to 12f,
                    "bubble_ai_content_padding_left" to 12f,
                    "bubble_ai_content_padding_right" to 12f,
                ),
            )
    }
}

data class ThemePreferenceSnapshot(
    val source: String,
    val sourceId: String? = null,
    val themeMode: String,
    val useSystemTheme: Boolean,
    val useCustomColors: Boolean,
    val customPrimaryColor: Int? = null,
    val customSecondaryColor: Int? = null,
    val onColorMode: String,
    val useBackgroundImage: Boolean,
    val backgroundImageUri: String? = null,
    val backgroundMediaType: String,
    val backgroundImageOpacity: Float,
    val chatHeaderTransparent: Boolean,
    val chatHeaderOverlayMode: Boolean,
    val chatInputTransparent: Boolean,
    val chatInputFloating: Boolean,
    val chatInputLiquidGlass: Boolean,
    val chatInputWaterGlass: Boolean,
    val chatStyle: String,
    val inputStyle: String,
    val bubbleShowAvatar: Boolean,
    val bubbleWideLayoutEnabled: Boolean,
    val cursorUserBubbleFollowTheme: Boolean,
    val cursorUserBubbleColor: Int? = null,
    val bubbleUserBubbleColor: Int? = null,
    val bubbleAiBubbleColor: Int? = null,
    val bubbleUserTextColor: Int? = null,
    val bubbleAiTextColor: Int? = null,
    val bubbleUserUseImage: Boolean,
    val bubbleAiUseImage: Boolean,
    val bubbleUserImageUri: String? = null,
    val bubbleAiImageUri: String? = null,
    val bubbleImageRenderMode: String,
    val bubbleUserRoundedCornersEnabled: Boolean,
    val bubbleAiRoundedCornersEnabled: Boolean,
    val bubbleUserContentPaddingLeft: Float,
    val bubbleUserContentPaddingRight: Float,
    val bubbleAiContentPaddingLeft: Float,
    val bubbleAiContentPaddingRight: Float,
    val customUserAvatarUri: String? = null,
    val customAiAvatarUri: String? = null,
    val avatarShape: String,
    val avatarCornerRadius: Float,
    val fontType: String,
    val systemFontName: String? = null,
    val customFontPath: String? = null,
    val fontScale: Float,
    val showThinkingProcess: Boolean,
    val showStatusTags: Boolean,
    val showModelProvider: Boolean,
    val showModelName: Boolean,
    val showRoleName: Boolean,
    val showUserName: Boolean,
    val showMessageTokenStats: Boolean,
    val showMessageTimingStats: Boolean,
    val showMessageTimestamp: Boolean,
    val showInputProcessingStatus: Boolean,
    val useCustomFont: Boolean = false,
    val cursorUserBubbleLiquidGlass: Boolean = false,
    val cursorUserBubbleWaterGlass: Boolean = false,
    val bubbleUserBubbleLiquidGlass: Boolean = false,
    val bubbleUserBubbleWaterGlass: Boolean = false,
    val bubbleAiBubbleLiquidGlass: Boolean = false,
    val bubbleAiBubbleWaterGlass: Boolean = false,
    val values: ThemePreferenceValues = ThemePreferenceValues.defaultVisual(),
)
