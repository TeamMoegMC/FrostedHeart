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

package com.teammoeg.frostedheart.foundation.tooltips;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.compat.jei.JEICompat;
import com.teammoeg.frostedheart.content.climate.data.BlockTempData;
import com.teammoeg.frostedheart.content.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.foundation.recipes.InspireRecipe;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.TranslateUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.teammoeg.frostedheart.content.climate.food.FoodTemperatureHandler.*;

public class FHTooltips {


    /**
     * Register all items that will have custom tooltips here.
     * The actual tooltip is handled in various TooltipModifer classes.
     * These modifiers are handled in sequence at FHMain.REGISTRATE.setTooltipModifierFactory()
     *
     * For convenience we register all items and blocks here.
     */
    public static void registerTooltipModifiers() {
        ForgeRegistries.ITEMS.getKeys().forEach(FHTooltips::register);
        ForgeRegistries.BLOCKS.getKeys().forEach(FHTooltips::register);
    }

    public static void register(Item item) {
        Function<Item, TooltipModifier> factory = FHMain.REGISTRATE.getTooltipModifierFactory();
        if (factory != null) {
            TooltipModifier.REGISTRY.registerDeferred(RegistryUtils.getRegistryName(item), factory);
        }
    }

    public static void register(ResourceLocation key) {
        Function<Item, TooltipModifier> factory = FHMain.REGISTRATE.getTooltipModifierFactory();
        if (factory != null) {
            TooltipModifier.REGISTRY.registerDeferred(key, factory);
        }
    }


