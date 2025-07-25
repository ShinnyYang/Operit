package com.ai.assistance.operit.data.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.ai.assistance.operit.data.db.ObjectBoxManager
import com.ai.assistance.operit.data.model.Memory
import com.ai.assistance.operit.data.model.MemoryLink
import com.ai.assistance.operit.data.model.MemoryTag
import com.ai.assistance.operit.data.model.MemoryTag_
import com.ai.assistance.operit.data.model.Memory_
import com.ai.assistance.operit.services.EmbeddingService
import com.ai.assistance.operit.ui.features.memory.screens.graph.model.Edge
import com.ai.assistance.operit.ui.features.memory.screens.graph.model.Graph
import com.ai.assistance.operit.ui.features.memory.screens.graph.model.Node
import com.ai.assistance.operit.util.vector.MemoryItem
import com.ai.assistance.operit.util.vector.VectorIndexManager
import io.objectbox.Box
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for handling Memory data operations. It abstracts the data source (ObjectBox) from the
 * rest of the application.
 */
class MemoryRepository(context: Context, profileId: String) {

    private val store = ObjectBoxManager.get(context, profileId)
    private val memoryBox: Box<Memory> = store.boxFor()
    private val tagBox = store.boxFor<MemoryTag>()
    private val linkBox = store.boxFor<MemoryLink>()

    // --- HNSW向量索引集成 ---
    private val vectorIndexManager: VectorIndexManager<MemoryItem, String> by lazy {
        val indexFile = File(context.filesDir, "memory_hnsw_${profileId}.idx")
        val manager =
                VectorIndexManager<MemoryItem, String>(
                        dimensions = 100, // 实际embedding维度为100
                        maxElements = 100_000,
                        indexFile = indexFile
                )
        manager.initIndex()
        // 首次构建索引
        memoryBox.all.filter { it.embedding != null }.forEach { memory ->
            manager.addItem(MemoryItem(memory.uuid, memory.embedding!!.vector, memory))
        }
        manager
    }

    // --- Memory CRUD Operations ---

    /**
     * Creates or updates a memory, automatically generating its embedding.
     * @param memory The memory object to be saved.
     * @return The ID of the saved memory.
     */
    suspend fun saveMemory(memory: Memory): Long = withContext(Dispatchers.IO){
        // Generate embedding before saving
        if (memory.content.isNotBlank()) {
            memory.embedding = EmbeddingService.generateEmbedding(memory.content)
        }
        memoryBox.put(memory)
    }

    /**
     * Finds a memory by its ID.
     * @param id The ID of the memory to find.
     * @return The found Memory object, or null if not found.
     */
    suspend fun findMemoryById(id: Long): Memory? = withContext(Dispatchers.IO) {
        memoryBox.get(id)
    }

    /**
     * Finds a memory by its UUID.
     * @param uuid The UUID of the memory to find.
     * @return The found Memory object, or null if not found.
     */
    suspend fun findMemoryByUuid(uuid: String): Memory? = withContext(Dispatchers.IO) {
        memoryBox.query(Memory_.uuid.equal(uuid)).build().findFirst()
    }

    /**
     * Finds a memory by its exact title.
     * @param title The title of the memory to find.
     * @return The found Memory object, or null if not found.
     */
    suspend fun findMemoryByTitle(title: String): Memory? = withContext(Dispatchers.IO) {
        memoryBox.query(Memory_.title.equal(title)).build().findFirst()
    }

    /**
     * Deletes a memory and all its links. This is a critical operation and should be handled with
     * care.
     * @param memoryId The ID of the memory to delete.
     * @return True if deletion was successful, false otherwise.
     */
    suspend fun deleteMemory(memoryId: Long): Boolean = withContext(Dispatchers.IO) {
        val memory = findMemoryById(memoryId) ?: return@withContext false
        // Before deleting the memory, we must clean up its links.
        // This prevents dangling references.
        memory.links.forEach { linkBox.remove(it) }
        memory.backlinks.forEach { linkBox.remove(it) }
        memoryBox.remove(memory)
    }

    // --- Link CRUD Operations ---
    suspend fun findLinkById(linkId: Long): MemoryLink? = withContext(Dispatchers.IO) {
        linkBox.get(linkId)
    }

