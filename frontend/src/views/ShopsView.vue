<script setup>
import { onMounted, ref } from 'vue'
import { api, auth } from '../api'

const shopTypes = ref([])
const activeType = ref(null)
const shopList = ref([])
const keyword = ref('')
const shopMsg = ref(null)

const selectedShop = ref(null)   // 店铺详情（null = 列表视图）
const detailMsg = ref(null)

// 评价
const reviews = ref([])
const myReview = ref(null)
const reviewRating = ref(5)
const reviewContent = ref('')

// 券
const vouchers = ref([])

function show(el, text, ok) { el.value = text ? { text, ok } : null; }
function imgList(images) { return (images || '').split(',').filter(Boolean); }
function isSk(v) { return v.type === 1 || v.stock != null; }

async function loadTypes() {
  try { shopTypes.value = await api.get('/shop-type/list') || []; }
  catch { shopTypes.value = []; }
  if (shopTypes.value.length) loadShops(shopTypes.value[0].id);
}

async function loadShops(typeId) {
  activeType.value = typeId;
  keyword.value = '';
  try {
    shopList.value = await api.get(`/shop/of/type?typeId=${typeId}&current=1`) || [];
    show(shopMsg, null, true);
  } catch (e) { show(shopMsg, e.message, false); }
}

async function searchShops() {
  const kw = keyword.value.trim();
  if (!kw) return loadShops(activeType.value || shopTypes.value[0]?.id);
  try {
    shopList.value = await api.get('/shop/search?keyword=' + encodeURIComponent(kw) + '&page=1&size=20') || [];
    show(shopMsg, `「${kw}」共找到 ${shopList.value.length} 条结果（ES 高亮）`, true);
  } catch (e) { show(shopMsg, e.message, false); }
}

async function rebuildIndex() {
  try { await api.get('/shop/search/init'); show(shopMsg, 'ES 索引重建完成', true); }
  catch (e) { show(shopMsg, e.message, false); }
}

async function openShop(id) {
  try { selectedShop.value = await api.get('/shop/' + id); }
  catch (e) { return show(shopMsg, e.message, false); }
  loadVouchers();
  loadReviews();
}

function closeShop() { selectedShop.value = null; }

async function loadVouchers() {
  if (!selectedShop.value) return;
  try { vouchers.value = await api.get('/voucher/list/' + selectedShop.value.id) || []; }
  catch { vouchers.value = []; }
}

async function loadReviews() {
  reviews.value = [];
  if (!selectedShop.value) return;
  if (!auth.loggedIn) return;   // 未登录不查评价
  try { myReview.value = await api.get('/shop-review/my?shopId=' + selectedShop.value.id); }
  catch { myReview.value = null; }
  if (myReview.value) {
    reviewRating.value = myReview.value.rating;
    reviewContent.value = myReview.value.content || '';
  }
  try { reviews.value = await api.get('/shop-review/of/shop?shopId=' + selectedShop.value.id + '&current=1') || []; }
  catch (e) { show(detailMsg, e.message, false); }
}

async function saveReview() {
  const body = {
    shopId: selectedShop.value.id,
    rating: reviewRating.value,
    content: reviewContent.value.trim(),
    images: '',
  };
  try { await api.post('/shop-review', body); show(detailMsg, '评价成功', true); loadReviews(); }
  catch (e) { show(detailMsg, e.message, false); }
}

onMounted(loadTypes);
</script>

