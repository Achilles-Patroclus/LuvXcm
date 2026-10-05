#!/system/bin/sh
# 供 WebUI 调用：读写配置 + 状态检测 + ADB 授权管理
# 用法: config.sh <action> [args]
#
# 兼容性约束：只用 toybox/busybox 内置命令（sed/awk/grep/printf/date/cat/echo/cut/head/tail
# 等）。不使用 grep -P（toybox grep 无 PCRE）、不使用 netstat -p。

CONFIG_FILE="/data/adb/scoretrace_adb/config"
LOG_FILE="/data/adb/scoretrace_adb/module.log"
ADB_KEYS="/data/misc/adb/adb_keys"
mkdir -p "$(dirname "$CONFIG_FILE")"

ACTION="$1"

read_port() {
    p=$(sed -n 's/^PORT=//p' "$CONFIG_FILE" 2>/dev/null | head -n1)
    case "$p" in
        ''|*[!0-9]*) p=5555 ;;
    esac
    if [ "$p" -lt 1024 ] || [ "$p" -gt 65535 ]; then
        p=5555
    fi
    echo "$p"
}

# 端口是否处于 TCP LISTEN：优先直读 /proc/net/tcp(4)（state 0A 即 LISTEN），
# IPv4 / IPv6 任一命中即视为运行中；不可读时回退 netstat / ss。
is_listening() {
    want="$1"
    hex=$(printf '%04X' "$want" 2>/dev/null)
    for f in /proc/net/tcp /proc/net/tcp6; do
        [ -r "$f" ] || continue
        if awk -v h=":$hex" '$4=="0A" && index($2,h)>0 {f=1} END{exit(f?0:1)}' "$f" 2>/dev/null; then
            return 0
        fi
    done
    if command -v netstat >/dev/null 2>&1 && netstat -tln 2>/dev/null | grep -Eq "[:.]$want[[:space:]]"; then
        return 0
    fi
    if command -v ss >/dev/null 2>&1 && ss -tln 2>/dev/null | grep -Eq "[:.]$want[[:space:]]"; then
        return 0
    fi
    return 1
}

# 获取手机在局域网中的 IPv4 地址（依次尝试 ip route / ip addr / ifconfig）
get_ip() {
    ip=$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.* src \([0-9][0-9.]*\).*/\1/p' | head -n1)
    [ -n "$ip" ] && { echo "$ip"; return; }
    ip=$(ip -4 addr show wlan0 2>/dev/null | sed -n 's/.*inet \([0-9][0-9.]*\)\/.*/\1/p' | head -n1)
    [ -n "$ip" ] && { echo "$ip"; return; }
    ip=$(ifconfig wlan0 2>/dev/null | awk '/inet /{for(i=1;i<=NF;i++){v=$i; sub(/^addr:/,"",v); if(v ~ /^[0-9]+(\.[0-9]+){3}$/){print v; exit}}}')
    [ -n "$ip" ] && { echo "$ip"; return; }
    echo "unknown"
}

count_keys() {
    if [ -f "$ADB_KEYS" ]; then
        n=$(grep -cE '^(QAAAA|ssh-rsa|ssh-ed25519|ecdsa-sha2-)' "$ADB_KEYS" 2>/dev/null)
        [ -z "$n" ] && n=0
        echo "$n"
    else
        echo 0
    fi
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
        echo "LAST_CHECK=$(date '+%Y-%m-%d %H:%M:%S')"
        ;;

    restart)
        PORT=$(read_port)
        setprop persist.adb.tcp.port "$PORT"
        setprop service.adb.tcp.port "$PORT"
        stop adbd
        start adbd
        sleep 2
        echo "adbd restarted on port $PORT"
        ;;

    logs)
        # 优先读模块自写的文件日志；没有则回退 logcat
        if [ -f "$LOG_FILE" ]; then
            tail -n 50 "$LOG_FILE"
        else
            logcat -d -t 40 -s ScoreTrace-ADB 2>/dev/null | tail -n 20
        fi
        ;;

    keys-count)
        echo "COUNT=$(count_keys)"
        ;;

    keys-add)
        # 公钥可能含空格（base64 + 注释），因此取全部剩余参数
        shift
        KEY_CONTENT="$*"
        KEY_CONTENT=$(printf '%s' "$KEY_CONTENT" | tr -d '\r\n')
        if [ -z "$KEY_CONTENT" ]; then
            echo "ERROR: empty key"
            exit 1
        fi
        # 基本格式校验，拒绝明显不是公钥的内容
        case "$KEY_CONTENT" in
            QAAAA*|ssh-rsa\ *|ssh-ed25519\ *|ecdsa-sha2-*) ;;
            *) echo "ERROR: 不是有效的 ADB 公钥（应以 QAAAA 或 ssh-rsa / ssh-ed25519 开头）"; exit 1 ;;
        esac
        mkdir -p /data/misc/adb
        touch "$ADB_KEYS"
        if grep -qF "$KEY_CONTENT" "$ADB_KEYS" 2>/dev/null; then
            echo "OK (already present)"
            exit 0
        fi
        printf '%s\n' "$KEY_CONTENT" >> "$ADB_KEYS"
        chmod 640 "$ADB_KEYS" 2>/dev/null
        chown system:shell "$ADB_KEYS" 2>/dev/null
        if command -v restorecon >/dev/null 2>&1; then
            restorecon "$ADB_KEYS" 2>/dev/null
        fi
        echo "OK"
        ;;

    keys-clear)
        rm -f "$ADB_KEYS"
        echo "OK"
        ;;

    *)
        echo "Unknown action: $ACTION"
        exit 1
        ;;
esac
