<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, auth } from '../api'
import StarRating from '../components/StarRating.vue'
import ShopListItem from '../components/ShopListItem.vue'

const route = useRoute()
const router = useRouter()

const shopTypes = ref([])
const activeTypeId = ref(null)
const list = ref([])
const keyword = ref('')
const searching = ref(false)
const msg = ref(null)

// 排序：smart 智能 / near 最近 / score 好评 / sold 热销
const sortMode = ref('smart')
const SORTS = [
  { v: 'smart', text: '智能' },
  { v: 'near', text: '最近' },
  { v: 'score', text: '好评' },
  { v: 'sold', text: '热销' },
]

// 热门搜索词（优先走后端 /shop/hot/keywords，失败降级内置）
const DEFAULT_HOT_WORDS = ['火锅', '自助餐', '烤肉', '奶茶', '日料', 'KTV', '咖啡']
const hotWords = ref([])

// 轮播 Banner（优先走后端 /banner/list，失败降级内置）
const DEFAULT_BANNERS = [
  { title: '限时秒杀', sub: '低至 5 折 · 手慢无', link: '/seckill', icon: '⚡', grad: 'linear-gradient(90deg,#ff6a3d,#ff9535)' },
  { title: '探店笔记', sub: '看看大家都在吃什么', link: '/blog', icon: '✍️', grad: 'linear-gradient(90deg,#ff8a3d,#ffb35c)' },
  { title: '附近好店', sub: '离你最近的宝藏店铺', link: '/nearby', icon: '🧭', grad: 'linear-gradient(90deg,#ff6034,#ff7b43)' },
]
const banners = ref([])
const bannerIndex = ref(0)
let bannerTimer = null

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
  if (searching.value) return `搜索「${keyword.value}」 · 共 ${list.value.length} 条`
  const t = shopTypes.value.find(t => t.id === activeTypeId.value)
  return t ? t.name + ' · 推荐' : '猜你喜欢'
})

// 金刚区图标与配色
const EMOJI_MAP = {
  美食: '🍜', 电影: '🎬', 酒店: '🏨', 丽人: '💄', '休闲娱乐': '🎮', '生活服务': '🛠️',
  KTV: '🎤', '周边游': '🏞️', 健身: '🏋️', 咖啡: '☕', 甜品: '🍰', 儿童: '🧸',
  火锅: '🍲', 烧烤: '🍢', 自助: '🍱', 快餐: '🍔', 日料: '🍣',
}
const EMOJI_FALLBACK = ['🍜', '🎬', '🏨', '💄', '🎮', '🛠️', '🎤', '🏞️', '🏋️', '☕']
const KANG_COLORS = ['#ff6034', '#ff8a3d', '#ffb53d', '#4ac18e', '#5b8ff9', '#9254de', '#f759ab', '#13c2c2', '#faad14', '#eb2f96']
function emoji(name, i) { return EMOJI_MAP[name] || EMOJI_FALLBACK[i % EMOJI_FALLBACK.length] }
// 文本里的 <br>/<br/> 转成换行；其余 HTML 由 {{ }} 自动转义，不会注入
function fmtContent(text) { return (text || '').replace(/<br\s*\/?>/gi, '\n') }
function kangStyle(i) { return { background: KANG_COLORS[i % KANG_COLORS.length] + '1f', color: KANG_COLORS[i % KANG_COLORS.length] } }

function imgs(s) { return (s.images || '').split(',').filter(Boolean) }
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
function hotSearch(w) { keyword.value = w; doSearch() }

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

// ---------- 热门搜索词 ----------
async function loadHotWords() {
  try {
    const arr = await api.get('/shop/hot/keywords')
    hotWords.value = (arr && arr.length) ? arr : DEFAULT_HOT_WORDS
  } catch { hotWords.value = DEFAULT_HOT_WORDS }
}

