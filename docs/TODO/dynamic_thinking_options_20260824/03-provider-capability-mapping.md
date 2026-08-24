# Provider capability mapping

思考参数现在按模型和协议能力生成，不再把供应商名视为统一能力集合：

- OpenRouter 使用模型目录中的 reasoning 能力、supported_efforts、mandatory 和 token 上限
- OpenCode 使用模型目录声明的 effort、budget 或协议专属 toggle
- Gemini 2.5 使用 thinkingBudget，Gemini 3 使用型号对应的 thinkingLevel
- Anthropic 旧型号使用预算，新型号使用 adaptive thinking 与 effort
- NVIDIA 按 NIM 模型 schema 写 reasoning_effort：GPT-OSS 为 low/medium/high，Nemotron 3 Super/Ultra 使用各自的 none/low/high 或 none/medium/high
- SiliconFlow 按供应商路径和模型家族判断：GLM/Hunyuan 使用 enable_thinking，Qwen3/DeepSeek V3 使用 thinking_budget，DeepSeek V4 Flash 使用 reasoning_effort
- xAI 仅对官方文档列出的 Grok 4.5、4.6 和 4.20 multi-agent 写 reasoning_effort
- OpenCode 目录模型 ID 的 provider/model 路径参与协议选择，避免把命名空间模型路由到错误 API
- 已知型号使用官方字段和档位；未列入目录的新型号按供应商/协议家族使用通用字段，不因型号未知而隐藏思考控件
- OpenCode/OpenRouter 目录能力优先用于细化参数，目录缺失时仍保留协议级通用思考参数
- [DeepSeek API 文档](https://api-docs.deepseek.com/api/create-chat-completion) 的 `reasoning_effort` 仅提供 `low`、`high`、`max` 三个真实值；`medium`/`xhigh` 是映射到 `high` 的兼容别名，因此滑块注册三档，不显示重复档位
- 选项 id 无效时请求失败，不再静默取第一项或切换协议格式

本地 llama.cpp 将 enableThinking 传入 llama.cpp chat template 的 enable_thinking 输入字段。

[DONE]
