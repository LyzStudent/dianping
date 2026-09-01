<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, auth } from '../api'

const route = useRoute()
const router = useRouter()

const shopTypes = ref([])
const activeTypeId = ref(null)
const list = ref([])
const keyword = ref('')
const searching = ref(false)
const msg = ref(null)

// 店铺详情
const selectedShop = ref(null)
const vouchers = ref([])
const reviews = ref([])
const myReview = ref(null)
const reviewRating = ref(5)
const reviewContent = ref('')
const detailMsg = ref(null)

// 定位（演示默认陆家嘴，点击可尝试浏览器定位）
const loc = reactive({ name: '陆家嘴', x: 121.505, y: 31.245, located: false })

// 收藏状态
const fav = reactive({ fav: false, count: 0 })

const feedTitle = computed(() => {
  if (searching.value) return `搜索「${keyword.value}」`
  const t = shopTypes.value.find(t => t.id === activeTypeId.value)
  return t ? t.name + ' · 推荐' : '猜你喜欢'
})

const EMOJI_MAP = {
  美食: '🍜', 电影: '🎬', 酒店: '🏨', 丽人: '💄', '休闲娱乐': '🎮', '生活服务': '🛠️',
  KTV: '🎤', '周边游': '🏞️', 健身: '🏋️', 咖啡: '☕', 甜品: '🍰', 儿童: '🧸',
  火锅: '🍲', 烧烤: '🍢', 自助: '🍱', 快餐: '🍔', 日料: '🍣',
}
const EMOJI_FALLBACK = ['🍜', '🎬', '🏨', '💄', '🎮', '🛠️', '🎤', '🏞️', '🏋️', '☕']
function emoji(name, i) { return EMOJI_MAP[name] || EMOJI_FALLBACK[i % EMOJI_FALLBACK.length] }

function firstImg(s) { return ((s.images || '').split(',').filter(Boolean))[0] || '' }
function imgs(s) { return (s.images || '').split(',').filter(Boolean) }
function score(s) { return ((s.score || 0) / 10).toFixed(1) }
function price(s) { return ((s.avgPrice || 0) / 100).toFixed(0) }
function dist(s) { return s.distance != null ? (s.distance / 1000).toFixed(1) : null }
function isSk(v) { return v.type === 1 || v.stock != null }
function show(el, text, ok) { el.value = text ? { text, ok } : null }

async function loadTypes() {
  try { shopTypes.value = await api.get('/shop-type/list') || [] } catch { shopTypes.value = [] }
  if (shopTypes.value.length) await pickType(shopTypes.value[0].id)
}

async function pickType(typeId) {
  activeTypeId.value = typeId
  searching.value = false
  keyword.value = ''
  await loadList()
}

async function loadList() {
  let qs = `typeId=${activeTypeId.value}&current=1`
  if (loc.located) qs += `&x=${loc.x}&y=${loc.y}`
  try {
    list.value = await api.get('/shop/of/type?' + qs) || []
    // GEO 未灌数据时兜底：带坐标为空则去掉坐标重查，保证首页有内容
    if (loc.located && !list.value.length) {
      list.value = await api.get(`/shop/of/type?typeId=${activeTypeId.value}&current=1`) || []
    }
    show(msg, null, true)
  } catch (e) { show(msg, e.message, false) }
}

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) { searching.value = false; return loadList() }
  searching.value = true
  try {
    list.value = await api.get('/shop/search?keyword=' + encodeURIComponent(kw) + '&page=1&size=20') || []
    show(msg, null, true)
  } catch (e) { show(msg, e.message, false) }
}

function clearSearch() { keyword.value = ''; searching.value = false; loadList() }

function locate() {
  const useGeo = (x, y) => { loc.x = x; loc.y = y; loc.located = true; loc.name = '当前位置'; loadList() }
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      p => useGeo(p.coords.longitude, p.coords.latitude),
      () => useGeo(loc.x, loc.y),
      { timeout: 4000 }
    )
  } else {
    loc.located = true
    loadList()
  }
}

// ---------- 店铺详情 ----------
async function openShop(id) {
  try { selectedShop.value = await api.get('/shop/' + id) }
  catch (e) { return show(detailMsg, e.message, false) }
  loadVouchers()
  loadReviews()
  loadFav()
}
function closeShop() { selectedShop.value = null }

