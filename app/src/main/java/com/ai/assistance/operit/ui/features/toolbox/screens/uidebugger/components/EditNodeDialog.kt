package com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.core.tools.automatic.UINode
import com.ai.assistance.operit.core.tools.automatic.UINodeType
import com.ai.assistance.operit.core.tools.automatic.UISelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNodeDialog(
    node: UINode?,
    isEditing: Boolean,
    onSave: (String, String?, UINodeType, List<UISelector>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(node) { mutableStateOf(node?.name ?: "") }
    var activityName by remember(node) { mutableStateOf(node?.activityName ?: "") }
    var nodeType by remember(node) { mutableStateOf(node?.nodeType ?: UINodeType.APP_HOME) }
    var matchCriteria by remember(node) { mutableStateOf(node?.matchCriteria ?: emptyList()) }
    var showNodeTypeDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "编辑节点" else "添加节点")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("节点名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    label = { Text("Activity名称（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = showNodeTypeDropdown,
                    onExpandedChange = {
                        showNodeTypeDropdown = !showNodeTypeDropdown
                    }
                ) {
                    OutlinedTextField(
                        value = nodeType.name,
                        onValueChange = { },
                        label = { Text("节点类型") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showNodeTypeDropdown)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = showNodeTypeDropdown,
                        onDismissRequest = { showNodeTypeDropdown = false }
                    ) {
                        UINodeType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    nodeType = type
                                    showNodeTypeDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "匹配条件 (可选)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "如果设置, 将优先使用这些条件来识别页面",
                    style = MaterialTheme.typography.bodySmall,
                )
                MatchCriteriaEditor(
                    criteria = matchCriteria,
                    onCriteriaChange = { matchCriteria = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, activityName.takeIf { it.isNotBlank() }, nodeType, matchCriteria)
                    }
                },
                enabled = name.isNotBlank()
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

@Composable
private fun MatchCriteriaEditor(
    criteria: List<UISelector>,
    onCriteriaChange: (List<UISelector>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        criteria.forEachIndexed { index, selector ->
            SelectorListItem(
                selector = selector,
                onSelectorChange = { newSelector ->
                    val newList = criteria.toMutableList().also { it[index] = newSelector }
                    onCriteriaChange(newList)
                },
                onRemove = {
                    val newList = criteria.toMutableList().also { it.removeAt(index) }
                    onCriteriaChange(newList)
                }
            )
        }

        OutlinedButton(
            onClick = {
                val newSelector = UISelector.ByText("") // Default new selector
                onCriteriaChange(criteria + newSelector)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加条件")
            Spacer(Modifier.width(4.dp))
            Text("添加条件")
        }
    }
}

@Composable
private fun SelectorListItem(
    selector: UISelector,
    onSelectorChange: (UISelector) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(18.dp))
                }
            }
            SelectorEditor(
                selector = selector,
                onSelectorChange = onSelectorChange
            )
        }
    }
}