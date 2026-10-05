#!/system/bin/sh
# ScoreTrace ADB 模块 v2.0 - post-fs-data 阶段「执行埋点」
#
# 该阶段是「阻塞」的，且在 Zygote 启动前，官方文档建议不要在此 setprop
# （可能造成开机死锁），因此这里不做任何实际配置，只记录本阶段是否被触发。
# 作用：当 service.sh 疑似没执行时，用本埋点判断「模块脚本到底跑没跑」——
#   - 两个阶段都有日志 → 模块脚本在跑，问题在 service.sh 内部逻辑，看它的日志定位
#   - 只有本阶段有日志   → 该 KernelSU 版本未按预期执行 service.sh
#   - 两个阶段都没日志   → 模块根本没被执行（未启用 / 未重启 / 安装异常）
# 真正的 adbd 启动放在 service.sh（late_start，系统更稳定）。

LOG_FILE="/data/adb/scoretrace_adb/module.log"
mkdir -p /data/adb/scoretrace_adb 2>/dev/null
echo "[$(date '+%Y-%m-%d %H:%M:%S')] [post-fs-data] 被调用 KSU=$KSU KSU_VER=$KSU_VER uid=$(id -u 2>/dev/null)" >> "$LOG_FILE" 2>/dev/null
log -t ScoreTrace-ADB "post-fs-data.sh invoked" 2>/dev/null
exit 0
