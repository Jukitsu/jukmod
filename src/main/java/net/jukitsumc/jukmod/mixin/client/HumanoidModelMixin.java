package net.jukitsumc.jukmod.mixin.client;

import net.jukitsumc.jukmod.Jukmod;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> extends AgeableListModel<T> implements ArmedModel, HeadedModel {

    @Shadow
    @Final
    public ModelPart head;
    @Shadow
    @Final
    public ModelPart hat;
    @Shadow
    @Final
    public ModelPart body;
    @Shadow
    @Final
    public ModelPart rightArm;
    @Shadow
    @Final
    public ModelPart leftArm;


    private HumanoidArm getAttackArm(T livingEntity) {
        HumanoidArm humanoidArm = livingEntity.getMainArm();
        return livingEntity.swingingArm == InteractionHand.MAIN_HAND ? humanoidArm : humanoidArm.getOpposite();
    }
    @Shadow
    protected abstract ModelPart getArm(HumanoidArm humanoidArm);


    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true)
    public void onSetupAttackAnimation(T humanoidRenderState, float f, CallbackInfo ci) {
        if (!(this.attackTime <= 0.0F)) {
            HumanoidArm humanoidArm = this.getAttackArm(humanoidRenderState);
            ModelPart modelPart = this.getArm(humanoidArm);
            float g = this.attackTime;
            this.body.yRot = Mth.sin(Mth.sqrt(g) * 6.2831855F) * 0.2F;
            ModelPart var10000;
            if (humanoidArm == HumanoidArm.LEFT) {
                this.body.yRot *= -1.0F;
            }

            this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F;
            this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F;
            this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F;
            this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F;
            this.rightArm.yRot += this.body.yRot;
            this.leftArm.yRot += this.body.yRot;

            if (humanoidArm == HumanoidArm.LEFT) {
                this.rightArm.xRot -= this.body.yRot;
            } else {
                this.leftArm.xRot += this.body.yRot;
            }

            g = 1.0F - this.attackTime;
            g *= g;
            g *= g;
            g = 1.0F - g;
            float h = Mth.sin(g * 3.1415927F);
            float i = Mth.sin(this.attackTime * 3.1415927F) * -(this.head.xRot - 0.7F) * 0.75F;
            modelPart.xRot -= h * 1.2F + i;
            modelPart.yRot += this.body.yRot * 2.0F;
            if (humanoidArm == HumanoidArm.LEFT) {
                modelPart.zRot += Mth.sin(this.attackTime * (float) Math.PI) * 0.4F;
            } else {
                modelPart.zRot -= Mth.sin(this.attackTime * (float) Math.PI) * 0.4F;
            }
            ci.cancel();
        }
    }
}
