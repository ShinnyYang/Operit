---
fork: https://github.com/luojiaping/Operit.git
scope: Chat View Slot 宿主 UI 与 ToolPkg 注册接口移植
---

# Chat View Slot 接口移植

目标是在当前 `development` 插件体系中加入聊天输入区的宿主插槽接口，供 ToolPkg 注册文本或 Compose DSL 内容。

本阶段范围：

- 宿主 Chat View Slot Registry
- ToolPkg 注册捕获、解析、runtime 与 bridge
- Classic/Agent 输入区的三个插入点
- TypeScript 声明、开发 runner 和 API 文档

本阶段不包含 Plan/Build Agent 的权限、会话、工具集合和 Prompt 隔离；这些能力单独设计和移植。

详细变更见 [1_interface_migration.md](1_interface_migration.md)。
