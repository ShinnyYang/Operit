package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.ai.assistance.operit.ui.components.CustomScaffold
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.preferences.UserPreferencesManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

/** 根据用户选择的关键信息生成偏好描述 */
private fun generatePreferencesDescription(
        gender: String,
        occupation: String,
        birthDate: Long
): String {
    val genderDesc =
            when (gender) {
                "男" -> "一位男性用户"
                "女" -> "一位女性用户"
                else -> "一位用户"
            }

    // 根据出生日期计算年龄
    val age =
            if (birthDate > 0) {
                val today = Calendar.getInstance()
                val birthCal = Calendar.getInstance().apply { timeInMillis = birthDate }
                var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
                // 如果今年的生日还没过，年龄减一
                if (today.get(Calendar.MONTH) < birthCal.get(Calendar.MONTH) ||
                                (today.get(Calendar.MONTH) == birthCal.get(Calendar.MONTH) &&
                                        today.get(Calendar.DAY_OF_MONTH) <
                                                birthCal.get(Calendar.DAY_OF_MONTH))
                ) {
                    age--
                }
                age
            } else {
                0
            }

    val ageDesc =
            when {
                age in 1..12 -> "儿童"
                age in 13..17 -> "青少年"
                age in 18..25 -> "年轻人"
                age in 26..40 -> "壮年人士"
                age in 41..60 -> "中年人士"
                age > 60 -> "老年人士"
                else -> ""
            }

    val occupationDesc =
            when (occupation) {
                "学生" -> "学生身份，关注学习和个人发展"
                "上班族" -> "职场人士，注重工作效率和专业发展"
                "自由职业" -> "自由职业者，重视灵活性和创新能力"
                else -> "工作者"
            }

    val interestTopics =
            when (occupation) {
                "学生" -> "学习资源、知识整理、考试准备和个人成长"
                "上班族" -> "时间管理、专业技能、职场沟通和工作效率提升"
                "自由职业" -> "创意设计、项目管理、自我提升和技能拓展"
                else -> "各类实用工具、生活便利和个人提升"
            }

    val preferenceStyle =
            when (gender) {
                "男" -> "直接明了的交流方式，偏好简洁高效的解决方案"
                "女" -> "细致周到的交流方式，注重细节和用户体验"
                else -> "清晰有条理的交流方式，关注实用性和效率"
            }

    // 组装完整描述
    val description = buildString {
        append("我是$genderDesc")
        if (ageDesc.isNotEmpty()) append("，$ageDesc")
        append("，$occupationDesc。")
        append("我对$interestTopics 特别感兴趣。")
        append("在沟通中我喜欢$preferenceStyle。")
    }

    // 确保不超过100字
    return if (description.length > 100) description.substring(0, 97) + "..." else description
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserPreferencesGuideScreen(
        profileName: String = "",
        profileId: String = "",
        onComplete: () -> Unit,
        navigateToPermissions: () -> Unit = onComplete,
        onBackPressed: () -> Unit = onComplete
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { UserPreferencesManager(context) }

    var selectedGender by remember { mutableStateOf("") }
    var selectedOccupation by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf(0L) }
    var selectedPersonality by remember { mutableStateOf(setOf<String>()) }
    var selectedIdentity by remember { mutableStateOf(setOf<String>()) }
    var selectedAiStyleTags by remember { mutableStateOf(setOf<String>()) }

    // 自定义标签相关状态
    var customPersonalityTags by remember { mutableStateOf(setOf<String>()) }
    var customIdentityTags by remember { mutableStateOf(setOf<String>()) }
    var customAiStyleTags by remember { mutableStateOf(setOf<String>()) }

    // 新增标签对话框相关状态
    var showCustomTagDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var currentTagCategory by remember { mutableStateOf("") } // personality, identity, or aiStyle

    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    // 初始化日期选择器状态
    val initialSelectedDateMillis =
            if (birthDate > 0) birthDate
            else {
                // 默认设置为1990年1月1日
                Calendar.getInstance()
                        .apply {
                            set(Calendar.YEAR, 1990)
                            set(Calendar.MONTH, Calendar.JANUARY)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        .timeInMillis
            }
    val datePickerState =
            rememberDatePickerState(
                    initialSelectedDateMillis = initialSelectedDateMillis,
                    initialDisplayMode = DisplayMode.Picker
            )

    // 各种选项数据
    val genderOptions = listOf("男", "女", "其他")
    val occupationOptions =
            listOf("学生", "教师", "医生", "工程师", "设计师", "程序员", "企业主", "销售", "客服", "自由职业", "退休人员", "其他")
    val personalityOptions =
            listOf(
                    "外向",
                    "内向",
                    "敏感",
                    "理性",
                    "感性",
                    "谨慎",
                    "冒险",
                    "耐心",
                    "急躁",
                    "乐观",
                    "悲观",
                    "好奇",
                    "保守",
                    "创新",
                    "细致",
                    "粗放"
            )
    val identityOptions =
            listOf(
                    "学生",
                    "教师",
                    "父母",
                    "音乐爱好者",
                    "艺术爱好者",
                    "游戏玩家",
                    "运动员",
                    "技术宅",
                    "旅行爱好者",
                    "美食家",
                    "创业者",
                    "专业人士"
            )
    val aiStyleOptions = listOf("专业严谨", "活泼幽默", "简洁直接", "耐心细致", "创意思维", "技术导向", "教学指导", "情感支持")

    // 从配置文件加载现有数据
    LaunchedEffect(profileId) {
        try {
            // 检查是否存在配置文件列表
            val profiles = preferencesManager.profileListFlow.first()

            // 如果配置列表为空，创建默认配置
            if (profiles.isEmpty()) {
                // 创建默认配置并设置为活动配置
                val defaultProfileId = preferencesManager.createProfile("默认配置", isDefault = true)
                preferencesManager.setActiveProfile(defaultProfileId)
                // 给一点时间让数据存储更新
                delay(100)
            }

            // 如果提供了profileId，加载特定配置；否则加载活动配置
            val profile =
                    if (profileId.isNotEmpty()) {
                        preferencesManager.getUserPreferencesFlow(profileId).first()
                    } else {
                        preferencesManager.getUserPreferencesFlow().first()
                    }

            // 填充表单字段
            selectedGender = profile.gender
            selectedOccupation = profile.occupation
            birthDate = profile.birthDate

            // 解析个性特点
            val personalityTags =
                    profile.personality.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val (standard, custom) = personalityTags.partition { personalityOptions.contains(it) }
            selectedPersonality = standard.toSet()
            customPersonalityTags = custom.toSet()

            // 解析身份认同
            val identityTags =
                    profile.identity.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val (standardId, customId) = identityTags.partition { identityOptions.contains(it) }
            selectedIdentity = standardId.toSet()
            customIdentityTags = customId.toSet()

            // 解析AI风格标签
            val styleTags = profile.aiStyle.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val (standardStyle, customStyle) = styleTags.partition { aiStyleOptions.contains(it) }
            selectedAiStyleTags = standardStyle.toSet()
            customAiStyleTags = customStyle.toSet()

            // 如果没有已选的标签，默认选择一些并保存到配置中
            var needsUpdate = false

            if (selectedAiStyleTags.isEmpty() && customAiStyleTags.isEmpty()) {
                selectedAiStyleTags = setOf("专业严谨", "简洁直接")
                needsUpdate = true
            }

            if (selectedPersonality.isEmpty() && customPersonalityTags.isEmpty()) {
                selectedPersonality = setOf("理性", "耐心")
                needsUpdate = true
            }

            // 如果需要更新默认值，保存到配置
            if (needsUpdate) {
                if (profileId.isNotEmpty()) {
                    // 更新指定的配置文件
                    preferencesManager.updateProfileCategory(
                            profileId = profileId,
                            aiStyle = selectedAiStyleTags.joinToString(", "),
                            personality = selectedPersonality.joinToString(", ")
                    )
                } else {
                    // 更新当前活动的配置文件
                    preferencesManager.updateProfileCategory(
                            aiStyle = selectedAiStyleTags.joinToString(", "),
                            personality = selectedPersonality.joinToString(", ")
                    )
                }
            }
        } catch (e: Exception) {
            // 如果获取配置失败，创建默认配置
            try {
                // 创建默认配置
                val defaultProfileId = preferencesManager.createProfile("默认配置", isDefault = true)

                // 设置默认值
                selectedAiStyleTags = setOf("专业严谨", "简洁直接")
                selectedPersonality = setOf("理性", "耐心")

                // 保存默认值到配置
                if (profileId.isNotEmpty()) {
                    // 如果有指定的profileId，更新指定配置
                    preferencesManager.updateProfileCategory(
                            profileId = profileId,
                            aiStyle = selectedAiStyleTags.joinToString(", "),
                            personality = selectedPersonality.joinToString(", ")
                    )
                } else {
                    // 否则更新默认配置
                    preferencesManager.updateProfileCategory(
                            profileId = defaultProfileId,
                            aiStyle = selectedAiStyleTags.joinToString(", "),
                            personality = selectedPersonality.joinToString(", ")
                    )
                }
            } catch (ex: Exception) {
                // 如果还是失败，至少确保UI有默认值显示
                selectedAiStyleTags = setOf("专业严谨", "简洁直接")
                selectedPersonality = setOf("理性", "耐心")
            }
        }
    }

    // 自定义标签对话框
    if (showCustomTagDialog) {
        AlertDialog(
                onDismissRequest = {
                    showCustomTagDialog = false
                    newTagText = ""
                },
                title = {
                    Text(
                            when (currentTagCategory) {
                                "personality" -> "添加自定义性格特点"
                                "identity" -> "添加自定义身份认同"
                                "aiStyle" -> "添加自定义AI风格"
                                else -> "添加自定义标签"
                            }
                    )
                },
                text = {
                    OutlinedTextField(
                            value = newTagText,
                            onValueChange = { if (it.length <= 10) newTagText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("输入标签名称（最多10字符）") },
                            singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                if (newTagText.isNotEmpty()) {
                                    when (currentTagCategory) {
                                        "personality" -> {
                                            customPersonalityTags =
                                                    customPersonalityTags + newTagText
                                            selectedPersonality = selectedPersonality + newTagText
                                        }
                                        "identity" -> {
                                            customIdentityTags = customIdentityTags + newTagText
                                            selectedIdentity = selectedIdentity + newTagText
                                        }
                                        "aiStyle" -> {
                                            customAiStyleTags = customAiStyleTags + newTagText
                                            selectedAiStyleTags = selectedAiStyleTags + newTagText
                                        }
                                    }
                                }
                                newTagText = ""
                                showCustomTagDialog = false
                            },
                            enabled = newTagText.isNotEmpty()
                    ) { Text("添加") }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                newTagText = ""
                                showCustomTagDialog = false
                            }
                    ) { Text("取消") }
                }
        )
    }

    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { birthDate = it }
                                showDatePicker = false
                            }
                    ) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            DatePicker(
                    state = datePickerState,
                    title = {
                        Text(
                                "选择您的出生日期",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                        )
                    }
            )
        }
    }

    CustomScaffold() { paddingValues ->
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                        text =
                                if (profileName.isNotEmpty()) "配置「$profileName」"
                                else stringResource(id = R.string.preferences_guide_title),
                        style = MaterialTheme.typography.titleMedium
                )
                
                // 显示当前编辑的配置ID（调试用）
                if (profileId.isNotEmpty()) {
                    Text(
                            text = "正在编辑配置ID: $profileId",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 添加说明卡片，提示所有选项都是可选的
            Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "信息",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "以下所有选项均为可选，您可以根据自己的偏好自由填写。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 性别选择（标签选择）
            Text("性别 (可选)", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genderOptions.forEach { option ->
                    FilterChip(
                            selected = selectedGender == option,
                            onClick = { selectedGender = option },
                            label = { Text(option) },
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // 职业选择（标签选择）
            Text(
                    "职业 (可选)",
                    style = MaterialTheme.typography.titleSmall
            )
            FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                occupationOptions.forEach { option ->
                    FilterChip(
                            selected = selectedOccupation == option,
                            onClick = { selectedOccupation = option },
                            label = { Text(option) },
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // 出生日期选择
            Text(
                    "出生日期 (可选)",
                    style = MaterialTheme.typography.titleSmall
            )
            OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { showDatePicker = true }
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                            text =
                                    if (birthDate > 0) dateFormatter.format(Date(birthDate))
                                    else "请选择出生日期",
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                    if (birthDate > 0) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "选择日期",
                            tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 性格特点选择（多选标签）
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text("性格特点 (可选，可多选)", style = MaterialTheme.typography.titleSmall)
                TextButton(
                        onClick = {
                            currentTagCategory = "personality"
                            showCustomTagDialog = true
                        }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                    Text("添加自定义")
                }
            }
            FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 预定义标签
                personalityOptions.forEach { option ->
                    val isSelected = selectedPersonality.contains(option)
                    FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedPersonality = selectedPersonality - option
                                } else {
                                    selectedPersonality = selectedPersonality + option
                                }
                            },
                            label = { Text(option) },
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 自定义标签
                customPersonalityTags.forEach { tag ->
                    FilterChip(
                            selected = selectedPersonality.contains(tag),
                            onClick = {
                                if (selectedPersonality.contains(tag)) {
                                    selectedPersonality = selectedPersonality - tag
                                } else {
                                    selectedPersonality = selectedPersonality + tag
                                }
                            },
                            label = { Text(tag) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            customPersonalityTags = customPersonalityTags - tag
                                            selectedPersonality = selectedPersonality - tag
                                        }
                                )
                            }
                    )
                }
            }

            // 身份认同选择（多选标签）
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text("身份认同 (可选，可多选)", style = MaterialTheme.typography.titleSmall)
                TextButton(
                        onClick = {
                            currentTagCategory = "identity"
                            showCustomTagDialog = true
                        }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                    Text("添加自定义")
                }
            }
            FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 预定义标签
                identityOptions.forEach { option ->
                    val isSelected = selectedIdentity.contains(option)
                    FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedIdentity = selectedIdentity - option
                                } else {
                                    selectedIdentity = selectedIdentity + option
                                }
                            },
                            label = { Text(option) },
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 自定义标签
                customIdentityTags.forEach { tag ->
                    FilterChip(
                            selected = selectedIdentity.contains(tag),
                            onClick = {
                                if (selectedIdentity.contains(tag)) {
                                    selectedIdentity = selectedIdentity - tag
                                } else {
                                    selectedIdentity = selectedIdentity + tag
                                }
                            },
                            label = { Text(tag) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            customIdentityTags = customIdentityTags - tag
                                            selectedIdentity = selectedIdentity - tag
                                        }
                                )
                            }
                    )
                }
            }

            // AI风格标签选择
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text("期待的AI风格 (可选，可多选)", style = MaterialTheme.typography.titleSmall)
                TextButton(
                        onClick = {
                            currentTagCategory = "aiStyle"
                            showCustomTagDialog = true
                        }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                    Text("添加自定义")
                }
            }
            FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 预定义标签
                aiStyleOptions.forEach { option ->
                    val isSelected = selectedAiStyleTags.contains(option)
                    FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedAiStyleTags = selectedAiStyleTags - option
                                } else {
                                    selectedAiStyleTags = selectedAiStyleTags + option
                                }
                            },
                            label = { Text(option) },
                            modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 自定义标签
                customAiStyleTags.forEach { tag ->
                    FilterChip(
                            selected = selectedAiStyleTags.contains(tag),
                            onClick = {
                                if (selectedAiStyleTags.contains(tag)) {
                                    selectedAiStyleTags = selectedAiStyleTags - tag
                                } else {
                                    selectedAiStyleTags = selectedAiStyleTags + tag
                                }
                            },
                            label = { Text(tag) },
                            modifier = Modifier.padding(vertical = 4.dp),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            customAiStyleTags = customAiStyleTags - tag
                                            selectedAiStyleTags = selectedAiStyleTags - tag
                                        }
                                )
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 完成按钮
            Button(
                    onClick = {
                        scope.launch {
                            // 将选中的标签合并为字符串（包括自定义标签）
                            val personalityTags = selectedPersonality.joinToString(", ")
                            val identityTags = selectedIdentity.joinToString(", ")
                            val aiStyleTags = selectedAiStyleTags.joinToString(", ")

                            // 更新配置信息
                            if (profileId.isNotEmpty()) {
                                // 如果提供了profileId，更新指定的配置文件
                                preferencesManager.updateProfileCategory(
                                        profileId = profileId,
                                        birthDate = birthDate,
                                        gender = selectedGender,
                                        occupation = selectedOccupation,
                                        personality = personalityTags,
                                        identity = identityTags,
                                        aiStyle = aiStyleTags
                                )
                            } else {
                                // 否则更新当前活动的配置文件
                                preferencesManager.updateProfileCategory(
                                        birthDate = birthDate,
                                        gender = selectedGender,
                                        occupation = selectedOccupation,
                                        personality = personalityTags,
                                        identity = identityTags,
                                        aiStyle = aiStyleTags
                                )
                            }

                            // 根据流程选择不同的导航目标
                            if (profileName.isNotEmpty()) {
                                // 如果是从配置页来的，返回到配置页
                                onComplete()
                            } else {
                                // 如果是首次启动应用，进入权限页
                                navigateToPermissions()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(id = R.string.complete)) }
        }
    }
}
