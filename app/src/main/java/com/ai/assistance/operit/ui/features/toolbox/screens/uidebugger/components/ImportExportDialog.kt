package com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.assistance.operit.core.tools.automatic.UIEdgeDefinition
import com.ai.assistance.operit.core.tools.automatic.UIFunction
import com.ai.assistance.operit.core.tools.automatic.UINode
import com.ai.assistance.operit.core.tools.automatic.config.AutomationPackageInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ai.assistance.operit.core.tools.automatic.UINodeType
import com.ai.assistance.operit.core.tools.automatic.UIOperation
import com.ai.assistance.operit.core.tools.automatic.UISelector
import androidx.compose.ui.text.input.KeyboardType
import com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.ExportPackageItem
import com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.ImportExportMode

@Composable
fun ImportExportDialog(
    mode: ImportExportMode,
    builtInPackages: List<AutomationPackageInfo>,
    externalPackages: List<AutomationPackageInfo>,
    isLoading: Boolean,
    onModeChange: (ImportExportMode) -> Unit,
    onPackageSelected: (AutomationPackageInfo) -> Unit,
    onImportFromFile: () -> Unit,
    onExportPackage: (AutomationPackageInfo) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (mode == ImportExportMode.IMPORT) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (mode == ImportExportMode.IMPORT) "导入配置" else "导出配置")
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onRefresh) {
                        Text("刷新")
                    }
                }
                
                // 模式切换
                Row(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    FilterChip(
                        selected = mode == ImportExportMode.IMPORT,
                        onClick = { onModeChange(ImportExportMode.IMPORT) },
                        label = { Text("导入") },
                        leadingIcon = { 
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == ImportExportMode.EXPORT,
                        onClick = { onModeChange(ImportExportMode.EXPORT) },
                        label = { Text("导出") },
                        leadingIcon = { 
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (mode) {
                    ImportExportMode.IMPORT -> {
                        Column {
                            // 从文件导入按钮
                            OutlinedButton(
                                onClick = onImportFromFile,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("从文件导入")
                            }
                            
                            if (builtInPackages.isNotEmpty() || externalPackages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "或选择内置配置包",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp)
                                ) {
                                    if (builtInPackages.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "内置配置",
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        items(builtInPackages) { packageInfo ->
                                            PackageItem(
                                                packageInfo = packageInfo,
                                                onClick = { onPackageSelected(packageInfo) }
                                            )
                                        }
                                    }

                                    if (externalPackages.isNotEmpty()) {
                                        item {
                                            Text(
                                                text = "外部配置",
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        items(externalPackages) { packageInfo ->
                                            PackageItem(
                                                packageInfo = packageInfo,
                                                onClick = { onPackageSelected(packageInfo) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("没有找到可用的配置包")
                            }
                        }
                    }
                    ImportExportMode.EXPORT -> {
                        if (builtInPackages.isEmpty() && externalPackages.isEmpty()) {
                            Text("没有找到可导出的配置包")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                            ) {
                                if (builtInPackages.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "内置配置",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(builtInPackages) { packageInfo ->
                                        ExportPackageItem(
                                            packageInfo = packageInfo,
                                            onExport = { onExportPackage(packageInfo) }
                                        )
                                    }
                                }

                                if (externalPackages.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "外部配置",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    items(externalPackages) { packageInfo ->
                                        ExportPackageItem(
                                            packageInfo = packageInfo,
                                            onExport = { onExportPackage(packageInfo) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun PackageItem(
    packageInfo: AutomationPackageInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (packageInfo.isBuiltIn) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (packageInfo.isBuiltIn) Icons.Default.Description else Icons.Default.Settings,
                contentDescription = null,
                tint = if (packageInfo.isBuiltIn) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageInfo.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (packageInfo.isBuiltIn) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = packageInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (packageInfo.isBuiltIn) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (packageInfo.description.isNotBlank()) {
                    Text(
                        text = packageInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (packageInfo.isBuiltIn) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Text(
                text = if (packageInfo.isBuiltIn) "内置" else "外部",
                style = MaterialTheme.typography.labelSmall,
                color = if (packageInfo.isBuiltIn) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
