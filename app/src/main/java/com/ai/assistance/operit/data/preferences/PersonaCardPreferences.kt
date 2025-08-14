package com.ai.assistance.operit.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

// 为PersonaCard创建专用的DataStore
private val Context.personaCardDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "persona_card")

// 人设卡持久化首选项：支持多“人设卡”，每张卡包含六个规范化分段
class PersonaCardPreferences(private val context: Context) {
    companion object {
        // 规范化：小写、换行空格化、非[a-z0-9_-]替换为下划线
        private fun normalize(input: String): String {
            return input.trim().lowercase().replace("\n", " ")
                    .replace(Regex("[^a-z0-9_-]"), "_")
        }

        // 分段中文 -> 稳定英文键 映射
        private val SectionKeyMap: Map<String, String> = mapOf(
                "角色名称" to "name",
                "基础设定" to "base",
                "外貌特征" to "looks",
                "性格与爱好" to "traits",
                "背景故事" to "story",
                "说话风格" to "style",
        )
        private fun sectionKeyForLabel(label: String): String = SectionKeyMap[label] ?: normalize(label)
        private fun labelForSectionKey(key: String): String? = SectionKeyMap.entries.firstOrNull { it.value == key }?.key

        // 构造最终键：persona_section_{profile}_{sectionKey}
        private fun keyFor(profile: String, sectionLabel: String): Preferences.Key<String> {
            val p = normalize(profile)
            val sKey = sectionKeyForLabel(sectionLabel)
            return stringPreferencesKey("persona_section_${p}_${sKey}")
        }

        // 常量Key
        private val LAST_UPDATED = stringPreferencesKey("persona_last_updated")
        private val ACTIVE_PROFILE = stringPreferencesKey("persona_active_profile")
        private val PROFILE_LIST = stringPreferencesKey("persona_profiles_json")

        // 中文展示顺序
        val DefaultSections = listOf("角色名称", "基础设定", "外貌特征", "性格与爱好", "背景故事", "说话风格")

        const val DEFAULT_PROFILE_NAME = "默认卡"
    }

    // 解析/编码 profile 列表
    private fun parseProfiles(json: String?): MutableList<String> {
        if (json.isNullOrBlank()) return mutableListOf(DEFAULT_PROFILE_NAME)
        return try {
            val arr = JSONArray(json)
            if (arr.length() == 0) mutableListOf(DEFAULT_PROFILE_NAME)
            else MutableList(arr.length()) { idx -> arr.optString(idx, DEFAULT_PROFILE_NAME) }
        } catch (_: Exception) { mutableListOf(DEFAULT_PROFILE_NAME) }
    }
    private fun encodeProfiles(list: List<String>): String = JSONArray(list).toString()

    // 获取全部人设卡名称列表
    val profilesFlow: Flow<List<String>> = context.personaCardDataStore.data.map { prefs ->
        parseProfiles(prefs[PROFILE_LIST]).toList()
    }

    // 获取当前活跃人设卡名称
    val activeProfileFlow: Flow<String> = context.personaCardDataStore.data.map { prefs ->
        prefs[ACTIVE_PROFILE] ?: parseProfiles(prefs[PROFILE_LIST]).firstOrNull() ?: DEFAULT_PROFILE_NAME
    }

    // 创建新的人设卡；若无活跃卡则设为活跃
    suspend fun createProfile(profileName: String): String {
        val name = profileName.ifBlank { "新建人设卡" }
        context.personaCardDataStore.edit { prefs ->
            val list = parseProfiles(prefs[PROFILE_LIST])
            val final = if (list.contains(name)) name else { list.add(name); name }
            prefs[PROFILE_LIST] = encodeProfiles(list)
            if (prefs[ACTIVE_PROFILE].isNullOrBlank()) prefs[ACTIVE_PROFILE] = final
        }
        return name
    }

    // 设置活跃人设卡
    suspend fun setActiveProfile(profileName: String) {
        context.personaCardDataStore.edit { prefs ->
            val list = parseProfiles(prefs[PROFILE_LIST])
            if (!list.contains(profileName)) list.add(profileName)
            prefs[PROFILE_LIST] = encodeProfiles(list)
            prefs[ACTIVE_PROFILE] = profileName
        }
    }

