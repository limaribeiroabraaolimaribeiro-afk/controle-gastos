(function () {
  "use strict";

  const App = {};
  window.App = App;

  /* =========================
     CONFIG
  ========================= */
  App.FIREBASE_CONFIG = {
    apiKey: "AIzaSyAlzYNuMGe1UZbgYjCDATv8ChO0ysrEPLE",
    authDomain: "controle-gastos-pro.firebaseapp.com",
    projectId: "controle-gastos-pro"
  };

  App.API = "https://api-rf1w.onrender.com";

  App.DEFAULT_CATEGORIAS = [
    "Casa",
    "Alimentação",
    "Transporte",
    "Lazer",
    "Pets",
    "Investimentos",
    "Outros"
  ];

  App.THEME_KEYS = {
    preset: "theme_preset_v1",
    accent: "theme_accent_v1"
  };

  App.NOTIF_KEYS = {
    permAsked: "notif_perm_asked_v1",
    sentPrefix: "notif_sent_v1"
  };

  App.VOICE_KEYS = {
    enabled: "voice_enabled_v1"
  };

  App.THEME_PRESETS = {
    default: {
      dark:  { bg:"#07101f", g1:"rgba(86,156,255,.22)", g2:"rgba(57,217,138,.16)", g3:"rgba(255,93,93,.12)" },
      light: { bg:"#f4f6ff", g1:"rgba(86,156,255,.14)", g2:"rgba(57,217,138,.10)", g3:"rgba(255,93,93,.08)" }
    },
    purple: {
      dark:  { bg:"#0d0a1f", g1:"rgba(170,110,255,.25)", g2:"rgba(86,156,255,.12)", g3:"rgba(57,217,138,.10)" },
      light: { bg:"#fbf8ff", g1:"rgba(170,110,255,.16)", g2:"rgba(86,156,255,.10)", g3:"rgba(57,217,138,.08)" }
    },
    green: {
      dark:  { bg:"#061a14", g1:"rgba(57,217,138,.28)", g2:"rgba(86,156,255,.10)", g3:"rgba(255,209,102,.10)" },
      light: { bg:"#f7fffb", g1:"rgba(57,217,138,.16)", g2:"rgba(86,156,255,.08)", g3:"rgba(255,209,102,.10)" }
    },
    sunset: {
      dark:  { bg:"#1a0b0b", g1:"rgba(255,93,93,.22)", g2:"rgba(255,209,102,.18)", g3:"rgba(86,156,255,.10)" },
      light: { bg:"#fff7f2", g1:"rgba(255,93,93,.14)", g2:"rgba(255,209,102,.14)", g3:"rgba(86,156,255,.08)" }
    },
    mono: {
      dark:  { bg:"#05070c", g1:"rgba(255,255,255,.10)", g2:"rgba(255,255,255,.06)", g3:"rgba(255,255,255,.04)" },
      light: { bg:"#ffffff", g1:"rgba(0,0,0,.06)", g2:"rgba(0,0,0,.04)", g3:"rgba(0,0,0,.03)" }
    }
  };

  App.regras = [
    { palavra:"aluguel", categoria:"Casa" },
    { palavra:"luz", categoria:"Casa" },
    { palavra:"agua", categoria:"Casa" },
    { palavra:"água", categoria:"Casa" },
    { palavra:"mercado", categoria:"Alimentação" },
    { palavra:"padaria", categoria:"Alimentação" },
    { palavra:"gasolina", categoria:"Transporte" },
    { palavra:"uber", categoria:"Transporte" },
    { palavra:"ração", categoria:"Pets" },
    { palavra:"pet", categoria:"Pets" },
    { palavra:"cdb", categoria:"Investimentos" },
    { palavra:"tesouro", categoria:"Investimentos" },
    { palavra:"invest", categoria:"Investimentos" }
  ];

  /* =========================
     STATE
  ========================= */
  App.auth = null;
  App.USER = null;
  App.USER_ID = null;

  App.state = { meses:{} };
  App.categorias = [...App.DEFAULT_CATEGORIAS];
  App.fixos = [];
  App.investFixos = [];

  /* =========================
     HELPERS
  ========================= */
  App.money = function (n) {
    return Number(n || 0).toLocaleString("pt-BR", {
      style: "currency",
      currency: "BRL"
    });
  };

  App.uid = function () {
    return Math.random().toString(16).slice(2) + Date.now().toString(16);
  };

  App.todayISO = function () {
    return new Date().toISOString().slice(0, 10);
  };

  App.escapeHtml = function (s) {
    return String(s || "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  };

  App.normalize = function (text) {
    return String(text || "")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase()
      .trim();
  };

  App.safeParse = function (json, fallback) {
    try {
      return JSON.parse(json);
    } catch (e) {
      return fallback;
    }
  };

  App.detectarCategoria = function (desc) {
    const txt = String(desc || "").toLowerCase();
    for (const r of App.regras) {
      if (txt.includes(r.palavra)) return r.categoria;
    }
    return "Outros";
  };

  App.isLightMode = function () {
    return document.body.classList.contains("light");
  };

  App.showLoading = function () {
    const loading = document.getElementById("loading");
    const app = document.getElementById("app");
    if (loading) loading.style.display = "flex";
    if (app) app.style.display = "none";
  };

  App.showApp = function () {
    const loading = document.getElementById("loading");
    const app = document.getElementById("app");
    if (loading) loading.style.display = "none";
    if (app) app.style.display = "block";
  };

  App.fixKey = function (desc, amount, category) {
    return `${String(desc || "").toLowerCase()}|${Number(amount || 0)}|${String(category || "").toLowerCase()}`;
  };

  App.ensureItemDefaults = function (it) {
    if (!it) return it;
    if (typeof it.paid !== "boolean") it.paid = false;
    return it;
  };

  App.kMes = function () {
    return "gastos_pro_state_" + App.USER_ID;
  };

  App.kCat = function () {
    return "categorias_" + App.USER_ID;
  };

  App.kFix = function () {
    return "fixos_" + App.USER_ID;
  };

  App.kInv = function () {
    return "inv_fixos_" + App.USER_ID;
  };

  App.kVoiceEnabled = function () {
    return `${App.VOICE_KEYS.enabled}_${App.USER_ID || "guest"}`;
  };

  App.kNotifSent = function (invId, mes, tag) {
    return `${App.NOTIF_KEYS.sentPrefix}_${App.USER_ID || "guest"}_${invId}_${mes}_${tag}`;
  };

  /* =========================
     THEME
  ========================= */
  App.applyThemeSettings = function () {
    const preset = localStorage.getItem(App.THEME_KEYS.preset) || "default";
    const accent = localStorage.getItem(App.THEME_KEYS.accent) || "#569cff";
    const p = App.THEME_PRESETS[preset] || App.THEME_PRESETS.default;
    const mode = App.isLightMode() ? "light" : "dark";
    const colors = p[mode] || p.dark;

    document.documentElement.style.setProperty("--accent", accent);
    document.documentElement.style.setProperty("--bg", colors.bg);
    document.documentElement.style.setProperty("--grad1", colors.g1);
    document.documentElement.style.setProperty("--grad2", colors.g2);
    document.documentElement.style.setProperty("--grad3", colors.g3);

    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", accent);
  };

  App.toggleTheme = function () {
    document.body.classList.toggle("light");
    localStorage.setItem("theme", document.body.classList.contains("light") ? "light" : "dark");
    App.applyThemeSettings();
  };

  App.setThemePreset = function (preset) {
    localStorage.setItem(App.THEME_KEYS.preset, preset);
    App.applyThemeSettings();
  };

  App.setAccentColor = function (color) {
    localStorage.setItem(App.THEME_KEYS.accent, color);
    App.applyThemeSettings();
  };

  App.resetThemes = function () {
    localStorage.removeItem(App.THEME_KEYS.preset);
    localStorage.removeItem(App.THEME_KEYS.accent);
    App.applyThemeSettings();
  };

  /* =========================
     STORAGE
  ========================= */
  App.loadAll = function () {
    try {
      const raw = localStorage.getItem(App.kMes());
      App.state = raw ? JSON.parse(raw) : { meses:{} };
    } catch (e) {
      App.state = { meses:{} };
    }

    if (!App.state || typeof App.state !== "object") App.state = { meses:{} };
    if (!App.state.meses || typeof App.state.meses !== "object") App.state.meses = {};

    try {
      const raw = localStorage.getItem(App.kCat());
      App.categorias = raw ? JSON.parse(raw) : [...App.DEFAULT_CATEGORIAS];
      if (!Array.isArray(App.categorias) || !App.categorias.length) App.categorias = [...App.DEFAULT_CATEGORIAS];
      if (!App.categorias.some(c => String(c).toLowerCase() === "investimentos")) {
        App.categorias.splice(App.categorias.length - 1, 0, "Investimentos");
      }
    } catch (e) {
      App.categorias = [...App.DEFAULT_CATEGORIAS];
    }

    try {
      const raw = localStorage.getItem(App.kFix());
      App.fixos = raw ? JSON.parse(raw) : [];
      if (!Array.isArray(App.fixos)) App.fixos = [];
      App.fixos = App.fixos.map(f => ({ ...f, isFixo:true }));
    } catch (e) {
      App.fixos = [];
    }

    try {
      const raw = localStorage.getItem(App.kInv());
      App.investFixos = raw ? JSON.parse(raw) : [];
      if (!Array.isArray(App.investFixos)) App.investFixos = [];
    } catch (e) {
      App.investFixos = [];
    }
  };

  App.saveAll = function () {
    localStorage.setItem(App.kMes(), JSON.stringify(App.state));
    localStorage.setItem(App.kCat(), JSON.stringify(App.categorias));
    localStorage.setItem(App.kFix(), JSON.stringify(App.fixos));
    localStorage.setItem(App.kInv(), JSON.stringify(App.investFixos));
  };

  /* =========================
     MONTH DATA
  ========================= */
  App.ensureSavedArray = function (d) {
    if (!Array.isArray(d.saved)) d.saved = [];
    return d.saved;
  };

  App.ensureMonthInvestStatus = function (d) {
    if (!d.investStatus || typeof d.investStatus !== "object") d.investStatus = {};
    return d.investStatus;
  };

  App.currentMonthKeyFromDate = function (baseDate = new Date()) {
    const y = baseDate.getFullYear();
    const m = String(baseDate.getMonth() + 1).padStart(2, "0");
    return `${y}-${m}`;
  };

  App.getDataMes = function (m) {
    const mm = m || App.currentMonthKeyFromDate();
    if (!App.state.meses[mm]) {
      App.state.meses[mm] = {
        salary:0,
        meta:0,
        items:[],
        saved:[],
        hiddenFixos:[],
        investStatus:{}
      };
    }

    const d = App.state.meses[mm];
    if (!Array.isArray(d.items)) d.items = [];
    if (!Array.isArray(d.hiddenFixos)) d.hiddenFixos = [];
    App.ensureSavedArray(d);
    App.ensureMonthInvestStatus(d);

    d.items = d.items.map(App.ensureItemDefaults);

    const hidden = new Set(d.hiddenFixos.map(String));
    const present = new Set(d.items.map(it => App.fixKey(it.desc, it.amount, it.category)));

    for (const f of App.fixos) {
      const fk = App.fixKey(f.desc, f.amount, f.category);
      if (hidden.has(fk)) continue;
      if (present.has(fk)) continue;
      d.items.unshift(App.ensureItemDefaults({ ...f, isFixo:true, isInvestFixo:false }));
      present.add(fk);
    }

    for (const inv of App.investFixos) {
      const desc = `Juntar: ${inv.nome}`;
      const amount = Number(inv.mensal || 0);
      const fk = App.fixKey(desc, amount, "Investimentos");
      if (hidden.has(fk)) continue;
      if (present.has(fk)) continue;

      if (typeof d.investStatus[inv.id] !== "boolean") d.investStatus[inv.id] = false;
      const paid = !!d.investStatus[inv.id];

      d.items.unshift(App.ensureItemDefaults({
        desc,
        amount,
        category:"Investimentos",
        isFixo:true,
        isInvestFixo:true,
        invId: inv.id,
        diaPagar: Number(inv.diaPagar || 0),
        paid
      }));
      present.add(fk);
    }

    return d;
  };

  App.getMonthSummary = function (m) {
    const d = App.getDataMes(m);
    const salary = Number(d.salary || 0);
    const spent = (d.items || []).reduce((a, b) => a + Number(b.amount || 0), 0);
    const saved = (d.saved || []).reduce((a, b) => a + Number(b.value || 0), 0);
    const left = salary - spent;
    return { salary, spent, saved, left };
  };

  /* =========================
     SAVED / INVEST AUTO
  ========================= */
  App.hasAutoSavedForInv = function (d, invId, month) {
    const saved = App.ensureSavedArray(d);
    return saved.some(s => s && s.source === "investFixo" && s.invId === invId && s.month === month);
  };

  App.addAutoSavedForInv = function (d, invId, month, value, desc) {
    const saved = App.ensureSavedArray(d);
    if (App.hasAutoSavedForInv(d, invId, month)) return;
    saved.unshift({
      value: Number(value || 0),
      desc: desc || "Investimento fixo",
      date: App.todayISO(),
      source: "investFixo",
      invId,
      month
    });
  };

  App.removeAutoSavedForInv = function (d, invId, month) {
    const saved = App.ensureSavedArray(d);
    d.saved = saved.filter(s => !(s && s.source === "investFixo" && s.invId === invId && s.month === month));
  };

  /* =========================
     FIXOS
  ========================= */
  App.addFixo = function ({ desc, amount, category }) {
    App.fixos.push({
      desc,
      amount: Number(amount || 0),
      category: category || "Outros",
      isFixo: true
    });
    App.saveAll();
  };

  App.removeFixo = function (idx) {
    if (idx < 0 || idx >= App.fixos.length) return false;
    App.fixos.splice(idx, 1);
    App.saveAll();
    return true;
  };

  App.hideFixoOnlyThisMonth = function (monthKey, item) {
    const d = App.getDataMes(monthKey);
    const fk = App.fixKey(item.desc, item.amount, item.category);
    if (!d.hiddenFixos.includes(fk)) d.hiddenFixos.push(fk);
    d.items = (d.items || []).filter(x => App.fixKey(x.desc, x.amount, x.category) !== fk);

    if (item.isInvestFixo && item.invId) {
      App.removeAutoSavedForInv(d, item.invId, monthKey);
      App.ensureMonthInvestStatus(d);
      d.investStatus[item.invId] = false;
    }

    App.saveAll();
  };

  /* =========================
     CATEGORIAS
  ========================= */
  App.addCategoria = function (nome) {
    const val = String(nome || "").trim();
    if (!val) return { ok:false, error:"Digite o nome da categoria." };
    if (App.categorias.some(c => String(c).toLowerCase() === val.toLowerCase())) {
      return { ok:false, error:"Essa categoria já existe." };
    }

    App.categorias.push(val);
    App.saveAll();
    return { ok:true };
  };

  App.removeCategoria = function (idx) {
    if (idx < 0 || idx >= App.categorias.length) return { ok:false, error:"Índice inválido." };

    const cat = App.categorias[idx];
    if (String(cat).toLowerCase() === "investimentos") {
      return { ok:false, error:"A categoria Investimentos não pode ser removida." };
    }

    App.categorias.splice(idx, 1);

    App.fixos = App.fixos.map(f => {
      if (String(f.category || "") === cat) return { ...f, category:"Outros" };
      return f;
    });

    Object.keys(App.state.meses || {}).forEach(m => {
      const d = App.state.meses[m];
      if (!d) return;
      if (Array.isArray(d.items)) {
        d.items = d.items.map(it => {
          if (String(it.category || "") === cat) return { ...it, category:"Outros" };
          return it;
        });
      }
    });

    App.saveAll();
    return { ok:true };
  };

  /* =========================
     INVEST FIXOS
  ========================= */
  App.calcMensalAuto = function (metaFinal, prazoMeses) {
    const meta = Number(metaFinal || 0);
    const meses = Number(prazoMeses || 0);
    if (!meta || meta <= 0) return 0;
    if (!meses || meses <= 0) return 0;
    return Math.round((meta / meses) * 100) / 100;
  };

  App.addInvestFixo = function ({ nome, metaFinal, prazoMeses, diaPagar }) {
    const mensal = App.calcMensalAuto(metaFinal, prazoMeses);
    const id = App.uid();

    const inv = {
      id,
      nome: String(nome || "").trim(),
      metaFinal: Number(metaFinal || 0),
      prazoMeses: Number(prazoMeses || 12),
      mensal,
      diaPagar: Number(diaPagar || 0)
    };

    App.investFixos.push(inv);

    const mesAtual = App.currentMonthKeyFromDate();
    const d = App.getDataMes(mesAtual);
    App.ensureMonthInvestStatus(d);
    d.investStatus[id] = false;

    App.saveAll();
    return inv;
  };

  App.removeInvestFixo = function (idx) {
    if (idx < 0 || idx >= App.investFixos.length) return false;
    const inv = App.investFixos[idx];

    Object.keys(App.state.meses || {}).forEach(m => {
      const d = App.state.meses[m];
      if (d && d.investStatus && inv && inv.id) delete d.investStatus[inv.id];
      if (d && inv && inv.id) App.removeAutoSavedForInv(d, inv.id, m);
    });

    App.investFixos.splice(idx, 1);
    App.saveAll();
    return true;
  };

  /* =========================
     NOTIFICATIONS
  ========================= */
  App.daysUntilDue = function (day, baseDate = new Date()) {
    const y = baseDate.getFullYear();
    const m = baseDate.getMonth();
    const maxDay = new Date(y, m + 1, 0).getDate();
    const dueDay = Math.min(Math.max(Number(day || 1), 1), maxDay);

    const today = new Date(y, m, baseDate.getDate());
    const due = new Date(y, m, dueDay);
    const diffMs = due.getTime() - today.getTime();
    return Math.floor(diffMs / 86400000);
  };

  App.showBrowserNotification = function (title, body) {
    if (!("Notification" in window)) return;
    if (Notification.permission !== "granted") return;

    try {
      new Notification(title, {
        body,
        icon: "icons/icon-192.png",
        badge: "icons/icon-192.png",
        tag: "gastos-pro-prazo"
      });
    } catch (e) {}
  };

  App.requestNotificationPermission = function () {
    if (!("Notification" in window)) {
      alert("Seu navegador não suporta notificações.");
      return;
    }

    Notification.requestPermission().then((perm) => {
      if (perm === "granted") {
        alert("Notificações ativadas com sucesso.");
        localStorage.setItem(App.NOTIF_KEYS.permAsked, "1");
        App.checkDueNotifications();
      } else {
        alert("Notificações não foram permitidas.");
      }
    }).catch(() => {});
  };

  App.checkDueNotifications = function () {
    if (!App.USER_ID) return;
    if (!("Notification" in window)) return;
    if (Notification.permission !== "granted") return;

    const now = new Date();
    const mes = App.currentMonthKeyFromDate(now);
    const d = App.getDataMes(mes);
    App.ensureMonthInvestStatus(d);

    (App.investFixos || []).forEach(inv => {
      if (!inv || !inv.id) return;

      const dueDay = Number(inv.diaPagar || 0);
      if (!dueDay || dueDay < 1) return;

      const paid = !!d.investStatus[inv.id];
      if (paid) return;

      const diff = App.daysUntilDue(dueDay, now);
      if (diff < 0 || diff > 3) return;

      const tag = `faltam_${diff}_dias`;
      const alreadySent = localStorage.getItem(App.kNotifSent(inv.id, mes, tag)) === "1";
      if (alreadySent) return;

      let body = "";
      if (diff === 0) {
        body = `O prazo para pagar ${inv.nome} é hoje (dia ${dueDay}).`;
      } else if (diff === 1) {
        body = `O prazo para pagar ${inv.nome} está chegando. Falta 1 dia (vence dia ${dueDay}).`;
      } else {
        body = `O prazo para pagar ${inv.nome} está chegando. Faltam ${diff} dias (vence dia ${dueDay}).`;
      }

      App.showBrowserNotification("Controle de Gastos PRO", body);
      localStorage.setItem(App.kNotifSent(inv.id, mes, tag), "1");
    });
  };

  App.startDueNotificationWatcher = function () {
    App.checkDueNotifications();
    if (window.__dueNotifInterval) clearInterval(window.__dueNotifInterval);
    window.__dueNotifInterval = setInterval(() => {
      App.checkDueNotifications();
    }, 60 * 60 * 1000);
  };

  /* =========================
     VOICE
  ========================= */
  App.isVoiceEnabled = function () {
    const v = localStorage.getItem(App.kVoiceEnabled());
    if (v == null) return false;
    return v === "1";
  };

  App.setVoiceEnabled = function (on) {
    localStorage.setItem(App.kVoiceEnabled(), on ? "1" : "0");
  };

  App.toggleVoiceEnabled = function () {
    App.setVoiceEnabled(!App.isVoiceEnabled());
    return App.isVoiceEnabled();
  };

  /* =========================
     NETWORK
  ========================= */
  App.postJSON = async function (url, body) {
    const resp = await fetch(url, {
      method: "POST",
      headers: { "Content-Type":"application/json" },
      body: JSON.stringify(body)
    });
    return resp;
  };

  App.syncSalvarMes = function (mes, salary, meta) {
    if (!App.USER_ID) return Promise.resolve(null);
    return App.postJSON(App.API + "/salvarMes", {
      userId: App.USER_ID,
      mes,
      salary,
      meta
    }).catch(() => null);
  };

  App.syncAddGasto = function (mes, desc, amount, category) {
    if (!App.USER_ID) return Promise.resolve(null);
    return App.postJSON(App.API + "/addGasto", {
      userId: App.USER_ID,
      mes,
      desc,
      amount,
      category
    }).catch(() => null);
  };

  App.syncDelGasto = function (mes, desc, amount) {
    if (!App.USER_ID) return Promise.resolve(null);
    return App.postJSON(App.API + "/delGasto", {
      userId: App.USER_ID,
      mes,
      desc,
      amount
    }).catch(() => null);
  };

  App.fetchMes = async function (mes) {
    if (!App.USER_ID) return [];
    try {
      const r = await fetch(App.API + "/mes/" + App.USER_ID + "/" + mes);
      return await r.json();
    } catch (e) {
      return [];
    }
  };

  /* =========================
     AUTH
  ========================= */
  App.initFirebase = async function () {
    if (!window.firebase) return false;

    try {
      if (!firebase.apps || !firebase.apps.length) {
        firebase.initializeApp(App.FIREBASE_CONFIG);
      }
    } catch (e) {}

    App.auth = firebase.auth();

    try {
      await App.auth.setPersistence(firebase.auth.Auth.Persistence.LOCAL);
    } catch (e) {}

    return true;
  };

  App.requireAuth = function (onReady) {
    if (!App.auth) return;

    App.auth.onAuthStateChanged((u) => {
      App.USER = u || null;

      if (!App.USER) {
        App.USER_ID = null;
        location.replace("./login.html");
        return;
      }

      App.USER_ID = App.USER.uid;

      const t = localStorage.getItem("theme");
      if (t === "light") document.body.classList.add("light");
      App.applyThemeSettings();

      App.loadAll();
      if (typeof onReady === "function") onReady(App.USER);
      App.showApp();
    });
  };

  App.loginGoogle = async function () {
    if (!window.firebase || !App.auth) return;
    const provider = new firebase.auth.GoogleAuthProvider();
    provider.setCustomParameters({ prompt:"select_account" });

    try {
      try {
        await App.auth.signInWithPopup(provider);
      } catch (e) {
        await App.auth.signInWithRedirect(provider);
      }
    } catch (e) {
      throw e;
    }
  };

  App.logout = async function () {
    try {
      if (App.auth) await App.auth.signOut();
    } catch (e) {}
    location.replace("./login.html");
  };

  /* =========================
     HISTORY HELPERS
  ========================= */
  App.openMonth = function (monthKey) {
    localStorage.setItem("mes_aberto", monthKey);
    location.href = "./index.html";
  };

  App.deleteMonth = function (monthKey) {
    if (!App.state.meses || !App.state.meses[monthKey]) return false;
    delete App.state.meses[monthKey];
    App.saveAll();
    return true;
  };

  /* =========================
     SIMULATORS
  ========================= */
  App.simularInvestimento = function ({ tipo, valor, meses, extra, aporteMensal }) {
    let taxaMensal = 0;

    if (tipo === "poupanca") taxaMensal = (extra || 0.5) / 100;
    else if (tipo === "cdb") {
      const cdiMensal = 0.009;
      taxaMensal = cdiMensal * ((extra || 100) / 100);
    } else if (tipo === "tesouro") taxaMensal = (extra || 0.9) / 100;
    else taxaMensal = (extra || 1.2) / 100;

    let saldo = Number(valor || 0);
    const aporte = Number(aporteMensal || 0);
    const qtdMeses = Number(meses || 0);

    for (let i = 1; i <= qtdMeses; i++) {
      saldo = saldo * (1 + taxaMensal);
      if (aporte > 0) saldo += aporte;
    }

    const totalInvestido = Number(valor || 0) + (aporte * qtdMeses);
    const lucro = saldo - totalInvestido;

    return {
      taxaMensal,
      totalInvestido,
      saldo,
      lucro
    };
  };

  App.simularEmprestimo = function ({ modelo, valor, parcelas, jurosMensal }) {
    const principal = Number(valor || 0);
    const n = Number(parcelas || 0);
    const juros = Number(jurosMensal || 0) / 100;

    let primeira = 0;
    let totalPago = 0;
    const preview = [];

    if (modelo === "price") {
      const p = juros === 0
        ? (principal / n)
        : (principal * (juros / (1 - Math.pow(1 + juros, -n))));

      primeira = p;
      totalPago = p * n;

      for (let i = 1; i <= Math.min(n, 12); i++) {
        preview.push({ num:i, valor:p });
      }
    } else {
      const amort = principal / n;
      let saldo = principal;

      for (let i = 1; i <= n; i++) {
        const j = saldo * juros;
        const parcela = amort + j;
        if (i === 1) primeira = parcela;
        totalPago += parcela;
        saldo -= amort;

        if (i <= 12) {
          preview.push({ num:i, valor:parcela });
        }
      }
    }

    return { primeira, totalPago, preview };
  };

  /* =========================
     BOOT
  ========================= */
  App.boot = async function (onReady) {
    App.showLoading();
    App.applyThemeSettings();

    const ok = await App.initFirebase();
    if (!ok) return false;

    App.requireAuth(onReady);
    return true;
  };

  /* =========================
     SUPABASE — LEITOR ANDROID
     Conecta ao Supabase para ler e confirmar
     importações detectadas pelo app Android.
  ========================= */
  App.SUPABASE_URL = window.SUPABASE_URL || "";
  App.SUPABASE_ANON_KEY = window.SUPABASE_ANON_KEY || "";
  App._supabase = null;
  App._androidImportsAll = [];
  App._androidImportsFilter = "todos";

  App.getSupabaseClient = function () {
    if (!App._supabase && window.supabase && App.SUPABASE_URL && App.SUPABASE_ANON_KEY) {
      App._supabase = window.supabase.createClient(App.SUPABASE_URL, App.SUPABASE_ANON_KEY);
    }
    return App._supabase;
  };

  App.supabaseGetSession = async function () {
    const sb = App.getSupabaseClient();
    if (!sb) return null;
    const { data } = await sb.auth.getSession();
    return data?.session?.user || null;
  };

  App.loadAndroidImports = async function () {
    const sb = App.getSupabaseClient();
    if (!sb) return [];
    const user = await App.supabaseGetSession();
    if (!user) return [];
    const { data, error } = await sb
      .from("bank_notification_imports")
      .select("*")
      .eq("user_id", user.id)
      .order("notification_time", { ascending: false })
      .limit(100);
    if (error) { console.warn("Importações Android:", error.message); return []; }
    App._androidImportsAll = data || [];
    return App._androidImportsAll;
  };

  App.confirmAndroidImport = async function (imp) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) return false;
    const mes = (imp.notification_time || imp.created_at || "").slice(0, 7) || App.currentMonthKeyFromDate();
    const data = (imp.notification_time || imp.created_at || "").slice(0, 10) || App.todayISO();
    const isEntrada = imp.type === "entrada";
    const tabela = isEntrada ? "income_entries" : "expenses";
    await sb.from(tabela).insert({
      user_id: user.id, mes,
      descricao: imp.description || (isEntrada ? "Receita bancária" : "Gasto bancário"),
      categoria: imp.category || (isEntrada ? "Entrada" : "Outros"),
      valor: imp.amount, data, origem: "notificacao_banco", bank_import_id: imp.id
    });
    const { error } = await sb.from("bank_notification_imports").update({ status: "confirmed" }).eq("id", imp.id);
    return !error;
  };

  App.ignoreAndroidImport = async function (importId) {
    const sb = App.getSupabaseClient();
    if (!sb) return false;
    const { error } = await sb.from("bank_notification_imports").update({ status: "ignored" }).eq("id", importId);
    return !error;
  };

  App.deleteAndroidImport = async function (importId) {
    const sb = App.getSupabaseClient();
    if (!sb) return false;
    const { error } = await sb.from("bank_notification_imports").delete().eq("id", importId);
    return !error;
  };

  App.renderAndroidImports = function (items) {
    const list = document.getElementById("androidImportsList");
    const badge = document.getElementById("androidImportsBadge");
    if (!list) return;
    if (badge) badge.textContent = `${items.length} ite${items.length === 1 ? "m" : "ns"}`;
    if (!items.length) {
      list.innerHTML = `<div class="small" style="text-align:center;padding:24px;opacity:.6">
        Nenhuma importação encontrada. Use o app Android para detectar transações.</div>`;
      return;
    }
    list.innerHTML = items.map(imp => {
      const isEntrada = imp.type === "entrada";
      const valor = Number(imp.amount || 0).toLocaleString("pt-BR", { style:"currency", currency:"BRL" });
      const dataStr = (imp.notification_time || imp.created_at || "").slice(0, 16).replace("T", " ");
      const statusMap = { pending:"⏳ Pendente", confirmed:"✅ Confirmado", ignored:"🚫 Ignorado", auto_confirmed:"⚡ Auto" };
      const isPending = imp.status === "pending";
      const cor = isEntrada ? "var(--good)" : "var(--danger)";
      const pref = isEntrada ? "+" : "-";
      return `<div class="card" style="margin-bottom:10px">
        <div class="row" style="align-items:flex-start;gap:10px">
          <div style="flex:1;min-width:0">
            <div class="small">${App.escapeHtml(imp.bank_name||"Banco")}</div>
            <div style="font-weight:900;margin-top:2px">${App.escapeHtml(imp.description||"—")}</div>
            <div class="small" style="margin-top:4px">${App.escapeHtml(dataStr)}</div>
          </div>
          <div style="font-weight:900;white-space:nowrap;color:${cor}">${pref} ${valor}</div>
        </div>
        <div class="row" style="margin-top:6px;gap:6px;align-items:center;flex-wrap:wrap">
          <span class="badge">${statusMap[imp.status]||imp.status}</span>
          <span class="badge small">${App.escapeHtml(imp.category||"outro")}</span>
          <span class="small">${imp.confidence_score||0}% conf.</span>
        </div>
        ${isPending?`<div class="row" style="margin-top:10px;gap:8px">
          <button class="btn primary mini" style="flex:1" onclick="androidConfirm('${imp.id}')">✅ Lançar</button>
          <button class="btn mini" style="flex:1;color:var(--danger)" onclick="androidIgnore('${imp.id}')">🚫 Ignorar</button>
          <button class="btn mini" onclick="androidDelete('${imp.id}')">🗑️</button>
        </div>`:""}
      </div>`;
    }).join("");
  };

  App.filterAndroidImports = function (filter) {
    App._androidImportsFilter = filter;
    let items = App._androidImportsAll;
    if (filter === "pending") items = items.filter(i => i.status === "pending");
    else if (filter === "confirmed") items = items.filter(i => ["confirmed","auto_confirmed"].includes(i.status));
    else if (filter === "ignored") items = items.filter(i => i.status === "ignored");
    App.renderAndroidImports(items);
  };

  App.updateAndroidReaderStatus = function () {
    const statusEl = document.getElementById("androidReaderStatus");
    const pendingEl = document.getElementById("androidPendingCount");
    const pending = App._androidImportsAll.filter(i => i.status === "pending").length;
    if (statusEl) statusEl.textContent = App._androidImportsAll.length > 0 ? "Conectado ✅" : "Nunca conectado";
    if (pendingEl) pendingEl.textContent = pending > 0 ? `${pending} pendente${pending>1?"s":""}` : "—";
  };

  /* =========================
     SUPABASE — SYNC NUVEM
     Salva e carrega dados do PWA no Supabase
     com fallback para localStorage.
  ========================= */
  App._syncStatus = "idle"; // idle | syncing | ok | error

  App.setSyncStatus = function (status, msg) {
    App._syncStatus = status;
    const el = document.getElementById("syncStatusBar");
    if (!el) return;
    const map = {
      syncing: { text: "☁️ Sincronizando…", color: "var(--accent)" },
      ok:      { text: "✅ Salvo na nuvem", color: "var(--good)" },
      local:   { text: "💾 Salvo localmente", color: "var(--text-muted)" },
      error:   { text: "⚠️ Erro ao sincronizar", color: "var(--danger)" },
      idle:    { text: "", color: "transparent" }
    };
    const s = map[status] || map.idle;
    el.textContent = s.text;
    el.style.color = s.color;
    el.style.display = s.text ? "block" : "none";
    if (msg) console.warn("[Sync]", msg);
  };

  App.syncSaveMonthlySettings = async function (mes, salary, meta) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) { App.setSyncStatus("local"); return false; }
    App.setSyncStatus("syncing");
    const { error } = await sb.from("monthly_settings").upsert(
      { user_id: user.id, mes, salary, meta },
      { onConflict: "user_id,mes" }
    );
    if (error) { App.setSyncStatus("error", error.message); return false; }
    App.setSyncStatus("ok");
    return true;
  };

  App.syncLoadMonthlySettings = async function (mes) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) return null;
    const { data, error } = await sb
      .from("monthly_settings")
      .select("salary,meta")
      .eq("user_id", user.id)
      .eq("mes", mes)
      .maybeSingle();
    if (error) { console.warn("[Sync] monthly_settings:", error.message); return null; }
    return data;
  };

  App.syncSaveExpenses = async function (mes, expenses) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) { App.setSyncStatus("local"); return false; }
    App.setSyncStatus("syncing");
    await sb.from("expenses").delete().eq("user_id", user.id).eq("mes", mes).eq("origem", "manual");
    if (expenses.length === 0) { App.setSyncStatus("ok"); return true; }
    const rows = expenses.map(e => ({
      user_id: user.id, mes,
      descricao: e.desc || e.descricao || "",
      categoria: e.cat || e.categoria || "Outros",
      valor: Number(e.valor) || 0,
      data: e.data || null,
      paid: !!e.paid,
      is_fixo: !!e.fixo,
      origem: "manual"
    })).filter(r => r.valor > 0);
    const { error } = await sb.from("expenses").insert(rows);
    if (error) { App.setSyncStatus("error", error.message); return false; }
    App.setSyncStatus("ok");
    return true;
  };

  App.syncSaveCategories = async function (categorias) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) return false;
    await sb.from("categories").delete().eq("user_id", user.id);
    if (!categorias.length) return true;
    const rows = categorias.map((nome, i) => ({ user_id: user.id, nome, ordem: i }));
    const { error } = await sb.from("categories").insert(rows);
    if (error) { console.warn("[Sync] categories:", error.message); return false; }
    return true;
  };

  App.syncLoadCategories = async function () {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) return null;
    const { data, error } = await sb
      .from("categories")
      .select("nome")
      .eq("user_id", user.id)
      .order("ordem");
    if (error) { console.warn("[Sync] categories:", error.message); return null; }
    return data ? data.map(r => r.nome) : null;
  };

  App.syncSaveFixedExpenses = async function (fixos) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) return false;
    await sb.from("fixed_expenses").delete().eq("user_id", user.id);
    if (!fixos.length) return true;
    const rows = fixos.map((f, i) => ({
      user_id: user.id,
      descricao: f.desc || f.descricao || "",
      categoria: f.cat || f.categoria || "Outros",
      valor: Number(f.valor) || 0,
      ordem: i,
      ativo: f.ativo !== false
    })).filter(r => r.valor > 0);
    const { error } = await sb.from("fixed_expenses").insert(rows);
    if (error) { console.warn("[Sync] fixed_expenses:", error.message); return false; }
    return true;
  };

  App.syncSaveFixedInvestments = async function (investimentos) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) return false;
    await sb.from("fixed_investments").delete().eq("user_id", user.id);
    if (!investimentos.length) return true;
    const rows = investimentos.map(inv => ({
      user_id: user.id,
      nome: inv.nome || "",
      meta_final: Number(inv.metaFinal || inv.meta_final) || 0,
      prazo_meses: Number(inv.prazo || inv.prazo_meses) || 12,
      mensal: Number(inv.mensal) || 0,
      dia_pagar: Number(inv.diaPagar || inv.dia_pagar) || 0,
      ativo: inv.ativo !== false
    })).filter(r => r.nome);
    const { error } = await sb.from("fixed_investments").insert(rows);
    if (error) { console.warn("[Sync] fixed_investments:", error.message); return false; }
    return true;
  };

  App.syncSaveHistory = async function (mes, snapshot) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) return false;
    const row = {
      user_id: user.id, mes,
      salary:       Number(snapshot.salary || snapshot.salario) || 0,
      meta:         Number(snapshot.meta) || 0,
      total_gastos: Number(snapshot.totalGastos || snapshot.total_gastos) || 0,
      total_saved:  Number(snapshot.totalSaved || snapshot.total_saved) || 0,
      saldo:        Number(snapshot.saldo) || 0,
      snapshot
    };
    const { error } = await sb.from("history").upsert(row, { onConflict: "user_id,mes" });
    if (error) { console.warn("[Sync] history:", error.message); return false; }
    return true;
  };

  App.syncLoadHistory = async function () {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) return [];
    const { data, error } = await sb
      .from("history")
      .select("mes,salary,meta,total_gastos,total_saved,saldo,snapshot")
      .eq("user_id", user.id)
      .order("mes", { ascending: false })
      .limit(24);
    if (error) { console.warn("[Sync] history:", error.message); return []; }
    return data || [];
  };

  App.syncAll = async function (dadosMes) {
    const sb = App.getSupabaseClient();
    const user = await App.supabaseGetSession();
    if (!sb || !user) { App.setSyncStatus("local"); return false; }
    App.setSyncStatus("syncing");
    try {
      const { mes, salary, meta, expenses, categorias, fixos, investimentos } = dadosMes;
      await Promise.all([
        App.syncSaveMonthlySettings(mes, salary, meta),
        App.syncSaveExpenses(mes, expenses || []),
        categorias ? App.syncSaveCategories(categorias) : Promise.resolve(),
        fixos ? App.syncSaveFixedExpenses(fixos) : Promise.resolve(),
        investimentos ? App.syncSaveFixedInvestments(investimentos) : Promise.resolve()
      ]);
      App.setSyncStatus("ok");
      return true;
    } catch (e) {
      App.setSyncStatus("error", e.message);
      return false;
    }
  };

})();