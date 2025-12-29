package net.jukitsumc.jukmod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.jukitsumc.jukmod.entity.RangedAttackHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangedBowAttackGoal.class)
public class RangedBowAttackGoalMixin<T extends Monster & RangedAttackMob> {
    @Shadow
    @Final
    private T mob;

    @Shadow @Final private float attackRadiusSqr;

    @Unique private boolean lockedIn;
    @Unique private int lockInTime;

    @Unique private int coyoteTime;

    @Shadow private int attackTime;

    @Shadow private int attackIntervalMin;

    @Inject(method = "tick", at = @At(value="INVOKE", target="Lnet/minecraft/world/entity/monster/Monster;getTicksUsingItem()I"))
    public void bowSpam(CallbackInfo info) {
        LivingEntity target = this.mob.getTarget();
        int i = this.mob.getTicksUsingItem();
        if (i >= 10 && this.mob.distanceToSqr(target) < 25.0F) {
            this.mob.stopUsingItem();
            ((RangedAttackMob)this.mob).performRangedAttack(target, BowItem.getPowerForTime(i));
            this.attackTime = this.attackIntervalMin;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void fixSkeletonStrafing(CallbackInfo info) {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null) {
            Vec3 v = RangedAttackHandler.getInitialVector(this.mob, livingEntity, this.mob.getEyeY(), 3.0);
            this.mob.getLookControl().setLookAt(this.mob.getEyePosition().add(v));
        }


    }

    @Redirect(method="tick", at=@At(value = "INVOKE", target="Lnet/minecraft/world/entity/monster/Monster;stopUsingItem()V"))
    public void doNothing(Monster instance) {

    }

    @Redirect(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/monster/RangedAttackMob;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V"))
    public void randomizeShooting(RangedAttackMob skeleton, LivingEntity livingEntity, float v, @Local double d) {
        this.coyoteTime = Math.max(0, this.coyoteTime - 1);
        Vec3 velocity = livingEntity.getKnownMovement();
        Vec3 displacement = this.mob.getPosition(0.0f).subtract(livingEntity.getPosition(0.0f));
        double dotproduct = velocity.dot(displacement);
        if ((this.coyoteTime == 0 && dotproduct * dotproduct < 0.25 * velocity.lengthSqr() * displacement.lengthSqr() && velocity.lengthSqr() > 0.04F)
                || livingEntity.isBlocking()) {
            this.lockedIn = true;
            this.lockInTime += 1;
        } else if (this.lockedIn) {
            this.lockedIn = false;
            this.lockInTime = 0;
            this.coyoteTime = this.mob.getRandom().nextInt(2, 6);
        }
        if ((this.coyoteTime == 0 && !this.lockedIn && this.mob.getRandom().nextInt(24 - this.mob.level().getDifficulty().getId() * 4) > 5)
                || (this.lockedIn && this.mob.getRandom().nextInt(24 - this.mob.level().getDifficulty().getId() * 4) > 11)
                || (this.lockInTime >= 40 && this.mob.getRandom().nextInt(24 - this.mob.level().getDifficulty().getId() * 4) > 8)) {
            this.coyoteTime = 0;
            this.lockInTime = 0;
            this.mob.stopUsingItem();
            this.mob.performRangedAttack(livingEntity, v);
        }
    }


}
