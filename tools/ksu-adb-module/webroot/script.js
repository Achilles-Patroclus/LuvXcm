// ScoreTrace ADB 控制面板
//
// 兼容两代 KernelSU WebUI API：
//   1) 新版（KernelSU ≥ 1.0，npm 包 kernelsu）：import { exec, toast } from 'kernelsu'
//      exec 返回 Promise<{errno, stdout, stderr}>
//   2) 旧版（全局 ksu 对象）：ksu.exec(cmd) 同步返回字符串，
//      或 ksu.exec(cmd, "回调名") 异步 + 全局具名回调 (exitCode, stdout, stderr)
// 通过动态 import 优先用新版，失败再回退旧版。

(function () {
    'use strict';

    var MODULE_ID = 'scoretrace_adb_autostart';
    var CONFIG_SCRIPT = '/data/adb/modules/' + MODULE_ID + '/config.sh';

    var ksuModule = null;
    var ksuChecked = false;

    async function loadKsuModule() {
        if (ksuChecked) return ksuModule;
        ksuChecked = true;
        try {
            var m = await import('kernelsu');
            if (m && typeof m.exec === 'function') ksuModule = m;
        } catch (e) {
            ksuModule = null;
        }
        return ksuModule;
    }

    function legacyExec(cmd) {
        return new Promise(function (resolve) {
            if (typeof ksu === 'undefined' || typeof ksu.exec !== 'function') {
                resolve('');
                return;
            }
            // 旧版同步形式：直接返回输出字符串
            try {
                var out = ksu.exec(cmd);
                if (typeof out === 'string') { resolve(out); return; }
            } catch (e) { /* 不是同步形式，继续尝试异步形式 */ }

            // 旧版异步形式：需要全局具名回调
            var cbName = 'ksu_cb_' + Date.now() + '_' + Math.floor(Math.random() * 1e6);
            window[cbName] = function (exitCode, stdout) {
                try { delete window[cbName]; } catch (e) { window[cbName] = undefined; }
                resolve(stdout || '');
            };
            try {
                ksu.exec(cmd, cbName);
            } catch (e) {
                resolve('');
            }
        });
    }

    async function runConfig(action) {
        var full = 'sh ' + CONFIG_SCRIPT + ' ' + action;
        var m = await loadKsuModule();
        if (m) {
            try {
                var res = await m.exec(full);
                if (res && typeof res.stdout === 'string') return res.stdout.trim();
                return '';
            } catch (e) {
                return '';
            }
        }
        return (await legacyExec(full)).trim();
    }

    async function toast(msg) {
        var m = await loadKsuModule();
        if (m && typeof m.toast === 'function') { m.toast(msg); return; }
        if (typeof ksu !== 'undefined' && typeof ksu.toast === 'function') { ksu.toast(msg); return; }
        var el = document.createElement('div');
        el.className = 'toast';
        el.textContent = msg;
        document.body.appendChild(el);
        setTimeout(function () { el.remove(); }, 2000);
    }

    function parseKeyValues(text) {
        var out = {};
        (text || '').split('\n').forEach(function (line) {
            var i = line.indexOf('=');
            if (i > 0) out[line.slice(0, i).trim()] = line.slice(i + 1).trim();
        });
        return out;
    }

    async function refreshStatus() {
        var badge = document.getElementById('adb-status');
        var portDisplay = document.getElementById('port-display');
        var ipDisplay = document.getElementById('ip-display');

        var info = parseKeyValues(await runConfig('status'));
        var port = info.PORT || '--';
        var listening = info.LISTENING === '1';

        portDisplay.textContent = port;
        ipDisplay.textContent = info.IP || '--';
        badge.textContent = listening ? '运行中' : '已停止';
        badge.className = 'status-badge ' + (listening ? 'online' : 'offline');
    }

    async function loadConfig() {
        var cfg = parseKeyValues(await runConfig('get'));
        document.getElementById('toggle-enabled').checked = cfg.ENABLED === 'true';
        document.getElementById('toggle-autostart').checked = cfg.AUTOSTART === 'true';
        document.getElementById('port-input').value = cfg.PORT || 5555;
    }

    async function copyText(text) {
        try {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                await navigator.clipboard.writeText(text);
                return true;
            }
        } catch (e) { /* 回退到 execCommand */ }
        try {
            var ta = document.createElement('textarea');
            ta.value = text;
            ta.style.position = 'fixed';
            ta.style.opacity = '0';
            document.body.appendChild(ta);
            ta.select();
            var ok = document.execCommand('copy');
            ta.remove();
            return ok;
        } catch (e) {
            return false;
        }
    }

    document.addEventListener('DOMContentLoaded', async function () {
        await loadConfig();
        await refreshStatus();

        document.getElementById('toggle-enabled').addEventListener('change', async function (e) {
            var value = e.target.checked ? 'true' : 'false';
            await runConfig('set ENABLED ' + value);
            toast(value === 'true' ? '已启用 ADB' : '已禁用 ADB');
        });

        document.getElementById('toggle-autostart').addEventListener('change', async function (e) {
            var value = e.target.checked ? 'true' : 'false';
            await runConfig('set AUTOSTART ' + value);
            toast(value === 'true' ? '开机自启已开启' : '开机自启已关闭');
        });

        document.getElementById('save-port').addEventListener('click', async function () {
            var port = parseInt(document.getElementById('port-input').value, 10);
            if (!(port >= 1024 && port <= 65535)) {
                toast('端口范围应为 1024-65535');
                return;
            }
            await runConfig('set PORT ' + port);
            toast('端口已改为 ' + port + '，正在重启 adbd...');
            await runConfig('restart');
            await refreshStatus();
        });

        document.getElementById('restart-adbd').addEventListener('click', async function () {
            toast('正在重启 ADB 服务...');
            await runConfig('restart');
            setTimeout(refreshStatus, 1500);
        });

        document.getElementById('copy-command').addEventListener('click', async function () {
            var info = parseKeyValues(await runConfig('status'));
            var cmd = 'adb connect ' + (info.IP || '') + ':' + (info.PORT || '5555');
            var ok = await copyText(cmd);
            toast(ok ? '已复制：' + cmd : cmd);
        });

        document.getElementById('view-logs').addEventListener('click', async function () {
            var logs = await runConfig('logs');
            document.getElementById('logs-content').textContent = logs || '暂无日志';
            document.getElementById('logs-section').classList.remove('hidden');
        });

        document.getElementById('close-logs').addEventListener('click', function () {
            document.getElementById('logs-section').classList.add('hidden');
        });

        setInterval(refreshStatus, 5000);
    });
})();
