/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.base.item.FHArmorMaterial;
import com.teammoeg.frostedheart.base.item.FHBaseArmorItem;
import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.base.item.FoodBlockItem;
import com.teammoeg.frostedheart.content.foods.CannedFoodItem;
import com.teammoeg.frostedheart.content.foods.FHSoupItem;
import com.teammoeg.frostedheart.content.steamenergy.HeatDebugItem;
import com.teammoeg.frostedheart.content.temperature.MushroomBed;
import com.teammoeg.frostedheart.content.temperature.SoilThermometer;
import com.teammoeg.frostedheart.content.temperature.SteamBottleItem;
import com.teammoeg.frostedheart.content.temperature.ThermometerItem;
import com.teammoeg.frostedheart.content.temperature.ThermosItem;
import com.teammoeg.frostedheart.content.temperature.handstoves.CoalHandStove;
import com.teammoeg.frostedheart.content.temperature.heatervest.HeaterVestItem;
import com.teammoeg.frostedheart.content.tools.CeramicBucket;
import com.teammoeg.frostedheart.content.tools.oredetect.CoreSpade;
import com.teammoeg.frostedheart.content.tools.oredetect.GeologistsHammer;
import com.teammoeg.frostedheart.content.tools.oredetect.ProspectorPick;
import com.teammoeg.frostedheart.research.machines.FHBasePen;
import com.teammoeg.frostedheart.research.machines.FHReusablePen;
import com.teammoeg.frostedheart.research.machines.RubbingTool;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Food;
import net.minecraft.item.Foods;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Properties;
import net.minecraft.item.Items;

public class FHItems {
	public static String[] colors=new String[]{"black","blue","brown","cyan","gray","green","light_blue","light_gray","lime","magenta","orange","pink","purple","red","white","yellow"};
    public static void init() {
    }

    static Properties createProps() {
        return new Item.Properties().group(FHMain.itemGroup);
    }

    public static Item hand_stove = new CoalHandStove("hand_stove", createProps().defaultMaxDamage(10));
    public static Item coal_stick = new FHBaseItem("coal_stick", createProps());
    public static Item charcoal_stick = new FHBaseItem("charcoal_stick", createProps());
    public static Item energy_core = new FHBaseItem("energy_core", createProps());
    public static Item wolfberries = new FoodBlockItem(FHBlocks.wolfberry_bush_block, createProps(), FHFoods.WOLFBERRIES, "wolfberries");
    public static Item dried_wolfberries = new FHBaseItem("dried_wolfberries", createProps().food(FHFoods.DRIED_WOLFBERRIES));
    public static Item rye = new FHBaseItem("rye", createProps());
    public static Item generator_ash = new FHBaseItem("generator_ash", createProps());
    public static Item frozen_seeds = new FHBaseItem("frozen_seeds", createProps());
    public static Item rye_flour = new FHBaseItem("rye_flour", createProps());
    public static Item raw_rye_bread = new FHBaseItem("raw_rye_bread", createProps());
    public static Item random_seeds = new FHBaseItem("random_seeds", createProps());
    public static Item mercury_body_thermometer = new ThermometerItem("mercury_body_thermometer", createProps());
    public static Item rye_bread = new FHBaseItem("rye_bread", createProps().food(FHFoods.RYE_BREAD));
    public static Item black_bread = new FHBaseItem("black_bread", createProps().food(FHFoods.BLACK_BREAD));
    public static Item vegetable_sawdust_soup = new FHSoupItem("vegetable_sawdust_soup", createProps().maxStackSize(1).food(FHFoods.VEGETABLE_SAWDUST_SOUP), true);
    public static Item rye_sawdust_porridge = new FHSoupItem("rye_sawdust_porridge", createProps().maxStackSize(1).food(FHFoods.RYE_SAWDUST_PORRIDGE), true);
    public static Item rye_porridge = new FHSoupItem("rye_porridge", createProps().maxStackSize(1).food(FHFoods.RYE_SAWDUST_PORRIDGE), false);
    public static Item vegetable_soup = new FHSoupItem("vegetable_soup", createProps().maxStackSize(1).food(FHFoods.VEGETABLE_SAWDUST_SOUP), false);
    public static Item military_rations = new CannedFoodItem("military_rations", createProps().food(new Food.Builder().hunger(6).saturation(0.6f).build()));
    public static Item compressed_biscuits_pack = new CannedFoodItem("compressed_biscuits_pack", createProps().food(Foods.BREAD));
    public static Item compressed_biscuits = new CannedFoodItem("compressed_biscuits", createProps().food(Foods.BREAD));
    public static Item packed_nuts=new CannedFoodItem("packed_nuts",createProps().food((new Food.Builder()).hunger(2).saturation(0.8F).build()),false);
    public static Item dried_vegetables=new CannedFoodItem("dried_vegetables",createProps().food((new Food.Builder()).hunger(4).saturation(0.6F).build()));
    public static Item chocolate=new FHBaseItem("chocolate",createProps().food((new Food.Builder()).hunger(4).saturation(0.8F).meat().fastToEat().build()));
    
