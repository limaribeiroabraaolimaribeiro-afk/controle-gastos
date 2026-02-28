const CACHE_NAME = "gastospro-cache-v1";
const APP_SHELL = [
  "./",
  "./index.html",
  "./manifest.json",
  "./config.js",
  "./icons/icon-192.png",
  "./icons/icon-512.png"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)).catch(() => {})
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) return caches.delete(key);
        })
      )
    )
  );
  self.clients.claim();
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  event.respondWith(
    caches.match(req).then((cached) => {
      if (cached) return cached;

      return fetch(req)
        .then((res) => {
          const copy = res.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(req, copy).catch(() => {});
          });
          return res;
        })
        .catch(() => caches.match("./index.html"));
    })
  );
});

self.addEventListener("message", (event) => {
  const data = event.data || {};

  if (data.type === "STORE_LAST_NOTIFICATION") {
    self.__lastNotification = data.payload || null;
    return;
  }

  if (data.type === "SHOW_NOTIFICATION") {
    const payload = data.payload || {};
    event.waitUntil(
      self.registration.showNotification(
        payload.title || "Controle de Gastos PRO",
        {
          body: payload.body || "Você tem um lembrete.",
          icon: payload.icon || "icons/icon-192.png",
          badge: payload.badge || "icons/icon-192.png",
          tag: payload.tag || "gastos-pro-manual",
          renotify: true,
          data: payload.data || { url: "./index.html" }
        }
      )
    );
  }
});

/*
  Push real:
  Este listener já deixa o app preparado para quando você quiser ligar
  push de verdade por servidor (Firebase, OneSignal ou Web Push próprio).
*/
self.addEventListener("push", (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch (e) {
    try {
      data = { body: event.data.text() };
    } catch (_) {
      data = {};
    }
  }

  const title = data.title || "Controle de Gastos PRO";
  const options = {
    body: data.body || "Você tem um novo lembrete.",
    icon: data.icon || "icons/icon-192.png",
    badge: data.badge || "icons/icon-192.png",
    tag: data.tag || "gastos-pro-push",
    renotify: true,
    data: data.data || { url: "./index.html" }
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();

  const url = (event.notification.data && event.notification.data.url) || "./index.html";

  event.waitUntil(
    clients.matchAll({ type: "window", includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ("focus" in client) {
          client.navigate(url).catch(() => {});
          return client.focus();
        }
      }
      if (clients.openWindow) return clients.openWindow(url);
    })
  );
});