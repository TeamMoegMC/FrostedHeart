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

package com.teammoeg.frostedheart.content.utility.oredetect;


import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import com.teammoeg.frostedheart.compat.tetra.TetraCompat;
import com.teammoeg.frostedheart.content.utility.FHLeveledTool;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import se.mickelus.tetra.properties.IToolProvider;

import net.minecraft.item.Item.Properties;

public class GeologistsHammer extends FHLeveledTool {
    public static ResourceLocation tag = new ResourceLocation("forge:ores");

    public static ActionResultType doProspect(PlayerEntity player, World world, BlockPos blockpos, ItemStack is, Hand h) {
        if (player != null && (!(player instanceof FakePlayer))) {//fake players does not deserve XD
            if (world.getBlockState(blockpos).getBlock().getTags().contains(tag)) {//early exit 'cause ore found
                player.displayClientMessage(TranslateUtils.translate(world.getBlockState(blockpos).getBlock().getDescriptionId()).withStyle(TextFormatting.GOLD), false);
                return ActionResultType.SUCCESS;
            }
            int x = blockpos.getX();
            int y = blockpos.getY();
            int z = blockpos.getZ();
            is.hurtAndBreak(1, player, (player2) -> player2.broadcastBreakEvent(h));
            if (!world.isClientSide) {
                float corr = getCorrectness(is);
                Random rnd = new Random(BlockPos.asLong(x, y, z) ^ 0xebd763e5b71a0128L);//randomize
                //This is predictable, but not any big problem. Cheaters can use x-ray or other things rather then hacking in this.
                if (rnd.nextInt((int) (20 * corr)) != 0) {//mistaken rate 5%
                    BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
                    Block ore;
                    HashMap<String, Integer> founded = new HashMap<>();

                    int hrange = getHorizonalRange(is);
                    int vrange = getVerticalRange(is);
                    for (int x2 = -hrange; x2 < hrange; x2++)
                        for (int y2 = -Math.min(y, vrange); y2 < vrange; y2++)
                            for (int z2 = -hrange; z2 < hrange; z2++) {
                                int BlockX = x + x2;
                                int BlockY = y + y2;
                                int BlockZ = z + z2;
                                ore = world.getBlockState(mutable.set(BlockX, BlockY, BlockZ)).getBlock();
                                if (ore.getTags().contains(tag)) {
                                    founded.merge(ore.getDescriptionId(), 1, Integer::sum);
                                }
                            }
                    if (!founded.isEmpty()) {
                        int count = 0;
                        IFormattableTextComponent s = TranslateUtils.translateMessage("vein_size.found");
                        for (Entry<String, Integer> f : founded.entrySet()) {
                            if (rnd.nextInt((int) (f.getValue() * corr)) != 0) {
                                int rval = f.getValue();
                                if (rval >= 5) {
                                    int err = (int) (rval / 5 / corr);
                                    if (err > 0)
                                        rval += rnd.nextInt(err * 2) - err;
                                }
                                s = s.append(TranslateUtils.translateMessage("vein_size.count", rval).append(TranslateUtils.translate(f.getKey()).withStyle(TextFormatting.GREEN)).append(" "));
                                count++;
                            }
                        }
                        if (count > 0) {
                            player.displayClientMessage(s, false);
                            return ActionResultType.SUCCESS;
                        }
                    }
                }
                player.displayClientMessage(TranslateUtils.translateMessage("vein_size.nothing").withStyle(TextFormatting.GRAY), false);
            }
        }
        return ActionResultType.SUCCESS;
    }

    public static float getCorrectness(ItemStack item) {
        if (item.getItem() instanceof FHLeveledTool)
            return 1;

        return ((IToolProvider) item.getItem()).getToolEfficiency(item, TetraCompat.geoHammer) + 1;
    }

    public static int getHorizonalRange(ItemStack item) {
        return getLevel(item) + 4;
    }

    public static int getLevel(ItemStack item) {
        if (item.getItem() instanceof FHLeveledTool)
            return ((FHLeveledTool) item.getItem()).getLevel();

        return ((IToolProvider) item.getItem()).getToolLevel(item, TetraCompat.geoHammer);
    }

    public static int getVerticalRange(ItemStack item) {
        return getLevel(item) + 3;
    }

    public GeologistsHammer(int lvl, Properties properties) {
        super(lvl, properties);

    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        return doProspect(context.getPlayer(), context.getLevel(), context.getClickedPos(), context.getItemInHand(), context.getHand());
    }
}
