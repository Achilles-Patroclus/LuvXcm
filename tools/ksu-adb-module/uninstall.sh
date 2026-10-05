#!/system/bin/sh
# 卸载模块时恢复默认 adbd 行为
setprop service.adb.tcp.port -1
setprop persist.adb.tcp.port ""
stop adbd
start adbd
rm -rf /data/adb/scoretrace_adb
log -t ScoreTrace-ADB "Module uninstalled, adbd restored"
