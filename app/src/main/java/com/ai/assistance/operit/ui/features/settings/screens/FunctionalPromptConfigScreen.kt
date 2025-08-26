package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ai.assistance.operit.ui.components.CustomScaffold
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.model.PromptProfile
import com.ai.assistance.operit.data.preferences.FunctionalPromptManager
import com.ai.assistance.operit.data.preferences.PromptFunctionType
import com.ai.assistance.operit.data.preferences.PromptPreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionalPromptConfigScreen(
        onBackPressed: () -> Unit = {},
        onNavigateToModelPrompts: () -> Unit = {}
) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // 配置管理器
        val functionalPromptManager = remember { FunctionalPromptManager(context) }
        val promptPreferencesManager = remember { PromptPreferencesManager(context) }

        // 配置映射状态
        val promptMapping =
                functionalPromptManager.functionPromptMappingFlow.collectAsState(
                        initial = emptyMap()
                )

        // 提示词配置列表
        val profileList =
                promptPreferencesManager.profileListFlow.collectAsState(initial = listOf("default"))
                        .value

        // 配置摘要列表
        var promptProfiles by remember { mutableStateOf<List<PromptProfile>>(emptyList()) }

        // UI状态
        var isLoading by remember { mutableStateOf(true) }
        var showSaveSuccess by remember { mutableStateOf(false) }

        // 加载提示词配置信息
        LaunchedEffect(profileList) {
                isLoading = true
                val profiles = mutableListOf<PromptProfile>()
                for (profileId in profileList) {
                        val profile =
                                promptPreferencesManager.getPromptProfileFlow(profileId).first()
                        profiles.add(profile)
                }
                promptProfiles = profiles
                isLoading = false
        }

        CustomScaffold() { paddingValues ->
                if (isLoading) {
                        Box(
                                modifier = Modifier.fillMaxSize().padding(paddingValues),
                                contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                } else {
                        LazyColumn(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(horizontal = 16.dp)
                        ) {
                                item {
                                        Card(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(vertical = 8.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors =
                                                        CardDefaults.cardColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .surfaceVariant
                                                                                .copy(alpha = 0.7f)
                                                        )
                                        ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                bottom = 8.dp
                                                                        )
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default
                                                                                        .Message,
                                                                        contentDescription = null,
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        modifier =
                                                                                Modifier.size(24.dp)
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(8.dp)
                                                                )
                                                                Text(
                                                                        text = stringResource(R.string.functional_prompt_title),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleMedium,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }

                                                        Text(
                                                                text = stringResource(R.string.functional_prompt_desc),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                bottom = 8.dp
                                                                        )
                                                        )

                                                                                                Text(
                                                text = stringResource(id = R.string.advanced_config_notice),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                                        Divider(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                vertical = 8.dp
                                                                        ),
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .outline.copy(
                                                                                        alpha = 0.2f
                                                                                )
                                                        )

                                                        Row(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .clickable {
                                                                                        onNavigateToModelPrompts()
                                                                                }
                                                                                .padding(
                                                                                        vertical =
                                                                                                4.dp
                                                                                ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.SpaceBetween
                                                        ) {
                                                                Text(
                                                                        text = stringResource(R.string.manage_all_prompts),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        fontWeight =
                                                                                FontWeight.Medium
                                                                )
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.AutoMirrored
                                                                                        .Filled
                                                                                        .ArrowForward,
                                                                        contentDescription = stringResource(R.string.manage_all_prompts),
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                        }
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                }

                                // 功能类型列表
                                items(PromptFunctionType.values()) { functionType ->
                                        val currentProfileId =
                                                promptMapping.value[functionType]
                                                        ?: FunctionalPromptManager
                                                                .getDefaultProfileIdForFunction(
                                                                        functionType
                                                                )
                                        val currentProfile =
                                                promptProfiles.find { it.id == currentProfileId }

                                        FunctionPromptCard(
                                                functionType = functionType,
                                                currentProfile = currentProfile,
                                                availableProfiles = promptProfiles,
                                                onProfileSelected = { profileId ->
                                                        scope.launch {
                                                                functionalPromptManager
                                                                        .setPromptProfileForFunction(
                                                                                functionType,
                                                                                profileId
                                                                        )
                                                                showSaveSuccess = true
                                                        }
                                                }
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                }

                                item {
                                        // 重置按钮
                                        OutlinedButton(
                                                onClick = {
                                                        scope.launch {
                                                                functionalPromptManager
                                                                        .resetAllFunctionPrompts()
                                                                showSaveSuccess = true
                                                        }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                border =
                                                        BorderStroke(
                                                                1.dp,
                                                                MaterialTheme.colorScheme.outline
                                                        )
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(stringResource(R.string.reset_all_functions))
                                        }

                                        // 成功提示
                                        AnimatedVisibility(
                                                visible = showSaveSuccess,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                        ) {
                                                Card(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 16.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors =
                                                                CardDefaults.cardColors(
                                                                        containerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primaryContainer
                                                                )
                                                ) {
                                                        Row(
                                                                modifier = Modifier.padding(16.dp),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(8.dp)
                                                                )
                                                                Text(
                                                                        text = stringResource(R.string.config_saved),
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                }

                                                LaunchedEffect(showSaveSuccess) {
                                                        kotlinx.coroutines.delay(2000)
                                                        showSaveSuccess = false
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun FunctionPromptCard(
        functionType: PromptFunctionType,
        currentProfile: PromptProfile?,
        availableProfiles: List<PromptProfile>,
        onProfileSelected: (String) -> Unit
) {
        var expanded by remember { mutableStateOf(false) }
        val context = LocalContext.current

        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border =
                        BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
        ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                        // 功能标题和描述
                        Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                        text = getFunctionDisplayName(functionType, context),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )

                                Text(
                                        text = getFunctionDescription(functionType, context),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 当前配置
                                Surface(
                                        modifier =
                                                Modifier.fillMaxWidth().clickable {
                                                        expanded = !expanded
                                                },
                                        shape = RoundedCornerShape(8.dp),
                                        color =
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                        alpha = 0.5f
                                                )
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Column {
                                                        Text(
                                                                text = stringResource(
                                                                    R.string.current_prompt_config, 
                                                                    currentProfile?.name ?: stringResource(R.string.unnamed_config)
                                                                ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodyMedium,
                                                                fontWeight = FontWeight.Medium
                                                        )

                                                        if (currentProfile != null) {
                                                                Text(
                                                                        text = stringResource(
                                                                            R.string.intro_preview, 
                                                                            currentProfile.introPrompt.take(30)
                                                                        ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                )
                                                        }
                                                }

                                                Icon(
                                                        imageVector =
                                                                if (expanded)
                                                                        Icons.Default
                                                                                .KeyboardArrowUp
                                                                else
                                                                        Icons.Default
                                                                                .KeyboardArrowDown,
                                                        contentDescription = stringResource(R.string.expand),
                                                        tint =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant
                                                )
                                        }
                                }
                        }

                        // 配置列表
                        AnimatedVisibility(visible = expanded) {
                                Column(
                                        modifier =
                                                Modifier.padding(
                                                        horizontal = 16.dp,
                                                        vertical = 8.dp
                                                )
                                ) {
                                        Text(
                                                text = stringResource(R.string.select_prompt_config_title),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        availableProfiles.forEach { profile ->
                                                val isSelected =
                                                        profile.id ==
                                                                (currentProfile?.id
                                                                        ?: FunctionalPromptManager
                                                                                .getDefaultProfileIdForFunction(
                                                                                        functionType
                                                                                ))

                                                Surface(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 4.dp)
                                                                        .clickable {
                                                                                onProfileSelected(
                                                                                        profile.id
                                                                                )
                                                                                expanded = false
                                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        color =
                                                                if (isSelected)
                                                                        MaterialTheme.colorScheme
                                                                                .primaryContainer
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .surface,
                                                        border =
                                                                BorderStroke(
                                                                        width =
                                                                                if (isSelected) 0.dp
                                                                                else 0.5.dp,
                                                                        color =
                                                                                if (isSelected)
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary
                                                                                else
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .outlineVariant
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.5f
                                                                                                )
                                                                )
                                                ) {
                                                        Row(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(12.dp),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                if (isSelected) {
                                                                        Icon(
                                                                                imageVector =
                                                                                        Icons.Default
                                                                                                .Check,
                                                                                contentDescription = stringResource(R.string.status_granted),
                                                                                tint =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary,
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                16.dp
                                                                                        )
                                                                        )
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                8.dp
                                                                                        )
                                                                        )
                                                                }

                                                                Column(
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                ) {
                                                                        Text(
                                                                                text = profile.name,
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium,
                                                                                fontWeight =
                                                                                        if (isSelected
                                                                                        )
                                                                                                FontWeight
                                                                                                        .Bold
                                                                                        else
                                                                                                FontWeight
                                                                                                        .Normal,
                                                                                color =
                                                                                        if (isSelected
                                                                                        )
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primary
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                        )

                                                                        Text(
                                                                                text = stringResource(
                                                                                    R.string.intro_preview, 
                                                                                    profile.introPrompt.take(30)
                                                                                ),
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodySmall,
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .onSurfaceVariant
                                                                        )
                                                                }
                                                        }
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                }
                        }

                        Divider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f)
                        )
                }
        }
}

// 获取功能类型的显示名称
fun getFunctionDisplayName(functionType: PromptFunctionType, context: android.content.Context): String {
        return when (functionType) {
                PromptFunctionType.CHAT -> context.getString(R.string.chat_function)
                PromptFunctionType.VOICE -> context.getString(R.string.voice_function)
                PromptFunctionType.DESKTOP_PET -> context.getString(R.string.desktop_pet_function)
        }
}

// 获取功能类型的描述
fun getFunctionDescription(functionType: PromptFunctionType, context: android.content.Context): String {
        return when (functionType) {
                PromptFunctionType.CHAT -> context.getString(R.string.chat_function_desc)
                PromptFunctionType.VOICE -> context.getString(R.string.voice_function_desc)
                PromptFunctionType.DESKTOP_PET -> context.getString(R.string.desktop_pet_function_desc)
        }
}
