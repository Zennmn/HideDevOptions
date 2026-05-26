# HideDevOptions

`HideDevOptions` 是一个用于 Android 的轻量级 LSPosed/Xposed 模块。它会拦截目标应用对系统设置的读取，让应用读取到的“开发者选项”和“ADB 调试”状态始终为关闭。

## 功能说明

- Hook `Settings.Global.getInt(...)`
- Hook `Settings.Secure.getInt(...)`
- Hook `Settings.Global.getString(...)`
- Hook `Settings.Secure.getString(...)`
- 当读取以下字段时，强制返回 `0` 或 `"0"`
  - `development_settings_enabled`
  - `adb_enabled`

这适合用于某些会因为检测到开发者选项或 USB 调试开启而限制功能的应用场景。

## 项目结构

- `app/src/main/java/com/example/hideadb/MainHook.java`
  Xposed 模块入口和核心 Hook 逻辑
- `app/src/main/resources/META-INF/xposed/`
  Xposed 模块元数据
- `app/src/main/res/values/arrays.xml`
  LSPosed 默认作用域配置

## 环境要求

- Android SDK
- JDK 11
- 仓库已包含 Gradle Wrapper
- LSPosed 或其他兼容的现代 Xposed 框架

## 编译方法

Linux / macOS:

```bash
./gradlew assembleRelease
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleRelease
```

如果项目根目录存在本地 `signing.properties`，则 `release` 会自动使用正式签名。

可以先复制一份示例文件：

```powershell
Copy-Item .\signing.properties.example .\signing.properties
```

`signing.properties` 示例字段：

```properties
storeFile=C:/path/to/your/release-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

注意：

- `signing.properties` 仅用于本地签名，已加入 `.gitignore`
- 没有该文件时，项目仍可继续构建，但不会使用正式签名

生成的 APK 默认位于：

```text
app/build/outputs/apk/release/
```

## 使用方法

1. 编译生成 APK。
2. 在安装了 LSPosed 的环境中安装模块。
3. 在 LSPosed Manager 中启用模块。
4. 为模块手动选择需要隐藏开发者选项检测的目标应用。
5. 重启设备，或至少强制结束并重新打开目标应用。

## 注意事项

- 本模块默认作用域为空，需要你在 LSPosed 中手动勾选目标应用。
- `local.properties`、构建产物等文件已通过 `.gitignore` 排除，不会提交到仓库。
- 不同 Android 版本和 ROM 上的兼容性可能会有差异，建议自行测试。

## 开源协议

本项目使用 [MIT License](./LICENSE) 开源，你可以自由使用、修改和分发，但需保留原始许可证声明。
