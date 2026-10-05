// ScoreTrace ADB 控制面板 v2.0
//
// 兼容两代 KernelSU WebUI API：
//   1) 新版（KernelSU ≥ 1.0，npm 包 kernelsu）：import { exec, toast } from 'kernelsu'
//      exec 返回 Promise<{errno, stdout, stderr}>
//   2) 旧版（全局 ksu 对象）：ksu.exec(cmd, cbName) + 全局具名回调 (exitCode, stdout, stderr)
//      （WebUI-Next 也支持 ksu.exec(cmd) 同步返回字符串）
// 统一用「动态 import 新版 → 回退全局 ksu」的双路桥接，且旧版只调用一次，避免副作用重复。

(function () {
    'use strict';

    var MODULE_ID = 'scoretrace_adb_autostart';
    var CONFIG_SCRIPT = '/data/adb/modules/' + MODULE_ID + '/config.sh';

    var ksuModule = null;
    var ksuChecked = false;
    var lastListening = null;

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

    // 旧版全局 ksu：只调用一次。传回调名，兼容异步实现；若实现直接返回字符串则直接用。
    function legacyExec(cmd) {
        return new Promise(function (resolve) {
            if (typeof ksu === 'undefined' || typeof ksu.exec !== 'function') {
                resolve('');
                return;
            }
            var done = false;
            function finish(v) { if (!done) { done = true; resolve(v == null ? '' : v); } }

            var cbName = 'ksu_cb_' + Date.now() + '_' + Math.floor(Math.random() * 1e6);
            window[cbName] = function (code, stdout) {
                try { delete window[cbName]; } catch (e) { window[cbName] = undefined; }
                finish(stdout);
            };

            var out;
            try {
                out = ksu.exec(cmd, cbName);
            } catch (e) {
                finish('');
                return;
            }
            if (typeof out === 'string') {
                finish(out);
                return;
            }
            // 异步形式：等回调，最多 8 秒
            setTimeout(function () { finish(''); }, 8000);
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
        setTimeout(function () { if (el.parentNode) el.remove(); }, 2500);
    }

    function parseKV(text) {
        var out = {};
        (text || '').split('\n').forEach(function (line) {
            var i = line.indexOf('=');
            if (i > 0) out[line.slice(0, i).trim()] = line.slice(i + 1).trim();
        });
        return out;
    }

    // 安全的单引号 shell 引用，供把公钥作为参数传给 config.sh
    function shQuote(s) {
        return "'" + String(s).replace(/'/g, "'\\''") + "'";
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

    async function refreshStatus() {
        var info = parseKV(await runConfig('status'));
        var listening = info.LISTENING === '1';
        var port = info.PORT || '5555';
        var ip = info.IP && info.IP !== 'unknown' ? info.IP : '';

        var badge = document.getElementById('adb-status');
        badge.textContent = listening ? '运行中' : '已停止';
        badge.className = 'status-badge ' + (listening ? 'online' : 'offline');

        document.getElementById('port-display').textContent = info.PORT || '--';
        document.getElementById('ip-display').textContent = info.IP || '--';
        document.getElementById('last-check').textContent = info.LAST_CHECK || '--';

        var cmd = 'adb connect ' + (ip || '<在WebUI顶部查看IP>') + ':' + port;
        document.getElementById('conn-cmd').textContent = cmd;

        var hint;
        if (listening) {
            hint = '手机端已就绪。在容器执行上面的命令即可连接；首次连接需在手机弹窗中点「始终允许」。\n' +
                   '若显示 unauthorized：手机上确认授权弹窗；若 offline：容器执行 adb kill-server 后重连。';
        } else {
            hint = 'adbd 未在端口 ' + port + ' 监听：点上方「重启 ADB 服务」，或检查该端口是否被占用。';
        }
        document.getElementById('diag-hint').textContent = hint;

        if (lastListening !== null && lastListening !== listening) {
            toast(listening ? 'ADB 已开始监听' : 'ADB 已停止监听');
        }
        lastListening = listening;
    }

    async function loadConfig() {
        var cfg = parseKV(await runConfig('get'));
        document.getElementById('toggle-enabled').checked = cfg.ENABLED === 'true';
        document.getElementById('toggle-autostart').checked = cfg.AUTOSTART === 'true';
        document.getElementById('port-input').value = cfg.PORT || 5555;
    }

    async function refreshKeys() {
        var info = parseKV(await runConfig('keys-count'));
        var n = parseInt(info.COUNT, 10);
        document.getElementById('key-count').textContent = (isNaN(n) ? '--' : n) + ' 个';
    }

    document.addEventListener('DOMContentLoaded', async function () {
        await loadConfig();
        await refreshStatus();
        await refreshKeys();

        document.getElementById('refresh-status').addEventListener('click', async function () {
            toast('正在刷新...');
            await refreshStatus();
        });

        document.getElementById('toggle-enabled').addEventListener('change', async function (e) {
            var value = e.target.checked ? 'true' : 'false';
            await runConfig('set ENABLED ' + value);
            toast(value === 'true' ? '已启用 ADB' : '已禁用 ADB');
            await refreshStatus();
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
            await refreshStatus();
        });

        document.getElementById('view-logs').addEventListener('click', async function () {
            var logs = await runConfig('logs');
            document.getElementById('logs-content').textContent = logs || '暂无日志';
            document.getElementById('logs-section').classList.remove('hidden');
        });

        document.getElementById('close-logs').addEventListener('click', function () {
            document.getElementById('logs-section').classList.add('hidden');
        });

        document.getElementById('refresh-keys').addEventListener('click', async function () {
            await refreshKeys();
            toast('授权列表已刷新');
        });

        document.getElementById('add-key').addEventListener('click', async function () {
            var input = document.getElementById('key-input');
            var key = (input.value || '').trim().replace(/\s+/g, ' ');
            if (!key) { toast('请先粘贴 ADB 公钥'); return; }
            var out = await runConfig('keys-add ' + shQuote(key));
            toast(out || '已添加');
            if (out.indexOf('ERROR') === 0) return;
            input.value = '';
            await refreshKeys();
        });

        // 两段式确认清空，避免依赖 WebView 里可能被禁用的 window.confirm
        var clearArmed = false;
        var clearTimer = null;
        document.getElementById('clear-keys').addEventListener('click', async function () {
            var btn = document.getElementById('clear-keys');
            if (!clearArmed) {
                clearArmed = true;
                btn.textContent = '再点一次确认清空';
                clearTimer = setTimeout(function () {
                    clearArmed = false;
                    btn.textContent = '清空全部授权';
                }, 3000);
                return;
            }
            clearTimeout(clearTimer);
            clearArmed = false;
            btn.textContent = '清空全部授权';
            var out = await runConfig('keys-clear');
            toast(out || '已清空');
            await refreshKeys();
        });

        document.getElementById('copy-conn').addEventListener('click', async function () {
            var cmd = document.getElementById('conn-cmd').textContent;
            var ok = await copyText(cmd);
            toast(ok ? '已复制：' + cmd : cmd);
        });

        setInterval(refreshStatus, 5000);
    });
})();
