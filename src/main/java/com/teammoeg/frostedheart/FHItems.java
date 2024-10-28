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
import com.teammoeg.frostedheart.content.ore.FHSnowballItem;
import com.teammoeg.frostedheart.content.research.blocks.FHBasePen;
import com.teammoeg.frostedheart.content.research.blocks.FHReusablePen;
import com.teammoeg.frostedheart.content.research.blocks.RubbingTool;
import com.teammoeg.frostedheart.content.steamenergy.debug.HeatDebugItem;
import com.teammoeg.frostedheart.content.utility.DebugItem;
import com.teammoeg.frostedheart.content.utility.CeramicBucket;
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

import com.teammoeg.frostedheart.content.water.item.FluidBottleItem;
import com.teammoeg.frostedheart.content.water.item.IronBottleItem;
import com.teammoeg.frostedheart.content.water.item.LeatherWaterBagItem;
import com.teammoeg.frostedheart.content.water.item.WoodenCupItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * All items.
 */
public class FHItems {

    static final DeferredRegister<Item> registry = DeferredRegister.create(ForgeRegistries.ITEMS, FHMain.MODID);

    // helper method: use FHBaseItem as the item class
    public static RegistryObject<Item> register(String name) {
        return register(name, n -> new FHBaseItem(createProps()));
    }

    static <T extends Item> RegistryObject<T> register(String name, Function<String, T> supplier) {
        return registry.register(name, () -> {
            //item.setRegistryName(FHMain.MODID, name);
            return supplier.apply(name);
        });
    }

    static Properties createProps() {
        return new Item.Properties();
    }

    public static void init() {

    }

