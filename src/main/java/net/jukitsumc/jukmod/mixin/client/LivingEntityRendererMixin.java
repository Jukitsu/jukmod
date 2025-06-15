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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
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
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements RenderLayerParent<S, M> {

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

    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"))
    private boolean deathWalkAnimation(boolean original) {
        return deathWalk.get() || original;
    }

    @Inject(method="setupRotations", at=@At(value="INVOKE",
            target="Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V",
            ordinal = 1
    ), cancellable = true)
    protected void lieCorrectlyWhenDying(S livingEntityRenderState, PoseStack poseStack, float f, float g, CallbackInfo ci) {


        float i = ((float)livingEntityRenderState.deathTime - 1.0F) / 20.0F * 1.6F;
        i = Mth.sqrt(i);
        if (i > 1.0F) {
            i = 1.0F;
        }

        if (oldPlayerBackwardsOption.get()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(i * this.getFlipDegrees()));
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(i * this.getFlipDegrees()));
        }

        ci.cancel();
    }

    @Shadow protected abstract float getFlipDegrees();

}
