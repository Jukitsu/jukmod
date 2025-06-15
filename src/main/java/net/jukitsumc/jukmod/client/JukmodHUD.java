package net.jukitsumc.jukmod.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class JukmodHUD implements HudRenderCallback {
    private final Minecraft client = Minecraft.getInstance();

    @Override
    public void onHudRender(GuiGraphics drawContext, DeltaTracker deltaTracker) {
        if (!client.getDebugOverlay().showDebugScreen()) {
            drawContext.drawString(this.client.font, "Minecraft 1.21.5", 2, 2, 0xffffffff, true);
            drawContext.drawString(this.client.font, String.format("%s fps", client.getFps()), 2, client.font.lineHeight + 2, 0xffffffff, true);
        }
    }

}
