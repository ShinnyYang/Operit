package com.ai.assistance.operit.core.tools.automatic

import com.ai.assistance.operit.core.tools.SimplifiedUINode

/**
 * UI 元素查找器
 *
 * 在 SimplifiedUINode 树中递归查找满足特定选择器条件的节点
 */
object UIElementFinder {

    /**
     * 根据选择器在UI节点树中查找单个元素
     *
     * @param node The root [SimplifiedUINode] to start the search from.
     * @param selector The [UISelector] criteria to match.
     * @return The first matching [SimplifiedUINode], or `null` if not found.
     */
    fun findElement(node: SimplifiedUINode, selector: UISelector): SimplifiedUINode? {
        val matches = when (selector) {
            is UISelector.ByText -> node.text?.contains(selector.text, ignoreCase = true) == true
            is UISelector.ByResourceId -> node.resourceId?.endsWith(selector.id) == true
            is UISelector.ByContentDesc -> node.contentDesc?.contains(selector.desc, ignoreCase = true) == true
            is UISelector.ByClassName -> node.className == selector.name
            else -> false // ByBounds, ByXPath, and Compound are not supported in-memory
        }
        if (matches) return node

        for (child in node.children) {
            val found = findElement(child, selector)
            if (found != null) return found
        }

        return null
    }
} 