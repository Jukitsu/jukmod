package net.jukitsumc.jukmod.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeapAtTargetGoal.class)
public abstract class LeapAtTargetGoalMixin extends Goal {
    @Shadow
    @Final
    private Mob mob;
    @Shadow
    private LivingEntity target;


    @Override
    public void tick() {
        if (this.mob.isWithinMeleeAttackRange(target) && this.mob.getSensing().hasLineOfSight(target)) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget((ServerLevel)target.level(), target);
            super.stop();
        }
    }
}
