/**
 * POST /api/seeds
 *
 * Receives the player's current seed/sapling inventory from the RuneLite
 * plugin and stores it for display on the PWA dashboard.
 *
 * Expected JSON body:
 * {
 *   "seeds": [
 *     { "name": "Magic sapling", "quantity": 3, "xpPerHour": 1606 }
 *   ],
 *   "player": "PlayerName"
 * }
 */

import { Router } from 'express';
import { setSeedInventory } from '../services/stateService.js';

const router = Router();

router.post('/', (req, res) => {
  const { seeds, player } = req.body;

  if (!Array.isArray(seeds) || !player) {
    return res.status(400).json({ error: 'Missing required fields: seeds (array), player' });
  }

  setSeedInventory(player, seeds);
  return res.status(200).json({ ok: true });
});

export default router;
