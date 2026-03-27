/**
 * In-memory state store with JSON-file persistence.
 *
 * Managed state:
 *  - push subscriptions (one per browser/device)
 *  - current farming patch events (latest patch_ready payload per patch name)
 *  - current seed inventory (latest /api/seeds payload per player)
 *
 * The state file is written synchronously on every mutation so that a
 * server restart does not lose subscriptions.
 */

import fs   from 'fs';
import path from 'path';
import { STATE_FILE } from '../config.js';

// ---------------------------------------------------------------------------
// Internal state shape
// ---------------------------------------------------------------------------

/** @type {{ subscriptions: object[], patches: object, seeds: object }} */
let state = {
  subscriptions: [],
  patches: {},   // patchName → latest patch_ready event object
  seeds:   {},   // playerName → latest seeds array
};

// ---------------------------------------------------------------------------
// Initialisation
// ---------------------------------------------------------------------------

/**
 * Loads persisted state from disk.  Call once at server startup.
 * Non-fatal: if the file does not exist the server starts with empty state.
 */
export function loadState() {
  try {
    const dir = path.dirname(STATE_FILE);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    if (fs.existsSync(STATE_FILE)) {
      const raw = fs.readFileSync(STATE_FILE, 'utf-8');
      state = { ...state, ...JSON.parse(raw) };
      console.log(`[state] Loaded ${state.subscriptions.length} subscription(s) from ${STATE_FILE}`);
    }
  } catch (err) {
    console.warn('[state] Could not load state file:', err.message);
  }
}

// ---------------------------------------------------------------------------
// Push subscriptions
// ---------------------------------------------------------------------------

/**
 * Adds a new push subscription.
 * Duplicate endpoints are silently ignored.
 *
 * @param {object} subscription  Web Push subscription object from the browser
 */
export function addSubscription(subscription) {
  const exists = state.subscriptions.some(s => s.endpoint === subscription.endpoint);
  if (!exists) {
    state.subscriptions.push(subscription);
    persist();
    console.log('[state] New subscription added. Total:', state.subscriptions.length);
  }
}

/**
 * Removes a subscription by endpoint URL.
 * Called when a push delivery fails with HTTP 410 Gone (subscription expired).
 *
 * @param {string} endpoint
 */
export function removeSubscription(endpoint) {
  const before = state.subscriptions.length;
  state.subscriptions = state.subscriptions.filter(s => s.endpoint !== endpoint);
  if (state.subscriptions.length < before) {
    persist();
    console.log('[state] Removed expired subscription.');
  }
}

/**
 * Returns a shallow copy of all current push subscriptions.
 *
 * @returns {object[]}
 */
export function getSubscriptions() {
  return [...state.subscriptions];
}

// ---------------------------------------------------------------------------
// Patch state
// ---------------------------------------------------------------------------

/**
 * Records a patch_ready event.
 *
 * @param {object} event  The parsed JSON body from POST /api/notify
 */
export function setPatchEvent(event) {
  state.patches[event.patch] = { ...event, receivedAt: Date.now() };
  persist();
}

/**
 * Returns all recorded patch events.
 *
 * @returns {object}  Keys are patch names; values are event objects.
 */
export function getPatchEvents() {
  return { ...state.patches };
}

// ---------------------------------------------------------------------------
// Seed inventory
// ---------------------------------------------------------------------------

/**
 * Updates the seed inventory for a player.
 *
 * @param {string}   playerName
 * @param {object[]} seeds  Array of { name, quantity, xpPerHour }
 */
export function setSeedInventory(playerName, seeds) {
  state.seeds[playerName] = seeds;
  persist();
}

/**
 * Returns the stored seed inventories keyed by player name.
 *
 * @returns {object}
 */
export function getSeedInventories() {
  return { ...state.seeds };
}

// ---------------------------------------------------------------------------
// Persistence
// ---------------------------------------------------------------------------

function persist() {
  try {
    fs.writeFileSync(STATE_FILE, JSON.stringify(state, null, 2), 'utf-8');
  } catch (err) {
    console.error('[state] Failed to write state file:', err.message);
  }
}
