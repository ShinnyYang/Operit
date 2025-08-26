package com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.core.tools.automatic.UIOperation
import com.ai.assistance.operit.core.tools.automatic.UISelector
import com.ai.assistance.operit.core.tools.automatic.UINode
import com.ai.assistance.operit.core.tools.automatic.UIFunction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFunctionDialog(
    function: UIFunction?,
    packageNodes: List<UINode>,
    onSave: (String, String, String, UIOperation) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(function) { mutableStateOf(function?.name ?: "") }
    var description by remember(function) { mutableStateOf(function?.description ?: "") }
    var targetNodeName by remember(function) { mutableStateOf(function?.targetNodeName ?: "") }
    var showTargetNodeDropdown by remember { mutableStateOf(false) }

    var operations by remember(function) {
        val initialOps = function?.operation
        val opList = if (initialOps is UIOperation.Sequential) {
            initialOps.operations
        } else if (initialOps != null) {
            listOf(initialOps)
        } else {
            emptyList()
        }
        mutableStateOf(opList)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (function == null) "添加功能" else "编辑功能")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("功能名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("功能描述") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // 目标节点选择
                ExposedDropdownMenuBox(
                    expanded = showTargetNodeDropdown,
                    onExpandedChange = {
                        showTargetNodeDropdown = !showTargetNodeDropdown
                    }
                ) {
                    OutlinedTextField(
                        value = targetNodeName,
                        onValueChange = { },
                        label = { Text("目标节点") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTargetNodeDropdown)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = showTargetNodeDropdown,
                        onDismissRequest = { showTargetNodeDropdown = false }
                    ) {
                        packageNodes.forEach { node ->
                            DropdownMenuItem(
                                text = { Text(node.name) },
                                onClick = {
                                    targetNodeName = node.name
                                    showTargetNodeDropdown = false
                                }
                            )
                        }
                    }
                }

                // 操作配置
                Text(
                    text = "操作配置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OperationEditor(
                    operations = operations,
                    onOperationsChange = { newOperations ->
                        operations = newOperations
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalOperation = when {
                        operations.isEmpty() -> UIOperation.Sequential(emptyList(), "Empty function operation")
                        operations.size == 1 -> operations.first()
                        else -> {
                            // Let's generate a description for the sequential operation
                            val opsSummary = operations.take(2).joinToString(", ") { it.description.take(10) }
                            val fullDescription = if (operations.size > 2) "$opsSummary..." else opsSummary
                            UIOperation.Sequential(operations, "Sequential: $fullDescription")
                        }
                    }
                    onSave(name, description, targetNodeName, finalOperation)
                },
                enabled = name.isNotBlank() && description.isNotBlank() && targetNodeName.isNotBlank() && operations.isNotEmpty()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
} 