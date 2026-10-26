package dev.spotifyoverlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

public class OverlayRenderer {
    public static final int W = 210, H = 76;
    static float angle; static long last = System.nanoTime();

    static void ctext(DrawContext c, TextRenderer tr, String s, int cx, int y, int col) {
        c.drawText(tr, s, cx - tr.getWidth(s) / 2, y, col, true);
    }

    public static void render(DrawContext c) {
        Cfg cf = Cfg.I;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        long n = System.nanoTime(); float dt = (n - last) / 1e9f; last = n;
        if (SpotifyApi.playing) angle = (angle + dt * 40f) % 360f;

        Matrix3x2fStack ms = c.getMatrices();
        ms.pushMatrix(); ms.translate(cf.x, cf.y); ms.scale(cf.scale, cf.scale);

        rrect(c, 0, 0, W, H, 10, cf.body(), 0x66FFFFFF);

        // Schallplatte (vorgebackene Textur, rotiert)
        Identifier t = SpotifyApi.vinylTex;
        if (t != null) {
            ms.pushMatrix(); ms.translate(38f, 38f); ms.rotate((float) Math.toRadians(angle));
            c.drawTexture(RenderPipelines.GUI_TEXTURED, t, -30, -30, 0f, 0f, 60, 60, Vinyl.S, Vinyl.S, Vinyl.S, Vinyl.S);
            ms.popMatrix();
        }

        c.drawText(tr, tr.trimToWidth(SpotifyApi.title, W - 86), 76, 9, 0xFFFFFFFF, true);
        c.drawText(tr, tr.trimToWidth(SpotifyApi.artist, W - 86), 76, 21, 0xFFC8C8C8, false);

        int bw = W - 10 - 76;
        c.fill(76, 36, W - 10, 38, 0x40FFFFFF);
        if (SpotifyApi.duration > 0)
            c.fill(76, 36, 76 + (int) (bw * (SpotifyApi.progressNow() / (double) SpotifyApi.duration)), 38, cf.accent());

        ctext(c, tr, "|<", 84, 49, 0xFFFFFFFF);
        ctext(c, tr, SpotifyApi.playing ? "||" : ">", 104, 49, 0xFFFFFFFF);
        ctext(c, tr, ">|", 124, 49, 0xFFFFFFFF);

        c.fill(146, 51, 198, 54, 0x40FFFFFF);
        int vx = 146 + (int) (52 * SpotifyApi.volume / 100.0);
        c.fill(146, 51, vx, 54, cf.accent());
        c.fill(vx - 1, 48, vx + 1, 57, 0xFFFFFFFF);

        ms.popMatrix();
    }

    /** 0 = nichts, 1 prev, 2 play, 3 next, 4 volume, 5 panel */
    public static int hit(double mx, double my) {
        double lx = lx(mx), ly = (my - Cfg.I.y) / Cfg.I.scale;
        if (lx < 0 || ly < 0 || lx > W || ly > H) return 0;
        if (ly >= 46 && ly <= 58) {
            if (lx >= 76 && lx < 92) return 1;
            if (lx >= 96 && lx < 112) return 2;
            if (lx >= 116 && lx < 132) return 3;
            if (lx >= 142 && lx <= 202) return 4;
        }
        return 5;
    }
    public static double lx(double mx) { return (mx - Cfg.I.x) / Cfg.I.scale; }

    static void rrect(DrawContext c, int x, int y, int w, int h, int rad, int body, int border) {
        for (int i = 0; i < h; i++) {
            double dy = i < rad ? rad - i - 0.5 : (i >= h - rad ? i - (h - rad) + 0.5 : -1);
            int in = dy < 0 ? 0 : (int) Math.round(rad - Math.sqrt(rad * rad - dy * dy));
            if (i == 0 || i == h - 1) { c.fill(x + in, y + i, x + w - in, y + i + 1, border); continue; }
            c.fill(x + in + 1, y + i, x + w - in - 1, y + i + 1, body);
            c.fill(x + in, y + i, x + in + 1, y + i + 1, border);
            c.fill(x + w - in - 1, y + i, x + w - in, y + i + 1, border);
        }
        c.fillGradient(x + rad, y + 1, x + w - rad, y + h / 2, 0x38FFFFFF, 0x00FFFFFF); // Glas-Glanz
    }
}
