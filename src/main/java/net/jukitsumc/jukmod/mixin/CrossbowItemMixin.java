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
        double x = livingEntity2.getX() - livingEntity.getX();
        double z = livingEntity2.getZ() - livingEntity.getZ();
        double y = livingEntity2.getEyeY() - projectile.getY();
        Vec3 vector = RangedAttackHandler.getInitialVector(livingEntity, livingEntity2, y, 3.15F);
        if (arrowVelocity > 3.0f) {
            return new Vector3f((float)vector.x, (float)vector.y, (float)vector.z);
        } else {
            double d = Math.hypot(x, z);

            if (d < 1e-6) {
                return new Vector3f((float)x, (float)y, (float)z); // Shoot straight
            }

            // Predict their movement at arrow landing, assuming the trajectory being straight
            double dt = d / arrowVelocity;

            Vec3 ds = livingEntity.getKnownMovement().scale(dt); // ds = v * dt = v * ds'/dv'
            double px = x + ds.x;
            double py = y + Math.min(0.0D, ds.y);
            double pz = z + ds.z;

            return new Vector3f((float)px, (float)py, (float)pz);
        }

    }
}
