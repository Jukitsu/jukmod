package net.jukitsumc.jukmod.mixin;


import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.jukitsumc.jukmod.Jukmod;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BodyRotationControl.class)
public class BodyRotationControlMixin {

    @Shadow @Final
    private Mob mob;

    @ModifyExpressionValue(method="clientTick",
            at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Mob;getYRot()F"))
    public float handleMobRotation(float original) {
        return mob.yBodyRot;
    }

    @Overwrite
    private void rotateHeadIfNecessary() {

    }
}
