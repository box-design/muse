# Muse · 私人数字博物馆

一款本地优先的 Android 数字博物馆 App，方便你以「展览 · 收藏 · 创作」的方式整理和回味手机相册里的点点滴滴。

## ✨ 功能

- **博览（Explore）**：按时间线整理照片成日折卡，宽屏双栏展示，展览封面可左右图文排版。
- **收藏（Collection）**：收藏喜爱的作品并分组展示，影集封面架、明信片等。
- **创作（Create）**：
  - **明信片**：选取照片，自定义纸质与文字，渲染为 1080×1440 PNG 保存至系统相册。
  - **取色器（Palette）**：提取照片主色调，生成配色。
  - **影集（Zine）**：多选 8–20 张照片装订成册，支持拖拽排序、封面滤镜与翻页阅读器。
- **横屏适配**：基于 `WindowSizeClass` 的自适应布局，平板与横屏体验更佳。

## 🛠 技术栈

- Kotlin · Jetpack Compose（Material 3）
- MVVM（`ViewModel` + Repository）
- 本地持久化：JSON（`MuseStore`）
- MediaStore 导出、自适应布局、动画为主

## 🚀 构建

环境要求：JDK 17+，Android SDK 35。

```bash
# 生成签名密钥（首次）
keytool -genkeypair -alias muse -keyalg RSA -keysize 2048 \
  -keystore muse-release.keystore -storetype PKCS12

# 配置签名凭据（请将以下口令改为你自己的，勿提交到仓库）
cat > keystore.properties <<EOF
storeFile=muse-release.keystore
storePassword=你的口令
keyAlias=muse
keyPassword=你的口令
EOF

# 组装 release 包
./gradlew assembleRelease
```

> 说明：`muse-release.keystore` 与 `keystore.properties` 属于签名凭据，已被 `.gitignore` 忽略，**严禁提交到仓库**。

## 📦 安装包

- 最新 release 安装包见 [`releases/Muse-v1.0.0-release.apk`](releases/Muse-v1.0.0-release.apk)。

## 🔒 安全

- 仓库历史已压缩为单个干净提交，签名密钥与口令已从版本库中移除并纳入 `.gitignore`。
- 建议妥善备份 `muse-release.keystore` 与 `keystore.properties`，一旦丢失将无法更新已发布的应用。

## 📄 License

项目所有权归作者所有。当前未提供开源许可证，如需使用请联系作者。