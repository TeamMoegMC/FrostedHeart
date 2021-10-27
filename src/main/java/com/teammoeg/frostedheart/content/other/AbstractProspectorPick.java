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

package com.teammoeg.frostedheart.content.other;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;

public abstract class AbstractProspectorPick extends FHBaseItem {
    public static ResourceLocation tag = new ResourceLocation("forge:ores");

    public AbstractProspectorPick(String name, Properties properties) {
        super(name, properties);
    }
    public abstract int getHorizonalRange(ItemStack item);
    public abstract int getVerticalRange(ItemStack item);
    @SuppressWarnings("resource")
	@Override
    public ActionResultType onItemUse(ItemUseContext context) {
        PlayerEntity player = context.getPlayer();
        IWorld world = context.getWorld();
        BlockPos blockpos = context.getPos();
        int x = blockpos.getX();
        int y = blockpos.getY();
        int z = blockpos.getZ();
        BlockPos.Mutable mutable = new BlockPos.Mutable(x, y, z);
        Block ore;
        HashMap<String,Integer> founded=new HashMap<>();
        boolean found = false;
        if (player != null) {
            context.getItem().damageItem(1, player, (player2) -> player2.sendBreakAnimation(context.getHand()));
        }
        int rseed=0;
        int hrange=this.getHorizonalRange(context.getItem());
        int vrange=this.getVerticalRange(context.getItem());
        for (int x2 = -hrange; x2 < hrange; x2++)
            for (int y2 = -vrange; y2 < vrange; y2++)
                for (int z2 = -hrange; z2 < hrange; z2++) {
                    int BlockX = x + x2;
                    int BlockY = y + y2;
                    int BlockZ = z + z2;
                    ore = world.getBlockState(mutable.setPos(BlockX, BlockY, BlockZ)).getBlock();
                    if (ore.getTags().contains(tag)) {
                        founded.merge(ore.getTranslationKey(),1,(a,b)->a+b);
                        rseed++;
                    }
                }
        if (player != null) {
            if (!founded.isEmpty()) {
            	rseed%=founded.size();//TODO may we add miss rate?
            	String ore_name=null;
            	int count=0;
            	for(Map.Entry<String,Integer> me:founded.entrySet()) {
            		if(rseed<=0) {
            			ore_name=me.getKey();
            			count=me.getValue();
            		}
            		rseed--;
            	}
            	if(ore_name!=null) {
	                if (count < 20)
	                    player.sendStatusMessage(GuiUtils.translateMessage("vein_size.small").appendSibling(new TranslationTextComponent(ore_name)).mergeStyle(TextFormatting.GOLD), true);
	                else if (count < 40)
	                    player.sendStatusMessage(GuiUtils.translateMessage("vein_size.medium").appendSibling(new TranslationTextComponent(ore_name)).mergeStyle(TextFormatting.GOLD), true);
	                else {
	                    player.sendStatusMessage(GuiUtils.translateMessage("vein_size.large").appendSibling(new TranslationTextComponent(ore_name)).mergeStyle(TextFormatting.GOLD), true);
	                }
	                return ActionResultType.SUCCESS;
            	}
            }
            player.sendStatusMessage(GuiUtils.translateMessage("vein_size.nothing").mergeStyle(TextFormatting.GOLD), true);
        }
        return ActionResultType.SUCCESS;
    }
}
