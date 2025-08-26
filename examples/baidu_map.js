/*
METADATA
{
    "name": "Experimental_baidu_map",
    "description": "百度地图工具集合，提供AOI（兴趣区域）数据获取接口。通过调用百度地图API，支持按地理范围查询AOI边界坐标，基于位置的路线规划，助力地理信息系统应用开发和空间数据分析。",
    "tools": [
        {
            "name": "search_aoi",
            "description": "搜索百度地图兴趣区域(AOI)信息",
            "parameters": [
                {
                    "name": "keyword",
                    "description": "搜索关键词，如商场、小区名称等",
                    "type": "string",
                    "required": true
                },
                {
                    "name": "city_name",
                    "description": "城市名称，如'北京'，默认全国范围",
                    "type": "string",
                    "required": false
                }
            ]
        },
        {
            "name": "get_aoi_boundary",
            "description": "获取特定AOI的边界坐标",
            "parameters": [
                {
                    "name": "uid",
                    "description": "AOI的唯一ID",
                    "type": "string",
                    "required": true
                }
            ]
        },
        {
            "name": "planRoute",
            "description": "智能路线规划，从当前位置到指定目的地",
            "parameters": [
                {
                    "name": "destination",
                    "description": "目的地名称",
                    "type": "string",
                    "required": true
                },
                {
                    "name": "city_name",
                    "description": "城市名称，辅助目的地查找",
                    "type": "string",
                    "required": false
                },
                {
                    "name": "transport_mode",
                    "description": "交通方式：driving(驾车)、walking(步行)或transit(公交)，默认driving",
                    "type": "string",
                    "required": false
                }
            ]
        },
        {
            "name": "getAoiByName",
            "description": "通过名称直接获取AOI详细信息和边界",
            "parameters": [
                {
                    "name": "name",
                    "description": "AOI名称，如商场、景区、建筑名",
                    "type": "string",
                    "required": true
                },
                {
                    "name": "city_name",
                    "description": "城市名，如'北京'",
                    "type": "string",
                    "required": false
                },
                {
                    "name": "need_boundary",
                    "description": "是否需要获取边界坐标，默认true",
                    "type": "boolean",
                    "required": false
                }
            ]
        }
    ],
    "category": "NETWORK"
}
*/
const baiduMap = (function () {
    // 常用城市编码
    const CITY_CODES = {
        "北京": "131",
        "上海": "289",
        "广州": "257",
        "深圳": "340",
        "杭州": "179",
        "南京": "315",
        "武汉": "218",
        "成都": "75",
        "重庆": "132",
        "西安": "233",
        "全国": "1" // 默认值
    };
    // 请求头配置
    const HEADERS = {
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
        'Host': 'map.baidu.com',
        'Pragma': 'no-cache',
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'none',
        'Sec-Fetch-User': '?1',
        'Upgrade-Insecure-Requests': '1',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Edg/136.0.0.0',
        'sec-ch-ua': '"Chromium";v="136", "Microsoft Edge";v="136", "Not.A/Brand";v="99"',
        'sec-ch-ua-mobile': '?0',
        'sec-ch-ua-platform': '"Windows"',
        'Referer': 'https://map.baidu.com/'
    };
    // 日志级别配置
    const LOG_LEVELS = {
        NONE: 0, // 不输出任何日志
        ERROR: 1, // 只输出错误信息
        WARN: 2, // 输出警告和错误
        INFO: 3, // 输出普通信息、警告和错误
        DEBUG: 4, // 输出调试信息、普通信息、警告和错误
        TRACE: 5 // 输出所有日志，包括跟踪信息
    };
    // 默认日志级别
    let currentLogLevel = LOG_LEVELS.INFO;
    /**
     * 设置日志级别
     */
    function setLogLevel(level) {
        if (level >= LOG_LEVELS.NONE && level <= LOG_LEVELS.TRACE) {
            currentLogLevel = level;
        }
    }
    /**
     * 统一的日志输出函数
     */
    function logger(level, message, data) {
        if (level > currentLogLevel)
            return;
        switch (level) {
            case LOG_LEVELS.ERROR:
                if (data instanceof Error) {
                    console.error(`[错误] ${message}`, data.message);
                    // 只在TRACE级别输出堆栈
                    if (currentLogLevel >= LOG_LEVELS.TRACE) {
                        console.error(data.stack);
                    }
                }
                else {
                    console.error(`[错误] ${message}`, data ? data : '');
                }
                break;
            case LOG_LEVELS.WARN:
                console.warn(`[警告] ${message}`, data ? data : '');
                break;
            case LOG_LEVELS.INFO:
                console.log(`[信息] ${message}`, data ? data : '');
                break;
            case LOG_LEVELS.DEBUG:
                console.log(`[调试] ${message}`, data ? data : '');
                break;
            case LOG_LEVELS.TRACE:
                console.log(`[跟踪] ${message}`, data ? data : '');
                break;
        }
    }
    /**
     * 创建HTTP客户端
     * 使用OkHttp库实现网络请求
     */
    function createHttpClient() {
        return OkHttp.newBuilder()
            .connectTimeout(10000)
            .readTimeout(30000)
            .writeTimeout(15000)
            .followRedirects(true)
            .build();
    }
    /**
     * 提取JSON对象的前N层结构
     * @param obj 要处理的对象
     * @param maxDepth 最大深度，默认为3
     * @returns 只包含前N层的新对象
     */
    function extractTopLevels(obj, maxDepth = 3) {
        // 基本类型或undefined直接返回类型说明
        if (obj === undefined)
            return "undefined";
        if (typeof obj !== 'object')
            return `${typeof obj}:${String(obj).substring(0, 10)}${String(obj).length > 10 ? '...' : ''}`;
        // 达到最大深度，返回提示信息
        if (maxDepth <= 0) {
            if (Array.isArray(obj)) {
                return `[数组:${obj.length}项]`;
            }
            else {
                return `{对象:${Object.keys(obj).length}键}`;
            }
        }
        // 处理数组 - 只取前5个元素作为样本
        if (Array.isArray(obj)) {
            const sample = obj.slice(0, 5).map(item => extractTopLevels(item, maxDepth - 1));
            if (obj.length > 5) {
                sample.push(`...还有${obj.length - 5}项`);
            }
            return sample;
        }
        // 处理对象 - 只提取键的结构
        const result = {};
        for (const key of Object.keys(obj)) {
            result[key] = extractTopLevels(obj[key], maxDepth - 1);
        }
        return result;
    }
    /**
     * 对中文进行URL编码
     */
    function encodeURIComponentSafe(str) {
        try {
            return encodeURIComponent(str);
        }
        catch (e) {
            logger(LOG_LEVELS.ERROR, `编码失败:`, e);
            return str;
        }
    }
    /**
     * 使用OkHttp发起GET请求
     */
    async function httpGet(url) {
        try {
            const client = createHttpClient();
            const response = await client.get(url, HEADERS);
            if (!response.isSuccessful()) {
                throw new Error(`请求失败: ${response.statusCode} ${response.statusMessage}`);
            }
            try {
                const jsonResponse = await response.json();
                // 提取前三层键结构
                const keyStructure = extractTopLevels(jsonResponse, 3);
                logger(LOG_LEVELS.INFO, `API响应(仅前三层键结构):`, JSON.stringify(keyStructure, undefined, 2));
                return jsonResponse;
            }
            catch (e) {
                logger(LOG_LEVELS.ERROR, `解析JSON失败:`, e);
                return response.content;
            }
        }
        catch (e) {
            logger(LOG_LEVELS.ERROR, `网络请求错误:`, e);
            throw e;
        }
    }
    /**
     * 搜索百度地图AOI信息
     * 参考 https://blog.csdn.net/Jacey_cai/article/details/131524758
     */
    async function search_aoi(params) {
        var _a, _b;
        try {
            // 参数处理
            const keyword = params.keyword;
            if (!keyword) {
                throw new Error("关键词不能为空");
            }
            // 处理城市编码 - 改进版
            let cityCode = "1"; // 默认全国
            if (params.city_name) {
                // 使用动态获取城市编码功能
                cityCode = await getCityCode(params.city_name);
                logger(LOG_LEVELS.DEBUG, `城市 "${params.city_name}" 对应的编码:`, cityCode);
            }
            // 构建URL - 使用搜索API
            const encodedKeyword = encodeURIComponentSafe(keyword);
            const url = `https://map.baidu.com/?newmap=1&qt=s&da_src=searchBox.button&wd=${encodedKeyword}&c=${cityCode}`;
            logger(LOG_LEVELS.INFO, `搜索AOI: ${keyword}, 城市名称: ${params.city_name || '全国'}`);
            // 发起请求
            const result = await httpGet(url);
            // 记录完整响应以便调试
            logger(LOG_LEVELS.DEBUG, `AOI搜索结果结构:`, Object.keys(result || {}));
            // 主要结构检查 (result.result 或 result.content 或其他可能的结构)
            const dataContent = ((_a = result === null || result === void 0 ? void 0 : result.result) === null || _a === void 0 ? void 0 : _a.content) ||
                (result === null || result === void 0 ? void 0 : result.content) ||
                ((_b = result === null || result === void 0 ? void 0 : result.place_info) === null || _b === void 0 ? void 0 : _b.content) ||
                [];
            // 如果没有找到任何内容数据结构，返回空结果
            if (!dataContent || dataContent.length === 0) {
                logger(LOG_LEVELS.INFO, `搜索结果为空或格式不符合预期:`, result);
                return {
                    success: true,
                    keyword: keyword,
                    city_name: params.city_name,
                    total: 0,
                    aois: []
                };
            }
            // 尝试多种路径查找AOI数据
            let potentialAois = [];
            if (Array.isArray(dataContent)) {
                logger(LOG_LEVELS.DEBUG, `找到${dataContent.length}个潜在AOI条目`);
                // 筛选含有坐标或边界数据的地点
                potentialAois = dataContent
                    .filter(item => {
                    // 检查各种可能包含地理信息的字段
                    return (item.geo && item.geo.length > 0) ||
                        (item.x && item.y) ||
                        (item.point && item.point.x && item.point.y) ||
                        (item.ext && item.ext.detail_info && item.ext.detail_info.point) ||
                        (item.uid); // 至少有UID，后续可尝试获取边界
                })
                    .map(item => {
                    // 提取位置信息（考虑多种可能的数据结构）
                    let lng = item.x ||
                        (item.point && item.point.x) ||
                        (item.ext && item.ext.detail_info && item.ext.detail_info.point && item.ext.detail_info.point.x) ||
                        0;
                    let lat = item.y ||
                        (item.point && item.point.y) ||
                        (item.ext && item.ext.detail_info && item.ext.detail_info.point && item.ext.detail_info.point.y) ||
                        0;
                    return {
                        uid: item.uid || "",
                        name: item.name || "",
                        address: item.addr || "",
                        type: (item.std_tag && item.std_tag.primary_industry) ||
                            (item.type) ||
                            (item.catalog_name) ||
                            "",
                        has_geo_data: !!(item.geo && item.geo.length > 0),
                        center: {
                            lng: lng,
                            lat: lat
                        },
                        detail_url: (item.ext && item.ext.detail_info && item.ext.detail_info.detailUrl) || "",
                        raw_data: item // 保留原始数据以便进一步分析
                    };
                });
            }
            else if (typeof dataContent === 'object') {
                // 处理单个结果对象
                logger(LOG_LEVELS.DEBUG, `找到单个AOI条目`);
                if (dataContent.uid) {
                    let lng = dataContent.x ||
                        (dataContent.point && dataContent.point.x) ||
                        (dataContent.ext && dataContent.ext.detail_info && dataContent.ext.detail_info.point && dataContent.ext.detail_info.point.x) ||
                        0;
                    let lat = dataContent.y ||
                        (dataContent.point && dataContent.point.y) ||
                        (dataContent.ext && dataContent.ext.detail_info && dataContent.ext.detail_info.point && dataContent.ext.detail_info.point.y) ||
                        0;
                    potentialAois.push({
                        uid: dataContent.uid,
                        name: dataContent.name || "",
                        address: dataContent.addr || "",
                        type: (dataContent.std_tag && dataContent.std_tag.primary_industry) ||
                            (dataContent.type) ||
                            (dataContent.catalog_name) ||
                            "",
                        has_geo_data: !!(dataContent.geo && dataContent.geo.length > 0),
                        center: {
                            lng: lng,
                            lat: lat
                        },
                        detail_url: (dataContent.ext && dataContent.ext.detail_info && dataContent.ext.detail_info.detailUrl) || "",
                        raw_data: dataContent
                    });
                }
            }
            logger(LOG_LEVELS.DEBUG, `找到${potentialAois.length}个AOI结果`);
            return {
                success: true,
                keyword: keyword,
                city_name: params.city_name,
                total: potentialAois.length,
                aois: potentialAois
            };
        }
        catch (error) {
            logger(LOG_LEVELS.ERROR, `[search_aoi] 错误:`, error);
            logger(LOG_LEVELS.ERROR, `错误堆栈:`, error.stack);
            return {
                success: false,
                message: `搜索AOI失败: ${error.message}`,
                keyword: params.keyword,
                city_name: params.city_name
            };
        }
    }
    /**
     * 获取AOI边界坐标
     */
    async function get_aoi_boundary(params) {
        try {
            const uid = params.uid;
            if (!uid) {
                throw new Error("AOI的UID不能为空");
            }
            // 构建URL - 使用百度地图地点详情接口 (尝试两种可能的接口)
            const url = `https://map.baidu.com/?qt=ext&uid=${uid}`;
            const backupUrl = `https://map.baidu.com/?qt=inf&uid=${uid}`;
            logger(LOG_LEVELS.INFO, `获取AOI边界: ${uid}`);
            // 发起请求
            let result = await httpGet(url);
            // 记录完整响应以便调试
            logger(LOG_LEVELS.DEBUG, `AOI边界结果结构:`, Object.keys(result || {}));
            // 如果主接口没返回地理数据，尝试备用接口
            if (!result || !result.content || !result.content.geo) {
                logger(LOG_LEVELS.INFO, `主接口未返回地理数据，尝试备用接口: ${backupUrl}`);
                // 增加延迟避免请求过快
                await Tools.System.sleep(500);
                result = await httpGet(backupUrl);
                logger(LOG_LEVELS.DEBUG, `备用接口响应结构:`, Object.keys(result || {}));
            }
            // 仍然没有找到地理数据，返回失败
            if (!result || !result.content) {
                return {
                    success: false,
                    message: "未找到AOI边界数据",
                    uid: uid
                };
            }
            const content = result.content;
            // 尝试多种可能的地理数据结构
            let geoData = content.geo;
            if (!geoData && content.ext && content.ext.geo) {
                geoData = content.ext.geo;
            }
            else if (!geoData && content.geodata) {
                geoData = content.geodata;
            }
            let boundary = [];
            if (Array.isArray(geoData)) {
                // 尝试解析多边形边界
                boundary = geoData.map(point => ({
                    lng: point.x || (typeof point[0] !== 'undefined' ? point[0] : 0),
                    lat: point.y || (typeof point[1] !== 'undefined' ? point[1] : 0)
                }));
                logger(LOG_LEVELS.DEBUG, `解析到${boundary.length}个边界点`);
            }
            else if (typeof geoData === 'string') {
                // 有些接口返回字符串形式的坐标集
                try {
                    const coordPairs = geoData.split(';');
                    boundary = coordPairs.map(pair => {
                        const [x, y] = pair.split(',').map(parseFloat);
                        return { lng: x || 0, lat: y || 0 };
                    });
                    logger(LOG_LEVELS.DEBUG, `从字符串解析到${boundary.length}个边界点`);
                }
                catch (e) {
                    logger(LOG_LEVELS.ERROR, `解析字符串坐标失败:`, e);
                }
            }
            return {
                success: true,
                uid: uid,
                name: content.name || "",
                address: content.addr || "",
                center: {
                    lng: content.x || (content.point && content.point.x) || 0,
                    lat: content.y || (content.point && content.point.y) || 0
                },
                boundary: boundary,
                point_count: boundary.length,
                raw_data: content
            };
        }
        catch (error) {
            logger(LOG_LEVELS.ERROR, `[get_aoi_boundary] 错误:`, error);
            logger(LOG_LEVELS.ERROR, `错误堆栈:`, error.stack);
            return {
                success: false,
                message: `获取AOI边界失败: ${error.message}`,
                uid: params.uid
            };
        }
    }
    /**
     * 包装函数 - 统一处理所有百度地图工具函数的返回结果
     */
    async function map_wrap(func, params, successMessage, failMessage, additionalInfo = "") {
        try {
            logger(LOG_LEVELS.DEBUG, `开始执行函数: ${func.name || '匿名函数'}`);
            logger(LOG_LEVELS.TRACE, `参数:`, params);
            // 处理getCityCode参数特殊情况
            if (func === getCityCode && typeof params === 'string') {
                params = { city_name: params };
            }
            // 执行原始函数
            const result = await func(params);
            // 仅在TRACE级别输出完整结果
            logger(LOG_LEVELS.TRACE, `函数 ${func.name || '匿名函数'} 执行结果:`, result);
            // 在INFO级别只输出简化结果
            if (currentLogLevel === LOG_LEVELS.INFO && result) {
                const simplified = Object.assign(Object.assign(Object.assign({ success: result.success, message: result.message }, (result.pois ? { pois_count: result.pois.length } : {})), (result.aois ? { aois_count: result.aois.length } : {})), (result.boundary ? { boundary_points: result.boundary.length } : {}));
                logger(LOG_LEVELS.INFO, `函数 ${func.name || '匿名函数'} 执行完成:`, simplified);
            }
            // 如果原始函数已经调用了complete，就不需要再次调用
            if (result === undefined)
                return;
            // 根据结果类型处理
            if (typeof result === "boolean") {
                // 布尔类型结果
                complete({
                    success: result,
                    message: result ? successMessage : failMessage,
                    additionalInfo: additionalInfo
                });
            }
            else {
                // 数据类型结果
                complete({
                    success: result.success !== false,
                    message: result.success !== false ? successMessage : (result.message || failMessage),
                    additionalInfo: additionalInfo,
                    data: result
                });
            }
        }
        catch (error) {
            // 详细记录错误信息
            logger(LOG_LEVELS.ERROR, `函数 ${func.name || '匿名函数'} 执行失败:`, error);
            // 处理错误
            complete({
                success: false,
                message: `${failMessage}: ${error.message}`,
                additionalInfo: additionalInfo,
                error_stack: error.stack
            });
        }
    }
    /**
     * 测试和展示所有功能
     */
    async function main() {
        var _a, _b, _c;
        try {
            const results = {};
            logger(LOG_LEVELS.INFO, "========== 百度地图工具集合测试 ==========");
            // 测试AOI搜索和边界获取
            logger(LOG_LEVELS.INFO, "\n[1] 测试AOI搜索和边界获取...");
            try {
                const aoiResult = await search_aoi({
                    keyword: "颐和园",
                    city_name: "北京"
                });
                logger(LOG_LEVELS.INFO, `✓ AOI搜索成功，找到 ${aoiResult.total} 个结果`);
                if (aoiResult.aois && aoiResult.aois.length > 0) {
                    const aoi = aoiResult.aois[0];
                    logger(LOG_LEVELS.INFO, `   名称: ${aoi.name}`);
                    logger(LOG_LEVELS.INFO, `   地址: ${aoi.address}`);
                    // 获取边界
                    if (aoi.uid) {
                        const boundaryResult = await get_aoi_boundary({ uid: aoi.uid });
                        if (boundaryResult.success && boundaryResult.boundary) {
                            logger(LOG_LEVELS.INFO, `   成功获取边界坐标，共 ${boundaryResult.boundary.length} 个点`);
                            aoiResult.boundary = boundaryResult;
                        }
                    }
                }
                results.aoi_search = {
                    success: aoiResult.success,
                    total: aoiResult.total,
                    first_result: (_a = aoiResult.aois) === null || _a === void 0 ? void 0 : _a[0],
                    boundary: aoiResult.boundary
                };
            }
            catch (error) {
                logger(LOG_LEVELS.INFO, `✗ AOI搜索失败: ${error.message}`);
                results.aoi_search = { error: error.message };
            }
            await Tools.System.sleep(1000);
            // 测试高级功能 - getAoiByName
            logger(LOG_LEVELS.INFO, "\n[2] 测试高级功能 - 按名称获取AOI...");
            try {
                const aoiInfo = await getAoiByName({
                    name: "北京大学",
                    city_name: "北京"
                });
                logger(LOG_LEVELS.INFO, "✓ 通过名称获取AOI成功");
                if (aoiInfo.success && aoiInfo.aoi_info) {
                    logger(LOG_LEVELS.INFO, `   名称: ${aoiInfo.aoi_info.name}`);
                    logger(LOG_LEVELS.INFO, `   地址: ${aoiInfo.aoi_info.address}`);
                    logger(LOG_LEVELS.INFO, `   边界点数: ${((_b = aoiInfo.aoi_info.boundary) === null || _b === void 0 ? void 0 : _b.length) || 0}`);
                }
                results.advanced_feature = {
                    success: aoiInfo.success,
                    aoi_info: aoiInfo.aoi_info
                };
            }
            catch (error) {
                logger(LOG_LEVELS.INFO, `✗ 高级功能测试失败: ${error.message}`);
                results.advanced_feature = { error: error.message };
            }
            await Tools.System.sleep(1000);
            // 测试路径规划功能
            logger(LOG_LEVELS.INFO, "\n[3] 测试路径规划功能...");
            try {
                const routeResult = await planRoute({
                    destination: "故宫博物院",
                    city_name: "北京",
                    transport_mode: "driving"
                });
                if (routeResult.success) {
                    logger(LOG_LEVELS.INFO, "✓ 路径规划成功");
                    logger(LOG_LEVELS.INFO, `   目的地: ${routeResult.destination.name}`);
                    logger(LOG_LEVELS.INFO, `   地址: ${routeResult.destination.address}`);
                    logger(LOG_LEVELS.INFO, `   预估距离: ${routeResult.estimated_distance}`);
                    logger(LOG_LEVELS.INFO, `   预估时间: ${routeResult.estimated_duration}`);
                    logger(LOG_LEVELS.INFO, `   交通方式: ${routeResult.transport_mode}`);
                    logger(LOG_LEVELS.INFO, `   建议: ${routeResult.suggestion}`);
                }
                else {
                    logger(LOG_LEVELS.INFO, `✗ 路径规划失败: ${routeResult.message}`);
                }
                results.route_planning = {
                    success: routeResult.success,
                    destination: (_c = routeResult.destination) === null || _c === void 0 ? void 0 : _c.name,
                    distance: routeResult.estimated_distance,
                    duration: routeResult.estimated_duration
                };
            }
            catch (error) {
                logger(LOG_LEVELS.INFO, `✗ 路径规划测试失败: ${error.message}`);
                results.route_planning = { error: error.message };
            }
            logger(LOG_LEVELS.INFO, "\n========== 测试完成 ==========");
            // 返回测试结果摘要
            return {
                message: "百度地图功能测试完成",
                summary: "测试了AOI搜索与边界获取、按名称获取AOI和路径规划功能",
                timestamp: new Date().toISOString(),
                test_results: results
            };
        }
        catch (error) {
            return {
                success: false,
                message: `测试过程中发生错误: ${error.message}`
            };
        }
    }
    /**
     * 获取用户当前位置
     * 使用系统提供的位置API获取当前位置坐标
     */
    async function getCurrentLocation() {
        try {
            logger(LOG_LEVELS.INFO, "正在获取用户当前位置...");
            const locationResult = await Tools.System.getLocation();
            if (!locationResult) {
                logger(LOG_LEVELS.ERROR, "获取位置失败:", "未知错误");
                return undefined;
            }
            // 获取成功，返回经纬度
            return {
                lng: locationResult.longitude,
                lat: locationResult.latitude
            };
        }
        catch (error) {
            logger(LOG_LEVELS.ERROR, "获取位置出错:", error.message);
            return undefined;
        }
    }
    /**
     * 智能路线规划
     * 高级封装函数，根据用户当前位置和目的地名称，提供导航信息
     */
    async function planRoute(params) {
        try {
            // 获取用户当前位置
            const currentLocation = await getCurrentLocation();
            if (!currentLocation) {
                return {
                    success: false,
                    message: "无法获取当前位置信息"
                };
            }
            // 使用search_aoi查找目的地
            const searchResults = await search_aoi({
                keyword: params.destination,
                city_name: params.city_name
            });
            if (!searchResults.success || !searchResults.aois || searchResults.aois.length === 0) {
                return {
                    success: false,
                    message: `未能找到目的地: ${params.destination}`,
                    current_location: currentLocation
                };
            }
            const destination = searchResults.aois[0];
            const destLocation = destination.center;
            // 检查目的地坐标是否有效
            if (!destLocation || !destLocation.lng || !destLocation.lat) {
                return {
                    success: false,
                    message: `目的地坐标信息无效: ${params.destination}`,
                    current_location: currentLocation
                };
            }
            // 计算直线距离（简单估算，不是实际路程）
            const distance = calculateDistance(currentLocation.lat, currentLocation.lng, destLocation.lat, destLocation.lng);
            // 构建导航链接（使用百度地图APP或网页版导航）
            const mode = params.transport_mode || "driving";
            // 获取城市编码用于导航链接
            let cityCode = "1";
            if (params.city_name) {
                cityCode = await getCityCode(params.city_name);
            }
            const navUrl = `https://api.map.baidu.com/direction?origin=${currentLocation.lat},${currentLocation.lng}&destination=${destLocation.lat},${destLocation.lng}&mode=${mode}&region=${cityCode}&output=html`;
            return {
                success: true,
                current_location: currentLocation,
                destination: {
                    name: destination.name,
                    address: destination.address,
                    location: destLocation
                },
                estimated_distance: `${(distance / 1000).toFixed(2)}公里`,
                estimated_duration: estimateDuration(distance, mode),
                transport_mode: mode,
                navigation_url: navUrl,
                suggestion: getSuggestion(distance, mode)
            };
        }
        catch (error) {
            logger(LOG_LEVELS.ERROR, `[planRoute] 错误:`, error);
            return {
                success: false,
                message: `路线规划失败: ${error.message}`
            };
        }
    }
    // 工具函数：计算两点间距离（米）
    function calculateDistance(lat1, lng1, lat2, lng2) {
        const R = 6371000; // 地球半径，单位米
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLng = (lng2 - lng1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    // 工具函数：估算行程时间
    function estimateDuration(distance, mode) {
        // 根据不同交通方式估算时间
        switch (mode) {
            case "walking":
                // 步行速度约4-5km/h
                const walkingMinutes = Math.ceil(distance / 1000 * 15);
                return `步行约${walkingMinutes}分钟`;
            case "transit":
                // 公共交通复杂，粗略估算
                const transitMinutes = Math.ceil(distance / 1000 * 6);
                return `乘坐公共交通约${transitMinutes}分钟`;
            case "driving":
            default:
                // 驾车速度取决于路况，市区平均30km/h
                const drivingMinutes = Math.ceil(distance / 1000 * 2);
                return `驾车约${drivingMinutes}分钟`;
        }
    }
    // 工具函数：根据距离和交通方式给出建议
    function getSuggestion(distance, mode) {
        if (distance < 500) {
            return "目的地非常近，步行即可到达";
        }
        else if (distance < 2000) {
            return "距离适中，可步行或乘坐短途交通工具";
        }
        else if (distance < 5000) {
            return "距离较远，建议使用公共交通工具";
        }
        else {
            return "目的地较远，建议驾车或使用公共交通工具";
        }
    }
    /**
     * 按名称获取AOI详细信息和边界
     * 基于CSDN文章 https://blog.csdn.net/Jacey_cai/article/details/131524758
     * 实现了一个更专注于AOI获取的高级封装函数
     */
    async function getAoiByName(params) {
        try {
            logger(LOG_LEVELS.INFO, `开始通过名称查找AOI: ${params.name}`);
            // 参数预处理
            const name = params.name;
            if (!name) {
                throw new Error("AOI名称不能为空");
            }
            // 城市处理 - 改进版，支持动态获取城市编码
            let cityCode = "1"; // 默认全国
            if (params.city_name) {
                // 使用新函数动态获取城市编码
                cityCode = await getCityCode(params.city_name);
                logger(LOG_LEVELS.DEBUG, `城市 "${params.city_name}" 对应的编码:`, cityCode);
            }
            const needBoundary = params.need_boundary !== false;
            // 第一步：搜索AOI获取uid
            const searchResults = await search_aoi({
                keyword: name,
                city_name: params.city_name // 传递城市名称以便在日志中显示
            });
            if (!searchResults.success || !searchResults.aois || searchResults.aois.length === 0) {
                return {
                    success: false,
                    message: `未找到名为"${name}"的AOI`,
                    name: name,
                    city_name: params.city_name
                };
            }
            logger(LOG_LEVELS.DEBUG, `找到${searchResults.aois.length}个匹配"${name}"的AOI结果`);
            // 找出最匹配的AOI（通常是第一个结果）
            // 如果需要更精确匹配，可以在这里添加名称相似度比较逻辑
            const bestMatch = searchResults.aois[0];
            // 收集基本信息
            const aoiInfo = {
                uid: bestMatch.uid,
                name: bestMatch.name,
                address: bestMatch.address,
                type: bestMatch.type,
                center: bestMatch.center,
                has_geo_data: bestMatch.has_geo_data,
                boundary: []
            };
            // 如果需要获取边界且找到了uid
            if (needBoundary && bestMatch.uid) {
                // 增加延迟以避免请求过快被限制
                await Tools.System.sleep(1000);
                logger(LOG_LEVELS.DEBUG, `开始获取AOI边界: ${bestMatch.uid} (${bestMatch.name})`);
                try {
                    const boundaryResult = await get_aoi_boundary({ uid: bestMatch.uid });
                    if (boundaryResult.success && boundaryResult.boundary) {
                        aoiInfo.boundary = boundaryResult.boundary;
                        logger(LOG_LEVELS.DEBUG, `成功获取AOI边界，共${boundaryResult.boundary.length}个坐标点`);
                    }
                    else {
                        logger(LOG_LEVELS.DEBUG, `获取边界返回成功但未找到边界数据`);
                    }
                }
                catch (error) {
                    logger(LOG_LEVELS.ERROR, `获取AOI边界出错:`, error);
                    // 即使边界获取失败，仍然返回基本信息
                }
            }
            return {
                success: true,
                name: name,
                city_name: params.city_name,
                aoi_info: aoiInfo,
                other_matches: searchResults.aois.length > 1 ?
                    searchResults.aois.slice(1, 5).map(aoi => ({
                        uid: aoi.uid,
                        name: aoi.name,
                        address: aoi.address
                    })) : []
            };
        }
        catch (error) {
            logger(LOG_LEVELS.ERROR, `[getAoiByName] 错误:`, error);
            return {
                success: false,
                message: `获取AOI失败: ${error.message}`,
                name: params.name,
                city_name: params.city_name
            };
        }
    }
    /**
     * 根据城市名称动态获取城市编码
     * @param cityName 城市名称，如"杭州"、"南京"等
     * @returns 返回城市编码字符串，如果找不到返回默认编码"1"(全国)
     */
    async function getCityCode(cityName) {
        try {
            logger(LOG_LEVELS.INFO, `开始查询城市编码: ${cityName}`);
            // 如果已在本地映射表中，直接返回
            if (CITY_CODES[cityName]) {
                logger(LOG_LEVELS.DEBUG, `本地映射表中找到城市"${cityName}"的编码:`, CITY_CODES[cityName]);
                return CITY_CODES[cityName];
            }
            // 使用百度地图API查询城市信息
            const encodedCityName = encodeURIComponentSafe(cityName);
            const url = `https://map.baidu.com/?newmap=1&qt=s&wd=${encodedCityName}&c=1`;
            logger(LOG_LEVELS.INFO, `发送请求获取城市编码:`, url);
            const result = await httpGet(url);
            // 尝试从不同路径提取城市编码
            let cityCode = "1"; // 默认值
            // 路径1: current_city.code
            if (result && result.current_city && result.current_city.code) {
                cityCode = result.current_city.code.toString();
                logger(LOG_LEVELS.DEBUG, `从current_city中找到城市编码:`, cityCode);
            }
            // 路径2: content[].area_code 或 city_id
            else if (result && result.content && Array.isArray(result.content) && result.content.length > 0) {
                for (const item of result.content) {
                    if (item.area_code) {
                        cityCode = item.area_code.toString();
                        logger(LOG_LEVELS.DEBUG, `从content[].area_code中找到城市编码:`, cityCode);
                        break;
                    }
                    else if (item.city_id) {
                        cityCode = item.city_id.toString();
                        logger(LOG_LEVELS.DEBUG, `从content[].city_id中找到城市编码:`, cityCode);
                        break;
                    }
                }
            }
            // 路径3: result_code
            else if (result && result.result && result.result.code) {
                cityCode = result.result.code.toString();
                logger(LOG_LEVELS.DEBUG, `从result.code中找到城市编码:`, cityCode);
            }
            // 路径4: result.city_id
            else if (result && result.result && result.result.city_id) {
                cityCode = result.result.city_id.toString();
                logger(LOG_LEVELS.DEBUG, `从result.city_id中找到城市编码:`, cityCode);
            }
            // 如果没找到，尝试第二个API端点
            if (cityCode === "1") {
                // 使用城市搜索API
                const secondUrl = `https://map.baidu.com/?qt=cur&wd=${encodedCityName}`;
                logger(LOG_LEVELS.DEBUG, `未找到编码，尝试第二个API端点:`, secondUrl);
                try {
                    // 增加延迟避免请求过快
                    await Tools.System.sleep(500);
                    const secondResult = await httpGet(secondUrl);
                    if (secondResult && secondResult.current_city && secondResult.current_city.code) {
                        cityCode = secondResult.current_city.code.toString();
                        logger(LOG_LEVELS.DEBUG, `从第二个API获取到城市编码:`, cityCode);
                    }
                }
                catch (e) {
                    logger(LOG_LEVELS.ERROR, `第二个API请求失败:`, e);
                }
            }
            // 如果仍未找到，使用默认值
            if (cityCode === "1") {
                logger(LOG_LEVELS.INFO, `未能找到城市"${cityName}"的编码，使用默认编码"1"(全国)`);
            }
            else {
                // 找到编码后，可以临时添加到CITY_CODES中供本次会话使用
                CITY_CODES[cityName] = cityCode;
                logger(LOG_LEVELS.DEBUG, `已将城市"${cityName}"的编码${cityCode}添加到临时映射表`);
            }
            return cityCode;
        }
        catch (error) {
            logger(LOG_LEVELS.ERROR, `获取城市编码失败:`, error);
            return "1"; // 出错时返回默认编码(全国)
        }
    }
    return {
        // 日志控制API
        setLogLevel: (level) => setLogLevel(level),
        LOG_LEVELS: LOG_LEVELS,
        // 基础API
        search_aoi: async (params) => await map_wrap(search_aoi, params, "AOI搜索成功", "AOI搜索失败"),
        get_aoi_boundary: async (params) => await map_wrap(get_aoi_boundary, params, "获取AOI边界成功", "获取AOI边界失败"),
        // 高级封装API - 更适合AI使用
        planRoute: async (params) => await map_wrap(planRoute, params, "路线规划成功", "路线规划失败", "从您当前位置到目的地的路线"),
        // 测试函数
        main: async (params) => await map_wrap(main, params, "测试完成", "测试失败"),
        // 新增功能
        getAoiByName: async (params) => await map_wrap(getAoiByName, params, "获取AOI信息成功", "获取AOI信息失败"),
    };
})();
// 逐个导出
exports.setLogLevel = baiduMap.setLogLevel;
exports.LOG_LEVELS = baiduMap.LOG_LEVELS;
exports.search_aoi = baiduMap.search_aoi;
exports.get_aoi_boundary = baiduMap.get_aoi_boundary;
// 导出高级封装API
exports.planRoute = baiduMap.planRoute;
exports.getAoiByName = baiduMap.getAoiByName;
exports.main = baiduMap.main;
