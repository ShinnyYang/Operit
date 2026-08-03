---
title: 参数映射测试与交付
status: complete
---

# 参数映射测试与交付

## 验证范围

为映射器增加 JVM 单元测试，覆盖全局五档思考程度、思考摘要开关和模型参数保留字段。测试直接检查生成的 JSON 层级，不依赖网络或 API 密钥。

## 交付检查

- 检查 Google 和 Gemini Generic provider 共用相同的全局映射器
- 检查模型自定义思考参数不会写入 `generationConfig`
- 检查思考 Part 的解析和签名历史代码未被改变
- 审查 Git diff 与文档完成标记

本任务按仓库执行准则不运行本地测试命令。

[DONE]
