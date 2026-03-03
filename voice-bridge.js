(function () {
  "use strict";

  const KEYS = {
    cmd: "voice_cmd",
    last: "voice_last",
    ts: "voice_ts",
    ack: "voice_cmd_ack"
  };

  const pageName = (location.pathname.split("/").pop() || "index.html").toLowerCase();

  function nowISO() {
    return new Date().toISOString();
  }

  function normalize(text) {
    return String(text || "")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase()
      .trim();
  }

  function safeParse(json, fallback) {
    try {
      const parsed = JSON.parse(json);
      return parsed == null ? fallback : parsed;
    } catch (e) {
      return fallback;
    }
  }

  function setAck(ok, message, extra) {
    const payload = {
      ok: !!ok,
      page: pageName,
      message: String(message || ""),
      time: nowISO(),
      ...(extra || {})
    };
    localStorage.setItem(KEYS.ack, JSON.stringify(payload));
    return payload;
  }

  function clearCommand() {
    localStorage.removeItem(KEYS.cmd);
  }

  function readCommand() {
    return (localStorage.getItem(KEYS.cmd) || "").trim();
  }

  function readLastCommand() {
    return (localStorage.getItem(KEYS.last) || "").trim();
  }

  function saveCommand(raw) {
    const text = String(raw || "").trim();
    if (!text) return false;
    localStorage.setItem(KEYS.cmd, text);
    localStorage.setItem(KEYS.last, text);
    localStorage.setItem(KEYS.ts, String(Date.now()));
    return true;
  }

  function route(raw, url) {
    if (!saveCommand(raw)) return false;
    location.href = url;
    return true;
  }

  function openIndex(raw) {
    return route(raw, "./index.html");
  }

  function openFixos(raw) {
    return route(raw, "./fixos.html");
  }

  function openInvestFixos(raw) {
    return route(raw, "./investfixos.html");
  }

  function openCategorias(raw) {
    return route(raw, "./categorias.html");
  }

  function openHistorico(raw) {
    return route(raw, "./historico.html");
  }

  function openSimuladores(raw) {
    return route(raw, "./simuladores.html");
  }

  function openVoice(raw) {
    return route(raw, "./voz.html");
  }

  function withHash(url, hash) {
    const base = String(url || "").replace(/#.*$/, "");
    return hash ? `${base}#${hash}` : base;
  }

  function executeHandler(raw, handler, options) {
    if (!raw) return false;

    try {
      const result = handler(raw);

      if (result === true) {
        if ((options && options.autoAck) !== false) {
          setAck(true, "Comando executado.", { command: raw });
        }
        if ((options && options.autoClear) !== false) {
          clearCommand();
        }
        return true;
      }

      if (typeof result === "string") {
        if ((options && options.autoAck) !== false) {
          setAck(true, result, { command: raw });
        }
        if ((options && options.autoClear) !== false) {
          clearCommand();
        }
        return true;
      }

      if (result && typeof result === "object") {
        const ok = result.ok !== false;
        if ((options && options.autoAck) !== false) {
          setAck(ok, result.message || (ok ? "Comando executado." : "Comando não executado."), {
            command: raw,
            ...(result.extra || {})
          });
        }
        if ((options && options.autoClear) !== false) {
          clearCommand();
        }
        return ok;
      }

      return false;
    } catch (err) {
      if ((options && options.autoAck) !== false) {
        setAck(false, err && err.message ? err.message : "Erro ao executar comando.", {
          command: raw
        });
      }
      if ((options && options.autoClear) !== false) {
        clearCommand();
      }
      return false;
    }
  }

  function bind(handler, options) {
    if (typeof handler !== "function") return;

    const opts = {
      handleInitial: true,
      autoClear: true,
      autoAck: true,
      ...options
    };

    window.addEventListener("storage", (e) => {
      if (e.key === KEYS.cmd && e.newValue) {
        executeHandler(String(e.newValue || "").trim(), handler, opts);
      }
    });

    if (opts.handleInitial) {
      const pending = readCommand();
      if (pending) {
        executeHandler(pending, handler, opts);
      }
    }
  }

  function bindGlobalHandler(options) {
    const globalHandler =
      typeof window.handleVoiceCommand === "function"
        ? window.handleVoiceCommand
        : null;

    if (globalHandler) {
      bind(globalHandler, options);
    }
  }

  window.VoiceBridge = {
    KEYS,
    pageName,
    normalize,
    safeParse,
    setAck,
    clearCommand,
    readCommand,
    readLastCommand,
    saveCommand,
    route,
    openIndex,
    openFixos,
    openInvestFixos,
    openCategorias,
    openHistorico,
    openSimuladores,
    openVoice,
    withHash,
    bind,
    bindGlobalHandler
  };

  if (typeof window.handleVoiceCommand === "function") {
    bindGlobalHandler();
  }
})();