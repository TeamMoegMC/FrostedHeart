/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One hanging curtain, with one visual strip per exposed roof edge. */
public final class EaveIcicleBlock extends Block {
    public static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    static {
        VoxelShape[] strips = {box(0, 5, 0, 16, 16, 3), box(13, 5, 0, 16, 16, 16),
                box(0, 5, 13, 16, 16, 16), box(0, 5, 0, 3, 16, 16)};
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < 4; i++) if ((mask & 1 << i) != 0) shape = Shapes.or(shape, strips[i]);
            SHAPES[mask] = shape;
        }
    }

    public EaveIcicleBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction edge : EDGES) state = state.setValue(property(edge), false);
        registerDefaultState(state.setValue(property(Direction.NORTH), true));
    }

    public static BooleanProperty property(Direction edge) { return MultifaceBlock.getFaceProperty(edge); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction edge : EDGES) builder.add(property(edge));
    }

    public static boolean isEaveIce(BlockState state) { return state.getBlock() instanceof EaveIcicleBlock; }

    public static int mask(BlockState state) {
        int mask = 0;
        for (int i = 0; i < 4; i++) if (state.getValue(property(EDGES[i]))) mask |= 1 << i;
        return mask;
    }

    public BlockState withEdges(int mask) {
        BlockState state = defaultBlockState();
        for (int i = 0; i < 4; i++) state = state.setValue(property(EDGES[i]), (mask & 1 << i) != 0);
        return state;
    }

    /** Unknown neighboring columns defer growth rather than loading chunks. */
    public static int roofEdges(LevelReader level, BlockPos roof) {
        int mask = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos outside = roof.relative(EDGES[i]);
            if (!level.hasChunkAt(outside)) return 0;
            if (level.getBlockState(outside).isAir() && level.getBlockState(outside.below()).isAir()) mask |= 1 << i;
        }
        return mask;
    }

    public static boolean isRoof(LevelReader level, BlockPos roof) {
        if (!level.hasChunkAt(roof)) return false;
        return exposed(level, roof)
                && level.getBlockState(roof).isFaceSturdy(level, roof, Direction.DOWN);
    }

    private static boolean exposed(LevelReader level, BlockPos roof) {
        BlockState cover = level.getBlockState(roof.above());
        int allowedHeight = roof.getY() + (cover.is(Blocks.SNOW) || cover.is(Blocks.SNOW_BLOCK) ? 2 : 1);
        // Heightmap exposure describes rain access and does not wait for skylight propagation.
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, roof.getX(), roof.getZ()) <= allowedHeight;
    }

    public static boolean hasWater(ServerLevel level, BlockPos roof, boolean precipitation) {
        BlockState cover = level.getBlockState(roof.above());
        return cover.is(Blocks.SNOW) || cover.is(Blocks.SNOW_BLOCK) || level.getBlockState(roof).is(Blocks.SNOW_BLOCK)
                || precipitation && exposed(level, roof)
                // The caller uses FH's actual snowy/blizzard track when present,
                // just like the existing surface snow pass, including dry vanilla biomes.
                && (WorldClimate.get(level) != null
                    || level.getBiome(roof.above()).value().getPrecipitationAt(roof.above()) != Biome.Precipitation.NONE);
    }

    /** One surface-column sample per interval, normalized to ordinary section random ticks. */
    public static double surfaceChance(int randomTickSpeed, int intervalTicks) {
        return Math.min(.5, IcicleBlock.ROOT_CHANCE * randomTickSpeed * (double) intervalTicks / 16);
    }

    public static void randomRoofTick(ServerLevel level, BlockPos surface, boolean precipitation,
            int randomTickSpeed, int intervalTicks, RandomSource random) {
        BlockPos roof = level.getBlockState(surface).is(Blocks.SNOW) ? surface.below() : surface;
        if (level.getBlockState(roof).is(Blocks.SNOW_BLOCK) && !level.getBlockState(roof.below()).isAir()) roof = roof.below();
        BlockPos target = roof.below();
        // Ineligible water/ground columns do not consume the existing weather RNG.
        if (!level.getBlockState(target).isAir()
                || !level.getBlockState(roof).isFaceSturdy(level, roof, Direction.DOWN)) return;
        double chance = surfaceChance(randomTickSpeed, intervalTicks);
        if (random.nextDouble() >= chance * 2 || !isRoof(level, roof)) return;
        int edges = roofEdges(level, roof);
        if (edges == 0 || !hasWater(level, roof, precipitation)) return;
        if (random.nextDouble() < .5 * IcicleBlock.growthMultiplier(MinecraftPhaseController.temperature(level, roof)))
            level.setBlockAndUpdate(target, FHBlocks.EAVE_ICICLES.get().withEdges(edges));
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos roof = pos.above();
        return !level.hasChunkAt(roof) || level.getBlockState(roof).isFaceSturdy(level, roof, Direction.DOWN);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos())) return null;
        int edges = roofEdges(context.getLevel(), context.getClickedPos().above());
        return withEdges(edges == 0 ? 1 << java.util.Arrays.asList(EDGES).indexOf(context.getHorizontalDirection().getOpposite()) : edges);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[mask(state)];
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) level.scheduleTick(pos, this, 1);
        return state;
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState before, boolean moving) {
        if (!level.isClientSide && !canSurvive(state, level, pos)) level.scheduleTick(pos, this, 1);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) fall(level, pos);
    }

    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) {
            double temperature = MinecraftThermalInput.gameplayPassiveEnvironment(level, pos, WorldTemperature.naturalAir(level, pos));
            if (random.nextDouble() < IcicleBlock.fractureChance(temperature, 0, true)) fall(level, pos);
        }
    }

    public static void fall(ServerLevel level, BlockPos pos) {
        if (!isEaveIce(level.getBlockState(pos))) return;
        FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, level.getBlockState(pos));
        entity.dropItem = false;
    }
}
