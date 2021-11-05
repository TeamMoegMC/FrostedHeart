/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.tools.oredetect;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Item.Properties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

public class CoreSpade extends FHBaseItem {
	public static ResourceLocation otag = new ResourceLocation("forge:ores");
	public static ResourceLocation stag = new ResourceLocation("forge:stone");

	public CoreSpade(String name, int hrange, int vrange, Properties properties) {
        super(name, properties);
        this.vrange=vrange;
        this.hrange=hrange;
    }
    private int vrange;
    private int hrange;
    public int getHorizonalRange(ItemStack item) {
    	return hrange;
    }
    public int getVerticalRange(ItemStack item) {
    	return vrange;
    }

	@SuppressWarnings("resource")
	@Override
	public ActionResultType onItemUse(ItemUseContext context) {
		PlayerEntity player = context.getPlayer();
		if (player != null && (!(player instanceof FakePlayer))) {// fake players does not deserve XD
			World world = context.getWorld();
			BlockPos blockpos = context.getPos();
			if (world.getBlockState(blockpos).getBlock().getTags().contains(otag)) {// early exit 'cause ore found
				player.sendMessage(
						new TranslationTextComponent(world.getBlockState(blockpos).getBlock().getTranslationKey())
								.mergeStyle(TextFormatting.GOLD),
								player.getUniqueID());
				return ActionResultType.SUCCESS;
			}
			int x = blockpos.getX();
			int y = blockpos.getY();
			int z = blockpos.getZ();
			context.getItem().damageItem(1, player, (player2) -> player2.sendBreakAnimation(context.getHand()));
			if (!world.isRemote) {
				Random rnd = new Random(BlockPos.pack(x, y, z) ^ 0x9a6dc5270b92313dL);// randomize
				// This is predictable, but not any big problem. Cheaters can use x-ray or other
				// things rather then hacking in this.

				Predicate<Set<ResourceLocation>> tagdet;
				if (rnd.nextInt(20) != 0) {
					tagdet = ts -> (ts.contains(otag)) || ts.contains(stag);
				} else
					tagdet = ts -> ts.contains(stag);
				BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
				Block ore;
				HashMap<String, Integer> founded = new HashMap<>();
				int hrange = this.getHorizonalRange(context.getItem());
				int vrange = this.getVerticalRange(context.getItem());
				vrange = Math.min(y, (rnd.nextInt(vrange) + vrange) / 2);

				for (int x2 = -hrange; x2 < hrange; x2++)
					for (int y2 = -vrange; y2 < 0; y2++)
						for (int z2 = -hrange; z2 < hrange; z2++) {
							int BlockX = x + x2;
							int BlockY = y + y2;
							int BlockZ = z + z2;
							ore = world.getBlockState(mutable.setPos(BlockX, BlockY, BlockZ)).getBlock();
							if (tagdet.test(ore.getTags())) {
								founded.merge(ore.getTranslationKey(), 1, (a, b) -> a + b);
							}
						}

				if (!founded.isEmpty()) {
					int count = 0;
					IFormattableTextComponent s = GuiUtils.translateMessage("corespade.ore");
					for (Entry<String, Integer> f : founded.entrySet()) {
						if (rnd.nextInt(f.getValue()) != 0) {
							s = s.appendSibling(new TranslationTextComponent(f.getKey())
									.mergeStyle(TextFormatting.GREEN).appendString(" "));
							count++;
						}
					}
					if (count > 0) {
						player.sendMessage(s, player.getUniqueID());
						return ActionResultType.SUCCESS;
					}
				}
				player.sendMessage(GuiUtils.translateMessage("corespade.nothing").mergeStyle(TextFormatting.GRAY),
						player.getUniqueID());
			}
		}
		return ActionResultType.SUCCESS;
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		tooltip.add(GuiUtils.translateTooltip("meme.core_spade").mergeStyle(TextFormatting.GRAY));
	}
}