<template>
  <!-- ======= 列表视图 ======= -->
  <div v-if="!selectedShop">
    <div class="card">
      <div class="form-row">
        <input type="text" v-model="keyword" placeholder="输入关键词搜索店铺（ES）" style="flex:1" @keyup.enter="searchShops">
        <button class="btn dark" @click="searchShops">搜索</button>
        <button class="btn ghost" @click="loadTypes">重置</button>
        <button class="btn ghost" @click="rebuildIndex">重建ES索引</button>
      </div>
      <div class="form-row" v-if="shopTypes.length">
        <button v-for="t in shopTypes" :key="t.id" class="btn ghost sm"
                :style="{ borderColor: activeType === t.id ? '#ff6034' : '' }"
                @click="loadShops(t.id)">{{ t.name }}</button>
      </div>
    </div>
    <div v-if="shopMsg" class="msg" :class="shopMsg.ok ? 'ok' : 'err'">{{ shopMsg.text }}</div>
    <div v-if="shopList.length" class="grid">
      <div v-for="s in shopList" :key="s.id" class="shop-card" @click="openShop(s.id)">
        <div class="shop-img">
          <img v-if="imgList(s.images)[0]" :src="imgList(s.images)[0]" @error="e => e.target.style.display='none'">
        </div>
        <div class="body">
          <div class="name">{{ s.name }}</div>
          <div class="meta"><span class="score">{{ ((s.score || 0) / 10).toFixed(1) }}分</span> · 人均 ¥{{ ((s.avgPrice || 0) / 100).toFixed(0) }} · 销量{{ s.sold || 0 }}</div>
          <div class="meta">{{ s.area }}<template v-if="s.distance != null"> · {{ (s.distance / 1000).toFixed(1) }}km</template></div>
        </div>
      </div>
    </div>
    <div v-else class="empty">没有找到店铺</div>
  </div>

  <!-- ======= 详情视图 ======= -->
  <div v-else>
    <div class="card">
      <div class="row-between">
        <h3>{{ selectedShop.name }}</h3>
        <button class="btn ghost" @click="closeShop">← 返回列表</button>
      </div>
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <img v-for="u in imgList(selectedShop.images)" :key="u" :src="u" style="width:120px;height:120px;object-fit:cover;border-radius:8px" @error="e => e.target.style.display='none'">
      </div>
      <div style="margin-top:10px;line-height:2;font-size:14px">
        <div>💰 人均 ¥{{ ((selectedShop.avgPrice || 0) / 100).toFixed(0) }}　⭐ {{ ((selectedShop.score || 0) / 10).toFixed(1) }}分　📦 销量 {{ selectedShop.sold || 0 }}</div>
        <div>🏪 {{ selectedShop.area }} · {{ selectedShop.address }}</div>
        <div>🕐 {{ selectedShop.openTime }}　评论 {{ selectedShop.comments || 0 }} 条</div>
      </div>
      <div v-if="detailMsg" class="msg" :class="detailMsg.ok ? 'ok' : 'err'">{{ detailMsg.text }}</div>
    </div>

    <div class="card">
      <h3>店铺优惠券</h3>
      <div v-if="vouchers.length" class="voucher-grid">
        <div v-for="v in vouchers" :key="v.id" class="voucher-card">
          <div class="head">
            <div class="title">{{ isSk(v) ? '⚡秒杀·' : '' }}{{ v.title }}</div>
            <div class="sub">{{ v.subTitle }}</div>
          </div>
          <div class="foot">
            <div class="price">¥{{ ((v.actualValue || 0) / 100).toFixed(2) }}<small>　抵扣券：¥{{ ((v.payValue || 0) / 100).toFixed(2) }}</small></div>
            <span class="muted">ID:{{ v.id }}<template v-if="isSk(v)"> · 库存{{ v.stock }}</template></span>
          </div>
        </div>
      </div>
      <div v-else class="empty">该店铺暂无优惠券</div>
    </div>

    <div class="card">
      <h3>用户评价</h3>
      <template v-if="auth.loggedIn">
        <div class="form-row">
          <label class="field-label">评分</label>
          <select v-model="reviewRating" style="min-width:auto">
            <option v-for="r in [5,4,3,2,1]" :key="r" :value="r">{{ '★'.repeat(r) }}{{ '☆'.repeat(5 - r) }}</option>
          </select>
          <input type="text" v-model="reviewContent" placeholder="说点什么…" style="flex:1">
          <button class="btn sm" @click="saveReview">{{ myReview ? '更新' : '发表' }}评价</button>
        </div>
        <div v-if="reviews.length">
          <div v-for="r in reviews" :key="r.id" class="comment">
            <div class="nick">{{ r.nickName || '用户' + r.userId }}　<span class="muted" style="font-weight:400">★{{ r.rating }} · {{ r.createTime }}</span></div>
            <div class="text">{{ r.content }}</div>
          </div>
        </div>
        <div v-else class="empty">暂无评价</div>
      </template>
      <div v-else class="muted">登录后可发表评价 / 查看评价</div>
    </div>
  </div>
</template>
