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
import com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.EditingEdge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEdgeDialog(
    editingEdge: EditingEdge,
    packageNodes: List<UINode>,
    onSave: (EditingEdge) -> Unit,
    onDismiss: () -> Unit
) {
    var toNodeName by remember(editingEdge) { mutableStateOf(editingEdge.toNodeName) }
    var showToNodeDropdown by remember { mutableStateOf(false) }

    var operations by remember(editingEdge) { mutableStateOf(editingEdge.operations) }
    var validation by remember(editingEdge) { mutableStateOf(editingEdge.validation?.let { listOf(it) } ?: emptyList()) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingEdge.originalEdge == null) "添加边" else "编辑边")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "从节点: ${editingEdge.fromNodeName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                // 目标节点选择
                ExposedDropdownMenuBox(
                    expanded = showToNodeDropdown,
                    onExpandedChange = {
                        showToNodeDropdown = !showToNodeDropdown
                    }
                ) {
                    OutlinedTextField(
                        value = toNodeName,
                        onValueChange = { },
                        label = { Text("目标节点") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToNodeDropdown)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = showToNodeDropdown,
                        onDismissRequest = { showToNodeDropdown = false }
                    ) {
                        packageNodes
                            .filter { it.name != editingEdge.fromNodeName }
                            .forEach { node ->
                                DropdownMenuItem(
                                    text = { Text(node.name) },
                                    onClick = {
                                        toNodeName = node.name
                                        showToNodeDropdown = false
                                    }
                                )
                            }
                    }
                }

                // 操作配置
                Text(
                    text = "操作序列",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OperationEditor(
                    operations = operations,
                    onOperationsChange = { newOperations ->
                        operations = newOperations
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "验证步骤 (可选)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OperationEditor(
                    operations = validation,
                    onOperationsChange = { newValidation ->
                        // Validation should only have one or zero items
                        validation = newValidation.filterIsInstance<UIOperation.ValidateElement>().take(1)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedEdge = editingEdge.copy(
                        toNodeName = toNodeName,
                        operations = operations,
                        validation = validation.firstOrNull()
                    )
                    onSave(updatedEdge)
                },
                enabled = toNodeName.isNotBlank() && operations.isNotEmpty()
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
