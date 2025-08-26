# Operit AI 用户指南

<div align="center">
  <strong>简体中文</strong> | <a href="./USER_GUIDE(E).md">English</a>
</div>

<p align="center">
  <img src="../app/src/main/res/playstore-icon.png" width="120" height="120" alt="Operit Logo">
</p>
<p align="center">
  <strong>从这里开始，你将看到无数用户的创造力。</strong><br>
  <strong>从这里开始，你将展示你的创造力！</strong>
</p>

---

<h2 id="toc">📖 目录</h2>

- [✨ 简介](#section-1)
- [🗺️ 基本流程讲解](#section-2)
  - [初次使用/试用](#section-2-1)
  - [如何打包AI写好的WEB应用](#section-2-2)
  - [如何配置自己的API/其他模型](#section-2-3)
    - [配置自己的DeepSeek API](#section-2-3-1)
    - [切换其他AI模型](#section-2-3-2)
  - [Shizuku授权流程](#section-2-4)
  - [包管理与MCP使用说明](#section-2-5)
    - [包管理启用过程](#section-2-5-1)
    - [一键快捷配置环境](#section-2-5-2)
    - [MCP配置流程](#section-2-5-3)
- [🚀 拓展用法实操](#section-3)
  - [🧰 开箱即用](#section-3-1)
  - [📦 拓展包](#section-3-2)
  - [🛠️ 核心工具](#section-3-3)
  - [🛒 MCP市场](#section-3-4)
    - [MCP工作机制](#section-3-4-1)
    - [MCP下载和部署机制](#section-3-4-2)
    - [MCP常见问题](#section-3-4-3)
- [❔ 常见问题解答](#section-4)
  - [MCP包问题排查](#section-4-1)
- [🎉 加入我们](#section-5)
- [💡 许愿池](#section-6)
- [📜 朝花夕拾 (旧版本问题解答)](#section-7)

---

<h2 id="section-1" style="display: flex; justify-content: space-between; align-items: center;"><span>✨ 简介</span><a href="#toc" style="text-decoration: none; font-size: 0.8em;" title="返回目录">🔝</a></h2>

欢迎使用 **Operit AI**！本指南旨在帮助您快速上手，并充分利用 Operit AI 的强大功能，将您的手机变成一个真正的智能助手。

>*此文档最新更新：2025/6/17*

<div STYLE="page-break-after: always;"></div>

<h2 id="section-2" style="display: flex; justify-content: space-between; align-items: center;"><span>🗺️ 基本流程讲解</span><a href="#toc" style="text-decoration: none; font-size: 0.8em;" title="返回目录">🔝</a></h2>

<h3 id="section-2-1" style="display: flex; justify-content: space-between; align-items: center;"><span>初次使用/试用</span><a href="#section-2" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>

初次使用 Operit AI 时，您需要进行简单的设置以授予应用必要权限，从而解锁全部功能。以下是详细步骤：
 >演示版本`1.1.5`，之后的版本将在右上角加入 '跳过'

<table style="width: 100%;">
  <thead>
    <tr>
      <th style="width: 25%; text-align: left;">步骤 (Step)</th>
      <th style="width: 45%; text-align: left;">说明与操作 (Explanation & Action)</th>
      <th style="width: 30%; text-align: left;">截图 (Screenshot)</th>
    </tr>
  </thead>
  <tbody>
    <tr id="step-1">
      <td style="vertical-align: top;"><strong>步骤一：阅读我们的协议</strong></td>
      <td style="vertical-align: top; padding-right: 15px;">
        <em>在此声明，数据只在本地和您所提供的API平台之间流动，我们并没有任何服务器</em>
      </td>
      <td style="vertical-align: top;">
        <a href="assets/user_step/step_for_frist_1.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/user_step/step_for_frist_1.jpg" alt="用户协议及隐私政策" height="280">
        </a>
      </td>
    </tr>
    <tr id="step-2">
      <td style="vertical-align: top;"><strong>步骤二：在系统设置中找到并启用 Operit AI</strong></td>
      <td style="vertical-align: top; padding-right: 15px;">
        您可以直接跳转至设置相关页面，也可能需要在"已安装的应用"列表中，找到 Operit AI 并点击进入。<br>
        <em>在设置列表中找到"Operit AI"，点击以进行下一步配置。</em>
        <blockquote><code>1.1.6</code>以后的版本可以跳过引导</blockquote>
        <blockquote>如果你卡在这里无法授权某些权限，请退出软件并熄屏重试，来跳过引导。因为这个引导仅在第一次进入时有效。</blockquote>
      </td>
      <td style="vertical-align: top;">
        <a href="assets/user_step/step_for_frist_2.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/user_step/step_for_frist_2.jpg" alt="权限引导" height="280">
        </a>
      </td>
    </tr>
    <tr id="step-3">
      <td style="vertical-align: top;"><strong>步骤三：设置用户偏好</strong></td>
      <td style="vertical-align: top; padding-right: 15px;">
        在第一次我们会建议您去设置，这将会决定AI眼中的你是什么样的。
        <blockquote>后续可通过<code>设置>用户偏好设置</code>进行更改，支持自定义</blockquote>
        <em>打开"使用 Operit AI"的开关，并在系统弹出的确认窗口中点击"允许"。这是安全警告，Operit AI 会妥善使用此权限。</em>
      </td>
      <td style="vertical-align: top;">
        <a href="assets/user_step/step_for_frist_3.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/user_step/step_for_frist_3.jpg" alt="偏好配置" height="280">
        </a>
      </td>
    </tr>
    <tr id="step-4">
      <td style="vertical-align: top;"><strong>步骤四：配置自己的API</strong></td>
      <td style="vertical-align: top; padding-right: 15px;">
        完成配置后，您就可以返回 Operit AI，开始您的智能助手之旅了！当然您也可以通过使用作者默认API来获得一次完整的体验（每天）。<br>
        <em>配置API后开始使用即可。</em>
        <blockquote>AI的API和模型可在<code>设置>AI模型配置>模型与参数配置/功能模型配置</code>中更改</blockquote>
        <blockquote>模型提示词可在<code>设置>个性化>模型提示词设置</code>处更改，一些模型参数的设置也在这</blockquote>
      </td>
      <td style="vertical-align: top;">
        <a href="assets/user_step/step_for_frist_4.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/user_step/step_for_frist_4.jpg" alt="配置API后开始使用" height="280">
        </a>
      </td>
    </tr>
  </tbody>
</table>

<h3 id="section-2-2" style="display: flex; justify-content: space-between; align-items: center;"><span>如何打包AI写好的WEB应用</span><a href="#section-2" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>
<em>以下步骤将演示如何打包由AI完成开发的Web应用。（图片可点击放大）</em>
<br>
<table style="width: 100%;">
  <tbody>
    <tr>
      <td style="text-align: center; padding: 8px; width: 50%;">步骤一：进入打包页面</td>
      <td style="text-align: center; padding: 8px; width: 50%;">步骤二：开始打包</td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/teach_step/1-1.png" target="_blank" rel="noopener noreferrer">
          <img src="assets/teach_step/1-1.png" alt="进入打包" style="max-height: 280px; max-width: 100%; height: auto;">
        </a>
      </td>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/teach_step/1-2.png" target="_blank" rel="noopener noreferrer">
          <img src="assets/teach_step/1-2.png" alt="开始打包" style="max-height: 280px; max-width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px;">步骤三：设置应用信息</td>
      <td style="text-align: center; padding: 8px;">步骤四：下载或分享</td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/teach_step/1-3.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/teach_step/1-3.jpg" alt="设置信息" style="max-height: 280px; max-width: 100%; height: auto;">
        </a>
      </td>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/teach_step/1-4.jpg" target="_blank" rel="noopener noreferrer">
         <img src="assets/teach_step/1-4.jpg" alt="下载分享" style="max-height: 280px; max-width: 100%; height: auto;">
        </a>
      </td>
    </tr>
  </tbody>
</table>
<br>

<h3 id="section-2-3" style="display: flex; justify-content: space-between; align-items: center;"><span>如何配置自己的API/其他模型</span><a href="#section-2" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>

<h4 id="section-2-3-1" style="display: flex; justify-content: space-between; align-items: center;"><span>配置自己的DeepSeek API</span><a href="#section-2-3" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>
<em>按照以下步骤，您可以轻松配置好DeepSeek的API，以便在Operit AI中使用。</em>
<p>如果您想配置自己的API（而非使用应用内提供的默认接口），可以参照以下流程：</p>

<table style="width: 100%;">
  <tbody>
    <tr>
      <td colspan="3" style="padding-bottom: 8px;">
        <h5>步骤一：登录/注册 DeepSeek 开放平台</h5>
        <p>首先，您需要访问 DeepSeek 开放平台 并登录您的账户。我们已在软件内部嵌入了deepseek开放平台。如果您是第一次使用，需要先完成注册。</p>
      </td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px; vertical-align: top; width: 33%;"><a href="assets/deepseek_API_step/1.png" target="_blank" rel="noopener noreferrer"><img src="assets/deepseek_API_step/1.png" alt="DeepSeek 官网" style="max-height: 200px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a></td>
      <td style="text-align: center; padding: 8px; vertical-align: top; width: 33%;"><a href="assets/deepseek_API_step/2.png" target="_blank" rel="noopener noreferrer"><img src="assets/deepseek_API_step/2.png" alt="登录页面" style="max-height: 200px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a></td>
      <td style="text-align: center; padding: 8px; vertical-align: top; width: 33%;"><a href="assets/deepseek_API_step/3.png" target="_blank" rel="noopener noreferrer"><img src="assets/deepseek_API_step/3.png" alt="控制台" style="max-height: 200px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a></td>
    </tr>
    <tr>
      <td style="vertical-align: top; padding: 8px; width: 33%;">
        <h5>步骤二：充值以获取额度</h5>
        <p>API的调用需要消耗账户额度。您可以根据图五的指引完成充值。即便只是少量充值（例如1元），也足以让您长时间体验V3模型。</p>
      </td>
      <td style="vertical-align: top; padding: 8px; width: 33%;">
        <h5>步骤三：创建并复制API密钥</h5>
        <p>充值成功后，请点击左侧的"创建API"按钮。<strong>请注意：密钥仅在创建时完整显示一次，请务必立即复制并妥善保管。</strong></p>
      </td>
      <td style="vertical-align: top; padding: 8px; width: 33%;">
        <h5>步骤四：在Operit AI中配置密钥</h5>
        <p>创建并复制密钥后，返回Operit AI应用。您可以直接在配置页面输入您的API密钥。</p>
      </td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/deepseek_API_step/4.png" target="_blank" rel="noopener noreferrer"><img src="assets/deepseek_API_step/4.png" alt="API密钥页面" style="max-height: 200px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
      </td>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/deepseek_API_step/5.png" target="_blank" rel="noopener noreferrer"><img src="assets/deepseek_API_step/5.png" alt="创建密钥" style="max-height: 200px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
      </td>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/deepseek_API_step/9.png" target="_blank" rel="noopener noreferrer"><img src="assets/deepseek_API_step/9.png" alt="在App中配置" style="max-height: 200px; height: auto; border: 1px solid #ddd; border-radius: 4px;"></a>
      </td>
    </tr>
  </tbody>
</table>

<p>我们支持包括gemini在内的大多数模型。如果还有更新的模型我们没有支持，请提醒我们！</p>

<h4 id="section-2-3-2" style="display: flex; justify-content: space-between; align-items: center;"><span>切换其他AI模型</span><a href="#section-2-3" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>
<p>您可以按照以下步骤切换和配置您想使用的AI模型：</p>
<table style="width: 100%;">
  <tbody>
    <tr>
      <td style="text-align: center; padding: 8px; width: 50%;">步骤一：进入设置</td>
      <td style="text-align: center; padding: 8px; width: 50%;">步骤二：选择AI模型配置</td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/model/1.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/model/1.jpg" alt="步骤一" style="height: 280px; width: auto; max-width: 100%;">
        </a>
      </td>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/model/2.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/model/2.jpg" alt="步骤二" style="height: 280px; width: auto; max-width: 100%;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px;">步骤三：模型与参数配置</td>
      <td style="text-align: center; padding: 8px;">步骤四：选择你的模型</td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/model/3.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/model/3.jpg" alt="步骤三" style="height: 280px; width: auto; max-width: 100%;">
        </a>
      </td>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
         <a href="assets/model/4.jpg" target="_blank" rel="noopener noreferrer">
           <img src="assets/model/4.jpg" alt="步骤四" style="height: 280px; width: auto; max-width: 100%;">
         </a>
      </td>
    </tr>
  </tbody>
</table>

<h3 id="section-2-4" style="display: flex; justify-content: space-between; align-items: center;"><span>Shizuku授权流程</span><a href="#section-2" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>

<p>完成shizuku的配置后，内置包（除<code>coderunner</code>）就都可以用了。</p>

<h3 id="section-2-5" style="display: flex; justify-content: space-between; align-items: center;"><span>包管理与MCP使用说明</span><a href="#section-2" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>
<p>内置包（除<code>coderunner</code>外）开箱即用。其余拓展包与MCP依赖Termux环境，使用前请确保Termux已在后台运行。</p>

<h4 id="section-2-5-1" style="display: flex; justify-content: space-between; align-items: center;"><span>包管理启用过程</span><a href="#section-2-5" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>

<table style="width: 100%; border-collapse: separate; border-spacing: 0 1em;">
    <thead>
      <tr>
       <th style="text-align: center; padding: 8px;">步骤一：进入包管理</th>
        <th style="text-align: center; padding: 8px;">步骤二：启用所需拓展包</th>
      </tr>
     <p>内置包（除<code>coderunner</code>外）开箱即用。其余拓展包与MCP依赖Termux环境，使用前请确保Termux已在后台运行。</p>
    </thead>
    <tbody>
      <tr>
       <td style="text-align: center; padding: 8px; vertical-align: top;">
          <a href="assets/package_or_MCP/1.jpg" target="_blank" rel="noopener noreferrer"><img src="assets/package_or_MCP/1.jpg" alt="启用包管理1" style="height: 280px; width: auto; max-width: 100%;"></a>
       </td>
       <td style="text-align: center; padding: 8px; vertical-align: top;">
          <a href="assets/package_or_MCP/2.jpg" target="_blank" rel="noopener noreferrer"><img src="assets/package_or_MCP/2.jpg" alt="启用包管理2" style="height: 280px; width: auto; max-width: 100%;"></a>
       </td>
     </tr>
   </tbody>
</table>

<h4 id="section-2-5-2" style="display: flex; justify-content: space-between; align-items: center;"><span>一键快捷配置环境</span><a href="#section-2-5" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>
<table border="0" style="width:100%; border-collapse: collapse; text-align: center;">
  <tr style="vertical-align: top;">
    <td style="padding: 5px; width: 33%;">
      <strong>步骤一：进入工具箱</strong><br>
      在主界面或设置中找到"工具箱"入口并点击进入。
    </td>
    <td style="padding: 5px; width: 33%;">
      <strong>步骤二：选择终端自动配置</strong><br>
      在工具箱中，找到并选择"终端自动配置"功能，以开始自动化环境设置。
    </td>
    <td style="padding: 5px; width: 33%;">
      <strong>步骤三：开始配置</strong><br>
      点击"开始配置"按钮，系统将自动完成所需环境的安装和配置。
    </td>
  </tr>
  <tr>
    <td style="padding: 5px;">
      <a href="assets/package_or_MCP/3.jpg" target="_blank" rel="noopener noreferrer">
        <img src="assets/package_or_MCP/3.jpg" alt="配置环境1" style="width: 100%; max-width: 200px; height: auto;">
      </a>
    </td>
    <td style="padding: 5px;">
      <a href="assets/package_or_MCP/4.jpg" target="_blank" rel="noopener noreferrer">
        <img src="assets/package_or_MCP/4.jpg" alt="配置环境2" style="width: 100%; max-width: 200px; height: auto;">
      </a>
    </td>
    <td style="padding: 5px;">
      <a href="assets/package_or_MCP/5.jpg" target="_blank" rel="noopener noreferrer">
        <img src="assets/package_or_MCP/5.jpg" alt="配置环境3" style="width: 100%; max-width: 200px; height: auto;">
      </a>
    </td>
  </tr>
</table>

<h4 id="section-2-5-3" style="display: flex; justify-content: space-between; align-items: center;"><span>MCP配置流程</span><a href="#section-2-5" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>
<table style="width: 100%;">
  <tbody>
    <tr>
      <td style="text-align: center; padding: 8px; width: 50%;">步骤一：进入MCP市场</td>
      <td style="text-align: center; padding: 8px; width: 50%;">步骤二：点击刷新按钮</td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/package_or_MCP/7.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/package_or_MCP/7.jpg" alt="MCP配置1" style="height: 280px; width: auto; max-width: 100%;">
        </a>
      </td>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/package_or_MCP/8.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/package_or_MCP/8.jpg" alt="MCP配置2" style="height: 280px; width: auto; max-width: 100%;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px;">步骤三：等待加载完成</td>
      <td style="text-align: center; padding: 8px;">步骤四：选择并使用MCP</td>
    </tr>
    <tr>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
        <a href="assets/package_or_MCP/9.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/package_or_MCP/9.jpg" alt="MCP配置3" style="height: 280px; width: auto; max-width: 100%;">
        </a>
      </td>
      <td style="text-align: center; padding: 8px; vertical-align: top;">
         <a href="assets/package_or_MCP/10.jpg" target="_blank" rel="noopener noreferrer">
           <img src="assets/package_or_MCP/10.jpg" alt="MCP配置4" style="height: 280px; width: auto; max-width: 100%;">
         </a>
      </td>
    </tr>
  </tbody>
</table>

<p align="right"><i>想深入了解MCP的工作机制、部署与排错？<a href="#section-3-4">查看更多关于MCP市场的说明</a>...</i></p>

<div STYLE="page-break-after: always;"></div>

<h2 id="section-3" style="display: flex; justify-content: space-between; align-items: center;"><span>🚀 拓展用法实操</span><a href="#toc" style="text-decoration: none; font-size: 0.8em;" title="返回目录">🔝</a></h2>

*(本部分将通过实际案例，向您展示如何利用拓展包、计划模式等高级功能，完成更复杂的任务。)*

<h3 id="section-3-1" style="display: flex; justify-content: space-between; align-items: center;"><span>🧰 开箱即用</span><a href="#section-3" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>
<em>这部分为<strong>内置包</strong></em>
<br>
当你让AI写软件，软件的性能取决于AI的能力。示例中的模型为<code>DeepSeek-R1</code>模型

<table style="width: 100%;">
  <thead>
    <tr>
      <th style="width: 20%; text-align: left;">示例 (Example)</th>
      <th style="width: 30%; text-align: left;">说明 (Description)</th>
      <th style="width: 50%; text-align: left;">预览 (Preview)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="vertical-align: top;"><strong>写一个2D弹幕游戏</strong></td>
      <td style="vertical-align: top;">
        通过简单的对话，让AI为您构思并实现一个经典的2D弹幕射击游戏。Operit AI能够调用其基础代码能力，仅使用HTML和JavaScript，从零开始构建出完整的游戏逻辑与动态画面。
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/game_maker_chat.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/game_maker_chat.jpg" alt="2D弹幕游戏聊天" style="width: 100%; height: auto; margin-bottom: 5px;">
        </a>
        <a href="assets/game_maker_show.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/game_maker_show.jpg" alt="2D弹幕游戏展示" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><strong>用HTML代码写一个3D游戏</strong></td>
      <td style="vertical-align: top;">
        无需任何拓展包，Operit AI 仅通过内置的核心工具，就可以直接用HTML和JavaScript代码，为您呈现一个动态的3D游戏场景。
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/expamle/3ddebdde4958ac152eeca436e39c0f6.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/3ddebdde4958ac152eeca436e39c0f6.jpg" alt="3D游戏示例1" style="width: 100%; height: auto; margin-bottom: 5px;">
        </a>
        <a href="assets/expamle/759d86a7d74351675b32acb6464585d.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/759d86a7d74351675b32acb6464585d.jpg" alt="3D游戏示例2" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><strong>简单的视频处理</strong></td>
      <td style="vertical-align: top;">
        同样地，应用内置了强大的FFmpeg工具，无需额外安装，即可让AI帮您完成视频格式转换、截取、合并等多种处理任务。
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/d7580a42ae03c723121bd172e1f9e7d.jpg" target="_blank" rel="noopener noreferrer">
          <img src="./assets/d7580a42ae03c723121bd172e1f9e7d.jpg" alt="简单的视频处理示例" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><strong>软件打包与部署</strong></td>
      <td style="vertical-align: top;">
        从编写代码到最终发布，Operit AI 可以进一步调用平台工具，将完成的软件打包成适用于安卓（APK）或Windows（EXE）的可执行文件，实现端到端的自动化开发流程。
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/web_developer.jpg" target="_blank" rel="noopener noreferrer">
          <img src="./assets/web_developer.jpg" alt="软件打包示例1" style="width: 100%; height: auto; margin-bottom: 5px;">
        </a>
        <a href="assets/game_maker_packer.jpg" target="_blank" rel="noopener noreferrer">
          <img src="./assets/game_maker_packer.jpg" alt="软件打包示例2" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
  </tbody>
</table>

<h3 id="section-3-2" style="display: flex; justify-content: space-between; align-items: center;"><span>📦 拓展包</span><a href="#section-3" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>

>演示版本`1.1.6`
（图片可点击放大）

<table style="width: 100%;">
  <thead>
    <tr>
      <th style="width: 20%; text-align: left;">拓展包 (Package)</th>
      <th style="width: 30%; text-align: left;">功能说明 (Description)</th>
      <th style="width: 50%; text-align: left;">预览 (Preview)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="vertical-align: top;"><code>writer</code></td>
      <td style="vertical-align: top;">
        高级文件编辑和读取功能，支持分段编辑、差异编辑、行号编辑以及高级文件读取操作
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/expamle/065e5ca8a8036c51a7905d206bbb56c.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/065e5ca8a8036c51a7905d206bbb56c.jpg" alt="writer示例" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>various_search</code></td>
      <td style="vertical-align: top;">
        多平台搜索功能，支持从必应、百度、搜狗、夸克等平台获取搜索结果
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/expamle/90a1778510df485d788b80d4bc349f9.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/90a1778510df485d788b80d4bc349f9.jpg" alt="多平台搜索示例1" style="width: 100%; height: auto; margin-bottom: 5px;">
        </a>
        <a href="assets/expamle/f9b8aeba4878775d1252ad8d5d8620a.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/f9b8aeba4878775d1252ad8d5d8620a.jpg" alt="多平台搜索示例2" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>daily_life</code></td>
      <td style="vertical-align: top;">
        日常生活工具集合，包括日期时间查询、设备状态监测、天气搜索、提醒闹钟设置、短信电话通讯等
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/expamle/615cf7a99e421356b6d22bb0b9cc87b.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/615cf7a99e421356b6d22bb0b9cc87b.jpg" alt="日常生活示例" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>super_admin</code></td>
      <td style="vertical-align: top;">
        超级管理员工具集，提供终端命令和Shell操作的高级功能
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/expamle/731f67e3d7494886c1c1f8639216bf2.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/731f67e3d7494886c1c1f8639216bf2.jpg" alt="超级管理员示例1" style="width: 100%; height: auto; margin-bottom: 5px;">
        </a>
        <a href="assets/expamle/6f81901ae47f5a3584167148017d132.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/6f81901ae47f5a3584167148017d132.jpg" alt="超级管理员示例2" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>code_runner</code></td>
      <td style="vertical-align: top;" colspan="2">多语言代码执行能力，支持JavaScript、Python、Ruby、Go和Rust脚本的运行<br><em>你可以在<code>工具箱>终端自动配置</code>中完成以上环境的配置</em></td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>baidu_map</code></td>
      <td style="vertical-align: top;">
        百度地图相关功能
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/expamle/71fd917c5310c1cebaa1abb19882a6d.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/71fd917c5310c1cebaa1abb19882a6d.jpg" alt="百度地图示例" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>qq_intelligent</code></td>
      <td style="vertical-align: top;" colspan="2">QQ智能助手，通过UI自动化技术实现QQ应用交互</td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>time</code></td>
      <td style="vertical-align: top;" colspan="2">提供时间相关功能</td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>various_output</code></td>
      <td style="vertical-align: top;">
        提供图片输出功能
      </td>
      <td style="vertical-align: top; text-align: center;">
        <a href="assets/expamle/5fff4b49db78ec01e189658de8ea997.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/expamle/5fff4b49db78ec01e189658de8ea997.jpg" alt="图片输出示例" style="width: 100%; height: auto;">
        </a>
      </td>
    </tr>
  </tbody>
</table>


<h3 id="section-3-3" style="display: flex; justify-content: space-between; align-items: center;"><span>🛠️ 核心工具</span><a href="#section-3" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>

| 工具 (Tool) | 功能说明 (Description) |
|---|---|
| `sleep` | 短暂暂停执行 |
| `device_info` | 获取设备详细信息 |
| `use_package` | 激活扩展包 |
| `query_knowledge_library` | 查询问题库，获取类似的过去解决方案、用户风格偏好和用户信息 |

文件系统工具：

| 工具 (Tool) | 功能说明 (Description) |
|---|---|
| `list_files` | 列出目录中的文件 |
| `read_file_part` | 分部分读取文件内容（每部分200行） |
| `apply_file` | 智能地修改文件，使用占位符保留不变的部分 |
| `delete_file` | 删除文件或目录 |
| `file_exists` | 检查文件或目录是否存在 |
| `move_file` | 移动或重命名文件或目录 |
| `copy_file` | 复制文件或目录 |
| `make_directory` | 创建目录 |
| `find_files` | 搜索匹配模式的文件 |
| `file_info` | 获取文件或目录的详细信息 |
| `zip_files/unzip_files` | 压缩/解压文件 |
| `open_file` | 使用系统默认应用程序打开文件 |
| `share_file` | 与其他应用程序共享文件 |
| `download_file` | 从网络下载文件 |
| `convert_file` | 将文件从一种格式转换为另一种格式 |
| `get_supported_conversions` | 列出所有支持的文件格式转换 |

HTTP工具：

| 工具 (Tool) | 功能说明 (Description) |
|---|---|
| `http_request` | 发送HTTP请求 |
| `multipart_request` | 上传文件 |
| `manage_cookies` | 管理cookies |
| `visit_web` | 访问网页并提取内容 |

系统操作工具：

| 工具 (Tool) | 功能说明 (Description) |
|---|---|
| `get_system_setting` | 获取系统设置的值 |
| `modify_system_setting` | 修改系统设置的值 |
| `install_app/uninstall_app`| 安装/卸载应用程序 |
| `list_installed_apps` | 获取已安装应用程序列表 |
| `start_app/stop_app` | 启动/停止应用程序 |
| `get_notifications` | 获取设备通知 |
| `get_device_location` | 获取设备当前位置信息 |

UI自动化工具：

| 工具 (Tool) | 功能说明 (Description) |
|---|---|
| `get_page_info` | 获取当前UI屏幕的信息 |
| `tap` | 在特定坐标模拟点击 |
| `click_element` | 点击由资源ID或类名标识的元素 |
| `set_input_text` | 在输入字段中设置文本 |
| `press_key` | 模拟按键 |
| `swipe` | 模拟滑动手势 |
| `find_element` | 查找符合特定条件的UI元素 |

FFmpeg工具：

| 工具 (Tool) | 功能说明 (Description) |
|---|---|
| `ffmpeg_execute` | 执行自定义FFmpeg命令 |
| `ffmpeg_info` | 获取FFmpeg系统信息 |
| `ffmpeg_convert` | 使用简化参数转换视频文件 |

<h3 id="section-3-4" style="display: flex; justify-content: space-between; align-items: center;"><span>🛒 MCP市场</span><a href="#section-3" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>

> 考虑到手机环境的特殊性，要完整、稳定地配置所有MCP（Model context protocol）所需的环境是相当有挑战性的。因此，直接调用MCP可能会遇到较多困难。
> 
> 目前，应用内确认可用的MCP主要有 `12306`。
> 
> 为了提供更流畅、更可靠的体验，我们已经用更适配安卓系统的方式，将许多核心MCP的功能重制并整合到了内置工具和拓展包中。我们强烈建议您优先使用这些经过优化的功能。

> 下面是一些目前社区测试可用的MCP：(等待测试人员更新)

（图片可点击放大）

<table style="width: 100%;">
  <thead>
    <tr>
      <th style="width: 25%; text-align: left;">MCP (Package)</th>
      <th style="width: 50%; text-align: left;">功能说明 (Description)</th>
      <th style="width: 25%; text-align: left;">预览 (Preview)</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="vertical-align: top;"><code>tavily</code></td>
      <td style="vertical-align: top;">
        Tavily 搜索工具，提供强大的网络搜索能力，可用于研究和信息获取。
      </td>
      <td style="vertical-align: top; text-align: right;">
        <a href="assets/ee852df3c187771fba0aa92b36a57f8.jpg" target="_blank" rel="noopener noreferrer">
          <img src="assets/ee852df3c187771fba0aa92b36a57f8.jpg" alt="Tavily搜索示例" height="280">
        </a>
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top;"><code>12306</code></td>
      <td style="vertical-align: top;" colspan="2">
        用于查询12306火车票信息。
      </td>
    </tr>
  </tbody>
</table>

<table style="width: 100%; margin-top: 1em;">
  <thead>
    <tr>
      <th colspan="2" style="text-align: left; padding: 12px;">
        <h4 id="section-3-4-1" style="margin: 0;">
          <span>MCP工作机制</span>
          <a href="#section-3-4" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a>
        </h4>
      </th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="width: 30%; padding: 12px; vertical-align: top;">
        我们的MCP服务器通过Termux运行在本地，并和软件进行交互。MCP会在软件打开的时候进行尝试启动，启动命令由每个插件配置中的<code>arg</code>参数以及<code>env</code>的环境变量决定。
      </td>
      <td style="width: 60%; padding: 12px; vertical-align: top; text-align: center;">
        <img src="assets/41ebc2ec5278bd28e8361e3eb72128d.jpg" alt="MCP配置示例" style="width: 100%; max-width: 400px; height: auto; border-radius: 4px;">
      </td>
    </tr>
  </tbody>
</table>

<h4 id="section-3-4-2" style="display: flex; justify-content: space-between; align-items: center;"><span>MCP下载和部署机制</span><a href="#section-3-4" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>
<p>由于环境特殊，并且MCP的生态本身就杂乱不堪，README文档质量参差不齐，所以我们加入了自动匹配包结构的机制。目前支持识别Node.js和Python两种语言编写的包。</p>
<p>下载MCP时，应用会直接获取仓库的ZIP压缩包，下载到<code>Download/Operit/</code>目录下，并修改一个JSON文件加入ID。如有需要，您也可以在软件内导入自定义MCP或手动将文件放入该目录。</p>
<h5>部署机制</h5>
<p>我们将在部署时为两种项目结构自动生成执行命令。</p>
<table style="width: 100%; margin-top: 1em;">
  <thead>
    <tr>
      <th style="width: 50%; padding: 12px; text-align: left;">Python包</th>
      <th style="width: 50%; padding: 12px; text-align: left;">Node.js包</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="padding: 12px; vertical-align: top;">
        对于Python包，我们会先尝试使用<code>pip</code>安装依赖，然后自动生成一个启动命令的配置。您可以在部署时通过"自定义部署命令"来查看和修改。
      </td>
      <td style="padding: 12px; vertical-align: top;">
        对于Node.js包，我们会先尝试进行换源，然后使用<code>npm</code>或<code>pnpm</code>下载依赖。如果项目是TypeScript编写的，我们会尝试编译项目；如果是JavaScript，则会尝试直接获取入口文件。最后，系统将生成一份配置代码，启动命令会指向入口文件或编译后的文件。
      </td>
    </tr>
  </tbody>
</table>
<blockquote>以上的两种识别模式对于很多包而言都是通用的。当然，也总会有一些意外情况。</blockquote>
<blockquote><b>注意：</b>部署和启动之前，包文件都会被复制到Termux内部进行操作。也就是说，只有下载的原始压缩包才会存放在外部的<code>Download</code>路径下。</blockquote>

<h4 id="section-3-4-3" style="display: flex; justify-content: space-between; align-items: center;"><span>MCP常见问题</span><a href="#section-3-4" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>
<table style="width: 100%; border-collapse: separate; border-spacing: 0 1em;">
  <tbody>
    <tr>
      <td style="width: 30%; vertical-align: middle; padding-right: 15px;">有的插件需要key，但是这部分需要手动加入。如图，请根据readme，把key写在启动环境变量，否则会报错。</td>
      <td style="width: 60%; vertical-align: middle;"><img src="assets/package_or_MCP/7b8ec8ba567c3c670d6a063121614fe.jpg" alt="配置key" style="width: 100%; height: auto; border-radius: 4px;"></td>
    </tr>
    <tr>
      <td style="width: 30%; vertical-align: middle; padding-right: 15px;">插件的部署情况可以手动进入termux进行查看，方式如下。在这里，build文件夹为部署中自动编译后的结果，里面有我们启动需要的文件路径。</td>
      <td style="width: 60%; vertical-align: middle;"><img src="assets/package_or_MCP/401cda27abf79b9d0311816947b1bdd.jpg" alt="查看部署" style="width: 100%; height: auto; border-radius: 4px;"></td>
    </tr>
    <tr>
      <td style="width: 30%; vertical-align: middle; padding-right: 15px;">你可以尝试运行它，以此修正你的启动命令(图中由于缺少key，启动失败)</td>
      <td style="width: 60%; vertical-align: middle;"><img src="assets/package_or_MCP/0946d845d9adad20bbd039a93d1196f.jpg" alt="修正启动命令" style="width: 100%; height: auto; border-radius: 4px;"></td>
    </tr>
  </tbody>
</table>

<div style="background-color: #fffbe6; border-left: 4px solid #ffc107; padding: 12px 16px; margin: 1em 0;">
  <p style="margin: 0; padding: 0;"><strong>注意:</strong> 有的包带了docker文件，但是我们是不支持docker的，请忽视它。</p>
</div>

<div style="background-color: #fffbe6; border-left: 4px solid #ffc107; padding: 12px 16px; margin: 1em 0;">
  <p style="margin: 0; padding: 0;"><strong>注意:</strong> 我们的环境termux是linux，有一些win才能用的包要运行.exe，比如playwright，那当然是不支持的了。</p>
</div>

<h3 id="section-3-5" style="display: flex; justify-content: space-between; align-items: center;"><span>⏳ 计划模式</span><a href="#section-3" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>
`1.1.6`及以后版本不复存在

适用于AI长时间工作，但跟没开计划模式的区别不大（甚至没开的效果更好）。将在后续版本删除，并用`任务模式`替换

>`任务模式`下，AI可能主动给您发消息

>注：不正当的使用将加快token的消耗

<div STYLE="page-break-after: always;"></div>

<h2 id="section-4" style="display: flex; justify-content: space-between; align-items: center;"><span>❔ 常见问题解答</span><a href="#toc" style="text-decoration: none; font-size: 0.8em;" title="返回目录">🔝</a></h2>

这里收录了**最新版本 `1.1.6`** 用户群和 issue 的全部问题。
如果您使用的是旧版本，可以来<a href="#section-7">这里找找</a>。

<h3 id="section-4-1" style="display: flex; justify-content: space-between; align-items: center;"><span>MCP包问题排查</span><a href="#section-4" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>

**MCP包不加载问题可能原因**
- **Shizuku未正确配置**：请参照<a href="#section-2-4">Shizuku授权流程</a>完成配置。
- **Termux未正确配置**：请参照<a href="#section-2-5">包管理与MCP使用说明</a>完成配置。
- **Termux未挂在后台**：在启动软件前，请务必先打开Termux并保持其在后台运行。

**MCP运行失败原因**
- **环境配置问题**：部分MCP插件对运行环境有特殊要求。您需要访问相应插件的GitHub开源地址，根据其文档完成环境配置。关于配置的更多信息，请参考<a href="#section-3-4">MCP市场</a>章节的详细说明。
- **版本兼容性问题**：更早版本中存在的问题大多已在后续更新中得到解决。我们强烈建议您下载并安装最新版本以获取最佳体验。

您可以从[Release页面](https://github.com/AAswordman/Operit/releases)下载最新APK。

<div STYLE="page-break-after: always;"></div>
<h2 id="section-5" style="display: flex; justify-content: space-between; align-items: center;"><span>🎉 加入我们</span><a href="#toc" style="text-decoration: none; font-size: 0.8em;" title="返回目录">🔝</a></h2>

我们诚挚地邀请您加入我们的社区，与其他用户交流心得，分享您的创意，或向我们提出宝贵的建议。

**欢迎加入我们的用户QQ群！**

<div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 12px; padding: 16px; margin-top: 1.5em; max-width: 400px; box-shadow: 0 4px 8px rgba(0,0,0,0.05); font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
  <div style="font-size: 1.1em; font-weight: 600; color: #495057; margin-bottom: 12px;">Operit AI 用户交流群</div>
  <div style="display: flex; justify-content: space-between; align-items: center; gap: 15px;">
    <span style="font-family: 'Courier New', Courier, monospace; font-size: 1.6em; font-weight: bold; color: #007bff; letter-spacing: 1px; word-break: break-all;">458862019</span>
    <button style="background-color: #28a745; color: white; border: none; padding: 8px 15px; border-radius: 8px; cursor: pointer; font-size: 1em; font-weight: 500; white-space: nowrap;" onclick="try { var btn = this; var originalText = btn.innerText; navigator.clipboard.writeText('458862019').then(function() { btn.innerText = '已复制!'; setTimeout(function() { btn.innerText = originalText; }, 2000); }); } catch (err) { alert('复制失败, 请手动复制。'); }">复制</button>
  </div>
</div>

<div STYLE="page-break-after: always;"></div>

<h2 id="section-6" style="display: flex; justify-content: space-between; align-items: center;"><span>💡 许愿池</span><a href="#toc" style="text-decoration: none; font-size: 0.8em;" title="返回目录">🔝</a></h2>

以下是我们正在计划或正在开发中的功能：

- **核心功能增强**:
  - 加入TTS（文字转语音）和语音识别模型，并进一步实现更自然的对话系统。
  - 实现全新的 `任务模式` 来替代现有的 `计划模式`，让AI可以主动、智能地执行和跟进长期任务。
- **用户体验优化**:
  - 实现一个更美观、更现代、更友好的交互界面。
  - 支持多语言，让全球用户都能无障碍使用。
- **社区与生态**:
  - 我们会认真对待社区（如Issue、QQ群）中提出的每一个建议，并努力将它们变为现实。
  - 推广！推广！推广！让更多人认识并使用Operit AI。

有好的想法或功能建议？除了在QQ群中向我们提出，您也可以关注我们未来的更新计划，也许您期待的功能已经在路上！


<div STYLE="page-break-after: always;"></div>

<h2 id="section-7" style="display: flex; justify-content: space-between; align-items: center;"><span>📜 朝花夕拾 (旧版本问题解答)</span><a href="#toc" style="text-decoration: none; font-size: 0.8em;" title="返回目录">🔝</a></h2>

<h3 id="1-1-5-版本" style="display: flex; justify-content: space-between; align-items: center;"><span><code>1.1.5</code>版本</span><a href="#section-7" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>

*(相关问题)*


<h3 id="1-1-3-以前" style="display: flex; justify-content: space-between; align-items: center;"><span><code>1.1.3</code>以前</span><a href="#section-7" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">↩️</a></h3>


*(相关问题)*
<h4 id="gemini格式未做适配" style="display: flex; justify-content: space-between; align-items: center;"><span>Gemini格式未做适配</span><a href="#1-1-3-以前" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>
新版本已解决，支持了更多模型
<h4 id="termux" style="display: flex; justify-content: space-between; align-items: center;"><span>Termux</span><a href="#1-1-3-以前" style="text-decoration: none; font-size: 0.8em;" title="返回上一级">⤴️</a></h4>


##### 类型一 Termux版本不正确
##### 类型二 MCP包不加载
软件运行时建议将Termux挂后台

后续将通过内置Termux解决这类问题