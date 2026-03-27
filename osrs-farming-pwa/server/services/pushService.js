/**
 * Web Push delivery service.
 *
 * Wraps the `web-push` library and broadcasts notifications to all
 * stored subscriptions.  Expired subscriptions (HTTP 410) are
 * automatically removed from state.
 */

import webpush from 'web-push';
import { VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT } from '../config.js';
import { getSubscriptions, removeSubscription } from './stateService.js';

// Configure web-push once at module load
webpush.setVapidDetails(VAPID_SUBJECT, VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY);

/**
 * Broadcasts a push notification to every stored subscription.
 *
 * @param {object} payload  Notification data, serialised to JSON.
 * @param {string} payload.title   Notification title shown in the OS notification tray.
 * @param {string} payload.body    Notification body text.
 * @param {string} [payload.icon]  URL to an icon image (optional).
 * @param {object} [payload.data]  Extra data forwarded to the service worker (optional).
 */
export async function broadcast(payload) {
  const subscriptions = getSubscriptions();
  if (subscriptions.length === 0) {
    console.log('[push] No subscribers — skipping broadcast.');
    return;
  }

  const message = JSON.stringify(payload);
  const results = await Promise.allSettled(
    subscriptions.map(sub => sendOne(sub, message))
  );

  const sent   = results.filter(r => r.status === 'fulfilled').length;
  const failed = results.filter(r => r.status === 'rejected').length;
  console.log(`[push] Broadcast complete: ${sent} sent, ${failed} failed.`);
}

// ---------------------------------------------------------------------------

/**
 * Sends a push message to a single subscription.
 * Removes the subscription if the push service returns HTTP 410 (Gone).
 *
 * @param {object} subscription  Web Push subscription object
 * @param {string} message       JSON-serialised payload string
 */
async function sendOne(subscription, message) {
  try {
    await webpush.sendNotification(subscription, message);
  } catch (err) {
    if (err.statusCode === 410) {
      console.log('[push] Subscription expired, removing:', subscription.endpoint);
      removeSubscription(subscription.endpoint);
    } else {
      console.warn('[push] Delivery failed:', err.message);
      throw err;
    }
  }
}
