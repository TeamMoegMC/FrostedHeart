/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.reference.FHSoundEvents;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hanging ice only. Connections, not natural-growth length, determine its shape. */
public class IcicleBlock extends Block {
    public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");
    /** Invisible state change invokes the normal material reset after a tip melts. */
    public static final BooleanProperty MELT_CYCLE = BooleanProperty.create("melt_cycle");
    public static final IntegerProperty SHAPE = IntegerProperty.create("shape", 0, 3);
    public static final int TIP = 0, TAIL = 1, MIDDLE = 2, ROOT = 3;
    public static final int MAX_NATURAL_LENGTH = 8;
    public static final double ROOT_CHANCE = .005;
    public static final double HEAT_THRESHOLD_C = 5;
    private static final ResourceLocation ICE_CAVES = new ResourceLocation("the_winter_rescue", "ice_caves");
    private static final VoxelShape[] SHAPES = {
            box(5, 8, 5, 11, 16, 11), box(5, 0, 5, 11, 16, 11),
            box(4, 0, 4, 12, 16, 12), box(3, 0, 3, 13, 16, 13)};

    public IcicleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRACKED, false).setValue(MELT_CYCLE, false).setValue(SHAPE, TIP));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRACKED, MELT_CYCLE, SHAPE);
    }

    public static boolean isIcicle(BlockState state) { return state.getBlock() instanceof IcicleBlock; }

    public static boolean canGrowRootFrom(BlockState state) {
        return state.is(BlockTags.ICE) && !(state.getBlock() instanceof ThinIceBlock);
    }

    public static double growthMultiplier(double temperatureC) {
        return Double.isFinite(temperatureC) && temperatureC <= 0 ? Math.min(2, 1 - .1 * temperatureC) : 0;
    }

    public static double fractureChance(double temperatureC, int below, boolean root) {
        return Double.isFinite(temperatureC) && temperatureC >= HEAT_THRESHOLD_C
                ? Math.min(1, .125 * (1 + (temperatureC - HEAT_THRESHOLD_C) / 5)
                        * (1 + .1 * Math.min(below, 5)) * (root ? 2 : 1)) : 0;
    }

    public static boolean inGrowthBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(ICE_CAVES);
    }

    private static boolean supported(LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        // Unknown unloaded support is not air. Vertical chains stay in one chunk.
        if (!level.hasChunkAt(above)) return true;
        BlockState state = level.getBlockState(above);
        return isIcicle(state) || state.isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return supported(level, pos);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        return supported(context.getLevel(), pos) ? connectedState(defaultBlockState(), context.getLevel(), pos) : null;
    }

    public static BlockState connectedState(BlockState state, LevelReader level, BlockPos pos) {
        if (!isIcicle(level.getBlockState(pos.below()))) return state.setValue(SHAPE, TIP);
        if (!isIcicle(level.getBlockState(pos.above()))) return state.setValue(SHAPE, ROOT);
        return state.setValue(SHAPE, isIcicle(level.getBlockState(pos.below(2))) ? MIDDLE : TAIL);
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (level instanceof ServerLevel server && direction == Direction.UP && !supported(server, pos)) {
            // A support check must supersede an existing long warning timer.
            server.getBlockTicks().clearArea(new BoundingBox(pos));
            server.scheduleTick(pos, this, 1);
        }
        return connectedState(state, level, pos);
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        if (level instanceof ServerLevel server && !previous.is(this)) {
            if (!supported(server, pos)) server.scheduleTick(pos, this, 1);
            else if (state.getValue(CRACKED)) server.scheduleTick(pos, this, 80 + server.random.nextInt(241));
        }
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this) && level instanceof ServerLevel server)
            server.getBlockTicks().clearArea(new BoundingBox(pos));
        super.onRemove(state, level, pos, next, moving);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(SHAPE)];
    }

    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CRACKED)) {
            // A command or legacy/incomplete save can supply warning state without its timer.
            if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 80 + random.nextInt(241));
            return;
        }
        if (random.nextInt(8) == 0) {
            double environment = MinecraftThermalInput.gameplayPassiveEnvironment(level, pos, WorldTemperature.naturalAir(level, pos));
            if (environment >= HEAT_THRESHOLD_C) {
                int below = 0;
                while (below < 5 && isIcicle(level.getBlockState(pos.below(below + 1)))) below++;
                if (random.nextDouble() < fractureChance(environment, below, !isIcicle(level.getBlockState(pos.above())))) {
                    warn(level, pos, random);
                    return;
                }
            }
        }
        // Cheap maximum-probability gate before biome/material reads; the second draw
        // restores exactly 5% at 0 C and 10% at -10 C, independently of heat sampling.
        if (random.nextDouble() < .1 && !isIcicle(level.getBlockState(pos.below()))
                && level.getBlockState(pos.below()).isAir() && inGrowthBiome(level, pos)) {
            int length = 1;
            BlockPos.MutableBlockPos cursor = pos.mutable();
            while (length < MAX_NATURAL_LENGTH && isIcicle(level.getBlockState(cursor.move(Direction.UP)))) {
                if (level.getBlockState(cursor).getValue(CRACKED)) return;
                length++;
            }
            if (length >= MAX_NATURAL_LENGTH) return;
            double temperature = MinecraftPhaseController.temperature(level, pos);
            if (random.nextDouble() < .5 * growthMultiplier(temperature))
                level.setBlockAndUpdate(pos.below(), defaultBlockState());
        }
    }

    /** Called from the shared random-position sampler so packed/blue ice also grow roots. */
    public static void randomRootTick(ServerLevel level, BlockPos substrate, BlockState state, RandomSource random) {
        if (!canGrowRootFrom(state) || random.nextDouble() >= ROOT_CHANCE * 2) return;
        BlockPos root = substrate.below();
        if (!level.getBlockState(root).isAir() || !state.isFaceSturdy(level, substrate, Direction.DOWN)
                || !inGrowthBiome(level, root) || hasNearbyRoot(level, root)) return;
        if (random.nextDouble() < .5 * growthMultiplier(MinecraftPhaseController.temperature(level, substrate)))
            level.setBlockAndUpdate(root, FHBlocks.ICICLE.get().defaultBlockState());
    }

    public static boolean hasNearbyRoot(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = -1; y <= 1; y++) {
            cursor.setWithOffset(pos, x, y, z);
            if (!level.hasChunkAt(cursor)) return true; // Defer until the neighborhood is known.
            if (isIcicle(level.getBlockState(cursor)) && !isIcicle(level.getBlockState(cursor.above()))) return true;
        }
        return false;
    }

    public boolean warn(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || state.getValue(CRACKED)) return false;
        level.setBlockAndUpdate(pos, state.setValue(CRACKED, true));
        level.playSound(null, pos, FHSoundEvents.ICE_CRACKING.get(), SoundSource.BLOCKS, .9F, 1);
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 80 + random.nextInt(561));
        return true;
    }

    public void shatter(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        level.levelEvent(2001, pos, Block.getId(state));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        fallChain(level, pos.below());
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!supported(level, pos)) fallChain(level, pos);
        else if (state.getValue(CRACKED)) shatter(level, pos);
    }

    public static void fallChain(ServerLevel level, BlockPos first) {
        // At most dimension height, with no artificial eight-block truncation.
        int length = 0;
        while (!level.isOutsideBuildHeight(first.below(length)) && isIcicle(level.getBlockState(first.below(length)))) length++;
        BlockState[] states = new BlockState[length];
        for (int i = 0; i < length; i++) states[i] = level.getBlockState(first.below(i));
        for (int i = 0; i < length; i++) {
            BlockPos pos = first.below(i);
            BlockState state = level.getBlockState(pos);
            if (!isIcicle(state)) break;
            FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, states[i]);
            falling.dropItem = false;
            if (i == length - 1) falling.setHurtsEntities(Math.min(1 + 2 * length, 40), 40);
        }
    }

    /** One completed local latent budget removes exactly one connected terminal segment. */
    public static boolean meltTip(ServerLevel level, BlockPos heated) {
        if (!isIcicle(level.getBlockState(heated))) return false;
        BlockPos.MutableBlockPos tip = heated.mutable();
        while (!level.isOutsideBuildHeight(tip.below()) && isIcicle(level.getBlockState(tip.below()))) tip.move(Direction.DOWN);
        return level.setBlockAndUpdate(tip, Blocks.AIR.defaultBlockState());
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CRACKED))
            level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state),
                    pos.getX() + .3 + random.nextDouble() * .4, pos.getY() + .05,
                    pos.getZ() + .3 + random.nextDouble() * .4, 0, -.03, 0);
    }
}