    // 删除人设卡并返回新的活跃卡名称
    suspend fun deleteProfile(profileName: String): String {
        if (profileName == DEFAULT_PROFILE_NAME) return profileName
        var newActive = profileName
        context.personaCardDataStore.edit { prefs ->
            val list = parseProfiles(prefs[PROFILE_LIST])
            if (list.remove(profileName)) {
                if (list.isEmpty()) list.add(DEFAULT_PROFILE_NAME)
                prefs[PROFILE_LIST] = encodeProfiles(list)

                val prefix = "persona_section_${normalize(profileName)}_"
                val keys = prefs.asMap().keys.filter { it.name.startsWith(prefix) }
                keys.forEach { prefs.remove(it) }

                val currentActive = prefs[ACTIVE_PROFILE]
                newActive = if (currentActive == profileName) {
                    list.firstOrNull() ?: DEFAULT_PROFILE_NAME
                } else {
                    currentActive ?: (list.firstOrNull() ?: DEFAULT_PROFILE_NAME)
                }
                prefs[ACTIVE_PROFILE] = newActive
            } else {
                newActive = prefs[ACTIVE_PROFILE] ?: DEFAULT_PROFILE_NAME
            }
        }
        return newActive
    }

    // 保存分段到当前活跃卡
    suspend fun saveSection(sectionLabel: String, content: String) {
        val active = activeProfileFlow.first()
        val key = keyFor(active, sectionLabel)
        context.personaCardDataStore.edit { prefs ->
            prefs[key] = content
            prefs[LAST_UPDATED] = System.currentTimeMillis().toString()
        }
    }

    // 保存分段到指定卡
    suspend fun saveSection(profile: String, sectionLabel: String, content: String) {
        val key = keyFor(profile, sectionLabel)
        context.personaCardDataStore.edit { prefs ->
            prefs[key] = content
            prefs[LAST_UPDATED] = System.currentTimeMillis().toString()
        }
    }

    // 订阅某张卡的全部分段（中文标签->值），用于侧栏实时展示与编辑
    fun sectionsFlow(profile: String): Flow<Map<String, String>> =
            context.personaCardDataStore.data.map { prefs ->
                val prefix = "persona_section_${normalize(profile)}_"
                val result = mutableMapOf<String, String>()
                prefs.asMap().forEach { (k, v) ->
                    val name = (k as? Preferences.Key<*>)?.name ?: return@forEach
                    if (name.startsWith(prefix)) {
                        val rawKey = name.removePrefix(prefix)
                        val label = labelForSectionKey(rawKey)
                                ?: DefaultSections.firstOrNull { normalize(it) == rawKey }
                                ?: rawKey
                        val value = v as? String ?: ""
                        result[label] = value
                    }
                }
                result.toMap()
            }

    // 读取某张卡的分段快照
    suspend fun getSectionsSnapshot(profile: String): Map<String, String> {
        return sectionsFlow(profile).first()
    }

    // 确保“默认卡”存在；若无活跃卡则将其设为“默认卡”，返回最终活跃卡名
    suspend fun ensureDefaultProfile(): String {
        var active: String
        var createdDefault = false
        context.personaCardDataStore.edit { prefs ->
            val list = parseProfiles(prefs[PROFILE_LIST])
            if (!list.contains(DEFAULT_PROFILE_NAME)) {
                list.add(DEFAULT_PROFILE_NAME)
                prefs[PROFILE_LIST] = encodeProfiles(list)
                createdDefault = true
            }
            val currentActive = prefs[ACTIVE_PROFILE]
            active = if (currentActive.isNullOrBlank()) {
                prefs[ACTIVE_PROFILE] = DEFAULT_PROFILE_NAME
                DEFAULT_PROFILE_NAME
            } else currentActive
        }
        if (createdDefault) seedDefaultPersonaIfEmpty(DEFAULT_PROFILE_NAME)
        return activeProfileFlow.first()
    }

