package com.farmingtracker.webhook;

import com.farmingtracker.FarmingTrackerConfig;
import com.farmingtracker.patch.TrackedPatch;
import com.farmingtracker.seed.SeedItemWithQty;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;

/**
 * Sends asynchronous HTTP POST webhooks to the configured PWA backend.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST {webhookUrl}/api/notify} — fired when a patch becomes ready</li>
 *   <li>{@code POST {webhookUrl}/api/seeds}  — fired when the seed inventory changes</li>
 * </ul>
 *
 * <p>All calls are fire-and-forget on a background thread.
 * Network failures are logged at WARN level and silently ignored so that
 * the plugin never interrupts gameplay.
 */
@Slf4j
@Singleton
public class WebhookService
{
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private FarmingTrackerConfig config;

    @Inject
    private Gson gson;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Sends a {@code patch_ready} event to {@code /api/notify}.
     *
     * @param patch      the patch that has finished growing
     * @param playerName the logged-in player's display name
     */
    public void sendPatchReady(TrackedPatch patch, String playerName)
    {
        String url = buildUrl("/api/notify");
        if (url == null) return;

        JsonObject body = new JsonObject();
        body.addProperty("event", "patch_ready");
        body.addProperty("patch", patch.getPatchName());
        body.addProperty("plant", patch.getCropName());
        body.addProperty("timestamp", System.currentTimeMillis());
        body.addProperty("player", playerName);

        postAsync(url, body.toString());
        log.debug("Sent patch_ready webhook for {} at {}", patch.getCropName(), patch.getPatchName());
    }

    /**
     * Sends the player's current seed inventory to {@code /api/seeds}.
     * Called whenever the inventory or bank contents change.
     *
     * @param seeds      current list of saplings with quantities
     * @param playerName the logged-in player's display name
     */
    public void sendSeedUpdate(List<SeedItemWithQty> seeds, String playerName)
    {
        String url = buildUrl("/api/seeds");
        if (url == null) return;

        JsonArray seedArray = new JsonArray();
        for (SeedItemWithQty sqty : seeds)
        {
            JsonObject seedObj = new JsonObject();
            seedObj.addProperty("name", sqty.getSeed().getDisplayName());
            seedObj.addProperty("quantity", sqty.getQuantity());
            seedObj.addProperty("xpPerHour", Math.round(sqty.getSeed().getXpPerHour()));
            seedArray.add(seedObj);
        }

        JsonObject body = new JsonObject();
        body.add("seeds", seedArray);
        body.addProperty("player", playerName);

        postAsync(url, body.toString());
        log.debug("Sent seed update webhook ({} seed types)", seeds.size());
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String buildUrl(String path)
    {
        String base = config.webhookUrl();
        if (base == null || base.isBlank())
        {
            log.debug("Webhook URL not configured — skipping call to {}", path);
            return null;
        }
        return base.replaceAll("/+$", "") + path;
    }

    private void postAsync(String url, String jsonBody)
    {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.warn("Webhook POST failed [{}]: {}", url, e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful())
                {
                    log.warn("Webhook POST [{}] returned HTTP {}", url, response.code());
                }
                response.close();
            }
        });
    }
}
