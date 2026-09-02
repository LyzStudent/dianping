<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, auth } from '../api'

const router = useRouter()

const blogForm = ref({ shopId: 1, title: '', image: '', content: '' })
const bMsg = ref(null)
const mode = ref('hot')
const blogs = ref([])
const showPublish = ref(false)

// blogId -> 展开状态 / 评论列表 / 关注状态 / 评论输入
const expanded = reactive({})
const commentsMap = reactive({})
const followState = reactive({})
const commentInput = reactive({})

function show(el, text, ok) { el.value = text ? { text, ok } : null; }
function imgList(images) { return (images || '').split(',').filter(Boolean); }
function avatar(b) { return b.icon || '' }
function nameOf(b) { return b.name || '用户' + b.userId }
function fmtTime(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '' }
// 笔记/评论里的 <br>/<br/> 转成换行；其余 HTML 由 {{ }} 自动转义，不会注入
function fmtContent(text) { return (text || '').replace(/<br\s*\/?>/gi, '\n') }

async function loadBlogs(kind) {
  mode.value = kind;
  try {
    if (kind === 'hot') blogs.value = await api.get('/blog/hot?current=1') || [];
    else if (kind === 'mine') blogs.value = await api.get('/blog/of/me?current=1') || [];
  } catch (e) { show(bMsg, e.message, false); return; }
  show(bMsg, null, true);
  if (auth.loggedIn) blogs.value.forEach(checkFollow);
}

async function loadFollowFeed() {
  mode.value = 'follow';
  try {
    const data = await api.get('/blog/of/follow?lastId=9999999999999&offset=0');
    blogs.value = (data && data.list) || [];
    show(bMsg, blogs.value.length ? '已加载关注动态' : '关注动态为空（先关注别人并让对方发笔记）', true);
    if (auth.loggedIn) blogs.value.forEach(checkFollow);
  } catch (e) { show(bMsg, e.message, false); }
}

async function checkFollow(b) {
  if (!auth.loggedIn) return;
  try { followState[b.id] = (await api.get('/follow/or/not/' + b.userId)) ? '1' : '0'; }
  catch { followState[b.id] = '0'; }
}

async function toggleFollow(b) {
  if (!auth.loggedIn) return show(bMsg, '请先登录', false);
  const next = followState[b.id] === '1' ? '0' : '1';
  try { await api.put(`/follow/${b.userId}/${next === '1'}`); followState[b.id] = next; }
  catch (e) { show(bMsg, e.message, false); }
}

async function toggleLike(b) {
  if (!auth.loggedIn) return show(bMsg, '请先登录再点赞', false);
  try {
    await api.put('/blog/like/' + b.id);
    b.isLike = !b.isLike;
    b.liked = (b.liked || 0) + (b.isLike ? 1 : -1);
  } catch (e) { show(bMsg, e.message, false); }
}

async function createBlog() {
  if (!blogForm.value.title || !blogForm.value.content) return show(bMsg, '标题和内容必填', false);
  try {
    const id = await api.post('/blog', blogForm.value);
    show(bMsg, '发布成功', true);
    blogForm.value = { shopId: blogForm.value.shopId, title: '', image: '', content: '' };
    showPublish.value = false;
    loadBlogs('hot');
  } catch (e) { show(bMsg, e.message, false); }
}

async function uploadImage(e) {
  const file = e.target.files[0];
  if (!file) return;
  const fd = new FormData();
  fd.append('file', file);
  try {
    const name = await api.post('/upload/blog', fd, { isForm: true });
    blogForm.value.image = blogForm.value.image ? blogForm.value.image + ',' + name : name;
  } catch (err) { show(bMsg, '上传失败：' + err.message, false); }
  e.target.value = '';
}

async function toggleComments(b) {
  if (!expanded[b.id]) { expanded[b.id] = true; await loadComments(b); }
  else expanded[b.id] = false;
}

async function loadComments(b) {
  try { commentsMap[b.id] = await api.get('/blog-comments/of/blog?blogId=' + b.id + '&current=1') || []; }
  catch { commentsMap[b.id] = []; }
}

async function addComment(b) {
  const content = (commentInput[b.id] || '').trim();
  if (!content) return;
  try {
    await api.post('/blog-comments', { blogId: b.id, content });
    commentInput[b.id] = '';
    b.comments = (b.comments || 0) + 1;
    await loadComments(b);
  } catch (e) { show(bMsg, e.message, false); }
}

async function likeComment(c) {
  if (!auth.loggedIn) return show(bMsg, '请先登录', false);
  try {
    await api.put('/blog-comments/like/' + c.id);
    c.isLike = !c.isLike;
    c.liked = (c.liked || 0) + (c.isLike ? 1 : -1);
  } catch (e) { show(bMsg, e.message, false); }
}

