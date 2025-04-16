package net.jukitsumc.jukmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.jukitsumc.jukmod.entity.RangedAttackHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
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
        return original.add(Attributes.FOLLOW_RANGE, 32.0F);
    }

    @Inject(method="reassessWeaponGoal", at=@At("HEAD"), cancellable = true)
    public void reassessWeaponGoal(CallbackInfo ci) {
        if (this.level() != null && !this.level().isClientSide) {
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            this.goalSelector.removeGoal(this.newBowGoal);
            ItemStack itemStack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
            if (itemStack.is(Items.BOW)) {
                int i = 20;
                if (this.level().getDifficulty() != Difficulty.HARD) {
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

    @Shadow protected abstract AbstractArrow getArrow(ItemStack itemStack, float f);

    @Inject(method="performRangedAttack", at=@At("HEAD"), cancellable = true)
    public void performRangedAttack(LivingEntity livingEntity, float f, CallbackInfo ci) {
        ItemStack itemStack = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW)));

        AbstractArrow abstractArrow = this.getArrow(itemStack, f);

        if (this.getRandom().nextInt(20 - this.level().getDifficulty().getId() * 4) >= 1) {
            Vec3 v = RangedAttackHandler.getInitialVector(this, livingEntity, abstractArrow, 1.6);
            abstractArrow.shoot(v.x, v.y, v.z, 1.6F, (float)(12 - this.level().getDifficulty().getId() * 4));
        } else {
            Vec3 v = RangedAttackHandler.getInitialVector(this, livingEntity, abstractArrow, 3.0);
            abstractArrow.shoot(v.x, v.y, v.z, 3.0F, (float)(Math.max(0.0D, 8 - this.level().getDifficulty().getId() * 4)));
            if (this.getRandom().nextFloat() >= 0.5F) {
                abstractArrow.setCritArrow(true);
            }
        }


        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(abstractArrow);
        ci.cancel();
    }
}
