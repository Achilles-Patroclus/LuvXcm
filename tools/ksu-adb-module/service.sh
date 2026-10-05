#!/system/bin/sh
# ScoreTrace ADB 自启脚本 v2.0
#
# 执行时机：KernelSU 的 service 阶段（late_start service mode）。
# 官方文档（context7 / kernelsu.org）明确指出 late_start service mode 是「非阻塞」的：
# 脚本与开机流程并行执行，不会拖住开机。因此这里同步等待 boot_completed 是安全的，
# 也不需要后台化——v1.0 用后台子 shell `( ... ) &`，父脚本一退出子进程就可能被回收，
# 反倒可能导致自启失败。
#
# 全程写文件日志 + logcat 双通道，并 set -x 记录执行过程，便于定位「自启为何没生效」。

MODDIR=${0%/*}
CONFIG_DIR="/data/adb/scoretrace_adb"
CONFIG_FILE="$CONFIG_DIR/config"
LOG_FILE="$CONFIG_DIR/module.log"

mkdir -p "$CONFIG_DIR" 2>/dev/null

log_msg() {
    msg="[$(date '+%Y-%m-%d %H:%M:%S')] [service] $1"
    echo "$msg" >> "$LOG_FILE" 2>/dev/null
    log -t ScoreTrace-ADB "$1" 2>/dev/null
}

# 端口是否处于 TCP LISTEN（IPv4 或 IPv6 任一命中即算）
listen_check() {
    h=$(printf '%04X' "$1" 2>/dev/null)
    for f in /proc/net/tcp /proc/net/tcp6; do
        [ -r "$f" ] || continue
        awk -v p=":$h" '$4=="0A" && index($2,p)>0 {f=1} END{exit(f?0:1)}' "$f" 2>/dev/null && return 0
    done
    return 1
}

# 把本脚本的 stderr（含 set -x 追踪）追加到日志文件；写不进去也不影响主流程
if touch "$LOG_FILE" 2>/dev/null; then
    exec 2>>"$LOG_FILE"
fi
set -x

log_msg "===== service.sh 开始执行 ====="
log_msg "KSU=$KSU KSU_VER=$KSU_VER KSU_VER_CODE=$KSU_VER_CODE MODDIR=$MODDIR"
log_msg "uid=$(id 2>&1)"

# 等待系统启动完成（最长 5 分钟，避免死循环）
WAIT_COUNT=0
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 1))
    if [ "$WAIT_COUNT" -gt 150 ]; then
        log_msg "等待开机超时（5 分钟），强制继续"
        break
    fi
done
log_msg "系统已启动（等待 $((WAIT_COUNT * 2)) 秒）"
sleep 5

# 读取配置
ENABLED=true
PORT=5555
AUTOSTART=true
if [ -f "$CONFIG_FILE" ]; then
    . "$CONFIG_FILE"
    log_msg "已读取配置：ENABLED=$ENABLED PORT=$PORT AUTOSTART=$AUTOSTART"
else
    log_msg "配置文件不存在，使用默认值"
fi

if [ "$ENABLED" != "true" ] || [ "$AUTOSTART" != "true" ]; then
    log_msg "模块已禁用（ENABLED=$ENABLED AUTOSTART=$AUTOSTART），跳过自启"
    exit 0
fi

# 让 adbd 监听指定端口。persist.* 跨重启保留（init 起 adbd 前即可读到），
# service.* 让当前 adbd 实例立即生效。
log_msg "准备让 adbd 监听端口 $PORT"
setprop persist.adb.tcp.port "$PORT"
log_msg "setprop persist.adb.tcp.port $PORT -> rc=$?"
setprop service.adb.tcp.port "$PORT"
log_msg "setprop service.adb.tcp.port $PORT -> rc=$?"
stop adbd 2>>"$LOG_FILE"
sleep 1
start adbd 2>>"$LOG_FILE"
sleep 3

if listen_check "$PORT"; then
    log_msg "adbd 已在端口 $PORT 监听 ✅"
else
    log_msg "adbd 未在端口 $PORT 监听 ❌（检查端口占用 / SELinux / ROM 是否禁用 TCP 调试）"
fi
log_msg "===== service.sh 执行结束 ====="
exit 0
