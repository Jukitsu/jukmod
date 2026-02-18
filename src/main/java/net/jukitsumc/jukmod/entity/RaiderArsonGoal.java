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
import java.util.function.BiFunction;

public class RaiderArsonGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private final int searchRadius;
    private BlockPos targetBlock;
    private int tryTicks = 0;

    private final int MAX_TRYING_TICKS = 160;
    private final int COOLDOWN_TICKS = 80;
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
        this.tryTicks = MAX_TRYING_TICKS; // 100 ticks = 5 seconds
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
        if (this.targetBlock == null) {
            return;
        };

        this.mob.getLookControl().setLookAt(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
        if (this.mob.getBlockPosBelowThatAffectsMyMovement().above().distSqr(targetBlock) < 9.0) {
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
            this.mob.getLookControl().setLookAt(firePos.getX(), firePos.getY(), firePos.getZ());
            this.mob.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    private BlockPos findFlammableBlock() {
        Level level = mob.level();
        BlockPos mobPos = mob.blockPosition();
        int radius = searchRadius;
        int DENSITY_RADIUS = 5;      // size of neighbourhood for cluster density
        int FIRE_RADIUS = 10;        // blocks to check for existing fire (tune as needed)

        // Bounding box for flammable search (original radius)
        int sMinX = mobPos.getX() - radius;
        int sMinY = mobPos.getY() - radius;
        int sMinZ = mobPos.getZ() - radius;
        int sMaxX = mobPos.getX() + radius;
        int sMaxY = mobPos.getY() + radius;
        int sMaxZ = mobPos.getZ() + radius;

        // Expanded bounding box to include fire that may affect neighbourhood checks
        int fMinX = sMinX - FIRE_RADIUS;
        int fMinY = sMinY - FIRE_RADIUS;
        int fMinZ = sMinZ - FIRE_RADIUS;
        int fMaxX = sMaxX + FIRE_RADIUS;
        int fMaxY = sMaxY + FIRE_RADIUS;
        int fMaxZ = sMaxZ + FIRE_RADIUS;

        int sSizeX = sMaxX - sMinX + 1;
        int sSizeY = sMaxY - sMinY + 1;
        int sSizeZ = sMaxZ - sMinZ + 1;
        int fSizeX = fMaxX - fMinX + 1;
        int fSizeY = fMaxY - fMinY + 1;
        int fSizeZ = fMaxZ - fMinZ + 1;

        // Grids: 1 = flammable (search area only), 1 = fire (expanded area)
        int[][][] flammable = new int[sSizeX][sSizeY][sSizeZ];
        int[][][] fire = new int[fSizeX][fSizeY][fSizeZ];

        // Fill flammable grid (only blocks inside original search radius)
        for (int x = sMinX; x <= sMaxX; x++) {
            for (int y = sMinY; y <= sMaxY; y++) {
                for (int z = sMinZ; z <= sMaxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.is(Blocks.FIRE) &&
                            FlammableBlockRegistry.getDefaultInstance().get(state.getBlock()).getSpreadChance() > 0) {
                        flammable[x - sMinX][y - sMinY][z - sMinZ] = 1;
                    }
                }
            }
        }

        // Fill fire grid (expanded area)
        for (int x = fMinX; x <= fMaxX; x++) {
            for (int y = fMinY; y <= fMaxY; y++) {
                for (int z = fMinZ; z <= fMaxZ; z++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.FIRE)) {
                        fire[x - fMinX][y - fMinY][z - fMinZ] = 1;
                    }
                }
            }
        }

        // Build summed‑area tables for O(1) cube counts
        int[][][] sumFlammable = buildSumTable(flammable, sSizeX, sSizeY, sSizeZ);
        int[][][] sumFire = buildSumTable(fire, fSizeX, fSizeY, fSizeZ);

        // Helper to count flammable blocks in a cube (indices in flammable grid)
        BiFunction<int[], int[], Integer> countFlammable = (p1, p2) -> {
            int x1 = p1[0], y1 = p1[1], z1 = p1[2];
            int x2 = p2[0], y2 = p2[1], z2 = p2[2];
            x1 = Math.max(0, x1); y1 = Math.max(0, y1); z1 = Math.max(0, z1);
            x2 = Math.min(sSizeX - 1, x2); y2 = Math.min(sSizeY - 1, y2); z2 = Math.min(sSizeZ - 1, z2);
            if (x1 > x2 || y1 > y2 || z1 > z2) return 0;
            return sumFlammable[x2+1][y2+1][z2+1]
                    - sumFlammable[x1][y2+1][z2+1]
                    - sumFlammable[x2+1][y1][z2+1]
                    - sumFlammable[x2+1][y2+1][z1]
                    + sumFlammable[x1][y1][z2+1]
                    + sumFlammable[x1][y2+1][z1]
                    + sumFlammable[x2+1][y1][z1]
                    - sumFlammable[x1][y1][z1];
        };

        // Helper to count fire blocks in a cube (indices in fire grid)
        BiFunction<int[], int[], Integer> countFire = (p1, p2) -> {
            int x1 = p1[0], y1 = p1[1], z1 = p1[2];
            int x2 = p2[0], y2 = p2[1], z2 = p2[2];
            x1 = Math.max(0, x1); y1 = Math.max(0, y1); z1 = Math.max(0, z1);
            x2 = Math.min(fSizeX - 1, x2); y2 = Math.min(fSizeY - 1, y2); z2 = Math.min(fSizeZ - 1, z2);
            if (x1 > x2 || y1 > y2 || z1 > z2) return 0;
            return sumFire[x2+1][y2+1][z2+1]
                    - sumFire[x1][y2+1][z2+1]
                    - sumFire[x2+1][y1][z2+1]
                    - sumFire[x2+1][y2+1][z1]
                    + sumFire[x1][y1][z2+1]
                    + sumFire[x1][y2+1][z1]
                    + sumFire[x2+1][y1][z1]
                    - sumFire[x1][y1][z1];
        };

        // Evaluate every flammable block in the search area
        int bestCleanCount = 0;
        double bestCleanDistSq = Double.MAX_VALUE;
        BlockPos bestCleanBlock = null;

        int bestAnyCount = 0;
        double bestAnyDistSq = Double.MAX_VALUE;
        BlockPos bestAnyBlock = null;

        for (int x = sMinX; x <= sMaxX; x++) {
            for (int y = sMinY; y <= sMaxY; y++) {
                for (int z = sMinZ; z <= sMaxZ; z++) {
                    if (flammable[x - sMinX][y - sMinY][z - sMinZ] == 0) continue;

                    // Density: count flammable blocks in DENSITY_RADIUS cube (search grid coordinates)
                    int cx = x - sMinX;
                    int cy = y - sMinY;
                    int cz = z - sMinZ;
                    int density = countFlammable.apply(
                            new int[]{cx - DENSITY_RADIUS, cy - DENSITY_RADIUS, cz - DENSITY_RADIUS},
                            new int[]{cx + DENSITY_RADIUS, cy + DENSITY_RADIUS, cz + DENSITY_RADIUS}
                    );

                    // Fire count: count fire blocks in FIRE_RADIUS cube (fire grid coordinates)
                    int fx = x - fMinX;
                    int fy = y - fMinY;
                    int fz = z - fMinZ;
                    int fireCount = countFire.apply(
                            new int[]{fx - FIRE_RADIUS, fy - FIRE_RADIUS, fz - FIRE_RADIUS},
                            new int[]{fx + FIRE_RADIUS, fy + FIRE_RADIUS, fz + FIRE_RADIUS}
                    );

                    double distSq = mobPos.distSqr(new BlockPos(x, y, z));

                    if (fireCount == 0) {
                        // No fire nearby – preferred candidate
                        if (density > bestCleanCount || (density == bestCleanCount && distSq < bestCleanDistSq)) {
                            bestCleanCount = density;
                            bestCleanDistSq = distSq;
                            bestCleanBlock = new BlockPos(x, y, z);
                        }
                    } else {
                        // Fire already present in the neighbourhood – fallback candidate
                        if (density > bestAnyCount || (density == bestAnyCount && distSq < bestAnyDistSq)) {
                            bestAnyCount = density;
                            bestAnyDistSq = distSq;
                            bestAnyBlock = new BlockPos(x, y, z);
                        }
                    }
                }
            }
        }

        // Return the best clean block if any exists; otherwise fallback to a fire‑nearby block
        return bestCleanBlock != null ? bestCleanBlock : bestAnyBlock;
    }

    /**
     * Builds a 3D summed‑area table (prefix sums) for O(1) cuboid queries.
     */
    private int[][][] buildSumTable(int[][][] grid, int sizeX, int sizeY, int sizeZ) {
        int[][][] sum = new int[sizeX + 1][sizeY + 1][sizeZ + 1];
        for (int i = 0; i < sizeX; i++) {
            for (int j = 0; j < sizeY; j++) {
                for (int k = 0; k < sizeZ; k++) {
                    sum[i+1][j+1][k+1] = grid[i][j][k]
                            + sum[i][j+1][k+1]
                            + sum[i+1][j][k+1]
                            + sum[i+1][j+1][k]
                            - sum[i][j][k+1]
                            - sum[i][j+1][k]
                            - sum[i+1][j][k]
                            + sum[i][j][k];
                }
            }
        }
        return sum;
    }
}