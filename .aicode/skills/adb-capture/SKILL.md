---
name: adb-capture
description: 截取真机屏幕、导出 UI 层级、定位元素坐标。处理「截个图」「看看界面」「这个按钮在哪」等请求，并为 adb-input 提供点击坐标。
---

# 截图与 UI 捕获

截取 ScoreTrace 真机屏幕、导出 UI 层级 XML、把元素换算成屏幕坐标。

## 触发条件
用户要求截图、查看当前界面、获取元素坐标、查找某按钮位置。

## 环境准备与前台拉取（每次必做）

```bash
export ANDROID_ADB_SERVER_PORT=5038
ADB=/root/android/sdk/platform-tools/adb
SERIAL="$(hostname -I | tr ' ' '\n' | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' | head -n1):5555"
$ADB connect "$SERIAL"

# AiCode 自己占前台，截图/取坐标前必须先把 ScoreTrace 拉回前台
$ADB -s "$SERIAL" shell input keyevent KEYCODE_HOME
sleep 1
$ADB -s "$SERIAL" shell am start -n com.fenji.scoretrace/.MainActivity
sleep 2
$ADB -s "$SERIAL" shell dumpsys window | grep -m1 mCurrentFocus   # 必须是 com.fenji.scoretrace
```

- 用 `terminal` 工具跑（Bash 直跑 adb 会挂）；截图/XML 统一存 `~/workspace/test-results/`。

## 核心命令

```bash
mkdir -p ~/workspace/test-results

# 截图
TS=$(date +%s)
$ADB -s "$SERIAL" shell screencap -p /sdcard/st_$TS.png
$ADB -s "$SERIAL" pull /sdcard/st_$TS.png ~/workspace/test-results/

# UI dump（拿元素坐标）
$ADB -s "$SERIAL" shell uiautomator dump /sdcard/window_dump.xml
$ADB -s "$SERIAL" pull /sdcard/window_dump.xml ~/workspace/test-results/
```

## UI dump 解析技巧

- 元素格式：`<node ... text="登录" resource-id="com.fenji.scoretrace:id/btn_login" bounds="[x1,y1][x2,y2]" .../>`
- 找元素并取 bounds：
  ```bash
  # 按文本找
  grep -oE 'text="[^"]*"[^>]*bounds="\[[0-9,]+\]\[[0-9,]+\]"' window_dump.xml | grep "登录"
  # 按 resource-id 找
  grep -oE 'resource-id="[^"]*login[^"]*"[^>]*bounds="\[[0-9,]+\]\[[0-9,]+\]"' window_dump.xml
  ```
- 坐标换算：`bounds="[x1,y1][x2,y2]"` 的**中心点** = `((x1+x2)/2, (y1+y2)/2)`，用于后续 `input tap`。
- 注意写死的坐标会因分辨率失效，**始终以当次 dump 的 bounds 为准**。

## 排错：uiautomator dump 报 "could not get idle state"

ScoreTrace 首页有持续动画（倒计时呼吸、音乐音柱、无限过渡），`uiautomator dump` 会一直等不到空闲，报：

```
ERROR: could not get idle state.
```

此时该命令**退出码仍为 0**，但不会生成 `/sdcard/window_dump.xml`，随后的 pull 会报 `No such file or directory`。

绕过办法：临时把动画缩放设为 0，dump 完再恢复（**实测有效**）：

```bash
# 记录原值（正常都是 1.0）
$ADB -s "$SERIAL" shell settings get global window_animation_scale
# 关闭动画
$ADB -s "$SERIAL" shell settings put global window_animation_scale 0
$ADB -s "$SERIAL" shell settings put global transition_animation_scale 0
$ADB -s "$SERIAL" shell settings put global animator_duration_scale 0
sleep 1
$ADB -s "$SERIAL" shell uiautomator dump /sdcard/window_dump.xml
$ADB -s "$SERIAL" pull /sdcard/window_dump.xml ~/workspace/test-results/
# 恢复（务必执行，别把用户手机动画关了）
$ADB -s "$SERIAL" shell settings put global window_animation_scale 1.0
$ADB -s "$SERIAL" shell settings put global transition_animation_scale 1.0
$ADB -s "$SERIAL" shell settings put global animator_duration_scale 1.0
```

- 原因：Compose 的 `rememberInfiniteTransition` 遵循系统 `animator_duration_scale`，设为 0 后动画立即结束、界面进入 idle。
- 若只是看图、不需要坐标，直接截图即可，不必折腾动画设置。
- **每次 dump 前重新确认焦点**：用户或别的 App 可能中途抢走前台（实测就抓到过别的 App 的 UI），`dumpsys window | grep -m1 mCurrentFocus` 必须是 `com.fenji.scoretrace`。

## 输出规范
- 截图：文件路径 + 简要描述（可用 `viewImage` 自查内容，必要时 `sendFile` 发到聊天区）。
- UI dump：目标元素的**中心坐标**（供 `adb-input` 使用）。
- 命中多个元素时全部列出，让用户选择，不要擅自挑一个。

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
