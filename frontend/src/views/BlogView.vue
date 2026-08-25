<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api, auth } from '../api'

const blogForm = ref({ shopId: 1, title: '', image: '', content: '' })
const bMsg = ref(null)
const mode = ref('hot')
const blogs = ref([])

// blogId -> 展开状态 / 评论列表 / 关注状态 / 评论输入
const expanded = reactive({})
const commentsMap = reactive({})
const followState = reactive({})
const commentInput = reactive({})

function show(el, text, ok) { el.value = text ? { text, ok } : null; }
function imgList(images) { return (images || '').split(',').filter(Boolean); }

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
    show(bMsg, `发布成功，blogId=${id}`, true);
    blogForm.value.title = ''; blogForm.value.content = ''; blogForm.value.image = '';
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
    show(bMsg, `上传成功，图片URL=${name}`, true);
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
</script>

<template>
  <!-- 发布笔记 -->
  <div class="card">
    <h3>发布探店笔记</h3>
    <div class="form-row">
      <label class="field-label">店铺ID</label>
      <input type="number" v-model="blogForm.shopId">
      <label class="field-label">图片URL</label>
      <input type="text" v-model="blogForm.image" placeholder="逗号分隔多个图片URL" style="flex:1">
      <button class="btn dark" @click="$refs.up.click()">上传图片</button>
      <input type="file" ref="up" style="display:none" @change="uploadImage">
    </div>
    <div class="form-row"><label class="field-label">标题</label><input type="text" v-model="blogForm.title" placeholder="标题"></div>
    <div class="form-row"><label class="field-label">内容</label><textarea v-model="blogForm.content" rows="3" style="flex:1" placeholder="探店内容"></textarea></div>
    <button class="btn" @click="createBlog">发布</button>
    <div v-if="bMsg" class="msg" :class="bMsg.ok ? 'ok' : 'err'">{{ bMsg.text }}</div>
    <div class="hint">上传走 /upload/blog（免登录，存本地磁盘，无静态映射时浏览器预览不到，仅拿到路径填进上面的图片URL）。</div>
  </div>

  <!-- 笔记流 -->
  <div class="card">
    <div class="row-between">
      <h3>笔记流</h3>
      <div class="form-row" style="margin:0">
        <button class="btn ghost sm" :class="{ 'btn-active': mode === 'hot' }" @click="loadBlogs('hot')">热门</button>
        <button class="btn ghost sm" :class="{ 'btn-active': mode === 'mine' }" @click="loadBlogs('mine')">我的笔记</button>
        <button class="btn ghost sm" :class="{ 'btn-active': mode === 'follow' }" @click="loadFollowFeed">关注动态</button>
      </div>
    </div>

    <div v-for="b in blogs" :key="b.id" class="blog-card">
      <div class="title">{{ b.title }}</div>
      <div v-if="imgList(b.image).length" class="images">
        <img v-for="u in imgList(b.image)" :key="u" :src="u" @error="e => e.target.style.display='none'">
      </div>
      <div class="content">{{ b.content }}</div>
      <div class="actions">
        <span>👤 {{ b.name || '用户' + b.userId }}
          <template v-if="auth.loggedIn">
            <a class="follow-link" @click="toggleFollow(b)">{{ followState[b.id] === '1' ? '已关注' : '关注TA' }}</a>
          </template>
        </span>
        <span :class="{ liked: b.isLike }" @click="toggleLike(b)">👍 {{ b.liked || 0 }}</span>
        <span @click="toggleComments(b)">💬 {{ b.comments || 0 }} 评论</span>
      </div>

      <!-- 评论 -->
      <div v-if="expanded[b.id]" class="blog-comments-box">
        <div class="form-row" style="margin-top:10px">
          <input type="text" v-model="commentInput[b.id]" placeholder="写评论…" style="flex:1" @keyup.enter="addComment(b)">
          <button class="btn sm" @click="addComment(b)">发表评论</button>
        </div>
        <div v-if="(commentsMap[b.id] || []).length">
          <div v-for="c in commentsMap[b.id]" :key="c.id" class="comment">
            <div class="nick">
              {{ c.nickName || '用户' + c.userId }}
              <span class="muted" style="font-weight:400">{{ c.createTime }}</span>
              <span :class="{ liked: c.isLike }" style="cursor:pointer;font-size:12px;float:right" @click="likeComment(c)">👍 {{ c.liked || 0 }}</span>
            </div>
            <div class="text">{{ c.content }}</div>
          </div>
        </div>
        <div v-else class="muted">暂无评论</div>
      </div>
    </div>

    <div v-if="!blogs.length" class="empty">暂无笔记</div>
  </div>
</template>

<style scoped>
.btn-active { border-color: #ff6034 !important; }
</style>
