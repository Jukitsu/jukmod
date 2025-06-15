package net.jukitsumc.jukmod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
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


    @Shadow private int attackTime;

    @Shadow private int attackIntervalMin;

    @Inject(method = "tick", at = @At("TAIL"))
    public void fixSkeletonStrafing(CallbackInfo info) {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null)
            this.mob.getLookControl().setLookAt(livingEntity, 30.0F, 30.0F);
    }

    @Redirect(method="tick", at=@At(value = "INVOKE", target="Lnet/minecraft/world/entity/monster/Monster;stopUsingItem()V"))
    public void doNothing(Monster instance) {

    }

    @Redirect(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/monster/RangedAttackMob;performRangedAttack(Lnet/minecraft/world/entity/LivingEntity;F)V"))
    public void randomizeShooting(RangedAttackMob skeleton, LivingEntity livingEntity, float v, @Local double d) {
        Vec3 velocity = livingEntity.getKnownMovement();
        Vec3 displacement = this.mob.getPosition(0.0f).subtract(livingEntity.getPosition(0.0f));
        double dotproduct = velocity.dot(displacement);
        if (dotproduct * dotproduct >= 0.25 * velocity.lengthSqr() * displacement.lengthSqr()
                || this.mob.getRandom().nextInt(12 - this.mob.level().getDifficulty().getId() * 2) > 5) {
            this.mob.stopUsingItem();
            this.mob.performRangedAttack(livingEntity, v);
        }
    }


}
