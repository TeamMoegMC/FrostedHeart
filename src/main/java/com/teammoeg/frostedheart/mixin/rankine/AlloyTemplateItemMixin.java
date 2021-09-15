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

package com.teammoeg.frostedheart.mixin.rankine;

import java.util.List;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.cannolicatfish.rankine.items.AlloyTemplateItem;
import com.cannolicatfish.rankine.recipe.helper.AlloyRecipeHelper;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

@Mixin(AlloyTemplateItem.class)
public class AlloyTemplateItemMixin extends Item {

    public AlloyTemplateItemMixin(Properties properties) {
        super(properties);
    }

    /**
     * @author yuesha-yc
     * @reason fixes ArrayIndexOutOfBoundException
     */
    @Overwrite
    public ITextComponent getDisplayName(ItemStack stack) {
        if (AlloyTemplateItem.getTemplate(stack).size() != 0) {
            String comp = AlloyTemplateItem.getTemplate(stack).get("NameAdd").getString();
            String p1;
            String p2;
            String p3;
            if (comp.contains("#")) {
                p1 = comp.split("#")[0];
                p2 = new TranslationTextComponent(comp.split("#")[1]).getString();
            } else {
                p1 = "";
                p2 = new TranslationTextComponent(comp).getString();
            }

            ITextComponent text = new TranslationTextComponent(this.getTranslationKey(stack));
            if (text.getString().split(" ").length <= 1) {
                // local is Chinese or Japanese
                p3 = text.getString();
            } else {
                p3 = text.getString().split(" ")[1];
            }

            return new StringTextComponent(p1 + " " + p2 + " " + p3);
        } else {
            return new TranslationTextComponent(this.getTranslationKey(stack));
        }

    }

    /**
     * @author yuesha-yc
     * @reason fixes ArrayIndexOutOfBoundException
     */
    @OnlyIn(Dist.CLIENT)
    @Overwrite
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (AlloyTemplateItem.getTemplate(stack).size() != 0) {
            tooltip.add(new StringTextComponent("Composition: " + AlloyTemplateItem.getOutputAlloyData(stack)).mergeStyle(TextFormatting.GRAY));
            tooltip.add(new StringTextComponent("Requires:").mergeStyle(TextFormatting.DARK_GREEN));
            String comp = AlloyTemplateItem.getTemplate(stack).get("StoredTemplate").getString();
            int count = 0;
            for (String s : comp.split("-")) {
                String str = s.replaceAll("[^A-Za-z]+", "");
                String end = "";
                int num = Integer.parseInt(s.replaceAll("[A-Za-z]+", ""));

                String namespace, path;
                ListNBT nbt = AlloyTemplateItem.getTemplate(stack).getList("Inputs", 10);
                String nbtstring = nbt.getString(count);
                String t = nbtstring.split("\"")[3];
                namespace = t.split(":")[0];
                path = t.split(":")[1];

                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(namespace, path));
                if (item != null) {
                    int reduce = AlloyRecipeHelper.returnMaterialCountFromStack(new ItemStack(item, 1));
                    num = num / reduce;
                    count++;


                    String display = new TranslationTextComponent(item.getTranslationKey()).getString();
                    tooltip.add(new StringTextComponent(num + "x " + display).mergeStyle(TextFormatting.GRAY));
                }

                //tooltip.add(new StringTextComponent(num + "x " + utils.getElementBySymbol(str).toString().substring(0,1).toUpperCase() + utils.getElementBySymbol(str).toString().substring(1).toLowerCase() + " " + end).mergeStyle(TextFormatting.GRAY));
            }
            tooltip.add(new StringTextComponent(""));
            tooltip.add(new StringTextComponent("Made in:").mergeStyle(TextFormatting.DARK_GREEN));
            int tier = AlloyTemplateItem.getTier(stack);
            if ((tier & 1) != 0) {
                tooltip.add(new StringTextComponent("Alloy Furnace").mergeStyle(TextFormatting.GRAY));
            }
            if ((tier & 2) != 0) {
                tooltip.add(new StringTextComponent("Induction Furnace").mergeStyle(TextFormatting.GRAY));
            }
        }
    }
}
