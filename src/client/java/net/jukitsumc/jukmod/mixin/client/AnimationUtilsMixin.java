package net.jukitsumc.jukmod.mixin.client;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AnimationUtils.class)
public class AnimationUtilsMixin {
    @Overwrite
    public static void animateZombieArms(ModelPart modelPart, ModelPart modelPart2, boolean bl, float f, float g) {
        float j;
        float h = Mth.sin(f * (float) Math.PI);
        float i = Mth.sin((1.0f - (1.0f - f) * (1.0f - f)) * (float) Math.PI);
        modelPart2.zRot = 0.0f;
        modelPart.zRot = 0.0f;
        modelPart2.yRot = -(0.1f - h * 0.6f);
        modelPart.yRot = 0.1f - h * 0.6f;
        modelPart2.xRot = j = (float) (-Math.PI) / 2.25f;
        modelPart.xRot = j;
        modelPart2.xRot -= h * 1.2f - i * 0.4f;
        modelPart.xRot -= h * 1.2f - i * 0.4f;
        AnimationUtils.bobArms(modelPart2, modelPart, g);
    }
}