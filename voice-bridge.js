(function () {
  const VOICE_CMD_KEY = "voice_cmd";
  const VOICE_ACK_KEY = "voice_cmd_ack";
  const VOICE_LAST_PAGE_KEY = "voice_last_page";
  const VOICE_RESULT_KEY = "voice_result";
  const VOICE_QUEUE_KEY = "voice_queue";
  const VOICE_ENABLED_KEY_PREFIX = "voice_enabled_v1_";

  const pageName = (location.pathname.split("/").pop() || "index.html").toLowerCase();

  function nowISO() {
    return new Date().toISOString();
  }

  function safeParse(json, fallback) {
    try {
      const parsed = JSON.parse(json);
      return parsed == null ? fallback : parsed;
    } catch (e) {
      return fallback;
    }
  }

  function normalize(text) {
    return String(text || "")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase()
      .trim();
  }

  function setResult(ok, message, extra) {
    const payload = {
      ok: !!ok,
      page: pageName,
      message: String(message || ""),
      time: nowISO(),
      ...(extra || {})
    };
    localStorage.setItem(VOICE_RESULT_KEY, JSON.stringify(payload));
    localStorage.setItem(VOICE_ACK_KEY, JSON.stringify(payload));
  }

  function getCurrentUserId() {
    try {
      if (window.App && window.App.USER_ID) return String(window.App.USER_ID);
      if (window.USER_ID) return String(window.USER_ID);
    } catch (e) {}
    return "guest";
  }

  function isVoiceEnabled() {
    const key = VOICE_ENABLED_KEY_PREFIX + getCurrentUserId();
    const v = localStorage.getItem(key);
    if (v == null) return true;
    return v === "1";
  }

  function clickFirst(selectors) {
    for (const sel of selectors) {
      const el = document.querySelector(sel);
      if (el) {
        el.click();
        return true;
      }
    }
    return false;
  }

  function focusFirst(selectors) {
    for (const sel of selectors) {
      const el = document.querySelector(sel);
      if (el) {
        el.focus();
        return el;
      }
    }
    return null;
  }

  function setValue(selectors, value, trigger = true) {
    for (const sel of selectors) {
      const el = document.querySelector(sel);
      if (el) {
        el.value = value;
        if (trigger) {
          el.dispatchEvent(new Event("input", { bubbles: true }));
          el.dispatchEvent(new Event("change", { bubbles: true }));
        }
        return true;
      }
    }
    return false;
  }

  function openPage(url) {
    localStorage.setItem(VOICE_LAST_PAGE_KEY, pageName);
    location.href = url;
  }

  function monthNameToNumber(text) {
    const t = normalize(text);
    const map = {
      janeiro: "01",
      fevereiro: "02",
      marco: "03",
      "março": "03",
      abril: "04",
      maio: "05",
      junho: "06",
      julho: "07",
      agosto: "08",
      setembro: "09",
      outubro: "10",
      novembro: "11",
      dezembro: "12"
    };
    return map[t] || null;
  }

  function parseMoney(text) {
    if (text == null) return 0;
    const s = String(text)
      .replace(/[^\d,.-]/g, "")
      .replace(/\.(?=\d{3}\b)/g, "")
      .replace(",", ".");
    const n = Number(s);
    return Number.isFinite(n) ? n : 0;
  }

  function tryCall(fnName, ...args) {
    try {
      const fn = window[fnName];
      if (typeof fn === "function") {
        return fn(...args);
      }
    } catch (e) {}
    return undefined;
  }

  function enqueueIfNeeded(raw) {
    let queue = safeParse(localStorage.getItem(VOICE_QUEUE_KEY), []);

    if (!Array.isArray(queue)) {
      queue = [];
    }

    queue.push({
      cmd: raw,
      page: pageName,
      time: nowISO()
    });

    localStorage.setItem(VOICE_QUEUE_KEY, JSON.stringify(queue.slice(-30)));
  }

  function clearCommand() {
    localStorage.removeItem(VOICE_CMD_KEY);
  }

  function markHandled(message, extra) {
    setResult(true, message || "Comando executado.", extra);
    clearCommand();
  }

  function markIgnored(message, extra) {
    setResult(false, message || "Comando ignorado.", extra);
    clearCommand();
  }

  function goHomeMonthIfExists(monthKey) {
    if (!monthKey) return false;
    localStorage.setItem("mes_aberto", monthKey);
    openPage("./index.html");
    return true;
  }

  function parseMonthReference(text) {
    const norm = normalize(text);

    const yyyyMm = norm.match(/\b(20\d{2})[-\/](0[1-9]|1[0-2])\b/);
    if (yyyyMm) return `${yyyyMm[1]}-${yyyyMm[2]}`;

    const monthYear = norm.match(/\b(janeiro|fevereiro|marco|março|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)\s+(de\s+)?(20\d{2})\b/);
    if (monthYear) {
      const mm = monthNameToNumber(monthYear[1]);
      return mm ? `${monthYear[3]}-${mm}` : null;
    }

    return null;
  }

  function handleGlobalRoute(cmd) {
    if (cmd.includes("abrir gastos fixos") || cmd === "gastos fixos" || cmd.includes("ir para gastos fixos")) {
      openPage("./fixos.html");
      return "Abrindo gastos fixos.";
    }
    if (cmd.includes("abrir investimentos fixos") || cmd.includes("abrir investimento fixo") || cmd.includes("ir para investimentos fixos")) {
      openPage("./investfixos.html");
      return "Abrindo investimentos fixos.";
    }
    if (cmd.includes("abrir categorias") || cmd.includes("abrir minhas categorias") || cmd.includes("ir para categorias")) {
      openPage("./categorias.html");
      return "Abrindo categorias.";
    }
    if (cmd.includes("abrir historico") || cmd.includes("abrir histórico") || cmd.includes("ir para historico") || cmd.includes("ir para histórico")) {
      openPage("./historico.html");
      return "Abrindo histórico.";
    }
    if (cmd.includes("abrir simuladores") || cmd.includes("abrir simulador") || cmd.includes("ir para simuladores")) {
      openPage("./simuladores.html");
      return "Abrindo simuladores.";
    }
    if (cmd.includes("abrir ia") || cmd.includes("abrir assistente") || cmd.includes("abrir inteligencia artificial")) {
      openPage("./ia.html");
      return "Abrindo IA.";
    }
    if (cmd.includes("abrir voz") || cmd.includes("abrir comando de voz")) {
      openPage("./voz.html");
      return "Abrindo voz.";
    }
    if (cmd.includes("abrir inicio") || cmd.includes("abrir início") || cmd.includes("abrir tela principal") || cmd.includes("ir para home") || cmd === "home") {
      openPage("./index.html");
      return "Abrindo tela principal.";
    }
    if (cmd.startsWith("abrir mes ") || cmd.startsWith("abrir mês ")) {
      const ref = parseMonthReference(cmd);
      if (ref) {
        goHomeMonthIfExists(ref);
        return `Abrindo o mês ${ref}.`;
      }
    }
    return null;
  }

  function handleThemeCommands(cmd) {
    if (cmd.includes("tema claro")) {
      if (!document.body.classList.contains("light")) {
        tryCall("toggleTheme");
      }
      return "Tema claro aplicado.";
    }
    if (cmd.includes("tema escuro") || cmd.includes("tema normal")) {
      if (document.body.classList.contains("light")) {
        tryCall("toggleTheme");
      }
      return "Tema escuro aplicado.";
    }

    const colorMap = [
      { keys: ["tema roxo", "cor roxa", "tema purple"], value: "purple", label: "Tema roxo aplicado." },
      { keys: ["tema verde", "cor verde"], value: "green", label: "Tema verde aplicado." },
      { keys: ["tema por do sol", "tema pôr do sol", "tema sunset"], value: "sunset", label: "Tema pôr do sol aplicado." },
      { keys: ["tema minimal", "tema mono", "tema preto e branco"], value: "mono", label: "Tema minimal aplicado." },
      { keys: ["tema padrao", "tema padrão", "voltar tema", "resetar tema"], value: "default", label: "Tema padrão aplicado." }
    ];

    for (const item of colorMap) {
      if (item.keys.some(k => cmd.includes(k))) {
        localStorage.setItem("theme_preset_v1", item.value);
        if (typeof window.applyThemeSettings === "function") window.applyThemeSettings();
        if (window.App && typeof window.App.applyThemeSettings === "function") window.App.applyThemeSettings();
        return item.label;
      }
    }

    return null;
  }

  function handleIndexPage(cmd) {
    if (pageName !== "index.html" && pageName !== "") return null;

    const route = handleGlobalRoute(cmd);
    if (route) return route;

    const theme = handleThemeCommands(cmd);
    if (theme) return theme;

    if (cmd.includes("abrir menu")) {
      if (typeof window.openMenu === "function") {
        window.openMenu();
        return "Abrindo menu.";
      }
      if (clickFirst([".menuBtn"])) return "Abrindo menu.";
    }

    if (cmd.includes("abrir historico") || cmd.includes("abrir histórico")) {
      if (typeof window.openHistory === "function") {
        window.openHistory();
        return "Abrindo histórico.";
      }
      if (typeof window.openHistoryPage === "function") {
        window.openHistoryPage();
        return "Abrindo histórico.";
      }
    }

    if (cmd.includes("abrir configuracoes") || cmd.includes("abrir configurações")) {
      if (typeof window.openScreen === "function") {
        window.openScreen("screen-config");
        return "Abrindo configurações.";
      }
    }

    if (cmd.includes("abrir temas")) {
      if (typeof window.openScreen === "function") {
        window.openScreen("screen-themes");
        return "Abrindo temas.";
      }
    }

    if (cmd.includes("abrir pdf") || cmd.includes("abrir imprimir pdf")) {
      if (typeof window.openPdfScreen === "function") {
        window.openPdfScreen();
        return "Abrindo PDF.";
      }
    }

    if (cmd.includes("abrir painel") || cmd.includes("abrir edicao do painel") || cmd.includes("abrir edição do painel")) {
      if (typeof window.openHomeEditor === "function") {
        window.openHomeEditor();
        return "Abrindo edição do painel.";
      }
    }

    if (cmd.includes("abrir gasto") || cmd.includes("adicionar gasto")) {
      if (typeof window.openAddExpense === "function") {
        window.openAddExpense();
        return "Abrindo adicionar gasto.";
      }
    }

    if (cmd.includes("abrir voz")) {
      if (typeof window.openVoicePage === "function") {
        window.openVoicePage();
        return "Abrindo tela de voz.";
      }
    }

    if (cmd.includes("abrir ia")) {
      if (typeof window.openIa === "function") {
        window.openIa();
        return "Abrindo IA.";
      }
      if (typeof window.openScreen === "function") {
        window.openScreen("screen-ia");
        return "Abrindo IA.";
      }
    }

    if (cmd.includes("salvar mes") || cmd.includes("salvar mês")) {
      if (typeof window.saveSettings === "function") {
        window.saveSettings();
        return "Salvando mês.";
      }
    }

    if (cmd.includes("resumo do ano")) {
      const btn = [...document.querySelectorAll("button")].find(b => normalize(b.textContent).includes("resumo do ano"));
      if (btn) {
        btn.click();
        return "Gerando resumo do ano.";
      }
    }

    if (cmd.includes("relatorio completo") || cmd.includes("relatório completo")) {
      const btn = [...document.querySelectorAll("button")].find(b => normalize(b.textContent).includes("completo"));
      if (btn) {
        btn.click();
        return "Gerando relatório completo.";
      }
    }

    if (cmd.startsWith("salario ") || cmd.startsWith("salário ")) {
      const value = parseMoney(cmd.replace("salario ", "").replace("salário ", ""));
      if (setValue(["#salary"], value)) return "Salário preenchido.";
    }

    if (cmd.startsWith("meta ")) {
      const value = parseMoney(cmd.replace("meta ", ""));
      if (setValue(["#meta"], value)) return "Meta preenchida.";
    }

    if (cmd.startsWith("guardado ")) {
      const value = parseMoney(cmd.replace("guardado ", ""));
      if (setValue(["#savedValue"], value)) return "Guardado preenchido.";
    }

    if (cmd.startsWith("descricao guardado ") || cmd.startsWith("descrição guardado ")) {
      const text = cmd.replace("descricao guardado ", "").replace("descrição guardado ", "");
      if (setValue(["#savedDesc"], text)) return "Descrição do guardado preenchida.";
    }

    if (cmd.includes("adicionar guardado")) {
      if (typeof window.addSaved === "function") {
        window.addSaved();
        return "Adicionando guardado.";
      }
    }

    if (cmd.startsWith("descricao gasto ") || cmd.startsWith("descrição gasto ")) {
      const text = cmd.replace("descricao gasto ", "").replace("descrição gasto ", "");
      if (setValue(["#mDesc"], text)) return "Descrição do gasto preenchida.";
    }

    if (cmd.startsWith("valor gasto ")) {
      const value = parseMoney(cmd.replace("valor gasto ", ""));
      if (setValue(["#mValor"], value)) return "Valor do gasto preenchido.";
    }

    if (cmd.startsWith("categoria gasto ")) {
      const text = cmd.replace("categoria gasto ", "").trim();
      if (setValue(["#mCategoria"], text)) return "Categoria do gasto preenchida.";
    }

    if (cmd.includes("confirmar gasto") || cmd.includes("salvar gasto")) {
      if (typeof window.confirmAddExpense === "function") {
        window.confirmAddExpense();
        return "Salvando gasto.";
      }
    }

    if (cmd.includes("fechar modal") || cmd.includes("cancelar gasto")) {
      const ok = clickFirst(['[onclick*="closeModal"]', '.closeX']);
      if (ok) return "Fechando janela.";
    }

    return null;
  }

  function handleFixosPage(cmd) {
    if (pageName !== "fixos.html") return null;

    const route = handleGlobalRoute(cmd);
    if (route) return route;

    const theme = handleThemeCommands(cmd);
    if (theme) return theme;

    if (cmd.startsWith("descricao ")) {
      const text = cmd.replace("descricao ", "");
      if (setValue(["#descFixo"], text)) return "Descrição preenchida.";
    }

    if (cmd.startsWith("valor ")) {
      const value = parseMoney(cmd.replace("valor ", ""));
      if (setValue(["#valorFixo"], value)) return "Valor preenchido.";
    }

    if (cmd.startsWith("categoria ")) {
      const text = cmd.replace("categoria ", "");
      if (setValue(["#categoriaFixo"], text)) return "Categoria preenchida.";
    }

    if (cmd.includes("adicionar gasto fixo") || cmd.includes("salvar gasto fixo")) {
      if (typeof window.addFixo === "function") {
        window.addFixo();
        return "Adicionando gasto fixo.";
      }
    }

    if (cmd.includes("limpar formulario") || cmd.includes("limpar formulário")) {
      if (typeof window.clearForm === "function") {
        window.clearForm();
        return "Formulário limpo.";
      }
    }

    return null;
  }

  function handleInvestFixosPage(cmd) {
    if (pageName !== "investfixos.html") return null;

    const route = handleGlobalRoute(cmd);
    if (route) return route;

    const theme = handleThemeCommands(cmd);
    if (theme) return theme;

    if (cmd.startsWith("nome ")) {
      const text = cmd.replace("nome ", "");
      if (setValue(["#invNome"], text)) return "Nome preenchido.";
    }

    if (cmd.startsWith("meta final ")) {
      const value = parseMoney(cmd.replace("meta final ", ""));
      if (setValue(["#invMetaFinal"], value)) return "Meta final preenchida.";
    }

    if (cmd.startsWith("prazo ")) {
      const value = parseMoney(cmd.replace("prazo ", ""));
      if (setValue(["#invPrazoMeses"], value)) return "Prazo preenchido.";
    }

    if (cmd.startsWith("dia pagar ") || cmd.startsWith("dia de pagar ")) {
      const value = parseMoney(cmd.replace("dia pagar ", "").replace("dia de pagar ", ""));
      if (setValue(["#invDiaPagar"], value)) return "Dia de pagamento preenchido.";
    }

    if (cmd.includes("ativar notificacoes") || cmd.includes("ativar notificações")) {
      if (typeof window.requestNotificationPermission === "function") {
        window.requestNotificationPermission();
        return "Ativando notificações.";
      }
    }

    if (cmd.includes("adicionar investimento fixo") || cmd.includes("salvar investimento fixo")) {
      if (typeof window.addInvestFixo === "function") {
        window.addInvestFixo();
        return "Adicionando investimento fixo.";
      }
    }

    if (cmd.includes("limpar formulario") || cmd.includes("limpar formulário")) {
      if (typeof window.clearForm === "function") {
        window.clearForm();
        return "Formulário limpo.";
      }
    }

    return null;
  }

  function handleCategoriasPage(cmd) {
    if (pageName !== "categorias.html") return null;

    const route = handleGlobalRoute(cmd);
    if (route) return route;

    const theme = handleThemeCommands(cmd);
    if (theme) return theme;

    if (cmd.startsWith("nome categoria ") || cmd.startsWith("categoria ")) {
      const text = cmd.replace("nome categoria ", "").replace("categoria ", "");
      if (setValue(["#novaCategoria"], text)) return "Categoria preenchida.";
    }

    if (cmd.includes("adicionar categoria") || cmd.includes("salvar categoria")) {
      if (typeof window.addCategoria === "function") {
        window.addCategoria();
        return "Adicionando categoria.";
      }
    }

    if (cmd.includes("limpar formulario") || cmd.includes("limpar formulário")) {
      if (typeof window.clearForm === "function") {
        window.clearForm();
        return "Formulário limpo.";
      }
    }

    return null;
  }

  function handleHistoricoPage(cmd) {
    if (pageName !== "historico.html") return null;

    const route = handleGlobalRoute(cmd);
    if (route) return route;

    const theme = handleThemeCommands(cmd);
    if (theme) return theme;

    if (cmd.startsWith("buscar ")) {
      const text = cmd.replace("buscar ", "");
      if (setValue(["#busca"], text)) {
        tryCall("renderHistory");
        return "Busca preenchida.";
      }
    }

    if (cmd.includes("ordenar por gasto")) {
      if (setValue(["#sortMode"], "highest_spent")) {
        tryCall("renderHistory");
        return "Ordenando por maior gasto.";
      }
    }

    if (cmd.includes("ordenar por guardado")) {
      if (setValue(["#sortMode"], "highest_saved")) {
        tryCall("renderHistory");
        return "Ordenando por maior guardado.";
      }
    }

    if (cmd.includes("ordenar mais recente")) {
      if (setValue(["#sortMode"], "recent")) {
        tryCall("renderHistory");
        return "Ordenando por mais recente.";
      }
    }

    if (cmd.includes("ordenar mais antigo")) {
      if (setValue(["#sortMode"], "oldest")) {
        tryCall("renderHistory");
        return "Ordenando por mais antigo.";
      }
    }

    if (cmd.startsWith("abrir mes ") || cmd.startsWith("abrir mês ")) {
      const ref = parseMonthReference(cmd);
      if (ref) {
        if (typeof window.openMonth === "function") {
          window.openMonth(ref);
          return `Abrindo o mês ${ref}.`;
        }
      }
    }

    return null;
  }

  function handleSimuladoresPage(cmd) {
    if (pageName !== "simuladores.html") return null;

    const route = handleGlobalRoute(cmd);
    if (route) return route;

    const theme = handleThemeCommands(cmd);
    if (theme) return theme;

    if (cmd.includes("abrir investimento")) {
      if (setValue(["#tipoSimulacao"], "investimento")) {
        tryCall("toggleSimulator");
        return "Abrindo simulador de investimento.";
      }
    }

    if (cmd.includes("abrir emprestimo") || cmd.includes("abrir empréstimo") || cmd.includes("abrir compra")) {
      if (setValue(["#tipoSimulacao"], "emprestimo")) {
        tryCall("toggleSimulator");
        return "Abrindo simulador de empréstimo.";
      }
    }

    if (cmd.startsWith("valor inicial ")) {
      const value = parseMoney(cmd.replace("valor inicial ", ""));
      if (setValue(["#valorInvestimento"], value)) return "Valor inicial preenchido.";
    }

    if (cmd.startsWith("meses ")) {
      const value = parseMoney(cmd.replace("meses ", ""));
      if (setValue(["#mesesInvestimento"], value)) return "Meses preenchidos.";
    }

    if (cmd.startsWith("aporte ")) {
      const value = parseMoney(cmd.replace("aporte ", ""));
      if (setValue(["#aporteMensal"], value)) return "Aporte preenchido.";
    }

    if (cmd.startsWith("tipo investimento ")) {
      const raw = normalize(cmd.replace("tipo investimento ", ""));
      const map = {
        poupanca: "poupanca",
        "poupança": "poupanca",
        cdb: "cdb",
        tesouro: "tesouro",
        fixo: "fixo"
      };
      if (map[raw] && setValue(["#investTipo"], map[raw])) {
        tryCall("updateInvestUI");
        return "Tipo de investimento selecionado.";
      }
    }

    if (cmd.startsWith("parametro ") || cmd.startsWith("parâmetro ")) {
      const value = parseMoney(cmd.replace("parametro ", "").replace("parâmetro ", ""));
      if (setValue(["#investExtraValue"], value)) return "Parâmetro preenchido.";
    }

    if (cmd.includes("calcular investimento")) {
      if (typeof window.simularInvestimento === "function") {
        window.simularInvestimento();
        return "Calculando investimento.";
      }
    }

    if (cmd.includes("limpar investimento")) {
      if (typeof window.clearInvestimento === "function") {
        window.clearInvestimento();
        return "Simulação de investimento limpa.";
      }
    }

    if (cmd.startsWith("valor emprestimo ") || cmd.startsWith("valor empréstimo ")) {
      const value = parseMoney(cmd.replace("valor emprestimo ", "").replace("valor empréstimo ", ""));
      if (setValue(["#loanValor"], value)) return "Valor do empréstimo preenchido.";
    }

    if (cmd.startsWith("parcelas ")) {
      const value = parseMoney(cmd.replace("parcelas ", ""));
      if (setValue(["#loanParcelas"], value)) return "Parcelas preenchidas.";
    }

    if (cmd.startsWith("juros ")) {
      const value = parseMoney(cmd.replace("juros ", ""));
      if (setValue(["#loanJuros"], value)) return "Juros preenchido.";
    }

    if (cmd.startsWith("modelo ")) {
      const raw = normalize(cmd.replace("modelo ", ""));
      const model = raw.includes("sac") ? "sac" : "price";
      if (setValue(["#loanTipo"], model)) return "Modelo preenchido.";
    }

    if (cmd.includes("calcular emprestimo") || cmd.includes("calcular empréstimo")) {
      if (typeof window.simularEmprestimo === "function") {
        window.simularEmprestimo();
        return "Calculando empréstimo.";
      }
    }

    if (cmd.includes("limpar emprestimo") || cmd.includes("limpar empréstimo")) {
      if (typeof window.clearEmprestimo === "function") {
        window.clearEmprestimo();
        return "Simulação de empréstimo limpa.";
      }
    }

    return null;
  }

  function handleIAPage(cmd) {
    if (pageName !== "ia.html") return null;

    const route = handleGlobalRoute(cmd);
    if (route) return route;

    const theme = handleThemeCommands(cmd);
    if (theme) return theme;

    if (cmd.startsWith("pergunta ")) {
      const text = cmd.replace("pergunta ", "");
      if (setValue(["#iaPrompt"], text)) return "Pergunta preenchida.";
    }

    if (cmd.includes("enviar ia") || cmd.includes("perguntar ia")) {
      if (typeof window.iaSend === "function") {
        window.iaSend();
        return "Enviando para IA.";
      }
    }

    return null;
  }

  function handleVoicePage(cmd) {
    if (pageName !== "voz.html") return null;

    const route = handleGlobalRoute(cmd);
    if (route) return route;

    const theme = handleThemeCommands(cmd);
    if (theme) return theme;

    return null;
  }

  function handleAnyPage(cmd) {
    return (
      handleIndexPage(cmd) ||
      handleFixosPage(cmd) ||
      handleInvestFixosPage(cmd) ||
      handleCategoriasPage(cmd) ||
      handleHistoricoPage(cmd) ||
      handleSimuladoresPage(cmd) ||
      handleIAPage(cmd) ||
      handleVoicePage(cmd)
    );
  }

  function run(raw) {
    if (!isVoiceEnabled()) {
      markIgnored("Sistema de voz está bloqueado.", { blocked: true });
      return;
    }

    const original = String(raw || "").trim();
    const cmd = normalize(original);

    if (!cmd) {
      markIgnored("Comando vazio.");
      return;
    }

    enqueueIfNeeded(original);

    const result = handleAnyPage(cmd);

    if (result) {
      markHandled(result, { command: original });
      return;
    }

    markIgnored("Comando não reconhecido nesta tela.", { command: original });
  }

  function boot() {
    window.addEventListener("storage", (e) => {
      if (e.key === VOICE_CMD_KEY && e.newValue) run(e.newValue);
    });

    const first = localStorage.getItem(VOICE_CMD_KEY);
    if (first) run(first);
  }

  window.VoiceBridge = {
    run,
    setResult,
    normalize
  };

  boot();
})();
