# 共享流结束原因

## 旧实现

`HotStream.share` 把上游异常作为 `Completion` 的 cause 发送，所有 `collect` 都会重新抛出该异常。

## 新实现

共享流公开只读的 `completionCause`。默认行为仍然向收集器传播异常；聊天消息共享流关闭传播，主消息处理订阅读取 `completionCause` 后显式抛出，旁观订阅者只接收已缓存内容并正常结束。

## 验证

- `HotStreamTest` 保留默认异常传播测试
- 新增关闭传播测试，验证部分内容和结束原因同时保留

[DONE]
