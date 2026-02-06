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

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Add generator effect for ice (melt)
 * <p>
 * */
@Mixin(IceBlock.class)
public abstract class IceBlockMixin_Melt extends HalfTransparentBlock{
    public IceBlockMixin_Melt(Properties pProperties) {
		super(pProperties);
	}
	/**
     * @author khjxiaogu
     * @reason add generator effect on ice
     */
    @Overwrite
    public void randomTick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
		// we already have our melting logic
//        if (worldIn.getBrightness(LightLayer.BLOCK, pos) > 11 - state.getLightBlock(worldIn, pos) || WorldTemperature.block(worldIn, pos) > WorldTemperature.WATER_ICE_MELTS) {
//            if (random.nextInt(50) == 0) {
//				this.melt(state, worldIn, pos);
//			}
//        }
    }
	/**
	 * 
	 * @reason remove water melt when it's cold
	 * @author khjxiaogu
	 */
	@Overwrite
	public void playerDestroy(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState, @Nullable BlockEntity pTe, ItemStack pStack) {
		super.playerDestroy(pLevel, pPlayer, pPos, pState, pTe, pStack);
		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, pStack) == 0) {
			if (WorldTemperature.block(pLevel, pPos) > WorldTemperature.WATER_ICE_MELTS) {
				if (pLevel.dimensionType().ultraWarm()) {
					pLevel.removeBlock(pPos, false);
					return;
				}
				BlockState blockstate = pLevel.getBlockState(pPos.below());
				if (blockstate.blocksMotion() || blockstate.liquid()) {
					pLevel.setBlockAndUpdate(pPos, IceBlock.meltsInto());
				}
			}
		}

	}
    @Shadow
    protected abstract void melt(BlockState state, Level world, BlockPos pos);
}
