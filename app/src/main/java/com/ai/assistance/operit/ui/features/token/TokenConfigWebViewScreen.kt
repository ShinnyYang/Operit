package com.ai.assistance.operit.ui.features.token

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.ai.assistance.operit.ui.components.CustomScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ai.assistance.operit.ui.features.token.network.DeepseekApiConstants
import com.ai.assistance.operit.ui.features.token.webview.WebViewConfig
import kotlinx.coroutines.launch

/** Deepseek Token配置屏幕 */
@Composable
fun TokenConfigWebViewScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }

    // 创建WebView实例
    val webView = remember { WebViewConfig.createWebView(context) }

    // 添加JavaScript接口以允许WebView直接调用原生功能
    DisposableEffect(webView) {
        // 添加JavaScript接口来辅助处理外部应用链接
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun openExternalApp(url: String) {
                scope.launch {
                    try {
                        val uri = android.net.Uri.parse(url)
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("TokenConfigWebView", "无法通过JS接口打开外部应用: ${e.message}")
                    }
                }
            }
        }, "Android")
        
        onDispose {}
    }

    // 导航链接
    data class NavDestination(val title: String, val url: String, val icon: ImageVector)

    val navDestinations =
            listOf(
                    NavDestination(
                            "API 密钥",
                            DeepseekApiConstants.DEEPSEEK_API_KEYS_URL,
                            Icons.Default.Key
                    ),
                    NavDestination(
                            "用量",
                            DeepseekApiConstants.DEEPSEEK_USAGE_URL,
                            Icons.Default.Dashboard
                    ),
                    NavDestination(
                            "充值",
                            "https://platform.deepseek.com/top_up",
                            Icons.Default.CreditCard
                    ),
                    NavDestination(
                            "个人资料",
                            "https://platform.deepseek.com/profile",
                            Icons.Default.Person
                    )
            )

    // 导航到指定URL
    fun navigateTo(url: String, index: Int) {
        isLoading = true
        webView.loadUrl(url)
        selectedTabIndex = index
    }

    // 创建WebViewClient
    val webViewClient = remember {
        object : WebViewClient() {
            // 处理URL加载
            override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
            ): Boolean {
                request?.url?.let { uri ->
                    val url = uri.toString()
                    
                    // 检查是否是需要外部应用处理的URL scheme
                    if (url.startsWith("alipays:") || 
                        url.startsWith("alipay:") || 
                        url.startsWith("weixin:") ||
                        url.startsWith("weixins:") ||
                        url.contains("platformapi") ||
                        !url.startsWith("http")) {
                        
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            Log.e("TokenConfigWebView", "无法打开外部应用: ${e.message}")
                        }
                    }
                    
                    // 如果是标准http/https链接，可能是需要在外部浏览器打开的链接
                    if ((url.startsWith("http://") || url.startsWith("https://")) &&
                        (url.contains("pay") || url.contains("bank") || url.contains("login"))) {
                        // 可能是支付或登录页面，尝试在外部浏览器打开
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            Log.e("TokenConfigWebView", "无法在浏览器中打开: ${e.message}")
                        }
                    }
                }
                // 其他情况由WebView自行处理
                return false
            }

            // 处理旧版本Android
            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    val uri = android.net.Uri.parse(it)
                    if (it.startsWith("alipays:") || 
                        it.startsWith("alipay:") || 
                        it.startsWith("weixin:") ||
                        it.startsWith("weixins:") ||
                        it.contains("platformapi") ||
                        !it.startsWith("http")) {
                        
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            Log.e("TokenConfigWebView", "无法打开外部应用: ${e.message}")
                        }
                    }
                    
                    // 如果是标准http/https链接，可能是需要在外部浏览器打开的链接
                    if ((it.startsWith("http://") || it.startsWith("https://")) &&
                        (it.contains("pay") || it.contains("bank") || it.contains("login"))) {
                        // 可能是支付或登录页面，尝试在外部浏览器打开
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            Log.e("TokenConfigWebView", "无法在浏览器中打开: ${e.message}")
                        }
                    }
                }
                return false
            }
            
            override fun onReceivedSslError(
                    view: WebView?,
                    handler: android.webkit.SslErrorHandler?,
                    error: android.net.http.SslError?
            ) {
                // 创建一个对话框让用户决定是否继续访问
                val builder = android.app.AlertDialog.Builder(context)
                var message = "SSL证书验证失败，存在安全风险"
                
                // 显示更详细的错误信息
                error?.let {
                    when (it.primaryError) {
                        android.net.http.SslError.SSL_UNTRUSTED -> message += "\n证书颁发机构不受信任"
                        android.net.http.SslError.SSL_EXPIRED -> message += "\n证书已过期"
                        android.net.http.SslError.SSL_IDMISMATCH -> message += "\n主机名与证书不匹配"
                        android.net.http.SslError.SSL_NOTYETVALID -> message += "\n证书尚未生效"
                        android.net.http.SslError.SSL_DATE_INVALID -> message += "\n证书日期无效"
                        else -> message += "\n未知SSL错误"
                    }
                    message += "\n\n访问: ${it.url}"
                }
                
                builder.setTitle("安全警告")
                    .setMessage(message)
                    .setPositiveButton("继续访问") { _, _ ->
                        handler?.proceed()
                    }
                    .setNegativeButton("取消访问") { _, _ ->
                        handler?.cancel()
                    }
                    .setIcon(android.R.drawable.ic_dialog_alert)
                
                // 在UI线程上显示对话框
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    val dialog = builder.create()
                    dialog.show()
                }
            }

            // 允许混合内容（HTTP和HTTPS同时存在）
            @Suppress("deprecation")
            override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(
                        "TokenConfigWebView",
                        "Web error: $errorCode - $description, URL: $failingUrl"
                )
            }

            // Android 6.0以上的错误处理
            override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Log.e(
                            "TokenConfigWebView",
                            "Web error: ${error?.errorCode} - ${error?.description}, URL: ${request?.url}"
                    )
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { finishedUrl ->
                    Log.d("TokenConfigWebView", "Page finished loading: $finishedUrl")

                    // 页面加载完成后配置cookies
                    try {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(view, true)
                        cookieManager.flush()
                    } catch (e: Exception) {
                        Log.e("TokenConfigWebView", "Cookie error: ${e.message}")
                    }

                    // 更新状态
                    scope.launch {
                        isLoading = false
                        currentUrl = finishedUrl

                        // 更新当前选中的标签
                        navDestinations.forEachIndexed { index, destination ->
                            if (finishedUrl.contains(destination.url)) {
                                selectedTabIndex = index
                            }
                        }
                    }
                }
            }
        }
    }

    // 将WebViewClient应用到WebView
    DisposableEffect(webView) {
        webView.webViewClient = webViewClient
        
        // 添加WebChromeClient以处理新窗口请求
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            // 处理window.open
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                if (isUserGesture) {
                    // 获取新窗口的WebView
                    val transport = resultMsg?.obj as? android.webkit.WebView.WebViewTransport
                    val newWebView = WebView(context)
                    
                    // 为新窗口设置WebViewClient
                    newWebView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            request?.url?.let { uri ->
                                val url = uri.toString()
                                // 尝试在外部打开所有链接
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                    return true
                                } catch (e: Exception) {
                                    Log.e("TokenConfigWebView", "无法打开链接: ${e.message}")
                                }
                            }
                            return false
                        }
                    }
                    
                    transport?.webView = newWebView
                    resultMsg?.sendToTarget()
                    return true
                }
                return false
            }
        }
        
        // 添加JavaScript代码来捕获链接
        webView.evaluateJavascript("""
            (function() {
                // 监听所有点击事件
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    // 向上查找链接元素
                    while(target && target.tagName !== 'A') {
                        target = target.parentNode;
                    }
                    
                    if(target && target.tagName === 'A') {
                        var href = target.href;
                        // 检测支付链接
                        if(href && (
                            href.startsWith('alipay:') || 
                            href.startsWith('alipays:') || 
                            href.indexOf('platformapi') > -1 ||
                            href.indexOf('pay') > -1 ||
                            href.indexOf('bank') > -1 ||
                            target.getAttribute('target') === '_blank'
                        )) {
                            e.preventDefault();
                            Android.openExternalApp(href);
                            return false;
                        }
                    }
                }, true);
                
                // 拦截window.open
                var originalOpen = window.open;
                window.open = function(url) {
                    if(url) {
                        Android.openExternalApp(url);
                        return null;
                    }
                    return originalOpen.apply(this, arguments);
                };
            })();
        """, null)

        // 加载初始URL
        webView.loadUrl(DeepseekApiConstants.DEEPSEEK_SIGNIN_URL)

        onDispose {
            // 清理资源
            webView.stopLoading()
            webView.destroy()
        }
    }

    // UI布局
    CustomScaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // 超简洁导航栏
                Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 2.dp // 降低阴影
                ) {
                    Column {
                        HorizontalDivider(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                thickness = 0.5.dp
                        )

                        // 行式导航栏
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(60.dp)
                                                .background(Color.White),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            navDestinations.forEachIndexed { index, destination ->
                                val isSelected = selectedTabIndex == index
                                Column(
                                        modifier =
                                                Modifier.weight(1f)
                                                        .fillMaxSize()
                                                        .clickable {
                                                            navigateTo(destination.url, index)
                                                        }
                                                        .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.title,
                                            modifier = Modifier.size(24.dp),
                                            tint =
                                                    if (isSelected)
                                                            MaterialTheme.colorScheme.primary
                                                    else Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                            text = destination.title,
                                            fontSize = 12.sp,
                                            fontWeight =
                                                    if (isSelected) FontWeight.Medium
                                                    else FontWeight.Normal,
                                            color =
                                                    if (isSelected)
                                                            MaterialTheme.colorScheme.primary
                                                    else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 确保WebView始终占满整个屏幕宽度，使用更简洁的指针拦截方法
            AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                    // 移除指针拦截，因为我们已经在WebView自身设置了更可靠的触摸事件处理
                    ) // 加载指示器
            if (isLoading) {
                LinearProgressIndicator(
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                )
            }
        }
    }
}
