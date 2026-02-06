/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.energy.wind;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.teammoeg.chorda.text.CFormatHelper;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VAWTBlock extends KineticBlock implements IBE<VAWTBlockEntity> {

    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");
    public final VoxelShape shape;
    public final VAWTType type;

    @Getter
    public static class VAWTType {
        static final Map<String, VAWTType> ALL_TYPES = new HashMap<>();

        final String name;
        final long durability;
        final float weight;
        final Block block;
        // TODO
//        final VoxelShape AlternatorShape;

        private VAWTType(String name, int durability, float weight, Block owner) {
            this.name = name;
            this.durability = durability * 60L * 1000L;
            this.weight = weight;
            this.block = owner;
            ALL_TYPES.put(name, this);
        }

        public int getDurability() {
            return (int)(durability * FHConfig.SERVER.VAWT.vawtDurability.get());
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
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        long durability = stack.getMaxDamage()-stack.getDamageValue();

        if (durability <= 0) {
            tooltip.add(Component.translatable("message.frostedheart.vawt.state.damaged").withStyle(ChatFormatting.RED));
            return;
        }
        
        tooltip.add(Component.translatable("gui.frostedheart.durability_left").append(CFormatHelper.msToTime(durability)));
        tooltip.add(Component.translatable("message.frostedheart.vawt.speed_bonus",
                        Component.literal(String.valueOf(type.weight*100-100)))
                .withStyle(type.weight < 1 ? ChatFormatting.RED : ChatFormatting.GREEN));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof VAWTBlockEntity vbe) {
            return List.of(setNBTOnStack(new ItemStack(this), vbe, state));
        }
        return super.getDrops(state, params);
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return setNBTOnStack(super.getCloneItemStack(level, pos, state), level.getBlockEntity(pos), state);
    }

    public static ItemStack setNBTOnStack(ItemStack stack, BlockEntity be, BlockState state) {
        if (be instanceof VAWTBlockEntity v) {
            // 不使用 saveToItem 的原因是会额外储存机械动力的网络数据
        	stack.setDamageValue(v.getDamage());
        }
        if (state.getValue(DAMAGED)) {
            var stateTag = new CompoundTag();
            stateTag.putBoolean(DAMAGED.getName(), true);
            stack.addTagElement("BlockStateTag", stateTag);
        }
        return stack;
    }

	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(worldIn, pos, state, placer, stack);
		BlockEntity blockEntity = worldIn.getBlockEntity(pos);
		if (blockEntity instanceof VAWTBlockEntity vawt)
			vawt.setDamage(stack.getDamageValue());

	}

    @Override
    public int getLightBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return 3;
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
