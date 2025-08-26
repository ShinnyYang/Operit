    package com.ai.assistance.operit.provider;

/**
 * AIDL接口，定义了主应用和无障碍服务提供者之间的通信契约。
 */
    interface IAccessibilityProvider {
    /**
     * 获取UI层次结构的XML快照。
     */
        String getUiHierarchy();

    /**
     * 在指定坐标执行点击操作。
     * @param x X坐标
     * @param y Y坐标
     * @return 操作是否成功
     */
    boolean performClick(int x, int y);

    /**
     * 执行一个全局操作。
     * @param actionId 要执行的全局操作的ID (例如 AccessibilityService.GLOBAL_ACTION_BACK)
     * @return 操作是否成功
     */
    boolean performGlobalAction(int actionId);

    /**
     * 在指定坐标之间执行滑动操作。
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 结束X坐标
     * @param endY 结束Y坐标
     * @param duration 滑动持续时间（毫秒）
     * @return 操作是否成功
     */
    boolean performSwipe(int startX, int startY, int endX, int endY, long duration);
    
    /**
     * 查找当前拥有输入焦点的可编辑节点的标识符。
     * @return 节点的唯一标识符（例如，bounds字符串），如果找不到则返回null。
     */
    String findFocusedNodeId();

    /**
     * 在由其ID标识的节点上设置文本。
     * @param nodeId 节点的唯一标识符 (例如, bounds 字符串)
     * @param text 要设置的文本
     * @return 操作是否成功
     */
    boolean setTextOnNode(String nodeId, String text);

    /**
     * 检查远程无障碍服务是否已在系统设置中启用。
     * @return 如果服务已启用则返回true，否则返回false。
     */
    boolean isAccessibilityServiceEnabled();

    /**
     * 获取当前前台Activity的名称。
     * @return 当前Activity的类名，如果无法获取则返回null。
     */
    String getCurrentActivityName();

    /**
     * 注册一个回调来接收无障碍事件。
     * @param callback 要注册的回调监听器。
     */
    void registerAccessibilityEventListener(com.ai.assistance.operit.provider.IAccessibilityEventCallback callback);

    /**
     * 注销之前注册的无障碍事件回调。
     * @param callback 要注销的回调监听器。
     */
    void unregisterAccessibilityEventListener(com.ai.assistance.operit.provider.IAccessibilityEventCallback callback);
    }