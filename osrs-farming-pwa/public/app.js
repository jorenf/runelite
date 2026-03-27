/**
 * OSRS Farming Tracker PWA — frontend JavaScript.
 *
 * Responsibilities:
 *  1. Register the service worker and request push notification permission.
 *  2. Subscribe to Web Push using the server's VAPID public key.
 *  3. Poll /api/state every 30 s and on page load, then render the UI.
 *  4. Manage countdown timers for growing patches.
 */

'use strict';

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------
const POLL_INTERVAL_MS = 30_000;

// ---------------------------------------------------------------------------
// Service worker registration
// ---------------------------------------------------------------------------
async function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return null;
  try {
    const reg = await navigator.serviceWorker.register('/sw.js');
    console.log('[sw] Registered:', reg.scope);
    return reg;
  } catch (err) {
    console.error('[sw] Registration failed:', err);
    return null;
  }
}

// ---------------------------------------------------------------------------
// Push subscription
// ---------------------------------------------------------------------------
async function subscribeToPush(swRegistration) {
  if (!swRegistration || !('PushManager' in window)) {
    showToast('Push notifications not supported in this browser.');
    return;
  }

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    showToast('Notification permission denied.');
    updateNotifyButton('denied');
    return;
  }

  try {
    // Fetch the VAPID public key from the server
    const res = await fetch('/api/vapid-public-key');
    const { publicKey } = await res.json();

    const sub = await swRegistration.pushManager.subscribe({
      userVisibleOnly:      true,
      applicationServerKey: urlBase64ToUint8Array(publicKey),
    });

    // Send the subscription to the server
    await fetch('/api/subscribe', {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(sub),
    });

    updateNotifyButton('subscribed');
    showToast('🔔 Notifications enabled!');
  } catch (err) {
    console.error('[push] Subscribe error:', err);
    showToast('Failed to enable notifications. Check console.');
  }
}

// ---------------------------------------------------------------------------
// State fetching + rendering
// ---------------------------------------------------------------------------
let countdownHandles = {};

async function loadAndRender() {
  try {
    const res   = await fetch('/api/state');
    const state = await res.json();
    renderPatches(state.patches ?? {});
    renderSeeds(state.seeds ?? {});
  } catch (err) {
    console.error('[app] Failed to load state:', err);
  }
}

/**
 * Renders the "Patches" section.
 * Patches is an object: { patchName: { event object } }
 */
function renderPatches(patches) {
  const list = document.getElementById('patch-list');
  list.innerHTML = '';

  // Stop existing countdowns
  Object.values(countdownHandles).forEach(clearInterval);
  countdownHandles = {};

  const entries = Object.values(patches);
  if (entries.length === 0) {
    list.innerHTML = '<p class="empty">No patch events received yet.<br>Plant a tree and the plugin will check in!</p>';
    return;
  }

  // Sort newest first
  entries.sort((a, b) => (b.receivedAt ?? 0) - (a.receivedAt ?? 0));

  for (const evt of entries) {
    const card = buildPatchCard(evt);
    list.appendChild(card);
  }
}

function buildPatchCard(evt) {
  const card = document.createElement('div');
  card.className = 'card';
  card.dataset.patch = evt.patch;

  const info = document.createElement('div');
  info.style.flex = '1';
  info.style.minWidth = '0';

  const name = document.createElement('div');
  name.className = 'card-name';
  name.textContent = capitalise(evt.plant ?? 'Unknown tree');

  const sub = document.createElement('div');
  sub.className = 'card-sub';
  sub.textContent = evt.patch ?? '';

  info.appendChild(name);
  info.appendChild(sub);

  // Badge: always READY (the server only stores patch_ready events)
  const badge = document.createElement('span');
  badge.className = 'badge badge-ready';
  badge.textContent = 'READY';

  const time = document.createElement('div');
  time.className = 'timer';
  time.textContent = evt.timestamp ? formatAgo(evt.timestamp) : '';

  card.appendChild(info);
  card.appendChild(badge);
  card.appendChild(time);
  return card;
}

/**
 * Renders the "Seeds" section.
 * seeds is an object: { playerName: [ {name, quantity, xpPerHour}, … ] }
 */
