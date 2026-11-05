package com.pvptournament;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

final class ApiClient
{
    private final OkHttpClient http;
    private final Gson gson;
    final String base;

    ApiClient(OkHttpClient http, Gson gson, String url)
    {
        URI uri = URI.create(url.trim());
        String host = uri.getHost();
        boolean local = "127.0.0.1".equals(host) || "localhost".equals(host) || "[::1]".equals(host);
        if (host == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
            || !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme())))
            || !(uri.getPath().isEmpty() || "/".equals(uri.getPath())))
        {
            throw new IllegalArgumentException("Use an HTTPS origin, or http://127.0.0.1:8080 locally");
        }
        this.base = url.trim().replaceAll("/+$", "");
        this.gson = gson;
        this.http = http.newBuilder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS).build();
    }

    JsonObject request(String path, String token, JsonObject body) throws IOException
    {
        Request.Builder request = new Request.Builder().url(base + "/v1/tournaments" + path);
        if (!token.isEmpty()) { request.header("Authorization", "Bearer " + token); }
        if (body != null)
        {
            request.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), gson.toJson(body)));
        }
        try (Response response = http.newCall(request.build()).execute())
        {
            if (response.body() == null) { throw new IOException("Empty service response"); }
            if (response.body().contentLength() > 262144) { throw new IOException("Response too large"); }
            // Bound even chunked responses before parsing.
            okio.BufferedSource source = response.body().source();
            if (source.request(262145)) { throw new IOException("Response too large"); }
            JsonObject result;
            try { result = gson.fromJson(source.readUtf8(), JsonObject.class); }
            catch (RuntimeException e) { throw new IOException("Invalid service response"); }
            if (result == null) { throw new IOException("Invalid service response"); }
            if (!response.isSuccessful())
            {
                String message = result.has("error") ? result.get("error").getAsString() : "Service error " + response.code();
                throw new IOException(message);
            }
            return result;
        }
    }
}
