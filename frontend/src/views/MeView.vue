<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api, auth } from '../api'

const email = ref('')
const code = ref('')
const sending = ref(false)
const msg = ref(null)
const signCount = ref('-')
const loadingSign = ref(false)

// 月历状态
const cal = reactive({ y: 0, m: 0, title: '', cells: [] })
const signDays = ref([])

const roleText = computed(() => {
  switch (auth.role) {
    case 2: return '商家'
    case 3: return '管理员'
    default: return '普通用户'
  }
})

// 会员等级：优先用后端 /user/me 返回的 level，否则按积分兜底计算
const levelInfo = computed(() => {
  const me = auth.me || {}
  const points = Number(me.points) || 0
  let level = me.level
  if (!level) {
    if (points >= 20000) level = '黑金会员'
    else if (points >= 5000) level = '金卡会员'
    else if (points >= 1000) level = '银卡会员'
    else level = '普卡会员'
  }
  return { level, points }
})

function show(text, ok) { msg.value = { text, ok }; }
function pad(n) { return String(n).padStart(2, '0') }
function dateStr(y, m, d) { return `${y}-${pad(m)}-${pad(d)}` }

async function refreshProfile() {
  await auth.refreshMe();
  if (!auth.loggedIn) return;
  try { signCount.value = (await api.get('/user/sign/count')) ?? '-' } catch { signCount.value = '-' }
}

// 加载某月签到记录（后端未实现时降级：无圆点）
async function loadSignDate(y, m) {
  try { signDays.value = await api.get(`/user/sign/date?date=${dateStr(y, m, 1)}`) || [] }
  catch { signDays.value = [] }
  buildCalendar(y, m)
}

function buildCalendar(y, m) {
  const today = new Date()
  const todayStr = today.getFullYear() === y && today.getMonth() + 1 === m
    ? dateStr(y, m, today.getDate()) : ''
  const daysInMonth = new Date(y, m, 0).getDate()      // 当月天数
  const offset = new Date(y, m - 1, 1).getDay()        // 1号是周几（0=周日）
  const set = new Set(signDays.value)
  const cells = []
  for (let i = 0; i < offset; i++) cells.push({ key: 'b' + i, blank: true })
  for (let d = 1; d <= daysInMonth; d++) {
    const ds = dateStr(y, m, d)
    cells.push({ key: ds, day: d, signed: set.has(ds), today: ds === todayStr })
  }
  cal.y = y; cal.m = m; cal.cells = cells
  cal.title = `${y} 年 ${m} 月`
}

function prevMonth() { const d = new Date(cal.y, cal.m - 2, 1); loadSignDate(d.getFullYear(), d.getMonth() + 1) }
function nextMonth() { const d = new Date(cal.y, cal.m, 1); loadSignDate(d.getFullYear(), d.getMonth() + 1) }

async function doSign() {
  if (!auth.loggedIn) return show('请先登录', false)
  loadingSign.value = true
  try {
    await api.post('/user/sign')
    show('签到成功', true)
    refreshProfile()
    loadSignDate(cal.y, cal.m)
  } catch (e) { show(e.message, false) }
  finally { loadingSign.value = false }
}

// ---- 未登录：登录表单 ----
async function sendCode() {
  if (!email.value) return show('请输入邮箱', false);
  sending.value = true;
  try {
    await api.post('/user/code?phone=' + encodeURIComponent(email.value));
    show('验证码已发送', true);
  } catch (e) { show(e.message, false); }
  finally { sending.value = false; }
}

async function login() {
  if (!email.value || !code.value) return show('请输入邮箱和验证码', false);
  try {
    const jwt = await api.post('/user/login', { phone: email.value, code: code.value });
    auth.setToken(jwt);
    await auth.refreshMe();
    show('登录成功', true);
  } catch (e) { show(e.message, false); }
}

onMounted(() => {
  const now = new Date()
  refreshProfile()
  loadSignDate(now.getFullYear(), now.getMonth() + 1)
})
</script>

