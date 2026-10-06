---
name: adb-install-verify
description: 编译 Debug APK → 安装到真机 → 启动 App → 验证运行状态。处理「装一下」「跑最新版」「冒烟测试」等请求。
---

# 编译安装与冒烟验证

把 ScoreTrace 构建、安装到真机并验证能正常运行。

## 触发条件
用户要求「装一下」「跑最新版」「装最新 APK 到手机」「冒烟测试」。

## 核心流程（5 步）

### 1. 编译
```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64 ./gradlew assembleDebug
```
- 耗时：增量约 30 秒-2 分钟，改依赖触发全量约 9-13 分钟；长时间无输出加 `--no-daemon`。
- **熔断（重要）**：编译连续失败 **两次** 即停止并报告日志，不要反复重试。
- 若报 `kspDebugKotlin` 的 `Number of loaded files in snapshots differs`（Gradle 执行历史不一致，非源码错误）→ `rm -rf .gradle/9.6.0` 后重试。
- 产物：`app/build/outputs/apk/debug/app-debug.apk`。

### 2. 安装
```bash
export ANDROID_ADB_SERVER_PORT=5038
ADB=/root/android/sdk/platform-tools/adb
SERIAL="$(hostname -I | tr ' ' '\n' | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' | head -n1):5555"
$ADB connect "$SERIAL"
$ADB -s "$SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
```
- 用 `terminal` 工具跑 adb（Bash 直跑会挂）。
- 若报 `INSTALL_FAILED_VERSION_DOWNGRADE` → 加 `-d`；若报 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`（签名不符）→ 提示用户是否卸载重装。

### 3. 启动
```bash
$ADB -s "$SERIAL" shell am start -n com.fenji.scoretrace/.MainActivity
sleep 3
```

### 4. 验证
```bash
# 焦点检查（必须是 com.fenji.scoretrace）
$ADB -s "$SERIAL" shell dumpsys window | grep -m1 mCurrentFocus
# 崩溃检查
$ADB -s "$SERIAL" logcat -d -t 200 | grep -iE "FATAL|AndroidRuntime"
# 截图
TS=$(date +%s); mkdir -p ~/workspace/test-results
$ADB -s "$SERIAL" shell "mkdir -p /sdcard/ADB/screenshots"
$ADB -s "$SERIAL" shell screencap -p /sdcard/ADB/screenshots/verify_$TS.png
$ADB -s "$SERIAL" pull /sdcard/ADB/screenshots/verify_$TS.png ~/workspace/test-results/
```
- 注意：`am start` / 截图前先 `keyevent HOME` 再 `am start`，避免 AiCode 占焦点（见 `adb-capture`）。

### 5. 报告
编译结果 + 安装结果 + 启动结果 + 崩溃日志（如有）+ 截图路径。判定用 ✅ / ❌ / ⚠️。

## 关联技能
- 启动后若要操作界面 → `adb-input`；要看当前页面 → `adb-capture`；崩溃排查 → `adb-log-triage`。

---

## ⚠️ 收尾规范（强制）

**每次 ADB 操作完成后，必须把焦点归还给 AiCode**，否则用户看到的是 ScoreTrace 界面，会误以为 AiCode 卡住了。

### 标准收尾命令

```bash
export ANDROID_ADB_SERVER_PORT=5038
ADB=/root/android/sdk/platform-tools/adb
SERIAL="$(hostname -I | tr ' ' '\n' | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' | head -n1):5555"
AICODE_PKG="com.aicode"

# 三重回退：显式启动 → monkey 启动 → 应用切换键
$ADB -s "$SERIAL" shell am start -n "$AICODE_PKG/.MainActivity" 2>/dev/null \
  || $ADB -s "$SERIAL" shell monkey -p "$AICODE_PKG" -c android.intent.category.LAUNCHER 1 \
  || $ADB -s "$SERIAL" shell input keyevent KEYCODE_APP_SWITCH
```

### 例外情况

**仅当用户明确要求**「停留在 ScoreTrace 看效果」「我要自己看界面」时，**跳过**此步骤。

### 验证

执行完成后，手机屏幕应显示 AiCode 对话界面，而非 ScoreTrace。

---

## 📁 ADB 文件路径规范（强制，2026-10-06 v5.12 建立）

**所有 ADB 产物必须放在 `/sdcard/ADB/` 下，不得污染 `/sdcard/` 根目录。**

- 截图 → `/sdcard/ADB/screenshots/`
- uiautomator dump → `/sdcard/ADB/dumps/`（用完即删）
- logcat → `/sdcard/ADB/logs/`
- 临时文件 → `/sdcard/ADB/tmp/`（操作后清空）

**🚨 绝对禁止**：
- 不得删除 `/sdcard/` 根目录下的用户文件/文件夹
- 不得对根目录使用通配符删除（如 `rm /sdcard/*.xml`）
- 不得删除 UUID 命名的文件（可能是用户资产）
- 不确定文件归属时，**保留不删**

**操作示例**：
```bash
export ANDROID_ADB_SERVER_PORT=5038
ADB=/root/android/sdk/platform-tools/adb
SERIAL="$(hostname -I | tr ' ' '\n' | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' | head -n1):5555"
$ADB -s "$SERIAL" shell "mkdir -p /sdcard/ADB/screenshots /sdcard/ADB/dumps /sdcard/ADB/logs /sdcard/ADB/tmp"
$ADB -s "$SERIAL" shell screencap -p /sdcard/ADB/screenshots/home.png
$ADB -s "$SERIAL" pull /sdcard/ADB/screenshots/home.png ~/workspace/test-results/
```

**操作收尾**：拉取后若本地已保存，删除设备端 dump 临时文件，并清空 tmp：
```bash
$ADB -s "$SERIAL" shell "rm -f /sdcard/ADB/dumps/window_dump.xml"
$ADB -s "$SERIAL" shell "rm -rf /sdcard/ADB/tmp/*"
```
（`/sdcard/ADB/*` 是 ADB 自己的目录，可以清理；**`/sdcard/` 根目录严禁通配符**。）