async function loadVouchers() {
  if (!selectedShop.value) return
  try { vouchers.value = await api.get('/voucher/list/' + selectedShop.value.id) || [] }
  catch { vouchers.value = [] }
}

async function loadReviews() {
  reviews.value = []
  if (!selectedShop.value) return
  if (!auth.loggedIn) return
  try { myReview.value = await api.get('/shop-review/my?shopId=' + selectedShop.value.id) }
  catch { myReview.value = null }
  if (myReview.value) {
    reviewRating.value = myReview.value.rating
    reviewContent.value = myReview.value.content || ''
  }
  try { reviews.value = await api.get('/shop-review/of/shop?shopId=' + selectedShop.value.id + '&current=1') || [] }
  catch (e) { show(detailMsg, e.message, false) }
}

// ---------- 收藏 ----------
async function loadFav() {
  fav.fav = false; fav.count = 0
  try { fav.count = await api.get('/shop/favorite/count/' + selectedShop.value.id) || 0 } catch { fav.count = 0 }
  if (!auth.loggedIn) return
  try { fav.fav = await api.get('/shop/favorite/status/' + selectedShop.value.id) } catch { fav.fav = false }
}

async function toggleFav() {
  if (!auth.loggedIn) return router.push({ path: '/login', query: { redirect: '/shops' } })
  try {
    const now = await api.put('/shop/favorite/' + selectedShop.value.id)
    fav.fav = (typeof now === 'boolean') ? now : !fav.fav
    fav.count = Math.max(0, fav.count + (fav.fav ? 1 : -1))
  } catch (e) { show(detailMsg, e.message, false) }
}

async function saveReview() {
  if (!auth.loggedIn) return router.push({ path: '/login', query: { redirect: '/shops' } })
  const body = { shopId: selectedShop.value.id, rating: reviewRating.value, content: reviewContent.value.trim(), images: '' }
  try { await api.post('/shop-review', body); show(detailMsg, '评价成功', true); loadReviews() }
  catch (e) { show(detailMsg, e.message, false) }
}

async function buyVoucher(v) {
  if (!auth.loggedIn) return router.push({ path: '/login', query: { redirect: '/shops' } })
  try {
    const orderId = await api.post('/seckill/' + v.id)
    show(detailMsg, `秒杀成功！订单ID=${orderId}`, true)
  } catch (e) { show(detailMsg, e.message, false) }
}

onMounted(() => {
  loadTypes()
  if (route.query.shopId) openShop(route.query.shopId)
})
</script>