    /**
     * Custom tooltip handling.
     */
    public static void onItemTooltip(ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        final List<Component> text = event.getToolTip();
        if (!stack.isEmpty()) {
            // These are ordered in a predictable fashion
            // 1. Common information, that is important to know about the item stack itself (such as size, food, heat, etc.). Static (unchanging) information is ordered before dynamic (changing) information.
            // 2. Extra information, that is useful QoL info, but not necessary (such as possible recipes, melting into, etc.)
            // 3. Debug information, that is only available in debug mode.

            // Part 1: Common information
            // Ignition materials
            if (stack.is(Tags.Items.RODS_WOODEN)) {
                text.add(TranslateUtils.translateTooltip("double_stick_ignition").withStyle(ChatFormatting.RED));
            }
            else {
                if (stack.is(FHTags.Items.IGNITION_MATERIAL.tag)) {
                    text.add(TranslateUtils.translateTooltip("ignition_material").withStyle(ChatFormatting.RED));
                    text.add(TranslateUtils.translateTooltip("ignition_tutorial").withStyle(ChatFormatting.GRAY));
                    List<Item> metals = ForgeRegistries.ITEMS.getValues().stream()
                            .filter(item -> item.builtInRegistryHolder().is(FHTags.Items.IGNITION_METAL.tag))
                            .toList();
                    for (Item item : metals) {
                        text.add(item.getDescription().copy().withStyle(ChatFormatting.GRAY));
                    }
                }
                if (stack.is(FHTags.Items.IGNITION_METAL.tag)) {
                    text.add(TranslateUtils.translateTooltip("ignition_metal").withStyle(ChatFormatting.RED));
                    text.add(TranslateUtils.translateTooltip("ignition_tutorial").withStyle(ChatFormatting.GRAY));
                    // append the localized names of ignition materials from the tag
                    // get all items in the tag
                    List<Item> materials = ForgeRegistries.ITEMS.getValues().stream()
                            .filter(item -> item.builtInRegistryHolder().is(FHTags.Items.IGNITION_MATERIAL.tag))
                            .toList();
                    for (Item item : materials) {
                        text.add(item.getDescription().copy().withStyle(ChatFormatting.GRAY));
                    }
                }
            }


            // Dry food cannot be frozen nor heated
            if (stack.is(FHTags.Items.DRY_FOOD.tag)) {
                text.add(TranslateUtils.translateTooltip("food.dry").withStyle(ChatFormatting.YELLOW));
            }
            if (stack.is(FHTags.Items.INSULATED_FOOD.tag)) {
                text.add(TranslateUtils.translateTooltip("food.insulated").withStyle(ChatFormatting.GREEN));
            }

            // Food temperature status
            if (canChangeTemperatureFood(stack)) {
                byte temperature = getTemperature(stack);
                if (temperature != -1) {
                    if (temperature == FROZEN) {
                        text.add(TranslateUtils.translateTooltip("food.temperature.frozen").withStyle(ChatFormatting.BLUE));
                    } else if (temperature == HOT) {
                        text.add(TranslateUtils.translateTooltip("food.temperature.hot").withStyle(ChatFormatting.GOLD));
                    } else if (temperature == COLD) {
                        text.add(TranslateUtils.translateTooltip("food.temperature.cold").withStyle(ChatFormatting.AQUA));
                    }
                }
            }

            Item i = stack.getItem();


            // Inspiration item
            for (InspireRecipe ir : FHUtils.filterRecipes(null, InspireRecipe.TYPE)) {
                if (ir.item.test(stack)) {
                    event.getToolTip().add(TranslateUtils.translateTooltip("inspire_item").withStyle(ChatFormatting.GRAY));
                    break;
                }
            }
//            float tspeed = (float) (double) FHConfig.SERVER.tempSpeed.get();
//
//            // Food adjust temperature
//            ITempAdjustFood itf = null;
//            if (i instanceof ITempAdjustFood) {
//                itf = (ITempAdjustFood) i;
//            }
//            else {
//                itf = FHDataManager.getTempAdjustFood(stack);
//            }

            // Handle ITF first
//            if (itf != null) {
//                float temp = itf.getHeat(stack,
//                        event.getEntity() == null ? 37 : PlayerTemperatureData.getCapability(event.getEntity()).map(PlayerTemperatureData::getEnvTemp).orElse(0f)) * tspeed;
//                temp = (Math.round(temp * 1000)) / 1000.0F;// round
//                if (temp != 0)
//                    if (temp > 0)
//                        event.getToolTip()
//                                .add(TranslateUtils.translateTooltip("food_temp", TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp)).withStyle(ChatFormatting.GOLD));
//                    else
//                        event.getToolTip()
//                                .add(TranslateUtils.translateTooltip("food_temp", TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp)).withStyle(ChatFormatting.AQUA));
//            }
//            // If not ITF, apply the default temp modifier through temp status
//            else {
//                if (canChangeTemperatureFood(stack)) {
//                    byte temperature = getTemperature(stack);
//                    if (temperature != -1) {
//                        if (temperature == HOT) {
//                            text.add(TranslateUtils.translateTooltip("food_temp", TemperatureDisplayHelper.toTemperatureDeltaFloatString(DEFAULT_HOT_FOOD_HEAT)).withStyle(ChatFormatting.GOLD));
//                        } else if (temperature == COLD) {
//                            text.add(TranslateUtils.translateTooltip("food_temp", TemperatureDisplayHelper.toTemperatureDeltaFloatString(DEFAULT_COLD_FOOD_HEAT)).withStyle(ChatFormatting.AQUA));
//                        }
//                    }
//                }
//            }

            // Equipment temperature
            //IWarmKeepingEquipment iwe = null;
           /* if (i instanceof IWarmKeepingEquipment) {
                iwe = (IWarmKeepingEquipment) i;
            } else {
                String s = ItemNBTHelper.getString(stack, "inner_cover");
                EquipmentSlotType aes = MobEntity.getSlotForItemStack(stack);
                if (s.length() > 0 && aes != null) {
                    event.getToolTip().add(GuiUtils.translateTooltip("inner").mergeStyle(TextFormatting.GREEN)
                            .appendSibling(TranslateUtils.translate("item." + s.replaceFirst(":", "."))));
                    if (!ItemNBTHelper.getBoolean(stack, "inner_bounded")) {
                        if (stack.hasTag() && stack.getTag().contains("inner_cover_tag")) {
                            CompoundNBT cn = stack.getTag().getCompound("inner_cover_tag");
                            int damage = cn.getInt("Damage");
                            if (damage != 0) {
                                InstallInnerRecipe ri = InstallInnerRecipe.recipeList.get(new ResourceLocation(s));
                                if (ri != null) {
                                    int maxDmg = ri.getDurability();
                                    float temp = damage * 1.0F / maxDmg;
                                    String temps = Integer.toString((Math.round(temp * 100)));
                                    event.getToolTip().add(GuiUtils.translateTooltip("inner_damage", temps));
                                }
                            }
                            if (cn.contains("Enchantments")) {
                                ListNBT ln = cn.getList("Enchantments", 10);
                                if (!ln.isEmpty()) {
                                    event.getToolTip().add(
                                            GuiUtils.translateTooltip("inner_enchantment").mergeStyle(TextFormatting.GRAY));
                                    ItemStack.addEnchantmentTooltips(event.getToolTip(), ln);
                                }
                            }

                        }
                    }
                    iwe = FHDataManager.getArmor(s + "_" + aes.getName());
                } else
                    iwe = FHDataManager.getArmor(stack);
            }*/

            // Block temperature
//            BlockTempData btd = FHDataManager.getBlockData(stack);
//            if (btd != null) {
//                float temp = btd.getTemp();
//                temp = (Math.round(temp * 100)) / 100.0F;// round
//                if (temp != 0)
//                    if (temp > 0)
//                        event.getToolTip()
//                                .add(TranslateUtils.translateTooltip("block_temp", TemperatureDisplayHelper.toTemperatureFloatString(temp)).withStyle(ChatFormatting.GOLD));
//                    else
//                        event.getToolTip()
//                                .add(TranslateUtils.translateTooltip("block_temp", TemperatureDisplayHelper.toTemperatureFloatString(temp)).withStyle(ChatFormatting.AQUA));
//            }

          /*  if (iwe != null) {
                float temp = iwe.getFactor(null, stack);
                temp = Math.round(temp * 100);
                String temps = Float.toString(temp);
                if (temp != 0)
                    event.getToolTip().add(GuiUtils.translateTooltip("armor_warm", temps).mergeStyle(TextFormatting.GOLD));
            }*/

            // Heating equipment
//            if (i instanceof IHeatingEquipment) {
//                float temp = ((IHeatingEquipment) i).getEffectiveTempAdded(null, stack,0, 0);
//                temp = (Math.round(temp * 2000)) / 1000.0F;
//                if (temp != 0)
//                    if (temp > 0)
//                        event.getToolTip().add(
//                                TranslateUtils.translateTooltip("armor_heating", TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp)).withStyle(ChatFormatting.GOLD));
//                    else
//                        event.getToolTip()
//                                .add(TranslateUtils.translateTooltip("armor_heating", TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp)).withStyle(ChatFormatting.AQUA));
//            }

            // Research tooltip
            Map<String, Component> rstooltip= JEICompat.research.get(i);
            if(rstooltip!=null)
                event.getToolTip().addAll(rstooltip.values());

        }
    }
}
