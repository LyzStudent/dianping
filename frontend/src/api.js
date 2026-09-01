import { reactive } from 'vue'

// ---- 登录态全局 store（Vue 响应式，任何组件 import auth 即用） ----
export const auth = reactive({
  token: localStorage.getItem('dp_token') || '',
  me: null,
  get loggedIn() { return !!this.token; },
  // 当前角色：1普通用户 2商家 3管理员（未加载完先给1）
  get role() { return this.me && this.me.role ? this.me.role : 1; },
  setToken(t) { this.token = t; if (t) localStorage.setItem('dp_token', t); else localStorage.removeItem('dp_token'); },
  clear() { this.token = ''; this.me = null; localStorage.removeItem('dp_token'); },
  async refreshMe() {
    if (!this.loggedIn) { this.me = null; return; }
    try { this.me = await api.get('/user/me'); }
    catch { this.clear(); }
  }
})

// 请求前缀：开发走 Vite 代理（同源），部署可改用 VITE_API_BASE 指向网关
const BASE = import.meta.env.VITE_API_BASE || ''

// 403（无权访问）时由 main.js 注入跳转回调，避免 api 与 router 循环依赖
export function setForbiddenHandler(fn) { onForbidden = fn; }
let onForbidden = null;

/** 按角色返回默认首页 */
export function homeByRole(role) {
  if (role === 3) return '/admin/users'
  if (role === 2) return '/merchant/shop'
  return '/shops'
}

async function request(path, { method = 'GET', body, isForm = false } = {}) {
  const headers = {};
  if (auth.token) headers['Authorization'] = auth.token;
  if (body && !isForm) headers['Content-Type'] = 'application/json';

  let res;
  try {
    res = await fetch(BASE + path, {
      method,
      headers,
      // isForm 时直接传 URLSearchParams / FormData，fetch 会自动带 content-type
      body: isForm ? body : (body ? JSON.stringify(body) : undefined),
    });
  } catch { throw new Error('网络错误，请确认网关已启动'); }

  // 网关/服务 401：未登录、登录过期、被拉黑
  if (res.status === 401) { auth.clear(); throw new Error('未登录或登录已过期，请重新登录'); }
  // 403：角色不匹配（后端 @RequireRole 拦截）
  if (res.status === 403) {
    if (onForbidden) onForbidden();
    throw new Error('无权访问该功能');
  }

  let json;
  try { json = await res.json(); } catch { throw new Error('响应不是合法 JSON (HTTP ' + res.status + ')'); }

  if (json && json.success === false) throw new Error(json.errorMsg || '请求失败');
  return json ? json.data : null;
}

/** 把普通对象转成表单参数，配合 postForm 使用 */
export function toForm(obj) {
  const p = new URLSearchParams();
  Object.entries(obj || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.append(k, v);
  });
  return p;
}

export const api = {
  get: (p, o) => request(p, { ...o, method: 'GET' }),
  post: (p, b, o) => request(p, { ...o, method: 'POST', body: b }),
  put: (p, b, o) => request(p, { ...o, method: 'PUT', body: b }),
  del: (p, o) => request(p, { ...o, method: 'DELETE' }),
  // 表单提交（后端 @RequestParam / model 属性绑定，如 /merchant/shop）
  postForm: (p, obj, o) => request(p, { ...o, method: 'POST', body: toForm(obj), isForm: true }),
};
