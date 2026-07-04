# NetworkCapture - LSPosed 网络抓包模块

一款专业的 LSPosed 网络请求抓包工具，专为私服游戏调试设计。

## ✨ 功能特点

### 核心功能
- **网络请求拦截**：自动拦截并记录所有 HTTP/HTTPS 网络请求
- **完整信息捕获**：记录请求头、请求体、响应头、响应体等完整信息
- **实时日志展示**：以列表形式实时展示所有网络请求记录
- **详情页面查看**：点击单条记录查看详细的请求内容

### 高级功能
- **智能过滤**：按请求方法（GET/POST/PUT/DELETE）快速过滤
- **实时搜索**：搜索 URL 或请求方法快速定位请求
- **一键导出**：导出所有日志到文件并复制到剪贴板
- **清空日志**：一键清空所有历史记录
- **统计信息**：显示总请求数和今日请求数

### 设计特点
- **深色主题**：专为开发者设计的 Code Dark 配色方案
- **方法色彩**：不同 HTTP 方法使用不同颜色标识
- **状态指示**：响应状态码采用颜色编码直观展示
- **Monospace 字体**：所有代码内容使用等宽字体显示
- **Material Design 3**：现代化的 Material Design 设计风格

## 🎨 设计系统

本项目使用 UI-UX-Pro-Max 技能生成的设计系统：

### 配色方案
- **主色调**：#1E293B (Code Dark)
- **背景色**：#0F172A (深色背景)
- **强调色**：#22C55E (Run Green)
- **文字色**：#F8FAFC (高对比度白色)

### HTTP 方法颜色
- **GET**：#22C55E (绿色)
- **POST**：#3B82F6 (蓝色)
- **PUT**：#F59E0B (橙色)
- **DELETE**：#EF4444 (红色)
- **PATCH**：#8B5CF6 (紫色)

### 字体
- **标题/正文**：IBM Plex Sans
- **代码/URL**：JetBrains Mono / Monospace

## 🔧 技术架构

### LSPosed Hook
- **OkHttp 拦截**：通过拦截器注入 OkHttp 网络请求
- **HttpURLConnection 拦截**：Hook HttpURLConnection 的输入输出流
- **目标应用**：com.feiyu.stepbystepapp
- **API 版本**：LSPosed API 101

### 数据存储
- **Room 数据库**：本地持久化存储网络请求记录
- **Flow 流**：实时数据更新
- **协程**：异步数据处理

### UI 组件
- **RecyclerView**：高效的列表展示
- **MaterialCardView**：现代化卡片布局
- **ChipGroup**：过滤器组件
- **SwipeRefreshLayout**：下拉刷新

## 📦 项目结构

```
NetworkCapture/
├── app/
│   ├── build.gradle              # 应用构建配置
│   ├── proguard-rules.pro        # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单
│       ├── assets/
│       │   └── xposed_init       # LSPosed 入口类
│       ├── java/com/networkcapture/module/
│       │   ├── data/
│       │   │   ├── model/        # 数据模型
│       │   │   ├── db/           # Room 数据库
│       │   │   └── repository/   # 数据仓库
│       │   ├── hook/             # LSPosed Hook 实现
│       │   ├── ui/
│       │   │   ├── adapters/     # RecyclerView 适配器
│       │   │   ├── viewmodel/    # ViewModel
│       │   │   ├── MainActivity.kt    # 主界面
│       │   │   └── DetailActivity.kt  # 详情页面
│       ├── res/
│       │   ├── layout/           # 布局文件
│       │   ├── values/           # 资源值
│       │   ├── drawable/         # 图形资源
│       │   └── mipmap/           # 应用图标
├── build.gradle                  # 项目构建配置
├── settings.gradle               # 项目设置
```

## 🚀 安装使用

### 前置要求
- Android 设备（Android 8.0+ / API 26+）
- LSPosed 框架已安装并激活
- Root 权限

### 安装步骤

1. **编译项目**
   ```bash
   cd NetworkCapture
   ./gradlew assembleDebug
   ```

2. **安装 APK**
   - 将生成的 APK 安装到设备
   - 位置：`app/build/outputs/apk/debug/app-debug.apk`

3. **激活模块**
   - 在 LSPosed 管理器中找到 NetworkCapture 模块
   - 勾选模块并激活
   - 设置作用域为 `com.feiyu.stepbystepapp`
   - 重启目标应用

4. **使用模块**
   - 打开 NetworkCapture 应用
   - 开启抓包开关
   - 启动目标应用 com.feiyu.stepbystepapp
   - 所有网络请求将自动记录并显示

## 📖 使用说明

### 主界面功能

