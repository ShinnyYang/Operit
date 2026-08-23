# Chat View Slot 接口移植

## 原状

当前 `development` 已有 Chat View Hook、Chat Input Hook 和 Compose DSL runtime，但聊天输入区没有给 ToolPkg 提供宿主拥有的命名渲染区域。

## 目标

新增三个稳定插槽：

- `above_input`
- `input_drawer`
- `input_toolbar_right`

ToolPkg 通过 `ToolPkg.registerChatViewSlotPlugin` 注册渲染函数。渲染函数接收聊天 ID、runtime、输入风格、处理状态、焦点状态和输入文本，并返回文本或 Compose DSL 页面。

## 实现边界

- 保留当前 `chatMessageHooks`、WASM、Logo、market-origin 和 Hook 超时预算。
- Slot Bridge 使用现有 `ToolPkgHookExecutionBudget`，不为每次输入变化绕过运行时限制。
- Compose DSL Slot 使用当前 `XmlRenderPluginRegistry` 的 execution engine 生命周期。
- 不加入 Agent 注册、Agent 权限或 Prompt 上下文字段。

## 验证

- `git diff --check`
- 确认三个 Slot 在 Classic 和 Agent 输入区均有调用点
- 确认 ToolPkg 注册捕获、解析、runtime、bridge、类型声明和 runner 使用同一事件名

[DONE]
