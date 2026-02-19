package net.jukitsumc.jukmod.entity;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.jukitsumc.jukmod.Jukmod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.BiFunction;

public class RaiderArsonGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private final int searchRadius;
    private BlockPos targetBlock;
    private BlockPos firePos;
    private int tryTicks = 0;

    private final int MAX_TRYING_TICKS = 120;
    private final int COOLDOWN_TICKS = 60;
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
        if (!raider.hasActiveRaid() && !raider.hasPatrolTarget()) {
            return false;
        }

        if ((mob.tickCount < nextSearchTick && !raider.isCelebrating()) || this.mob.getRandom().nextFloat() > 0.2F) {
            return false;
        }

        Tuple<BlockPos, BlockPos> target = findFlammableBlock();
        if (target != null) {
            this.targetBlock = target.getB();
            this.firePos = target.getA();
        }

        return target != null;
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
        this.tryTicks = MAX_TRYING_TICKS;
        this.nextSearchTick = mob.tickCount + COOLDOWN_TICKS;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        this.targetBlock = null;

    }

    @Override
    public void tick() {
        // If close enough (within 2 blocks), ignite the target
        if (this.targetBlock == null) {
            return;
        };

        this.mob.getLookControl().setLookAt(firePos.getX(), firePos.getY(), firePos.getZ());
        if (this.mob.getBlockPosBelowThatAffectsMyMovement().above().distSqr(targetBlock) < 9.0) {
            int success = igniteTarget();
            if (success == -1) {
                this.nextSearchTick -= COOLDOWN_TICKS / 2; // Try another target
            }
            stop(); // stop after igniting
        }
    }


    private int igniteTarget() {
        Level level = mob.level();

        BlockState targetState = level.getBlockState(targetBlock);
        if (targetState.isAir() || targetState.is(Blocks.FIRE) || FlammableBlockRegistry.getDefaultInstance().get(targetState.getBlock()).getSpreadChance() <= 0) {
            return -1; // target no longer valid
        }

        if (level.isEmptyBlock(firePos)) {
            // Place fire in the adjacent air block; it will attach to the target block
            level.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
            this.mob.getLookControl().setLookAt(firePos.getX(), firePos.getY(), firePos.getZ());
            this.mob.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
            return 1;
        }

        return -1;
    }

    private boolean isArsonable(BlockState state) {
        int spreadChance = FlammableBlockRegistry.getDefaultInstance().get(state.getBlock()).getSpreadChance();
        int burnChance = FlammableBlockRegistry.getDefaultInstance().get(state.getBlock()).getBurnChance();
        return !state.isAir() && !state.is(Blocks.FIRE) && (burnChance == 5 || burnChance == 30 || state.getBlock() == Blocks.TNT
                || (burnChance == 60 && spreadChance <= 20));
    }
    private static final int CLUSTER_RADIUS = 6; // Max Chebyshev distance to consider blocks "connected"

    private static final int FIRE_RADIUS = 2;     // Blocks to check for existing fire near a cluster

    private Tuple<BlockPos, BlockPos> findFlammableBlock() {
        Level level = mob.level();
        BlockPos mobPos = mob.blockPosition();
        int radius = searchRadius;

        // Bounding box for flammable search
        int sMinX = mobPos.getX() - radius;
        int sMinY = mobPos.getY() - radius;
        int sMinZ = mobPos.getZ() - radius;
        int sMaxX = mobPos.getX() + radius;
        int sMaxY = mobPos.getY() + radius;
        int sMaxZ = mobPos.getZ() + radius;

        // Expanded bounding box for fire detection (to catch fires just outside the search area)
        int fMinX = sMinX - FIRE_RADIUS;
        int fMinY = sMinY - FIRE_RADIUS;
        int fMinZ = sMinZ - FIRE_RADIUS;
        int fMaxX = sMaxX + FIRE_RADIUS;
        int fMaxY = sMaxY + FIRE_RADIUS;
        int fMaxZ = sMaxZ + FIRE_RADIUS;

        // Collect all flammable blocks within the search area
        List<BlockPos> flammableList = new ArrayList<>();
        Set<BlockPos> flammableSet = new HashSet<>(); // for O(1) boundary checks

        for (int x = sMinX; x <= sMaxX; x++) {
            for (int y = sMinY; y <= sMaxY; y++) {
                for (int z = sMinZ; z <= sMaxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);


                    if (isArsonable(state)) {
                        flammableList.add(pos);
                        flammableSet.add(pos);
                    }
                }
            }
        }

        if (flammableList.isEmpty()) {
            return null;
        }

        // Collect all fire blocks within the expanded area (for fire‑near‑cluster checks)
        Set<BlockPos> fireSet = new HashSet<>();
        for (int x = fMinX; x <= fMaxX; x++) {
            for (int y = fMinY; y <= fMaxY; y++) {
                for (int z = fMinZ; z <= fMaxZ; z++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.FIRE)) {
                        fireSet.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        // Cluster flammable blocks using Union‑Find (Chebyshev distance ≤ CLUSTER_RADIUS)
        int n = flammableList.size();
        UnionFind uf = new UnionFind(n);

        for (int i = 0; i < n; i++) {
            BlockPos a = flammableList.get(i);
            for (int j = i + 1; j < n; j++) {
                BlockPos b = flammableList.get(j);
                int dx = Math.abs(a.getX() - b.getX());
                int dy = Math.abs(a.getY() - b.getY());
                int dz = Math.abs(a.getZ() - b.getZ());
                if (dx <= CLUSTER_RADIUS && dy <= CLUSTER_RADIUS && dz <= CLUSTER_RADIUS) {
                    uf.union(i, j);
                }
            }
        }

        // Group positions by cluster root
        Map<Integer, List<BlockPos>> clusters = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = uf.find(i);
            clusters.computeIfAbsent(root, k -> new ArrayList<>()).add(flammableList.get(i));
        }

        // Separate clusters into those with fire nearby and those without
        List<List<BlockPos>> cleanClusters = new ArrayList<>();
        List<List<BlockPos>> dirtyClusters = new ArrayList<>();

        for (List<BlockPos> cluster : clusters.values()) {
            boolean hasFireNearby = false;

            // Compute bounding box of the cluster
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos p : cluster) {
                if (p.getX() < minX) minX = p.getX();
                if (p.getY() < minY) minY = p.getY();
                if (p.getZ() < minZ) minZ = p.getZ();
                if (p.getX() > maxX) maxX = p.getX();
                if (p.getY() > maxY) maxY = p.getY();
                if (p.getZ() > maxZ) maxZ = p.getZ();
            }

            // Expand bounding box by FIRE_RADIUS
            int checkMinX = minX - FIRE_RADIUS;
            int checkMinY = minY - FIRE_RADIUS;
            int checkMinZ = minZ - FIRE_RADIUS;
            int checkMaxX = maxX + FIRE_RADIUS;
            int checkMaxY = maxY + FIRE_RADIUS;
            int checkMaxZ = maxZ + FIRE_RADIUS;

            // Check if any fire lies within the expanded box
            for (BlockPos firePos : fireSet) {
                if (firePos.getX() >= checkMinX && firePos.getX() <= checkMaxX &&
                        firePos.getY() >= checkMinY && firePos.getY() <= checkMaxY &&
                        firePos.getZ() >= checkMinZ && firePos.getZ() <= checkMaxZ) {
                    hasFireNearby = true;
                    break;
                }
            }

            if (!hasFireNearby) {
                cleanClusters.add(cluster);
            }

            else {
               // dirtyClusters.add(cluster);
            }

        }


        // Choose the largest cluster, preferring clean ones
        List<BlockPos> targetCluster;
        if (!cleanClusters.isEmpty()) {
            targetCluster = Collections.max(cleanClusters, Comparator.comparingInt(List::size));
        } else if (!dirtyClusters.isEmpty()) {
            targetCluster = Collections.max(dirtyClusters, Comparator.comparingInt(List::size));
        } else {
            return null; // Should never happen because we already checked flammableList not empty
        }



        // Determine boundary blocks of the largest cluster (strict 6‑neighbor adjacency to non‑flammable)
        Set<Tuple<BlockPos, BlockPos>> boundary = new HashSet<>();
        for (BlockPos pos : targetCluster) {
            for (Direction dir : Direction.values()) { // 6 directions
                BlockPos neighbor = pos.relative(dir);
                if (level.getBlockState(neighbor).isAir()) {
                    boundary.add(new Tuple<>(neighbor, pos));
                    break; // No need to check other directions for this block
                }
            }
        }

        // If for some reason there is no boundary (e.g., cluster fills entire search area), fallback to any block
        if (boundary.isEmpty()) {
            return null;
        }

        // From the boundary, pick the one closest to the mob
        Tuple<BlockPos, BlockPos> bestTarget = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Tuple<BlockPos, BlockPos> couple: boundary) {
            BlockPos pos = couple.getB();
            double distSq = mobPos.distSqr(pos);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestTarget = couple;
            }
        }

        return bestTarget;
    }

    // Simple Union-Find helper
    private static class UnionFind {
        private final int[] parent;
        private final int[] rank;

        public UnionFind(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) parent[i] = i;
        }

        public int find(int x) {
            while (parent[x] != x) {
                parent[x] = parent[parent[x]];
                x = parent[x];
            }
            return x;
        }

        public void union(int x, int y) {
            int rx = find(x);
            int ry = find(y);
            if (rx == ry) return;
            if (rank[rx] < rank[ry]) {
                parent[rx] = ry;
            } else if (rank[rx] > rank[ry]) {
                parent[ry] = rx;
            } else {
                parent[ry] = rx;
                rank[rx]++;
            }
        }
    }

}