    suspend fun updateLink(linkId: Long, type: String, weight: Float, description: String): MemoryLink? = withContext(Dispatchers.IO) {
        val link = findLinkById(linkId) ?: return@withContext null
        val sourceMemory = link.source.target

        link.type = type
        link.weight = weight
        link.description = description
        linkBox.put(link)

        // 在更新link后，同样put其所属的source memory。
        // 这是为了向ObjectBox明确指出，这个父实体的关系集合“脏了”，
        // 以此来避免后续查询时拿到缓存的旧数据。
        if (sourceMemory != null) {
            memoryBox.put(sourceMemory)
        }

        link
    }

    suspend fun deleteLink(linkId: Long): Boolean = withContext(Dispatchers.IO) {
        // 为了健壮性，在删除链接后，也更新其父实体。
        val link = findLinkById(linkId)
        val sourceMemory = link?.source?.target

        val wasRemoved = linkBox.remove(linkId)

        if (wasRemoved && sourceMemory != null) {
            // 通过put源实体，我们确保它的ToMany关系缓存在其他线程或未来的查询中得到更新。
            memoryBox.put(sourceMemory)
        }
        wasRemoved
    }

    // --- Tagging Operations ---

    /**
     * Adds a tag to a memory.
     * @param memory The memory to tag.
     * @param tagName The name of the tag.
     * @return The MemoryTag object.
     */
    suspend fun addTagToMemory(memory: Memory, tagName: String): MemoryTag = withContext(Dispatchers.IO) {
        // Find existing tag or create a new one
        val tag =
                tagBox.query()
                        .equal(MemoryTag_.name, tagName, QueryBuilder.StringOrder.CASE_SENSITIVE)
                        .build()
                        .findFirst()
                        ?: MemoryTag(name = tagName).also { tagBox.put(it) }

        if (!memory.tags.any { it.id == tag.id }) {
            memory.tags.add(tag)
            memoryBox.put(memory)
        }
        tag
    }

    // --- Linking Operations ---

    /**
     * Creates a link between two memories.
     * @param source The source memory.
     * @param target The target memory.
     * @param type The type of the link (e.g., "causes", "explains").
     * @param weight The strength of the link.
     * @param description A description of the link.
     */
    suspend fun linkMemories(
            source: Memory,
            target: Memory,
            type: String,
            weight: Float = 1.0f,
            description: String = ""
    ) = withContext(Dispatchers.IO) {
        val link = MemoryLink(type = type, weight = weight, description = description)
        link.source.target = source
        link.target.target = target

        source.links.add(link)
        memoryBox.put(source)
    }

    /** Gets all outgoing links from a memory. */
    suspend fun getOutgoingLinks(memoryId: Long): List<MemoryLink> = withContext(Dispatchers.IO) {
        val memory = findMemoryById(memoryId)
        memory?.links ?: emptyList()
    }

    /** Gets all incoming links to a memory. */
    suspend fun getIncomingLinks(memoryId: Long): List<MemoryLink> = withContext(Dispatchers.IO) {
        val memory = findMemoryById(memoryId)
        memory?.backlinks ?: emptyList()
    }

    // --- Complex Queries ---

    /**
     * Searches memories using semantic search if a query is provided, otherwise returns all
     * memories.
     * @param query The search query string.
     * @return A list of matching Memory objects, sorted by relevance.
     */
    suspend fun searchMemories(query: String): List<Memory> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext memoryBox.all

        val scores = mutableMapOf<Long, Double>()
        val k = 60.0 // RRF constant for result fusion
        val allMemories = memoryBox.all // Fetch all memories once for efficiency

        // 1. Keyword-based search (Memory title/content contains query)
        val titleCondition =
                Memory_.title.contains(query, QueryBuilder.StringOrder.CASE_INSENSITIVE)
        val contentCondition =
                Memory_.content.contains(query, QueryBuilder.StringOrder.CASE_INSENSITIVE)
        val keywordResults = memoryBox.query(titleCondition.or(contentCondition)).build().find()

        keywordResults.forEachIndexed { index, memory ->
            val rank = index + 1
            scores[memory.id] = scores.getOrDefault(memory.id, 0.0) + (1.0 / (k + rank))
        }

        // 2. Reverse Containment Search (Query contains Memory Title)
        // This is crucial for finding "长安大学" within the query "长安大学在西安".
        val reverseContainmentResults =
                allMemories.filter { memory -> query.contains(memory.title, ignoreCase = true) }
        reverseContainmentResults.forEachIndexed { index, memory ->
            val rank = index + 1
            // Use the same RRF formula to add to the score
            scores[memory.id] = scores.getOrDefault(memory.id, 0.0) + (1.0 / (k + rank))
        }

