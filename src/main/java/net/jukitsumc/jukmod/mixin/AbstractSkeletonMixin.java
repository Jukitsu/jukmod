package net.jukitsumc.jukmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.jukitsumc.jukmod.entity.RangedAttackHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin extends Monster {
    @Shadow @Final private RangedBowAttackGoal<AbstractSkeleton> bowGoal;
    @Shadow @Final private MeleeAttackGoal meleeGoal;
    private final RangedBowAttackGoal<AbstractSkeleton> newBowGoal = new RangedBowAttackGoal(this, 1.0D, 20, 32.0F);

    protected AbstractSkeletonMixin(EntityType<? extends AbstractSkeleton> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyReturnValue(method="createAttributes", at=@At("TAIL"))
    private static AttributeSupplier.Builder modifyAttributes(AttributeSupplier.Builder original) {
        return original.add(Attributes.FOLLOW_RANGE, 64.0F);
    }

    @Inject(method="reassessWeaponGoal", at=@At("HEAD"), cancellable = true)
    public void reassessWeaponGoal(CallbackInfo ci) {
        if (this.level() != null && !this.level().isClientSide()) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            this.goalSelector.removeGoal(this.newBowGoal);
            ItemStack itemStack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
            if (itemStack.is(Items.BOW)) {
                int i = 10;
                if (this.level().getDifficulty() == Difficulty.EASY) {
                    i = 40;
                }

                this.newBowGoal.setMinAttackInterval(i);
                this.goalSelector.addGoal(4, this.newBowGoal);
            } else {
                this.goalSelector.addGoal(4, this.meleeGoal);
            }


        }
        ci.cancel();
    }

    @Shadow protected abstract AbstractArrow getArrow(ItemStack itemStack, float f, @Nullable ItemStack itemStack2);

    @Inject(method="performRangedAttack", at=@At("HEAD"), cancellable = true)
    public void performRangedAttack(LivingEntity livingEntity, float f, CallbackInfo ci) {
        ItemStack itemStack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
        ItemStack itemStack2 = this.getProjectile(itemStack);
        AbstractArrow abstractArrow = this.getArrow(itemStack2, f, itemStack);
        Level var15 = this.level();
        if (var15 instanceof ServerLevel serverLevel) {
            if ((this.getRandom().nextInt(20 - this.level().getDifficulty().getId() * 4) < 1
                || this.distanceToSqr(livingEntity) > 400) && this.level().getDifficulty().getId() > 2
                    && this.distanceToSqr(livingEntity) > 25.0F) {
                Vec3 v = RangedAttackHandler.getInitialVector(this, livingEntity, abstractArrow.getY(), 3.0);
                this.lookControl.setLookAt(this.getEyePosition().add(v));
                Projectile.spawnProjectileUsingShoot(abstractArrow, serverLevel, itemStack2, v.x, v.y, v.z, 3.0F, (float)(Math.max(0.0D, 8 - this.level().getDifficulty().getId() * 4)));
                if (this.getRandom().nextFloat() >= 0.9F) {
                    abstractArrow.setCritArrow(true);
                }
            }
            else if (this.distanceToSqr(livingEntity) < 25.0F) {
                Vec3 v = RangedAttackHandler.getInitialVector(this, livingEntity, abstractArrow.getY(), 0.8F);
                this.lookControl.setLookAt(this.getEyePosition().add(v));
                Projectile.spawnProjectileUsingShoot(abstractArrow, serverLevel, itemStack2, v.x, v.y, v.z, 0.8F, (float)(12 - this.level().getDifficulty().getId() * 4));
            }
            else {
                Vec3 v = RangedAttackHandler.getInitialVector(this, livingEntity, abstractArrow.getY(), 1.6);
                this.lookControl.setLookAt(this.getEyePosition().add(v));
                Projectile.spawnProjectileUsingShoot(abstractArrow, serverLevel, itemStack2, v.x, v.y, v.z, 1.6F, (float)(12 - this.level().getDifficulty().getId() * 4));
            }
        }

        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));

        ci.cancel();
    }
}
