package net.jukitsumc.jukmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.world.entity.monster.illager.Vindicator$VindicatorBreakDoorGoal")
public abstract class VindicatorBreakDoorGoalMixin extends BreakDoorGoal {

    public VindicatorBreakDoorGoalMixin(Mob mob) {
        super(mob, difficulty -> difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD);

    }
    @Override
    public void tick() {
        super.tick();
/*
        if (this.breakTime >= this.getDoorBreakTime() && this.mob.level().isEmptyBlock(this.doorPos)) {
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
        }
        */

    }
}
