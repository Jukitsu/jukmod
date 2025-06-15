package net.jukitsumc.jukmod.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OverlayTexture.class)
public abstract class OverlayTextureMixin {
    @Shadow
    @Final
    private DynamicTexture texture;

    private static int getColorInt(int red, int green, int blue, int alpha) {
        alpha = 255 - alpha;
        return (alpha << 24) + (red << 16) + (green << 8) + blue;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void oldHitColor(CallbackInfo ci) {

        NativeImage nativeImage = this.texture.getPixels();

        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                if (i < 8) {
                    assert nativeImage != null;
                    nativeImage.setPixel(j, i, getColorInt(255, 0, 0, 102));
                }
            }
        }

        this.texture.upload();
    }


}

