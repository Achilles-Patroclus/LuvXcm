---
name: adb-input
description: 模拟用户输入：点击、长按、滑动、文字输入、按键。用于自动化操作 ScoreTrace App，坐标依赖 adb-capture 的定位结果。
---

# 模拟输入

在 ScoreTrace 真机上模拟点击、长按、滑动、文字输入与按键。

## 触发条件
用户要求「点一下 X」「输入 Y」「滑动」「返回」「长按」等。

## 环境准备（每次必做）

```bash
export ANDROID_ADB_SERVER_PORT=5038
ADB=/root/android/sdk/platform-tools/adb
SERIAL="$(hostname -I | tr ' ' '\n' | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' | head -n1):5555"
$ADB connect "$SERIAL"
# 拉前台 + 确认焦点（否则输入会落到 AiCode 上）
$ADB -s "$SERIAL" shell input keyevent KEYCODE_HOME
sleep 1
$ADB -s "$SERIAL" shell am start -n com.fenji.scoretrace/.MainActivity
sleep 2
$ADB -s "$SERIAL" shell dumpsys window | grep -m1 mCurrentFocus   # 必须是 com.fenji.scoretrace
```

## 核心命令

```bash
# 点击（坐标来自 adb-capture 的 uiautomator dump）
$ADB -s "$SERIAL" shell input tap <x> <y>

# 长按（起点终点相同 + 时长）
$ADB -s "$SERIAL" shell input swipe <x> <y> <x> <y> 1000

# 滑动
$ADB -s "$SERIAL" shell input swipe <x1> <y1> <x2> <y2> <duration_ms>

# 文字输入（只支持 ASCII！）
$ADB -s "$SERIAL" shell input text "hello"

# 按键
$ADB -s "$SERIAL" shell input keyevent KEYCODE_BACK     # 返回
$ADB -s "$SERIAL" shell input keyevent KEYCODE_HOME     # 桌面
$ADB -s "$SERIAL" shell input keyevent KEYCODE_ENTER    # 回车
```

## 中文输入处理

- `input text` **只支持 ASCII**，输入中文会乱码或空。
- 需要中文时：让用户手动输入，或安装 ADBKeyboard 特殊输入法（需手机端配合，当前未部署）。
- 用户要求输入中文时，**明确提示「adb input 不支持中文，请手动输入」**，不要静默替换成拼音或英文。

## 操作安全规则

1. 每次点击前先用 `uiautomator dump` 确认坐标（见 `adb-capture`），不要凭记忆写坐标。
2. 每次点击后 `sleep 1` 等 UI 响应，再截图验证。
3. 关键操作（提交、删除、清空数据类）后必须截图留证。
4. 连续操作之间用 `dumpsys window | grep -m1 mCurrentFocus` 复查焦点，防止被弹窗 / AiCode 抢焦点。

## 输出规范
操作命令 + 操作结果（成功/失败判定）+ 后续建议；涉及多步时给操作序列与每步截图路径。

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
