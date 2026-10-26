package dev.spotifyoverlay;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SpotifyApi {
    static final HttpClient H = HttpClient.newHttpClient();
    static final String REDIRECT = "http://127.0.0.1:8888/callback";
    static final String API = "https://api.spotify.com/v1/";
    static String access, verifier, lastTrack = "";
    static long exp;

    public static volatile boolean connected, playing;
    public static volatile String title = "Nicht verbunden - O druecken", artist = "", artistId = "";
    public static volatile int volume = 50;
    public static volatile long progress, duration, stamp, volLock;
    public static volatile Identifier vinylTex;
    static volatile BufferedImage coverImg, artistImg;
    static int texN;

    interface Job { void run() throws Exception; }

    public static void start() {
        bake();
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r); t.setDaemon(true); return t; });
        ex.scheduleWithFixedDelay(SpotifyApi::poll, 1, 2, TimeUnit.SECONDS);
    }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static boolean token() {
        try {
            if (access != null && System.currentTimeMillis() < exp - 30000) return true;
            if (Cfg.I.refresh.isEmpty() || Cfg.I.clientId.isEmpty()) return false;
            return tokenReq("grant_type=refresh_token&refresh_token=" + enc(Cfg.I.refresh) + "&client_id=" + enc(Cfg.I.clientId));
        } catch (Exception e) { return false; }
    }

    static boolean tokenReq(String body) throws Exception {
        var r = H.send(HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(body)).build(), BodyHandlers.ofString());
        if (r.statusCode() != 200) return false;
        JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
        access = o.get("access_token").getAsString();
        exp = System.currentTimeMillis() + o.get("expires_in").getAsLong() * 1000;
        if (o.has("refresh_token")) { Cfg.I.refresh = o.get("refresh_token").getAsString(); Cfg.save(); }
        return true;
    }

    public static void login() {
        new Thread(() -> {
            try {
                byte[] rnd = new byte[48]; new SecureRandom().nextBytes(rnd);
                verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
                String ch = Base64.getUrlEncoder().withoutPadding().encodeToString(
                        MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8)));
                HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 8888), 0);
                s.createContext("/callback", ex -> {
                    String q = ex.getRequestURI().getQuery(), code = null;
                    if (q != null) for (String p : q.split("&")) if (p.startsWith("code=")) code = p.substring(5);
                    byte[] out = "<h2>Fertig! Zurueck zu Minecraft.</h2>".getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    ex.sendResponseHeaders(200, out.length);
                    ex.getResponseBody().write(out); ex.close();
                    try {
                        if (code != null) tokenReq("grant_type=authorization_code&code=" + code + "&redirect_uri=" + enc(REDIRECT)
                                + "&client_id=" + enc(Cfg.I.clientId) + "&code_verifier=" + verifier);
                    } catch (Exception ignored) {}
                    s.stop(0);
                });
                s.start();
                String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + enc(Cfg.I.clientId)
                        + "&scope=" + enc("user-read-playback-state user-modify-playback-state")
                        + "&redirect_uri=" + enc(REDIRECT) + "&code_challenge_method=S256&code_challenge=" + ch;
                Util.getOperatingSystem().open(URI.create(url));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    static HttpResponse<String> call(String m, String url) throws Exception {
        return H.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + access)
                .method(m, BodyPublishers.noBody()).build(), BodyHandlers.ofString());
    }

    static void async(Job j) {
        CompletableFuture.runAsync(() -> { try { if (token()) j.run(); } catch (Exception ignored) {} });
    }

    public static void toggle() {
        boolean p = playing; playing = !p; progress = progressNow(); stamp = System.currentTimeMillis();
        async(() -> call("PUT", API + "me/player/" + (p ? "pause" : "play")));
    }
    public static void next() { async(() -> { call("POST", API + "me/player/next"); Thread.sleep(600); poll(); }); }
    public static void prev() { async(() -> { call("POST", API + "me/player/previous"); Thread.sleep(600); poll(); }); }
    public static void setVolume(int v) {
        v = Math.max(0, Math.min(100, v)); volume = v; volLock = System.currentTimeMillis() + 3000;
        int f = v;
        async(() -> call("PUT", API + "me/player/volume?volume_percent=" + f));
    }
    public static long progressNow() {
        return playing ? Math.min(duration, progress + System.currentTimeMillis() - stamp) : progress;
    }

    static void poll() {
        try {
            if (!token()) { connected = false; return; }
            connected = true;
            var r = call("GET", API + "me/player");
            if (r.statusCode() != 200) { if (title.startsWith("Nicht")) title = "Starte Spotify & spiel was ab"; return; }
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            playing = o.get("is_playing").getAsBoolean();
            progress = o.get("progress_ms").getAsLong(); stamp = System.currentTimeMillis();
            JsonObject d = o.getAsJsonObject("device");
            if (d != null && d.get("volume_percent").isJsonPrimitive() && System.currentTimeMillis() > volLock)
                volume = d.get("volume_percent").getAsInt();
            if (!o.get("item").isJsonObject()) return;
            JsonObject it = o.getAsJsonObject("item");
            duration = it.get("duration_ms").getAsLong();
            title = it.get("name").getAsString();
            JsonObject a = it.getAsJsonArray("artists").get(0).getAsJsonObject();
            artist = a.get("name").getAsString();
            String id = it.get("id").getAsString();
            if (!id.equals(lastTrack)) {
                lastTrack = id;
                String cov = it.getAsJsonObject("album").getAsJsonArray("images").get(0).getAsJsonObject().get("url").getAsString();
                fetch(cov, i -> { coverImg = i; if (artistImg == null) bake(); });
                String aid = a.get("id").getAsString();
                if (!aid.equals(artistId)) {
                    artistId = aid; artistImg = null;
                    JsonArray im = JsonParser.parseString(call("GET", API + "artists/" + aid).body())
                            .getAsJsonObject().getAsJsonArray("images");
                    if (im.size() > 0) fetch(im.get(0).getAsJsonObject().get("url").getAsString(),
                            i -> { artistImg = i; bake(); });
                }
            }
        } catch (Exception ignored) {}
    }

    static void fetch(String url, Consumer<BufferedImage> cb) {
        CompletableFuture.runAsync(() -> {
            try {
                byte[] b = H.send(HttpRequest.newBuilder(URI.create(url)).build(), BodyHandlers.ofByteArray()).body();
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(b));
                if (bi != null) cb.accept(bi);
            } catch (Exception ignored) {}
        });
    }

    /** Backt Platte + Kuenstlerbild zu EINER Textur (1.21.6+ erlaubt kein eigenes Kreis-Rendering mehr). */
    static synchronized void bake() {
        try {
            BufferedImage out = Vinyl.bake(artistImg != null ? artistImg : coverImg);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bo);
            NativeImage ni = NativeImage.read(new ByteArrayInputStream(bo.toByteArray()));
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                TextureManager tm = mc.getTextureManager();
                Identifier id = Identifier.of("spotifyoverlay", "vinyl_" + (texN++));
                tm.registerTexture(id, new NativeImageBackedTexture(() -> "spotifyoverlay_vinyl", ni));
                Identifier old = vinylTex;
                vinylTex = id;
                if (old != null) tm.destroyTexture(old);
            });
        } catch (Exception e) { e.printStackTrace(); }
    }
}