        // 3. Semantic search (for conceptual matches)
        val queryEmbedding = EmbeddingService.generateEmbedding(query)
        if (queryEmbedding != null) {
            val allMemoriesWithEmbedding = allMemories.filter { it.embedding != null }

            val semanticResults =
                    allMemoriesWithEmbedding
                            .mapNotNull { memory ->
                                memory.embedding?.let { memoryEmbedding ->
                                    val similarity =
                                            EmbeddingService.cosineSimilarity(
                                                    queryEmbedding,
                                                    memoryEmbedding
                                            )
                                    Pair(memory, similarity)
                                }
                            }
                            .sortedByDescending { it.second }
                            .map { it.first } // We only need the ranked list of memories

            semanticResults.forEachIndexed { index, memory ->
                val rank = index + 1
                scores[memory.id] = scores.getOrDefault(memory.id, 0.0) + (1.0 / (k + rank))
            }
        }

        // 4. Fuse results using RRF and return sorted list
        if (scores.isEmpty()) {
            return@withContext emptyList()
        }
        val sortedMemoryIds = scores.entries.sortedByDescending { it.value }.map { it.key }

        // Fetch the sorted entities from the database
        val sortedMemories = memoryBox.get(sortedMemoryIds)

        // 5. Semantic Deduplication
        deduplicateBySemantics(sortedMemories)
    }

    suspend fun addMemoryToIndex(memory: Memory) = withContext(Dispatchers.IO) {
        if (memory.embedding != null) {
            vectorIndexManager.addItem(MemoryItem(memory.uuid, memory.embedding!!.vector, memory))
        }
    }
    suspend fun removeMemoryFromIndex(memory: Memory) = withContext(Dispatchers.IO) {
        // hnswlib支持removeEnabled时可用，若不支持可忽略
        // vectorIndexManager.removeItem(memory.uuid)
    }

    /** 使用HNSW索引的高效语义检索。 */
    suspend fun searchMemoriesPrecise(query: String, similarityThreshold: Float = 0.95f): List<Memory> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val queryEmbedding = EmbeddingService.generateEmbedding(query) ?: return@withContext emptyList()
        // 取前100个最相近的记忆，再按阈值过滤
        val candidates = vectorIndexManager.findNearest(queryEmbedding.vector, 100)
        candidates.map { it.memoryRef }
            .filter { it.embedding != null && EmbeddingService.cosineSimilarity(queryEmbedding, it.embedding!!) >= similarityThreshold }
    }

    /**
     * Deduplicates a list of memories based on semantic similarity. If two memories are very
     * similar, only the first one in the list (higher rank) is kept.
     */
    private fun deduplicateBySemantics(sortedMemories: List<Memory>): List<Memory> {
        if (sortedMemories.size < 2) {
            return sortedMemories
        }

        val deduplicatedList = mutableListOf<Memory>()
        val memoriesToProcess = sortedMemories.toMutableList()

        while (memoriesToProcess.isNotEmpty()) {
            val current = memoriesToProcess.removeAt(0)
            deduplicatedList.add(current)

            // Remove other memories that are too similar to the 'current' one
            memoriesToProcess.removeAll { other ->
                val similarity =
                        if (current.embedding != null && other.embedding != null) {
                            EmbeddingService.cosineSimilarity(
                                    current.embedding!!,
                                    other.embedding!!
                            )
                        } else {
                            0f
                        }
                // Use a slightly lower threshold for de-duping search results
                val isSimilar = similarity > 0.90f
                if (isSimilar) {
                    android.util.Log.d(
                            "MemoryRepo",
                            "Deduplicating '${other.title}' (similar to '${current.title}', similarity: $similarity)"
                    )
                }
                isSimilar
            }
        }
        android.util.Log.d(
                "MemoryRepo",
                "Deduplication complete. Initial: ${sortedMemories.size}, Final: ${deduplicatedList.size}"
        )
        return deduplicatedList
    }

    /**
     * Builds a Graph object from a given list of memories. This is used to display a subset of the
     * entire memory graph, e.g., after a search.
     * @param memories The list of memories to include in the graph.
     * @return A Graph object.
     */
    suspend fun getGraphForMemories(memories: List<Memory>): Graph = withContext(Dispatchers.IO) {
        // Expand the initial list of memories to include direct neighbors
        val expandedMemories = mutableSetOf<Memory>()
        expandedMemories.addAll(memories)

        memories.forEach { memory ->
            memory.links.forEach { link -> link.target.target?.let { expandedMemories.add(it) } }
            memory.backlinks.forEach { backlink ->
                backlink.source.target?.let { expandedMemories.add(it) }
            }
        }

        android.util.Log.d(
                "MemoryRepo",
                "Initial memories: ${memories.size}, Expanded memories: ${expandedMemories.size}"
        )
        buildGraphFromMemories(expandedMemories.toList())
    }

    /** Retrieves a single memory by its UUID. */
    suspend fun getMemoryByUuid(uuid: String): Memory? =
            withContext(Dispatchers.IO) {
                memoryBox.query(Memory_.uuid.equal(uuid)).build().findUnique()
            }

    /**
     * 创建新记忆并自动生成embedding，保存到数据库并同步索引。
     */
    suspend fun createMemory(title: String, content: String, contentType: String = "text/plain", source: String = "user_input"): Memory? = withContext(Dispatchers.IO) {
        val embedding = EmbeddingService.generateEmbedding(content) ?: return@withContext null
        val memory = Memory(
            title = title,
            content = content,
            contentType = contentType,
            source = source,
            embedding = embedding
        )
        saveMemory(memory)
        addMemoryToIndex(memory)
        memory
    }

    /**
     * 更新已有记忆内容（title/content等），自动更新embedding和索引。
     */
    suspend fun updateMemory(memory: Memory, newTitle: String, newContent: String, newContentType: String = memory.contentType): Memory? = withContext(Dispatchers.IO) {
        val newEmbedding = EmbeddingService.generateEmbedding(newContent) ?: return@withContext null
        val updated = memory.copy(
            title = newTitle,
            content = newContent,
            contentType = newContentType,
            embedding = newEmbedding,
            updatedAt = java.util.Date()
        )
        saveMemory(updated)
        addMemoryToIndex(updated)
        updated
    }

    /**
     * 删除记忆并同步索引。
     */
    suspend fun deleteMemoryAndIndex(memoryId: Long): Boolean = withContext(Dispatchers.IO) {
        val memory = findMemoryById(memoryId) ?: return@withContext false
        removeMemoryFromIndex(memory)
        deleteMemory(memoryId)
    }

    // --- Graph Export ---

    /** Fetches all memories and their links, and converts them into a Graph data structure. */
    suspend fun getMemoryGraph(): Graph = withContext(Dispatchers.IO) {
        buildGraphFromMemories(memoryBox.all)
    }

    /**
     * Private helper to construct a graph from a specific list of memories. Ensures that edges are
     * only created if both source and target nodes are in the list.
     */
    private fun buildGraphFromMemories(memories: List<Memory>): Graph {
        val memoryUuids = memories.map { it.uuid }.toSet()

        val nodes =
                memories.map { memory ->
                    Node(
                            id = memory.uuid,
                            label = memory.title,
                            color =
                                    when (memory.tags.firstOrNull()?.name) {
                                        "Person" -> Color(0xFF81C784) // Green
                                        "Concept" -> Color(0xFF64B5F6) // Blue
                                        else -> Color.LightGray
                                    }
                    )
                }

        val edges = mutableListOf<Edge>()
        memories.forEach { memory ->
            // 关键：重置关系缓存，确保获取最新的连接信息
            memory.links.reset()
            memory.links.forEach { link ->
                val sourceId = link.source.target?.uuid
                val targetId = link.target.target?.uuid
                // Only add edges if both source and target are in the filtered list
                if (sourceId != null &&
                    targetId != null &&
                    sourceId in memoryUuids &&
                    targetId in memoryUuids
                ) {
                    edges.add(
                        Edge(
                            id = link.id,
                            sourceId = sourceId,
                            targetId = targetId,
                            label = link.type,
                            weight = link.weight
                        )
                    )
                } else if (sourceId != null && targetId != null) {
                    // Log discarded edges for debugging
                    // android.util.Log.d("MemoryRepo", "Discarding edge: $sourceId -> $targetId
                    // (Not in filtered list)")
                }
            }
        }
        android.util.Log.d(
                "MemoryRepo",
                "Built graph with ${nodes.size} nodes and ${edges.distinct().size} edges."
        )
        return Graph(nodes = nodes, edges = edges.distinct())
    }
}
