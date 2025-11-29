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
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.bootstrap.common.ToolCompat;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.utility.FHLeveledTool;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;
import se.mickelus.tetra.properties.IToolProvider;

public class CoreSpade extends FHLeveledTool {
	
    public static InteractionResult doProspect(Player player, Level world, BlockPos blockpos, ItemStack is, InteractionHand h) {
        if (player != null && (!(player instanceof FakePlayer))) {// fake players does not deserve XD
            int x = blockpos.getX();
            int y = blockpos.getY();
            int z = blockpos.getZ();

            is.hurtAndBreak(1, player, (player2) -> player2.broadcastBreakEvent(h));
            if (!world.isClientSide) {
                Random rnd = new Random(BlockPos.asLong(x, y, z) ^ 0x9a6dc5270b92313dL);// randomize
                // This is predictable, but not any big problem. Cheaters can use x-ray or other
                // things rather than hacking in this.
                
                Predicate<Holder<Biome>> tagdet;
                float corr = getCorrectness(is);
                if (rnd.nextInt((int) (20 * corr)) != 0) {
                    tagdet = ts -> FHTags.Biomes.IS_CAVE.matches(ts)||FHTags.Biomes.IS_ORE_VEIN.matches(ts);
                } else
                    tagdet = ts -> FHTags.Biomes.IS_CAVE.matches(ts);
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x, y, z);
                Holder<Biome> ore;
                HashMap<Holder<Biome>, Integer> founded = new HashMap<>();
                final int hrange = getHorizonalRange(is);
                int vrange = getVerticalRange(is);
                vrange = Math.min(y, (rnd.nextInt(vrange) + vrange) / 2);
                //RegistryLookup<Biome> biomeRegistry=world.registryAccess().lookup(ForgeRegistries.Keys.BIOMES).get();

                for (int y2 = QuartPos.fromBlock(y-vrange); y2 < QuartPos.fromBlock(y); y2++)
                    for (int x2 = QuartPos.fromBlock(x-hrange); x2 < QuartPos.fromBlock(x+hrange); x2++)
                        for (int z2 = QuartPos.fromBlock(z-hrange); z2 < QuartPos.fromBlock(z+hrange); z2++) {
                            ore = world.getNoiseBiome(x2, y2, z2);
                            //System.out.println(ore);
                            if (tagdet.test(ore)) {
                                founded.merge(ore, 1, Integer::sum);
                            }
                        }

                if (!founded.isEmpty()) {
                    int count = 0;
                    MutableComponent s = Lang.translateMessage("corespade.ore");
                    for (Entry<Holder<Biome>, Integer> f : founded.entrySet()) {
                        if (rnd.nextInt((int) (f.getValue() * corr)) != 0) {
                        	;
                        	s = s.append(Lang.translateKey(f.getKey().unwrapKey().get().location().toLanguageKey("biome"))
                                    .withStyle(ChatFormatting.GREEN).append(","));
                            count++;
                        }
                    }
                    if (count > 0) {
                        player.displayClientMessage(s, false);
                        return InteractionResult.SUCCESS;
                    }
                }
                player.displayClientMessage(Lang.translateMessage("corespade.nothing").withStyle(ChatFormatting.GRAY),
                        false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static float getCorrectness(ItemStack item) {
        if (item.getItem() instanceof FHLeveledTool)
            return 1;

        return ((IToolProvider) item.getItem()).getToolEfficiency(item, ToolCompat.coreSpade) + 1;
    }

    public static int getHorizonalRange(ItemStack item) {
        return Math.max(3, getLevel(item));
    }



    public static int getVerticalRange(ItemStack item) {
        return getLevel(item) == 1 ? 32 : (48 + (getLevel(item) - 1) * 16);
    }

    public CoreSpade(int lvl, Properties properties) {
        super(lvl, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Lang.translateTooltip("meme.core_spade").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return doProspect(context.getPlayer(), context.getLevel(), context.getClickedPos(), context.getItemInHand(), context.getHand());

    }
}
