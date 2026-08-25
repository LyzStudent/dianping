<script setup>
import { onMounted, ref } from 'vue'
import { api, auth } from '../api'

const email = ref('')
const code = ref('')
const sending = ref(false)
const msg = ref(null)
const signCount = ref('-')

function show(text, ok) { msg.value = { text, ok }; }

async function sendCode() {
  if (!email.value) return show('请输入邮箱', false);
  sending.value = true;
  try {
    await api.post('/user/code?phone=' + encodeURIComponent(email.value));
    show('验证码已发送，请到 user-service 控制台查看', true);
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

async function signIn() {
  try { await api.post('/user/sign'); show('签到成功', true); refreshProfile(); }
  catch (e) { show(e.message, false); }
}

async function refreshProfile() {
  await auth.refreshMe();
  if (!auth.loggedIn) return;
  try { signCount.value = (await api.get('/user/sign/count')) ?? '-'; } catch { }
}

onMounted(refreshProfile);
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
    <div class="hint">未注册的邮箱会先自动注册再发码；验证码打印在 user-service 控制台日志里（MockMailSender）。</div>
  </div>

  <!-- 已登录：用户信息 -->
  <div v-else class="card">
    <div class="user-row">
      <img class="user-avatar" :src="auth.me.icon || ''" @error="e => e.target.style.display='none'">
      <div>
        <div style="font-size:18px;font-weight:600">{{ auth.me.nickName }}</div>
        <div class="muted">用户ID：{{ auth.me.id }}</div>
      </div>
    </div>
    <div class="stat-row"><div>连续签到 <b>{{ signCount }}</b> 天</div></div>
    <div class="form-row" style="margin-top:12px">
      <button class="btn" @click="signIn">今日签到</button>
      <button class="btn dark" @click="refreshProfile">刷新</button>
    </div>
    <div v-if="msg" class="msg" :class="msg.ok ? 'ok' : 'err'">{{ msg.text }}</div>
  </div>
</template>
