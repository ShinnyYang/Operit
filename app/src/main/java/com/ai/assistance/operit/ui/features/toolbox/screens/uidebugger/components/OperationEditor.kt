package com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.core.tools.automatic.UIOperation
import com.ai.assistance.operit.core.tools.automatic.UISelector
import com.ai.assistance.operit.core.tools.automatic.ValidationType

@Composable
fun OperationEditor(
    operations: List<UIOperation>,
    onOperationsChange: (List<UIOperation>) -> Unit
) {
    var expandedOperationIndex by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        operations.forEachIndexed { index, operation ->
            OperationListItem(
                operation = operation,
                isExpanded = expandedOperationIndex == index,
                onExpand = { expandedOperationIndex = if (expandedOperationIndex == index) null else index },
                onOperationChange = { newOperation ->
                    val newList = operations.toMutableList().also { it[index] = newOperation }
                    onOperationsChange(newList)
                },
                onRemove = {
                    val newList = operations.toMutableList().also { it.removeAt(index) }
                    onOperationsChange(newList)
                }
            )
        }

        // Add operation button
        Button(
            onClick = {
                val newOperation = UIOperation.Click(UISelector.ByText(""), "Click")
                onOperationsChange(operations + newOperation)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add operation")
            Spacer(Modifier.width(4.dp))
            Text("Add Operation", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OperationListItem(
    operation: UIOperation,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onOperationChange: (UIOperation) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    operation.javaClass.simpleName, 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, 
                            contentDescription = "Expand",
                            modifier = Modifier.size(18.dp)
                        )
    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Remove",
                            modifier = Modifier.size(18.dp)
                        )
        }
                }
            }

            if (isExpanded) {
                OperationDetailsEditor(operation, onOperationChange)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationDetailsEditor(
    operation: UIOperation,
    onOperationChange: (UIOperation) -> Unit
) {
    var operationType by remember { mutableStateOf(operation::class.java.simpleName) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Operation Type Selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = operationType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Type", style = MaterialTheme.typography.bodySmall) },
                textStyle = MaterialTheme.typography.bodyMedium,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("Click", "Input", "Wait", "ValidateElement", "PressKey", "LaunchApp").forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            operationType = type
                            expanded = false
                            // Create a new default operation of the selected type
                            val newOperation = when (type) {
                                "Click" -> UIOperation.Click(UISelector.ByText(""), "Click")
                                "Input" -> UIOperation.Input(UISelector.ByText(""), "text_key", "Input")
                                "Wait" -> UIOperation.Wait(500L, "Wait")
                                "ValidateElement" -> UIOperation.ValidateElement(UISelector.ByText(""), "expected_key", ValidationType.TEXT_EQUALS, "Validate")
                                "PressKey" -> UIOperation.PressKey("KEYCODE_ENTER", "Press Key")
                                "LaunchApp" -> UIOperation.LaunchApp("com.example.app", "Launch App")
                                else -> operation
                            }
                            onOperationChange(newOperation)
                        }
                    )
                }
            }
        }

        // Parameters based on type
        when (operation) {
            is UIOperation.Click -> ClickEditor(operation, onOperationChange)
            is UIOperation.Input -> InputEditor(operation, onOperationChange)
            is UIOperation.Wait -> WaitEditor(operation, onOperationChange)
            is UIOperation.ValidateElement -> ValidateElementEditor(operation, onOperationChange)
            is UIOperation.PressKey -> PressKeyEditor(operation, onOperationChange)
            is UIOperation.LaunchApp -> LaunchAppEditor(operation, onOperationChange)
            is UIOperation.Sequential -> { /* Sequential editor can be complex, skipping for now */ }
            else -> {
                Text("Editor for this operation type is not implemented yet.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ClickEditor(operation: UIOperation.Click, onOperationChange: (UIOperation.Click) -> Unit) {
    SelectorEditor(
        selector = operation.selector,
        onSelectorChange = { newSelector ->
            onOperationChange(operation.copy(selector = newSelector))
        }
    )
}

@Composable
private fun InputEditor(operation: UIOperation.Input, onOperationChange: (UIOperation.Input) -> Unit) {
    var textKey by remember { mutableStateOf(operation.textVariableKey) }

    SelectorEditor(
        selector = operation.selector,
        onSelectorChange = { newSelector ->
            onOperationChange(operation.copy(selector = newSelector))
        }
    )

    OutlinedTextField(
        value = textKey,
        onValueChange = {
            textKey = it
            onOperationChange(operation.copy(textVariableKey = it))
        },
        label = { Text("Text Variable Key", style = MaterialTheme.typography.bodySmall) },
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WaitEditor(operation: UIOperation.Wait, onOperationChange: (UIOperation.Wait) -> Unit) {
    var duration by remember { mutableStateOf(operation.durationMs.toString()) }

    OutlinedTextField(
        value = duration,
        onValueChange = {
            duration = it
            it.toLongOrNull()?.let { millis ->
                onOperationChange(operation.copy(durationMs = millis))
            }
        },
        label = { Text("Duration (ms)", style = MaterialTheme.typography.bodySmall) },
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ValidateElementEditor(operation: UIOperation.ValidateElement, onOperationChange: (UIOperation.ValidateElement) -> Unit) {
    var expectedKey by remember { mutableStateOf(operation.expectedValueKey) }

    SelectorEditor(
        selector = operation.selector,
        onSelectorChange = { newSelector ->
            onOperationChange(operation.copy(selector = newSelector))
        }
    )

    OutlinedTextField(
        value = expectedKey,
        onValueChange = {
            expectedKey = it
            onOperationChange(operation.copy(expectedValueKey = it))
        },
        label = { Text("Expected Value Key", style = MaterialTheme.typography.bodySmall) },
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
    // Simplified validation type selector could be added here
}

@Composable
private fun PressKeyEditor(operation: UIOperation.PressKey, onOperationChange: (UIOperation.PressKey) -> Unit) {
    var keyCode by remember { mutableStateOf(operation.keyCode) }

    OutlinedTextField(
        value = keyCode,
        onValueChange = {
            keyCode = it
            onOperationChange(operation.copy(keyCode = it))
        },
        label = { Text("Key Code", style = MaterialTheme.typography.bodySmall) },
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LaunchAppEditor(operation: UIOperation.LaunchApp, onOperationChange: (UIOperation.LaunchApp) -> Unit) {
     var packageName by remember { mutableStateOf(operation.packageName) }
        OutlinedTextField(
        value = packageName,
        onValueChange = {
            packageName = it
            onOperationChange(operation.copy(packageName = it))
        },
        label = { Text("Package Name", style = MaterialTheme.typography.bodySmall) },
        textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
}


@Composable
fun SelectorEditor(
    selector: UISelector,
    onSelectorChange: (UISelector) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectorTypes = listOf(
        "By Text" to { UISelector.ByText("") },
        "By Resource ID" to { UISelector.ByResourceId("") },
        "By Content Desc" to { UISelector.ByContentDesc("") },
        "By Class Name" to { UISelector.ByClassName("") },
        "By Bounds" to { UISelector.ByBounds("0,0,0,0") },
        "By XPath" to { UISelector.ByXPath("") },
        "Compound" to { UISelector.Compound(listOf(UISelector.ByText("")), "AND") }
    )

    val currentTypeName = when (selector) {
        is UISelector.ByText -> "By Text"
        is UISelector.ByResourceId -> "By Resource ID"
        is UISelector.ByContentDesc -> "By Content Desc"
        is UISelector.ByClassName -> "By Class Name"
        is UISelector.ByBounds -> "By Bounds"
        is UISelector.ByXPath -> "By XPath"
        is UISelector.Compound -> "Compound"
    }


    Column(
        modifier = modifier
                        .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        var typeMenuExpanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(
                onClick = { typeMenuExpanded = true },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(currentTypeName, style = MaterialTheme.typography.bodySmall)
                Icon(
                    Icons.Default.ArrowDropDown, 
                    contentDescription = "Change selector type",
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false }
                ) {
                selectorTypes.forEach { (name, factory) ->
                        DropdownMenuItem(
                        text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                            onSelectorChange(factory())
                            typeMenuExpanded = false
                            }
                        )
                    }
                }
            }


        when (selector) {
            is UISelector.ByText -> {
                OutlinedTextField(
                    value = selector.text,
                    onValueChange = { onSelectorChange(UISelector.ByText(it)) },
                    label = { Text("Text", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is UISelector.ByResourceId -> {
                OutlinedTextField(
                    value = selector.id,
                    onValueChange = { onSelectorChange(UISelector.ByResourceId(it)) },
                    label = { Text("Resource ID", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is UISelector.ByContentDesc -> {
                OutlinedTextField(
                    value = selector.desc,
                    onValueChange = { onSelectorChange(UISelector.ByContentDesc(it)) },
                    label = { Text("Content Description", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is UISelector.ByClassName -> {
                OutlinedTextField(
                    value = selector.name,
                    onValueChange = { onSelectorChange(UISelector.ByClassName(it)) },
                    label = { Text("Class Name", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
                )
        }
            is UISelector.ByBounds -> {
                OutlinedTextField(
                    value = selector.bounds,
                    onValueChange = { onSelectorChange(UISelector.ByBounds(it)) },
                    label = { Text("Bounds (left,top,right,bottom)", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is UISelector.ByXPath -> {
                OutlinedTextField(
                    value = selector.xpath,
                    onValueChange = { onSelectorChange(UISelector.ByXPath(it)) },
                    label = { Text("XPath", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is UISelector.Compound -> {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Operator", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.weight(1f))
                    val isAnd = selector.operator == "AND"
                    FilterChip(
                        selected = isAnd,
                        onClick = {
                            val newOperator = if (isAnd) "OR" else "AND"
                            onSelectorChange(selector.copy(operator = newOperator))
                        },
                        label = { Text(if (isAnd) "AND" else "OR", style = MaterialTheme.typography.bodySmall) }
                    )
                }

                selector.selectors.forEachIndexed { index, subSelector ->
                    Row(verticalAlignment = Alignment.Top) {
                        SelectorEditor(
                            selector = subSelector,
                            onSelectorChange = { newSubSelector ->
                                val newSubSelectors = selector.selectors.toMutableList()
                                newSubSelectors[index] = newSubSelector
                                onSelectorChange(selector.copy(selectors = newSubSelectors))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val newSubSelectors = selector.selectors.toMutableList().apply { removeAt(index) }
                                onSelectorChange(selector.copy(selectors = newSubSelectors))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.RemoveCircleOutline, 
                                contentDescription = "Remove selector",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                var addMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { addMenuExpanded = true },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Add selector",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Add Condition", style = MaterialTheme.typography.bodySmall)
                    }
                    DropdownMenu(
                        expanded = addMenuExpanded,
                        onDismissRequest = { addMenuExpanded = false }
                    ) {
                        selectorTypes.forEach { (name, factory) ->
                            DropdownMenuItem(
                                text = { Text(name, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    val newSubSelectors = selector.selectors + factory()
                                    onSelectorChange(selector.copy(selectors = newSubSelectors))
                                    addMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
