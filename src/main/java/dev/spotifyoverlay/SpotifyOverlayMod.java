package dev.spotifyoverlay;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class SpotifyOverlayMod implements ClientModInitializer {
    static final KeyBinding.Category CAT = KeyBinding.Category.create(Identifier.of("spotifyoverlay", "main"));

    static KeyBinding key(String n, int k) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.spotifyoverlay." + n,
                InputUtil.Type.KEYSYM, k, CAT));
    }

    @Override public void onInitializeClient() {
        System.setProperty("java.awt.headless", "true");
        Cfg.load();
        SpotifyApi.start();
        KeyBinding open = key("open", GLFW.GLFW_KEY_O), pp = key("pp", GLFW.GLFW_KEY_K),
                nx = key("next", GLFW.GLFW_KEY_L), pv = key("prev", GLFW.GLFW_KEY_J),
                vu = key("vup", GLFW.GLFW_KEY_PAGE_UP), vd = key("vdn", GLFW.GLFW_KEY_PAGE_DOWN);

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of("spotifyoverlay", "hud"), (ctx, tick) -> OverlayRenderer.render(ctx));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (open.wasPressed()) if (mc.currentScreen == null) mc.setScreen(new OverlayScreen());
            while (pp.wasPressed()) SpotifyApi.toggle();
            while (nx.wasPressed()) SpotifyApi.next();
            while (pv.wasPressed()) SpotifyApi.prev();
            while (vu.wasPressed()) SpotifyApi.setVolume(SpotifyApi.volume + 5);
            while (vd.wasPressed()) SpotifyApi.setVolume(SpotifyApi.volume - 5);
        });
    }
}
