import { reactive } from 'vue'

// ---- 登录态全局 store（Vue 响应式，任何组件 import auth 即用） ----
export const auth = reactive({
  token: localStorage.getItem('dp_token') || '',
  me: null,
  get loggedIn() { return !!this.token; },
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

async function request(path, { method = 'GET', body, isForm = false } = {}) {
  const headers = {};
  if (auth.token) headers['Authorization'] = auth.token;
  if (body && !isForm) headers['Content-Type'] = 'application/json';

  let res;
  try {
    res = await fetch(BASE + path, {
      method,
      headers,
      body: isForm ? body : (body ? JSON.stringify(body) : undefined),
    });
  } catch { throw new Error('网络错误，请确认网关已启动'); }

  // 网关/服务 401：未登录、登录过期、被拉黑
  if (res.status === 401) { auth.clear(); throw new Error('未登录或登录已过期，请重新登录'); }

  let json;
  try { json = await res.json(); } catch { throw new Error('响应不是合法 JSON (HTTP ' + res.status + ')'); }

  if (json && json.success === false) throw new Error(json.errorMsg || '请求失败');
  return json ? json.data : null;
}

export const api = {
  get: (p, o) => request(p, { ...o, method: 'GET' }),
  post: (p, b, o) => request(p, { ...o, method: 'POST', body: b }),
  put: (p, b, o) => request(p, { ...o, method: 'PUT', body: b }),
  del: (p, o) => request(p, { ...o, method: 'DELETE' }),
};
