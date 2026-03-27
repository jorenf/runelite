package com.farmingtracker;

import com.farmingtracker.optimizer.PlantingAdvice;
import com.farmingtracker.patch.PatchState;
import com.farmingtracker.patch.TrackedPatch;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * In-game panel overlay showing:
 * <ol>
 *   <li>Tracked patches with countdown timers (or "READY" label)</li>
 *   <li>Planting advice from the XP optimizer (when enabled)</li>
 * </ol>
 *
 * <p>The overlay is added/removed by {@link FarmingTrackerPlugin} in its
 * {@code startUp}/{@code shutDown} lifecycle methods.
 */
public class FarmingTrackerOverlay extends Overlay
{
    private static final Color COLOR_READY    = new Color(0, 255, 100);
    private static final Color COLOR_GROWING  = new Color(180, 180, 180);
    private static final Color COLOR_ADVICE   = new Color(255, 200, 50);
    private static final Color COLOR_HEADER   = Color.WHITE;

    private final FarmingTrackerPlugin plugin;
    private final FarmingTrackerConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public FarmingTrackerOverlay(FarmingTrackerPlugin plugin, FarmingTrackerConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(220, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showOverlay())
        {
            return null;
        }

        panelComponent.getChildren().clear();

        renderTrackedPatches();

        if (config.showOptimizer())
        {
            renderPlantingAdvice();
        }

        return panelComponent.render(graphics);
    }

    // -----------------------------------------------------------------------

    private void renderTrackedPatches()
    {
        List<TrackedPatch> patches = plugin.getTrackedPatches();
        if (patches.isEmpty())
        {
            return;
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Farming Tracker")
                .color(COLOR_HEADER)
                .build());

        long now = System.currentTimeMillis();

        for (TrackedPatch patch : patches)
        {
            PatchState state = patch.isNotified() ? PatchState.READY
                    : (now >= patch.getReadyAt() ? PatchState.READY : PatchState.GROWING);

            String left  = shorten(patch.getCropName(), 14);
            String right = state == PatchState.READY
                    ? "READY"
                    : formatDuration(patch.getReadyAt() - now);

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(left)
                    .right(right)
                    .leftColor(state == PatchState.READY ? COLOR_READY : COLOR_GROWING)
                    .rightColor(state == PatchState.READY ? COLOR_READY : COLOR_GROWING)
                    .build());
        }
    }

    private void renderPlantingAdvice()
    {
        List<PlantingAdvice> advice = plugin.getPlantingAdvice();
        if (advice.isEmpty())
        {
            return;
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Plant Next")
                .color(COLOR_ADVICE)
                .build());

        for (PlantingAdvice a : advice)
        {
            String left  = shorten(a.getSeed().getDisplayName(), 14);
            String right = shorten(a.getPatch().getName(), 12);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(left)
                    .right(right)
                    .leftColor(COLOR_ADVICE)
                    .rightColor(COLOR_GROWING)
                    .build());
        }
    }

    // -----------------------------------------------------------------------
    // Formatting helpers
    // -----------------------------------------------------------------------

    /**
     * Formats a millisecond duration as {@code Xh Ym} or {@code Ym Zs}.
     */
    private static String formatDuration(long millis)
    {
        if (millis <= 0)
        {
            return "READY";
        }
        long hours   = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0)
        {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0)
        {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private static String shorten(String text, int maxLen)
    {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "…";
    }
}
