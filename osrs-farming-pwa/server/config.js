/**
 * Central configuration loaded from environment variables.
 * Call `loadEnv()` once at server startup before importing this module.
 */

import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

/** Absolute path to the public directory served as the PWA. */
export const PUBLIC_DIR = path.resolve(__dirname, '..', 'public');

/** HTTP port the Express server listens on. */
export const PORT = parseInt(process.env.PORT ?? '3000', 10);

/** VAPID keys for Web Push (generated once per deployment). */
export const VAPID_PUBLIC_KEY  = process.env.VAPID_PUBLIC_KEY  ?? '';
export const VAPID_PRIVATE_KEY = process.env.VAPID_PRIVATE_KEY ?? '';

/**
 * mailto: or https: URI identifying the push sender.
 * Required by major push services (Chrome, Firefox).
 */
export const VAPID_SUBJECT = process.env.VAPID_SUBJECT ?? 'mailto:admin@example.com';

/**
 * Path to the JSON file used for persisting subscriptions and patch state.
 * Defaults to <repo-root>/data/state.json.
 */
export const STATE_FILE = process.env.STATE_FILE
  ?? path.resolve(__dirname, '..', 'data', 'state.json');

/**
 * Validates that all required VAPID keys are present and throws a clear
 * error message at startup if they are missing.
 */
export function validateConfig() {
  if (!VAPID_PUBLIC_KEY || !VAPID_PRIVATE_KEY) {
    throw new Error(
      'VAPID keys are not set.\n' +
      'Run: node -e "const wp=require(\'web-push\'); console.log(JSON.stringify(wp.generateVAPIDKeys(),null,2))"\n' +
      'Then add VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY to your .env file.'
    );
  }
}