onMounted(() => loadBlogs('hot'));

/** 点击笔记卡片打开详情页 */
function openDetail(id) { router.push('/blog/' + id) }
</script>

<template>
  <div class="note-page">
    <!-- 顶部段式切换 -->
    <div class="note-seg">
      <button :class="{ active: mode === 'hot' }" @click="loadBlogs('hot')">热门</button>
      <button :class="{ active: mode === 'follow' }" @click="loadFollowFeed">关注</button>
      <button :class="{ active: mode === 'mine' }" @click="loadBlogs('mine')">我的</button>
    </div>

    <div v-if="bMsg" class="note-msg" :class="bMsg.ok ? 'ok' : 'err'">{{ bMsg.text }}</div>

    <!-- 笔记流 -->
    <div class="note-list">
      <div v-for="b in blogs" :key="b.id" class="note-card" @click="openDetail(b.id)">
        <!-- 图：1张大图 / 多张小图网格 -->
        <div v-if="imgList(b.image).length" class="note-imgs" :class="'c' + Math.min(imgList(b.image).length, 3)">
          <img v-for="u in imgList(b.image).slice(0, 3)" :key="u" :src="u" @error="e => e.target.style.display='none'">
        </div>

        <div class="note-body">
          <div class="note-title">{{ b.title }}</div>
          <div class="note-content">{{ fmtContent(b.content) }}</div>
        </div>

        <!-- 作者行 -->
        <div class="note-author">
          <img :src="avatar(b)" @error="e => e.target.style.display='none'">
          <span class="nm">{{ nameOf(b) }}</span>
          <span v-if="fmtTime(b.createTime)" class="note-time">{{ fmtTime(b.createTime) }}</span>
          <span
            v-if="auth.loggedIn && !(auth.me && b.userId === auth.me.id)"
            class="f-btn"
            :class="{ on: followState[b.id] === '1' }"
            @click.stop="toggleFollow(b)"
          >{{ followState[b.id] === '1' ? '已关注' : '+ 关注' }}</span>
        </div>

        <!-- 操作栏 -->
        <div class="note-actions">
          <span :class="{ liked: b.isLike }" @click.stop="toggleLike(b)">♥ {{ b.liked || 0 }}</span>
          <span @click.stop="toggleComments(b)">💬 {{ b.comments || 0 }}</span>
        </div>

        <!-- 评论 -->
        <div v-if="expanded[b.id]" class="note-comments" @click.stop>
          <div v-if="auth.loggedIn" class="cbox">
            <input type="text" v-model="commentInput[b.id]" placeholder="写评论…" @keyup.enter="addComment(b)">
            <button class="btn sm" @click="addComment(b)">发送</button>
          </div>
          <div v-for="c in commentsMap[b.id] || []" :key="c.id" class="comment">
            <div class="nick">
              {{ c.nickName || '用户' + c.userId }}
              <span class="muted">{{ c.createTime }}</span>
              <span :class="{ liked: c.isLike }" style="float:right;cursor:pointer" @click="likeComment(c)">♥ {{ c.liked || 0 }}</span>
            </div>
            <div class="text">{{ fmtContent(c.content) }}</div>
          </div>
          <div v-if="!(commentsMap[b.id] || []).length" class="muted" style="padding:4px 0">暂无评论</div>
        </div>
      </div>

      <div v-if="!blogs.length && !bMsg" class="empty" style="padding:40px 0">暂无笔记</div>
    </div>

    <!-- 悬浮发布按钮 -->
    <button class="fab" title="发布笔记" @click="showPublish = true">✏️</button>

    <!-- 发布弹层 -->
    <div v-if="showPublish" class="note-mask" @click.self="showPublish = false">
      <div class="note-sheet">
        <h3>发布探店笔记</h3>
        <div class="s-row"><label>店铺ID</label><input type="number" v-model="blogForm.shopId"></div>
        <div class="s-row"><label>标题</label><input type="text" v-model="blogForm.title" placeholder="写下标题"></div>
        <div class="s-row">
          <label>图片</label>
          <input type="text" v-model="blogForm.image" placeholder="逗号分隔图片URL">
          <button class="btn sm" @click="$refs.up.click()">上传</button>
        </div>
        <div class="s-row"><label>内容</label><textarea v-model="blogForm.content" placeholder="探店体验…"></textarea></div>
        <div v-if="bMsg && showPublish" class="note-msg" :class="bMsg.ok ? 'ok' : 'err'">{{ bMsg.text }}</div>
        <div class="s-btn">
          <button class="no" @click="showPublish = false">取消</button>
          <button class="ok" @click="createBlog">发布</button>
        </div>
      </div>
    </div>
    <input type="file" ref="up" style="display:none" @change="uploadImage">
  </div>
