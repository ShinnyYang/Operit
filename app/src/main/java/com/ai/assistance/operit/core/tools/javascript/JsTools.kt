package com.ai.assistance.operit.core.tools.javascript

/** JavaScript Tools 对象定义 提供了一组便捷的工具调用方法，用于在JavaScript脚本中调用Android工具 */
fun getJsToolsDefinition(): String {
    return """
        // 工具调用的便捷方法
        var Tools = {
            // 文件系统操作
            Files: {
                list: (path) => toolCall("list_files", { path }),
                read: (path) => toolCall("read_file", { path }),
                // 分段读取文件内容
                readPart: (path, partIndex) => toolCall("read_file_part", { path, partIndex }),
                // 读取完整文件内容
                readFull: (path) => toolCall("read_file_full", { path }),
                write: (path, content, append) => {
                    const params = { path, content };
                    if (append !== undefined) params.append = append ? "true" : "false";
                    return toolCall("write_file", params);
                },
                deleteFile: (path, recursive) => {
                    const params = { path };
                    if (recursive !== undefined) params.recursive = recursive ? "true" : "false";
                    return toolCall("delete_file", params);
                },
                exists: (path) => toolCall("file_exists", { path }),
                move: (source, destination) => toolCall("move_file", { source, destination }),
                copy: (source, destination, recursive) => {
                    const params = { source, destination };
                    if (recursive !== undefined) params.recursive = recursive ? "true" : "false";
                    return toolCall("copy_file", params);
                },
                mkdir: (path, create_parents) => {
                    const params = { path };
                    if (create_parents !== undefined) params.create_parents = create_parents ? "true" : "false";
                    return toolCall("make_directory", params);
                },
                find: (path, pattern, options = {}) => {
                    const params = { path, pattern, ...options };
                    return toolCall("find_files", params);
                },
                info: (path) => toolCall("file_info", { path }),
                // 智能应用文件绑定
                apply: (path, content) => toolCall("apply_file", { path, content }),
                zip: (source, destination) => toolCall("zip_files", { source, destination }),
                unzip: (source, destination) => toolCall("unzip_files", { source, destination }),
                open: (path) => toolCall("open_file", { path }),
                share: (path, title) => {
                    const params = { path };
                    if (title) params.title = title;
                    return toolCall("share_file", params);
                },
                download: (url, destination) => toolCall("download_file", { url, destination }),
                convert: (sourcePath, targetPath, options = {}) => {
                    const params = {
                        source_path: sourcePath,
                        target_path: targetPath,
                        ...options
                    };
                    return toolCall("convert_file", params);
                },
                // 获取支持的文件转换格式
                getSupportedConversions: (formatType = null) => {
                    const params = formatType ? { format_type: formatType } : {};
                    return toolCall("get_supported_conversions", params);
                }
            },
            // 网络操作
            Net: {
                httpGet: (url) => toolCall("http_request", { url, method: "GET" }),
                httpPost: (url, data) => toolCall("http_request", { url, method: "POST", data }),
                visit: (url) => toolCall("visit_web", { url }),
                // 新增增强版HTTP请求
                http: (options) => toolCall("http_request", options),
                // 新增文件上传
                uploadFile: (url, files, formData = {}, options = {}) => {
                    const params = {
                        url,
                        files: Array.isArray(files) ? JSON.stringify(files) : files,
                        form_data: JSON.stringify(formData),
                        ...options
                    };
                    return toolCall("multipart_request", params);
                },
                // 新增Cookie管理
                cookies: {
                    get: (domain) => toolCall("manage_cookies", { action: "get", domain }),
                    set: (domain, cookies) => toolCall("manage_cookies", { action: "set", domain, cookies }),
                    clear: (domain) => toolCall("manage_cookies", { action: "clear", domain })
                }
            },
            // 系统操作
            System: {
                sleep: (milliseconds) => toolCall("sleep", { duration_ms: parseInt(milliseconds) }),
                getSetting: (key, namespace) => toolCall("get_system_setting", { key, namespace }),
                setSetting: (key, value, namespace) => toolCall("modify_system_setting", { key, value, namespace }),
                getDeviceInfo: () => toolCall("device_info"),
                // 使用工具包
                usePackage: (packageName) => toolCall("use_package", { package_name: packageName }),
                // 安装应用
                installApp: (path) => toolCall("install_app", { path }),
                // 卸载应用
                uninstallApp: (packageName) => toolCall("uninstall_app", { package_name: packageName }),
                startApp: (packageName, activity) => {
                    const params = { package_name: packageName };
                    if (activity) params.activity = activity;
                    return toolCall("start_app", params);
                },
                stopApp: (packageName) => toolCall("stop_app", { package_name: packageName }),
                listApps: (includeSystem) => toolCall("list_installed_apps", { include_system: !!includeSystem }),
                // 获取设备通知
                getNotifications: (limit = 10, includeOngoing = false) => 
                    toolCall("get_notifications", { limit: parseInt(limit), include_ongoing: !!includeOngoing }),
                // 获取设备位置
                getLocation: (highAccuracy = false, timeout = 10) => 
                    toolCall("get_device_location", { high_accuracy: !!highAccuracy, timeout: parseInt(timeout) }),
                shell: (command) => toolCall("execute_shell", { command }),
                // 执行终端命令 - 一次性收集输出
                terminal: (command, sessionId, timeoutMs) => {
                    const params = { command };
                    if (sessionId) params.session_id = sessionId;
                    if (timeoutMs) params.timeout_ms = timeoutMs;
                    return toolCall("execute_terminal", params);
                },
                // 执行Intent
                intent: (options) => {
                    // 支持新的单一参数对象方式和旧的多参数方式
                    if (typeof options === "object") {
                        return toolCall("execute_intent", options);
                    }
                    
                    // 旧的多参数方式支持
                    const [action, uri, pkg, component, flags, extras, type] = arguments;
                    const params = {};
                    if (action) params.action = action;
                    if (uri) params.uri = uri;
                    if (pkg) params.package = pkg;
                    if (component) params.component = component;
                    if (flags) params.flags = flags;
                    if (extras) params.extras = typeof extras === 'object' ? JSON.stringify(extras) : extras;
                    if (type) params.type = type;
                    return toolCall("execute_intent", params);
                }
            },
            // UI操作
            UI: {
                getPageInfo: () => toolCall("get_page_info"),
                tap: (x, y) => toolCall("tap", { x, y }),
                // UI自动化任务
                automateTask: (taskGoal, options = {}) => {
                    const params = {
                        task_goal: taskGoal,
                        ...options
                    };
                    return toolCall("automate_ui_task", params);
                },
                // 增强的clickElement方法，支持多种参数类型
                clickElement: function(param1, param2, param3) {
                    // 根据参数类型和数量判断调用方式
                    if (typeof param1 === 'object') {
                        // 如果第一个参数是对象，直接传递参数对象
                        return toolCall("click_element", param1);
                    } else if (arguments.length === 1) {
                        // 单参数，假定为resourceId
                        if (param1.startsWith('[') && param1.includes('][')) {
                            // 参数看起来像bounds格式 [x,y][x,y]
                            return toolCall("click_element", { bounds: param1 });
                        }
                        return toolCall("click_element", { resourceId: param1 });
                    } else if (arguments.length === 2) {
                        // 两个参数，假定为(resourceId, index)或(className, index)
                        if (param1 === 'resourceId') {
                            return toolCall("click_element", { resourceId: param2 });
                        } else if (param1 === 'className') {
                            return toolCall("click_element", { className: param2 });
                        } else if (param1 === 'bounds') {
                            return toolCall("click_element", { bounds: param2 });
                        } else {
                            return toolCall("click_element", { resourceId: param1, index: param2 });
                        }
                    } else if (arguments.length === 3) {
                        // 三个参数，假定为(type, value, index)
                        if (param1 === 'resourceId') {
                            return toolCall("click_element", { resourceId: param2, index: param3 });
                        } else if (param1 === 'className') {
                            return toolCall("click_element", { className: param2, index: param3 });
                        } else {
                            return toolCall("click_element", { resourceId: param1, className: param2, index: param3 });
                        }
                    }
                    // 默认情况
                    return toolCall("click_element", { resourceId: param1 });
                },
                // 查找UI元素方法，更详细的参数支持
                findElement: function(options) {
                    // 支持单一参数对象和多参数形式
                    if (typeof options === "object") {
                        return toolCall("find_element", options);
                    }
                    
                    // 处理多参数形式
                    const params = {};
                    if (arguments.length >= 1) params.resourceId = arguments[0];
                    if (arguments.length >= 2) params.className = arguments[1];
                    if (arguments.length >= 3) params.text = arguments[2];
                    if (arguments.length >= 4) params.index = arguments[3];
                    return toolCall("find_element", params);
                },
                setText: (text, resourceId) => {
                    const params = { text };
                    if (resourceId) params.resourceId = resourceId;
                    return toolCall("set_input_text", params);
                },
                swipe: (startX, startY, endX, endY, duration) => {
                    const params = { 
                        start_x: startX, 
                        start_y: startY, 
                        end_x: endX, 
                        end_y: endY 
                    };
                    if (duration) params.duration = duration;
                    return toolCall("swipe", params);
                },
                pressKey: (keyCode) => toolCall("press_key", { key_code: keyCode }),
            },
            // 知识查询
            Query: {
                // 查询问题库
                knowledge: (query) => toolCall("query_knowledge_library", { query })
            },
            // 计算功能
            calc: (expression) => toolCall("calculate", { expression }),
            
            // FFmpeg工具
            FFmpeg: {
                // 执行自定义FFmpeg命令
                execute: (command) => toolCall("ffmpeg_execute", { command }),
                
                // 获取FFmpeg系统信息
                info: () => toolCall("ffmpeg_info"),
                
                // 转换视频文件
                convert: (inputPath, outputPath, options = {}) => {
                    const params = {
                        input_path: inputPath,
                        output_path: outputPath,
                        ...options
                    };
                    return toolCall("ffmpeg_convert", params);
                }
            }
        };
    """.trimIndent()
}