1. **抓包开关**
   - 顶部开关控制是否抓包
   - 开启时显示绿色状态
   - 关闭时停止拦截网络请求

2. **搜索框**
   - 输入关键词搜索 URL 或方法
   - 实时过滤显示匹配结果

3. **过滤器**
   - 点击 Chip 快速过滤特定方法
   - 支持全部、GET、POST、PUT、DELETE

4. **统计信息**
   - 显示总请求数和今日请求数
   - 实时更新统计

5. **网络请求列表**
   - 每条记录显示：方法、URL、状态码、耗时、时间
   - 点击进入详情页面

6. **导出按钮**
   - 导出所有日志到文件
   - 同时复制到剪贴板
   - 文件保存在应用外部存储目录

7. **清空按钮**
   - 清空所有历史记录
   - 确认后执行删除

### 详情页面功能

1. **基本信息**
   - 显示请求方法、状态码、URL
   - 显示时间戳和耗时

2. **请求头**
   - JSON 格式化展示
   - 一键复制按钮

3. **请求体**
   - 支持多种格式（JSON、表单等）
   - JSON 自动格式化
   - 一键复制按钮

4. **响应头**
   - JSON 格式化展示
   - 一键复制按钮

5. **响应体**
   - 支持多种格式（JSON、HTML、文本等）
   - JSON 自动格式化
   - 一键复制按钮

6. **错误信息**
   - 失败请求显示错误详情
   - 红色高亮提示

7. **复制全部**
   - 一键复制完整请求信息
   - 包含所有字段和格式化内容

## 🔍 支持的网络库

- ✅ **OkHttp 3.x / 4.x**（最常用）
- ✅ **HttpURLConnection**（系统原生）
- ✅ **Retrofit**（基于 OkHttp）
- ⚠️ 其他网络库可能需要额外适配

## 🎯 目标应用配置

当前配置的作用域：
- `com.feiyu.stepbystepapp`

修改目标应用：
1. 编辑 `app/src/main/res/values/arrays.xml`
2. 修改 `xposed_scope` 数组中的包名
3. 重新编译安装

## ⚙️ 技术细节

### 数据库字段
```kotlin
@Entity(tableName = "network_requests")
data class NetworkRequest(
    val id: Long,                  // 主键
    val url: String,               // 请求 URL
    val method: String,            // 请求方法
    val requestHeaders: String?,   // 请求头（JSON）
    val requestBody: String?,      // 请求体
    val responseCode: Int,         // 响应状态码
    val responseHeaders: String?,  // 响应头（JSON）
    val responseBody: String?,     // 响应体
    val timestamp: Long,           // 时间戳
    val duration: Long,            // 耗时（毫秒）
    val isSuccess: Boolean,        // 是否成功
    val errorMessage: String?      // 错误信息
)
```

### Hook 原理
1. **OkHttp Hook**
   - Hook `OkHttpClient.Builder.build()`
   - 动态添加网络拦截器
   - 在拦截器中捕获请求和响应

2. **HttpURLConnection Hook**
   - Hook `getInputStream()` 捕获响应
   - Hook `getOutputStream()` 捕获请求体
   - 收集请求和响应信息

## 📊 性能优化

- **数据库索引**：timestamp、method、url 字段建立索引
- **异步处理**：所有数据库操作使用协程异步执行
- **Flow 流**：实时数据更新避免轮询
- **ViewHolder**：RecyclerView 复用优化
- **DiffUtil**：列表差异计算优化

## 🔐 权限要求

- `INTERNET`：网络访问权限（仅用于导出功能）

## 📝 开发日志

### 已实现功能
- ✅ 项目结构和 Gradle 配置
- ✅ LSPosed Hook 拦截网络请求
- ✅ Room 数据库存储
- ✅ 主界面日志列表
- ✅ 详情页面展示
- ✅ 过滤和搜索功能
- ✅ 导出和清空功能
- ✅ 深色主题 UI
- ✅ LSPosed 模块配置

### 待扩展功能
- 🔲 支持更多网络库（Volley、AsyncHttpClient 等）
- 🔲 WebSocket 拦截
- 🔲 请求重放功能
- 🔲 自动测试脚本生成
- 🔲 请求对比分析

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目仅供学习和研究使用。

## 🎨 设计来源

本项目 UI/UX 设计由 **UI-UX-Pro-Max Skill** 生成，包含：
- 67 种 UI 风格数据库
- 96 套行业配色方案
- 100+ 条设计推理规则
- 专业的设计系统推荐

---

**开发者**：Moon  
**工具**：TRAE IDE + UI-UX-Pro-Max Skill  
**日期**：2026-07-04