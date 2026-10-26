package dev.spotifyoverlay;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import java.util.function.Consumer;

public class OverlayScreen extends Screen {
    TextFieldWidget id;
    boolean drag, volDrag; double ox, oy;

    public OverlayScreen() { super(Text.literal("Spotify Overlay")); }

    static class S extends SliderWidget {
        final String n; final double mn, mx; final Consumer<Double> cb;
        S(int x, int y, int w, String n, double mn, double mx, double v, Consumer<Double> cb) {
            super(x, y, w, 20, Text.empty(), (v - mn) / (mx - mn));
            this.n = n; this.mn = mn; this.mx = mx; this.cb = cb; updateMessage();
        }
        @Override protected void updateMessage() {
            double v = mn + value * (mx - mn);
            setMessage(Text.literal(n + ": " + (mx - mn < 10 ? String.format("%.2f", v) : String.valueOf((int) v))));
        }
        @Override protected void applyValue() { cb.accept(mn + value * (mx - mn)); }
    }

    @Override protected void init() {
        Cfg c = Cfg.I;
        int cx = width / 2 - 100, y = height / 2 - 40;
        id = new TextFieldWidget(textRenderer, cx, y, 200, 20, Text.empty());
        id.setMaxLength(64); id.setText(c.clientId);
        addDrawableChild(id);
        addDrawableChild(ButtonWidget.builder(Text.literal("Bei Spotify einloggen"), b -> {
            c.clientId = id.getText().trim(); Cfg.save(); SpotifyApi.login();
        }).dimensions(cx, y + 22, 200, 20).build());
        addDrawableChild(new S(cx, y + 44, 99, "Rot", 0, 255, c.r, v -> c.r = v.intValue()));
        addDrawableChild(new S(cx + 101, y + 44, 99, "Gruen", 0, 255, c.g, v -> c.g = v.intValue()));
        addDrawableChild(new S(cx, y + 66, 99, "Blau", 0, 255, c.b, v -> c.b = v.intValue()));
        addDrawableChild(new S(cx + 101, y + 66, 99, "Glas", 10, 220, c.alpha, v -> c.alpha = v.intValue()));
        addDrawableChild(new S(cx, y + 88, 99, "Groesse", 0.5, 3, c.scale, v -> c.scale = v.floatValue()));
        addDrawableChild(ButtonWidget.builder(Text.literal("Position reset"), b -> { c.x = 12; c.y = 12; })
                .dimensions(cx + 101, y + 88, 99, 20).build());
    }

    @Override public void render(DrawContext c, int mx, int my, float d) {
        OverlayRenderer.ctext(c, textRenderer, "Spotify Client ID:", width / 2, height / 2 - 52, 0xFFFFFFFF);
        OverlayRenderer.ctext(c, textRenderer, "Overlay ziehen = verschieben | Buttons klicken = steuern", width / 2, height / 2 + 76, 0xFFAAAAAA);
        super.render(c, mx, my, d);
    }

    void setVol(double mx) { SpotifyApi.setVolume((int) Math.round(Math.max(0, Math.min(1, (OverlayRenderer.lx(mx) - 146) / 52.0)) * 100)); }

    @Override public void renderBackground(DrawContext c, int mx, int my, float d) { /* HUD sichtbar lassen */ }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        double mx = click.x(), my = click.y();
        switch (OverlayRenderer.hit(mx, my)) {
            case 1 -> SpotifyApi.prev();
            case 2 -> SpotifyApi.toggle();
            case 3 -> SpotifyApi.next();
            case 4 -> { volDrag = true; setVol(mx); }
            case 5 -> { drag = true; ox = mx - Cfg.I.x; oy = my - Cfg.I.y; }
            default -> { return false; }
        }
        return true;
    }

    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        double mx = click.x(), my = click.y();
        if (drag) { Cfg.I.x = (int) (mx - ox); Cfg.I.y = (int) (my - oy); return true; }
        if (volDrag) { setVol(mx); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override public boolean mouseReleased(Click click) {
        drag = volDrag = false; Cfg.save();
        return super.mouseReleased(click);
    }

    @Override public void removed() { Cfg.I.clientId = id.getText().trim(); Cfg.save(); }
    @Override public boolean shouldPause() { return false; }
}
