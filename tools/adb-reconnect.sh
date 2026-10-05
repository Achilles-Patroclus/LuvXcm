#!/bin/bash
# ScoreTrace 容器端一键重连脚本
#
# 功能：清理幽灵设备（emulator-*）+ 重连手机 + 验证连通性
# 用法：./tools/adb-reconnect.sh [手机IP] [端口=5555]
# 不带参数时自动探测本机 IPv4 作为手机 IP（AiCode 容器与安卓宿主共享网络命名空间）。

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
    echo "用法: $0 [手机IP] [端口，默认5555]" >&2
    exit 1
fi

# adb server 不会自行后台化，统一 setsid 且重定向 stdin，否则命令会挂住不返回
adb_run() { setsid "$ADB_BIN" "$@" </dev/null; }

echo "═══════════════════════════════════════"
echo " ScoreTrace ADB 一键重连"
echo " adb: $ADB_BIN"
echo " 目标：$PHONE_IP:$PHONE_PORT"
echo "═══════════════════════════════════════"

echo "[1/5] 停止 adb 服务..."
adb_run kill-server
sleep 1

echo "[2/5] 启动 adb 服务..."
adb_run start-server
sleep 1

echo "[3/5] 清理幽灵设备 (emulator-*)..."
adb_run devices | awk '/^emulator-/{print $1}' | while read -r dev; do
    echo "  断开 $dev"
    adb_run disconnect "$dev" 2>/dev/null
done

echo "[4/5] 连接手机 $PHONE_IP:$PHONE_PORT ..."
adb_run connect "$PHONE_IP:$PHONE_PORT"

echo "[5/5] 验证连通性..."
sleep 2
DEVICES=$(adb_run devices -l | grep -E "^$PHONE_IP:$PHONE_PORT[[:space:]]+device")
if [ -n "$DEVICES" ]; then
    echo ""
    echo "连接成功："
    echo "$DEVICES"
    echo ""
    echo "推荐（可持久化到 ~/.bashrc）："
    echo "  export ANDROID_SERIAL=$PHONE_IP:$PHONE_PORT"
    echo ""
    echo "快速测试："
    echo "  $ADB_BIN logcat -d -t 50"
    echo "  $ADB_BIN -s $PHONE_IP:$PHONE_PORT shell getprop ro.product.model"
else
    echo ""
    echo "未发现已连接设备，请检查："
    echo "  1. 手机已开机且与容器在同一 Wi-Fi"
    echo "  2. 手机 IP 是否仍是 $PHONE_IP（打开 WebUI 状态卡查看）"
    echo "  3. adbd 是否在监听（WebUI 状态卡是否显示「运行中」）"
    echo "  4. 授权是否有效（WebUI 授权管理卡片）"
    echo ""
    adb_run devices -l
fi
