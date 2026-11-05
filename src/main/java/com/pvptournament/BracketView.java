package com.pvptournament;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.*;
import javax.swing.JComponent;

final class BracketView extends JComponent
{
    private final JsonObject tournament;
    private static final int WIDTH = 178, ROW = 66, TOP = 32;
    BracketView(JsonObject tournament)
    {
        this.tournament = tournament;
        JsonArray rounds = tournament.getAsJsonArray("rounds");
        int rows = rounds.size() == 0 ? 1 : rounds.get(0).getAsJsonArray().size();
        setPreferredSize(new Dimension(Math.max(240, rounds.size() * WIDTH + 12), TOP + rows * ROW));
        setOpaque(true);
    }
    private String name(JsonElement id)
    {
        if (id == null || id.isJsonNull()) { return "TBD / bye"; }
        for (JsonElement p : tournament.getAsJsonArray("players"))
        {
            if (p.getAsJsonObject().get("id").equals(id)) { return p.getAsJsonObject().get("name").getAsString(); }
        }
        return "Unknown";
    }
    @Override protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(new Color(25, 28, 34)); g.fillRect(0, 0, getWidth(), getHeight());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        JsonArray rounds = tournament.getAsJsonArray("rounds");
        if (rounds.size() == 0) { g.setColor(Color.LIGHT_GRAY); g.drawString("Bracket appears when registration closes.", 10, 22); }
        for (int r = 0; r < rounds.size(); r++)
        {
            int x = 8 + r * WIDTH;
            g.setColor(new Color(235, 185, 65));
            g.drawString(r == rounds.size() - 1 ? "FINAL" : "ROUND " + (r + 1), x, 20);
            JsonArray matches = rounds.get(r).getAsJsonArray();
            for (int i = 0; i < matches.size(); i++)
            {
                int y = TOP + (int) ((i + 0.5) * (1 << r) * ROW) - 24;
                JsonObject m = matches.get(i).getAsJsonObject();
                g.setColor(new Color(48, 53, 64)); g.fillRoundRect(x, y, 150, 48, 8, 8);
                for (int side = 0; side < 2; side++)
                {
                    JsonElement player = m.get(side == 0 ? "a" : "b");
                    boolean winner = !player.isJsonNull() && player.equals(m.get("winner"));
                    g.setColor(winner ? new Color(125, 228, 165) : Color.WHITE);
                    g.drawString((winner ? "* " : "") + name(player), x + 7, y + 18 + side * 21);
                }
                if (r + 1 < rounds.size())
                {
                    int nextY = TOP + (int) ((i / 2 + 0.5) * (1 << (r + 1)) * ROW);
                    g.setColor(new Color(107, 116, 133));
                    g.drawLine(x + 150, y + 24, x + 160, y + 24);
                    g.drawLine(x + 160, y + 24, x + 160, nextY);
                    g.drawLine(x + 160, nextY, x + WIDTH, nextY);
                }
            }
        }
        g.dispose();
    }
}
