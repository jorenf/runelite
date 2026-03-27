# osrs-farming-plugin — Setup Guide

## Prerequisites

| Tool | Minimum version | How to install |
|------|----------------|----------------|
| Java JDK | 11 | https://adoptium.net |
| IntelliJ IDEA | any recent | https://www.jetbrains.com/idea/ |
| Git | any | https://git-scm.com |

---

## 1. Clone the official RuneLite plugin template

```bash
git clone https://github.com/runelite/plugin-template.git farming-tracker-plugin
cd farming-tracker-plugin
```

---

## 2. Replace the source tree with this plugin's source

Delete the template's `src/` directory and copy this repo's contents in:

```bash
# From inside farming-tracker-plugin/
rm -rf src/
cp -r /path/to/osrs-farming-plugin/src .
cp /path/to/osrs-farming-plugin/build.gradle .
cp /path/to/osrs-farming-plugin/settings.gradle .
```

Or simply clone this repo into a fresh directory and open `osrs-farming-plugin/` as an IntelliJ project — the `build.gradle` is already configured.

---

## 3. Build the plugin JAR

```bash
./gradlew shadowJar
```

The output is placed at:
```
build/libs/farming-tracker-1.0.0.jar
```

---

## 4. Load the plugin in RuneLite

1. Open the RuneLite launcher.
2. In the top-right gear menu → **Configuration** → **Developer** → enable **Developer mode**.
3. In the same menu enable **Side-loading** and point it at the `build/libs/` directory.
4. Restart RuneLite — the **Farming Tracker** plugin will appear in the Plugins list.

Alternatively, use the [RuneLite Plugin Hub](https://runelite.net/plugin-hub) packaging process to distribute to others.

---

## 5. Set the webhook URL

1. In RuneLite, open the **Settings** panel (wrench icon) and search for **Farming Tracker**.
2. Under **Webhook**, paste your PWA server URL, e.g.:
   ```
   https://my-server.example.com
   ```
   or for local testing with ngrok:
   ```
   https://abc123.ngrok-free.app
   ```
3. Toggle **Track tree patches** and **Track fruit-tree patches** as desired.
4. Enable **Include bank in seed inventory** for better optimizer results.

---

## 6. Verify it's working

1. Log into OSRS.
2. Plant any tree or fruit-tree sapling in a patch.
3. You should see:
   - The patch appear in the in-game overlay (top-left panel).
   - A webhook POST sent to `{webhookUrl}/api/notify` (check server logs).
4. When the grow timer elapses:
   - The overlay shows **READY** in green.
   - An in-game chat notification fires.
   - Another webhook POST is sent to `{webhookUrl}/api/notify`.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| No overlay visible | Overlay hidden | Settings → Farming Tracker → Show overlay → on |
| No webhook fired | URL not set | Set webhook URL in plugin settings |
| Wrong patch identified | Player far from patch | Stand within 15 tiles of the patch when planting |
| Patch not detected | Unusual chat message format | Open an issue with the exact chat message text |
| `FARMING_TRANSMIT` compile error | RuneLite API version mismatch | Update `runeLiteVersion` in `build.gradle` |

---

## Notes on grow times

Grow times are taken from RuneLite's `Produce.java` (time-tracking plugin):

| Crop | Stages | Tick (min) | Total grow time |
|------|--------|-----------|----------------|
| Oak | 5 | 40 | 3h 20m |
| Willow | 7 | 40 | 4h 40m |
| Maple | 9 | 40 | 6h |
| Yew | 11 | 40 | 7h 20m |
| Magic | 13 | 40 | 8h 40m |
| All fruit trees | 7 | 160 | 18h 40m |

The timer uses real-world clock time, so it continues counting even while logged out.
