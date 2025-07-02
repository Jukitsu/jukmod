package net.jukitsumc.jukmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.Control;
import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LookControl.class)
public abstract class LookControlMixin implements Control {

    @Shadow @Final protected Mob mob;

    @Shadow protected abstract float rotateTowards(float f, float g, float h);

    @ModifyExpressionValue(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/ai/control/LookControl;rotateTowards(FFF)F"))
    public float onlyRotateWhileMoving(float original) {
        return this.rotateTowards(this.mob.yHeadRot, this.mob.getYRot(), 10.0F);
    }

}
