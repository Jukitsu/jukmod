package net.jukitsumc.jukmod.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor ;
import net.minecraft.resources.Identifier;

public class JukmodHUD {

    private static final Minecraft client = Minecraft.getInstance();

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath("jukmod", "hud"),
                JukmodHUD::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickDelta) {

        if (!client.getDebugOverlay().showDebugScreen()) {

            graphics.text(
                    client.font,
                    "Minecraft 26.1.2",
                    2,
                    2,
                    0xFFFFFFFF,
                    true
            );

            graphics.text(
                    client.font,
                    String.format("%s fps", client.getFps()),
                    2,
                    client.font.lineHeight + 2,
                    0xFFFFFFFF,
                    true
            );
        }
    }
}