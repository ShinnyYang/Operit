/* METADATA
{
    "name": "ai_computer",
    "description": "提供AI电脑工具，用于与桌面环境进行交互。",
    "tools": [
        {
            "name": "computer_get_tabs",
            "description": "获取所有打开的电脑标签页。",
            "parameters": []
        },
        {
            "name": "computer_switch_to_tab",
            "description": "切换到电脑标签页。",
            "parameters": [
                { "name": "tab_id", "description": "标签页ID", "type": "string", "required": false },
                { "name": "tab_index", "description": "标签页索引", "type": "number", "required": false }
            ]
        },
        {
            "name": "computer_open_desktop",
            "description": "打开一个新的电脑桌面主页标签。",
            "parameters": []
        },
        {
            "name": "computer_open_browser",
            "description": "在电脑中打开一个新的浏览器标签。",
            "parameters": [
                { "name": "url", "description": "要打开的URL", "type": "string", "required": false }
            ]
        },
        {
            "name": "computer_get_page_info",
            "description": "获取当前页面的可交互元素，每个元素都有一个interaction_id。",
            "parameters": []
        },
        {
            "name": "computer_click_element",
            "description": "点击电脑页面上的一个元素。",
            "parameters": [
                { "name": "interaction_id", "description": "元素的交互ID", "type": "string", "required": true }
            ]
        },
        {
            "name": "computer_scroll_by",
            "description": "在电脑当前激活的标签页中，滚动页面。",
            "parameters": [
                { "name": "x", "description": "水平滚动距离", "type": "number", "required": true },
                { "name": "y", "description": "垂直滚动距离", "type": "number", "required": true }
            ]
        },
        {
            "name": "computer_input_text",
            "description": "在电脑页面上的输入框中输入文本。",
            "parameters": [
                { "name": "interaction_id", "description": "元素的交互ID", "type": "string", "required": true },
                { "name": "text", "description": "要输入的文本", "type": "string", "required": true }
            ]
        },
        {
            "name": "computer_close_tab",
            "description": "关闭指定的电脑标签页。",
            "parameters": [
                { "name": "tab_id", "description": "标签页ID", "type": "string", "required": false },
                { "name": "tab_index", "description": "标签页索引", "type": "number", "required": false }
            ]
        },
        {
            "name": "computer_go_back",
            "description": "在电脑浏览器中返回到上一个页面。",
            "parameters": []
        }
    ],
    "category": "UI_AUTOMATION"
}
*/

const AIComputerTools = (function () {

    interface ToolResponse {
        success: boolean;
        message: string;
        data?: any;
    }

    async function computer_get_tabs(params: {}): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_get_tabs', params); }
    async function computer_switch_to_tab(params: { tab_id?: string, tab_index?: number }): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_switch_to_tab', params); }
    async function computer_open_desktop(params: {}): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_open_desktop', params); }
    async function computer_open_browser(params: { url?: string }): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_open_browser', params); }
    async function computer_get_page_info(params: {}): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_get_page_info', params); }
    async function computer_click_element(params: { interaction_id: string }): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_click_element', params); }
    async function computer_scroll_by(params: { x: number, y: number }): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_scroll_by', params); }
    async function computer_input_text(params: { interaction_id: string, text: string }): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_input_text', params); }
    async function computer_close_tab(params: { tab_id?: string, tab_index?: number }): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_close_tab', params); }
    async function computer_go_back(params: {}): Promise<ToolResponse> { return await toolCall('ai_computer', 'computer_go_back', params); }

    async function wrapToolExecution<P>(func: (params: P) => Promise<ToolResponse>, params: P) {
        try {
            const result = await func(params);
            complete(result);
        } catch (error: any) {
            console.error(`Tool ${func.name} failed unexpectedly`, error);
            complete({
                success: false,
                message: `工具执行时发生意外错误: ${error.message}`,
            });
        }
    }

    async function main() {
        console.log("AI Computer Tools package main function. This is for testing and demonstration.");
        complete({ success: true, message: "AI Computer Tools main finished." });
    }

    return {
        computer_get_tabs: (params: {}) => wrapToolExecution(computer_get_tabs, params),
        computer_switch_to_tab: (params: { tab_id?: string, tab_index?: number }) => wrapToolExecution(computer_switch_to_tab, params),
        computer_open_desktop: (params: {}) => wrapToolExecution(computer_open_desktop, params),
        computer_open_browser: (params: { url?: string }) => wrapToolExecution(computer_open_browser, params),
        computer_get_page_info: (params: {}) => wrapToolExecution(computer_get_page_info, params),
        computer_click_element: (params: { interaction_id: string }) => wrapToolExecution(computer_click_element, params),
        computer_scroll_by: (params: { x: number, y: number }) => wrapToolExecution(computer_scroll_by, params),
        computer_input_text: (params: { interaction_id: string, text: string }) => wrapToolExecution(computer_input_text, params),
        computer_close_tab: (params: { tab_id?: string, tab_index?: number }) => wrapToolExecution(computer_close_tab, params),
        computer_go_back: (params: {}) => wrapToolExecution(computer_go_back, params),
        main,
    };
})();

exports.computer_get_tabs = AIComputerTools.computer_get_tabs;
exports.computer_switch_to_tab = AIComputerTools.computer_switch_to_tab;
exports.computer_open_desktop = AIComputerTools.computer_open_desktop;
exports.computer_open_browser = AIComputerTools.computer_open_browser;
exports.computer_get_page_info = AIComputerTools.computer_get_page_info;
exports.computer_click_element = AIComputerTools.computer_click_element;
exports.computer_scroll_by = AIComputerTools.computer_scroll_by;
exports.computer_input_text = AIComputerTools.computer_input_text;
exports.computer_close_tab = AIComputerTools.computer_close_tab;
exports.computer_go_back = AIComputerTools.computer_go_back;
exports.main = AIComputerTools.main; 