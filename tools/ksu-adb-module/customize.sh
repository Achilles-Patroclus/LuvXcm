#!/system/bin/sh
# KernelSU 模块安装时执行：设置脚本权限 + 初始化配置
ui_print "- ScoreTrace ADB 模块安装中..."

# 设置脚本可执行权限（KernelSU 解压时可能丢失）
chmod 755 "$MODPATH/service.sh"
chmod 755 "$MODPATH/uninstall.sh"
chmod 755 "$MODPATH/config.sh"

# 初始化配置文件目录
CONFIG_DIR="/data/adb/scoretrace_adb"
mkdir -p "$CONFIG_DIR"

# 首次安装时写入默认配置
if [ ! -f "$CONFIG_DIR/config" ]; then
    cat > "$CONFIG_DIR/config" <<EOF
ENABLED=true
PORT=5555
AUTOSTART=true
EOF
    chmod 600 "$CONFIG_DIR/config"
fi

ui_print "- 安装完成，重启后生效"
ui_print "- 重启后在 KernelSU 管理器点击模块卡片打开控制面板"
