package net.jukitsumc.jukmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.jukitsumc.jukmod.entity.RangedAttackHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
    @Redirect(method="shootProjectile", at=@At(value="INVOKE", target="Lnet/minecraft/world/item/CrossbowItem;getProjectileShotVector(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;F)Lorg/joml/Vector3f;"))
    public Vector3f getInitialVector(LivingEntity me, Vec3 vec3, float f, LivingEntity livingEntity, Projectile projectile, int i, float arrowVelocity, float g, float h, @Nullable LivingEntity livingEntity2) {
        Vec3 vector = RangedAttackHandler.getInitialVector(livingEntity, livingEntity2, projectile.getY(), arrowVelocity);
        return new Vector3f((float)vector.x, (float)vector.y, (float)vector.z);

    }
}
