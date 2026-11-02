/* 公共脚本：请求封装、顶栏、卡片渲染、分页、提示 */
const API_BASE = 'api/';

function buildQuery(params) {
    if (!params) return '';
    const parts = Object.entries(params)
        .filter(([, v]) => v !== undefined && v !== null && v !== '')
        .map(([k, v]) => encodeURIComponent(k) + '=' + encodeURIComponent(v));
    return parts.length ? '?' + parts.join('&') : '';
}

async function request(path, options = {}) {
    const { method = 'GET', body, params, silent = false } = options;
    let res;
    try {
        res = await fetch(API_BASE + path + buildQuery(params), {
            method,
            headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
            body: body !== undefined ? JSON.stringify(body) : undefined
        });
    } catch (e) {
        if (!silent) toast('网络异常，请检查服务是否启动', 'error');
        throw e;
    }
    let payload = null;
    try { payload = await res.json(); } catch (e) { /* 可能是非 JSON 响应 */ }

    if (res.status === 401) {
        if (!silent) {
            toast('请先登录', 'error');
            setTimeout(() => {
                location.href = 'login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
            }, 600);
        }
        throw new Error('未登录');
    }
    if (!res.ok || !payload || payload.code !== 200) {
        const message = (payload && payload.message) || ('请求失败（' + res.status + '）');
        if (!silent) toast(message, 'error');
        throw new Error(message);
    }
    return payload.data;
}

const api = {
    get: (path, params, opts = {}) => request(path, Object.assign({}, opts, { params })),
    post: (path, body, opts = {}) => request(path, Object.assign({}, opts, { method: 'POST', body })),
    put: (path, body, opts = {}) => request(path, Object.assign({}, opts, { method: 'PUT', body })),
    del: (path, opts = {}) => request(path, Object.assign({}, opts, { method: 'DELETE' }))
};

function esc(value) {
    return String(value === undefined || value === null ? '' : value).replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

function money(value) {
    return Number(value || 0).toFixed(2);
}

function qs(name) {
    return new URLSearchParams(location.search).get(name);
}

function toast(message, type = 'info') {
    const el = document.createElement('div');
    el.className = 'toast ' + type;
    el.textContent = message;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 2200);
}

function stars(rating) {
    const n = Math.max(0, Math.min(5, Math.round(Number(rating) || 0)));
    return '★'.repeat(n) + '☆'.repeat(5 - n);
}

/* ---------- 顶栏 ---------- */
async function renderHeader() {
    const holder = document.getElementById('app-header');
    if (!holder) return null;
    let me = null;
    try {
        me = await api.get('auth/me', null, { silent: true });
    } catch (e) {
        me = null;
    }
    const nav = ['<a href="index.html">教材市场</a>', '<a href="wanted.html">教材求购</a>'];
    if (me) {
        nav.push('<a href="publish.html">发布教材</a>',
            '<a href="my-books.html">我的发布</a>',
            '<a href="my-orders.html">我的订单</a>',
            '<a href="favorites.html">我的收藏</a>');
    }
    if (me && me.role === 1) {
        nav.push('<a href="admin.html">后台管理</a>');
    }
    const right = me
        ? '<span class="user-chip" onclick="location.href=\'profile.html\'">' + esc(me.nickname)
            + '<i>信用 ' + esc(me.creditScore) + '</i></span>'
            + '<button class="btn btn-ghost btn-sm" id="logout-btn">退出</button>'
        : '<a class="btn btn-ghost btn-sm" href="login.html">登录</a>'
            + '<a class="btn btn-sm" href="register.html">注册</a>';

    holder.className = 'topbar';
    holder.innerHTML = '<div class="topbar-inner">'
        + '<a class="brand" href="index.html">校园二手教材</a>'
        + '<nav class="nav">' + nav.join('') + '</nav>'
        + '<div class="nav-right">' + right + '</div></div>';

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.onclick = async () => {
            try { await api.post('auth/logout'); } catch (e) { /* 忽略 */ }
            location.href = 'index.html';
        };
    }
    return me;
}

/* ---------- 卡片与分页 ---------- */
function coverHtml(url, title) {
    return url
        ? '<img src="' + esc(url) + '" alt="' + esc(title) + '" loading="lazy">'
        : '<span class="cover-placeholder">暂无封面</span>';
}

function bookCard(book) {
    return '<a class="book-card" href="book-detail.html?id=' + book.id + '">'
        + '<div class="cover">' + coverHtml(book.coverUrl, book.title) + '</div>'
        + '<div class="book-body">'
        + '<div class="book-title">' + esc(book.title) + '</div>'
        + '<div class="book-meta">' + esc(book.conditionLabel || '') + ' · ' + esc(book.categoryName || '未知类目') + '</div>'
        + '<div class="book-foot"><span class="price">¥' + money(book.price) + '</span>'
        + '<span class="muted">' + esc(book.sellerNickname || '') + '</span></div>'
        + '</div></a>';
}

function emptyHtml(text) {
    return '<div class="empty">' + esc(text) + '</div>';
}

function renderPager(holder, pageData, onGo) {
    if (!holder) return;
    const page = pageData.page || 1;
    const pages = pageData.pages || 0;
    if (pages <= 1) {
        holder.innerHTML = '';
        return;
    }
    const buttons = [];
    buttons.push('<button class="page-btn" data-page="' + (page - 1) + '"' + (page <= 1 ? ' disabled' : '') + '>上一页</button>');
    let lastGap = false;
    for (let i = 1; i <= pages; i++) {
        if (i === 1 || i === pages || Math.abs(i - page) <= 2) {
            buttons.push('<button class="page-btn' + (i === page ? ' active' : '') + '" data-page="' + i + '">' + i + '</button>');
            lastGap = false;
        } else if (!lastGap) {
            buttons.push('<span class="page-gap">…</span>');
            lastGap = true;
        }
    }
    buttons.push('<button class="page-btn" data-page="' + (page + 1) + '"' + (page >= pages ? ' disabled' : '') + '>下一页</button>');
    holder.innerHTML = buttons.join('');
    holder.querySelectorAll('button[data-page]').forEach(btn => {
        btn.onclick = () => {
            const target = Number(btn.dataset.page);
            if (target >= 1 && target <= pages) onGo(target);
        };
    });
}

/* ---------- 图片压缩上传（封面/头像共用） ---------- */
async function uploadImage(file) {
    if (!file) return null;
    if (!/^image\//.test(file.type)) throw new Error('请选择图片文件');
    if (file.size > 8 * 1024 * 1024) throw new Error('图片不能超过 8MB');

    const dataUrl = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = () => reject(new Error('读取图片失败'));
        reader.readAsDataURL(file);
    });
    const img = await new Promise((resolve, reject) => {
        const image = new Image();
        image.onload = () => resolve(image);
        image.onerror = () => reject(new Error('图片解析失败'));
        image.src = dataUrl;
    });
    const scale = Math.min(1, 900 / Math.max(img.width, img.height));
    let output = dataUrl;
    if (scale < 1 || dataUrl.length > 500000) {
        const canvas = document.createElement('canvas');
        canvas.width = Math.round(img.width * scale);
        canvas.height = Math.round(img.height * scale);
        canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
        output = canvas.toDataURL('image/jpeg', 0.82);
    }
    const result = await api.post('files/image', { dataUrl: output });
    return result.url;
}

function requireLogin(me) {
    if (me) return false;
    location.href = 'login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
    return true;
}
