# osrs-farming-pwa — Setup Guide

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| Node.js | 18 |
| npm | 9 (bundled with Node 18) |

---

## 1. Install dependencies

```bash
cd osrs-farming-pwa
npm install
```

---

## 2. Generate VAPID keys

Web Push requires a one-time VAPID key pair.  Run this **once** and save the output:

```bash
node -e "
const wp = require('web-push');
const keys = wp.generateVAPIDKeys();
console.log('VAPID_PUBLIC_KEY=' + keys.publicKey);
console.log('VAPID_PRIVATE_KEY=' + keys.privateKey);
"
```

---

## 3. Create your `.env` file

```bash
cp .env.example .env
```

Open `.env` and fill in the values:

```env
PORT=3000
VAPID_PUBLIC_KEY=<paste public key here>
VAPID_PRIVATE_KEY=<paste private key here>
VAPID_SUBJECT=mailto:you@example.com
```

---

## 4. Start the server

```bash
npm start
```

The server starts at **http://localhost:3000**.

For development with auto-restart:

```bash
npm run dev
```

---

## 5. Open the PWA in a browser

1. Navigate to `http://localhost:3000`.
2. Click **Enable Notifications** and accept the browser permission prompt.
3. The page will show three sections:
   - **Patches** — receives events from the RuneLite plugin
   - **Seeds & Saplings** — your current inventory
   - **Plant Next (by XP/hr)** — optimizer advice

---

## 6. Expose to the internet (for testing on your phone)

Use [ngrok](https://ngrok.com) to create a temporary public URL:

```bash
# Install ngrok from https://ngrok.com, then:
ngrok http 3000
```

Copy the `https://` URL shown (e.g. `https://abc123.ngrok-free.app`) and paste it
into the RuneLite plugin's **Webhook URL** setting.

---

## 7. Deploy to your VPS

### Example: Ubuntu/Debian with systemd

```bash
# Copy the osrs-farming-pwa directory to your server
scp -r osrs-farming-pwa user@your-server:/opt/osrs-farming-pwa

# On the server
cd /opt/osrs-farming-pwa
npm install --omit=dev

# Create systemd service
sudo nano /etc/systemd/system/farming-pwa.service
```

Paste:
```ini
[Unit]
Description=OSRS Farming PWA
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/osrs-farming-pwa
EnvironmentFile=/opt/osrs-farming-pwa/.env
ExecStart=/usr/bin/node server/index.js
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now farming-pwa
sudo systemctl status farming-pwa
```

### Reverse proxy with nginx (recommended)

```nginx
server {
    listen 443 ssl;
    server_name your-domain.example.com;

    location / {
        proxy_pass         http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade $http_upgrade;
        proxy_set_header   Connection 'upgrade';
        proxy_set_header   Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

Use [Certbot](https://certbot.eff.org) for a free Let's Encrypt TLS certificate.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `VAPID keys are not set` error on start | Missing `.env` | Copy `.env.example` → `.env` and fill VAPID keys |
| Notifications not arriving | Permission not granted | Click "Enable Notifications" on the page |
| `HTTP 410` in logs | Subscription expired | App auto-removes it; subscribe again on your phone |
| Webhook POST 400 error | Missing fields in plugin payload | Check the plugin version matches this server |
| State file permission error | Wrong directory permissions | `chmod 755 data/` on the server |

---

## API reference

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/notify` | Receive `patch_ready` event from plugin |
| `POST` | `/api/seeds`  | Receive seed inventory update from plugin |
| `GET`  | `/api/state`  | Return current patches + seeds (dashboard) |
| `POST` | `/api/subscribe` | Save a push subscription from the browser |
| `GET`  | `/api/vapid-public-key` | Return VAPID public key for the front-end |
