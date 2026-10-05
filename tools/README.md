# tools

ScoreTrace 项目的辅助脚本与数据生成工具。

| 文件 | 说明 |
| --- | --- |
| `fetch_school_data.py` | 抓取全量院校数据，生成 `app/src/main/assets/schools.json` |
| `parse_major_catalog.py` | 解析教育部专业目录 PDF，生成 `app/src/main/assets/majors_v2.json` |
| `adb-autostart.sh` | 手机端：开机自动让 adbd 在 5555 端口监听（脚本方案，需 root） |
| `adb-connect.sh` | 容器端：连接手机 ADB，供 `adb install` / `adb logcat` / `run-test` 使用 |
| `ksu-adb-module/` | 手机端：KernelSU 模块源码（开机自启 + WebUI 控制面板） |
| `dist/scoretrace_adb_autostart.zip` | 打包好的 KernelSU 模块，直接导入管理器安装 |

## ADB Root 调试通道

让 AiCode 容器直接对手机执行 `adb install` / `adb logcat` / `run-test`，**不使用 Android 11+ 的「无线调试」配对机制**（那套每次开机随机端口 + 配对码，需反复配对）。

原理：手机 root 后绕过无线调试，直接让 `adbd` 在**固定端口 5555** 监听 TCP，用传统 ADB 授权密钥（RSA）认证，首次授权并勾选「始终允许」后永久有效。

### 前提条件

- 手机已 Root（Magisk / KernelSU / APatch）
- 手机与 AiCode 容器在同一局域网
- 手机已开启「开发者选项 → USB 调试」

### 关键概念：两个属性的区别

| 属性 | 作用域 | 重启后 |
| --- | --- | --- |
| `service.adb.tcp.port` | **运行时**，adbd 当前实例监听端口 | **丢失**，需重新设置 |
| `persist.adb.tcp.port` | **持久**，落在 `/data/property`，init 起 adbd 前即可读到 | **保留** |

因此脚本同时写两者：`persist.*` 保证端口配置不丢，`service.*` 保证当前实例立即生效。

### 首次配置

1. 把 `tools/adb-autostart.sh` 复制到手机 `/data/adb/service.d/`，权限 `755`：
   ```bash
   su
   cp /sdcard/adb-autostart.sh /data/adb/service.d/
   chmod 755 /data/adb/service.d/adb-autostart.sh
   ```
2. 重启手机（或直接 `su -c 'setprop persist.adb.tcp.port 5555; setprop service.adb.tcp.port 5555; stop adbd; start adbd'` 立即生效）。
3. 手机开启「开发者选项 → USB 调试」。

