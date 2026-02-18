package net.jukitsumc.jukmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.world.entity.monster.illager.AbstractIllager$RaiderOpenDoorGoal")
public abstract class RaiderOpenDoorGoalMixin extends OpenDoorGoal {

    public RaiderOpenDoorGoalMixin(Mob mob, boolean bl) {
        super(mob, bl);
    }
    @Override
    protected void setOpen(boolean bl) {
        super.setOpen(bl);
/*

        Direction raiderFacing = this.mob.getDirection();
        BlockPos firePos = this.doorPos.relative(raiderFacing.getOpposite());
        BlockPos firePos2 = this.mob.getBlockPosBelowThatAffectsMyMovement().above();
        BlockPos firePos3 = firePos.relative(Direction.getRandom(this.mob.getRandom()));


        if (this.mob.level().isEmptyBlock(firePos)) {
            this.mob.level().setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
            this.mob.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
        }
        if (this.mob.level().isEmptyBlock(firePos2)){
            this.mob.level().setBlock(firePos2, Blocks.FIRE.defaultBlockState(), 3);
            this.mob.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
        }
        if (this.mob.level().isEmptyBlock(firePos3)){
            this.mob.level().setBlock(firePos3, Blocks.FIRE.defaultBlockState(), 3);
            this.mob.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
        }


*/
    }
}