<template>
  <!-- 未登录：登录表单 -->
  <div v-if="!auth.loggedIn" class="card">
    <h3>登录</h3>
    <div class="form-row">
      <label class="field-label">邮箱</label>
      <input type="email" v-model="email" placeholder="注册时用的邮箱">
    </div>
    <div class="form-row">
      <label class="field-label">验证码</label>
      <input type="text" v-model="code" style="min-width:120px">
      <button class="btn dark" :disabled="sending" @click="sendCode">{{ sending ? '发送中…' : '获取验证码' }}</button>
    </div>
    <div class="form-row"><button class="btn" @click="login">登录</button></div>
    <div v-if="msg" class="msg" :class="msg.ok ? 'ok' : 'err'">{{ msg.text }}</div>
    <div class="hint">未注册的邮箱会先自动注册再发码。</div>
  </div>

  <!-- 已登录：用户卡 + 签到 + 月历 + 菜单 -->
  <div v-else>
    <div class="me-card">
      <img class="me-avatar" :src="auth.me.icon || ''" @error="e => e.target.style.display='none'">
      <div class="me-info">
        <div class="me-name">{{ auth.me.nickName }}</div>
        <div class="me-id">ID {{ auth.me.id }} · {{ roleText }}</div>
      </div>
      <div class="me-level">
        <span class="level-badge">🐋 {{ levelInfo.level }}</span>
        <span class="level-points">积分 {{ levelInfo.points }}</span>
      </div>
    </div>

    <div class="sign-banner">
      <div>
        <div class="sign-num">{{ signCount }}</div>
        <div class="sign-label">连续签到天数</div>
      </div>
      <button class="sign-btn" :disabled="loadingSign" @click="doSign">{{ loadingSign ? '…' : '立即签到' }}</button>
    </div>

    <div class="cal-card">
      <div class="cal-head">
        <span class="cal-nav" @click="prevMonth">‹</span>
        <span class="cal-title">{{ cal.title }}</span>
        <span class="cal-nav" @click="nextMonth">›</span>
      </div>
      <div class="cal-week">
        <span v-for="w in ['日', '一', '二', '三', '四', '五', '六']" :key="w">{{ w }}</span>
      </div>
      <div class="cal-grid">
        <div v-for="c in cal.cells" :key="c.key" class="cal-cell"
             :class="{ blank: c.blank, today: c.today, signed: c.signed }">
          <span class="cal-day">{{ c.day || '' }}</span>
          <span v-if="c.signed" class="cal-dot">✔</span>
        </div>
      </div>
      <div class="cal-tip">点击"立即签到"打卡，当天自动标 ✔</div>
    </div>

    <div class="me-menu">
      <router-link class="menu-row" to="/my-orders?tab=orders">
        <span>📋 我的订单</span><span class="muted">›</span>
      </router-link>
      <router-link class="menu-row" to="/my-orders?tab=vouchers">
        <span>🎫 我的券包</span><span class="muted">›</span>
      </router-link>
      <router-link class="menu-row" to="/my-favorites">
        <span>❤️ 我的收藏</span><span class="muted">›</span>
      </router-link>
    </div>

    <div v-if="msg" class="msg" :class="msg.ok ? 'ok' : 'err'">{{ msg.text }}</div>
  </div>
</template>

<style scoped>
.me-card {
  display: flex; align-items: center; gap: 12px; margin: 12px 12px 0;
  padding: 16px; border-radius: 14px; color: #fff;
  background: linear-gradient(135deg, #ff6034, #ff8a5c);
  box-shadow: 0 4px 12px rgba(255, 96, 52, .3);
}
.me-avatar {
  width: 52px; height: 52px; border-radius: 50%; object-fit: cover;
  border: 2px solid rgba(255, 255, 255, .6); background: #fff;
}
.me-name { font-size: 17px; font-weight: 700; }
.me-id { font-size: 12px; opacity: .85; margin-top: 2px; }
.me-level {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  min-width: 0;
}
.level-badge {
  font-size: 12px;
  font-weight: 700;
  background: rgba(255, 255, 255, .22);
  border: 1px solid rgba(255, 255, 255, .5);
  padding: 3px 12px;
  border-radius: 12px;
  white-space: nowrap;
}
.level-points { font-size: 12px; opacity: .9; }

.sign-banner {
  display: flex; align-items: center; justify-content: space-between;
  margin: 12px; padding: 14px 16px; border-radius: 14px;
  background: #fff; box-shadow: 0 1px 4px rgba(0, 0, 0, .06);
}
.sign-num { font-size: 26px; font-weight: 800; color: #ff6034; line-height: 1; }
.sign-label { font-size: 12px; color: #888; margin-top: 4px; }
.sign-btn {
  padding: 8px 22px; border: none; border-radius: 20px; font-size: 14px;
  background: linear-gradient(135deg, #ff6034, #ff7f50); color: #fff;
  cursor: pointer; box-shadow: 0 3px 8px rgba(255, 96, 52, .35);
}
.sign-btn:disabled { opacity: .6; }

.cal-card {
  margin: 0 12px 12px; padding: 14px 12px; border-radius: 14px;
  background: #fff; box-shadow: 0 1px 4px rgba(0, 0, 0, .06);
}
.cal-head { display: flex; align-items: center; justify-content: space-between; font-size: 15px; font-weight: 600; margin-bottom: 10px; }
.cal-nav { width: 30px; height: 30px; line-height: 28px; text-align: center; border-radius: 50%; cursor: pointer; font-size: 18px; }
.cal-nav:active { background: #f0f0f0; }
.cal-week { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; font-size: 12px; color: #999; margin-bottom: 6px; }
.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.cal-cell {
  position: relative; height: 40px; display: flex; align-items: center; justify-content: center;
  border-radius: 8px; font-size: 14px; color: #333;
}
.cal-cell.blank { visibility: hidden; }
.cal-cell.today { box-shadow: inset 0 0 0 1.5px #ff6034; font-weight: 700; }
.cal-cell.signed { background: #fff5f1; color: #ff6034; font-weight: 600; }
.cal-cell .cal-dot { position: absolute; bottom: 2px; font-size: 10px; }
.cal-tip { font-size: 11px; color: #bbb; margin-top: 8px; text-align: center; }

.me-menu { margin: 0 12px 12px; padding: 0 14px; border-radius: 14px; background: #fff; box-shadow: 0 1px 4px rgba(0, 0, 0, .06); }
.menu-row { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; font-size: 15px; color: #333; text-decoration: none; }
.menu-row + .menu-row { border-top: 1px solid #f0f0f0; }
</style>