// ---------- 轮播 Banner ----------
async function loadBanners() {
  try {
    const arr = await api.get('/banner/list')
    if (arr && arr.length) {
      banners.value = arr.map(b => ({
        title: b.title || '', sub: b.subTitle || '', link: b.link || '',
        image: b.image || '', icon: b.icon || '🏪', grad: 'linear-gradient(90deg,#ff6a3d,#ff9535)',
      }))
    } else {
      banners.value = DEFAULT_BANNERS
    }
  } catch { banners.value = DEFAULT_BANNERS }
}
function startBanner() {
  stopBanner()
  if (banners.value.length < 2) return
  bannerTimer = setInterval(() => {
    bannerIndex.value = (bannerIndex.value + 1) % banners.value.length
  }, 3500)
}
function stopBanner() { if (bannerTimer) { clearInterval(bannerTimer); bannerTimer = null } }
function gotoBanner(b) {
  if (!b.link) return
  if (/^https?:/.test(b.link)) window.open(b.link, '_blank')
  else router.push(b.link)
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

// ---------- 展示辅助 ----------
// 高德地图导航（网页版打点）
function mapUrl(s) {
  return `https://uri.amap.com/marker?position=${s.x},${s.y}&name=${encodeURIComponent(s.name)}`
}
// 券标签：从规则文案启发式识别（纯前端演示）
function voucherTags(v) {
  const t = []
  const r = (v.rules || v.subTitle || '')
  if (/免预约|无需预约/.test(r)) t.push('免预约')
  if (/随时退|随时可退/.test(r)) t.push('随时退')
  if (/过期/.test(r)) t.push('过期自动退')
  if (isSk(v)) t.push('限量秒杀')
  return t
}
// 实付 / 面值：payValue=用户支付，actualValue=可抵
function payYuan(v) { return ((v.payValue || 0) / 100).toFixed(2) }
function worthYuan(v) { return ((v.actualValue || 0) / 100).toFixed(2) }

// 评价摘要：平均分 + 星级分布
const reviewSummary = computed(() => {
  if (!reviews.value.length) return null
  const sum = reviews.value.reduce((a, r) => a + (r.rating || 0), 0)
  const avg = sum / reviews.value.length
  const counts = [5, 4, 3, 2, 1].map(star => ({
    star,
    n: reviews.value.filter(r => (r.rating || 0) === star).length,
  }))
  counts.forEach(c => { c.pct = c.n / reviews.value.length * 100 })
  return { avg, counts }
})

// 排序后的信息流
const sortedList = computed(() => {
  const arr = [...list.value]
  if (sortMode.value === 'score') return arr.sort((a, b) => (b.score || 0) - (a.score || 0))
  if (sortMode.value === 'sold') return arr.sort((a, b) => (b.sold || 0) - (a.sold || 0))
  if (sortMode.value === 'near') return arr.sort((a, b) => (a.distance ?? Infinity) - (b.distance ?? Infinity))
  return arr
})

onMounted(() => {
  loadTypes()
  loadHotWords()
  loadBanners().then(startBanner)
  if (route.query.shopId) openShop(route.query.shopId)
})
onBeforeUnmount(stopBanner)
</script>

<template>
  <!-- ===== 首页：搜索 / 金刚区 / 轮播 / 信息流 ===== -->
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
      <!-- 热门搜索词 -->
      <div v-if="hotWords.length" class="hot-words">
        <span class="hot-label">🔥 热门</span>
        <span v-for="w in hotWords.slice(0, 6)" :key="w" class="hot-word" @click="hotSearch(w)">{{ w }}</span>
      </div>
    </div>

    <!-- 轮播 Banner -->
    <div v-if="banners.length" class="banner" @mouseenter="stopBanner" @mouseleave="startBanner">
      <div class="banner-track" :style="{ transform: `translateX(-${bannerIndex * 100}%)` }">
        <div v-for="(b, i) in banners" :key="i" class="banner-slide"
             :style="{ backgroundImage: b.image || b.grad }" @click="gotoBanner(b)">
          <span class="banner-icon">{{ b.icon }}</span>
          <span class="banner-text">
            <span class="banner-title">{{ b.title }}</span>
            <span class="banner-sub">{{ b.sub }}</span>
          </span>
          <span class="banner-go">去看看 ›</span>
        </div>
      </div>
      <div class="banner-dots">
        <span v-for="(b, i) in banners" :key="i" class="dot" :class="{ on: i === bannerIndex }" @click="bannerIndex = i"></span>
      </div>
    </div>

    <!-- 金刚区分类（2 行 × 5） -->
    <div class="kang">
      <div class="kang-item" v-for="(t, i) in shopTypes.slice(0, 10)" :key="t.id" @click="pickType(t.id)">
        <div class="kang-ico" :class="{ on: activeTypeId === t.id }" :style="kangStyle(i)">{{ emoji(t.name, i) }}</div>
        <div class="kang-name">{{ t.name }}</div>
      </div>
    </div>

    <!-- 信息流 -->
    <div class="feed">
      <div class="feed-head">
        <div class="feed-title">{{ feedTitle }}</div>
        <div class="sort-chips">
          <span v-for="s in SORTS" :key="s.v" class="sort-chip" :class="{ on: sortMode === s.v }" @click="sortMode = s.v">{{ s.text }}</span>
        </div>
      </div>
      <ShopListItem v-for="s in sortedList" :key="s.id" :shop="s" @open="openShop" />
      <div v-if="!sortedList.length && !msg" class="empty">暂无结果，换个关键词试试</div>
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
        <StarRating :score="selectedShop.score" :show-text="true" :size="16" />
        <span>　人均 ¥{{ selectedShop.avgPrice || 0 }}</span>
        <span>　已售 {{ selectedShop.sold || 0 }}</span>
        <span>　评论 {{ selectedShop.comments || 0 }}</span>
      </div>
      <div class="d-fav" :class="{ on: fav.fav }" @click="toggleFav">
        <span class="heart">{{ fav.fav ? '♥' : '♡' }}</span>
        <span>{{ fav.fav ? '已收藏' : '收藏' }}</span>
        <span v-if="fav.count" class="cnt">{{ fav.count }}</span>
      </div>
      <div class="d-line">📍 {{ selectedShop.area }} · {{ selectedShop.address }}</div>
      <div class="d-line">🕐 {{ selectedShop.openTime || '未填写营业时间' }}</div>
      <div v-if="selectedShop.phone" class="d-line">
        📞 {{ selectedShop.phone }}
        <a class="d-act" :href="'tel:' + selectedShop.phone">拨打</a>
        <a class="d-act" :href="mapUrl(selectedShop)" target="_blank" rel="noopener">导航</a>
      </div>
      <a v-else class="d-act" :href="mapUrl(selectedShop)" target="_blank" rel="noopener" style="margin-top:6px">🗺 查看地图</a>
      <div v-if="detailMsg" class="msg" :class="detailMsg.ok ? 'ok' : 'err'">{{ detailMsg.text }}</div>
    </div>

    <div class="d-card">
      <div class="d-sec">优惠券</div>
      <template v-if="vouchers.length">
        <div v-for="v in vouchers" :key="v.id" class="v-row">
          <div class="v-left">
            <div class="v-title">{{ isSk(v) ? '⚡ ' : '' }}{{ v.title }}</div>
            <div class="v-tags">
              <span v-for="(t, i) in voucherTags(v)" :key="i" class="v-tag">{{ t }}</span>
            </div>
          </div>
          <div class="v-price">¥{{ payYuan(v) }}<small>¥{{ worthYuan(v) }}</small></div>
          <button v-if="isSk(v)" class="btn sm" @click="buyVoucher(v)">抢购</button>
          <button v-else class="btn sm ghost" disabled>到店用</button>
        </div>
      </template>
      <div v-else class="empty">该店铺暂无优惠券</div>
    </div>

    <div class="d-card">
      <div class="d-sec">用户评价</div>
      <!-- 评价摘要 -->
      <div v-if="reviewSummary" class="review-summary">
        <div class="rs-left">
          <div class="rs-score">{{ reviewSummary.avg.toFixed(1) }}</div>
          <StarRating :score="reviewSummary.avg * 10" />
          <div class="rs-label">{{ reviews.length }} 条评价</div>
        </div>
        <div class="rs-right">
          <div v-for="c in reviewSummary.counts" :key="c.star" class="rs-bar">
            <span class="rs-star">{{ c.star }}星</span>
            <div class="rs-track"><div class="rs-fill" :style="{ width: c.pct + '%' }"></div></div>
            <span class="rs-num">{{ c.n }}</span>
          </div>
        </div>
      </div>

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
            <div class="nick">
              <img class="r-avatar" :src="r.icon || ''" alt="" @error="e => e.target.style.display='none'">
              <span class="r-nick">{{ r.nickName || '用户' + r.userId }}</span>
              <span class="muted">　<StarRating :score="(r.rating || 0) * 10" :size="11" /> · {{ r.createTime }}</span>
            </div>
            <div class="text">{{ fmtContent(r.content) }}</div>
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
/* ---------- 热门搜索词 ---------- */
.hot-words {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: nowrap;
  overflow-x: auto;
  scrollbar-width: none;
}
.hot-words::-webkit-scrollbar { display: none; }
.hot-label { font-size: 11px; color: rgba(255, 255, 255, .85); flex: none; }
.hot-word {
  flex: none;
  font-size: 11px;
  color: #fff;
  background: rgba(255, 255, 255, .18);
  padding: 3px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background .15s;
}
.hot-word:hover { background: rgba(255, 255, 255, .3); }

/* ---------- 轮播 Banner ---------- */
.banner {
  position: relative;
  margin: 10px 10px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(255, 96, 52, .18);
}
.banner-track {
  display: flex;
  transition: transform .45s ease;
}
.banner-slide {
  flex: 0 0 100%;
  min-height: 92px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  color: #fff;
  background-size: cover;
  background-position: center;
  cursor: pointer;
}
.banner-icon { font-size: 30px; flex: none; }
.banner-text { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 0; }
.banner-title { font-size: 17px; font-weight: 700; letter-spacing: 1px; }
.banner-sub { font-size: 12px; opacity: .9; }
.banner-go {
  font-size: 12px;
  background: rgba(255, 255, 255, .22);
  padding: 5px 12px;
  border-radius: 14px;
  flex: none;
}
.banner-dots {
  position: absolute;
  right: 12px;
  bottom: 8px;
  display: flex;
  gap: 5px;
}
.banner-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, .5);
  cursor: pointer;
  transition: width .2s;
}
.banner-dots .dot.on { width: 14px; background: #fff; }

/* ---------- 金刚区 ---------- */
.kang { grid-template-columns: repeat(5, 1fr); row-gap: 16px; }
.kang-ico.on { box-shadow: 0 0 0 2px #ff6034; }

/* ---------- 信息流标题 + 排序 ---------- */
.feed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px 8px;
}
.feed-head .feed-title { padding: 0; }
.sort-chips { display: flex; gap: 6px; }
.sort-chip {
  font-size: 12px;
  color: #888;
  padding: 4px 10px;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
}
.sort-chip.on { color: #ff6034; background: #fff0ea; font-weight: 600; }

/* ---------- 店铺详情 ---------- */
.d-meta { display: flex; align-items: center; gap: 2px; }
.d-line {
  margin-top: 6px;
  font-size: 13px;
  color: #555;
  display: flex;
  align-items: center;
  gap: 6px;
}
.d-act {
  font-size: 12px;
  color: #ff6034;
  border: 1px solid #ffd6c9;
  border-radius: 12px;
  padding: 2px 10px;
  text-decoration: none;
  cursor: pointer;
}
.d-act:hover { background: #fff5f1; }

/* 券标签 */
.v-tags { display: flex; gap: 5px; margin-top: 4px; flex-wrap: wrap; }
.v-tag {
  font-size: 10px;
  color: #ff6034;
  background: #fff1ec;
  border-radius: 4px;
  padding: 1px 6px;
}

/* 评价摘要 */
.review-summary {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px dashed #f0f0f0;
  margin-bottom: 10px;
}
.rs-left {
  flex: none;
  width: 88px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.rs-score { font-size: 30px; font-weight: 800; color: #ff6034; line-height: 1; }
.rs-label { font-size: 11px; color: #999; }
.rs-right { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 5px; }
.rs-bar { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #999; }
.rs-star { flex: none; width: 28px; }
.rs-track { flex: 1; height: 6px; background: #f2f2f2; border-radius: 3px; overflow: hidden; }
.rs-fill { height: 100%; background: linear-gradient(90deg, #ff6034, #ff8a3d); border-radius: 3px; }
.rs-num { flex: none; width: 16px; text-align: right; }

/* 评价头像 */
.r-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  background: #eee;
  margin-right: 6px;
  vertical-align: middle;
}
.r-nick { font-weight: 600; }
</style>
