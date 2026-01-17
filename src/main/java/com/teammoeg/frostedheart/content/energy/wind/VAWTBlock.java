package com.teammoeg.frostedheart.content.energy.wind;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO 保存BE和BS; 检测空旷区域
public class VAWTBlock extends KineticBlock implements IBE<VAWTBlockEntity> {

    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");
    public final VoxelShape shape;
    public final VAWTType type;

    public static class VAWTType {
        public static final Map<String, VAWTType> ALL_TYPES = new HashMap<>();

        public final String name;
        public final long durability;
        public final float weight;
        public final Block block;

        private VAWTType(String name, int durability, float weight, Block owner) {
            this.name = name;
            this.durability = durability * 60L * 1000L;
            this.weight = weight;
            this.block = owner;
            ALL_TYPES.put(name, this);
        }
    }

    public static VAWTBlock create(Properties properties, String name, int durability, float weight, VoxelShape shape) {
        return new VAWTBlock(properties, name, durability, weight, shape); // TODO config
    }

    private VAWTBlock(Properties properties, String name, int durability, float weight, VoxelShape shape) {
        super(properties);
        this.shape = shape;
        this.type = new VAWTType(name, durability, weight, this);
        this.registerDefaultState(defaultBlockState().setValue(DAMAGED, false));
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return false;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        var tag = stack.getTag();
        if (tag != null && tag.contains("BlockEntityTag", 10)) {
            long durability = tag.getCompound("BlockEntityTag").getLong("durability");
            if (durability <= 0) {
                tooltip.add(Component.literal("Damaged").withStyle(ChatFormatting.RED)); // TODO Lang
                return;
            }
            tooltip.add(Component.translatable("gui.frostedheart.time_left").append(ClientUtils.asTime(durability)));
        }
    }

    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, worldIn, pos, oldState, isMoving);
    }

    public ItemStack getCloneItemStack(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return setNBTOnStack(super.getCloneItemStack(pLevel, pPos, pState), pState.getValue(DAMAGED));
    }

    public static ItemStack setNBTOnStack(ItemStack stack, boolean damaged) {
        if (damaged) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putBoolean(DAMAGED.getName(), true);
            stack.addTagElement("BlockStateTag", compoundtag);
        }

        return stack;
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if (worldIn.isClientSide) return;
        withBlockEntityDo(worldIn, pos, be -> {
            var tag = stack.getOrCreateTag();

        });
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(DAMAGED, Boolean.FALSE);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DAMAGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public Class<VAWTBlockEntity> getBlockEntityClass() {
        return VAWTBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VAWTBlockEntity> getBlockEntityType() {
        return FHBlockEntityTypes.VAWT.get();
    }
}