    public static Item steam_bottle = new SteamBottleItem("steam_bottle", createProps().maxStackSize(1));
    public static Item raw_hide = new FHBaseItem("raw_hide", createProps());
    public static Item rubbing_tool=new RubbingTool("rubbing_tool",createProps().maxDamage(5).setNoRepair());
    public static Item rubbing_pad=new FHBaseItem("rubbing_pad",createProps().maxStackSize(1));
    public static Item buff_coat = new FHBaseItem("buff_coat", createProps().defaultMaxDamage(384)).setRepairItem(raw_hide);
    public static Item gambeson = new FHBaseItem("gambeson", createProps().defaultMaxDamage(384)).setRepairItem(Items.WHITE_WOOL);
    public static Item kelp_lining = new FHBaseItem("kelp_lining", createProps().defaultMaxDamage(256)).setRepairItem(Items.KELP);
    public static Item straw_lining = new FHBaseItem("straw_lining", createProps().defaultMaxDamage(256));
    public static Item hay_boots = new FHBaseArmorItem("hay_boots", FHArmorMaterial.HAY, EquipmentSlotType.FEET, createProps());
    public static Item hay_hat = new FHBaseArmorItem("hay_hat", FHArmorMaterial.HAY, EquipmentSlotType.HEAD, createProps());
    public static Item hay_jacket = new FHBaseArmorItem("hay_jacket", FHArmorMaterial.HAY, EquipmentSlotType.CHEST, createProps());
    public static Item hay_pants = new FHBaseArmorItem("hay_pants", FHArmorMaterial.HAY, EquipmentSlotType.LEGS, createProps());
    public static Item wool_boots = new FHBaseArmorItem("wool_boots", FHArmorMaterial.WOOL, EquipmentSlotType.FEET, createProps());
    public static Item wool_hat = new FHBaseArmorItem("wool_hat", FHArmorMaterial.WOOL, EquipmentSlotType.HEAD, createProps());
    public static Item wool_jacket = new FHBaseArmorItem("wool_jacket", FHArmorMaterial.WOOL, EquipmentSlotType.CHEST, createProps());
    public static Item wool_pants = new FHBaseArmorItem("wool_pants", FHArmorMaterial.WOOL, EquipmentSlotType.LEGS, createProps());
    public static Item hide_boots = new FHBaseArmorItem("hide_boots", FHArmorMaterial.HIDE, EquipmentSlotType.FEET, createProps());
    public static Item hide_hat = new FHBaseArmorItem("hide_hat", FHArmorMaterial.HIDE, EquipmentSlotType.HEAD, createProps());
    public static Item hide_jacket = new FHBaseArmorItem("hide_jacket", FHArmorMaterial.HIDE, EquipmentSlotType.CHEST, createProps());
    public static Item hide_pants = new FHBaseArmorItem("hide_pants", FHArmorMaterial.HIDE, EquipmentSlotType.LEGS, createProps());
    public static Item heater_vest = new HeaterVestItem("heater_vest", createProps().maxStackSize(1).setNoRepair());
    public static List<Item> allthermos=new ArrayList<>();
    public static List<Item> alladvthermos=new ArrayList<>();
    public static Item thermos = new ThermosItem("thermos","item.frostedheart.thermos", 1500, 250,true);
    public static Item advanced_thermos = new ThermosItem("advanced_thermos","item.frostedheart.advanced_thermos", 3000, 250,true);
    static {
	    for(String s:FHItems.colors) {
	    	allthermos.add(new ThermosItem(s+"_thermos","item.frostedheart.thermos", 1500, 250,false));
		}
	    for(String s:FHItems.colors) {
	    	alladvthermos.add(new ThermosItem(s+"_advanced_thermos","item.frostedheart.advanced_thermos", 3000, 250,false));
	    }
    }
	
    
    public static Item copper_pro_pick = new ProspectorPick("copper_pro_pick",1, createProps().defaultMaxDamage(128));
    public static Item iron_pro_pick = new ProspectorPick("iron_pro_pick", 2, createProps().defaultMaxDamage(192));
    public static Item steel_pro_pick = new ProspectorPick("steel_pro_pick", 3, createProps().defaultMaxDamage(256));
    public static Item copper_core_spade = new CoreSpade("copper_core_spade", 1, createProps().defaultMaxDamage(96));
    public static Item iron_core_spade = new CoreSpade("iron_core_spade", 2, createProps().defaultMaxDamage(128));
    public static Item steel_core_spade = new CoreSpade("steel_core_spade", 3, createProps().defaultMaxDamage(160));
    public static Item copper_geologists_hammer = new GeologistsHammer("copper_geologists_hammer",1, createProps().defaultMaxDamage(96));
    public static Item iron_geologists_hammer = new GeologistsHammer("iron_geologists_hammer", 2, createProps().defaultMaxDamage(128));
    public static Item steel_geologists_hammer = new GeologistsHammer("steel_geologists_hammer", 3, createProps().defaultMaxDamage(160));
    public static Item soil_thermometer = new SoilThermometer("soil_thermometer", createProps());
    public static Item heat_debuger = new HeatDebugItem("heat_debugger");
    public static Item red_mushroombed = new MushroomBed("straw_briquette_red_mushroom", Items.RED_MUSHROOM, createProps().defaultMaxDamage(4800));
    public static Item brown_mushroombed = new MushroomBed("straw_briquette_brown_mushroom", Items.BROWN_MUSHROOM, createProps().defaultMaxDamage(4800));
    public static Item ceramic_bucket = new CeramicBucket("ceramic_bucket", createProps().maxStackSize(1));
    public static Item charcoal = new FHBasePen("charcoal", createProps().maxDamage(50).setNoRepair());
    public static Item quill_and_ink = new FHReusablePen("quill_and_ink", createProps().maxDamage(101).setNoRepair(), 1);
    public static Item weatherHelmet = new FHBaseArmorItem("weather_helmet", FHArmorMaterial.WEATHER, EquipmentSlotType.HEAD, createProps());
    public static Item weatherRadar = new FHBaseItem("weather_radar", createProps().maxStackSize(1));
    public static Item temperatureProbe = new FHBaseItem("temperature_probe", createProps().maxStackSize(1));
}