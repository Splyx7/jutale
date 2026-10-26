package dev.spotifyoverlay;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public class Cfg {
    public String clientId = "", refresh = "";
    public int x = 12, y = 12, r = 30, g = 215, b = 96, alpha = 90;
    public float scale = 1f;

    public static Cfg I = new Cfg();
    static final Path P = FabricLoader.getInstance().getConfigDir().resolve("spotifyoverlay.json");
    static final Gson G = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        try { if (Files.exists(P)) I = G.fromJson(Files.readString(P), Cfg.class); } catch (Exception ignored) {}
        if (I == null) I = new Cfg();
    }
    public static void save() { try { Files.writeString(P, G.toJson(I)); } catch (Exception ignored) {} }
    public int accent() { return 0xFF000000 | r << 16 | g << 8 | b; }
    public int body() { return alpha << 24 | r << 16 | g << 8 | b; }
}
