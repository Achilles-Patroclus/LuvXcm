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
