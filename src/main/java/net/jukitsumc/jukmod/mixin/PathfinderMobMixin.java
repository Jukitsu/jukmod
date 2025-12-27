package net.jukitsumc.jukmod.mixin;

import net.jukitsumc.jukmod.entity.AvoidSwollenCreeperGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PathfinderMob.class)
public class PathfinderMobMixin extends Mob {

    protected PathfinderMobMixin(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void fleeSwellingCreeper(EntityType<? extends Mob> entityType, Level level, CallbackInfo info) {
        if (level != null && !level.isClientSide()) {
            this.goalSelector.addGoal(1, new AvoidSwollenCreeperGoal((PathfinderMob) (Object) this, 4.0F, 1.0D, 1.2D));
        }
    }

}
