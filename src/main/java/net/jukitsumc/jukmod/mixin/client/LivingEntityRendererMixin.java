package net.jukitsumc.jukmod.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jukitsumc.jukmod.Jukmod;
import net.jukitsumc.jukmod.config.option.BooleanOption;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {
    protected M model;
    @Unique
    private BooleanOption deathWalk;

    @Unique
    private BooleanOption oldPlayerBackwardsOption;

    private float deathDir = -1;

    public LivingEntityRendererMixin(EntityRendererProvider.Context context, M entityModel, float f) {
        super(context);
        this.model = entityModel;
        this.shadowRadius = f;
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    private void initialize(CallbackInfo ci) {
        oldPlayerBackwardsOption = Jukmod.getInstance().getConfig().animations().oldPlayerBackwards();
        deathWalk = Jukmod.getInstance().getConfig().animations().deathWalk();
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"))
    private boolean deathWalkAnimation(boolean original) {
        return deathWalk.get() || original;
    }

    private static Vec3 getInputVector(Vec3 vec3, float f, float g) {
        double d = vec3.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec32 = (d > 1.0 ? vec3.normalize() : vec3).scale((double)f);
            float h = Mth.sin(g * 0.017453292F);
            float i = Mth.cos(g * 0.017453292F);
            return new Vec3(vec32.x * (double)i - vec32.z * (double)h, vec32.y, vec32.z * (double)i + vec32.x * (double)h);
        }
    }

    @Inject(method="setupRotations", at=@At(value="INVOKE",
            target="Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
            ordinal = 1
    ), cancellable = true)
    protected void lieCorrectlyWhenDying(T livingEntity, PoseStack poseStack,
                                         float f, float g, float h, CallbackInfo ci) {

        if (livingEntity.deathTime == 1) {
            Vec3 u = livingEntity.getDeltaMovement();
            Vec3 v = getInputVector(new Vec3(0, 0, 1), 1, livingEntity.yBodyRot);
            deathDir = (float)u.dot(v);
        }
        float i = ((float)livingEntity.deathTime + h - 1.0F) / 20.0F * 1.6F;
        i = Mth.sqrt(i);
        if (i > 1.0F) {
            i = 1.0F;
        }


        if (livingEntity instanceof Animal) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(i * this.getFlipDegrees(livingEntity)));
        }
        else if (deathDir <= 0.0F) {
            Axis axis = oldPlayerBackwardsOption.get() ? Axis.ZP : Axis.XP;
            poseStack.mulPose(axis.rotationDegrees(i * this.getFlipDegrees(livingEntity)));
        }
        else {
            poseStack.mulPose(Axis.XN.rotationDegrees(i * this.getFlipDegrees(livingEntity)));
        }

        ci.cancel();
    }

    @Shadow
    protected abstract float getFlipDegrees(T livingEntity);


}