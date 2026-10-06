---
name: adb-log-triage
description: 抓取真机 Logcat、过滤关键信息、识别崩溃、结构化输出。处理「看日志」「为什么崩」「报错了」等请求。
---

# Logcat 抓取与崩溃分诊

抓取 ScoreTrace 真机日志、过滤关键 tag、识别崩溃并结构化输出。

## 触发条件
用户要求「抓日志」「看崩溃」「为什么报错」「logcat」等。

## 环境准备

```bash
export ANDROID_ADB_SERVER_PORT=5038
ADB=/root/android/sdk/platform-tools/adb
SERIAL="$(hostname -I | tr ' ' '\n' | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' | head -n1):5555"
$ADB connect "$SERIAL"
mkdir -p ~/workspace/test-results
```
- 用 `terminal` 工具跑 adb（Bash 直跑会挂）。

## 核心命令

```bash
# 全量最近 500 行
$ADB -s "$SERIAL" logcat -d -t 500 > ~/workspace/test-results/logcat_full.txt

# 只抓 ScoreTrace 相关
$ADB -s "$SERIAL" logcat -d -t 1000 | grep -iE "scoretrace|fenji" > ~/workspace/test-results/logcat_app.txt

# 只抓崩溃（FATAL 起 50 行）
$ADB -s "$SERIAL" logcat -d | grep -A 50 "FATAL EXCEPTION" > ~/workspace/test-results/logcat_crash.txt

# 清空后抓新日志（让用户复现前先清）
$ADB -s "$SERIAL" logcat -c

# 带时间戳 / 只看 warning 以上
$ADB -s "$SERIAL" logcat -v time
$ADB -s "$SERIAL" logcat '*:W'
```

## 关键 tag 过滤

| Tag | 含义 |
| --- | --- |
| `AndroidRuntime` | 崩溃堆栈 |
| `ScoreTrace` | App 自有日志 |
| `GlmVision` | GLM 拍照识分 |
| `DeepSeek` | AI 对话 |
| `AiViewModel` | AI 逻辑 |
| `Room` | 数据库异常 |
| `ScoreTrace-ADB` | KSU 模块日志 |

## 崩溃分析输出格式

1. **异常类型 + message**（如 `java.lang.NullPointerException: ...`）。
2. **堆栈关键帧**（前 5 行，标注涉及 `com.fenji.scoretrace` 的帧）。
3. **复现路径推测**。
4. **关联代码位置**（能从堆栈帧推断时给出 `文件路径:行号`，否则标注「未定位」）。

## 注意事项
- 抓 logcat 前先确保 App 在前台（`keyevent HOME` + `am start`），否则抓到的是 AiCode 的日志。
- 崩溃日志优先抓 `AndroidRuntime`，一次给全，不要截断关键帧。
- 复现类问题建议先 `logcat -c` 清缓冲，再让用户操作，最后 `logcat -d` 抓增量。
- **容器内没有 `su` / `adb root` 被拒**，抓不到其他 App 的私有日志。

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
