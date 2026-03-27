/**
 * osrs-farming-pwa — Express server entrypoint.
 *
 * Start with:  npm start
 * Dev mode:    npm run dev   (auto-restarts on file changes, Node 18+)
 */

import 'dotenv/config';
import express           from 'express';
import path              from 'path';
import { fileURLToPath } from 'url';

import { PORT, PUBLIC_DIR, VAPID_PUBLIC_KEY, validateConfig } from './config.js';
import { loadState, addSubscription, getPatchEvents, getSeedInventories } from './services/stateService.js';
import notifyRouter from './routes/notify.js';
import seedsRouter  from './routes/seeds.js';

// ---------------------------------------------------------------------------
// Validate config before anything else
// ---------------------------------------------------------------------------
validateConfig();

// ---------------------------------------------------------------------------
// Load persisted state
// ---------------------------------------------------------------------------
loadState();

// ---------------------------------------------------------------------------
// Express app
// ---------------------------------------------------------------------------
const app = express();

app.use(express.json());

// Serve the PWA from /public
app.use(express.static(PUBLIC_DIR));

// ---------------------------------------------------------------------------
// API routes
// ---------------------------------------------------------------------------

/** Receives patch_ready webhooks from the RuneLite plugin. */
app.use('/api/notify', notifyRouter);

/** Receives seed-inventory updates from the RuneLite plugin. */
app.use('/api/seeds', seedsRouter);

/**
 * GET /api/state
 * Returns the full current state for the PWA dashboard.
 */
app.get('/api/state', (_req, res) => {
  res.json({
    patches: getPatchEvents(),
    seeds:   getSeedInventories(),
  });
});

/**
 * POST /api/subscribe
 * Saves a new Web Push subscription sent by the PWA front-end.
 *
 * Body: a PushSubscription JSON object
 */
app.post('/api/subscribe', (req, res) => {
  const subscription = req.body;
  if (!subscription?.endpoint) {
    return res.status(400).json({ error: 'Invalid subscription object' });
  }
  addSubscription(subscription);
  return res.status(201).json({ ok: true });
});

/**
 * GET /api/vapid-public-key
 * Returns the VAPID public key so the front-end can subscribe without
 * hard-coding it in the HTML.
 */
app.get('/api/vapid-public-key', (_req, res) => {
  res.json({ publicKey: VAPID_PUBLIC_KEY });
});

// ---------------------------------------------------------------------------
// Start
// ---------------------------------------------------------------------------
app.listen(PORT, () => {
  console.log(`osrs-farming-pwa listening on http://localhost:${PORT}`);
  console.log(`Serving PWA from: ${PUBLIC_DIR}`);
});
