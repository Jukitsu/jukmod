package net.jukitsumc.jukmod.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class AvoidSwollenCreeperGoal extends Goal {
    protected final PathfinderMob mob;
    private final double walkSpeedModifier;
    private final double sprintSpeedModifier;
    @Nullable
    protected Creeper toAvoid;
    protected final float maxDist;
    @Nullable
    protected Path path;
    protected final PathNavigation pathNav;
    protected final Predicate<LivingEntity> avoidPredicate;
    protected final Predicate<LivingEntity> predicateOnAvoidEntity;
    private final TargetingConditions avoidEntityTargeting;

    public AvoidSwollenCreeperGoal(PathfinderMob pathfinderMob, float f, double d, double e) {
        this(pathfinderMob, livingEntity -> true, f, d, e, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
    }

    public AvoidSwollenCreeperGoal(PathfinderMob pathfinderMob, Predicate<LivingEntity> predicate, float f, double d, double e, Predicate<LivingEntity> predicate2) {
        this.mob = pathfinderMob;
        this.avoidPredicate = predicate;
        this.maxDist = f;
        this.walkSpeedModifier = d;
        this.sprintSpeedModifier = e;
        this.predicateOnAvoidEntity = predicate2;
        this.pathNav = pathfinderMob.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        this.avoidEntityTargeting = TargetingConditions.forCombat().range(f).selector(predicate2.and(predicate));
    }

    public AvoidSwollenCreeperGoal(PathfinderMob pathfinderMob, float f, double d, double e, Predicate<LivingEntity> predicate) {
        this(pathfinderMob, livingEntity -> true, f, d, e, predicate);
    }

    @Override
    public boolean canUse() {
        this.toAvoid = this.mob.level().getNearestEntity(this.mob.level().getEntitiesOfClass(Creeper.class, this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist), livingEntity -> true), this.avoidEntityTargeting, this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ());
        if (this.toAvoid == null) {
            return false;
        }
        if (!this.toAvoid.isAlive() || this.toAvoid.getSwelling(0.0F) < 0.01F) {
            this.toAvoid = null;
            return false;
        }

        Vec3 vec3 = DefaultRandomPos.getPosAway(this.mob, 16, 7, ((Entity)this.toAvoid).position());
        if (vec3 == null) {
            return false;
        }
        if (((Entity)this.toAvoid).distanceToSqr(vec3.x, vec3.y, vec3.z) < ((Entity)this.toAvoid).distanceToSqr(this.mob)) {
            return false;
        }
        this.path = this.pathNav.createPath(vec3.x, vec3.y, vec3.z, 0);
        return this.path != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.pathNav.isDone();
    }

    @Override
    public void start() {
        this.pathNav.moveTo(this.path, this.walkSpeedModifier);
    }

    @Override
    public void stop() {
        this.toAvoid = null;
    }

    @Override
    public void tick() {
        if (this.mob.distanceToSqr((Entity)this.toAvoid) < 49.0) {
            this.mob.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
        } else {
            this.mob.getNavigation().setSpeedModifier(this.walkSpeedModifier);
        }
    }
}
