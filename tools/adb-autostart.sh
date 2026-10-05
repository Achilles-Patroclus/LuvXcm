#!/system/bin/sh
# ScoreTrace ADB 开机自启脚本
#
# 放置位置：/data/adb/service.d/adb-autostart.sh   （Magisk / KernelSU 通用）
# 权限：chmod 755
# 作用：开机后让 adbd 在固定端口 5555 监听 TCP，供 AiCode 容器 adb connect，
#       绕开 Android 11+ 无线调试的随机端口 + 配对码机制。
#
# 为什么同时写两个属性：
#   - persist.adb.tcp.port：持久属性（落在 /data/property），跨重启保留，
#     init 在起 adbd 前即可读到，是端口配置不丢的关键。
#   - service.adb.tcp.port：运行时属性，adbd 当前实例实际监听的端口。
#   只写 service.* 的话，重启后端口配置会丢，必须靠本脚本每次开机重新设置。

ADB_PORT=5555

# 等待系统启动完成
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 2
done

# 额外等待，确保 adbd 已完成初始化
sleep 5

setprop persist.adb.tcp.port "$ADB_PORT"
setprop service.adb.tcp.port "$ADB_PORT"
stop adbd
start adbd

log -t ScoreTrace-ADB "adbd restarted, listening on tcp:$ADB_PORT"
