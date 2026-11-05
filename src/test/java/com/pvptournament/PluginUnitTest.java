package com.pvptournament;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import okhttp3.OkHttpClient;
import org.junit.Test;
import static org.junit.Assert.*;

public class PluginUnitTest
{
    @Test public void restrictsEndpoints()
    {
        OkHttpClient http = new OkHttpClient(); Gson gson = new Gson();
        assertEquals("https://example.com", new ApiClient(http, gson, "https://example.com/").base);
        assertEquals("http://127.0.0.1:8080", new ApiClient(http, gson, "http://127.0.0.1:8080").base);
        for (String url : new String[] {"http://example.com", "https://user:pass@example.com", "https://example.com/path", "https://example.com?token=x", "file:///tmp/test", "https://example.com#fragment"})
        {
            try { new ApiClient(http, gson, url); fail(url); }
            catch (IllegalArgumentException expected) { /* rejected */ }
        }
    }

    @Test public void rendersBracketOffscreen() throws Exception
    {
        JsonObject room = new Gson().fromJson("{\"players\":[{\"id\":\"a\",\"name\":\"Alice\"},{\"id\":\"b\",\"name\":\"Bob\"}],\"rounds\":[[{\"id\":\"0-0\",\"a\":\"a\",\"b\":\"b\",\"winner\":\"a\",\"complete\":true}]]}", JsonObject.class);
        BracketView view = new BracketView(room);
        view.setSize(view.getPreferredSize());
        BufferedImage image = new BufferedImage(view.getWidth(), view.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        view.paint(graphics); graphics.dispose();
        assertTrue(view.getWidth() >= 240);
        assertNotEquals(image.getRGB(0, 0), image.getRGB(10, 42));
        new File("build").mkdirs();
        ImageIO.write(image, "png", new File("build/bracket-preview.png"));
    }
}
