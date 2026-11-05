package com.pvptournament;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.*;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;
import okhttp3.OkHttpClient;

final class TournamentPanel extends PluginPanel
{
    private final OkHttpClient http;
    private final Gson gson;
    private final TournamentConfig config;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final JPanel content = new JPanel();
    private final JTextArea status = area("Create a tournament or join an organizer's code.");
    private final Timer timer;
    private final java.util.List<JDialog> brackets = new java.util.ArrayList<>();
    private ApiClient api;
    private String code = "", token = "";
    private JsonObject state;
    private boolean busy, closed;

    TournamentPanel(OkHttpClient http, Gson gson, TournamentConfig config)
    {
        this.http = http; this.gson = gson; this.config = config;
        setLayout(new BorderLayout(0, 8));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        add(content, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        timer = new Timer(5000, event -> { if (!code.isEmpty()) { request("", null, false); } });
        timer.start(); render();
    }

    private static JTextArea area(String value)
    {
        JTextArea text = new JTextArea(value);
        text.setEditable(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setOpaque(false); text.setForeground(Color.LIGHT_GRAY);
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        return text;
    }
    private void button(String text, Runnable action)
    {
        JButton b = new JButton(text); b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> { if (!busy) { action.run(); } });
        content.add(b); content.add(Box.createVerticalStrut(6));
    }
    private String ask(String message, String initial)
    {
        return (String) JOptionPane.showInputDialog(this, message, "PvP Tournament", JOptionPane.PLAIN_MESSAGE, null, null, initial);
    }
    private boolean confirm(String message)
    {
        return JOptionPane.showConfirmDialog(this, message, "PvP Tournament", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }
    private boolean connect()
    {
        try
        {
            api = new ApiClient(http, gson, config.serviceUrl());
            return confirm("Connect to " + api.base + "?\nThis service receives your IP, entered name, registration and results.\nUse a trusted organizer. Names are self-declared. No game credentials are needed.");
        }
        catch (RuntimeException e) { status.setText(e.getMessage()); return false; }
    }
    private void create()
    {
        String title = ask("Tournament name (1–60 characters)", "Clan PvP Championship");
        if (title == null || title.trim().isEmpty() || !connect()) { return; }
        code = ""; token = "";
        JsonObject body = new JsonObject(); body.addProperty("title", title); body.addProperty("capacity", 64);
        request("", body, true);
    }
    private void join()
    {
        String entered = ask("Organizer's 8-character code", "");
        if (entered == null) { return; }
        entered = entered.trim().toUpperCase(Locale.ROOT);
        if (!entered.matches("[A-Z2-9]{8}")) { status.setText("Enter an 8-character tournament code."); return; }
        String name = ask("Your RuneScape display name (self-declared)", "");
        if (name == null || !connect()) { return; }
        code = entered;
        JsonObject body = new JsonObject(); body.addProperty("name", name);
        request("/join", body, true);
    }
    private void resume()
    {
        String saved = ask("Paste your private session JSON. Keep it secret.", "");
        if (saved == null) { return; }
        try
        {
            JsonObject s = gson.fromJson(saved, JsonObject.class);
            ApiClient candidate = new ApiClient(http, gson, s.get("service").getAsString());
            String c = s.get("code").getAsString(), t = s.get("token").getAsString();
            if (!c.matches("[A-Z2-9]{8}") || !t.matches("[A-Za-z0-9_-]{43}")) { throw new IllegalArgumentException(); }
            if (!confirm("Resume private session at " + candidate.base + "?")) { return; }
            api = candidate; code = c; token = t;
            request("", null, false);
        }
        catch (RuntimeException e) { status.setText("Invalid session JSON."); }
    }
    private void backup()
    {
        JsonObject s = new JsonObject(); s.addProperty("service", api.base);
        s.addProperty("code", code); s.addProperty("token", token);
        JTextArea text = new JTextArea(gson.toJson(s), 5, 30); text.setLineWrap(true); text.setEditable(false);
        JOptionPane.showMessageDialog(this, new Object[] {"Copy and save privately. This grants your tournament permissions.", new JScrollPane(text)},
            "Private session backup", JOptionPane.WARNING_MESSAGE);
    }
    private void request(String action, JsonObject body, boolean newSession)
    {
        if (closed || busy || api == null) { return; }
        busy = true;
        ApiClient currentApi = api;
        String path = (code.isEmpty() ? "" : "/" + code) + action;
        String credential = newSession ? "" : token;
        worker.submit(() -> {
            try
            {
                JsonObject response = currentApi.request(path, credential, body);
                SwingUtilities.invokeLater(() -> {
                    if (closed) { return; }
                    busy = false;
                    if (newSession)
                    {
                        token = response.get("token").getAsString();
                        state = response.getAsJsonObject("tournament");
                    }
                    else { state = response; }
                    code = state.get("code").getAsString();
                    status.setText("Synced. Results are organizer-confirmed."); render();
                });
            }
            catch (IOException | RuntimeException e)
            {
                SwingUtilities.invokeLater(() -> {
                    if (closed) { return; }
                    busy = false; status.setText("Unable to sync: " + e.getMessage());
                    if (newSession) { code = ""; token = ""; }
                });
            }
        });
    }
    private String player(JsonElement id)
    {
        for (JsonElement p : state.getAsJsonArray("players"))
        {
            if (p.getAsJsonObject().get("id").equals(id)) { return p.getAsJsonObject().get("name").getAsString(); }
        }
        return "Unknown";
    }
    private void render()
    {
        content.removeAll();
        content.add(area("PVP TOURNAMENT\nSingle elimination · up to 64 players\n"));
        if (state == null)
        {
            button("Create tournament", this::create); button("Join with code", this::join); button("Resume private session", this::resume);
        }
        else
        {
            String phase = state.get("status").getAsString();
            boolean host = state.get("organizer").getAsBoolean();
            content.add(area(state.get("title").getAsString() + "\nCode: " + code + "\n" + phase + (host ? " · ORGANIZER" : " · COMPETITOR") + "\n"));
            if (!state.get("champion").isJsonNull()) { content.add(area("#1 PVP CHAMPION\n" + player(state.get("champion")) + "\n")); }
            button("Private session backup", this::backup);
            button("Refresh", () -> request("", null, false));
            StringBuilder roster = new StringBuilder("PLAYERS\n");
            boolean registered = false;
            for (JsonElement p : state.getAsJsonArray("players"))
            {
                JsonObject person = p.getAsJsonObject();
                roster.append(person.get("name").getAsString()).append(person.get("registered").getAsBoolean() ? " · registered\n" : " · joined\n");
                if (person.get("id").equals(state.get("memberId"))) { registered = person.get("registered").getAsBoolean(); }
            }
            content.add(area(roster.toString()));
            if ("REGISTRATION".equals(phase))
            {
                if (host)
                {
                    button("Close registration & seed bracket", () -> {
                        if (confirm("Close registration and randomly seed all registered players? This cannot be undone.")) { request("/start", new JsonObject(), false); }
                    });
                    content.add(area("To compete as organizer, back up this session, disconnect, join your code, then back up the competitor session. Resume organizer credentials to manage results.\n"));
                }
                else
                {
                    final boolean next = !registered;
                    button(registered ? "Withdraw registration" : "Register to compete", () -> {
                        JsonObject body = new JsonObject(); body.addProperty("registered", next); request("/register", body, false);
                    });
                }
            }
            button("Open visual bracket", () -> {
                JDialog dialog = new JDialog((Frame) null, "Tournament bracket · snapshot", false);
                dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                brackets.removeIf(d -> !d.isDisplayable()); brackets.add(dialog);
                dialog.add(new JScrollPane(new BracketView(state)));
                dialog.setSize(850, 600); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
            });
            if (host && "ACTIVE".equals(phase))
            {
                for (JsonElement round : state.getAsJsonArray("rounds"))
                {
                    for (JsonElement element : round.getAsJsonArray())
                    {
                        JsonObject match = element.getAsJsonObject();
                        if (!match.get("complete").getAsBoolean() && !match.get("a").isJsonNull() && !match.get("b").isJsonNull())
                        {
                            content.add(area("Match " + match.get("id").getAsString() + ": " + player(match.get("a")) + " vs " + player(match.get("b"))));
                            for (String side : new String[] {"a", "b"})
                            {
                                button("Winner: " + player(match.get(side)), () -> {
                                    if (!confirm("Confirm " + player(match.get(side)) + " won? Results are final.")) { return; }
                                    JsonObject b = new JsonObject(); b.add("match", match.get("id")); b.add("winner", match.get(side)); b.add("revision", state.get("revision"));
                                    request("/result", b, false);
                                });
                            }
                        }
                    }
                }
            }
            if (host && ("ACTIVE".equals(phase) || "REGISTRATION".equals(phase)))
            {
                button("Cancel tournament", () -> { if (confirm("Cancel this tournament permanently?")) { request("/cancel", new JsonObject(), false); } });
            }
            button("Disconnect / switch session", () -> {
                if (confirm("Save your private session backup first. Disconnect now?")) { code = ""; token = ""; state = null; render(); }
            });
        }
        content.revalidate(); content.repaint();
    }
    void close()
    {
        closed = true; timer.stop(); worker.shutdownNow(); token = "";
        for (JDialog dialog : brackets) { dialog.dispose(); }
        brackets.clear();
    }
}
