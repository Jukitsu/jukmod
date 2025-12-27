package net.jukitsumc.jukmod.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin extends Goal {

    @Shadow private long lastCanUseCheck;

    @Shadow @Final protected PathfinderMob mob;
    @Shadow private int ticksUntilNextAttack;
    @Shadow private Path path;
    @Shadow @Final private double speedModifier;
    @Shadow private int ticksUntilNextPathRecalculation;
    @Shadow @Final private boolean followingTargetEvenIfNotSeen;
    @Shadow private double pathedTargetX;
    @Shadow private double pathedTargetY;
    @Shadow private double pathedTargetZ;

    @Shadow protected abstract void checkAndPerformAttack(LivingEntity livingEntity);

    /**
     * Fix ADHD Mobs
     * @author Jukitsu
     * @reason Refactor
     */
    @Overwrite
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        this.path = this.mob.getNavigation().createPath(target, 0);
        return this.path != null || this.mob.isWithinMeleeAttackRange(target);
    }

    /**
     * Fix ADHD Mobs
     * @author Jukitsu
     * @reason Refactor
     */
    @Overwrite
    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
    }

    /**
     * Fix ADHD Mobs
     * @author Jukitsu
     * @reason Refactor
     */
    @Overwrite
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);

        if (shouldRecalculatePath(target)) {
            recalculatePath(target);
        }

        this.ticksUntilNextAttack--;
        this.checkAndPerformAttack(target);
    }

    private boolean shouldRecalculatePath(LivingEntity target) {
        return (this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target))
                && this.ticksUntilNextPathRecalculation <= 0;
    }

    private void recalculatePath(LivingEntity target) {
        this.pathedTargetX = target.getX();
        this.pathedTargetY = target.getY();
        this.pathedTargetZ = target.getZ();
        this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);

        double distance = this.mob.distanceToSqr(target);
        if (distance > 1024.0D) {
            this.ticksUntilNextPathRecalculation += 10;
        } else if (distance > 256.0D) {
            this.ticksUntilNextPathRecalculation += 5;
        }

        if (!this.mob.getNavigation().moveTo(target, this.speedModifier))
            this.ticksUntilNextPathRecalculation += 15;

        this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
    }

    /**
     * Fix ADHD Mobs
     * @author Jukitsu
     * @reason Refactor
     */
    @Overwrite
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();

        return target != null
                && target.isAlive()
                && this.mob.isWithinHome(target.blockPosition())
                && (this.followingTargetEvenIfNotSeen || this.mob.getNavigation().isInProgress())
                && (!(target instanceof Player) || (!target.isSpectator() && !((Player) target).isCreative()));
    }

    /**
     * Fix ADHD Mobs
     * @author Jukitsu
     * @reason Refactor
     */
    @Overwrite
    public void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.getAttackInterval();
    }

    /**
     * Fix ADHD Mobs
     * @author Jukitsu
     * @reason Refactor
     */
    @Overwrite
    public int getAttackInterval() {
        int baseInterval = 25 - this.mob.level().getDifficulty().getId() * 5;
        double attackSpeed = this.mob.getAttributeValue(Attributes.ATTACK_SPEED);
        int interval = Math.max(baseInterval, Mth.floor(1.0D / attackSpeed * 20.0D));
        return this.adjustedTickDelay(interval);
    }
}