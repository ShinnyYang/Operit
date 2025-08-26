 package com.ai.assistance.operit.ui.features.settings.screens

import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ai.assistance.operit.ui.features.token.webview.WebViewConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneClickConfigWebViewScreen(
    onBackPressed: () -> Unit,
    onConfigCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("https://api.cccielo.cn/") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var isGettingApiKey by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }
    var accessToken by remember { mutableStateOf<String?>(null) }
    var hasCleared by remember { mutableStateOf(false) } // 标记是否已清除过
    
    // 创建WebView实例，确保使用全新的会话
    val webView = remember { 
        // 先全局清除WebView数据
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.removeAllCookie()
        cookieManager.flush()
        
        WebViewConfig.createWebView(context).apply {
            // 清除所有数据，确保是全新的会话
            clearCache(true)
            clearHistory()
            clearFormData()
            clearMatches()
            clearSslPreferences()
            
            // 清除WebView存储
            settings.databaseEnabled = false
            settings.domStorageEnabled = false
            clearCache(true)
            
            // 重新启用必要的设置
            settings.databaseEnabled = true
            settings.domStorageEnabled = true
        }
    }
    
    // 每次进入时强制清除所有状态和缓存
    LaunchedEffect(Unit) {
        // 清除所有状态
        isLoggedIn = false
        userId = null
        accessToken = null
        isGettingApiKey = false
        hasCleared = false // 重置清除标志，确保会执行JavaScript清除
        
        // 再次清除WebView数据（确保彻底清除）
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.removeAllCookie()
        cookieManager.flush()
        
        // 清除WebView缓存和存储
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        webView.clearMatches()
        webView.clearSslPreferences()
        
        delay(500) // 给清除操作一些时间
    }
    
    // OkHttpClient for API calls
    val okHttpClient = remember { 
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // 获取Token列表
    fun getTokenList() {
        if (userId == null || accessToken == null) return
        
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        Log.d("OneClickConfig", "获取Token列表")
                        
                        val getTokensRequest = Request.Builder()
                            .url("https://api.cccielo.cn/api/token/")
                            .header("Authorization", accessToken!!)
                            .header("New-Api-User", userId.toString())
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .get()
                            .build()
                        
                        val response = okHttpClient.newCall(getTokensRequest).execute()
                        val responseBody = response.body?.string() ?: ""
                        Log.d("OneClickConfig", "获取Token列表响应: $responseBody")
                        
                        if (response.isSuccessful) {
                            val responseData = JSONObject(responseBody)
                            if (responseData.has("success") && responseData.getBoolean("success")) {
                                if (responseData.has("data")) {
                                    val data = responseData.getJSONObject("data") // data是对象，不是数组
                                    if (data.has("items")) {
                                        val items = data.getJSONArray("items") // 真正的Token列表在items中
                                        if (items.length() > 0) {
                                            // 获取最新的Token（通常是最后一个）
                                            val latestToken = items.getJSONObject(items.length() - 1)
                                            if (latestToken.has("key")) {
                                                val tokenKey = latestToken.getString("key")
                                                Log.d("OneClickConfig", "找到Token: ${tokenKey.take(10)}...")
                                                tokenKey
                                            } else {
                                                Log.e("OneClickConfig", "Token对象中没有key字段: $latestToken")
                                                null
                                            }
                                        } else {
                                            Log.e("OneClickConfig", "Token列表为空")
                                            null
                                        }
                                    } else {
                                        Log.e("OneClickConfig", "data对象中没有items字段")
                                        null
                                    }
                                } else {
                                    Log.e("OneClickConfig", "获取Token列表响应中没有data字段")
                                    null
                                }
                            } else {
                                Log.e("OneClickConfig", "获取Token列表失败: $responseBody")
                                null
                            }
                        } else {
                            Log.e("OneClickConfig", "获取Token列表HTTP错误: ${response.code} - $responseBody")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("OneClickConfig", "获取Token列表失败", e)
                        null
                    }
                }
                
                if (result != null) {
                    snackbarHostState.showSnackbar("API密钥获取成功！")
                    delay(500)
                    onConfigCreated(result)
                } else {
                    snackbarHostState.showSnackbar("未找到可用的API密钥，请手动创建")
                }
            } catch (e: Exception) {
                Log.e("OneClickConfig", "获取Token时出错", e)
                snackbarHostState.showSnackbar("获取密钥失败: ${e.message}")
            }
        }
    }
    
    // 通过API创建Token
    fun createApiToken() {
        if (isGettingApiKey || userId == null || accessToken == null) return
        isGettingApiKey = true
        
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        // 使用POST /api/token/ 创建新的Token
                        val requestBody = JSONObject().apply {
                            put("name", "Operit自动配置密钥")
                            put("remain_quota", -1) // 无限制配额
                            put("expired_time", -1) // 永不过期
                            put("unlimited_quota", true) // 明确设置为无限额度
                            put("model_limits_enabled", false) // 不限制模型
                            put("model_limits", "") // 空模型限制
                            put("allow_ips", "") // 允许所有IP
                        }
                        
                        Log.d("OneClickConfig", "准备创建Token - 用户ID: $userId, Access Token: ${accessToken?.take(10)}...")
                        
                        val createTokenRequest = Request.Builder()
                            .url("https://api.cccielo.cn/api/token/")
                            .header("Authorization", accessToken!!)
                            .header("New-Api-User", userId.toString()) // 确保转换为字符串
                            .header("Content-Type", "application/json")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                            .build()
                        
                        Log.d("OneClickConfig", "发送请求头: Authorization=${accessToken?.take(10)}..., New-Api-User=$userId")
                        
                        val response = okHttpClient.newCall(createTokenRequest).execute()
                        val responseBody = response.body?.string() ?: ""
                        Log.d("OneClickConfig", "创建Token响应: $responseBody")
                        
                        if (response.isSuccessful) {
                            val responseData = JSONObject(responseBody)
                            if (responseData.has("success") && responseData.getBoolean("success")) {
                                Log.d("OneClickConfig", "Token创建成功，现在获取Token列表")
                                // 创建成功后，获取Token列表来得到新创建的Token
                                "TOKEN_CREATED_SUCCESS" // 返回标记，表示需要获取Token列表
                            } else {
                                Log.e("OneClickConfig", "创建Token失败: $responseBody")
                                null
                            }
                        } else {
                            Log.e("OneClickConfig", "HTTP错误: ${response.code} - $responseBody")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("OneClickConfig", "API调用失败", e)
                        null
                    }
                }
                
                if (result != null) {
                    if (result == "TOKEN_CREATED_SUCCESS") {
                        // Token创建成功，现在获取Token列表
                        Log.d("OneClickConfig", "Token创建成功，获取Token列表")
                        getTokenList()
                    } else {
                        // 直接返回了Token
                        snackbarHostState.showSnackbar("API密钥创建成功！")
                        delay(500)
                        onConfigCreated(result)
                    }
                } else {
                    // 如果创建失败，尝试获取现有的token列表
                    Log.d("OneClickConfig", "创建Token失败，尝试获取现有Token列表")
                    getTokenList()
                }
            } catch (e: Exception) {
                Log.e("OneClickConfig", "创建密钥时出错", e)
                snackbarHostState.showSnackbar("出现错误: ${e.message}")
            } finally {
                isGettingApiKey = false
            }
        }
    }
    
    // 获取用户Access Token
    fun getUserAccessToken() {
        if (isGettingApiKey || userId == null) return
        
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    try {
                        // 获取cookies用于认证
                        val cookieManager = CookieManager.getInstance()
                        val cookies = cookieManager.getCookie("https://api.cccielo.cn")
                        
                        if (cookies == null) {
                            Log.e("OneClickConfig", "未找到登录信息")
                            return@withContext null
                        }
                        
                        Log.d("OneClickConfig", "获取Access Token")
                        Log.d("OneClickConfig", "Cookies: ${cookies.take(100)}...")
                        Log.d("OneClickConfig", "用户ID: $userId")
                        
                        // 使用GET /api/user/token 获取用户级别的Access Token
                        // 根据API文档，这个接口也需要New-Api-User头部
                        val getTokenRequest = Request.Builder()
                            .url("https://api.cccielo.cn/api/user/token")
                            .header("Cookie", cookies)
                            .header("New-Api-User", userId.toString()) // 添加必需的New-Api-User头部
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .get()
                            .build()
                        
                        Log.d("OneClickConfig", "发送获取Access Token请求")
                        Log.d("OneClickConfig", "请求头: Cookie = ${cookies.take(100)}...")
                        Log.d("OneClickConfig", "请求头: New-Api-User = $userId")
                        Log.d("OneClickConfig", "请求头: User-Agent = Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        
                        val response = okHttpClient.newCall(getTokenRequest).execute()
                        val responseBody = response.body?.string() ?: ""
                        
                        Log.d("OneClickConfig", "获取Access Token - HTTP状态码: ${response.code}")
                        Log.d("OneClickConfig", "获取Access Token - 响应体: $responseBody")
                        
                        if (response.isSuccessful) {
                            val responseData = JSONObject(responseBody)
                            if (responseData.has("success") && responseData.getBoolean("success")) {
                                // API返回的data字段直接是token字符串，不是JSON对象
                                responseData.getString("data")
                            } else {
                                Log.e("OneClickConfig", "获取Access Token失败: $responseBody")
                                null
                            }
                        } else {
                            Log.e("OneClickConfig", "获取Access Token HTTP错误: ${response.code} - $responseBody")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("OneClickConfig", "获取Access Token失败", e)
                        null
                    }
                }
                
                if (result != null) {
                    accessToken = result
                    Log.d("OneClickConfig", "Access Token获取成功: ${result.take(10)}...")
                    // 现在可以创建API Token了
                    createApiToken()
                } else {
                    snackbarHostState.showSnackbar("获取Access Token失败，请重试")
                }
            } catch (e: Exception) {
                Log.e("OneClickConfig", "获取Access Token时出错", e)
                snackbarHostState.showSnackbar("出现错误: ${e.message}")
            }
        }
    }
    
    // JavaScript接口
    class WebViewJavaScriptInterface {
        @JavascriptInterface
        fun onLoginDetected(userIdFromJs: String) {
            Log.d("OneClickConfig", "检测到用户已登录，用户ID: $userIdFromJs")
            scope.launch {
                isLoggedIn = true
                userId = userIdFromJs
                snackbarHostState.showSnackbar("登录成功，正在获取API密钥...")
                getUserAccessToken()
            }
        }
        
        @JavascriptInterface
        fun log(message: String) {
            Log.d("OneClickConfig", "JS日志: $message")
        }
    }
    
    // 检查登录状态
    fun checkLoginStatus(webView: WebView?) {
        webView?.evaluateJavascript("""
            (function() {
                try {
                    Android.log('检查登录状态...');
                    
                    // 检查各种登录标志
                    var isLoggedIn = false;
                    var userId = null;
                    
                    // 检查URL
                    if (window.location.href.includes('/panel') || 
                        window.location.href.includes('/dashboard') ||
                        window.location.href.includes('/home') ||
                        window.location.href.includes('/user')) {
                        isLoggedIn = true;
                    }
                    
                    // 检查页面元素
                    var userElements = document.querySelectorAll('[class*="user"], [class*="profile"], [class*="logout"], [class*="dashboard"]');
                    if (userElements.length > 0) {
                        isLoggedIn = true;
                    }
                    
                    // 检查localStorage中的用户信息
                    for (var key in localStorage) {
                        var value = localStorage[key];
                        if (key.includes('user') || key.includes('auth') || key.includes('login')) {
                            try {
                                var userData = JSON.parse(value);
                                if (userData && (userData.id || userData.userId || userData.user_id)) {
                                    userId = String(userData.id || userData.userId || userData.user_id);
                                    isLoggedIn = true;
                                    Android.log('从localStorage找到用户ID: ' + userId);
                                    break;
                                }
                            } catch (e) {
                                // 尝试直接解析为数字
                                if (value && value.match(/^\d+$/)) {
                                    userId = value;
                                    isLoggedIn = true;
                                    Android.log('从localStorage找到纯数字用户ID: ' + userId);
                                    break;
                                }
                            }
                        }
                    }
                    
                    // 检查sessionStorage
                    if (!userId) {
                        for (var key in sessionStorage) {
                            var value = sessionStorage[key];
                            if (key.includes('user') || key.includes('auth') || key.includes('login')) {
                                try {
                                    var userData = JSON.parse(value);
                                    if (userData && (userData.id || userData.userId || userData.user_id)) {
                                        userId = String(userData.id || userData.userId || userData.user_id);
                                        isLoggedIn = true;
                                        Android.log('从sessionStorage找到用户ID: ' + userId);
                                        break;
                                    }
                                } catch (e) {
                                    if (value && value.match(/^\d+$/)) {
                                        userId = value;
                                        isLoggedIn = true;
                                        Android.log('从sessionStorage找到纯数字用户ID: ' + userId);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    // 尝试从页面中提取用户ID
                    if (isLoggedIn && !userId) {
                        // 查找可能包含用户ID的元素
                        var elements = document.querySelectorAll('[data-user-id], [data-userid], .user-id, #user-id, [user-id]');
                        for (var i = 0; i < elements.length; i++) {
                            var id = elements[i].getAttribute('data-user-id') || 
                                    elements[i].getAttribute('data-userid') || 
                                    elements[i].getAttribute('user-id') ||
                                    elements[i].textContent;
                            if (id && id.match(/^\d+$/)) {
                                userId = id;
                                Android.log('从页面元素找到用户ID: ' + userId);
                                break;
                            }
                        }
                    }
                    
                    // 尝试从全局变量中获取
                    if (isLoggedIn && !userId) {
                        if (typeof window.user !== 'undefined' && window.user && window.user.id) {
                            userId = String(window.user.id);
                            Android.log('从全局变量找到用户ID: ' + userId);
                        } else if (typeof window.userId !== 'undefined' && window.userId) {
                            userId = String(window.userId);
                            Android.log('从全局变量userId找到用户ID: ' + userId);
                        }
                    }
                    
                    if (isLoggedIn) {
                        Android.log('用户已登录，用户ID: ' + (userId || '未找到'));
                        Android.onLoginDetected(userId || '1'); // 如果找不到用户ID，使用默认值1
                    } else {
                        Android.log('用户未登录');
                    }
                } catch (e) {
                    Android.log('检查登录状态时出错: ' + e.toString());
                }
            })();
        """.trimIndent(), null)
    }
    
    // 添加JavaScript接口
    DisposableEffect(webView) {
        webView.addJavascriptInterface(WebViewJavaScriptInterface(), "Android")
        
        onDispose {
            webView.removeJavascriptInterface("Android")
        }
    }
    
    // 创建WebViewClient
    val webViewClient = remember {
        object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.let { uri ->
                    currentUrl = uri.toString()
                }
                return false
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                currentUrl = url ?: ""
                
                // 只在首次进入时清除一次存储数据
                if (!hasCleared) {
                    hasCleared = true
                    view?.evaluateJavascript("""
                        try {
                            // 清除localStorage
                            if (typeof localStorage !== 'undefined') {
                                localStorage.clear();
                                Android.log('localStorage已清除');
                            }
                            // 清除sessionStorage
                            if (typeof sessionStorage !== 'undefined') {
                                sessionStorage.clear();
                                Android.log('sessionStorage已清除');
                            }
                            // 清除所有cookies
                            if (typeof document !== 'undefined' && document.cookie) {
                                document.cookie.split(";").forEach(function(c) { 
                                    document.cookie = c.replace(/^ +/, "").replace(/=.*/, "=;expires=" + new Date().toUTCString() + ";path=/"); 
                                });
                                Android.log('cookies已清除');
                            }
                            Android.log('首次进入：所有本地存储数据已清除');
                        } catch (e) {
                            Android.log('清除存储数据时出错: ' + e.toString());
                        }
                    """.trimIndent(), null)
                }
                
                // 每次页面加载完成后检查登录状态
                if (!isLoggedIn) {
                    scope.launch {
                        delay(1000) // 等待页面完全加载
                        checkLoginStatus(view)
                    }
                }
            }
        }
    }
    
    // 设置WebView
    LaunchedEffect(webView) {
        webView.webViewClient = webViewClient
        webView.loadUrl("https://api.cccielo.cn/")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "一键配置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isLoggedIn) "正在获取API密钥..." else "请登录或注册账号",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isLoggedIn && !isGettingApiKey) {
                        TextButton(
                            onClick = { getUserAccessToken() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("重新获取密钥")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 提示信息 - 移到顶部
            if (!isLoading && !isLoggedIn) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "请先登录或注册，登录后系统将自动获取API密钥",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // WebView 区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
                
                // 加载指示器
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                // 获取密钥中的提示
                if (isGettingApiKey) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("正在获取API密钥...")
                            }
                        }
                    }
                }
            }
        }
    }
}