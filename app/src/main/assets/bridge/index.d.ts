/**
 * MCP TCP Bridge
 *
 * Creates a bridge that connects STDIO-based MCP servers to TCP clients
 */
interface BridgeConfig {
    port: number;
    host: string;
    mcpCommand: string;
    mcpArgs: string[];
    registryPath?: string;
    env?: Record<string, string>;
}
/**
 * MCP Bridge class
 */
declare class McpBridge {
    private config;
    private server;
    private mcpProcesses;
    private remoteServiceClients;
    private mcpToolsMap;
    private serviceReadyMap;
    private serviceRegistry;
    private activeConnections;
    private pendingRequests;
    private toolResponseMapping;
    private toolCallServiceMap;
    private readonly REQUEST_TIMEOUT;
    private mcpErrors;
    private restartAttempts;
    private readonly MAX_RESTART_ATTEMPTS;
    private readonly RESTART_DELAY_MS;
    constructor(config?: Partial<BridgeConfig>);
    /**
     * 注册新的MCP服务
     */
    private registerService;
    /**
     * 注销MCP服务
     */
    private unregisterService;
    /**
     * 获取已注册MCP服务列表
     */
    private getServiceList;
    /**
     * 检查服务是否激活 (运行中或已连接)
     */
    private isServiceActive;
    /**
     * 连接到远程MCP服务
     */
    private connectToRemoteService;
    /**
     * 处理远程连接关闭和重连
     */
    private handleRemoteClosure;
    /**
     * 启动特定服务的子进程
     */
    private startMcpProcess;
    /**
     * 发送请求到指定服务 (本地或远程)
     */
    private sendRequestToService;
    /**
     * 获取特定服务的MCP工具列表
     */
    private fetchMcpTools;
    /**
     * 处理来自特定服务的MCP响应数据
     */
    private handleMcpResponse;
    /**
     * 处理客户端MCP命令
     */
    private handleMcpCommand;
    /**
     * 处理工具调用请求
     */
    private handleToolCall;
    /**
     * 检查请求超时
     */
    private checkRequestTimeouts;
    /**
     * 启动TCP服务器
     */
    start(): void;
    /**
     * 关闭桥接器
     */
    shutdown(): void;
    /**
     * 从JSON-RPC请求中提取ID
     */
    private extractId;
}
export default McpBridge;
