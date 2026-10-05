#!/bin/bash
# ScoreTrace 容器端 ADB 连接脚本
#
# 用法：./tools/adb-connect.sh [手机IP] [端口]
# 示例：./tools/adb-connect.sh 192.168.1.157 5555
#
# 不带参数时自动探测本机 IPv4 作为手机 IP —— AiCode 容器与安卓宿主共享
# 网络命名空间（容器 IP 就是手机 LAN IP），故通常无需手动传 IP。

set -u

ADB_BIN="$(command -v adb 2>/dev/null || true)"
if [ -z "$ADB_BIN" ] && [ -x /root/android/sdk/platform-tools/adb ]; then
    ADB_BIN=/root/android/sdk/platform-tools/adb
fi
if [ -z "$ADB_BIN" ]; then
    echo "错误：找不到 adb。请安装 platform-tools 或把它加入 PATH。" >&2
    exit 1
fi

# AiCode 容器与安卓宿主共享网络命名空间，宿主侧常已占用 127.0.0.1:5037 的 adb server，
# 容器内默认端口的 adb 命令会因此挂起。改用独立端口避免冲突（可用环境变量覆盖）。
export ANDROID_ADB_SERVER_PORT="${ANDROID_ADB_SERVER_PORT:-5038}"

detect_ip() {
    hostname -I 2>/dev/null | tr ' ' '\n' | grep -E '^[0-9]+(\.[0-9]+){3}$' | head -n1
}

PHONE_IP="${1:-$(detect_ip)}"
PHONE_PORT="${2:-5555}"

if [ -z "$PHONE_IP" ]; then
    echo "用法: $0 [手机IP] [端口，默认5555]"
    echo "获取手机 IP：手机「设置 → 关于手机 → 状态信息」中的 IP 地址"
    exit 1
fi

echo "adb: $ADB_BIN"

# 清理幽灵 emulator-* 设备，避免后续 adb 命令必须用 -s 指定目标
setsid "$ADB_BIN" devices 2>/dev/null | awk '/^emulator-/{print $1}' | while read -r dev; do
    echo "清理幽灵设备 $dev"
    setsid "$ADB_BIN" disconnect "$dev" </dev/null 2>/dev/null
done

echo "正在连接 $PHONE_IP:$PHONE_PORT ..."
# adb server 不会自行后台化，必须 setsid 且重定向 stdin，否则命令会一直挂住不返回
setsid "$ADB_BIN" connect "$PHONE_IP:$PHONE_PORT" </dev/null

echo ""
echo "当前已连接设备："
setsid "$ADB_BIN" devices -l </dev/null

echo ""
echo "如果上方列表中出现你的设备，说明连接成功。"
echo "如果显示 unauthorized，请在手机屏幕上确认授权弹窗（勾选「始终允许」）。"
echo "如果显示 offline，请执行：adb kill-server && adb connect $PHONE_IP:$PHONE_PORT"
echo "或用一键重连脚本：./tools/adb-reconnect.sh $PHONE_IP $PHONE_PORT"
