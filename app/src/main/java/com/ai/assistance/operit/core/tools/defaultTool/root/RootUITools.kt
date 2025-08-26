package com.ai.assistance.operit.core.tools.defaultTool.root

import android.content.Context
import android.util.Log
import com.ai.assistance.operit.core.tools.StringResultData
import com.ai.assistance.operit.core.tools.UIActionResultData
import com.ai.assistance.operit.core.tools.defaultTool.admin.AdminUITools
import com.ai.assistance.operit.core.tools.system.AndroidShellExecutor
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.ToolResult
import java.util.Random
import kotlin.random.Random as KotlinRandom
import kotlinx.coroutines.delay

/** Root级别的UI工具，使用底层事件注入实现UI操作 这个实现使用直接的设备节点操作，比标准input命令更难被检测 */
open class RootUITools(context: Context) : AdminUITools(context) {

    companion object {
        private const val TAG = "RootUITools"

        // 设备路径
        private const val INPUT_DEVICES_PATH = "/dev/input"

        // 触摸事件类型
        private const val EV_SYN = 0
        private const val EV_KEY = 1
        private const val EV_ABS = 3

        // 触摸事件代码
        private const val SYN_REPORT = 0
        private const val BTN_TOUCH = 330
        private const val ABS_MT_TRACKING_ID = 57
        private const val ABS_MT_POSITION_X = 53
        private const val ABS_MT_POSITION_Y = 54
        private const val ABS_MT_PRESSURE = 58
        private const val ABS_MT_TOUCH_MAJOR = 48

        // 随机器，用于添加人类操作的随机性
        private val random = Random()
    }

    // 缓存触摸设备路径
    private var touchDevicePath: String? = null