function renderSeeds(seedsByPlayer) {
  const list = document.getElementById('seed-list');
  list.innerHTML = '';

  const allSeeds = Object.values(seedsByPlayer).flat();
  if (allSeeds.length === 0) {
    list.innerHTML = '<p class="empty">No seeds found yet.<br>Open your bank or inventory in-game.</p>';
    renderAdvice([]);
    return;
  }

  // Merge duplicates across players (edge case for multi-account)
  const merged = mergeSeeds(allSeeds);
  merged.sort((a, b) => b.xpPerHour - a.xpPerHour);

  for (const seed of merged) {
    const card = document.createElement('div');
    card.className = 'card';

    const name = document.createElement('div');
    name.className = 'card-name';
    name.textContent = seed.name;
    const sub = document.createElement('div');
    sub.className = 'card-sub';
    sub.textContent = `${Math.round(seed.xpPerHour).toLocaleString()} xp/hr`;
    const info = document.createElement('div');
    info.style.flex = '1';
    info.appendChild(name);
    info.appendChild(sub);

    const qty = document.createElement('span');
    qty.className = 'qty';
    qty.textContent = `×${seed.quantity}`;

    card.appendChild(info);
    card.appendChild(qty);
    list.appendChild(card);
  }

  renderAdvice(merged);
}

/**
 * Renders the "Planting Advice" section (derived client-side from seed data).
 * The server's optimizer advice is not separately requested here — the seeds
 * list itself (sorted by xp/hr) serves as the primary guide.
 */
function renderAdvice(seeds) {
  const container = document.getElementById('advice-list');
  container.innerHTML = '';

  if (seeds.length === 0) {
    container.innerHTML = '<p class="empty">No saplings available.</p>';
    return;
  }

  seeds.forEach((seed, i) => {
    const row = document.createElement('div');
    row.className = 'advice-row';

    const seedName = document.createElement('div');
    seedName.className = 'seed-name';
    seedName.textContent = `${i + 1}. ${seed.name}  ×${seed.quantity}`;

    const xpRate = document.createElement('div');
    xpRate.className = 'xp-rate';
    xpRate.textContent = `${Math.round(seed.xpPerHour).toLocaleString()} xp/hr`;

    row.appendChild(seedName);
    row.appendChild(xpRate);
    container.appendChild(row);
  });
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function mergeSeeds(seeds) {
  const map = new Map();
  for (const s of seeds) {
    if (map.has(s.name)) {
      map.get(s.name).quantity += s.quantity;
    } else {
      map.set(s.name, { ...s });
    }
  }
  return [...map.values()];
}

function formatAgo(timestampMs) {
  const diffMs = Date.now() - timestampMs;
  const mins   = Math.floor(diffMs / 60_000);
  if (mins < 1)   return 'just now';
  if (mins < 60)  return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24)   return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

function capitalise(str) {
  if (!str) return str;
  return str.charAt(0).toUpperCase() + str.slice(1);
}

/** Converts a URL-safe base64 string to a Uint8Array (for VAPID key). */
function urlBase64ToUint8Array(base64String) {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64  = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw     = atob(base64);
  return Uint8Array.from([...raw].map(c => c.charCodeAt(0)));
}

// ---------------------------------------------------------------------------
// Toast notification
// ---------------------------------------------------------------------------
let toastTimer;
function showToast(message) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3000);
}

function updateNotifyButton(state) {
  const btn = document.getElementById('notify-btn');
  if (state === 'subscribed') {
    btn.textContent = '🔔 Subscribed';
    btn.disabled = true;
  } else if (state === 'denied') {
    btn.textContent = 'Permission denied';
    btn.disabled = true;
  }
}

// ---------------------------------------------------------------------------
// Init
// ---------------------------------------------------------------------------
let swRegistration = null;

document.addEventListener('DOMContentLoaded', async () => {
  swRegistration = await registerServiceWorker();

  // Check if already subscribed
  if (swRegistration) {
    const existing = await swRegistration.pushManager.getSubscription();
    if (existing) updateNotifyButton('subscribed');
  }

  // Enable notifications button
  document.getElementById('notify-btn').addEventListener('click', () => {
    subscribeToPush(swRegistration);
  });

  // Manual refresh button
  document.getElementById('refresh-btn').addEventListener('click', loadAndRender);

  // Initial load + periodic polling
  await loadAndRender();
  setInterval(loadAndRender, POLL_INTERVAL_MS);
});
