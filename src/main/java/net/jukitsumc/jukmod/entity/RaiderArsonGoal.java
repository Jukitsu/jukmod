package net.jukitsumc.jukmod.entity;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.jukitsumc.jukmod.Jukmod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class RaiderArsonGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private final int searchRadius;
    private BlockPos targetBlock;
    private int tryTicks = 0;

    private final int COOLDOWN_TICKS = 400;
    private int nextSearchTick = 0;

    /**
     * @param mob           the entity using this goal
     * @param speedModifier movement speed while pathfinding
     * @param searchRadius  radius (in blocks) to search for flammable blocks
     */
    public RaiderArsonGoal(Mob mob, double speedModifier, int searchRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.searchRadius = searchRadius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.nextSearchTick = mob.tickCount;
    }

    @Override
    public boolean canUse() {
        Raider raider = (Raider) (this.mob);
        if (!raider.hasActiveRaid()) {
            return false;
        }

        if (mob.tickCount < nextSearchTick) {
            return false;
        }

        this.targetBlock = findFlammableBlock();
        return this.targetBlock != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetBlock == null) return false;

        Level level = mob.level();
        BlockState state = level.getBlockState(targetBlock);
        // Stop if block is gone, already fire, or no longer flammable
        if (state.isAir() || state.is(Blocks.FIRE) || FlammableBlockRegistry.getDefaultInstance().get(state.getBlock()).getSpreadChance() <= 0) {
            return false;
        }
        // Give up after ~3 seconds of trying
        if (tryTicks-- <= 0) {
            this.tryTicks = 0;
            return false;
        }
        // Stop if navigation finishes (meaning we can't reach the target)
        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ(), speedModifier);
        this.tryTicks = 60; // 100 ticks = 5 seconds
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        this.targetBlock = null;
        this.nextSearchTick = mob.tickCount + COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        // If close enough (within 2 blocks), ignite the target
        if (this.targetBlock != null && this.mob.getBlockPosBelowThatAffectsMyMovement().above().distSqr(targetBlock) < 4.0) {
            igniteTarget();
            stop(); // stop after igniting
        }
    }


    private void igniteTarget() {
        Level level = mob.level();
        BlockPos mobPos = mob.blockPosition();

        // Compute direction from mob to target block (simplified: horizontal direction)
        Direction approachDir = Direction.getNearest(
                targetBlock.getX() - mobPos.getX(),
                0,
                targetBlock.getZ() - mobPos.getZ(),
                Direction.UP
        );

        // The block adjacent to the target in the direction the mob is approaching from
        BlockPos firePos = targetBlock.relative(approachDir.getOpposite());

        // Check if that block is air and the target is still flammable
        BlockState targetState = level.getBlockState(targetBlock);
        if (targetState.isAir() || targetState.is(Blocks.FIRE) || FlammableBlockRegistry.getDefaultInstance().get(targetState.getBlock()).getSpreadChance() <= 0) {
            return; // target no longer valid
        }

        if (level.isEmptyBlock(firePos)) {
            // Place fire in the adjacent air block; it will attach to the target block
            level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
            this.mob.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    private BlockPos findFlammableBlock() {
        Level level = mob.level();
        BlockPos mobPos = mob.blockPosition();
        int radius = searchRadius;
        final int NEARBY_FIRE_RADIUS = 5; // prefer blocks not near fire

        // Expand scan by the fire radius to catch fire just outside the original cube
        int expandedRadius = radius + NEARBY_FIRE_RADIUS;
        BlockPos base = mobPos.offset(-expandedRadius, -expandedRadius, -expandedRadius);
        int range = 2 * expandedRadius + 1;
        boolean[][][] fireGrid = new boolean[range][range][range];

        List<BlockPos> candidates = new ArrayList<>();

        // First pass: record fire positions and collect flammable candidates
        for (int dx = -expandedRadius; dx <= expandedRadius; dx++) {
            for (int dy = -expandedRadius; dy <= expandedRadius; dy++) {
                for (int dz = -expandedRadius; dz <= expandedRadius; dz++) {
                    BlockPos pos = mobPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    // Mark fire blocks in the grid
                    if (state.is(Blocks.FIRE)) {
                        int ix = dx + expandedRadius;
                        int iy = dy + expandedRadius;
                        int iz = dz + expandedRadius;
                        fireGrid[ix][iy][iz] = true;
                    }

                    // Only consider candidates within the original search radius
                    if (Math.abs(dx) <= radius && Math.abs(dy) <= radius && Math.abs(dz) <= radius) {
                        if (!state.isAir() && !state.is(Blocks.FIRE) &&
                                FlammableBlockRegistry.getDefaultInstance().get(state.getBlock()).getSpreadChance() > 0 &&
                                FlammableBlockRegistry.getDefaultInstance().get(state.getBlock()).getBurnChance() > 0) {
                            candidates.add(pos.immutable());
                        }
                    }
                }
            }
        }

        // Evaluate candidates: separate those near fire from those not near fire
        BlockPos bestClean = null;
        double bestCleanDistSq = Double.MAX_VALUE;
        BlockPos bestAny = null;
        double bestAnyDistSq = Double.MAX_VALUE;

        for (BlockPos candidate : candidates) {
            double distSq = mobPos.distSqr(candidate);
            boolean nearFire = false;

            int cx = candidate.getX();
            int cy = candidate.getY();
            int cz = candidate.getZ();

            // Check for any fire within NEARBY_FIRE_RADIUS of this candidate
            searchNearFire:
            for (int ox = -NEARBY_FIRE_RADIUS; ox <= NEARBY_FIRE_RADIUS; ox++) {
                for (int oy = -NEARBY_FIRE_RADIUS; oy <= NEARBY_FIRE_RADIUS; oy++) {
                    for (int oz = -NEARBY_FIRE_RADIUS; oz <= NEARBY_FIRE_RADIUS; oz++) {
                        int wx = cx + ox;
                        int wy = cy + oy;
                        int wz = cz + oz;
                        int ix = wx - base.getX();
                        int iy = wy - base.getY();
                        int iz = wz - base.getZ();
                        if (ix >= 0 && ix < range && iy >= 0 && iy < range && iz >= 0 && iz < range) {
                            if (fireGrid[ix][iy][iz]) {
                                nearFire = true;
                                break searchNearFire;
                            }
                        }
                    }
                }
            }

            if (!nearFire) {
                if (distSq < bestCleanDistSq) {
                    bestCleanDistSq = distSq;
                    bestClean = candidate;
                }
            } else {
                if (distSq < bestAnyDistSq) {
                    bestAnyDistSq = distSq;
                    bestAny = candidate;
                }
            }
        }

        // Prefer a clean block; otherwise take any block (even if near fire)
        return bestClean != null ? bestClean : bestAny;
    }
}