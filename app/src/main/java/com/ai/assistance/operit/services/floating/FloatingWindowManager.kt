package com.ai.assistance.operit.services.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.AttachmentInfo
import com.ai.assistance.operit.data.model.ChatMessage
import com.ai.assistance.operit.data.model.PromptFunctionType
import com.ai.assistance.operit.services.FloatingChatService
import com.ai.assistance.operit.ui.floating.FloatingChatWindow
import com.ai.assistance.operit.ui.floating.FloatingMode
import com.ai.assistance.operit.ui.floating.FloatingWindowTheme

interface FloatingWindowCallback {
    fun onClose()
    fun onSendMessage(message: String, promptType: PromptFunctionType = PromptFunctionType.CHAT)
    fun onCancelMessage()
    fun onAttachmentRequest(request: String)
    fun onRemoveAttachment(filePath: String)
    fun getMessages(): List<ChatMessage>
    fun getAttachments(): List<AttachmentInfo>
    fun saveState()
    fun getColorScheme(): ColorScheme?
    fun getTypography(): Typography?
}

class FloatingWindowManager(
        private val context: Context,
        private val state: FloatingWindowState,
        private val lifecycleOwner: LifecycleOwner,
        private val viewModelStoreOwner: ViewModelStoreOwner,
        private val savedStateRegistryOwner: SavedStateRegistryOwner,
        private val callback: FloatingWindowCallback
) {
    private val TAG = "FloatingWindowManager"
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var statusIndicatorView: ComposeView? = null
    private var isViewAdded = false
    private var isIndicatorAdded = false

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isViewAdded) return

        try {
            composeView =
                    ComposeView(context).apply {
                        setViewTreeLifecycleOwner(lifecycleOwner)
                        setViewTreeViewModelStoreOwner(viewModelStoreOwner)
                        setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

                        setContent {
                            FloatingWindowTheme(
                                    colorScheme = callback.getColorScheme(),
                                    typography = callback.getTypography()
                            ) { FloatingChatUi() }
                        }
                    }

            val params = createLayoutParams()
            windowManager.addView(composeView, params)
            isViewAdded = true
            Log.d(TAG, "Floating view added at (${params.x}, ${params.y})")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating floating view", e)
        }
    }

    fun destroy() {
        hideStatusIndicator()
        if (isViewAdded) {
            composeView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing view", e)
                }
                composeView = null
                isViewAdded = false
            }
        }
    }

    @Composable
    private fun FloatingChatUi() {
        FloatingChatWindow(
                messages = callback.getMessages(),
                width = state.windowWidth.value,
                height = state.windowHeight.value,
                windowScale = state.windowScale.value,
                onScaleChange = { newScale ->
                    state.windowScale.value = newScale.coerceIn(0.5f, 1.0f)
                    updateWindowSizeInLayoutParams()
                    callback.saveState()
                },
                onClose = { callback.onClose() },
                onResize = { newWidth, newHeight ->
                    state.windowWidth.value = newWidth
                    state.windowHeight.value = newHeight
                    updateWindowSizeInLayoutParams()
                    callback.saveState()
                },
                currentMode = state.currentMode.value,
                previousMode = state.previousMode,
                ballSize = state.ballSize.value,
                onModeChange = { newMode -> switchMode(newMode) },
                onMove = { dx, dy, scale -> onMove(dx, dy, scale) },
                saveWindowState = { callback.saveState() },
                onSendMessage = { message, promptType ->
                    callback.onSendMessage(message, promptType)
                },
                onCancelMessage = { callback.onCancelMessage() },
                onInputFocusRequest = { setFocusable(it) },
                attachments = callback.getAttachments(),
                onAttachmentRequest = { callback.onAttachmentRequest(it) },
                onRemoveAttachment = { callback.onRemoveAttachment(it) },
                chatService = context as? FloatingChatService,
                windowState = state
        )
    }

    fun setWindowInteraction(enabled: Boolean) {
        composeView?.let { view ->
            val currentMode = state.currentMode.value
            if (enabled) {
                // Always make it visible when interaction is enabled
                view.visibility = View.VISIBLE
                hideStatusIndicator()
                updateViewLayout { params ->
                    // Restore interactiveness by removing the flag
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                }
                Log.d(TAG, "Floating window interaction enabled.")
            } else { // Interaction is disabled
                if (currentMode == FloatingMode.FULLSCREEN || currentMode == FloatingMode.WINDOW) {
                    // For fullscreen or window mode, hide the view completely to avoid interfering with screen capture
                    view.visibility = View.GONE
                    showStatusIndicator()
                    Log.d(TAG, "Floating window view hidden for $currentMode mode, showing status indicator.")
                } else {
                    // For other modes, just make it non-touchable but keep it visible for the overlay
                    updateViewLayout { params ->
                        params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    }
                    Log.d(TAG, "Floating window interaction disabled for mode: $currentMode.")
                }
            }
        }
    }

    private fun showStatusIndicator() {
        if (isIndicatorAdded) return
        statusIndicatorView = ComposeView(context).apply {
            // Set the necessary owners for the ComposeView to work correctly.
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                FloatingWindowTheme(
                    colorScheme = callback.getColorScheme(),
                    typography = callback.getTypography()
                ) {
                    StatusIndicator()
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (context.resources.displayMetrics.density * 16).toInt() // 16dp margin from top
        }
        windowManager.addView(statusIndicatorView, params)
        isIndicatorAdded = true
        Log.d(TAG, "Status indicator shown.")
    }

    private fun hideStatusIndicator() {
        if (isIndicatorAdded) {
            statusIndicatorView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing status indicator view", e)
                }
            }
            statusIndicatorView = null
            isIndicatorAdded = false
            Log.d(TAG, "Status indicator hidden.")
        }
    }

    @Composable
    private fun StatusIndicator() {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = context.getString(R.string.ui_automation_in_progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val params =
                WindowManager.LayoutParams(
                        0, // width
                        0, // height
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        0, // flags
                        PixelFormat.TRANSLUCENT
                )
        params.gravity = Gravity.TOP or Gravity.START

        when (state.currentMode.value) {
            FloatingMode.FULLSCREEN -> {
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.MATCH_PARENT
                params.flags = 0 // Focusable
                state.x = 0
                state.y = 0
            }
            FloatingMode.BALL, FloatingMode.VOICE_BALL -> {
                val ballSizeInPx = (state.ballSize.value.value * density).toInt()
                params.width = ballSizeInPx
                params.height = ballSizeInPx
                params.flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                val safeMargin = (16 * density).toInt()
                val minVisible = ballSizeInPx / 2
                state.x =
                        state.x.coerceIn(
                                -ballSizeInPx + minVisible + safeMargin,
                                screenWidth - minVisible - safeMargin
                        )
                state.y = state.y.coerceIn(safeMargin, screenHeight - minVisible - safeMargin)
            }
            FloatingMode.WINDOW, FloatingMode.DragonBones -> {
                val scale = state.windowScale.value
                val windowWidthDp = state.windowWidth.value
                val windowHeightDp = state.windowHeight.value
                params.width = (windowWidthDp.value * density * scale).toInt()
                params.height = (windowHeightDp.value * density * scale).toInt()
                params.flags =
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                val minVisibleWidth = (params.width * 2 / 3)
                val safeMargin = (20 * density).toInt()
                state.x =
                        state.x.coerceIn(
                                -(params.width - minVisibleWidth) + safeMargin,
                                screenWidth - minVisibleWidth - safeMargin
                        )
                state.y =
                        state.y.coerceIn(
                                safeMargin,
                                screenHeight - (params.height / 2) - safeMargin
                        )
            }
        }

        params.x = state.x
        params.y = state.y
        return params
    }

    private fun updateWindowSizeInLayoutParams() {
        updateViewLayout { params ->
            val density = context.resources.displayMetrics.density
            val scale = state.windowScale.value
            val widthDp = state.windowWidth.value
            val heightDp = state.windowHeight.value
            params.width = (widthDp.value * density * scale).toInt()
            params.height = (heightDp.value * density * scale).toInt()
        }
    }

    private fun updateViewLayout(configure: (WindowManager.LayoutParams) -> Unit = {}) {
        composeView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            configure(params)
            windowManager.updateViewLayout(view, params)
        }
    }

    private fun calculateCenteredPosition(
            fromX: Int,
            fromY: Int,
            fromWidth: Int,
            fromHeight: Int,
            toWidth: Int,
            toHeight: Int
    ): Pair<Int, Int> {
        val centerX = fromX + fromWidth / 2
        val centerY = fromY + fromHeight / 2
        val newX = centerX - toWidth / 2
        val newY = centerY - toHeight / 2
        return Pair(newX, newY)
    }

    private fun switchMode(newMode: FloatingMode) {
        if (state.isTransitioning || state.currentMode.value == newMode) return
        state.isTransitioning = true

        val view = composeView ?: return
        val currentParams = view.layoutParams as WindowManager.LayoutParams

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val currentWidth: Int
        val currentHeight: Int

        // Logic for leaving a mode
        state.previousMode = state.currentMode.value
        when (state.currentMode.value) {
            FloatingMode.BALL, FloatingMode.VOICE_BALL -> {
                state.lastBallPositionX = currentParams.x
                state.lastBallPositionY = currentParams.y
                currentWidth = (state.ballSize.value.value * density).toInt()
                currentHeight = currentWidth
            }
            FloatingMode.WINDOW -> {
                state.lastWindowPositionX = currentParams.x
                state.lastWindowPositionY = currentParams.y
                state.lastWindowScale = state.windowScale.value
                currentWidth =
                        (state.windowWidth.value.value * density * state.windowScale.value).toInt()
                currentHeight =
                        (state.windowHeight.value.value * density * state.windowScale.value).toInt()
            }
            FloatingMode.FULLSCREEN -> {
                // Leaving fullscreen, no special state to save
                currentWidth = screenWidth
                currentHeight = screenHeight
            }
            FloatingMode.DragonBones -> {
                // Treat DragonBones mode similar to Window mode when leaving
                state.lastWindowPositionX = currentParams.x
                state.lastWindowPositionY = currentParams.y
                state.lastWindowScale = state.windowScale.value
                currentWidth =
                        (state.windowWidth.value.value * density * state.windowScale.value).toInt()
                currentHeight =
                        (state.windowHeight.value.value * density * state.windowScale.value).toInt()
            }
        }

        state.currentMode.value = newMode
        callback.saveState()

        // Update layout for the new mode
        updateViewLayout { params ->
            when (newMode) {
                FloatingMode.BALL, FloatingMode.VOICE_BALL -> {
                    params.flags =
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    val ballSizeInPx = (state.ballSize.value.value * density).toInt()
                    params.width = ballSizeInPx
                    params.height = ballSizeInPx
                    val (newX, newY) =
                            calculateCenteredPosition(
                                    currentParams.x,
                                    currentParams.y,
                                    currentWidth,
                                    currentHeight,
                                    ballSizeInPx,
                                    ballSizeInPx
                            )
                    params.x = newX
                    params.y = newY

                    // Coerce position to be within screen bounds for ball mode
                    val minVisible = ballSizeInPx / 2
                    params.x =
                            params.x.coerceIn(-ballSizeInPx + minVisible, screenWidth - minVisible)
                    params.y = params.y.coerceIn(0, screenHeight - minVisible)
                }
                FloatingMode.WINDOW -> {
                    params.flags =
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    val newWidth =
                            (state.windowWidth.value.value * density * state.lastWindowScale)
                                    .toInt()
                    val newHeight =
                            (state.windowHeight.value.value * density * state.lastWindowScale)
                                    .toInt()
                    params.width = newWidth
                    params.height = newHeight
                    if (state.previousMode == FloatingMode.BALL ||
                                    state.previousMode == FloatingMode.VOICE_BALL
                    ) {
                        val (centeredX, centeredY) =
                                calculateCenteredPosition(
                                        currentParams.x,
                                        currentParams.y,
                                        currentWidth,
                                        currentHeight,
                                        newWidth,
                                        newHeight
                                )
                        params.x = centeredX
                        params.y = centeredY
                    } else {
                        params.x = state.lastWindowPositionX
                        params.y = state.lastWindowPositionY
                    }
                    state.windowScale.value = state.lastWindowScale

                    // Coerce position to be within screen bounds for window mode
                    val minVisibleWidth = (params.width * 2 / 3)
                    val minVisibleHeight = (params.height * 2 / 3)
                    params.x =
                            params.x.coerceIn(
                                    -(params.width - minVisibleWidth),
                                    screenWidth - minVisibleWidth / 2
                            )
                    params.y = params.y.coerceIn(0, screenHeight - minVisibleHeight)
                }
                FloatingMode.FULLSCREEN -> {
                    params.flags = 0 // Remove all flags, making it focusable
                    params.width = screenWidth
                    params.height = screenHeight
                    params.x = 0
                    params.y = 0
                }
                FloatingMode.DragonBones -> {
                    params.flags =
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    val newWidth =
                            (state.windowWidth.value.value * density * state.lastWindowScale)
                                    .toInt()
                    val newHeight =
                            (state.windowHeight.value.value * density * state.lastWindowScale)
                                    .toInt()
                    params.width = newWidth
                    params.height = newHeight
                    params.x = state.lastWindowPositionX
                    params.y = state.lastWindowPositionY

                    // Coerce position to be within screen bounds for DragonBones mode
                    val minVisibleWidth = (params.width * 2 / 3)
                    val minVisibleHeight = (params.height * 2 / 3)
                    params.x =
                            params.x.coerceIn(
                                    -(params.width - minVisibleWidth),
                                    screenWidth - minVisibleWidth / 2
                            )
                    params.y = params.y.coerceIn(0, screenHeight - minVisibleHeight)
                }
            }
            // Sync state with params to avoid inconsistency
            state.x = params.x
            state.y = params.y
        }
        // Use a Handler to reset the transitioning flag after a short delay
        Handler(Looper.getMainLooper()).postDelayed({ state.isTransitioning = false }, 300)
    }

    private fun onMove(dx: Float, dy: Float, scale: Float) {
        if (state.currentMode.value == FloatingMode.FULLSCREEN) return // Disable move in fullscreen

        updateViewLayout { params ->
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val density = displayMetrics.density

            state.windowScale.value = scale

            val sensitivity =
                    if (state.currentMode.value == FloatingMode.BALL ||
                                    state.currentMode.value == FloatingMode.VOICE_BALL
                    )
                            1.0f
                    else scale
            params.x += (dx * sensitivity).toInt()
            params.y += (dy * sensitivity).toInt()

            if (state.currentMode.value == FloatingMode.BALL ||
                            state.currentMode.value == FloatingMode.VOICE_BALL
            ) {
                val ballSize = (state.ballSize.value.value * density).toInt()
                val minVisible = ballSize / 2
                params.x = params.x.coerceIn(-ballSize + minVisible, screenWidth - minVisible)
                params.y = params.y.coerceIn(0, screenHeight - minVisible)
            } else {
                val windowWidth = (state.windowWidth.value.value * density * scale).toInt()
                val windowHeight = (state.windowHeight.value.value * density * scale).toInt()
                val minVisibleWidth = (windowWidth * 2 / 3)
                val minVisibleHeight = (windowHeight * 2 / 3)
                params.x =
                        params.x.coerceIn(
                                -(windowWidth - minVisibleWidth),
                                screenWidth - minVisibleWidth / 2
                        )
                params.y = params.y.coerceIn(0, screenHeight - minVisibleHeight)
            }
            state.x = params.x
            state.y = params.y
        }
    }

    private fun setFocusable(needsFocus: Boolean) {
        val view = composeView ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if (needsFocus) {
            // Step 1: 更新窗口参数使其可获取焦点
            updateViewLayout { params ->
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()

                // 为全屏模式特殊处理软键盘，以避免遮挡UI
                if (state.currentMode.value == FloatingMode.FULLSCREEN) {
                    params.softInputMode =
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                }
            }

            // Step 2: 延迟请求焦点并显示键盘
            // 延迟是必要的，以确保WindowManager有足够的时间处理窗口标志的变更
            view.postDelayed(
                    {
                        view.requestFocus()
                        imm.showSoftInput(view.findFocus(), InputMethodManager.SHOW_IMPLICIT)
                    },
                    200
            )
        } else {
            // Step 1: 立即隐藏键盘
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            // Step 2: 延迟恢复窗口的不可聚焦状态（全屏模式除外）
            view.postDelayed(
                    {
                        updateViewLayout { params ->
                            // 在非全屏模式下，恢复FLAG_NOT_FOCUSABLE，以便与窗口下的内容交互
                            if (state.currentMode.value != FloatingMode.FULLSCREEN) {
                                params.flags =
                                        params.flags or
                                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            }
                            // 重置软键盘模式
                            params.softInputMode =
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                        }
                    },
                    100
            )
        }
    }

    /**
     * 获取当前使用的ComposeView实例
     * @return View? 当前的ComposeView实例，如果未创建则返回null
     */
    fun getComposeView(): View? {
        return composeView
    }
}
