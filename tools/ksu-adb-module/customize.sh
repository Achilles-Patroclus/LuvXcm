#!/system/bin/sh
# KernelSU 模块安装时执行：设置脚本权限 + 初始化配置与日志
ui_print "- ScoreTrace ADB 模块 v2.0 安装中..."

MODULE_ID="scoretrace_adb_autostart"
CONFIG_DIR="/data/adb/scoretrace_adb"

# 设置模块脚本可执行权限（KernelSU 解压后可能丢失）
for f in service.sh post-fs-data.sh uninstall.sh config.sh; do
    if [ -f "$MODPATH/$f" ]; then
        chmod 755 "$MODPATH/$f"
        ui_print "- 已设置 $f 权限"
    fi
done

# 初始化配置目录
mkdir -p "$CONFIG_DIR"

# 首次安装时写入默认配置
if [ ! -f "$CONFIG_DIR/config" ]; then
    cat > "$CONFIG_DIR/config" <<EOF
ENABLED=true
PORT=5555
AUTOSTART=true
EOF
    chmod 600 "$CONFIG_DIR/config"
    ui_print "- 已初始化配置文件"
fi

# 初始化日志文件（服务端用文件 + logcat 双通道记录，方便排查自启问题）
touch "$CONFIG_DIR/module.log" 2>/dev/null
chmod 600 "$CONFIG_DIR/module.log" 2>/dev/null

# restorecon 只在部分 ROM 存在，缺失时跳过（本模块仅在需要处调用它）
if ! command -v restorecon > /dev/null 2>&1; then
    ui_print "- 提示：当前 ROM 无 restorecon 命令，已跳过"
fi

ui_print "- 安装完成，重启后生效"
ui_print "- 重启后点击模块卡片打开 WebUI 控制面板"