<template>
  <!-- ===== 首页：搜索 / 金刚区 / 信息流 ===== -->
  <div v-if="!selectedShop">
    <div class="home-top">
      <div class="home-search-row">
        <div class="loc" title="点击定位附近" @click="locate">
          <span>📍</span><span class="loc-name">{{ loc.name }}</span>
        </div>
        <div class="search-box">
          <span>🔍</span>
          <input v-model="keyword" placeholder="搜索美食 / 店铺" @keyup.enter="doSearch">
          <span v-if="searching || keyword" class="search-clear" @click="clearSearch">✕</span>
        </div>
      </div>
    </div>

    <div class="kang">
      <div class="kang-item" v-for="(t, i) in shopTypes" :key="t.id" @click="pickType(t.id)">
        <div class="kang-ico" :class="{ on: activeTypeId === t.id }">{{ emoji(t.name, i) }}</div>
        <div class="kang-name">{{ t.name }}</div>
      </div>
    </div>

    <div class="sk-banner" @click="router.push('/seckill')">
      <span>⚡ 限时秒杀 · 低至5折</span><span class="arrow">立即抢购 ›</span>
    </div>

    <div class="feed">
      <div class="feed-title">{{ feedTitle }}</div>
      <div v-for="s in list" :key="s.id" class="shop-row" @click="openShop(s.id)">
        <img :src="firstImg(s)" alt="" @error="e => e.target.style.display='none'">
        <div class="info">
          <div class="name">{{ s.name }}</div>
          <div class="line">
            <span class="star">★ {{ score(s) }}</span>
            <span class="meta-sm">人均 ¥{{ price(s) }}</span>
            <span class="meta-sm">销量 {{ s.sold || 0 }}</span>
          </div>
          <div class="line">
            <span class="meta-sm">{{ s.area }}</span>
            <span v-if="dist(s)" class="dist">距你 {{ dist(s) }}km</span>
          </div>
        </div>
      </div>
      <div v-if="!list.length && !msg" class="empty">暂无结果，换个关键词试试</div>
    </div>

    <div v-if="msg" class="msg" :class="msg.ok ? 'ok' : 'err'">{{ msg.text }}</div>
  </div>

  <!-- ===== 店铺详情 ===== -->
  <div v-else>
    <div class="detail-top">
      <span class="back" @click="closeShop">‹ 返回</span>
      <span class="d-title">店铺详情</span>
      <span></span>
    </div>

    <div class="d-gallery">
      <img v-for="u in imgs(selectedShop.images)" :key="u" :src="u" alt="" @error="e => e.target.style.display='none'">
    </div>

    <div class="d-card">
      <div class="d-name">{{ selectedShop.name }}</div>
      <div class="d-meta">
        <span class="star">★ {{ score(selectedShop) }}</span>
        <span>　人均 ¥{{ price(selectedShop) }}</span>
        <span>　销量 {{ selectedShop.sold || 0 }}</span>
        <span>　评论 {{ selectedShop.comments || 0 }}</span>
      </div>
      <div class="d-fav" :class="{ on: fav.fav }" @click="toggleFav">
        <span class="heart">{{ fav.fav ? '♥' : '♡' }}</span>
        <span>{{ fav.fav ? '已收藏' : '收藏' }}</span>
        <span v-if="fav.count" class="cnt">{{ fav.count }}</span>
      </div>
      <div class="d-addr">📍 {{ selectedShop.area }} · {{ selectedShop.address }}</div>
      <div class="d-addr">🕐 {{ selectedShop.openTime || '未填写营业时间' }}</div>
      <div v-if="detailMsg" class="msg" :class="detailMsg.ok ? 'ok' : 'err'">{{ detailMsg.text }}</div>
    </div>

    <div class="d-card">
      <div class="d-sec">优惠券</div>
      <template v-if="vouchers.length">
        <div v-for="v in vouchers" :key="v.id" class="v-row">
          <div class="v-left">
            <div class="v-title">{{ isSk(v) ? '⚡ ' : '' }}{{ v.title }}</div>
            <div class="v-sub">{{ v.subTitle }}</div>
          </div>
          <div class="v-price">¥{{ ((v.actualValue || 0) / 100).toFixed(2) }}<small>¥{{ ((v.payValue || 0) / 100).toFixed(2) }}</small></div>
          <button v-if="isSk(v)" class="btn sm" @click="buyVoucher(v)">抢购</button>
          <button v-else class="btn sm ghost" disabled>到店用</button>
        </div>
      </template>
      <div v-else class="empty">该店铺暂无优惠券</div>
    </div>

    <div class="d-card">
      <div class="d-sec">用户评价</div>
      <template v-if="auth.loggedIn">
        <div class="form-row" style="margin-bottom:10px">
          <label class="field-label">评分</label>
          <select v-model="reviewRating" style="min-width:auto">
            <option v-for="r in [5,4,3,2,1]" :key="r" :value="r">{{ '★'.repeat(r) }}{{ '☆'.repeat(5 - r) }}</option>
          </select>
          <input type="text" v-model="reviewContent" placeholder="说点什么…" style="flex:1">
          <button class="btn sm" @click="saveReview">{{ myReview ? '更新' : '发表' }}评价</button>
        </div>
        <div v-if="reviews.length">
          <div v-for="r in reviews" :key="r.id" class="comment">
            <div class="nick">{{ r.nickName || '用户' + r.userId }}<span class="muted">　★{{ r.rating }} · {{ r.createTime }}</span></div>
            <div class="text">{{ r.content }}</div>
          </div>
        </div>
        <div v-else class="empty">暂无评价，来抢首评</div>
      </template>
      <div v-else class="muted" style="padding:6px 0">
        登录后可发表 / 查看评价
        <a class="follow-link" @click="router.push({ path: '/login', query: { redirect: '/shops' } })">去登录 ›</a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.d-fav {
  display: inline-flex; align-items: center; gap: 6px; margin-top: 10px;
  padding: 6px 16px; border-radius: 20px; background: #fff5f1;
  color: #ff6034; font-size: 14px; cursor: pointer; user-select: none;
}
.d-fav.on { background: #ff6034; color: #fff; }
.d-fav .heart { font-size: 16px; }
.d-fav .cnt { font-size: 12px; opacity: .85; }
</style>
