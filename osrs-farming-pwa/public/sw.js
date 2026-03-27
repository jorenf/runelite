/**
 * Service Worker — handles Web Push notification events.
 *
 * The browser installs this file automatically when the PWA registers it
 * via `navigator.serviceWorker.register('/sw.js')`.
 */

const CACHE_NAME = 'farming-pwa-v1';

// Static assets to pre-cache on install
const PRECACHE_URLS = ['/', '/index.html', '/style.css', '/app.js', '/manifest.json'];

// ---------------------------------------------------------------------------
// Install — pre-cache static assets
// ---------------------------------------------------------------------------
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(PRECACHE_URLS))
  );
  self.skipWaiting();
});

// ---------------------------------------------------------------------------
// Activate — clean up old caches
// ---------------------------------------------------------------------------
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    )
  );
  self.clients.claim();
});

// ---------------------------------------------------------------------------
// Fetch — serve from cache, fall back to network
// ---------------------------------------------------------------------------
self.addEventListener('fetch', event => {
  // Only cache same-origin GET requests
  if (event.request.method !== 'GET' || !event.request.url.startsWith(self.location.origin)) {
    return;
  }
  event.respondWith(
    caches.match(event.request).then(cached => cached ?? fetch(event.request))
  );
});

// ---------------------------------------------------------------------------
// Push — display notification when a push message arrives
// ---------------------------------------------------------------------------
self.addEventListener('push', event => {
  let payload = { title: 'Farming Tracker', body: 'A patch is ready!', icon: '/icon-192.png', data: {} };

  if (event.data) {
    try {
      payload = { ...payload, ...JSON.parse(event.data.text()) };
    } catch {
      payload.body = event.data.text();
    }
  }

  event.waitUntil(
    self.registration.showNotification(payload.title, {
      body:    payload.body,
      icon:    payload.icon ?? '/icon-192.png',
      badge:   '/icon-192.png',
      tag:     payload.data?.patch ?? 'farming',   // group notifications per patch
      renotify: true,
      data:    payload.data,
      vibrate: [200, 100, 200],
    })
  );
});

// ---------------------------------------------------------------------------
// Notification click — focus or open the PWA
// ---------------------------------------------------------------------------
self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
      for (const client of clientList) {
        if (client.url.startsWith(self.location.origin) && 'focus' in client) {
          return client.focus();
        }
      }
      return clients.openWindow('/');
    })
  );
});
