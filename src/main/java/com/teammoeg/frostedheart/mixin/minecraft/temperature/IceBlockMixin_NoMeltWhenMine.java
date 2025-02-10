package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Overwrite;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class IceBlockMixin_NoMeltWhenMine extends HalfTransparentBlock {

	public IceBlockMixin_NoMeltWhenMine(Properties pProperties) {
		super(pProperties);
	}

	/**
	 * Called after a player has successfully harvested this block. This method will
	 * only be called if the player has used the correct tool and drops should be
	 * spawned.
	 */
	@Overwrite
	public void playerDestroy(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState, @Nullable BlockEntity pTe, ItemStack pStack) {
		super.playerDestroy(pLevel, pPlayer, pPos, pState, pTe, pStack);
		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, pStack) == 0) {
			if (WorldTemperature.block(pLevel, pPos) > -2) {
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
}
