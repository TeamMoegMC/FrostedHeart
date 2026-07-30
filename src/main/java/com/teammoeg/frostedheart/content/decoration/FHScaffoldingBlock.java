package com.teammoeg.frostedheart.content.decoration;

import com.teammoeg.chorda.block.CBlock;
import com.teammoeg.chorda.util.struct.FastEnumMap;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FHScaffoldingBlock extends CBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty SURFACE = BooleanProperty.create("surface");
    public static final BooleanProperty LEGS = BooleanProperty.create("legs");
    public static final BooleanProperty BRACES = BooleanProperty.create("braces");
    public static final BooleanProperty BRACES_REVERSE = BooleanProperty.create("braces_reverse");
    public static final BooleanProperty CROSS_X = BooleanProperty.create("cross_x");
    public static final BooleanProperty CROSS_Z = BooleanProperty.create("cross_z");
    public static final BooleanProperty HANDRAIL = BooleanProperty.create("handrail");

    private static final List<BooleanProperty> SHAPED_PROPS = List.of(SURFACE, LEGS, CROSS_X, CROSS_Z);
    private static final List<VoxelShape> ALL_SHAPES;
    static {
        VoxelShape surface = Block.box(0, 14, 0, 16, 16, 16);
        VoxelShape leg1 = Block.box(0, 0, 0, 2, 16, 2);
        VoxelShape leg2 = Block.box(14, 0, 0, 16, 16, 2);
        VoxelShape leg3 = Block.box(0, 0, 14, 2, 16, 16);
        VoxelShape leg4 = Block.box(14, 0, 14, 16, 16, 16);
        VoxelShape legs = Shapes.or(leg1, leg2, leg3, leg4);
        VoxelShape crossX = Block.box(6, 12, 0, 10, 14, 16);
        VoxelShape crossZ = Block.box(0, 12, 6, 16, 14, 10);
        ALL_SHAPES = List.of(surface, legs, crossX, crossZ);
    }

    private static final ConcurrentHashMap<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();

    public FHScaffoldingBlock(Properties blockProps) {
        super(blockProps);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(SURFACE, true)
                .setValue(LEGS, true)
                .setValue(BRACES, true)
                .setValue(BRACES_REVERSE, false)
                .setValue(CROSS_X, false)
                .setValue(CROSS_Z, false)
                .setValue(HANDRAIL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.WATERLOGGED, SURFACE, LEGS, BRACES, BRACES_REVERSE, CROSS_X, CROSS_Z, HANDRAIL);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        var pos = pContext.getClickedPos();
        var level = pContext.getLevel();
        return calcState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER), pos, level);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return (pContext.isHoldingItem(pState.getBlock().asItem()) || pContext.isHoldingItem(FHBlocks.METAL_SCAFFOLDING_STAIR.asItem())) ? Shapes.block() : shapeCache.computeIfAbsent(pState, this::computeShape);
    }

    private VoxelShape computeShape(BlockState state) {
        List<VoxelShape> activeShapes = new ArrayList<>();
        for (int i = 0; i < SHAPED_PROPS.size(); i++) {
            if (state.getValue(SHAPED_PROPS.get(i))) {
                activeShapes.add(ALL_SHAPES.get(i));
            }
        }

        if (activeShapes.isEmpty()) {
            return Shapes.empty();
        }

        return Shapes.or(Shapes.empty(), activeShapes.toArray(new VoxelShape[0]));
    }

    public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return Shapes.block();
    }

    @Override
    public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        return super.canBeReplaced(pState, pUseContext);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        if (pState.getValue(LEGS) && (!pContext.isAbove(Shapes.block(), pPos, true) || pContext.isDescending())) {
            return Shapes.empty();
        }
        return super.getCollisionShape(pState, pLevel, pPos, pContext);
    }

    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pPlayer.getItemInHand(pHand).is(FHTags.forgeTag(ForgeRegistries.ITEMS, "rods"))) {
            pLevel.setBlock(pPos, pState.setValue(HANDRAIL, !pState.getValue(HANDRAIL)), 2);
            pLevel.playSound((Player)null, pPos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F + pLevel.random.nextFloat() * 0.4F);
            return InteractionResult.SUCCESS;
        }
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
        if (pState.getValue(BlockStateProperties.WATERLOGGED)) {
            pLevel.scheduleTick(pPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }
        return calcState(pState, pPos, pLevel);
    }

    private @NotNull BlockState calcState(BlockState prevState, BlockPos pos, LevelAccessor level) {
        var neighbors = getNeighbors(pos);

        BlockPos posDown = neighbors.get(Direction.DOWN);
        BlockPos posUp = neighbors.get(Direction.UP);
        BlockState downState = level.getBlockState(posDown);
        BlockState upState = level.getBlockState(posUp);

        boolean legs = downState.getBlock() instanceof FHScaffoldingBlock
                || downState.isFaceSturdy(level, posDown, Direction.UP);
        boolean bracesReverse = legs && downState.hasProperty(BRACES) && downState.getValue(BRACES);
        boolean crossX;
        boolean crossZ;
        boolean surface;

        if (upState.getBlock() instanceof FHScaffoldingBlock) {
            crossX = false;
            crossZ = false;
        } else {
            BlockPos posNorth = neighbors.get(Direction.NORTH);
            BlockPos posSouth = neighbors.get(Direction.SOUTH);
            BlockPos posEast = neighbors.get(Direction.EAST);
            BlockPos posWest = neighbors.get(Direction.WEST);

            BlockState northState = level.getBlockState(posNorth);
            BlockState southState = level.getBlockState(posSouth);
            BlockState eastState = level.getBlockState(posEast);
            BlockState westState = level.getBlockState(posWest);

            crossX = (isScaffolding(northState)
                    || (!legs && northState.isFaceSturdy(level, posNorth, Direction.SOUTH)))
                    || (isScaffolding(southState)
                    || (!legs && southState.isFaceSturdy(level, posSouth, Direction.NORTH)));
            crossZ = (isScaffolding(eastState)
                    || (!legs && eastState.isFaceSturdy(level, posEast, Direction.WEST)))
                    || (isScaffolding(westState)
                    || (!legs && westState.isFaceSturdy(level, posWest, Direction.EAST)));
        }
        surface = crossX || crossZ || !(upState.getBlock() instanceof FHScaffoldingBlock && legs);

        return prevState
                .setValue(SURFACE, surface)
                .setValue(LEGS, legs)
                .setValue(BRACES, legs && !bracesReverse)
                .setValue(BRACES_REVERSE, bracesReverse)
                .setValue(CROSS_X, crossX)
                .setValue(CROSS_Z, crossZ);
    }

    public boolean isScaffolding(BlockState state) {
        var block = state.getBlock();
        return block instanceof FHScaffoldingBlock || block instanceof FHScaffoldingStairBlock;
    }

    private FastEnumMap<Direction, BlockPos> getNeighbors(BlockPos pos) {
        FastEnumMap<Direction, BlockPos> map = new FastEnumMap<>(Direction.values());
        for (Direction dir : Direction.values()) {
            map.put(dir, pos.relative(dir));
        }
        return map;
    }
}
