package com.pvptournament;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import okhttp3.OkHttpClient;

@PluginDescriptor(name = "PvP Tournament", description = "Private tournament codes and organizer-confirmed PvP brackets", tags = {"pvp", "tournament", "bracket"})
public class TournamentPlugin extends Plugin
{
    @Inject private ClientToolbar toolbar;
    @Inject private TournamentConfig config;
    @Inject private OkHttpClient http;
    @Inject private Gson gson;
    private NavigationButton navigation;
    private TournamentPanel panel;

    @Provides TournamentConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(TournamentConfig.class);
    }

    @Override protected void startUp() throws Exception
    {
        onEdt(() -> {
            panel = new TournamentPanel(http, gson, config);
            BufferedImage icon = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = icon.createGraphics();
            g.setColor(new Color(235, 185, 65));
            g.fillRect(7, 3, 10, 10); g.fillRect(10, 13, 4, 5); g.fillRect(6, 19, 12, 3);
            g.drawOval(2, 3, 19, 10); g.dispose();
            navigation = NavigationButton.builder().tooltip("PvP Tournament").icon(icon).priority(7).panel(panel).build();
            toolbar.addNavigation(navigation);
        });
    }

    @Override protected void shutDown() throws Exception
    {
        onEdt(() -> {
            if (panel != null) { panel.close(); }
            if (navigation != null) { toolbar.removeNavigation(navigation); }
        });
    }

    private static void onEdt(Runnable action) throws Exception
    {
        if (SwingUtilities.isEventDispatchThread()) { action.run(); }
        else { SwingUtilities.invokeAndWait(action); }
    }
}
