/* 营销返利 - 公共 JS */
window.RB = window.RB || {};

// Token存储键名
RB.TOKEN_KEY = 'REBATE_TOKEN';

// 保存token
RB.saveToken = function(token) {
    if (token) {
        localStorage.setItem(RB.TOKEN_KEY, token);
    }
};

// 获取token
RB.getToken = function() {
    return localStorage.getItem(RB.TOKEN_KEY);
};

// 清除token
RB.clearToken = function() {
    localStorage.removeItem(RB.TOKEN_KEY);
};

RB.redirectToLogin = function() {
    RB.clearToken();
    // 在 iframe 中使用 top.location 跳转到外层整个页面
    if (self !== top) {
        top.location.href = 'index.html';
    } else {
        location.href = 'index.html';
    }
};

RB.api = function (url, method, data) {
    method = method || 'POST';
    var apiUrl = url;
    if (url.startsWith('/api/')) {
        apiUrl = '.' + url;
    }
    var opts = {
        url: apiUrl,
        type: method,
        dataType: 'json',
        xhrFields: { withCredentials: true },
        beforeSend: function(xhr) {
            var token = RB.getToken();
            if (token) {
                xhr.setRequestHeader('X-Token', token);
            }
        },
        success: function (r) {
            if (r && r.code === 401) {
                RB.redirectToLogin();
            }
        },
        error: function (xhr, t, e) {
            // HTTP 401 同样视为登录过期，跳转到外层登录页
            if (xhr.status === 401) {
                RB.redirectToLogin();
                return;
            }
            console.error('api error', url, t, e);
            RB.toast('网络异常', 'error');
        }
    };
    if (data !== undefined) {
        if (data instanceof FormData) {
            opts.data = data;
            opts.processData = false;
            opts.contentType = false;
        } else {
            opts.data = JSON.stringify(data);
            opts.contentType = 'application/json;charset=UTF-8';
        }
    }
    return $.ajax(opts);
};

RB.get = function (url, data) { return RB.api(url, 'GET', data); };
RB.post = function (url, data) { return RB.api(url, 'POST', data); };

RB.toast = function (msg, type) {
    type = type || 'info';
    var color = type === 'error' ? '#d32f2f' : type === 'success' ? '#388e3c' : '#1976d2';
    var $t = $('<div></div>').text(msg).css({
        position: 'fixed', top: '20px', left: '50%', transform: 'translateX(-50%)',
        background: color, color: '#fff', padding: '8px 16px', borderRadius: '4px', zIndex: 9999,
        boxShadow: '0 2px 8px rgba(0,0,0,0.2)'
    }).appendTo('body');
    setTimeout(function () { $t.fadeOut(300, function () { $t.remove(); }); }, 3500);
};

RB.confirm = function (msg, ok) {
    if (window.confirm(msg)) ok();
};

RB.formatMoney = function (v) {
    if (v === null || v === undefined || v === '') return '-';
    var n = Number(v);
    if (isNaN(n)) return v;
    return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

RB.formatNumber = function (v) {
    if (v === null || v === undefined || v === '') return '-';
    var n = Number(v);
    if (isNaN(n)) return v;
    return n.toLocaleString('zh-CN');
};

RB.formatPercent = function (v) {
    if (v === null || v === undefined || v === '') return '-';
    return Number(v).toFixed(2) + '%';
};

RB.openModal = function (id) { $('#' + id).addClass('show'); };
RB.closeModal = function (id) { $('#' + id).removeClass('show'); };

RB.currentUser = null;

RB.checkLogin = function (cb) {
    RB.get('/api/auth?op=current').done(function (r) {
        if (r.code === 0) {
            RB.currentUser = r.data;
            cb(r.data);
        } else {
            RB.redirectToLogin();
        }
    }).fail(function() {
        RB.redirectToLogin();
    });
};

/**
 * 检查当前用户是否有指定权限
 * @param {string|string[]} perm 权限码或权限码数组（任一满足即通过）
 * @returns {boolean}
 */
RB.hasPerm = function (perm) {
    if (!RB.currentUser) return false;
    if (RB.currentUser.isAdmin === 1) return true;
    var codes = RB.currentUser.permCodes || [];
    if (Array.isArray(perm)) {
        for (var i = 0; i < perm.length; i++) {
            if (codes.indexOf(perm[i]) !== -1) return true;
        }
        return false;
    }
    return codes.indexOf(perm) !== -1;
};

/**
 * 权限检查并执行操作
 * @param {string|string[]} perm 权限码或权限码数组
 * @param {Function} action 权限满足时执行的回调
 * @param {string} [msg] 自定义提示信息
 */
RB.withPerm = function (perm, action, msg) {
    if (RB.hasPerm(perm)) {
        action();
    } else {
        RB.toast(msg || '权限不足，无法执行该操作', 'error');
    }
};

RB.showLoading = function (text) {
    text = text || '加载中...';
    if ($('#rb_loading').length === 0) {
        $('<div id="rb_loading"></div>').css({
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(255,255,255,0.8)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999
        }).append('<div style="font-size:16px; color:#333; padding:20px 40px; background:#fff; border-radius:8px; box-shadow:0 2px 12px rgba(0,0,0,0.15);">' + text + '</div>').appendTo('body');
    } else {
        $('#rb_loading div').text(text);
        $('#rb_loading').show();
    }
};
RB.hideLoading = function () {
    $('#rb_loading').remove();
};
RB.getQueryParam = function (key) {
    var params = location.search.substring(1).split('&');
    for (var i = 0; i < params.length; i++) {
        var kv = params[i].split('=');
        if (kv[0] === key) return decodeURIComponent(kv[1] || '');
    }
    return null;
};
// 自动获取上下文路径
(function() {
    var path = window.location.pathname;
    var idx = path.indexOf('/', 1);
    if (idx > 0) {
        RB.ctx = path.substring(0, idx);
    } else {
        RB.ctx = '';
    }
})();
