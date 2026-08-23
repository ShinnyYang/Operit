---
topic: Thinking quality slider UI
status: in_progress
---

# Thinking quality slider UI

## 原本状况

Android Classic 和 Agent 使用了两套重复的思考程度控件。控件只显示全局数字档位，Classic 还允许编辑数字；provider 的真实映射分散在各自的请求构建代码中。

## 修正意图

使用 Material 3 风格的离散滑块，显示当前 provider 的真实映射文本。保留内部 `thinking_quality_level` 整数契约，但不再把全局档位数字暴露给用户。

轨道 active 区域使用主题主色和低速流光动效，拖动时加速，系统减少动态效果时关闭动画。

## 作用域

- 统一 provider 映射描述和请求参数来源
- 重构 Android Classic、Agent 两套思考程度控件
- 同步 Web 模型选择状态和两套 Web 输入样式
- 保留重复映射位置，不合并相同的 provider 值
- 为映射契约补充测试和实现记录

## 细化步骤

1. [映射契约](./01_mapping_contract.md)
2. [Android 滑块](./02_native_slider.md)
3. [Web 同步](./03_web_parity.md)
4. [验证记录](./04_verification.md)

## 完成记录

- [DONE] provider 映射、Android/Web 滑块和主题主色流光已实现
- [DONE] 远程 Release 构建已通过，构建提交为 `f39af4438`
- [DONE] 视觉修订已通过远程 Release 构建，构建提交为 `d41f50f90`
- [REJECTED] `3a76aa699` 虽通过构建，但实际 thumb/track 比例和渐变未通过视觉验收
- [REJECTED] `8a2ae80ad` 虽通过构建，但黑色 thumb 和满宽渐变未通过视觉验收
- [WIP] 透明外框、仅填充 active 区域和内嵌高亮 thumb 已通过 Release 构建，提交为 `49b5ba3fc`，等待视觉验收
- [WIP] Android/Web 档位标签响应式锚点布局已通过 Release 构建，提交为 `65d4859f2`，等待视觉验收
