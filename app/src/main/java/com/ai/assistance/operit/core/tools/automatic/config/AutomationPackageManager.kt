package com.ai.assistance.operit.core.tools.automatic.config

import android.content.Context
import android.util.Log
import com.ai.assistance.operit.core.tools.automatic.JsonUIEdge
import com.ai.assistance.operit.core.tools.automatic.JsonUIFunction
import com.ai.assistance.operit.core.tools.automatic.JsonUINode
import com.ai.assistance.operit.core.tools.automatic.JsonUIOperation
import com.ai.assistance.operit.core.tools.automatic.UIRouteConfig
import com.ai.assistance.operit.core.tools.automatic.JsonUIRouteConfig
import com.ai.assistance.operit.core.tools.automatic.JsonUISelector
import com.ai.assistance.operit.core.tools.automatic.UIOperation
import com.ai.assistance.operit.core.tools.automatic.UISelector
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.IOException
import java.io.FileOutputStream
import android.net.Uri

/**
 * Manages UI automation configuration packages.
 *
 * This manager handles loading, importing, and accessing automation configurations
 * (defined in .json files) from both internal assets and external user storage.
 * It allows for a modular and extensible system for defining app-specific automation flows.
 *
 * Package Sources:
 * 1.  **Assets**: Built-in packages bundled with the application.
 * 2.  **External Storage**: User-imported packages for custom automation.
 */
class AutomationPackageManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AutomationPackageManager"
        private const val CONFIG_DIR_NAME = "automation_configs"
        
        @Volatile private var INSTANCE: AutomationPackageManager? = null

        fun getInstance(context: Context): AutomationPackageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutomationPackageManager(context.applicationContext).also {
                    INSTANCE = it
                    it.loadAllPackages()
                }
            }
        }
    }

    // A map to hold metadata of all available packages (name to description).
    private val availablePackageInfo = mutableMapOf<String, AutomationPackageInfo>()

    // The directory where user-imported configuration files are stored.
    private val externalConfigsDir: File by lazy {
        val dir = File(context.getExternalFilesDir(null), CONFIG_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
            Log.d(TAG, "Created external configs directory at: ${dir.absolutePath}")
        }
        dir
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Loads all automation packages from both assets and external storage.
     */
    private fun loadAllPackages() {
        Log.d(TAG, "Loading all automation packages...")
        availablePackageInfo.clear()
        loadPackagesFromAssets()
        loadPackagesFromExternalStorage()
        Log.d(TAG, "Finished loading. Total packages available: ${availablePackageInfo.size}")
    }

    /**
     * Scans the "automation_configs" directory in assets and loads package info.
     */
    private fun loadPackagesFromAssets() {
        try {
            val assetManager = context.assets
            val packageFiles = assetManager.list(CONFIG_DIR_NAME) ?: return

            packageFiles.filter { it.endsWith(".json") }.forEach { fileName ->
                val filePath = "$CONFIG_DIR_NAME/$fileName"
                try {
                    val jsonString = assetManager.open(filePath).bufferedReader().use { it.readText() }
                    val jsonConfig = json.decodeFromString<JsonUIRouteConfig>(jsonString)
                    val info = AutomationPackageInfo(
                        name = jsonConfig.appName,
                        packageName = jsonConfig.packageName,
                        description = jsonConfig.description,
                        isBuiltIn = true,
                        fileName = fileName
                    )
                    availablePackageInfo[info.name] = info
                    Log.d(TAG, "Loaded built-in package: ${info.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load or parse asset package: $fileName", e)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error accessing assets directory for automation packages.", e)
        }
    }

    /**
     * Scans the external storage directory and loads package info.
     */
    private fun loadPackagesFromExternalStorage() {
        if (!externalConfigsDir.exists() || !externalConfigsDir.isDirectory) return

        externalConfigsDir.listFiles { _, name -> name.endsWith(".json") }?.forEach { file ->
            try {
                val jsonString = file.readText()
                val jsonConfig = json.decodeFromString<JsonUIRouteConfig>(jsonString)
                val info = AutomationPackageInfo(
                    name = jsonConfig.appName,
                    packageName = jsonConfig.packageName,
                    description = jsonConfig.description,
                    isBuiltIn = false,
                    fileName = file.name
                )
                // User-imported packages can override built-in ones with the same name.
                availablePackageInfo[info.name] = info
                Log.d(TAG, "Loaded external package: ${info.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load or parse external package: ${file.name}", e)
            }
        }
    }
    
    /**
     * Retrieves a list of all available automation packages' metadata.
     * This is intended for displaying package options to the user.
     *
     * @return A list of [AutomationPackageInfo] objects.
     */
    fun getAllPackageInfo(): List<AutomationPackageInfo> {
        return availablePackageInfo.values.toList()
    }

    /**
     * Finds and loads a UIRouteConfig based on the application's package name.
     * It prioritizes user-imported packages over built-in ones if duplicates exist.
     *
     * @param appPackageName The package name of the target application (e.g., "com.example.app").
     * @return The corresponding [UIRouteConfig], or null if no matching configuration is found.
     */
    fun getConfigByAppPackageName(appPackageName: String): UIRouteConfig? {
        // Find the package info that matches the app package name, prioritizing external packages.
        val packageInfo = availablePackageInfo.values
            .sortedBy { it.isBuiltIn } // false (external) comes before true (built-in)
            .find { it.packageName == appPackageName }

        if (packageInfo == null) {
            Log.w(TAG, "No automation config found for app package name: $appPackageName")
            return null
        }

        // Use the private loader function to get the full config.
        return loadConfig(packageInfo.name)
    }

    /**
     * Loads the full UIRouteConfig for a given package name.
     * This is a private helper function.
     *
     * @param appName The user-facing name of the package to load.
     * @return The [UIRouteConfig] object, or null if the package is not found or fails to load.
     */
    private fun loadConfig(appName: String): UIRouteConfig? {
        val packageInfo = availablePackageInfo[appName]
        if (packageInfo == null) {
            Log.w(TAG, "Attempted to load a non-existent package by name: $appName")
            return null
        }

        try {
            val jsonString = if (packageInfo.isBuiltIn) {
                val filePath = "$CONFIG_DIR_NAME/${packageInfo.fileName}"
                context.assets.open(filePath).bufferedReader().use { it.readText() }
            } else {
                File(externalConfigsDir, packageInfo.fileName).readText()
            }
            return UIRouteConfig.loadFromJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading config for package: $appName", e)
            return null
        }
    }

    /**
     * Imports a new automation package from a file path into the app's external storage.
     *
     * @param filePath The absolute path to the .json configuration file.
     * @return A string message indicating success or failure.
     */
    fun importPackage(uriString: String): String {
        try {
            val uri = Uri.parse(uriString)
            // 从URI获取文件名
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val fileName = cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) it.getString(displayNameIndex) else "imported_config.json"
                } else {
                    "imported_config.json"
                }
            } ?: "imported_config.json"

            if (!fileName.endsWith(".json")) {
                return "Error: Only .json files are supported for import."
            }

            val destFile = File(externalConfigsDir, fileName)
            if (destFile.exists()) {
                Log.w(TAG, "Overwriting existing package: $fileName")
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IOException("Failed to open input stream for URI: $uriString")

            Log.d(TAG, "Successfully imported package to: ${destFile.absolutePath}")

            // Reload packages to include the new one.
            loadAllPackages()
            return "Successfully imported package: $fileName"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import package from: $uriString", e)
            return "Error: Failed to import package. ${e.message}"
        }
    }

    /**
     * Exports a package to the specified output path.
     *
     * @param packageInfo The package to export.
     * @param uriString The URI string representing the destination file.
     * @return A string message indicating success or failure.
     */
    fun exportPackage(packageInfo: AutomationPackageInfo, uriString: String): String {
        try {
            val jsonString = if (packageInfo.isBuiltIn) {
                val filePath = "$CONFIG_DIR_NAME/${packageInfo.fileName}"
                context.assets.open(filePath).bufferedReader().use { it.readText() }
            } else {
                val configFile = File(externalConfigsDir, packageInfo.fileName)
                if (!configFile.exists()) {
                    // 如果外部文件不存在，尝试从内存中的config生成
                    val config = getConfigByAppPackageName(packageInfo.packageName)
                    if (config != null) {
                        val jsonConfig = convertToJsonConfig(config, packageInfo)
                        json.encodeToString(jsonConfig)
                    } else {
                        throw IOException("Cannot find config file for external package: ${packageInfo.fileName}")
                    }
                } else {
                    configFile.readText()
                }
            }

            val uri = Uri.parse(uriString)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: throw IOException("Failed to open output stream for URI: $uriString")

            Log.d(TAG, "Successfully exported package to: $uriString")
            return "Successfully exported package to: $uriString"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export package: ${packageInfo.name}", e)
            return "Error: Failed to export package. ${e.message}"
        }
    }

    /**
     * Deletes a user-imported package. Built-in packages cannot be deleted.
     *
     * @param packageName The name of the package to delete.
     * @return True if deletion was successful, false otherwise.
     */
    fun deletePackage(packageName: String): Boolean {
        val packageInfo = availablePackageInfo[packageName]
        if (packageInfo == null) {
            Log.w(TAG, "Cannot delete non-existent package: $packageName")
            return false
        }
        if (packageInfo.isBuiltIn) {
            Log.w(TAG, "Cannot delete a built-in package: $packageName")
            return false
        }

        val fileToDelete = File(externalConfigsDir, packageInfo.fileName)
        return try {
            if (fileToDelete.delete()) {
                Log.d(TAG, "Successfully deleted package: $packageName")
                loadAllPackages() // Refresh the package list
                true
            } else {
                Log.e(TAG, "Failed to delete package file: ${fileToDelete.absolutePath}")
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error while deleting package: $packageName", e)
            false
        }
    }

    /**
     * 保存UIRouteConfig到文件
     * @param config 要保存的UIRouteConfig
     * @param packageInfo 包信息
     * @return 保存结果消息
     */
    fun saveConfig(config: UIRouteConfig, packageInfo: AutomationPackageInfo): String {
        try {
            // 转换UIRouteConfig为JsonUIRouteConfig
            val jsonConfig = convertToJsonConfig(config, packageInfo)
            val jsonString = json.encodeToString(jsonConfig)
            
            val file = File(externalConfigsDir, packageInfo.fileName)
            file.writeText(jsonString)
            
            // 重新加载所有包以更新缓存
            loadAllPackages()
            
            Log.d(TAG, "Successfully saved config for: ${packageInfo.name}")
            return "配置保存成功"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config for: ${packageInfo.name}", e)
            return "保存失败: ${e.message}"
        }
    }

    /**
     * 创建新的配置包
     * @param appName 应用名称
     * @param packageName 包名
     * @param description 描述
     * @return 新创建的配置包信息
     */
    fun createNewPackage(appName: String, packageName: String, description: String): AutomationPackageInfo {
        val fileName = "${appName.replace(" ", "_")}_${System.currentTimeMillis()}.json"
        val packageInfo = AutomationPackageInfo(
            name = appName,
            packageName = packageName,
            description = description,
            isBuiltIn = false,
            fileName = fileName
        )
        
        // 创建空的配置
        val emptyConfig = UIRouteConfig()
        saveConfig(emptyConfig, packageInfo)
        
        return packageInfo
    }

    /**
     * 将UIRouteConfig转换为JsonUIRouteConfig
     */
    private fun convertToJsonConfig(config: UIRouteConfig, packageInfo: AutomationPackageInfo): JsonUIRouteConfig {
        val jsonNodes = config.nodeDefinitions.values.map { node ->
            JsonUINode(
                name = node.name,
                description = node.name, // 可以后续改进添加专门的description字段
                activityName = node.activityName,
                nodeType = node.nodeType.name,
                matchCriteria = node.matchCriteria.map { convertToJsonSelector(it) }
            )
        }
        
        val jsonEdges = mutableListOf<JsonUIEdge>()
        config.edgeDefinitions.forEach { (from, edges) ->
            edges.forEach { edge ->
                jsonEdges.add(
                    JsonUIEdge(
                    from = from,
                    to = edge.toNodeName,
                    operations = edge.operations.map { convertToJsonOperation(it) },
                    validation = edge.validation?.let { convertToJsonOperation(it) },
                    conditions = edge.conditions,
                    weight = edge.weight
                )
                )
            }
        }
        
        val jsonFunctions = config.functionDefinitions.values.map { function ->
            JsonUIFunction(
                name = function.name,
                description = function.description,
                targetNodeName = function.targetNodeName,
                operation = convertToJsonOperation(function.operation)
            )
        }
        
        return JsonUIRouteConfig(
            appName = packageInfo.name,
            packageName = packageInfo.packageName,
            description = packageInfo.description,
            nodes = jsonNodes,
            edges = jsonEdges,
            functions = jsonFunctions
        )
    }

    /**
     * 将UIOperation转换为JsonUIOperation
     */
    private fun convertToJsonOperation(operation: UIOperation): JsonUIOperation {
        return when (operation) {
            is UIOperation.Click -> JsonUIOperation.Click(
                selector = convertToJsonSelector(operation.selector),
                description = operation.description,
                relativeX = operation.relativeX,
                relativeY = operation.relativeY
            )
            is UIOperation.Input -> JsonUIOperation.Input(
                selector = convertToJsonSelector(operation.selector),
                textVariableKey = operation.textVariableKey,
                description = operation.description
            )
            is UIOperation.LaunchApp -> JsonUIOperation.LaunchApp(
                packageName = operation.packageName,
                description = operation.description
            )
            is UIOperation.PressKey -> JsonUIOperation.PressKey(
                keyCode = operation.keyCode,
                description = operation.description
            )
            is UIOperation.Wait -> JsonUIOperation.Wait(
                durationMs = operation.durationMs,
                description = operation.description
            )
            is UIOperation.Sequential -> JsonUIOperation.Sequential(
                operations = operation.operations.map { convertToJsonOperation(it) },
                description = operation.description
            )
            is UIOperation.ValidateElement -> JsonUIOperation.ValidateElement(
                selector = convertToJsonSelector(operation.selector),
                expectedValueKey = operation.expectedValueKey,
                validationType = operation.validationType.name,
                description = operation.description
            )
            // 其他操作类型可以根据需要添加
            else -> JsonUIOperation.Wait(durationMs = 0, description = operation.description)
        }
    }

    /**
     * 将UISelector转换为JsonUISelector
     */
    private fun convertToJsonSelector(selector: UISelector): JsonUISelector {
        return when (selector) {
            is UISelector.ByResourceId -> JsonUISelector(type = "ByResourceId", value = selector.id)
            is UISelector.ByText -> JsonUISelector(type = "ByText", value = selector.text)
            is UISelector.ByContentDesc -> JsonUISelector(type = "ByContentDesc", value = selector.desc)
            is UISelector.ByClassName -> JsonUISelector(type = "ByClassName", value = selector.name)
            is UISelector.ByBounds -> JsonUISelector(type = "ByBounds", value = selector.bounds)
            is UISelector.ByXPath -> JsonUISelector(type = "ByXPath", value = selector.xpath)
            is UISelector.Compound -> JsonUISelector(
                type = "Compound",
                selectors = selector.selectors.map { convertToJsonSelector(it) },
                operator = selector.operator
            )
        }
    }
}

/**
 * Data class holding metadata about an automation package.
 */
data class AutomationPackageInfo(
    val name: String,
    val packageName: String,
    val description: String,
    val isBuiltIn: Boolean,
    val fileName: String
) 