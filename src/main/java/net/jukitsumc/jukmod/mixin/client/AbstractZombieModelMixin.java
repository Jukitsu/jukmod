package net.jukitsumc.jukmod.mixin.client;

import net.jukitsumc.jukmod.Jukmod;
import net.jukitsumc.jukmod.config.option.BooleanOption;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.zombie.AbstractZombieModel;
import net.minecraft.client.renderer.entity.state.UndeadRenderState;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.SwingAnimationType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractZombieModel.class)
public class AbstractZombieModelMixin<S extends ZombieRenderState> extends HumanoidModel<S> {
    protected AbstractZombieModelMixin(ModelPart modelPart) {
        super(modelPart);
    }
    @Unique
    private static BooleanOption oldZombieArm = Jukmod.getInstance().getConfig().animations().oldZombieArm();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initialize(CallbackInfo ci) {
        oldZombieArm = Jukmod.getInstance().getConfig().animations().oldZombieArm();
    }

    @Inject(method="setupAnim", at=@At("HEAD"), cancellable = true)
    public void newSetupAnim(S undeadRenderState, CallbackInfo ci) {
        if (oldZombieArm.get()) {
            ModelPart modelPart = this.leftArm;
            ModelPart modelPart2 = this.rightArm;

            if (undeadRenderState.swingAnimationType != SwingAnimationType.STAB) {
                super.setupAnim(undeadRenderState);

                float f = undeadRenderState.attackTime;
                float j;
                float h = Mth.sin(f * (float) Math.PI);
                float i = Mth.sin((1.0f - (1.0f - f) * (1.0f - f)) * (float) Math.PI);
                modelPart2.zRot = 0.0f;
                modelPart2.yRot = -(0.1f - h * 0.6f);
                modelPart2.xRot = j = (float) (-Math.PI) / 2.25f;
                modelPart2.xRot -= h * 1.2f - i * 0.4f;

                modelPart.zRot = 0.0f;
                modelPart.yRot = 0.1f - h * 0.6f;
                modelPart.xRot = j;
                modelPart.xRot -= h * 1.2f - i * 0.4f;
            }
            AnimationUtils.bobArms(modelPart2, modelPart, undeadRenderState.ageInTicks);
            ci.cancel();

        }
    }

}
