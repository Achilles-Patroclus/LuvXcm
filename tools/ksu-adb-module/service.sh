#!/system/bin/sh
# ScoreTrace ADB 开机自启脚本（KernelSU service 阶段执行）
#
# KernelSU 在 service 阶段同步执行本脚本，脚本必须尽快返回，否则会拖慢开机。
# 因此把「等待开机完成 + 配置 adbd」的逻辑整体放进后台子 shell。
#
# 为什么同时写两个属性：
#   - service.adb.tcp.port：当前 adbd 实例监听的端口，重启后丢失。
#   - persist.adb.tcp.port：落在 /data/property，跨重启保留。
#   本模块每次开机都会重设两者，保证监听端口与配置始终一致。

CONFIG_FILE="/data/adb/scoretrace_adb/config"

(
    # 等待系统启动完成
    until [ "$(getprop sys.boot_completed)" = "1" ]; do
        sleep 2
    done

    # 额外等待 5 秒确保 adbd 就绪
    sleep 5

    # 读取配置（配置文件为 KEY=VALUE 的 shell 片段）
    ENABLED=true
    PORT=5555
    AUTOSTART=true
    [ -f "$CONFIG_FILE" ] && . "$CONFIG_FILE"

    if [ "$ENABLED" != "true" ] || [ "$AUTOSTART" != "true" ]; then
        log -t ScoreTrace-ADB "Module disabled, skipping autostart"
        exit 0
    fi

    # 启动 adbd 监听指定端口
    setprop persist.adb.tcp.port "$PORT"
    setprop service.adb.tcp.port "$PORT"
    stop adbd
    start adbd

    log -t ScoreTrace-ADB "adbd restarted on port $PORT at $(date)"
) &