    // Materials
    public static RegistryObject<Item> generator_ash = register("generator_ash", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> raw_hide = register("raw_hide", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> makeshift_core_broken = register("makeshift_core_broken", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> handheld_core = register("handheld_core", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> energy_core = register("energy_core", n -> new FHBaseItem(createProps()));

    // Foods and plants
    public static RegistryObject<Item> dried_wolfberries = register("dried_wolfberries", n -> new FHBaseItem(createProps().food(FHFoodProperties.DRIED_WOLFBERRIES)));
    public static RegistryObject<Item> rye = register("rye", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> frozen_seeds = register("frozen_seeds", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> rye_flour = register("rye_flour", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> raw_rye_bread = register("raw_rye_bread", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> random_seeds = register("random_seeds", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> rye_bread = register("rye_bread", n -> new FHBaseItem(createProps().food(FHFoodProperties.RYE_BREAD)));
    public static RegistryObject<Item> black_bread = register("black_bread", n -> new FHBaseItem(createProps().food(FHFoodProperties.BLACK_BREAD)));
    public static RegistryObject<Item> vegetable_sawdust_soup = register("vegetable_sawdust_soup", n -> new FHSoupItem(createProps().stacksTo(1).food(FHFoodProperties.VEGETABLE_SAWDUST_SOUP), true));
    public static RegistryObject<Item> rye_sawdust_porridge = register("rye_sawdust_porridge", n -> new FHSoupItem(createProps().stacksTo(1).food(FHFoodProperties.RYE_SAWDUST_PORRIDGE), true));
    public static RegistryObject<Item> rye_porridge = register("rye_porridge", n -> new FHSoupItem(createProps().stacksTo(1).food(FHFoodProperties.RYE_SAWDUST_PORRIDGE), false));
    public static RegistryObject<Item> vegetable_soup = register("vegetable_soup", n -> new FHSoupItem(createProps().stacksTo(1).food(FHFoodProperties.VEGETABLE_SAWDUST_SOUP), false));
    public static RegistryObject<Item> military_rations = register("military_rations", n -> new CannedFoodItem(createProps().food(new FoodProperties.Builder().nutrition(6).saturationMod(0.6f).build())));
    public static RegistryObject<Item> compressed_biscuits_pack = register("compressed_biscuits_pack", n -> new CannedFoodItem(createProps().food(Foods.BREAD)));
    public static RegistryObject<Item> compressed_biscuits = register("compressed_biscuits", n -> new CannedFoodItem(createProps().food(Foods.BREAD)));
    public static RegistryObject<Item> packed_nuts = register("packed_nuts", n -> new CannedFoodItem(createProps().food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.8F).build()), false));
    public static RegistryObject<Item> dried_vegetables = register("dried_vegetables", n -> new CannedFoodItem(createProps().food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.6F).build())));
    public static RegistryObject<Item> chocolate = register("chocolate", n -> new FHBaseItem(createProps().food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.8F).meat().fast().build())));


    // Equipment and tools
    public static RegistryObject<Item> hand_stove = register("hand_stove", n -> new CoalHandStove(createProps().defaultDurability(10)));
    public static RegistryObject<Item> debug_item = register("debug_item", n -> new DebugItem(createProps()));
    public static RegistryObject<Item> coal_stick = register("coal_stick", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> charcoal_stick = register("charcoal_stick", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> mercury_body_thermometer = register("mercury_body_thermometer", n -> new ThermometerItem(createProps()));
    public static RegistryObject<Item> steam_bottle = register("steam_bottle", n -> new SteamBottleItem(createProps().stacksTo(1)));
    public static RegistryObject<Item> rubbing_tool = register("rubbing_tool", n -> new RubbingTool(createProps().durability(5).setNoRepair()));
    public static RegistryObject<Item> rubbing_pad = register("rubbing_pad", n -> new FHBaseItem(createProps().stacksTo(1)));
    public static RegistryObject<Item> buff_coat = register("buff_coat", n -> new FHBaseItem(createProps().defaultDurability(384)).setRepairItem(raw_hide.get()));
    public static RegistryObject<Item> gambeson = register("gambeson", n -> new FHBaseItem(createProps().defaultDurability(384)).setRepairItem(Items.WHITE_WOOL));
    public static RegistryObject<Item> kelp_lining = register("kelp_lining", n -> new FHBaseItem(createProps().defaultDurability(256)).setRepairItem(Items.KELP));
    public static RegistryObject<Item> straw_lining = register("straw_lining", n -> new FHBaseItem(createProps().defaultDurability(256)));
    public static RegistryObject<Item> hay_boots = register("hay_boots", n -> new FHBaseArmorItem(FHArmorMaterial.HAY, Type.BOOTS, createProps()));
    public static RegistryObject<Item> hay_hat = register("hay_hat", n -> new FHBaseArmorItem(FHArmorMaterial.HAY, Type.HELMET, createProps()));
    public static RegistryObject<Item> hay_jacket = register("hay_jacket", n -> new FHBaseArmorItem(FHArmorMaterial.HAY, Type.CHESTPLATE, createProps()));
    public static RegistryObject<Item> hay_pants = register("hay_pants", n -> new FHBaseArmorItem(FHArmorMaterial.HAY, Type.LEGGINGS, createProps()));
    public static RegistryObject<Item> wool_boots = register("wool_boots", n -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.BOOTS, createProps()));
    public static RegistryObject<Item> wool_hat = register("wool_hat", n -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.HELMET, createProps()));
    public static RegistryObject<Item> wool_jacket = register("wool_jacket", n -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.CHESTPLATE, createProps()));
    public static RegistryObject<Item> wool_pants = register("wool_pants", n -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.BOOTS, createProps()));
    public static RegistryObject<Item> hide_boots = register("hide_boots", n -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.BOOTS, createProps()));
    public static RegistryObject<Item> hide_hat = register("hide_hat", n -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.HELMET, createProps()));
    public static RegistryObject<Item> hide_jacket = register("hide_jacket", n -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.CHESTPLATE, createProps()));
    public static RegistryObject<Item> hide_pants = register("hide_pants", n -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.BOOTS, createProps()));
    public static RegistryObject<HeaterVestItem> heater_vest = register("heater_vest", n -> new HeaterVestItem(createProps().stacksTo(1).setNoRepair()));
    public static RegistryObject<Item> copper_pro_pick = register("copper_pro_pick", n -> new ProspectorPick(1, createProps().defaultDurability(128)));
    public static RegistryObject<Item> iron_pro_pick = register("iron_pro_pick", n -> new ProspectorPick(2, createProps().defaultDurability(192)));
    public static RegistryObject<Item> steel_pro_pick = register("steel_pro_pick", n -> new ProspectorPick(3, createProps().defaultDurability(256)));
    public static RegistryObject<Item> copper_core_spade = register("copper_core_spade", n -> new CoreSpade(1, createProps().defaultDurability(96)));
    public static RegistryObject<Item> iron_core_spade = register("iron_core_spade", n -> new CoreSpade(2, createProps().defaultDurability(128)));
    public static RegistryObject<Item> steel_core_spade = register("steel_core_spade", n -> new CoreSpade(3, createProps().defaultDurability(160)));
    public static RegistryObject<Item> copper_geologists_hammer = register("copper_geologists_hammer", n -> new GeologistsHammer(1, createProps().defaultDurability(96)));
    public static RegistryObject<Item> iron_geologists_hammer = register("iron_geologists_hammer", n -> new GeologistsHammer(2, createProps().defaultDurability(128)));
    public static RegistryObject<Item> steel_geologists_hammer = register("steel_geologists_hammer", n -> new GeologistsHammer(3, createProps().defaultDurability(160)));
    public static RegistryObject<Item> soil_thermometer = register("soil_thermometer", n -> new SoilThermometer(createProps()));
    public static RegistryObject<Item> heat_debuger = register("heat_debugger", n -> new HeatDebugItem());
    public static RegistryObject<Item> red_mushroombed = register("straw_briquette_red_mushroom", n -> new MushroomBed(Items.RED_MUSHROOM, createProps().defaultDurability(4800)));
    public static RegistryObject<Item> brown_mushroombed = register("straw_briquette_brown_mushroom", n -> new MushroomBed(Items.BROWN_MUSHROOM, createProps().defaultDurability(4800)));
    public static RegistryObject<Item> ceramic_bucket = register("ceramic_bucket", n -> new CeramicBucket(createProps().stacksTo(1)));
    public static RegistryObject<Item> charcoal = register("charcoal", n -> new FHBasePen(createProps().durability(50).setNoRepair()));
    public static RegistryObject<Item> quill_and_ink = register("quill_and_ink", n -> new FHReusablePen(createProps().durability(101).setNoRepair(), 1));
    public static RegistryObject<Item> weatherHelmet = register("weather_helmet", n -> new FHBaseArmorItem(FHArmorMaterial.WEATHER, Type.HELMET, createProps()));
    public static RegistryObject<Item> weatherRadar = register("weather_radar", n -> new FHBaseItem(createProps().stacksTo(1)));
    public static RegistryObject<Item> temperatureProbe = register("temperature_probe", n -> new FHBaseItem(createProps().stacksTo(1)));

    // Thermos
    public static String[] colors = new String[]{"black", "blue", "brown", "cyan", "gray", "green", "light_blue",
            "light_gray", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow"};
    public static List<RegistryObject<Item>> allthermos = new ArrayList<>();
    public static List<RegistryObject<Item>> alladvthermos = new ArrayList<>();
    public static RegistryObject<Item> thermos = register("thermos", n -> new ThermosItem("item.frostedheart.thermos", 1500, 250, true));
    public static RegistryObject<Item> advanced_thermos = register("advanced_thermos", n -> new ThermosItem("item.frostedheart.advanced_thermos", 3000, 250, true));

    static {
        for (String s : FHItems.colors) {
            allthermos.add(register(s + "_thermos", n -> new ThermosItem("item.frostedheart.thermos", 1500, 250, false)));
        }
        for (String s : FHItems.colors) {
            alladvthermos.add(register(s + "_advanced_thermos", n -> new ThermosItem("item.frostedheart.advanced_thermos", 3000, 250, false)));
        }
    }

    // ADDITIONAL SOIL DROPS
    public static RegistryObject<Item> PEAT = register("peat", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> KAOLIN = register("kaolin", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> BAUXITE = register("bauxite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> ROTTEN_WOOD = register("rotten_wood", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_KAOLIN = register("crushed_raw_kaolin", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_BAUXITE = register("crushed_raw_bauxite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> KAOLIN_DUST = register("kaolin_dust", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> BAUXITE_DUST = register("bauxite_dust", n -> new FHBaseItem(createProps()));

    // CONDENSED BALLS
    public static RegistryObject<Item> CONDENSED_BALL_IRON_ORE = register("condensed_ball_iron_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_COPPER_ORE = register("condensed_ball_copper_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_GOLD_ORE = register("condensed_ball_gold_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_ZINC_ORE = register("condensed_ball_zinc_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_SILVER_ORE = register("condensed_ball_silver_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_CASSITERITE_ORE = register("condensed_ball_cassiterite_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_PYRITE_ORE = register("condensed_ball_pyrite_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_PENTLANDITE_ORE = register("condensed_ball_pentlandite_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));
    public static RegistryObject<Item> CONDENSED_BALL_GALENA_ORE = register("condensed_ball_galena_ore", n -> new FHSnowballItem(createProps().stacksTo(16)));

    // CRUSHED ORES
    public static RegistryObject<Item> CRUSHED_SILVER_ORE = register("crushed_raw_silver", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_CASSITERITE_ORE = register("crushed_raw_cassiterite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_PYRITE_ORE = register("crushed_raw_pyrite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_PENTLANDITE_ORE = register("crushed_raw_pentlandite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_GALENA_ORE = register("crushed_raw_galena", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_HALITE_ORE = register("crushed_raw_halite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_POTASH_ORE = register("crushed_raw_potash", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> CRUSHED_MAGNESITE_ORE = register("crushed_raw_magnesite", n -> new FHBaseItem(createProps()));

    // RAW ORES
    public static RegistryObject<Item> RAW_SILVER = register("raw_silver", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_CASSITERITE = register("raw_cassiterite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_PYRITE = register("raw_pyrite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_PENTLANDITE = register("raw_pentlandite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_GALENA = register("raw_galena", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_HALITE = register("raw_halite", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_POTASH = register("raw_potash", n -> new FHBaseItem(createProps()));
    public static RegistryObject<Item> RAW_MAGNESITE = register("raw_magnesite", n -> new FHBaseItem(createProps()));

    // MATERIALS

    /*
    copper
    aluminum
    steel
    electrum
    constantan
    iron
    cast_iron
    brass
    duralumin
    gold
    silver
    nickel
    lead
    titanium
    bronze
    invar
    tungsten_steel
    zinc
    tin
    magnesium
    tungsten
     */

    // DUSTS
    public static RegistryObject<Item> COPPER_DUST = register("copper_dust");
    public static RegistryObject<Item> ALUMINUM_DUST = register("aluminum_dust");
    public static RegistryObject<Item> STEEL_DUST = register("steel_dust");
    public static RegistryObject<Item> ELECTRUM_DUST = register("electrum_dust");
    public static RegistryObject<Item> CONSTANTAN_DUST = register("constantan_dust");
    public static RegistryObject<Item> IRON_DUST = register("iron_dust");
    public static RegistryObject<Item> CAST_IRON_DUST = register("cast_iron_dust");
    public static RegistryObject<Item> BRASS_DUST = register("brass_dust");
    public static RegistryObject<Item> DURALUMIN_DUST = register("duralumin_dust");
    public static RegistryObject<Item> GOLD_DUST = register("gold_dust");
    public static RegistryObject<Item> SILVER_DUST = register("silver_dust");
    public static RegistryObject<Item> NICKEL_DUST = register("nickel_dust");
    public static RegistryObject<Item> LEAD_DUST = register("lead_dust");
    public static RegistryObject<Item> TITANIUM_DUST = register("titanium_dust");
    public static RegistryObject<Item> BRONZE_DUST = register("bronze_dust");
    public static RegistryObject<Item> INVAR_DUST = register("invar_dust");
    public static RegistryObject<Item> TUNGSTEN_STEEL_DUST = register("tungsten_steel_dust");
    public static RegistryObject<Item> ZINC_DUST = register("zinc_dust");
    public static RegistryObject<Item> TIN_DUST = register("tin_dust");
    public static RegistryObject<Item> MAGNESIUM_DUST = register("magnesium_dust");
    public static RegistryObject<Item> TUNGSTEN_DUST = register("tungsten_dust");

    // INGOTS

    // public static RegistryObject<Item> COPPER_INGOT = register("copper_ingot");
    public static RegistryObject<Item> ALUMINUM_INGOT = register("aluminum_ingot");
    public static RegistryObject<Item> STEEL_INGOT = register("steel_ingot");
    public static RegistryObject<Item> ELECTRUM_INGOT = register("electrum_ingot");
    public static RegistryObject<Item> CONSTANTAN_INGOT = register("constantan_ingot");
    // public static RegistryObject<Item> IRON_INGOT = register("iron_ingot");
    public static RegistryObject<Item> CAST_IRON_INGOT = register("cast_iron_ingot");
    // public static RegistryObject<Item> BRASS_INGOT = register("brass_ingot");
    public static RegistryObject<Item> DURALUMIN_INGOT = register("duralumin_ingot");
    // public static RegistryObject<Item> GOLD_INGOT = register("gold_ingot");
    public static RegistryObject<Item> SILVER_INGOT = register("silver_ingot");
    public static RegistryObject<Item> NICKEL_INGOT = register("nickel_ingot");
    public static RegistryObject<Item> LEAD_INGOT = register("lead_ingot");
    public static RegistryObject<Item> TITANIUM_INGOT = register("titanium_ingot");
    public static RegistryObject<Item> BRONZE_INGOT = register("bronze_ingot");
    public static RegistryObject<Item> INVAR_INGOT = register("invar_ingot");
    public static RegistryObject<Item> TUNGSTEN_STEEL_INGOT = register("tungsten_steel_ingot");
    // public static RegistryObject<Item> ZINC_INGOT = register("zinc_ingot");
    public static RegistryObject<Item> TIN_INGOT = register("tin_ingot");
    public static RegistryObject<Item> MAGNESIUM_INGOT = register("magnesium_ingot");
    public static RegistryObject<Item> TUNGSTEN_INGOT = register("tungsten_ingot");

    // NUGGET
    // public static RegistryObject<Item> COPPER_NUGGET = register("copper_nugget");
    public static RegistryObject<Item> ALUMINUM_NUGGET = register("aluminum_nugget");
    public static RegistryObject<Item> STEEL_NUGGET = register("steel_nugget");
    public static RegistryObject<Item> ELECTRUM_NUGGET = register("electrum_nugget");
    public static RegistryObject<Item> CONSTANTAN_NUGGET = register("constantan_nugget");
    // public static RegistryObject<Item> IRON_NUGGET = register("iron_nugget");
    public static RegistryObject<Item> CAST_IRON_NUGGET = register("cast_iron_nugget");
    // public static RegistryObject<Item> BRASS_NUGGET = register("brass_nugget");
    public static RegistryObject<Item> DURALUMIN_NUGGET = register("duralumin_nugget");
    // public static RegistryObject<Item> GOLD_NUGGET = register("gold_nugget");
    public static RegistryObject<Item> SILVER_NUGGET = register("silver_nugget");
    public static RegistryObject<Item> NICKEL_NUGGET = register("nickel_nugget");
    public static RegistryObject<Item> LEAD_NUGGET = register("lead_nugget");
    public static RegistryObject<Item> TITANIUM_NUGGET = register("titanium_nugget");
    public static RegistryObject<Item> BRONZE_NUGGET = register("bronze_nugget");
    public static RegistryObject<Item> INVAR_NUGGET = register("invar_nugget");
    public static RegistryObject<Item> TUNGSTEN_STEEL_NUGGET = register("tungsten_steel_nugget");
    // public static RegistryObject<Item> ZINC_NUGGET = register("zinc_nugget");
    public static RegistryObject<Item> TIN_NUGGET = register("tin_nugget");
    public static RegistryObject<Item> MAGNESIUM_NUGGET = register("magnesium_nugget");
    public static RegistryObject<Item> TUNGSTEN_NUGGET = register("tungsten_nugget");

    // SHEETS
    // public static RegistryObject<Item> COPPER_SHEET = register("copper_sheet");
    public static RegistryObject<Item> ALUMINUM_SHEET = register("aluminum_sheet");
    public static RegistryObject<Item> STEEL_SHEET = register("steel_sheet");
    public static RegistryObject<Item> ELECTRUM_SHEET = register("electrum_sheet");
    public static RegistryObject<Item> CONSTANTAN_SHEET = register("constantan_sheet");
    // public static RegistryObject<Item> IRON_SHEET = register("iron_sheet");
    public static RegistryObject<Item> CAST_IRON_SHEET = register("cast_iron_sheet");
    // public static RegistryObject<Item> BRASS_SHEET = register("brass_sheet");
    public static RegistryObject<Item> DURALUMIN_SHEET = register("duralumin_sheet");
    // public static RegistryObject<Item> GOLD_SHEET = register("gold_sheet");
    public static RegistryObject<Item> SILVER_SHEET = register("silver_sheet");
    public static RegistryObject<Item> NICKEL_SHEET = register("nickel_sheet");
    public static RegistryObject<Item> LEAD_SHEET = register("lead_sheet");
    public static RegistryObject<Item> TITANIUM_SHEET = register("titanium_sheet");
    public static RegistryObject<Item> BRONZE_SHEET = register("bronze_sheet");
    public static RegistryObject<Item> INVAR_SHEET = register("invar_sheet");
    public static RegistryObject<Item> TUNGSTEN_STEEL_SHEET = register("tungsten_steel_sheet");

    // RODS
    public static RegistryObject<Item> COPPER_ROD = register("copper_rod");
    public static RegistryObject<Item> ALUMINUM_ROD = register("aluminum_rod");
    public static RegistryObject<Item> STEEL_ROD = register("steel_rod");
    public static RegistryObject<Item> ELECTRUM_ROD = register("electrum_rod");
    public static RegistryObject<Item> CONSTANTAN_ROD = register("constantan_rod");
    public static RegistryObject<Item> IRON_ROD = register("iron_rod");
    public static RegistryObject<Item> CAST_IRON_ROD = register("cast_iron_rod");

    // WIRES
    public static RegistryObject<Item> COPPER_WIRE = register("copper_wire");
    public static RegistryObject<Item> ALUMINUM_WIRE = register("aluminum_wire");
    public static RegistryObject<Item> STEEL_WIRE = register("steel_wire");
    public static RegistryObject<Item> ELECTRUM_WIRE = register("electrum_wire");
    public static RegistryObject<Item> CONSTANTAN_WIRE = register("constantan_wire");

    // RUSTED ITEMS
    public static RegistryObject<Item> RUSTED_IRON_INGOT = register("rusted_iron_ingot");
    public static RegistryObject<Item> RUSTED_COPPER_INGOT = register("rusted_copper_ingot");
    public static RegistryObject<Item> GRAY_TIN_INGOT = register("gray_tin_ingot");

    // SLUGS
    public static RegistryObject<Item> IRON_SLUG = register("iron_slug");
    public static RegistryObject<Item> NICKEL_SLUG = register("nickel_slug");

    // CHEMICALS in dust
    /*
    copper_oxide
    zinc_oxide
    tin_oxide
    alumina
    magnesia
    lead_oxide
    aluminium_hydroxide
    sodium_hydroxide
    sodium_sulfide
    sodium_choloride
    potassium_choloride
    sulfur
    graphite
    cryolite
     */

    public static RegistryObject<Item> COPPER_OXIDE_DUST = register("copper_oxide_dust");
    public static RegistryObject<Item> ZINC_OXIDE_DUST = register("zinc_oxide_dust");
    public static RegistryObject<Item> TIN_OXIDE_DUST = register("tin_oxide_dust");
    public static RegistryObject<Item> ALUMINA_DUST = register("alumina_dust");
    public static RegistryObject<Item> MAGNESIA_DUST = register("magnesia_dust");
    public static RegistryObject<Item> LEAD_OXIDE_DUST = register("lead_oxide_dust");
    public static RegistryObject<Item> ALUMINIUM_HYDROXIDE_DUST = register("aluminium_hydroxide_dust");
    public static RegistryObject<Item> SODIUM_HYDROXIDE_DUST = register("sodium_hydroxide_dust");
    public static RegistryObject<Item> SODIUM_SULFIDE_DUST = register("sodium_sulfide_dust");
    public static RegistryObject<Item> SODIUM_CHLORIDE_DUST = register("sodium_chloride_dust");
    public static RegistryObject<Item> POTASSIUM_CHLORIDE_DUST = register("potassium_chloride_dust");
    public static RegistryObject<Item> SULFUR_DUST = register("sulfur_dust");
    public static RegistryObject<Item> GRAPHITE_DUST = register("graphite_dust");
    public static RegistryObject<Item> CRYOLITE_DUST = register("cryolite_dust");

    // MISC MATERIALS
    /*
    mortar
    vulcanized_rubber

    pulp

    fire_clay_ball

    high_refractory_brick

    sawdust
    biomass
    synthetic_leather
    quicklime
    sodium_ingot
    refractory_brick

     */

    public static RegistryObject<Item> MORTAR = register("mortar");
    public static RegistryObject<Item> VULCANIZED_RUBBER = register("vulcanized_rubber");
    public static RegistryObject<Item> PULP = register("pulp");
    public static RegistryObject<Item> FIRE_CLAY_BALL = register("fire_clay_ball");
    public static RegistryObject<Item> HIGH_REFRACTORY_BRICK = register("high_refractory_brick");
    public static RegistryObject<Item> SAWDUST = register("sawdust");
    public static RegistryObject<Item> BIOMASS = register("biomass");
    public static RegistryObject<Item> SYNTHETIC_LEATHER = register("synthetic_leather");
    public static RegistryObject<Item> QUICKLIME = register("quicklime");
    public static RegistryObject<Item> SODIUM_INGOT = register("sodium_ingot");
    public static RegistryObject<Item> REFRACTORY_BRICK = register("refractory_brick");

    //WaterSource section
    public final static RegistryObject<Item> fluid_bottle = register("fluid_bottle", (s) -> new FluidBottleItem(new Item.Properties().stacksTo(16)));
    public final static RegistryObject<Item> wooden_cup = register("wooden_cup", (s) -> new WoodenCupItem(new Item.Properties(), 250) {@Override public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundTag nbt) {return super.initCapabilities(new ItemStack(wooden_cup_drink.get()), nbt);}});
    public final static RegistryObject<Item> wooden_cup_drink = register("wooden_cup_drink", (s) -> new WoodenCupItem(new Item.Properties().stacksTo(1), 250));
    public final static RegistryObject<Item> LEATHER_WATER_BAG = register("leather_water_bag", (s) -> new LeatherWaterBagItem(new Item.Properties().stacksTo(1).setNoRepair(), 1500));
    public final static RegistryObject<Item> IRON_BOTTLE = register("iron_bottle", (s) -> new IronBottleItem(new Item.Properties().stacksTo(1).setNoRepair(), 1500));
}