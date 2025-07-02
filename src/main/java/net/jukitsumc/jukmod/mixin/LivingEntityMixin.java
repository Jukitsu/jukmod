package net.jukitsumc.jukmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.jukitsumc.jukmod.Jukmod;
import net.jukitsumc.jukmod.config.option.BooleanOption;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Unique
    private BooleanOption oldPlayerBackwardsOption;

    @Unique
    private BooleanOption deathWalk;

    @Unique
    private BooleanOption oldClientMovement;

    protected LivingEntityMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initialize(CallbackInfo ci) {
        oldPlayerBackwardsOption = Jukmod.getInstance().getConfig().animations().oldPlayerBackwards();
        oldClientMovement = Jukmod.getInstance().getConfig().entities().oldClientMovement();
        deathWalk = Jukmod.getInstance().getConfig().animations().deathWalk();
    }

    @Shadow
    public abstract void knockback(double d, double e, double f);

    @Shadow public float yHeadRot;
    @Shadow public float yBodyRot;

    @Shadow protected abstract float getMaxHeadRotationRelativeToBody();

    @Inject(method = "tickHeadTurn", at = @At("TAIL"), cancellable = true)
    public void tickHeadTurn(float f, float g, CallbackInfoReturnable ci) {
        float angle = Mth.wrapDegrees(f - this.yBodyRot);
        this.yBodyRot += angle * 0.3F;
        float relativeAngle = Mth.wrapDegrees(this.yHeadRot - this.yBodyRot);
        boolean bl = relativeAngle < -90.0F || relativeAngle >= 90.0F;
        if (oldPlayerBackwardsOption.get()) {
            if (Mth.abs(relativeAngle) > 75.0F) {
                relativeAngle = 75.0F * Mth.sign(relativeAngle);
            }
            this.yBodyRot = this.yHeadRot - relativeAngle;

            if (Mth.abs(relativeAngle) > getMaxHeadRotationRelativeToBody()) {
                this.yBodyRot += relativeAngle * 0.2F;
            }

        } else {
            float maxAngle = this.getMaxHeadRotationRelativeToBody();
            if (Math.abs(relativeAngle) > maxAngle) {
                this.yBodyRot += relativeAngle - (float)Mth.sign(relativeAngle) * maxAngle;
            }

        }
        if (bl) {
            g *= -1.0F;
        }
        ci.setReturnValue(g);

    }

    @ModifyExpressionValue(method="tick", at=@At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F",
        ordinal = 0))
    public float returnHeadYaw(float original) {
        return this.yHeadRot;
    }


    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isControlledByLocalInstance()Z"))
    private boolean addOldClientMovement(boolean b) {
        return this.oldClientMovement.get() || b;
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;abs(F)F"))
    private float forceOldBackwardsAnimations(float f) {
        return this.oldPlayerBackwardsOption.get() ? 0.0F : f;
    }


    /**
     * @author Jukitsu
     * @reason Fixes MC-147694
     */
    @Overwrite
    public void blockedByShield(LivingEntity livingEntity) {
        this.knockback(0.5, livingEntity.getX() - this.getX(), livingEntity.getZ() - this.getZ());
    }


}
