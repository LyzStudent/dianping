<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()

// 券列表
const listShopId = ref(1)
const vouchers = ref([])
const listMsg = ref(null)
// 秒杀下单
const buyVoucherId = ref('')
const buyMsg = ref(null)

function show(el, text, ok) { el.value = text ? { text, ok } : null; }

// 秒杀券判定：列表 SQL left join 秒杀表，只有秒杀券 stock 非空（type 后端没置，不能只靠它）
function isSk(v) { return v.type === 1 || v.stock != null; }

async function loadVouchers() {
  try {
    vouchers.value = await api.get('/voucher/list/' + listShopId.value) || [];
    show(listMsg, '', true);
  } catch (e) { show(listMsg, e.message, false); }
}

async function buy(vid) {
  const id = vid ?? buyVoucherId.value;
  if (!id) return show(buyMsg, '请先输入秒杀券ID', false);
  try {
    const orderId = await api.post('/seckill/' + id);
    show(buyMsg, `秒杀成功！订单号 ${orderId}`, true);
  } catch (e) { show(buyMsg, e.message, false); }
}

onMounted(loadVouchers);

/** 返回上一页（无历史记录时回首页） */
function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/shops')
}
</script>

<template>
  <div class="back-bar"><span class="page-back" @click="goBack">‹ 返回</span></div>

  <!-- 店铺券列表 -->
  <div class="card">
    <div class="row-between">
      <h3>秒杀活动</h3>
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
    <div v-else class="empty">该店铺暂无优惠券（发券由商家端完成）</div>
  </div>

  <!-- 秒杀下单 -->
  <div class="card">
    <h3>秒杀下单</h3>
    <div class="form-row">
      <label class="field-label">秒杀券ID</label>
      <input type="number" v-model="buyVoucherId" placeholder="voucherId">
      <button class="btn" @click="buy()">立即秒杀</button>
    </div>
    <div class="hint">参与秒杀请先登录，库存有限、先到先得。</div>
    <div v-if="buyMsg" class="msg" :class="buyMsg.ok ? 'ok' : 'err'">{{ buyMsg.text }}</div>
  </div>
</template>
