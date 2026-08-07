package com.ai.assistance.operit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ai.assistance.operit.data.dao.ChatContentDao
import com.ai.assistance.operit.data.dao.ChatDao
import com.ai.assistance.operit.data.dao.MessageDao
import com.ai.assistance.operit.data.dao.MessageVariantDao
import com.ai.assistance.operit.data.dao.TokenStatsDao
import com.ai.assistance.operit.data.model.ChatEntity
import com.ai.assistance.operit.data.model.MessageEntity
import com.ai.assistance.operit.data.model.MessageVariantEntity
import com.ai.assistance.operit.data.model.TokenStatBaselineEntity
import com.ai.assistance.operit.data.model.TokenStatCleanupItemEntity
import com.ai.assistance.operit.data.model.TokenStatCleanupOperationEntity
import com.ai.assistance.operit.data.model.TokenStatDisplayModelEntity
import com.ai.assistance.operit.data.model.TokenStatEventEntity
import com.ai.assistance.operit.data.model.TokenStatIdentityEntity
import com.ai.assistance.operit.data.model.TokenStatPriceOverrideEntity
import com.ai.assistance.operit.data.model.TokenStatRangeCutoffEntity
import com.ai.assistance.operit.data.model.TokenStatResetCutoffEntity
/** 应用数据库，包含聊天表和消息表 */
@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        MessageVariantEntity::class,
        TokenStatIdentityEntity::class,
        TokenStatDisplayModelEntity::class,
        TokenStatPriceOverrideEntity::class,
        TokenStatEventEntity::class,
        TokenStatBaselineEntity::class,
        TokenStatResetCutoffEntity::class,
        TokenStatRangeCutoffEntity::class,
        TokenStatCleanupOperationEntity::class,
        TokenStatCleanupItemEntity::class,
    ],
    version = 21,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /** 获取聊天DAO */
    abstract fun chatDao(): ChatDao

    /** 获取消息DAO */
    abstract fun messageDao(): MessageDao
    abstract fun messageVariantDao(): MessageVariantDao
    abstract fun chatContentDao(): ChatContentDao
    abstract fun tokenStatsDao(): TokenStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 定义从版本1到2的迁移
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 创建chats表
                    db.execSQL(
                        """
                            CREATE TABLE IF NOT EXISTS `chats` (
                                `id` TEXT NOT NULL,
                                `title` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                `updatedAt` INTEGER NOT NULL,
                                `inputTokens` INTEGER NOT NULL DEFAULT 0,
                                `outputTokens` INTEGER NOT NULL DEFAULT 0,
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent()
                    )

                    // 创建messages表
                    db.execSQL(
                        """
                            CREATE TABLE IF NOT EXISTS `messages` (
                                `messageId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `chatId` TEXT NOT NULL,
                                `sender` TEXT NOT NULL,
                                `content` TEXT NOT NULL,
                                `timestamp` INTEGER NOT NULL,
                                `orderIndex` INTEGER NOT NULL,
                                FOREIGN KEY(`chatId`) REFERENCES `chats`(`id`) ON DELETE CASCADE
                            )
                        """.trimIndent()
                    )

                    // 为messages表创建索引
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_chatId` ON `messages` (`chatId`)")
                }

            }

        // 定义从版本10到11的迁移
        private val MIGRATION_10_11 =
            object : Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向chats表添加workspaceEnv列
                    try {
                        db.execSQL("ALTER TABLE chats ADD COLUMN `workspaceEnv` TEXT")
                    } catch (_: Exception) {

                    }
                }
            }

        // 定义从版本11到12的迁移
        private val MIGRATION_11_12 =
            object : Migration(11, 12) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向chats表添加characterGroupId列（用于绑定群组角色卡）
                    try {
                        db.execSQL("ALTER TABLE chats ADD COLUMN `characterGroupId` TEXT")
                    } catch (_: Exception) {

                    }
                }
            }

        private val MIGRATION_12_13 =
            object : Migration(12, 13) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    try {
                        db.execSQL("ALTER TABLE messages ADD COLUMN `inputTokens` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {
                    }
                    try {
                        db.execSQL("ALTER TABLE messages ADD COLUMN `outputTokens` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {
                    }
                    try {
                        db.execSQL("ALTER TABLE messages ADD COLUMN `cachedInputTokens` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {
                    }
                    try {
                        db.execSQL("ALTER TABLE messages ADD COLUMN `sentAt` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {
                    }
                    try {
                        db.execSQL("ALTER TABLE messages ADD COLUMN `outputDurationMs` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {
                    }
                    try {
                        db.execSQL("ALTER TABLE messages ADD COLUMN `waitDurationMs` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {
                    }
                }
            }

        private val MIGRATION_13_14 =
            object : Migration(13, 14) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("DROP TABLE IF EXISTS `problem_records`")
                }
            }

        private val MIGRATION_14_15 =
            object : Migration(14, 15) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE messages ADD COLUMN `selectedVariantIndex` INTEGER NOT NULL DEFAULT 0"
                    )
                    db.execSQL(
                        """
                            CREATE TABLE IF NOT EXISTS `message_variants` (
                                `variantId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `chatId` TEXT NOT NULL,
                                `messageTimestamp` INTEGER NOT NULL,
                                `variantIndex` INTEGER NOT NULL,
                                `content` TEXT NOT NULL,
                                `roleName` TEXT NOT NULL DEFAULT '',
                                `provider` TEXT NOT NULL DEFAULT '',
                                `modelName` TEXT NOT NULL DEFAULT '',
                                `inputTokens` INTEGER NOT NULL DEFAULT 0,
                                `outputTokens` INTEGER NOT NULL DEFAULT 0,
                                `cachedInputTokens` INTEGER NOT NULL DEFAULT 0,
                                `sentAt` INTEGER NOT NULL DEFAULT 0,
                                `outputDurationMs` INTEGER NOT NULL DEFAULT 0,
                                `waitDurationMs` INTEGER NOT NULL DEFAULT 0,
                                FOREIGN KEY(`chatId`) REFERENCES `chats`(`id`) ON DELETE CASCADE
                            )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_message_variants_chatId_messageTimestamp` ON `message_variants` (`chatId`, `messageTimestamp`)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_message_variants_chatId_messageTimestamp_variantIndex` ON `message_variants` (`chatId`, `messageTimestamp`, `variantIndex`)"
                    )
                }
            }

        private val MIGRATION_15_16 =
            object : Migration(15, 16) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE messages ADD COLUMN `displayMode` TEXT NOT NULL DEFAULT 'NORMAL'"
                    )
                }
            }

        private val MIGRATION_16_17 =
            object : Migration(16, 17) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_messages_chatId_timestamp` ON `messages` (`chatId`, `timestamp`)"
                    )
                }
            }

        private val MIGRATION_17_18 =
            object : Migration(17, 18) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE messages ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        private val MIGRATION_18_19 =
            object : Migration(18, 19) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "ALTER TABLE messages ADD COLUMN `completedAt` INTEGER NOT NULL DEFAULT 0"
                    )
                    db.execSQL(
                        "ALTER TABLE message_variants ADD COLUMN `completedAt` INTEGER NOT NULL DEFAULT 0"
                    )
                }
            }

        private val MIGRATION_19_20 =
            object : Migration(19, 20) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE chats ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
                }
            }

        /**
         * v20 → v21：token 统计账本表（全部为纯新增，幂等可重入）。
         * 事件表通过外键级联到身份表；baseline 冻结价格语义见
         * [com.ai.assistance.operit.data.stats.TokenBaselineMigrator]。
         */
        private val MIGRATION_20_21 =
            object : Migration(20, 21) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `token_stat_identities` (
                            `identityId` TEXT NOT NULL,
                            `configId` TEXT NOT NULL,
                            `provider` TEXT NOT NULL,
                            `model` TEXT NOT NULL,
                            `displayModelId` TEXT NOT NULL,
                            PRIMARY KEY(`identityId`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_token_stat_identities_configId_provider_model` " +
                            "ON `token_stat_identities` (`configId`, `provider`, `model`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_token_stat_identities_displayModelId` " +
                            "ON `token_stat_identities` (`displayModelId`)"
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `token_stat_display_models` (
                            `displayModelId` TEXT NOT NULL,
                            `normalizedModel` TEXT NOT NULL,
                            `displayName` TEXT NOT NULL,
                            PRIMARY KEY(`displayModelId`)
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_token_stat_display_models_normalizedModel` " +
                            "ON `token_stat_display_models` (`normalizedModel`)"
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `token_stat_price_overrides` (
                            `rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `scope` TEXT NOT NULL,
                            `provider` TEXT NOT NULL,
                            `model` TEXT NOT NULL,
                            `configId` TEXT NOT NULL,
                            `billingMode` TEXT NOT NULL,
                            `pricingCurrency` TEXT NOT NULL,
                            `inputPricePerMillion` REAL,
                            `cachedInputPricePerMillion` REAL,
                            `cacheWritePricePerMillion` REAL,
                            `outputPricePerMillion` REAL,
                            `pricePerRequest` REAL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_token_stat_price_overrides_scope_provider_model_configId` " +
                            "ON `token_stat_price_overrides` (`scope`, `provider`, `model`, `configId`)"
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `token_stat_events` (
                            `eventId` TEXT NOT NULL,
                            `statIdentityId` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `startedAtMs` INTEGER NOT NULL,
                            `endedAtMs` INTEGER NOT NULL,
                            `firstTokenAtMs` INTEGER,
                            `uncachedInputTokens` INTEGER,
                            `cachedInputTokens` INTEGER,
                            `cacheWriteTokens` INTEGER,
                            `outputTokens` INTEGER,
                            `reasoningTokens` INTEGER,
                            `reasoningIncludedInOutput` INTEGER,
                            `billingMode` TEXT NOT NULL,
                            `pricingCurrency` TEXT NOT NULL,
                            `inputPricePerMillion` REAL,
                            `cachedInputPricePerMillion` REAL,
                            `cacheWritePricePerMillion` REAL,
                            `outputPricePerMillion` REAL,
                            `pricePerRequest` REAL,
                            `pricingSource` TEXT NOT NULL,
                            `costInPricingCurrency` REAL,
                            PRIMARY KEY(`eventId`),
                            FOREIGN KEY(`statIdentityId`)
                                REFERENCES `token_stat_identities`(`identityId`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS " +
                            "`index_token_stat_events_statIdentityId_startedAtMs` " +
                            "ON `token_stat_events` (`statIdentityId`, `startedAtMs`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_token_stat_events_startedAtMs` " +
                            "ON `token_stat_events` (`startedAtMs`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_token_stat_events_category_startedAtMs` " +
                            "ON `token_stat_events` (`category`, `startedAtMs`)"
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `token_stat_baselines` (
                            `identityId` TEXT NOT NULL,
                            `inputTokens` INTEGER NOT NULL,
                            `cachedInputTokens` INTEGER NOT NULL,
                            `outputTokens` INTEGER NOT NULL,
                            `requestCount` INTEGER NOT NULL,
                            `pricingCurrency` TEXT NOT NULL,
                            `costInPricingCurrency` REAL,
                            `isEstimated` INTEGER NOT NULL,
                            `fingerprint` TEXT NOT NULL,
                            `importedAtMs` INTEGER NOT NULL,
                            `frozenBillingMode` TEXT NOT NULL,
                            `frozenInputPricePerMillion` REAL,
                            `frozenCachedInputPricePerMillion` REAL,
                            `frozenOutputPricePerMillion` REAL,
                            `frozenPricePerRequest` REAL,
                            PRIMARY KEY(`identityId`),
                            FOREIGN KEY(`identityId`)
                                REFERENCES `token_stat_identities`(`identityId`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    // 事件表增加脱敏诊断列与费用计算所需的结构化列：
                    // - `acceptedGeneration`：reset tombstone 一致性边界（排空事务检查）；
                    // - `totalInputTokens`：provider 明确上报的总输入（拆分未知时重估直接读取）；
                    // - `cacheWriteSeparateBilling`：缓存写入是否独立计费；
                    // - `diagnosticsJson`：来源标签、usageObserved、usageReportCount 等诊断元数据。
                    // 另新增 `token_stat_reset_cutoffs` 表（reset tombstone）。全部为纯新增，
                    // 幂等可重入（重复执行时列/表已存在即跳过）。
                    try {
                        db.execSQL(
                            "ALTER TABLE `token_stat_events` ADD COLUMN " +
                                "`acceptedGeneration` INTEGER NOT NULL DEFAULT 0"
                        )
                    } catch (_: Exception) {
                        // 列已存在（幂等重放），忽略
                    }
                    try {
                        db.execSQL(
                            "ALTER TABLE `token_stat_events` ADD COLUMN `totalInputTokens` INTEGER"
                        )
                    } catch (_: Exception) {
                        // 列已存在（幂等重放），忽略
                    }
                    try {
                        db.execSQL(
                            "ALTER TABLE `token_stat_events` ADD COLUMN " +
                                "`cacheWriteSeparateBilling` INTEGER"
                        )
                    } catch (_: Exception) {
                        // 列已存在（幂等重放），忽略
                    }
                    try {
                        db.execSQL(
                            "ALTER TABLE `token_stat_events` ADD COLUMN `diagnosticsJson` TEXT"
                        )
                    } catch (_: Exception) {
                        // 列已存在（幂等重放），忽略
                    }
                    try {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `token_stat_reset_cutoffs` (
                                `kind` TEXT NOT NULL,
                                `provider` TEXT NOT NULL,
                                `model` TEXT NOT NULL,
                                `generation` INTEGER NOT NULL,
                                PRIMARY KEY(`kind`, `provider`, `model`)
                            )
                            """.trimIndent()
                        )
                    } catch (_: Exception) {
                        // 表已存在（幂等重放），忽略
                    }
                }
            }
                    try {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `token_stat_range_cutoffs` (
                                `generation` INTEGER NOT NULL,
                                `startMs` INTEGER NOT NULL,
                                `endMs` INTEGER NOT NULL,
                                PRIMARY KEY(`generation`)
                            )
                            """.trimIndent()
                        )
                    } catch (_: Exception) {
                        // 表已存在（幂等重放），忽略
                    }
                    try {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `token_stat_cleanup_operations` (
                                `operationId` TEXT NOT NULL,
                                `scope` TEXT NOT NULL,
                                `targetRef` TEXT NOT NULL,
                                `deleteBaselines` INTEGER NOT NULL,
                                `status` TEXT NOT NULL,
                                `createdAtMs` INTEGER NOT NULL,
                                PRIMARY KEY(`operationId`)
                            )
                            """.trimIndent()
                        )
                    } catch (_: Exception) {
                        // 表已存在（幂等重放），忽略
                    }
                    try {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `token_stat_cleanup_items` (
                                `operationId` TEXT NOT NULL,
                                `identityId` TEXT NOT NULL,
                                `provider` TEXT NOT NULL,
                                `model` TEXT NOT NULL,
                                PRIMARY KEY(`operationId`, `identityId`),
                                FOREIGN KEY(`operationId`)
                                    REFERENCES `token_stat_cleanup_operations`(`operationId`)
                                    ON UPDATE NO ACTION ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                    } catch (_: Exception) {
                        // 表已存在（幂等重放），忽略
                    }
                    try {
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS `index_token_stat_cleanup_items_operationId` " +
                                "ON `token_stat_cleanup_items` (`operationId`)"
                        )
                    } catch (_: Exception) {
                        // 索引已存在（幂等重放），忽略
                    }



        // 定义从版本2到3的迁移
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向chats表添加group列
                    db.execSQL("ALTER TABLE chats ADD COLUMN `group` TEXT")
                }
            }

        // 定义从版本3到4的迁移
        private val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向chats表添加displayOrder列，并用updatedAt填充现有数据
                    db.execSQL(
                        "ALTER TABLE chats ADD COLUMN `displayOrder` INTEGER NOT NULL DEFAULT 0"
                    )
                    db.execSQL("UPDATE chats SET displayOrder = updatedAt")
                }
            }

        // 定义从版本4到5的迁移
        private val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向chats表添加workspace列
                    db.execSQL("ALTER TABLE chats ADD COLUMN `workspace` TEXT")
                }
            }

        // 定义从版本5到6的迁移
        private val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 检查currentWindowSize列是否已存在，如果不存在则添加
                    try {
                        db.execSQL("ALTER TABLE chats ADD COLUMN `currentWindowSize` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {

                    }
                }
            }

        // 定义从版本6到7的迁移
        private val MIGRATION_6_7 =
            object : Migration(6, 7) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向messages表添加roleName列
                    db.execSQL("ALTER TABLE messages ADD COLUMN `roleName` TEXT NOT NULL DEFAULT ''")
                }
            }

        // 定义从版本7到8的迁移
        private val MIGRATION_7_8 =
            object : Migration(7, 8) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向chats表添加parentChatId列
                    db.execSQL("ALTER TABLE chats ADD COLUMN `parentChatId` TEXT")
                    // 向chats表添加characterCardName列（用于绑定角色卡）
                    db.execSQL("ALTER TABLE chats ADD COLUMN `characterCardName` TEXT")
                }
            }

        // 定义从版本8到9的迁移
        private val MIGRATION_8_9 =
            object : Migration(8, 9) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向messages表添加provider列（供应商）
                    db.execSQL("ALTER TABLE messages ADD COLUMN `provider` TEXT NOT NULL DEFAULT ''")
                    // 向messages表添加modelName列（模型名称）
                    db.execSQL("ALTER TABLE messages ADD COLUMN `modelName` TEXT NOT NULL DEFAULT ''")
                }
            }

        // 定义从版本9到10的迁移
        private val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // 向chats表添加locked列（锁定聊天，禁止删除）
                    try {
                        db.execSQL("ALTER TABLE chats ADD COLUMN `locked` INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {

                    }
                }
            }

        /** 获取数据库实例，单例模式 */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "app_database"
                        )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5,
                                MIGRATION_5_6,
                                MIGRATION_6_7,
                                MIGRATION_7_8,
                                MIGRATION_8_9,
                                MIGRATION_9_10,
                                MIGRATION_10_11,
                                MIGRATION_11_12,
                                MIGRATION_12_13,
                                MIGRATION_13_14,
                                MIGRATION_14_15,
                                MIGRATION_15_16,
                                MIGRATION_16_17,
                                MIGRATION_17_18,
                                MIGRATION_18_19,
                                MIGRATION_19_20,
                                MIGRATION_20_21
                            ) // 添加新的迁移
                            .build()
                    INSTANCE = instance
                    instance
                }
        }

        fun closeDatabase() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } finally {
                    INSTANCE = null
                }
            }
        }
    }
}
