/*
 * Copyright (c) 2022-2024 TeamMoeg
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
import java.util.function.Function;

import com.teammoeg.frostedheart.base.item.FHArmorMaterial;
import com.teammoeg.frostedheart.base.item.FHBaseArmorItem;
import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.content.foods.CannedFoodItem;
import com.teammoeg.frostedheart.content.foods.FHSoupItem;
import com.teammoeg.frostedheart.content.research.blocks.FHBasePen;
import com.teammoeg.frostedheart.content.research.blocks.FHReusablePen;
import com.teammoeg.frostedheart.content.research.blocks.RubbingTool;
import com.teammoeg.frostedheart.content.steamenergy.debug.HeatDebugItem;
import com.teammoeg.frostedheart.content.utility.DebugItem;
import com.teammoeg.frostedheart.content.utility.CeramicBucket;
import com.teammoeg.frostedheart.content.utility.GeneratorUpgraderI;
import com.teammoeg.frostedheart.content.utility.MushroomBed;
import com.teammoeg.frostedheart.content.utility.SoilThermometer;
import com.teammoeg.frostedheart.content.utility.SteamBottleItem;
import com.teammoeg.frostedheart.content.utility.ThermometerItem;
import com.teammoeg.frostedheart.content.utility.ThermosItem;
import com.teammoeg.frostedheart.content.utility.handstoves.CoalHandStove;
import com.teammoeg.frostedheart.content.utility.heatervest.HeaterVestItem;
import com.teammoeg.frostedheart.content.utility.oredetect.CoreSpade;
import com.teammoeg.frostedheart.content.utility.oredetect.GeologistsHammer;
import com.teammoeg.frostedheart.content.utility.oredetect.ProspectorPick;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Food;
import net.minecraft.item.Foods;
import net.minecraft.item.Item;
import net.minecraft.item.Item.Properties;
import net.minecraft.item.Items;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class FHItems {
	static final DeferredRegister<Item> registry=DeferredRegister.create(ForgeRegistries.ITEMS, FHMain.MODID);
	static <T extends Item> RegistryObject<T> register(String name,Function<String,T> supplier) {
		return registry.register(name,()->{
            //item.setRegistryName(FHMain.MODID, name);
			return supplier.apply(name);
		});
	}
    public static String[] colors = new String[]{"black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow"};

    public static RegistryObject<Item> hand_stove = register("hand_stove",n->new CoalHandStove( createProps().defaultMaxDamage(10)));

    public static RegistryObject<Item> debug_item = register("debug_item",n->new DebugItem( createProps()));

    public static RegistryObject<Item> coal_stick = register("coal_stick",n->new FHBaseItem( createProps()));

    public static RegistryObject<Item> charcoal_stick = register("charcoal_stick",n->new FHBaseItem( createProps()));
    public static RegistryObject<Item> energy_core = register("energy_core",n->new FHBaseItem( createProps()));
   // public static RegistryObject<Item> wolfberries = register("wolfberries",);
    public static RegistryObject<Item> dried_wolfberries = register("dried_wolfberries",n->new FHBaseItem( createProps().food(FHFoods.DRIED_WOLFBERRIES)));
    public static RegistryObject<Item> rye = register("rye",n->new FHBaseItem( createProps()));
    public static RegistryObject<Item> generator_ash = register("generator_ash",n->new FHBaseItem( createProps()));
    public static RegistryObject<Item> frozen_seeds = register("frozen_seeds",n->new FHBaseItem( createProps()));
    public static RegistryObject<Item> rye_flour = register("rye_flour",n->new FHBaseItem( createProps()));
    public static RegistryObject<Item> raw_rye_bread = register("raw_rye_bread",n->new FHBaseItem( createProps()));
    public static RegistryObject<Item> random_seeds = register("random_seeds",n->new FHBaseItem( createProps()));
    public static RegistryObject<Item> mercury_body_thermometer = register("mercury_body_thermometer",n->new ThermometerItem( createProps()));
    public static RegistryObject<Item> rye_bread = register("rye_bread",n->new FHBaseItem( createProps().food(FHFoods.RYE_BREAD)));
    public static RegistryObject<Item> black_bread = register("black_bread",n->new FHBaseItem( createProps().food(FHFoods.BLACK_BREAD)));
    public static RegistryObject<Item> vegetable_sawdust_soup = register("vegetable_sawdust_soup",n->new FHSoupItem( createProps().maxStackSize(1).food(FHFoods.VEGETABLE_SAWDUST_SOUP), true));
    public static RegistryObject<Item> rye_sawdust_porridge = register("rye_sawdust_porridge",n->new FHSoupItem( createProps().maxStackSize(1).food(FHFoods.RYE_SAWDUST_PORRIDGE), true));
    public static RegistryObject<Item> rye_porridge = register("rye_porridge",n->new FHSoupItem( createProps().maxStackSize(1).food(FHFoods.RYE_SAWDUST_PORRIDGE), false));
    public static RegistryObject<Item> vegetable_soup = register("vegetable_soup",n->new FHSoupItem( createProps().maxStackSize(1).food(FHFoods.VEGETABLE_SAWDUST_SOUP), false));
    public static RegistryObject<Item> military_rations = register("military_rations",n->new CannedFoodItem( createProps().food(new Food.Builder().hunger(6).saturation(0.6f).build())));
    public static RegistryObject<Item> compressed_biscuits_pack = register("compressed_biscuits_pack",n->new CannedFoodItem( createProps().food(Foods.BREAD)));
    public static RegistryObject<Item> compressed_biscuits = register("compressed_biscuits",n->new CannedFoodItem( createProps().food(Foods.BREAD)));
    public static RegistryObject<Item> packed_nuts = register("packed_nuts",n->new CannedFoodItem( createProps().food((new Food.Builder()).hunger(2).saturation(0.8F).build()), false));
    public static RegistryObject<Item> dried_vegetables = register("dried_vegetables",n->new CannedFoodItem( createProps().food((new Food.Builder()).hunger(4).saturation(0.6F).build())));
    public static RegistryObject<Item> chocolate = register("chocolate",n->new FHBaseItem( createProps().food((new Food.Builder()).hunger(4).saturation(0.8F).meat().fastToEat().build())));
    public static RegistryObject<Item> steam_bottle = register("steam_bottle",n->new SteamBottleItem( createProps().maxStackSize(1)));
    public static RegistryObject<Item> raw_hide = register("raw_hide",n->new FHBaseItem( createProps()));

    public static RegistryObject<Item> rubbing_tool = register("rubbing_tool",n->new RubbingTool(createProps().maxDamage(5).setNoRepair()));
    public static RegistryObject<Item> rubbing_pad = register("rubbing_pad",n->new FHBaseItem( createProps().maxStackSize(1)));
    public static RegistryObject<Item> buff_coat = register("buff_coat",n->new FHBaseItem( createProps().defaultMaxDamage(384)).setRepairItem(raw_hide.get()));
    public static RegistryObject<Item> gambeson = register("gambeson",n->new FHBaseItem( createProps().defaultMaxDamage(384)).setRepairItem(Items.WHITE_WOOL));
    public static RegistryObject<Item> kelp_lining = register("kelp_lining",n->new FHBaseItem( createProps().defaultMaxDamage(256)).setRepairItem(Items.KELP));
    public static RegistryObject<Item> straw_lining = register("straw_lining",n->new FHBaseItem( createProps().defaultMaxDamage(256)));
    public static RegistryObject<Item> hay_boots = register("hay_boots",n->new FHBaseArmorItem( FHArmorMaterial.HAY, EquipmentSlotType.FEET, createProps()));
    public static RegistryObject<Item> hay_hat = register("hay_hat",n->new FHBaseArmorItem( FHArmorMaterial.HAY, EquipmentSlotType.HEAD, createProps()));
    public static RegistryObject<Item> hay_jacket = register("hay_jacket",n->new FHBaseArmorItem( FHArmorMaterial.HAY, EquipmentSlotType.CHEST, createProps()));
    public static RegistryObject<Item> hay_pants = register("hay_pants",n->new FHBaseArmorItem( FHArmorMaterial.HAY, EquipmentSlotType.LEGS, createProps()));
    public static RegistryObject<Item> wool_boots = register("wool_boots",n->new FHBaseArmorItem( FHArmorMaterial.WOOL, EquipmentSlotType.FEET, createProps()));
    public static RegistryObject<Item> wool_hat = register("wool_hat",n->new FHBaseArmorItem( FHArmorMaterial.WOOL, EquipmentSlotType.HEAD, createProps()));
    public static RegistryObject<Item> wool_jacket = register("wool_jacket",n->new FHBaseArmorItem( FHArmorMaterial.WOOL, EquipmentSlotType.CHEST, createProps()));
    public static RegistryObject<Item> wool_pants = register("wool_pants",n->new FHBaseArmorItem( FHArmorMaterial.WOOL, EquipmentSlotType.LEGS, createProps()));
    public static RegistryObject<Item> hide_boots = register("hide_boots",n->new FHBaseArmorItem( FHArmorMaterial.HIDE, EquipmentSlotType.FEET, createProps()));
    public static RegistryObject<Item> hide_hat = register("hide_hat",n->new FHBaseArmorItem( FHArmorMaterial.HIDE, EquipmentSlotType.HEAD, createProps()));
    public static RegistryObject<Item> hide_jacket = register("hide_jacket",n->new FHBaseArmorItem( FHArmorMaterial.HIDE, EquipmentSlotType.CHEST, createProps()));
    public static RegistryObject<Item> hide_pants = register("hide_pants",n->new FHBaseArmorItem( FHArmorMaterial.HIDE, EquipmentSlotType.LEGS, createProps()));
    public static RegistryObject<Item> heater_vest = register("heater_vest",n->new HeaterVestItem( createProps().maxStackSize(1).setNoRepair()));
    public static List<RegistryObject<Item>> allthermos = new ArrayList<>();
    public static List<RegistryObject<Item>> alladvthermos = new ArrayList<>();
    public static RegistryObject<Item> thermos = register("thermos",n->new ThermosItem( "item.frostedheart.thermos", 1500, 250, true));
    public static RegistryObject<Item> advanced_thermos = register("advanced_thermos",n->new ThermosItem( "item.frostedheart.advanced_thermos", 3000, 250, true));
    public static RegistryObject<Item> generatorupgrader = register("generator_upgrade_i",n->new GeneratorUpgraderI( createProps().maxStackSize(0)));
    static {
        for (String s : FHItems.colors) {
            allthermos.add(register(s + "_thermos",n->new ThermosItem( "item.frostedheart.thermos", 1500, 250, false)));
        }
        for (String s : FHItems.colors) {
            alladvthermos.add(register(s + "_advanced_thermos",n->new ThermosItem( "item.frostedheart.advanced_thermos", 3000, 250, false)));
        }
    }
    public static RegistryObject<Item> copper_pro_pick = register("copper_pro_pick",n->new ProspectorPick( 1, createProps().defaultMaxDamage(128)));

    public static RegistryObject<Item> iron_pro_pick = register("iron_pro_pick",n->new ProspectorPick( 2, createProps().defaultMaxDamage(192)));


    public static RegistryObject<Item> steel_pro_pick = register("steel_pro_pick",n->new ProspectorPick( 3, createProps().defaultMaxDamage(256)));
    public static RegistryObject<Item> copper_core_spade = register("copper_core_spade",n->new CoreSpade( 1, createProps().defaultMaxDamage(96)));
    public static RegistryObject<Item> iron_core_spade = register("iron_core_spade",n->new CoreSpade( 2, createProps().defaultMaxDamage(128)));
    public static RegistryObject<Item> steel_core_spade = register("steel_core_spade",n->new CoreSpade( 3, createProps().defaultMaxDamage(160)));
    public static RegistryObject<Item> copper_geologists_hammer = register("copper_geologists_hammer",n->new GeologistsHammer( 1, createProps().defaultMaxDamage(96)));
    public static RegistryObject<Item> iron_geologists_hammer = register("iron_geologists_hammer",n->new GeologistsHammer( 2, createProps().defaultMaxDamage(128)));
    public static RegistryObject<Item> steel_geologists_hammer = register("steel_geologists_hammer",n->new GeologistsHammer( 3, createProps().defaultMaxDamage(160)));
    public static RegistryObject<Item> soil_thermometer = register("soil_thermometer",n->new SoilThermometer( createProps()));
    public static RegistryObject<Item> heat_debuger = register("heat_debugger",n->new HeatDebugItem());
    public static RegistryObject<Item> red_mushroombed = register("straw_briquette_red_mushroom",n->new MushroomBed( Items.RED_MUSHROOM, createProps().defaultMaxDamage(4800)));
    public static RegistryObject<Item> brown_mushroombed = register("straw_briquette_brown_mushroom",n->new MushroomBed( Items.BROWN_MUSHROOM, createProps().defaultMaxDamage(4800)));
    public static RegistryObject<Item> ceramic_bucket = register("ceramic_bucket",n->new CeramicBucket( createProps().maxStackSize(1)));
    public static RegistryObject<Item> charcoal = register("charcoal",n->new FHBasePen(createProps().maxDamage(50).setNoRepair()));
    public static RegistryObject<Item> quill_and_ink = register("quill_and_ink",n->new FHReusablePen(createProps().maxDamage(101).setNoRepair(), 1));
    public static RegistryObject<Item> weatherHelmet = register("weather_helmet",n->new FHBaseArmorItem( FHArmorMaterial.WEATHER, EquipmentSlotType.HEAD, createProps()));
    public static RegistryObject<Item> weatherRadar = register("weather_radar",n->new FHBaseItem( createProps().maxStackSize(1)));
    public static RegistryObject<Item> temperatureProbe = register("temperature_probe",n->new FHBaseItem( createProps().maxStackSize(1)));
    static Properties createProps() {
        return new Item.Properties().group(FHMain.itemGroup);
    }
    public static void init() {
    }

    //Mixxs section
    public static RegistryObject<Item> makeshift_core_broken = register("makeshift_core_broken",n->new FHBaseItem( createProps()));
    public static RegistryObject<Item> handheld_core = register("handheld_core",n->new FHBaseItem( createProps()));
    //public static RegistryObject<Item> body_lamp = register("heater_vest",n->new HeaterVestItem( createProps().maxStackSize(1).setNoRepair()));


}