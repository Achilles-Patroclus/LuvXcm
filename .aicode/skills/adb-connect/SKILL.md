---
name: adb-connect
description: 连接、重连、诊断 ScoreTrace 真机 ADB 通道。处理设备未连接、连接断开、Connection refused、unauthorized、设备为空等所有连接相关问题。
---

# ADB 连接与诊断

用于建立或恢复 AiCode 容器到 ScoreTrace 真机的 ADB 通道，并诊断连接故障。

## 触发条件
用户提到「连接手机」「adb 连不上」「Connection refused」「unauthorized」「设备列表为空」「no route to host」等。

## 环境准备（每次必做）

```bash
export ANDROID_ADB_SERVER_PORT=5038          # 容器宿主占 5037，不改端口 adb 会挂起
ADB=/root/android/sdk/platform-tools/adb      # adb 不在 PATH，用绝对路径
SERIAL="$(hostname -I | tr ' ' '\n' | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' | head -n1):5555"
```

- 容器与安卓宿主**共享网络命名空间**，`hostname -I` 的本机 IPv4 就是手机 LAN IP（实测 `192.168.1.157`）；也可从 KSU 模块 WebUI 状态卡读「本机 IP」。
- **跑 adb 用 `terminal` 工具建常驻会话**（复用同一 tab）；用 `Bash` 直跑 adb 会因守护进程继承 stdout 而挂住。

## 核心步骤

1. 检查现有连接：`$ADB devices -l`
2. 若为空或没有 `device` 状态的目标机 → 重连：
   ```bash
   $ADB kill-server
   $ADB start-server
   $ADB disconnect emulator-5554 2>/dev/null   # 幽灵设备（与真机是同一台，device 号相同）
   $ADB connect "$SERIAL"
   sleep 2
   $ADB devices -l
   ```
3. 确认状态：`$ADB -s "$SERIAL" get-state` 输出 `device` 才算成功。
4. 失败则按下方故障树排查。

## 常用命令模板

```bash
# 标准重连（一步到位）
export ANDROID_ADB_SERVER_PORT=5038
ADB=/root/android/sdk/platform-tools/adb
SERIAL="$(hostname -I | tr ' ' '\n' | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' | head -n1):5555"
$ADB kill-server; $ADB start-server
$ADB connect "$SERIAL"
$ADB -s "$SERIAL" devices -l

# 已有现成脚本（容器内，已内置 5038 与 setsid）
~/workspace/tools/adb-reconnect.sh          # 自动探测 IP + 清理幽灵设备 + 重连 + 验证
~/workspace/tools/adb-connect.sh            # 轻量连接
```

## 故障排查树

| 现象 | 原因 | 处置 |
| --- | --- | --- |
| `Connection refused` | 手机 adbd 未运行 | 让用户打开 KSU 模块 WebUI → 点「重启 ADB 服务」 |
| `unauthorized` | 授权失效 | 让用户到 WebUI「ADB 授权管理」卡片添加容器公钥 |
| `device offline` | 连接状态错乱 | `$ADB kill-server && $ADB connect "$SERIAL"` |
| `no route to host` / 超时 | 手机 IP 变了 | 重新 `hostname -I` 取 IP，或让用户看 WebUI 新 IP |
| `more than one device` | 幽灵 `emulator-5554` | 所有命令加 `-s "$SERIAL"` |
| 命令卡住不返回 | 端口/ stdout 继承 | 确认已 `export ...=5038`；改用 `terminal` 会话 |

- **容器内没有 `su`、`adb root` 被拒**（production build），需要 root 的操作（装模块、读 `/data/adb`）只能在手机端做。

## 输出规范
连接状态（`device` / `offline` / `unauthorized` / 空）+ 设备序列号 + 下一步建议；失败时附完整错误输出与对应处置步骤。
