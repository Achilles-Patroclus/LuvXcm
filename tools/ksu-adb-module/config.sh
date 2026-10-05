#!/system/bin/sh
# 供 WebUI 调用：读写配置 + 执行操作
# 用法: config.sh <action> [args]
#
# 注意：Android 的 toybox grep 不支持 -P（PCRE），netstat 在部分 ROM 上也
# 不完整，因此这里用 sed/awk 解析，监听状态直接读 /proc/net/tcp，避免依赖。

CONFIG_FILE="/data/adb/scoretrace_adb/config"
mkdir -p "$(dirname "$CONFIG_FILE")"

ACTION="$1"

read_port() {
    p=$(sed -n 's/^PORT=//p' "$CONFIG_FILE" 2>/dev/null | head -n1)
    case "$p" in
        ''|*[!0-9]*) p=5555 ;;
    esac
    [ "$p" -ge 1024 ] 2>/dev/null && [ "$p" -le 65535 ] 2>/dev/null || p=5555
    echo "$p"
}

# 判断某端口是否处于 TCP LISTEN
# 首选读 /proc/net/tcp（state 0A 即 LISTEN）；若不可读则回退 netstat / ss
is_listening() {
    want="$1"
    hex=$(printf '%04X' "$want" 2>/dev/null)
    for f in /proc/net/tcp /proc/net/tcp6; do
        [ -r "$f" ] || continue
        if awk -v h=":$hex" '$4=="0A" && index($2,h)>0 {f=1} END{exit(f?0:1)}' "$f" 2>/dev/null; then
            return 0
        fi
    done
    if netstat -tln 2>/dev/null | grep -Eq "[:.]$want[[:space:]]"; then
        return 0
    fi
    if ss -tln 2>/dev/null | grep -Eq "[:.]$want[[:space:]]"; then
        return 0
    fi
    return 1
}

# 获取手机在局域网中的 IPv4 地址
get_ip() {
    ip=$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.* src \([0-9][0-9.]*\).*/\1/p' | head -n1)
    [ -n "$ip" ] && { echo "$ip"; return; }
    ip=$(ip -4 addr show wlan0 2>/dev/null | sed -n 's/.*inet \([0-9][0-9.]*\)\/.*/\1/p' | head -n1)
    [ -n "$ip" ] && { echo "$ip"; return; }
    ip=$(ifconfig wlan0 2>/dev/null | awk '/inet /{for(i=1;i<=NF;i++){v=$i; sub(/^addr:/,"",v); if(v ~ /^[0-9]+(\.[0-9]+){3}$/){print v; exit}}}')
    [ -n "$ip" ] && { echo "$ip"; return; }
    echo "unknown"
}

case "$ACTION" in
    get)
        if [ -f "$CONFIG_FILE" ]; then
            cat "$CONFIG_FILE"
        else
            printf 'ENABLED=true\nPORT=5555\nAUTOSTART=true\n'
        fi
        ;;

    set)
        KEY="$2"
        VALUE="$3"
        if [ -z "$KEY" ] || [ -z "$VALUE" ]; then
            echo "ERROR: missing key or value"
            exit 1
        fi
        # 只接受已知键与合法取值，避免写入任意配置行
        case "$KEY" in
            ENABLED|AUTOSTART)
                case "$VALUE" in
                    true|false) ;;
                    *) echo "ERROR: bad value for $KEY"; exit 1 ;;
                esac
                ;;
            PORT)
                case "$VALUE" in
                    *[!0-9]*) echo "ERROR: bad port"; exit 1 ;;
                esac
                if [ "$VALUE" -lt 1024 ] || [ "$VALUE" -gt 65535 ]; then
                    echo "ERROR: port out of range"; exit 1
                fi
                ;;
            *) echo "ERROR: unknown key $KEY"; exit 1 ;;
        esac
        touch "$CONFIG_FILE"
        sed -i "/^$KEY=/d" "$CONFIG_FILE"
        printf '%s=%s\n' "$KEY" "$VALUE" >> "$CONFIG_FILE"
        echo "OK"
        ;;

    status)
        PORT=$(read_port)
        if is_listening "$PORT"; then LISTENING=1; else LISTENING=0; fi
        echo "PORT=$PORT"
        echo "LISTENING=$LISTENING"
        echo "IP=$(get_ip)"
        ;;

    restart)
        PORT=$(read_port)
        setprop service.adb.tcp.port "$PORT"
        stop adbd
        start adbd
        sleep 1
        echo "adbd restarted on port $PORT"
        ;;

    logs)
        logcat -d -t 40 -s ScoreTrace-ADB 2>/dev/null | tail -n 20
        ;;

    *)
        echo "Unknown action: $ACTION"
        exit 1
        ;;
esac