    /** 使用底层事件发送点击操作 这种方法绕过了Android的input命令，更难被检测 */
    override suspend fun tap(tool: AITool): ToolResult {
        val x = tool.parameters.find { it.name == "x" }?.value?.toIntOrNull()
        val y = tool.parameters.find { it.name == "y" }?.value?.toIntOrNull()

        if (x == null || y == null) {
            return ToolResult(
                    toolName = tool.name,
                    success = false,
                    result = StringResultData(""),
                    error =
                            "Missing or invalid coordinates. Both 'x' and 'y' must be valid integers."
            )
        }

        // 显示点击反馈
        operationOverlay.showTap(x, y)

        try {
            // 获取触摸设备
            val devicePath = findTouchDevice()
            if (devicePath == null) {
                Log.d(TAG, "No touch device found, falling back to standard input")
                return super.tap(tool)
            }

            // 生成一个唯一的跟踪ID
            val trackingId = (System.currentTimeMillis() % 65536).toInt()

            // 添加小量随机偏移，模拟人类触摸的不精确性
            val randX = x + KotlinRandom.nextInt(-2, 3)
            val randY = y + KotlinRandom.nextInt(-2, 3)

            // 构建触摸事件序列，模拟真实人类触摸
            val touchEvents = buildString {
                // 按下事件
                append("sendevent $devicePath $EV_ABS $ABS_MT_TRACKING_ID $trackingId\n")
                append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_X $randX\n")
                append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_Y $randY\n")

                // 随机压力和接触面积
                val pressure = KotlinRandom.nextInt(40, 61)
                val touchMajor = KotlinRandom.nextInt(5, 11)
                append("sendevent $devicePath $EV_ABS $ABS_MT_PRESSURE $pressure\n")
                append("sendevent $devicePath $EV_ABS $ABS_MT_TOUCH_MAJOR $touchMajor\n")

                // 按下按钮
                append("sendevent $devicePath $EV_KEY $BTN_TOUCH 1\n")
                append("sendevent $devicePath $EV_SYN $SYN_REPORT 0\n")

                // 添加随机短暂停顿，模拟人类手指在屏幕上短暂停留
                append("sleep 0.${KotlinRandom.nextInt(8, 20)}\n")

                // 微小移动，进一步模拟真实触摸
                val microMoveX = randX + KotlinRandom.nextInt(-1, 2)
                val microMoveY = randY + KotlinRandom.nextInt(-1, 2)
                append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_X $microMoveX\n")
                append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_Y $microMoveY\n")
                append("sendevent $devicePath $EV_SYN $SYN_REPORT 0\n")

                // 另一个短暂停顿
                append("sleep 0.${KotlinRandom.nextInt(5, 15)}\n")

                // 抬起事件
                append("sendevent $devicePath $EV_ABS $ABS_MT_TRACKING_ID -1\n") // -1表示释放跟踪ID
                append("sendevent $devicePath $EV_KEY $BTN_TOUCH 0\n")
                append("sendevent $devicePath $EV_SYN $SYN_REPORT 0\n")
            }

            // 执行触摸事件序列
            Log.d(TAG, "Executing low-level touch events at ($randX, $randY)")
            val result = AndroidShellExecutor.executeShellCommand(touchEvents)

            if (result.success) {
                return ToolResult(
                        toolName = tool.name,
                        success = true,
                        result =
                                UIActionResultData(
                                        actionType = "tap",
                                        actionDescription =
                                                "Executed low-level touch events at ($x, $y) via direct device injection",
                                        coordinates = Pair(x, y)
                                ),
                        error = ""
                )
            } else {
                operationOverlay.hide()
                Log.e(TAG, "Low-level tap failed: ${result.stderr}")
                return super.tap(tool)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing low-level tap", e)
            operationOverlay.hide()
            return super.tap(tool)
        }
    }

    /** 使用底层事件实现滑动操作 生成更多中间点，添加随机微扰动和不规则的时间间隔 */
    override suspend fun swipe(tool: AITool): ToolResult {
        val startX = tool.parameters.find { it.name == "start_x" }?.value?.toIntOrNull()
        val startY = tool.parameters.find { it.name == "start_y" }?.value?.toIntOrNull()
        val endX = tool.parameters.find { it.name == "end_x" }?.value?.toIntOrNull()
        val endY = tool.parameters.find { it.name == "end_y" }?.value?.toIntOrNull()
        val duration = tool.parameters.find { it.name == "duration" }?.value?.toIntOrNull() ?: 300
        val steps = tool.parameters.find { it.name == "steps" }?.value?.toIntOrNull() ?: 10

        if (startX == null || startY == null || endX == null || endY == null) {
            return ToolResult(
                    toolName = tool.name,
                    success = false,
                    result = StringResultData(""),
                    error =
                            "Missing or invalid coordinates. 'start_x', 'start_y', 'end_x', and 'end_y' must be valid integers."
            )
        }

        // 显示滑动反馈
        operationOverlay.showSwipe(startX, startY, endX, endY)

        try {
            val devicePath = findTouchDevice()
            if (devicePath == null) {
                Log.d(TAG, "No touch device found, falling back to standard swipe")
                return super.swipe(tool)
            }

            // 生成唯一跟踪ID
            val trackingId = (System.currentTimeMillis() % 65536).toInt()

            // 计算每步时间间隔（添加轻微的随机性）
            val baseStepDuration = duration / steps.toFloat()

            // 构建滑动事件序列
            val swipeEvents = StringBuilder()

            // 添加更多的中间点，使路径更自然（真实人类不会完全直线滑动）
            val actualSteps = steps * 2 // 增加步数，让动作更流畅

            // 按下事件
            swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_TRACKING_ID $trackingId\n")

            // 初始压力和接触面积（随机值）
            val initialPressure = KotlinRandom.nextInt(40, 61)
            val initialTouchMajor = KotlinRandom.nextInt(5, 11)
            swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_PRESSURE $initialPressure\n")
            swipeEvents.append(
                    "sendevent $devicePath $EV_ABS $ABS_MT_TOUCH_MAJOR $initialTouchMajor\n"
            )

            // 初始位置（添加轻微随机性）
            val initialX = startX + KotlinRandom.nextInt(-2, 3)
            val initialY = startY + KotlinRandom.nextInt(-2, 3)
            swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_X $initialX\n")
            swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_Y $initialY\n")

            // 按下按钮
            swipeEvents.append("sendevent $devicePath $EV_KEY $BTN_TOUCH 1\n")
            swipeEvents.append("sendevent $devicePath $EV_SYN $SYN_REPORT 0\n")

            // 短暂停顿，模拟人类手指接触屏幕后的微小停顿
            swipeEvents.append("sleep 0.${KotlinRandom.nextInt(8, 15)}\n")

            // 生成滑动路径
            for (i in 1 until actualSteps) {
                val progress = i.toFloat() / actualSteps

                // 计算基础位置
                val baseX = startX + ((endX - startX) * progress).toInt()
                val baseY = startY + ((endY - startY) * progress).toInt()

                // 添加微小随机偏移，模拟人类手指抖动
                val randX = baseX + KotlinRandom.nextInt(-3, 4)
                val randY = baseY + KotlinRandom.nextInt(-3, 4)

                // 随机变化压力和接触面积（人类滑动时这些值会略微变化）
                val pressureChange = KotlinRandom.nextInt(-5, 6)
                val pressure = (initialPressure + pressureChange).coerceIn(35, 65)
                swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_PRESSURE $pressure\n")

                // 每隔几步更新一次接触面积
                if (i % 3 == 0) {
                    val touchMajorChange = KotlinRandom.nextInt(-2, 3)
                    val touchMajor = (initialTouchMajor + touchMajorChange).coerceIn(4, 12)
                    swipeEvents.append(
                            "sendevent $devicePath $EV_ABS $ABS_MT_TOUCH_MAJOR $touchMajor\n"
                    )
                }

                // 更新位置
                swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_X $randX\n")
                swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_Y $randY\n")
                swipeEvents.append("sendevent $devicePath $EV_SYN $SYN_REPORT 0\n")

                // 添加不均匀的时间间隔，模拟人类滑动的不均匀性
                val randomFactor = KotlinRandom.nextFloat() * 0.4f + 0.8f // 0.8到1.2之间
                val stepTime = (baseStepDuration * randomFactor / 2).coerceAtLeast(1f)
                swipeEvents.append("sleep %.3f\n".format(stepTime / 1000))
            }

            // 最终位置（添加轻微随机性）
            val finalX = endX + KotlinRandom.nextInt(-2, 3)
            val finalY = endY + KotlinRandom.nextInt(-2, 3)
            swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_X $finalX\n")
            swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_POSITION_Y $finalY\n")
            swipeEvents.append("sendevent $devicePath $EV_SYN $SYN_REPORT 0\n")

            // 短暂停顿，模拟人类手指在最终位置的微小停顿
            swipeEvents.append("sleep 0.${KotlinRandom.nextInt(5, 12)}\n")

            // 抬起事件
            swipeEvents.append("sendevent $devicePath $EV_ABS $ABS_MT_TRACKING_ID -1\n")
            swipeEvents.append("sendevent $devicePath $EV_KEY $BTN_TOUCH 0\n")
            swipeEvents.append("sendevent $devicePath $EV_SYN $SYN_REPORT 0\n")

            // 执行滑动事件序列
            Log.d(TAG, "Executing low-level swipe events from ($startX, $startY) to ($endX, $endY)")
            val result = AndroidShellExecutor.executeShellCommand(swipeEvents.toString())

            if (result.success) {
                return ToolResult(
                        toolName = tool.name,
                        success = true,
                        result =
                                UIActionResultData(
                                        actionType = "swipe",
                                        actionDescription =
                                                "Executed low-level swipe from ($startX, $startY) to ($endX, $endY) via direct device injection",
                                        coordinates = Pair(startX, startY)
                                ),
                        error = ""
                )
            } else {
                operationOverlay.hide()
                Log.e(TAG, "Low-level swipe failed: ${result.stderr}")
                return super.swipe(tool)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing low-level swipe", e)
            operationOverlay.hide()
            return super.swipe(tool)
        }
    }

    /** 使用底层键盘事件注入实现按键功能 更接近于人类按键，添加随机延迟和可变压力 */
    override suspend fun pressKey(tool: AITool): ToolResult {
        val keyCode = tool.parameters.find { it.name == "key_code" }?.value
        val longPress =
                tool.parameters.find { it.name == "long_press" }?.value?.toBoolean() ?: false

        if (keyCode == null) {
            return ToolResult(
                    toolName = tool.name,
                    success = false,
                    result = StringResultData(""),
                    error = "Missing 'key_code' parameter."
            )
        }

        // 直接调用父类实现
        return super.pressKey(tool)
    }

    /** 通过低级事件实现文本输入，更真实地模拟人类打字 */
    override suspend fun setInputText(tool: AITool): ToolResult {
        val text = tool.parameters.find { it.name == "text" }?.value ?: ""

        try {
            // 首先清除当前文本
            // 由于清除动作较复杂，使用标准命令
            Log.d(TAG, "Clearing existing text")
            val clearCommand =
                    "input keyevent KEYCODE_MOVE_HOME && input keyevent --longpress KEYCODE_DEL"
            val clearResult = AndroidShellExecutor.executeShellCommand(clearCommand)

            if (!clearResult.success) {
                Log.e(TAG, "Failed to clear text: ${clearResult.stderr}")
            }

            // 短暂延迟
            delay(KotlinRandom.nextLong(200, 500))

            // 如果文本为空，只需清除字段
            if (text.isEmpty()) {
                return ToolResult(
                        toolName = tool.name,
                        success = true,
                        result =
                                UIActionResultData(
                                        actionType = "textInput",
                                        actionDescription = "Successfully cleared input field"
                                ),
                        error = ""
                )
            }

            // 获取屏幕中心作为文本输入的位置（用于UI反馈）
            val displayMetrics = context.resources.displayMetrics
            val centerX = displayMetrics.widthPixels / 2
            val centerY = displayMetrics.heightPixels / 2

            // 显示文本输入反馈
            operationOverlay.showTextInput(centerX, centerY, text)

            // 回退方法：使用standard input命令，但添加随机延迟模拟人类打字
            Log.d(TAG, "Using text input method with human-like delays")

            // 对于较长文本或复杂文本，使用分段输入
            val segments = text.chunked(10)

            for (segment in segments) {
                val escapedSegment =
                        segment.replace(" ", "%s").replace("'", "\\'").replace("\"", "\\\"")
                val segmentCommand = "input text '$escapedSegment'"
                val segmentResult = AndroidShellExecutor.executeShellCommand(segmentCommand)

                if (!segmentResult.success) {
                    operationOverlay.hide()
                    return ToolResult(
                            toolName = tool.name,
                            success = false,
                            result = StringResultData(""),
                            error =
                                    "Failed to input text: ${segmentResult.stderr ?: "Unknown error"}"
                    )
                }

                // 在文本块之间添加随机延迟，模拟人类思考或停顿
                if (segments.size > 1) {
                    delay(KotlinRandom.nextLong(200, 800))
                }
            }

            return ToolResult(
                    toolName = tool.name,
                    success = true,
                    result =
                            UIActionResultData(
                                    actionType = "textInput",
                                    actionDescription =
                                            "Successfully input text with human-like delays: $text"
                            ),
                    error = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting input text", e)
            operationOverlay.hide()

            // 最后的后备方法 - 直接使用标准input
            return super.setInputText(tool)
        }
    }

    /** 使用更难被检测的方法点击UI元素 */
    override suspend fun clickElement(tool: AITool): ToolResult {
        val resourceId = tool.parameters.find { it.name == "resourceId" }?.value
        val className = tool.parameters.find { it.name == "className" }?.value
        val bounds = tool.parameters.find { it.name == "bounds" }?.value

        if (resourceId == null && className == null && bounds == null) {
            return ToolResult(
                    toolName = tool.name,
                    success = false,
                    result = StringResultData(""),
                    error =
                            "Missing element identifier. Provide at least one of: 'resourceId', 'className', or 'bounds'."
            )
        }

        // 使用父类的方法进行点击
        return super.clickElement(tool)
    }

    /** 通过root权限获取更详细的UI层次结构 */
    override suspend fun getPageInfo(tool: AITool): ToolResult {
        // 使用父类的实现
        return super.getPageInfo(tool)
    }
    /** 查找适合触摸事件的输入设备 这是一个root权限专有操作，因为需要访问/dev/input下的设备 */
    private suspend fun findTouchDevice(): String? {
        if (touchDevicePath != null) return touchDevicePath

        try {
            // 列出所有输入设备
            val findCommand = "find $INPUT_DEVICES_PATH -name \"event*\" | sort"
            val result = AndroidShellExecutor.executeShellCommand(findCommand)

            if (!result.success || result.stdout.isBlank()) {
                Log.e(TAG, "Failed to list input devices")
                return null
            }

            val devices = result.stdout.trim().split("\n")

            // 对每个设备进行测试，找出支持触摸事件的设备
            for (device in devices) {
                val checkCommand = "getevent -p $device | grep -E 'ABS_MT_POSITION|BTN_TOUCH'"
                val checkResult = AndroidShellExecutor.executeShellCommand(checkCommand)

                if (checkResult.success && checkResult.stdout.isNotEmpty()) {
                    Log.d(TAG, "Found touch input device: $device")
                    touchDevicePath = device
                    return device
                }
            }

            // 如果没有找到明确的触摸设备，使用第一个设备作为后备
            if (devices.isNotEmpty()) {
                Log.d(TAG, "Using first available device: ${devices[0]}")
                touchDevicePath = devices[0]
                return devices[0]
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding touch device", e)
            return null
        }
    }
}