> 也可用现成 Magisk 模块替代自写脚本：
> - [`wadbd`](https://magisk.dev/modules/wadbd/)（Wireless ADB Controller）——`wadbd enable-on-boot 5555` 即可开机自启固定端口，还提供 `--import-key` 直接导入 ADB 公钥（手机不弹授权窗时用）。
> - [`Tawezy7/enable_adbd_on_boot`](https://github.com/Tawezy7/enable_adbd_on_boot)——Magisk/KernelSU 模块，开机自动 `start adbd`。

### 日常使用

1. 在 AiCode 容器执行：
   ```bash
   ./tools/adb-connect.sh            # 自动探测本机 IP（与手机共享网络命名空间）
   ./tools/adb-connect.sh 192.168.1.157 5555   # 或显式指定
   ```
2. 首次连接时手机弹授权窗，勾选「**始终允许**」。
3. 之后每次连接无需配对。

### 容器侧注意事项

- `adb` 位于 `/root/android/sdk/platform-tools/adb`，**不在 PATH**；脚本会自动定位它。
- adb server 不会自行后台化，直接跑 `adb` 会挂住不返回，脚本统一用 `setsid ... </dev/null` 规避。
- AiCode 容器与安卓宿主**共享网络命名空间**（容器 `hostname -I` 显示的 IPv4 就是手机 LAN IP），所以 `adb-connect.sh` 不带参数时能自动探测到手机 IP，通常无需手动传。

### 安全提醒

- 5555 端口在局域网内开放，**仅在可信 Wi-Fi 下使用**。
- 临时关闭：`su -c 'setprop service.adb.tcp.port -1; stop adbd; start adbd'`
- 永久关闭：删除 `/data/adb/service.d/adb-autostart.sh` 并 `su -c 'setprop persist.adb.tcp.port ""'`，然后重启。

### 故障排查

| 现象 | 处理 |
| --- | --- |
| `adb devices` 显示空 | 确认 5555 在监听（手机端 `netstat -tlnp \| grep 5555`）；确认手机 IP 未因 DHCP 变化 |
| `unauthorized` | 在手机屏幕确认授权弹窗；或手机「开发者选项 → 撤销 USB 调试授权」后重连 |
| `offline` | `adb kill-server && adb connect <IP>:5555` |
| 端口未监听 | ROM 可能禁用 adbd 改端口，改用上面的 Magisk 模块方案 |
| 连不上但端口在监听 | `ping <手机IP>` 测通局域网；用 `adb -a nodaemon server start` 前台模式看详细日志 |

## ADB KernelSU 模块（WebUI 控制面板）

上面的 `adb-autostart.sh` 是「脚本方案」：把脚本丢进 `/data/adb/service.d/`。本节是「模块方案」：把它做成标准 KernelSU 模块，并附带图形化控制面板（WebUI）——在 KernelSU 管理器里点模块卡片即可开关 ADB、改端口、看状态与日志，无需敲命令。

源码在 `tools/ksu-adb-module/`，打包产物为 `tools/dist/scoretrace_adb_autostart.zip`。

### 模块结构

```txt
tools/ksu-adb-module/
├── module.prop      # 模块元信息（id=scoretrace_adb_autostart）
├── customize.sh     # 安装时：设置脚本权限 + 初始化配置
├── service.sh       # 开机自启：等待开机完成后让 adbd 在配置端口监听
├── uninstall.sh     # 卸载时：恢复 adbd 默认行为
├── config.sh        # WebUI 后端脚本（get / set / status / restart / logs）
└── webroot/         # WebUI 页面（index.html + style.css + script.js）
```

配置持久化在 `/data/adb/scoretrace_adb/config`（`ENABLED` / `PORT` / `AUTOSTART` 三项），重启手机不丢失。

### 首次安装

1. 把 `tools/dist/scoretrace_adb_autostart.zip` 传到手机
2. 打开 KernelSU 管理器 → 模块 → 从本地安装 → 选择该 ZIP
3. 重启手机
4. 重启后模块列表出现「ScoreTrace ADB 调试通道」卡片，点「打开」进入控制面板

> 需要 KernelSU ≥ 1.0（WebUI 功能）。管理器若提示不支持 WebUI，请升级 KernelSU。

### WebUI 控制面板

- 状态卡：ADB 是否在监听、当前端口、本机 IP
- 开关「ADB 总开关」「开机自启」
- 修改监听端口 → 保存并按新端口重启 adbd
- 「重启 ADB 服务」「复制连接命令」「查看最近日志」

面板通过 KernelSU 的 WebUI JavaScript API 调用模块内的 `config.sh`（新版用 `import { exec } from 'kernelsu'`，旧版回退全局 `ksu.exec`），脚本改动后无需重装模块。

### 容器端连接

模块安装并开机后，容器端连法不变：

```bash
./tools/adb-connect.sh                      # 自动探测手机 IP（容器与宿主共享网络命名空间）
./tools/adb-connect.sh 192.168.1.157 5555   # 或显式指定
```

首次连接手机弹授权窗，勾选「始终允许」后永久有效。

### 安全提醒

- 固定端口在局域网内开放，仅在可信 Wi-Fi 下使用
- 临时关闭：WebUI 面板关闭「ADB 总开关」
- 永久关闭：在 KernelSU 管理器中禁用或卸载模块

### 故障排查

| 现象 | 处理 |
| --- | --- |
| WebUI 打不开 | 确认模块已正确安装；KernelSU 需 ≥ 1.0 才支持 WebUI |
| 模块不生效 | 手机端 `logcat -s ScoreTrace-ADB` 看 `service.sh` 是否执行、权限是否 755 |
| 端口未监听 | 端口可能被占用，在 WebUI 里换端口；或检查是否有其他 ADB 服务 |
| 容器连不上 | `ping <手机IP>` 测网络；手机端确认端口在监听；`adb kill-server` 后重连 |

### 重新打包

```bash
cd tools/ksu-adb-module
zip -r ../dist/scoretrace_adb_autostart.zip \
    module.prop customize.sh service.sh uninstall.sh config.sh webroot
```
