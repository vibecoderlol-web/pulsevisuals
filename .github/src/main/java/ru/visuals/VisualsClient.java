package ru.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;

import java.awt.Color;

public class VisualsClient implements ClientModInitializer {
    static boolean WATERMARK = true, INFO = true, HIT_MARKER = true, HIT_PARTICLES = true;

    private static long hitTime = -100_000L;
    private static String infoLine = "";

    @Override
    public void onInitializeClient() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) {
                hitTime = System.currentTimeMillis();
                if (HIT_PARTICLES) {
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4;
                        world.addParticleClient(ParticleTypes.CRIT,
                                entity.getX(), entity.getY() + entity.getHeight() * 0.6, entity.getZ(),
                                Math.cos(a) * 0.25, 0.15, Math.sin(a) * 0.25);
                    }
                }
            }
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null || mc.world.getTime() % 10 != 0) return;
            PlayerListEntry e = mc.getNetworkHandler() == null ? null
                    : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            int ping = e == null ? 0 : e.getLatency();
            infoLine = mc.getCurrentFps() + " FPS | " + ping + " ms | "
                    + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.of("pulsevisuals", "hud"), VisualsClient::render);
    }

    private static void render(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        long now = System.currentTimeMillis();

        if (WATERMARK) {
            float hue = (now % 4000L) / 4000f;
            float pulse = 0.75f + 0.25f * (float) Math.sin(now / 250.0);
            int col = 0xFF000000 | Color.HSBtoRGB(hue, 0.55f, pulse);
            ctx.drawTextWithShadow(mc.textRenderer, "Pulse Visuals", 4, 4, col);
            if (INFO) ctx.drawTextWithShadow(mc.textRenderer, infoLine, 4, 15, 0xFFFFFFFF);
        }

        if (HIT_MARKER) {
            float p = (now - hitTime) / 350f;
            if (p >= 0f && p < 1f) {
                int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
                int a = (int) (255 * (1f - p)), o = 3 + (int) (7 * p);
                int c = (a << 24) | 0xFF3355;
                ctx.fill(cx - o - 4, cy - 1, cx - o, cy + 1, c);
                ctx.fill(cx + o, cy - 1, cx + o + 4, cy + 1, c);
                ctx.fill(cx - 1, cy - o - 4, cx + 1, cy - o, c);
                ctx.fill(cx - 1, cy + o, cx + 1, cy + o + 4, c);
            }
        }
    }
}
