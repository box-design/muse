# Muse

Muse 是一款 Android 照片整理应用，将手机相册中的照片以博物馆展览的形式组织和呈现，所有数据保存在本地，无需联网和账号。

当前版本：v1.0.0

## 功能

- **博览** — 按日期自动整理照片，以时间线方式浏览
- **展览** — 将照片组织为展览，提供封面与画廊式浏览
- **收藏** — 收藏喜爱的照片、明信片和影集
- **明信片** — 用照片制作明信片，支持选择纸质效果、编辑文字，导出为 PNG 保存到相册
- **影集** — 选取 8–20 张照片装订成册，支持拖拽排序、封面样式和翻页阅读
- **取色器** — 从照片中提取主色调，生成配色方案
- **横屏 / 平板适配** — 基于 WindowSizeClass 的自适应布局

## 技术栈

- Kotlin
- Jetpack Compose（Material 3）
- MVVM 架构（ViewModel + Repository）
- 本地 JSON 持久化

## 环境要求

- Android Studio Ladybug 或更高版本
- JDK 17+
- Android SDK 35（minSdk 26 / targetSdk 35）

## 构建

```bash
git clone https://github.com/box-design/muse.git
cd muse
./gradlew assembleDebug
```

### Release 签名

仓库不包含签名文件。如需构建 release 包，在项目根目录创建 `muse-release.keystore` 和 `keystore.properties`：

```properties
storeFile=muse-release.keystore
storePassword=你的口令
keyAlias=muse
keyPassword=你的口令
```

这两个文件已被 `.gitignore` 忽略，请勿提交到仓库。

## 下载

- v1.0.0 安装包：[releases/Muse-v1.0.0-release.apk](releases/Muse-v1.0.0-release.apk)

## 目录结构

```
app/src/main/java/com/muse/app/
├── data/          # 数据层：模型、仓库、持久化
├── di/            # 依赖注入容器
├── ui/            # 界面：按功能模块分包
│   ├── adaptive/  #   窗口尺寸适配
│   ├── collection/#   收藏页
│   ├── create/    #   创作页（明信片、影集、取色器）
│   ├── explore/   #   博览与展览页
│   ├── viewer/    #   照片查看器
│   └── zine/      #   影集编辑与阅读
└── util/          # 工具类
```

## License

版权所有，保留所有权利。
