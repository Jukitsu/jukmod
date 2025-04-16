package net.jukitsumc.jukmod.mixin;

import com.mojang.logging.LogUtils;
import net.jukitsumc.jukmod.Jukmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.stream.StreamSupport;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    @Shadow
    @Final
    protected GoalSelector targetSelector;
    @Shadow
    @Final
    protected GoalSelector goalSelector;
    @Shadow
    protected JumpControl jumpControl;
    @Unique
    private int autoJumpTime = 0;
    @Shadow
    @Final
    private BodyRotationControl bodyRotationControl;


    protected MobMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @author Jukitsu
     * @reason Attack Speed
     */
    @Overwrite
    public static AttributeSupplier.Builder createMobAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.ATTACK_KNOCKBACK).add(Attributes.ATTACK_SPEED);
    }

    @Shadow
    public abstract int getMaxHeadYRot();

    @Shadow
    public abstract MoveControl getMoveControl();

    @Shadow
    public abstract JumpControl getJumpControl();

    @Shadow
    public abstract boolean isEffectiveAi();


    private boolean isMoving() {
        return this.xxa != 0.0F || this.zza != 0.0F;
    }

    private boolean canAutoJump() {
        return this.autoJumpTime <= 0
                && this.onGround()
                && !this.isPassenger()
                && this.isMoving()
                && (double) this.getBlockJumpFactor() >= 1.0D
                && this.isEffectiveAi();
    }

    protected void updateAutoJump(double f, double g) {
        if (!canAutoJump()) return;

        Vec3 currentPosition = this.position();
        Vec3 adjustedPosition = currentPosition.add(f, 0.0D, g);
        Vec3 directionVec = new Vec3(f, 0.0D, g);

        float speed = this.getSpeed();
        float lengthSquared = (float) directionVec.lengthSqr();

        if (lengthSquared <= 0.001F) {
            double deltaX = speed * this.xxa;
            double deltaZ = speed * this.zza;
            float sinYRot = Mth.sin(this.getYRot() * 0.017453292F);
            float cosYRot = Mth.cos(this.getYRot() * 0.017453292F);
            directionVec = new Vec3(deltaX * cosYRot - deltaZ * sinYRot, directionVec.y, deltaZ * cosYRot + deltaX * sinYRot);
            lengthSquared = (float) directionVec.lengthSqr();
            if (lengthSquared <= 0.001F) return;
        }

        float invSqrtLength = Mth.invSqrt(lengthSquared);
        Vec3 normalizedDirection = directionVec.scale(invSqrtLength);
        Vec3 forwardVec = this.getForward();
        float dotProduct = (float) (forwardVec.x * normalizedDirection.x + forwardVec.z * normalizedDirection.z);

        if (dotProduct >= -0.15F) {
            CollisionContext collisionContext = CollisionContext.of(this);
            BlockPos currentPos = BlockPos.containing(this.getX(), this.getBoundingBox().maxY, this.getZ());
            BlockState currentBlockState = this.level().getBlockState(currentPos);

            if (currentBlockState.getCollisionShape(this.level(), currentPos, collisionContext).isEmpty()) {
                BlockPos abovePos = currentPos.above();
                BlockState aboveBlockState = this.level().getBlockState(abovePos);

                if (aboveBlockState.getCollisionShape(this.level(), abovePos, collisionContext).isEmpty()) {
                    float jumpPower = this.getJumpPower() * this.getJumpPower() * 6.25F;
                    if (this.hasEffect(MobEffects.JUMP)) {
                        jumpPower += (float) (this.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.75F;
                    }

                    float scaleFactor = Math.max(speed * 2.718281828F, 1.0F / invSqrtLength);
                    Vec3 targetPosition = adjustedPosition.add(normalizedDirection.scale(scaleFactor));
                    float width = this.getBbWidth();
                    float height = this.getBbHeight();
                    AABB boundingBox = new AABB(currentPosition, targetPosition.add(0.0D, height, 0.0D)).inflate(width, 0.0D, width);

                    Vec3 currentPlusHalfY = currentPosition.add(0.0D, 0.5, 0.0D);
                    Vec3 targetPlusHalfY = targetPosition.add(0.0D, 0.5, 0.0D);
                    Vec3 crossProduct = normalizedDirection.cross(new Vec3(0.0D, 1.0D, 0.0D));
                    Vec3 halfWidthVec = crossProduct.scale(width * 0.5F);

                    Vec3 lowerLeft = currentPlusHalfY.subtract(halfWidthVec);
                    Vec3 lowerRight = targetPlusHalfY.subtract(halfWidthVec);
                    Vec3 upperLeft = currentPlusHalfY.add(halfWidthVec);
                    Vec3 upperRight = targetPlusHalfY.add(halfWidthVec);

                    Iterable<VoxelShape> collisions = this.level().getCollisions(this, boundingBox);
                    Iterator<AABB> collisionIterator = StreamSupport.stream(collisions.spliterator(), false)
                            .flatMap(voxelShape -> voxelShape.toAabbs().stream()).iterator();

                    float maxY = Float.MIN_VALUE;

                    while (collisionIterator.hasNext()) {
                        AABB collisionBox = collisionIterator.next();
                        if (collisionBox.intersects(lowerLeft, lowerRight) || collisionBox.intersects(upperLeft, upperRight)) {
                            maxY = (float) collisionBox.maxY;
                            Vec3 collisionCenter = collisionBox.getCenter();
                            BlockPos collisionPos = BlockPos.containing(collisionCenter);
                            int step = 1;

                            while (step <= jumpPower) {
                                BlockPos stepPos = collisionPos.above(step);
                                BlockState stepBlockState = this.level().getBlockState(stepPos);
                                VoxelShape stepShape = stepBlockState.getCollisionShape(this.level(), stepPos, collisionContext);

                                if (!stepShape.isEmpty()) {
                                    maxY = (float) stepShape.max(Direction.Axis.Y) + (float) stepPos.getY();
                                    if ((double) maxY - this.getY() > (double) jumpPower) {
                                        return;
                                    }
                                }

                                if (step > 1) {
                                    BlockPos aboveStepPos = stepPos.above();
                                    BlockState aboveStepBlockState = this.level().getBlockState(aboveStepPos);
                                    if (!aboveStepBlockState.getCollisionShape(this.level(), aboveStepPos, collisionContext).isEmpty()) {
                                        return;
                                    }
                                }

                                ++step;
                            }
                        }
                    }

                    if (maxY != Float.MIN_VALUE) {
                        float heightDiff = (float) ((double) maxY - this.getY());
                        if (heightDiff > this.maxUpStep() && heightDiff <= jumpPower) {
                            this.autoJumpTime = 1;
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "aiStep", at = @At(value = "HEAD"))
    private void checkAutoJump(CallbackInfo ci) {
        if (autoJumpTime > 0) {
            --this.autoJumpTime;
            this.getJumpControl().jump();
            this.getMoveControl().operation = MoveControl.Operation.JUMPING;
        }

    }

    @Override
    public void move(MoverType moverType, Vec3 vec3) {
        double d = this.getX();
        double e = this.getZ();
        super.move(moverType, vec3);
        this.updateAutoJump((float)(this.getX() - d), (float)(this.getZ() - e));
    }


    @Inject(method = "tickHeadTurn", at = @At("HEAD"), cancellable = true)
    public void tickHeadTurn(float f, float g, CallbackInfoReturnable ci) {
        g = super.tickHeadTurn(f, g);
        bodyRotationControl.clientTick();
        ci.setReturnValue(g);

    }
}
