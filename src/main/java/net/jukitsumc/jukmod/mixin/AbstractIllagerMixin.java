package net.jukitsumc.jukmod.mixin;

import net.jukitsumc.jukmod.Jukmod;
import net.jukitsumc.jukmod.entity.AvoidSwollenCreeperGoal;
import net.jukitsumc.jukmod.entity.RaiderArsonGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractIllager.class)
public abstract class AbstractIllagerMixin extends Raider {

    protected AbstractIllagerMixin(EntityType<? extends AbstractIllager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void addArsonGoal(EntityType<? extends AbstractIllager> entityType, Level level, CallbackInfo info) {
        if (level != null && !level.isClientSide()) {
            this.goalSelector.addGoal(2,
                    new RaiderArsonGoal(this, 1.0D, 30)
            );

        }
    }


}