</template>

<style scoped>
.note-page { padding-bottom: 90px; }

/* 顶部段式切换 */
.note-seg {
  position: sticky; top: 0; z-index: 20;
  display: flex; gap: 8px; padding: 10px 14px;
  background: rgba(245,245,245,.95); backdrop-filter: blur(6px);
}
.note-seg button {
  flex: 1; height: 32px; border: none; border-radius: 16px;
  background: #fff; color: #333; font-size: 14px; cursor: pointer;
}
.note-seg button.active { background: #ff6034; color: #fff; font-weight: 600; }

/* 信息流 */
.note-list { padding: 4px 12px 12px; }
.note-card {
  background: #fff; border-radius: 14px; margin-bottom: 12px;
  overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,.06);
  cursor: pointer;
}
.note-card:active { opacity: .85; }
.note-imgs { display: grid; gap: 2px; background: #f0f0f0; }
.note-imgs.c1 { grid-template-columns: 1fr; }
.note-imgs.c2 { grid-template-columns: 1fr 1fr; }
.note-imgs.c3 { grid-template-columns: 1fr 1fr 1fr; }
.note-imgs img { width: 100%; height: 100%; object-fit: cover; display: block; }
.note-imgs.c1 img { aspect-ratio: 4 / 3; }
.note-imgs.c2 img, .note-imgs.c3 img { aspect-ratio: 1; }

.note-body { padding: 12px 14px 8px; }
.note-title { font-size: 16px; font-weight: 700; color: #222; margin-bottom: 4px; }
.note-content {
  font-size: 13px; color: #666; line-height: 1.6;
  white-space: pre-line;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}

.note-author { display: flex; align-items: center; gap: 8px; padding: 8px 14px 4px; }
.note-author img { width: 26px; height: 26px; border-radius: 50%; object-fit: cover; background: #eee; }
.note-author .nm { flex: 1; font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.note-time { flex: none; font-size: 11px; color: #bbb; }
.note-author .f-btn { font-size: 12px; color: #ff6034; cursor: pointer; }
.note-author .f-btn.on { color: #999; }

.note-actions { display: flex; gap: 24px; padding: 6px 14px 10px; }
.note-actions span { font-size: 13px; color: #666; cursor: pointer; }
.note-actions .liked { color: #ff6034; }

/* 评论 */
.note-comments { border-top: 1px solid #f0f0f0; padding: 8px 14px 10px; background: #fafafa; }
.note-comments .cbox { display: flex; gap: 6px; margin-bottom: 8px; }
.note-comments .cbox input {
  flex: 1; height: 30px; border: 1px solid #eee; border-radius: 15px;
  padding: 0 12px; font-size: 13px; background: #fff;
}
.note-comments .comment .text { white-space: pre-line; }

.note-msg { margin: 8px 14px; padding: 8px 12px; border-radius: 8px; font-size: 13px; }
.note-msg.ok { background: #e8f5e9; color: #2e7d32; }
.note-msg.err { background: #fdecea; color: #c62828; }

/* 悬浮发布按钮（避开底部 TabBar，壳内居中） */
.fab {
  position: fixed; bottom: 78px; right: max(12px, calc(50% - 203px));
  width: 52px; height: 52px; border-radius: 50%; border: none;
  background: linear-gradient(135deg, #ff6034, #ff7f50);
  color: #fff; font-size: 22px; cursor: pointer; z-index: 30;
  box-shadow: 0 4px 12px rgba(255,96,52,.4);
}

/* 发布弹层 */
.note-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 40; }
.note-sheet {
  position: fixed; left: 0; right: 0; bottom: 0; margin: 0 auto;
  max-width: 430px; background: #fff; border-radius: 16px 16px 0 0;
  padding: 14px 16px calc(16px + env(safe-area-inset-bottom));
  max-height: 80vh; overflow-y: auto; z-index: 41;
}
.note-sheet h3 { font-size: 16px; margin-bottom: 10px; }
.s-row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.s-row label { font-size: 13px; color: #666; width: 56px; flex: none; }
.s-row input, .s-row textarea {
  flex: 1; border: 1px solid #eee; border-radius: 8px; padding: 8px 10px; font-size: 14px;
}
.s-row textarea { resize: none; min-height: 80px; }
.s-btn { display: flex; gap: 10px; margin-top: 4px; }
.s-btn button { flex: 1; height: 40px; border-radius: 20px; border: none; font-size: 14px; cursor: pointer; }
.s-btn .ok { background: #ff6034; color: #fff; }
.s-btn .no { background: #f0f0f0; color: #333; }
</style>