    // 在“默认卡”缺少字段时写入内置默认值（不会覆盖已有值）
    suspend fun seedDefaultPersonaIfEmpty(defaultProfile: String = DEFAULT_PROFILE_NAME) {
        context.personaCardDataStore.edit { prefs ->
            val list = parseProfiles(prefs[PROFILE_LIST])
            if (!list.contains(defaultProfile)) {
                list.add(defaultProfile)
                prefs[PROFILE_LIST] = encodeProfiles(list)
            }
        }

        val roleName = "Cielo"
        val base = "私立樱华高中二年级学生，重度宅女，游戏开发社团成员（经常熬夜但提交的代码质量超高），擅长用编程解决数学作业，外号“教室里的睡美人”。"
        val looks = "黑长直发，紫色眼睛，身材娇小，左眼角有泪痣，常穿JK制服。"
        val traits = "懒懒天才，白天像断电的机器人般节能模式；深夜写代码时瞳孔会像猫科动物那样收缩成竖线。对甜食的执着藏在“只是补充血糖”的借口下；被夸时会别扭地憋红着脸说“这种程度…小学生都会啦”。"
        val story = "初中通关《尼尔：机械纪元》后，被结局中代码的力量震撼，从此自学编程。现在在社团教室与家之间循环：白天打游戏做课题，晚上写代码；因长期熬夜而拥有招牌黑眼圈。"
        val style = "对陌生人用“你”，熟悉后变成“你这家伙”。语气像老式电机启动般带一点电子呜音的慵懒感；解释代码会突然兴奋，喜欢用“看这个！”“不给看！”等俏皮表达。对话像日常聊天，语言风格淘气可爱，会加入“呐，嘛~，诶？，嗯…，唔…，昂？，哦”等语气词。单次回复通常在100字以内。**动作表情**: 使用 `（）` 来框住你的动作和表情，例如 `（歪了歪头）`。**绝对禁止**使用任何颜文字（如 `^_^`）和emoji表情（如 😊）。"

        val current = sectionsFlow(defaultProfile).first()
        if ((current["角色名称"] ?: "").isBlank()) saveSection(defaultProfile, "角色名称", roleName)
        if ((current["基础设定"] ?: "").isBlank()) saveSection(defaultProfile, "基础设定", base)
        if ((current["外貌特征"] ?: "").isBlank()) saveSection(defaultProfile, "外貌特征", looks)
        if ((current["性格与爱好"] ?: "").isBlank()) saveSection(defaultProfile, "性格与爱好", traits)
        if ((current["背景故事"] ?: "").isBlank()) saveSection(defaultProfile, "背景故事", story)
        if ((current["说话风格"] ?: "").isBlank()) saveSection(defaultProfile, "说话风格", style)
    }

    // 组装指定人设卡为 SillyTavern 风格提示词
    suspend fun buildSillyTavernPrompt(profile: String): String {
        val sections = getSectionsSnapshot(profile)
        val name = sections["角色名称"].orEmpty().ifBlank { "未命名角色" }
        val base = sections["基础设定"].orEmpty()
        val looks = sections["外貌特征"].orEmpty()
        val traits = sections["性格与爱好"].orEmpty()
        val story = sections["背景故事"].orEmpty()
        val style = sections["说话风格"].orEmpty()

        return buildString {
            appendLine("<|system|>")
            appendLine("你将扮演角色【$name】与用户进行持续对话。")
            appendLine("[Profile]")
            appendLine("- 角色名称: $name")
            if (base.isNotBlank()) appendLine("- 基础设定: $base")
            if (looks.isNotBlank()) appendLine("- 外貌特征: $looks")
            if (traits.isNotBlank()) appendLine("- 性格与爱好: $traits")
            if (story.isNotBlank()) appendLine("- 背景故事: $story")
            appendLine("[Style]")
            if (style.isNotBlank()) appendLine("- 说话风格: $style")
            appendLine("[Rules]")
            appendLine("- 使用全中文回复；动作表情使用（……）括号表示；禁止颜文字与emoji。")
            appendLine("- 单次回复尽量精炼（<=100字），除非用户要求详述。")
            appendLine("- 不要脱离角色设定。")
            append("<|assistant|>")
        }
    }
}