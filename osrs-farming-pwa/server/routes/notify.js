/**
 * POST /api/notify
 *
 * Receives a patch_ready webhook from the RuneLite plugin, stores the
 * event, and broadcasts a Web Push notification to all subscribers.
 *
 * Expected JSON body:
 * {
 *   "event":     "patch_ready",
 *   "patch":     "Falador Tree Patch",
 *   "plant":     "magic",
 *   "timestamp": 1710000000000,
 *   "player":    "PlayerName"
 * }
 */

import { Router } from 'express';
import { setPatchEvent } from '../services/stateService.js';
import { broadcast }     from '../services/pushService.js';

const router = Router();

router.post('/', async (req, res) => {
  const { event, patch, plant, timestamp, player } = req.body;

  // Basic validation
  if (!event || !patch || !plant) {
    return res.status(400).json({ error: 'Missing required fields: event, patch, plant' });
  }

  const patchEvent = { event, patch, plant, timestamp: timestamp ?? Date.now(), player };

  // Persist to state
  setPatchEvent(patchEvent);

  // Broadcast Web Push to all subscribers
  try {
    await broadcast({
      title: `🌳 ${capitalise(plant)} is ready!`,
      body:  `Your ${plant} at ${patch} is fully grown and ready to harvest.`,
      icon:  '/icon-192.png',
      data:  patchEvent,
    });
  } catch (err) {
    // Push errors are non-fatal — the event is already stored
    console.error('[notify] Push broadcast error:', err.message);
  }

  return res.status(200).json({ ok: true });
});

export default router;

function capitalise(str) {
  if (!str) return str;
  return str.charAt(0).toUpperCase() + str.slice(1);
}
