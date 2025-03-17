/*
 * Copyright (c) 2024 TeamMoeg
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

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.compat.tetra.TetraCompat;
import com.teammoeg.frostedheart.content.utility.FHLeveledTool;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import se.mickelus.tetra.properties.IToolProvider;

public class GeologistsHammer extends FHLeveledTool {
    public static TagKey<Block> otag = FHTags.Blocks.ORES.tag;
    public static TagKey<Block> stag = FHTags.Blocks.STONE.tag;
    public static InteractionResult doProspect(Player player, Level world, BlockPos blockpos, ItemStack is, InteractionHand h) {
        if (player != null && (!(player instanceof FakePlayer))) {//fake players does not deserve XD
            if (world.getBlockState(blockpos).is(otag)) {//early exit 'cause ore found
                player.displayClientMessage(Lang.translateKey(world.getBlockState(blockpos).getBlock().getDescriptionId()).withStyle(ChatFormatting.GOLD), false);
                return InteractionResult.SUCCESS;
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
                    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x, y, z);
                    BlockState ore;
                    HashMap<String, Integer> founded = new HashMap<>();

                    int hrange = getHorizonalRange(is);
                    int vrange = getVerticalRange(is);
                    for (int x2 = -hrange; x2 < hrange; x2++)
                        for (int y2 = -Math.min(y, vrange); y2 < vrange; y2++)
                            for (int z2 = -hrange; z2 < hrange; z2++) {
                                int BlockX = x + x2;
                                int BlockY = y + y2;
                                int BlockZ = z + z2;
                                ore = world.getBlockState(mutable.set(BlockX, BlockY, BlockZ));
                                if (ore.is(otag)) {
                                    founded.merge(ore.getBlock().getDescriptionId(), 1, Integer::sum);
                                }
                            }
                    if (!founded.isEmpty()) {
                        int count = 0;
                        MutableComponent s = Lang.translateMessage("vein_size.found");
                        for (Entry<String, Integer> f : founded.entrySet()) {
                            if (rnd.nextInt((int) (f.getValue() * corr)) != 0) {
                                int rval = f.getValue();
                                if (rval >= 5) {
                                    int err = (int) (rval / 5 / corr);
                                    if (err > 0)
                                        rval += rnd.nextInt(err * 2) - err;
                                }
                                s = s.append(Lang.translateMessage("vein_size.count", rval).append(Lang.translateKey(f.getKey()).withStyle(ChatFormatting.GREEN)).append(" "));
                                count++;
                            }
                        }
                        if (count > 0) {
                            player.displayClientMessage(s, false);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
                player.displayClientMessage(Lang.translateMessage("vein_size.nothing").withStyle(ChatFormatting.GRAY), false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static float getCorrectness(ItemStack item) {
        if (item.getItem() instanceof FHLeveledTool)
            return 1;

        return ((IToolProvider) item.getItem()).getToolEfficiency(item, TetraCompat.geoHammer) + 1;
    }

    public static int getHorizonalRange(ItemStack item) {
        return getLevel(item) + 4;
    }

    public static int getVerticalRange(ItemStack item) {
        return getLevel(item) + 3;
    }

    public GeologistsHammer(int lvl, Properties properties) {
        super(lvl, properties);

    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return doProspect(context.getPlayer(), context.getLevel(), context.getClickedPos(), context.getItemInHand(), context.getHand());
    }
}
