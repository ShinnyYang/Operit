package com.ai.assistance.operit.ui.features.toolbox.screens.uidebugger.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.core.tools.automatic.UIOperation
import com.ai.assistance.operit.core.tools.automatic.UISelector
import com.ai.assistance.operit.core.tools.automatic.UINode
import com.ai.assistance.operit.core.tools.automatic.UIEdgeDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationDetailsDialog(
    edge: UIEdgeDefinition,
    fromNodeName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "操作详情",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 路径信息
                item {
                    PathInfoCard(fromNodeName, edge.toNodeName)
                }

                // 操作列表
                item {
                    SectionTitle("操作序列", edge.operations.size, Icons.Default.Build)
                }
                itemsIndexed(edge.operations) { index, operation ->
                    OperationInfoCard(operation = operation, index = index)
                }

                // 验证步骤
                edge.validation?.let { validationOperation ->
                    item {
                        SectionTitle("验证步骤", 1, Icons.Default.VerifiedUser)
                    }
                    item {
                        OperationInfoCard(operation = validationOperation, isValidation = true)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "关闭",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun PathInfoCard(fromNodeName: String, toNodeName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "路径",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "路径",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = fromNodeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "到",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = toNodeName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OperationInfoCard(
    operation: UIOperation,
    index: Int? = null,
    isValidation: Boolean = false
) {
    val cardColor = if (isValidation) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = getOperationIcon(operation)
                val title = operation.javaClass.simpleName
                val titleColor = if (isValidation) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

                index?.let {
                    Text(
                        text = "#${it + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = titleColor.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.Top)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = titleColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            HighlightedDescription(operation.description)

            // Details
            OperationDetails(operation)
        }
    }
}


@Composable
private fun OperationDetails(operation: UIOperation) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    ) {
        when (operation) {
            is UIOperation.Click -> {
                DetailRow("Selector", operation.selector.toString())
            }
            is UIOperation.Input -> {
                DetailRow("Selector", operation.selector.toString())
                DetailRow("Text Key", operation.textVariableKey, isTemplate = true)
            }
            is UIOperation.Wait -> {
                DetailRow("Duration", "${operation.durationMs} ms")
            }
            is UIOperation.ValidateElement -> {
                DetailRow("Selector", operation.selector.toString())
                DetailRow("Expected Key", operation.expectedValueKey, isTemplate = true)
                DetailRow("Validation", operation.validationType.name)
            }
            is UIOperation.PressKey -> {
                DetailRow("Key Code", operation.keyCode)
            }
            is UIOperation.LaunchApp -> {
                DetailRow("Package Name", operation.packageName)
            }
            is UIOperation.Sequential -> {
                Text("Sub-Operations:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                operation.operations.forEachIndexed { index, subOp ->
                    OperationInfoCard(operation = subOp, index = index)
                }
            }
            else -> {
                DetailRow("Info", "This operation type has no specific details.")
            }
        }
    }
}


@Composable
private fun HighlightedDescription(description: String) {
    val annotatedString = buildAnnotatedString {
        val pattern = "\\{\\{([^}]+)\\}\\}".toRegex()
        var lastIndex = 0

        pattern.findAll(description).forEach { matchResult ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            if (startIndex > lastIndex) {
                append(description.substring(lastIndex, startIndex))
            }
            withStyle(style = SpanStyle(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                background = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            )) {
                append(description.substring(startIndex, endIndex))
            }
            lastIndex = endIndex
        }
        if (lastIndex < description.length) {
            append(description.substring(lastIndex))
        }
    }

    if (annotatedString.isNotBlank()) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, isTemplate: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        if (isTemplate) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


private fun getOperationIcon(operation: UIOperation): ImageVector {
    return when (operation) {
        is UIOperation.Click -> Icons.Filled.TouchApp
        is UIOperation.Input -> Icons.Filled.TextFields
        is UIOperation.Wait -> Icons.Filled.Timer
        is UIOperation.ValidateElement -> Icons.Filled.CheckCircle
        is UIOperation.PressKey -> Icons.Filled.Keyboard
        is UIOperation.LaunchApp -> Icons.Filled.Launch
        is UIOperation.Sequential -> Icons.Filled.MoreVert
        else -> Icons.Filled.Info // Default icon for other types
    }
}