<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

// 普通券表单
const vf = ref({ shopId: 1, title: '', subTitle: '', rules: '', payValue: 100, actualValue: 3000 })
const vMsg = ref(null)
// 秒杀券表单
const sf = ref({ shopId: 1, title: '', subTitle: '', rules: '', payValue: 100, actualValue: 500, stock: 10, beginTime: '', endTime: '' })
const sMsg = ref(null)
// 券列表
const listShopId = ref(1)
const vouchers = ref([])
const listMsg = ref(null)
// 秒杀下单
const buyVoucherId = ref('')
const buyMsg = ref(null)

function show(el, text, ok) { el.value = text ? { text, ok } : null; }

// "2026-08-30T23:59" -> "2026-08-30 23:59:00"（后端 JacksonConfig 全局格式）
function toBackendTime(v) {
  if (!v) return null;
  return v.replace('T', ' ') + (v.length === 16 ? ':00' : '');
}
// 秒杀券判定：列表 SQL left join 秒杀表，只有秒杀券 stock 非空（type 后端没置，不能只靠它）
function isSk(v) { return v.type === 1 || v.stock != null; }

async function createVoucher() {
  try {
    const id = await api.post('/voucher', { ...vf.value, type: 0, status: 1 });
    show(vMsg, `创建成功，普通券ID=${id}`, true);
  } catch (e) { show(vMsg, e.message, false); }
}

async function createSeckill() {
  try {
    const id = await api.post('/voucher/seckill', {
      ...sf.value, type: 1, status: 1,
      beginTime: toBackendTime(sf.value.beginTime),
      endTime: toBackendTime(sf.value.endTime),
    });
    show(sMsg, `创建成功，秒杀券ID=${id}`, true);
    buyVoucherId.value = id;
  } catch (e) { show(sMsg, e.message, false); }
}

async function loadVouchers() {
  try {
    vouchers.value = await api.get('/voucher/list/' + listShopId.value) || [];
    show(listMsg, '', true);
  } catch (e) { show(listMsg, e.message, false); }
}

async function buy(vid) {
  const id = vid ?? buyVoucherId.value;
  if (!id) return show(buyMsg, '请先填写秒杀券ID', false);
  try {
    const orderId = await api.post('/seckill/' + id);
    show(buyMsg, `秒杀成功！订单ID=${orderId}（Redis预扣库存 + 一人一单 + Seata 全局事务）`, true);
  } catch (e) { show(buyMsg, e.message, false); }
}

// 秒杀时间默认：生效=现在，失效=明天
function setDefaultSkTimes() {
  const pad = n => String(n).padStart(2, '0');
  const toLocal = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  const now = new Date();
  sf.value.beginTime = toLocal(now);
  const end = new Date(now.getTime() + 24 * 3600 * 1000);
  sf.value.endTime = toLocal(end);
}
onMounted(setDefaultSkTimes);
</script>

<template>
  <!-- 新增普通券 -->
  <div class="card">
    <h3>新增普通券</h3>
    <div class="form-row"><label class="field-label">店铺ID</label><input type="number" v-model="vf.shopId"></div>
    <div class="form-row"><label class="field-label">标题</label><input type="text" v-model="vf.title" placeholder="如：满100减30"></div>
    <div class="form-row"><label class="field-label">副标题</label><input type="text" v-model="vf.subTitle"></div>
    <div class="form-row"><label class="field-label">使用规则</label><input type="text" v-model="vf.rules"></div>
    <div class="form-row"><label class="field-label">支付金额(分)</label><input type="number" v-model="vf.payValue"></div>
    <div class="form-row"><label class="field-label">抵扣金额(分)</label><input type="number" v-model="vf.actualValue"></div>
    <button class="btn" @click="createVoucher">创建普通券</button>
    <div v-if="vMsg" class="msg" :class="vMsg.ok ? 'ok' : 'err'">{{ vMsg.text }}</div>
  </div>

  <!-- 新增秒杀券 -->
  <div class="card">
    <h3>新增秒杀券</h3>
    <div class="form-row"><label class="field-label">店铺ID</label><input type="number" v-model="sf.shopId"></div>
    <div class="form-row"><label class="field-label">标题</label><input type="text" v-model="sf.title" placeholder="如：五一特惠·全场通用"></div>
    <div class="form-row"><label class="field-label">副标题</label><input type="text" v-model="sf.subTitle"></div>
    <div class="form-row"><label class="field-label">使用规则</label><input type="text" v-model="sf.rules"></div>
    <div class="form-row"><label class="field-label">支付金额(分)</label><input type="number" v-model="sf.payValue"></div>
    <div class="form-row"><label class="field-label">抵扣金额(分)</label><input type="number" v-model="sf.actualValue"></div>
    <div class="form-row"><label class="field-label">库存</label><input type="number" v-model="sf.stock"></div>
    <div class="form-row">
      <label class="field-label">生效时间</label><input type="datetime-local" v-model="sf.beginTime">
      <label class="field-label">失效时间</label><input type="datetime-local" v-model="sf.endTime">
    </div>
    <button class="btn" @click="createSeckill">创建秒杀券</button>
    <div v-if="sMsg" class="msg" :class="sMsg.ok ? 'ok' : 'err'">{{ sMsg.text }}</div>
  </div>

  <!-- 店铺券列表 -->
  <div class="card">
    <div class="row-between">
      <h3>店铺优惠券列表</h3>
      <div class="form-row" style="margin:0">
        <input type="number" v-model="listShopId" placeholder="店铺ID" style="min-width:100px">
        <button class="btn dark" @click="loadVouchers">查询</button>
      </div>
    </div>
    <div v-if="listMsg" class="msg" :class="listMsg.ok ? 'ok' : 'err'">{{ listMsg.text }}</div>
    <div v-if="vouchers.length" class="voucher-grid">
      <div v-for="v in vouchers" :key="v.id" class="voucher-card">
        <div class="head">
          <div class="title">{{ isSk(v) ? '⚡秒杀·' : '' }}{{ v.title }}</div>
          <div class="sub">{{ v.subTitle }}</div>
        </div>
        <div class="foot">
          <div class="price">¥{{ ((v.actualValue || 0) / 100).toFixed(2) }}<small>　抵扣券：¥{{ ((v.payValue || 0) / 100).toFixed(2) }}</small></div>
          <span class="muted">ID:{{ v.id }}<template v-if="isSk(v)"> · 库存{{ v.stock }}</template></span>
          <button v-if="isSk(v)" class="btn sm" @click="buy(v.id)">去秒杀</button>
        </div>
      </div>
    </div>
    <div v-else class="empty">该店铺暂无优惠券，先在上面创建一张</div>
  </div>

  <!-- 秒杀下单 -->
  <div class="card">
    <h3>秒杀下单</h3>
    <div class="form-row">
      <label class="field-label">秒杀券ID</label>
      <input type="number" v-model="buyVoucherId" placeholder="voucherId">
      <button class="btn" @click="buy()">立即秒杀</button>
    </div>
    <div class="hint">秒杀下单走 Redis 预扣 + 一人一单 + Seata 全局事务；需要先登录。Sentinel 限流时返回"系统繁忙"。</div>
    <div v-if="buyMsg" class="msg" :class="buyMsg.ok ? 'ok' : 'err'">{{ buyMsg.text }}</div>
  </div>
</template>
