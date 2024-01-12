/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.tools.oredetect;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.compat.tetra.TetraCompat;
import com.teammoeg.frostedheart.content.tools.FHLeveledTool;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import se.mickelus.tetra.properties.IToolProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public class CoreSpade extends FHLeveledTool {
    public static ResourceLocation otag = new ResourceLocation("forge:ores");
    public static ResourceLocation stag = new ResourceLocation("forge:stone");

    public CoreSpade(String name, int lvl, Properties properties) {
        super(name, lvl, properties);
    }

    public static int getHorizonalRange(ItemStack item) {
        return Math.max(3, getLevel(item));
    }

    public static int getVerticalRange(ItemStack item) {
        return getLevel(item) == 1 ? 32 : (48 + (getLevel(item) - 1) * 16);
    }

    public static int getLevel(ItemStack item) {
        if (item.getItem() instanceof FHLeveledTool)
            return ((FHLeveledTool) item.getItem()).getLevel();

        return ((IToolProvider) item.getItem()).getToolLevel(item, TetraCompat.coreSpade);
    }

    public static float getCorrectness(ItemStack item) {
        if (item.getItem() instanceof FHLeveledTool)
            return 1;

        return ((IToolProvider) item.getItem()).getToolEfficiency(item, TetraCompat.coreSpade) + 1;
    }

    public static ActionResultType doProspect(PlayerEntity player, World world, BlockPos blockpos, ItemStack is, Hand h) {
        if (player != null && (!(player instanceof FakePlayer))) {// fake players does not deserve XD
            if (!world.isRemote && world.getBlockState(blockpos).getBlock().getTags().contains(otag)) {// early exit 'cause ore found
                player.sendStatusMessage(
                        new TranslationTextComponent(world.getBlockState(blockpos).getBlock().getTranslationKey())
                                .mergeStyle(TextFormatting.GOLD),
                        false);
                return ActionResultType.SUCCESS;
            }
            int x = blockpos.getX();
            int y = blockpos.getY();
            int z = blockpos.getZ();

            is.damageItem(1, player, (player2) -> player2.sendBreakAnimation(h));
            if (!world.isRemote) {
                Random rnd = new Random(BlockPos.pack(x, y, z) ^ 0x9a6dc5270b92313dL);// randomize
                // This is predictable, but not any big problem. Cheaters can use x-ray or other
                // things rather then hacking in this.

                Predicate<Set<ResourceLocation>> tagdet;
                float corr = getCorrectness(is);
                if (rnd.nextInt((int) (20 * corr)) != 0) {
                    tagdet = ts -> (ts.contains(otag)) || ts.contains(stag);
                } else
                    tagdet = ts -> ts.contains(stag);
                BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
                Block ore;
                HashMap<String, Integer> founded = new HashMap<>();
                final int hrange = getHorizonalRange(is);
                int vrange = getVerticalRange(is);
                vrange = Math.min(y, (rnd.nextInt(vrange) + vrange) / 2);


                for (int y2 = -vrange; y2 < 0; y2++)
                    for (int x2 = -hrange; x2 < hrange; x2++)
                        for (int z2 = -hrange; z2 < hrange; z2++) {
                            int BlockX = x + x2;
                            int BlockY = y + y2;
                            int BlockZ = z + z2;
                            ore = world.getBlockState(mutable.setPos(BlockX, BlockY, BlockZ)).getBlock();
                            if (!ore.getRegistryName().getNamespace().equals("minecraft") && tagdet.test(ore.getTags())) {
                                founded.merge(ore.getTranslationKey(), 1, Integer::sum);
                            }
                        }

                if (!founded.isEmpty()) {
                    int count = 0;
                    IFormattableTextComponent s = GuiUtils.translateMessage("corespade.ore");
                    for (Entry<String, Integer> f : founded.entrySet()) {
                        if (rnd.nextInt((int) (f.getValue() * corr)) != 0) {
                            s = s.appendSibling(new TranslationTextComponent(f.getKey())
                                    .mergeStyle(TextFormatting.GREEN).appendString(","));
                            count++;
                        }
                    }
                    if (count > 0) {
                        player.sendStatusMessage(s, false);
                        return ActionResultType.SUCCESS;
                    }
                }
                player.sendStatusMessage(GuiUtils.translateMessage("corespade.nothing").mergeStyle(TextFormatting.GRAY),
                        false);
            }
        }
        return ActionResultType.SUCCESS;
    }

    @SuppressWarnings("resource")
    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        return doProspect(context.getPlayer(), context.getWorld(), context.getPos(), context.getItem(), context.getHand());

    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(GuiUtils.translateTooltip("meme.core_spade").mergeStyle(TextFormatting.GRAY));
    }
}
