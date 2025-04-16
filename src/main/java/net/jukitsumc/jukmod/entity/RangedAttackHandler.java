package net.jukitsumc.jukmod.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Overwrite;

public class RangedAttackHandler {

    private static final double arrowGravity = 0.05D;

    public static Vec3 getInitialVector(Mob me, LivingEntity livingEntity, Projectile projectile, double arrowVelocity) {
        // Get the vector this -> target
        double x = livingEntity.getX() - me.getX();
        double y = livingEntity.getEyeY() - projectile.getY();
        double z = livingEntity.getZ() - me.getZ();

        double d = Math.hypot(x, z);

        if (d < 1e-6) {
            return new Vec3(x, y, z); // Shoot straight
        }

        // Predict their movement at arrow landing, assuming the trajectory being straight
        double dt = d / arrowVelocity;

        Vec3 ds = livingEntity.getDeltaMovement().scale(dt); // ds = v * dt = v * ds'/dv'
        double px = x + ds.x;
        double py = y + Math.min(0.0D, ds.y);
        double pz = z + ds.z;

        double pd2 = px * px + pz * pz;
        double pd = Math.sqrt(pd2);

        // Calculate the initial vertical velocity, defining the curve of the trajectory
        // This took me way too long
        // Now I understand why Skeletons don't have full-proof aimbots
        double pdt = Math.sqrt(px * px + py * py + pz * pz) / arrowVelocity;
        double integratedFriction = (1 - Math.pow(0.99D, pdt)) / (0.01005033585D * pdt);
        double adjustedArrowVelocity = arrowVelocity * integratedFriction;

        double a = 1 + (py * py) / pd2;
        double b = arrowGravity * py - adjustedArrowVelocity * adjustedArrowVelocity;
        double delta = b * b - a * arrowGravity * arrowGravity * pd2;

        if (delta < 0) {
            return new Vec3(x, y, z); // Shoot straight
        }

        double vdSqr = (-b + Math.sqrt(delta)) / (2.0D * a);

        if (vdSqr < 0) {
            return new Vec3(x, y, z); // Shoot straight
        }

        double sign = Math.signum(arrowGravity * pd * 0.5D + py * vdSqr / pd);
        double vd = Math.sqrt(vdSqr);

        if (adjustedArrowVelocity * adjustedArrowVelocity - vdSqr < 0) {
            return new Vec3(x, y, z); // Shoot straight
        }

        double vy = sign * Math.sqrt(adjustedArrowVelocity * adjustedArrowVelocity - vdSqr);


        // Project the result on the 3D space using trigonometry hacks
        double alpha = Math.atan2(pz, px);
        double vx = vd * Math.cos(alpha);
        double vz = vd * Math.sin(alpha);

        return new Vec3(vx, vy, vz);
    }
}
