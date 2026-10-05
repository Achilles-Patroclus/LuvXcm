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
