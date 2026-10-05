#!/system/bin/sh
# 卸载模块时恢复默认 adbd 行为，并保留一条卸载日志
log -t ScoreTrace-ADB "Module uninstalling, restoring adbd"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] [uninstall] 模块卸载，恢复 adbd 默认" >> /data/adb/scoretrace_adb/module.log 2>/dev/null

setprop service.adb.tcp.port -1
setprop persist.adb.tcp.port ""
stop adbd
start adbd
rm -rf /data/adb/scoretrace_adb
