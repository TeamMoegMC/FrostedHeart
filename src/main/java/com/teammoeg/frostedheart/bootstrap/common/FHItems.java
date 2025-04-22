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

package com.teammoeg.frostedheart.bootstrap.common;

import java.util.function.Function;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.item.ItemDescription;
import com.teammoeg.caupona.CPTags;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.item.DyedItemList;
import com.teammoeg.frostedheart.bootstrap.reference.FHArmorMaterial;
import com.teammoeg.frostedheart.item.FHBaseArmorItem;
import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.item.FHToolMaterials;

import com.teammoeg.frostedheart.bootstrap.client.FHTabs;
import com.teammoeg.frostedheart.bootstrap.reference.FHFoodProperties;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.health.food.CannedFoodItem;
import com.teammoeg.frostedheart.content.health.food.FHSoupItem;
import com.teammoeg.frostedheart.content.world.item.FHSnowballItem;
import com.teammoeg.frostedheart.content.steamenergy.debug.HeatDebugItem;
import com.teammoeg.frostedheart.content.utility.CeramicBucket;
import com.teammoeg.frostedheart.content.utility.DebugItem;
import com.teammoeg.frostedheart.content.utility.KnifeItem;
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
import com.teammoeg.frostedheart.content.utility.transportation.PowderedSnowWalkable;
import com.teammoeg.frostedheart.content.water.item.FluidBottleItem;
import com.teammoeg.frostedheart.content.water.item.IronBottleItem;
import com.teammoeg.frostedheart.content.water.item.LeatherWaterBagItem;
import com.teammoeg.frostedheart.content.water.item.WoodenCupItem;
import com.teammoeg.frostedheart.util.FHAssetsUtils;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.*;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.teammoeg.frostedheart.FHMain.REGISTRATE;
import static com.teammoeg.frostedheart.bootstrap.reference.FHTags.forgeItemTag;

/**
 * All items.
 */
public class FHItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FHMain.MODID);

    @Deprecated
    protected static RegistryObject<Item> register(String name) {
        return register(name, n -> new FHBaseItem(createProps()));
    }

    @Deprecated
    protected static <T extends Item> RegistryObject<T> register(String name, Function<String, T> supplier) {
        return ITEMS.register(name, () -> supplier.apply(name));
    }

    protected static Properties createProps() {
        return new Item.Properties();
    }

    public static void init() {

    }

    private static ItemEntry<Item> ingredient(String name) {
        return REGISTRATE.item(name, Item::new)
                .register();
    }

    private static ItemEntry<SequencedAssemblyItem> sequencedIngredient(String name) {
        return REGISTRATE.item(name, SequencedAssemblyItem::new)
                .register();
    }

    @SafeVarargs
    private static ItemEntry<Item> taggedIngredient(String name, TagKey<Item>... tags) {
        return REGISTRATE.item(name, Item::new)
                .tag(tags)
                .register();
    }

    // REGISTRATE STYLE REGISTRY - USE THIS!

    static {
        REGISTRATE.setCreativeTab(FHTabs.MISC);
    }

    // Spawn Eggs
    // Well, you MUST use ForgeSpawnEggItem, to take in the RegistryObject Supplier, instead of the EntityType itself,
    // because ITEMS always register before ENTITY_TYPES. I wasted 2 hours on this.
    public static final ItemEntry<ForgeSpawnEggItem> CURIOSITY_SPAWN_EGG =
            REGISTRATE.item("curiosity_spawn_egg", p -> new ForgeSpawnEggItem(FHEntityTypes.CURIOSITY, 0xfffeff, 0xafbdc0, createProps()))
                    .lang("Curiosity of Deep Frostland Spawn Egg")
                    .model(AssetLookup.existingItemModel())
                    .register();
    public static final ItemEntry<ForgeSpawnEggItem> WANDERING_REFUGEE_SPAWN_EGG =
            REGISTRATE.item("wandering_refugee_spawn_egg", p -> new ForgeSpawnEggItem(FHEntityTypes.WANDERING_REFUGEE, 0xfffeff, 0x11374f, createProps()))
                    .lang("Wandering Refugee Spawn Egg")
                    .model(AssetLookup.existingItemModel())
                    .register();
    /*
    antifreeze.png
cable.png
deflection_coil.png
engine_oil.png
ferrocerium_fire_starter.png
insulator.png
iron_fence_remains.png
iron_parts.png
iron_plating.png
iron_truss_remains.png
lead_acid_battery.png
moly_lubricant.png
rust_remover.png
silicone_grease.png
stator.png
thinner.png
     */
    // add these as simple taggedIngredient, with the tag FHTags.GARBAGE.tag
    public static ItemEntry<Item> ANTIFREEZE =
            taggedIngredient("antifreeze",
                    FHTags.Items.GARBAGE.tag);
    public static ItemEntry<Item> CABLE =
            taggedIngredient("cable",
                    FHTags.Items.GARBAGE.tag,
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> DEFLECTION_COIL =
            taggedIngredient("deflection_coil",
                    FHTags.Items.GARBAGE.tag,
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> ENGINE_OIL =
            taggedIngredient("engine_oil",
                    FHTags.Items.GARBAGE.tag);
    public static ItemEntry<Item> FERROCERIUM_FIRE_STARTER =
            taggedIngredient("ferrocerium_fire_starter",
                    FHTags.Items.GARBAGE.tag,
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> INSULATOR =
            taggedIngredient("insulator",
                    FHTags.Items.GARBAGE.tag,
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> IRON_FENCE_REMAINS =
            taggedIngredient("iron_fence_remains",
                    FHTags.Items.GARBAGE.tag,
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> IRON_PARTS =
            taggedIngredient("iron_parts",
                    FHTags.Items.GARBAGE.tag,
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> IRON_PLATING =
            taggedIngredient("iron_plating",
                    FHTags.Items.GARBAGE.tag,
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> IRON_TRUSS_REMAINS =
            taggedIngredient("iron_truss_remains",
                    FHTags.Items.GARBAGE.tag,
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> LEAD_ACID_BATTERY =
            taggedIngredient("lead_acid_battery", FHTags.Items.GARBAGE.tag);
    public static ItemEntry<Item> MOLY_LUBRICANT =
            taggedIngredient("moly_lubricant", FHTags.Items.GARBAGE.tag);
    public static ItemEntry<Item> RUST_REMOVER =
            taggedIngredient("rust_remover", FHTags.Items.GARBAGE.tag);
    public static ItemEntry<Item> SILICONE_GREASE =
            taggedIngredient("silicone_grease", FHTags.Items.GARBAGE.tag);
    public static ItemEntry<Item> STATOR =
            taggedIngredient("stator", FHTags.Items.GARBAGE.tag);
    public static ItemEntry<Item> THINNER =
            taggedIngredient("thinner", FHTags.Items.GARBAGE.tag);


    static {
        REGISTRATE.setCreativeTab(FHTabs.INGREDIENTS);
    }

    // ice_chips
    public static ItemEntry<FHSnowballItem> ICE_CHIP =
            REGISTRATE.item("ice_chip", FHSnowballItem::new)
                    .tag(forgeItemTag("raw_materials/ice"))
                    .register();
    // Soil drops
    public static ItemEntry<Item> PEAT =
            taggedIngredient("peat",
                    forgeItemTag("raw_materials"),
                    forgeItemTag("raw_materials/peat"));
    public static ItemEntry<Item> KAOLIN =
            taggedIngredient("kaolin",
                    forgeItemTag("raw_materials"),
                    forgeItemTag("raw_materials/kaolin"));
    public static ItemEntry<Item> BAUXITE =
            taggedIngredient("bauxite",
                    forgeItemTag("raw_materials"),
                    forgeItemTag("raw_materials/bauxite")
            );
    public static ItemEntry<Item> ROTTEN_WOOD =
            ingredient("rotten_wood");
    public static ItemEntry<Item> CRUSHED_RAW_BAUXITE =
            taggedIngredient("crushed_raw_bauxite",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/bauxite"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);
    public static ItemEntry<Item> CRUSHED_RAW_KAOLIN =
            taggedIngredient("crushed_raw_kaolin",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/kaolin"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);
    public static ItemEntry<Item> KAOLIN_DUST =
            taggedIngredient("kaolin_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/kaolin"));
    public static ItemEntry<Item> BAUXITE_DUST =
            taggedIngredient("bauxite_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/bauxite"));

    // Redo condensed balls
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_IRON_ORE =
            REGISTRATE.item("condensed_ball_iron_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Iron Aeroslit Ball")
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_COPPER_ORE =
            REGISTRATE.item("condensed_ball_copper_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Copper Aeroslit Ball")
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_GOLD_ORE =
            REGISTRATE.item("condensed_ball_gold_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Gold Aeroslit Ball")
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_ZINC_ORE =
            REGISTRATE.item("condensed_ball_zinc_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Zinc Aeroslit Ball")
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_SILVER_ORE =
            REGISTRATE.item("condensed_ball_silver_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Silver Aeroslit Ball")
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_TIN_ORE =
            REGISTRATE.item("condensed_ball_tin_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Tin Aeroslit Ball")
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_PYRITE_ORE =
            REGISTRATE.item("condensed_ball_pyrite_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Pyrite Aeroslit Ball")
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_NICKEL_ORE =
            REGISTRATE.item("condensed_ball_nickel_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Nickel Aeroslit Ball")
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_LEAD_ORE =
            REGISTRATE.item("condensed_ball_lead_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .lang("Lead Aeroslit Ball")
                    .register();

    // Slurry
    public static ItemEntry<Item> IRON_SLURRY =
            taggedIngredient("iron_slurry", FHTags.Items.SLURRY.tag);
    public static ItemEntry<Item> COPPER_SLURRY =
            taggedIngredient("copper_slurry", FHTags.Items.SLURRY.tag);
    public static ItemEntry<Item> GOLD_SLURRY =
            taggedIngredient("gold_slurry", FHTags.Items.SLURRY.tag);
    public static ItemEntry<Item> ZINC_SLURRY =
            taggedIngredient("zinc_slurry", FHTags.Items.SLURRY.tag);
    public static ItemEntry<Item> SILVER_SLURRY =
            taggedIngredient("silver_slurry", FHTags.Items.SLURRY.tag);
    public static ItemEntry<Item> TIN_SLURRY =
            taggedIngredient("tin_slurry", FHTags.Items.SLURRY.tag);
    public static ItemEntry<Item> PYRITE_SLURRY =
            taggedIngredient("pyrite_slurry", FHTags.Items.SLURRY.tag);
    public static ItemEntry<Item> NICKEL_SLURRY =
            taggedIngredient("nickel_slurry", FHTags.Items.SLURRY.tag);
    public static ItemEntry<Item> LEAD_SLURRY =
            taggedIngredient("lead_slurry", FHTags.Items.SLURRY.tag);

    // Crushed ores
    public static ItemEntry<Item> CRUSHED_RAW_TIN =
            taggedIngredient("crushed_raw_tin",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/tin"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);
    public static ItemEntry<Item> CRUSHED_RAW_PYRITE =
            taggedIngredient("crushed_raw_pyrite",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/pyrite"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);
    public static ItemEntry<Item> CRUSHED_RAW_HALITE =
            taggedIngredient("crushed_raw_halite",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/halite"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);
    public static ItemEntry<Item> CRUSHED_RAW_SYLVITE =
            taggedIngredient("crushed_raw_sylvite",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/sylvite"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);
    public static ItemEntry<Item> CRUSHED_RAW_MAGNESITE =
            taggedIngredient("crushed_raw_magnesite",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/magnesite"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);

    // Raw ores
    public static ItemEntry<Item> RAW_TIN =
            taggedIngredient("raw_tin",
                    forgeItemTag("raw_materials/tin"),
                    forgeItemTag("raw_materials"));
    public static ItemEntry<Item> RAW_PYRITE =
            taggedIngredient("raw_pyrite",
                    forgeItemTag("raw_materials/pyrite"),
                    forgeItemTag("raw_materials"),
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> RAW_HALITE =
            taggedIngredient("raw_halite",
                    forgeItemTag("raw_materials/halite"),
                    forgeItemTag("raw_materials/salt"),
                    forgeItemTag("raw_materials"));
    public static ItemEntry<Item> RAW_SYLVITE =
            taggedIngredient("raw_sylvite",
                    forgeItemTag("raw_materials/sylvite"),
                    forgeItemTag("raw_materials/potash"),
                    forgeItemTag("raw_materials"));
    public static ItemEntry<Item> RAW_MAGNESITE =
            taggedIngredient("raw_magnesite",
                    forgeItemTag("raw_materials/magnesite"),
                    forgeItemTag("raw_materials"));

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

    // Dusts
    public static ItemEntry<Item> COPPER_DUST =
            taggedIngredient("copper_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/copper")
            );
    public static ItemEntry<Item> ALUMINUM_DUST =
            taggedIngredient("aluminum_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/aluminum")
            );
//    public static ItemEntry<Item> STEEL_DUST =
//            taggedIngredient("steel_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/steel")
//            );
//    public static ItemEntry<Item> ELECTRUM_DUST =
//            taggedIngredient("electrum_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/electrum")
//            );
//    public static ItemEntry<Item> CONSTANTAN_DUST =
//            taggedIngredient("constantan_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/constantan")
//            );
//    public static ItemEntry<Item> IRON_DUST =
//            taggedIngredient("iron_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/iron")
//            );
//    public static ItemEntry<Item> CAST_IRON_DUST =
//            taggedIngredient("cast_iron_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/cast_iron")
//            );
//    public static ItemEntry<Item> BRASS_DUST =
//            taggedIngredient("brass_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/brass")
//            );
    public static ItemEntry<Item> DURALUMIN_DUST =
            taggedIngredient("duralumin_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/duralumin")
            );
    public static ItemEntry<Item> GOLD_DUST =
            taggedIngredient("gold_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/gold")
            );
    public static ItemEntry<Item> SILVER_DUST =
            taggedIngredient("silver_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/silver")
            );
    public static ItemEntry<Item> NICKEL_DUST =
            taggedIngredient("nickel_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/nickel")
            );
    public static ItemEntry<Item> LEAD_DUST =
            taggedIngredient("lead_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/lead")
            );
    public static ItemEntry<Item> TITANIUM_DUST =
            taggedIngredient("titanium_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/titanium")
            );
//    public static ItemEntry<Item> BRONZE_DUST =
//            taggedIngredient("bronze_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/bronze")
//            );
//    public static ItemEntry<Item> INVAR_DUST =
//            taggedIngredient("invar_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/invar")
//            );
//    public static ItemEntry<Item> TUNGSTEN_STEEL_DUST =
//            taggedIngredient("tungsten_steel_dust",
//                    forgeItemTag("dusts"),
//                    forgeItemTag("dusts/tungsten_steel")
//            );
    public static ItemEntry<Item> ZINC_DUST =
            taggedIngredient("zinc_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/zinc")
            );
    public static ItemEntry<Item> TIN_DUST =
            taggedIngredient("tin_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/tin")
            );
    public static ItemEntry<Item> MAGNESIUM_DUST =
            taggedIngredient("magnesium_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/magnesium")
            );
    public static ItemEntry<Item> TUNGSTEN_DUST =
            taggedIngredient("tungsten_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/tungsten")
            );

    // Ingots
//    public static ItemEntry<Item> COPPER_INGOT =
//            taggedIngredient("copper_ingot",
//                    forgeItemTag("ingots"),
//                    forgeItemTag("ingots/copper")
//            );
    public static ItemEntry<Item> ALUMINUM_INGOT =
            taggedIngredient("aluminum_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/aluminum")
            );
    public static ItemEntry<Item> STEEL_INGOT =
            taggedIngredient("steel_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/steel"),
                    FHTags.Items.IGNITION_METAL.tag
            );
    public static ItemEntry<Item> ELECTRUM_INGOT =
            taggedIngredient("electrum_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/electrum")
            );
    public static ItemEntry<Item> CONSTANTAN_INGOT =
            taggedIngredient("constantan_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/constantan")
            );
//    public static ItemEntry<Item> IRON_INGOT =
//            taggedIngredient("iron_ingot",
//                    forgeItemTag("ingots"),
//                    forgeItemTag("ingots/iron")
//            );
    public static ItemEntry<Item> CAST_IRON_INGOT =
            taggedIngredient("cast_iron_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/cast_iron"),
                    FHTags.Items.IGNITION_METAL.tag
            );
//    public static ItemEntry<Item> BRASS_INGOT =
//            taggedIngredient("brass_ingot",
//                    forgeItemTag("ingots"),
//                    forgeItemTag("ingots/brass")
//            );
    public static ItemEntry<Item> DURALUMIN_INGOT =
            taggedIngredient("duralumin_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/duralumin"),
                    FHTags.Items.IGNITION_METAL.tag
            );
//    public static ItemEntry<Item> GOLD_INGOT =
//            taggedIngredient("gold_ingot",
//                    forgeItemTag("ingots"),
//                    forgeItemTag("ingots/gold")
//            );
    public static ItemEntry<Item> SILVER_INGOT =
            taggedIngredient("silver_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/silver")
            );
    public static ItemEntry<Item> NICKEL_INGOT =
            taggedIngredient("nickel_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/nickel")
            );
    public static ItemEntry<Item> LEAD_INGOT =
            taggedIngredient("lead_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/lead")
            );
    public static ItemEntry<Item> TITANIUM_INGOT =
            taggedIngredient("titanium_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/titanium")
            );
    public static ItemEntry<Item> BRONZE_INGOT =
            taggedIngredient("bronze_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/bronze")
            );
    public static ItemEntry<Item> INVAR_INGOT =
            taggedIngredient("invar_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/invar")
            );
    public static ItemEntry<Item> TUNGSTEN_STEEL_INGOT =
            taggedIngredient("tungsten_steel_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/tungsten_steel")
            );
//    public static ItemEntry<Item> ZINC_INGOT =
//            taggedIngredient("zinc_ingot",
//                    forgeItemTag("ingots"),
//                    forgeItemTag("ingots/zinc")
//            );
    public static ItemEntry<Item> TIN_INGOT =
            taggedIngredient("tin_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/tin")
            );
    public static ItemEntry<Item> MAGNESIUM_INGOT =
            taggedIngredient("magnesium_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/magnesium"),
                    FHTags.Items.IGNITION_METAL.tag
            );
    public static ItemEntry<Item> TUNGSTEN_INGOT =
            taggedIngredient("tungsten_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/tungsten")
            );

    // Nuggets
//    public static ItemEntry<Item> COPPER_NUGGET =
//            taggedIngredient("copper_nugget",
//                    forgeItemTag("nuggets"),
//                    forgeItemTag("nuggets/copper")
//            );
    public static ItemEntry<Item> ALUMINUM_NUGGET =
            taggedIngredient("aluminum_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/aluminum")
            );
    public static ItemEntry<Item> STEEL_NUGGET =
            taggedIngredient("steel_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/steel"),
                    FHTags.Items.IGNITION_METAL.tag
            );
    public static ItemEntry<Item> ELECTRUM_NUGGET =
            taggedIngredient("electrum_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/electrum")
            );
    public static ItemEntry<Item> CONSTANTAN_NUGGET =
            taggedIngredient("constantan_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/constantan")
            );
//    public static ItemEntry<Item> IRON_NUGGET =
//            taggedIngredient("iron_nugget",
//                    forgeItemTag("nuggets"),
//                    forgeItemTag("nuggets/iron")
//            );
    public static ItemEntry<Item> CAST_IRON_NUGGET =
            taggedIngredient("cast_iron_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/cast_iron"),
                    FHTags.Items.IGNITION_METAL.tag
            );
//    public static ItemEntry<Item> BRASS_NUGGET =
//            taggedIngredient("brass_nugget",
//                    forgeItemTag("nuggets"),
//                    forgeItemTag("nuggets/brass")
//            );
    public static ItemEntry<Item> DURALUMIN_NUGGET =
            taggedIngredient("duralumin_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/duralumin"),
                    FHTags.Items.IGNITION_METAL.tag
            );
//    public static ItemEntry<Item> GOLD_NUGGET =
//            taggedIngredient("gold_nugget",
//                    forgeItemTag("nuggets"),
//                    forgeItemTag("nuggets/gold")
//            );
    public static ItemEntry<Item> SILVER_NUGGET =
            taggedIngredient("silver_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/silver")
            );
    public static ItemEntry<Item> NICKEL_NUGGET =
            taggedIngredient("nickel_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/nickel")
            );
    public static ItemEntry<Item> LEAD_NUGGET =
            taggedIngredient("lead_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/lead")
            );
    public static ItemEntry<Item> TITANIUM_NUGGET =
            taggedIngredient("titanium_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/titanium")
            );
    public static ItemEntry<Item> BRONZE_NUGGET =
            taggedIngredient("bronze_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/bronze")
            );
    public static ItemEntry<Item> INVAR_NUGGET =
            taggedIngredient("invar_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/invar")
            );
    public static ItemEntry<Item> TUNGSTEN_STEEL_NUGGET =
            taggedIngredient("tungsten_steel_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/tungsten_steel")
            );
//    public static ItemEntry<Item> ZINC_NUGGET =
//            taggedIngredient("zinc_nugget",
//                    forgeItemTag("nuggets"),
//                    forgeItemTag("nuggets/zinc")
//            );
    public static ItemEntry<Item> TIN_NUGGET =
            taggedIngredient("tin_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/tin")
            );
    public static ItemEntry<Item> MAGNESIUM_NUGGET =
            taggedIngredient("magnesium_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/magnesium"),
                    FHTags.Items.IGNITION_METAL.tag
            );
    public static ItemEntry<Item> TUNGSTEN_NUGGET =
            taggedIngredient("tungsten_nugget",
                    forgeItemTag("nuggets"),
                    forgeItemTag("nuggets/tungsten")
            );

    // Sheets
//    public static ItemEntry<Item> COPPER_SHEET =
//            taggedIngredient("copper_sheet",
//                    forgeItemTag("sheets"),
//                    forgeItemTag("plates"),
//                    forgeItemTag("sheets/copper"),
//                    forgeItemTag("plates/copper")
//            );
    public static ItemEntry<Item> ALUMINUM_SHEET =
            taggedIngredient("aluminum_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/aluminum"),
                    forgeItemTag("plates/aluminum")
            );
    public static ItemEntry<Item> STEEL_SHEET =
            taggedIngredient("steel_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/steel"),
                    forgeItemTag("plates/steel")
            );
    public static ItemEntry<Item> ELECTRUM_SHEET =
            taggedIngredient("electrum_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/electrum"),
                    forgeItemTag("plates/electrum")
            );
    public static ItemEntry<Item> CONSTANTAN_SHEET =
            taggedIngredient("constantan_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/constantan"),
                    forgeItemTag("plates/constantan")
            );
//    public static ItemEntry<Item> IRON_SHEET =
//            taggedIngredient("iron_sheet",
//                    forgeItemTag("sheets"),
//                    forgeItemTag("plates"),
//                    forgeItemTag("sheets/iron"),
//                    forgeItemTag("plates/iron")
//            );
    public static ItemEntry<Item> CAST_IRON_SHEET =
            taggedIngredient("cast_iron_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/cast_iron"),
                    forgeItemTag("plates/cast_iron")
            );
//    public static ItemEntry<Item> BRASS_SHEET =
//            taggedIngredient("brass_sheet",
//                    forgeItemTag("sheets"),
//                    forgeItemTag("plates"),
//                    forgeItemTag("sheets/brass"),
//                    forgeItemTag("plates/brass")
//            );
    public static ItemEntry<Item> DURALUMIN_SHEET =
            taggedIngredient("duralumin_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/duralumin"),
                    forgeItemTag("plates/duralumin")
            );
//    public static ItemEntry<Item> GOLD_SHEET =
//            taggedIngredient("gold_sheet",
//                    forgeItemTag("sheets"),
//                    forgeItemTag("plates"),
//                    forgeItemTag("sheets/gold"),
//                    forgeItemTag("plates/gold")
//            );
    public static ItemEntry<Item> SILVER_SHEET =
            taggedIngredient("silver_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/silver"),
                    forgeItemTag("plates/silver")
            );
    public static ItemEntry<Item> NICKEL_SHEET =
            taggedIngredient("nickel_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/nickel"),
                    forgeItemTag("plates/nickel")
            );
    public static ItemEntry<Item> LEAD_SHEET =
            taggedIngredient("lead_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/lead"),
                    forgeItemTag("plates/lead")
            );
    public static ItemEntry<Item> TITANIUM_SHEET =
            taggedIngredient("titanium_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/titanium"),
                    forgeItemTag("plates/titanium")
            );
    public static ItemEntry<Item> BRONZE_SHEET =
            taggedIngredient("bronze_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/bronze"),
                    forgeItemTag("plates/bronze")
            );
    public static ItemEntry<Item> INVAR_SHEET =
            taggedIngredient("invar_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/invar"),
                    forgeItemTag("plates/invar")
            );
    public static ItemEntry<Item> TUNGSTEN_STEEL_SHEET =
            taggedIngredient("tungsten_steel_sheet",
                    forgeItemTag("sheets"),
                    forgeItemTag("plates"),
                    forgeItemTag("sheets/tungsten_steel"),
                    forgeItemTag("plates/tungsten_steel")
            );

    // Rods
    public static ItemEntry<Item> COPPER_ROD =
            taggedIngredient("copper_rod",
                    forgeItemTag("rods"),
                    forgeItemTag("rods/copper")
            );
    public static ItemEntry<Item> ALUMINUM_ROD =
            taggedIngredient("aluminum_rod",
                    forgeItemTag("rods"),
                    forgeItemTag("rods/aluminum")
            );
    public static ItemEntry<Item> STEEL_ROD =
            taggedIngredient("steel_rod",
                    forgeItemTag("rods"),
                    forgeItemTag("rods/steel"),
                    FHTags.Items.IGNITION_METAL.tag
            );
    public static ItemEntry<Item> ELECTRUM_ROD =
            taggedIngredient("electrum_rod",
                    forgeItemTag("rods"),
                    forgeItemTag("rods/electrum")
            );
    public static ItemEntry<Item> CONSTANTAN_ROD =
            taggedIngredient("constantan_rod",
                    forgeItemTag("rods"),
                    forgeItemTag("rods/constantan")
            );
    public static ItemEntry<Item> IRON_ROD =
            taggedIngredient("iron_rod",
                    forgeItemTag("rods"),
                    forgeItemTag("rods/iron"),
                    FHTags.Items.IGNITION_METAL.tag
            );
    public static ItemEntry<Item> CAST_IRON_ROD =
            taggedIngredient("cast_iron_rod",
                    forgeItemTag("rods"),
                    forgeItemTag("rods/cast_iron"),
                    FHTags.Items.IGNITION_METAL.tag
            );

    // Wires
    public static ItemEntry<Item> COPPER_WIRE =
            taggedIngredient("copper_wire",
                    forgeItemTag("wires"),
                    forgeItemTag("wires/copper")
            );
    public static ItemEntry<Item> ALUMINUM_WIRE =
            taggedIngredient("aluminum_wire",
                    forgeItemTag("wires"),
                    forgeItemTag("wires/aluminum")
            );
    public static ItemEntry<Item> STEEL_WIRE =
            taggedIngredient("steel_wire",
                    forgeItemTag("wires"),
                    forgeItemTag("wires/steel")
            );
    public static ItemEntry<Item> ELECTRUM_WIRE =
            taggedIngredient("electrum_wire",
                    forgeItemTag("wires"),
                    forgeItemTag("wires/electrum")
            );
    public static ItemEntry<Item> CONSTANTAN_WIRE =
            taggedIngredient("constantan_wire",
                    forgeItemTag("wires"),
                    forgeItemTag("wires/constantan")
            );

    // Rusted ingots
    public static ItemEntry<Item> RUSTED_IRON_INGOT = REGISTRATE
            .item("rusted_iron_ingot", Item::new)
            .tag(forgeItemTag("ingots"), forgeItemTag("ingots/rusted_iron"), FHTags.Items.IGNITION_METAL.tag)
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "item.frostedheart.rusted_metal"))
            .register();
    public static ItemEntry<Item> RUSTED_COPPER_INGOT = REGISTRATE
            .item("rusted_copper_ingot", Item::new)
            .tag(forgeItemTag("ingots"), forgeItemTag("ingots/rusted_copper"))
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "item.frostedheart.rusted_metal"))
            .register();
    public static ItemEntry<Item> GRAY_TIN_INGOT = REGISTRATE
            .item("gray_tin_ingot", Item::new)
            .tag(forgeItemTag("ingots"), forgeItemTag("ingots/gray_tin"))
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "item.frostedheart.rusted_metal"))
            .register();

    // Slags
    public static ItemEntry<Item> IRON_SLAG =
            taggedIngredient("iron_slag",
                    forgeItemTag("slags"),
                    forgeItemTag("slags/iron")
            );
    public static ItemEntry<Item> NICKEL_MATTE =
            taggedIngredient("nickel_matte",
                    forgeItemTag("slags"),
                    forgeItemTag("slags/nickel")
            );

    // Chemicals
    public static ItemEntry<Item> COPPER_OXIDE_DUST =
            taggedIngredient("copper_oxide_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/copper_oxide")
            );
    public static ItemEntry<Item> ZINC_OXIDE_DUST =
            taggedIngredient("zinc_oxide_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/zinc_oxide")
            );
    public static ItemEntry<Item> ALUMINA_DUST =
            taggedIngredient("alumina_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/alumina")
            );
    public static ItemEntry<Item> MAGNESIA_DUST =
            taggedIngredient("magnesia_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/magnesia")
            );
    public static ItemEntry<Item> LEAD_OXIDE_DUST =
            taggedIngredient("lead_oxide_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/lead_oxide")
            );
    public static ItemEntry<Item> ALUMINIUM_HYDROXIDE_DUST =
            taggedIngredient("aluminium_hydroxide_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/aluminium_hydroxide")
            );
    public static ItemEntry<Item> SODIUM_HYDROXIDE_DUST =
            taggedIngredient("sodium_hydroxide_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/sodium_hydroxide")
            );
    public static ItemEntry<Item> SODIUM_SULFIDE_DUST =
            taggedIngredient("sodium_sulfide_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/sodium_sulfide")
            );
    public static ItemEntry<Item> SODIUM_CHLORIDE_DUST =
            taggedIngredient("sodium_chloride_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/sodium_chloride")
            );
    public static ItemEntry<Item> CRYOLITE_DUST =
            taggedIngredient("cryolite_dust",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/cryolite")
            );

    // Misc
    public static ItemEntry<Item> MORTAR =
            ingredient("mortar");
    public static ItemEntry<Item> VULCANIZED_RUBBER =
            taggedIngredient("vulcanized_rubber",
                    forgeItemTag("rubber")
            );
    public static ItemEntry<Item> PULP =
            ingredient("pulp");
    public static ItemEntry<Item> FIRE_CLAY_BALL =
            ingredient("fire_clay_ball");
    public static ItemEntry<Item> HIGH_REFRACTORY_BRICK =
            ingredient("high_refractory_brick");
    public static ItemEntry<Item> SAWDUST =
            taggedIngredient("sawdust",
                    forgeItemTag("dusts/wooden")
            );
    public static ItemEntry<Item> BIOMASS =
            ingredient("biomass");
    public static ItemEntry<Item> SYNTHETIC_LEATHER =
            taggedIngredient("synthetic_leather",
                    forgeItemTag("leather")
            );
    public static ItemEntry<Item> QUICKLIME =
            taggedIngredient("quicklime",
                    forgeItemTag("dusts"),
                    forgeItemTag("dusts/quicklime")
            );
    public static ItemEntry<Item> SODIUM_INGOT =
            taggedIngredient("sodium_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/sodium")
            );
    public static ItemEntry<Item> REFRACTORY_BRICK =
            ingredient("refractory_brick");
    public static ItemEntry<Item> FLUX =
            ingredient("flux");

    public static ItemEntry<Item> generator_ash = REGISTRATE
            .item("generator_ash", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> raw_hide = REGISTRATE
            .item("raw_hide", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> makeshift_core_broken = REGISTRATE
            .item("makeshift_core_broken", Item::new)
            .model(AssetLookup.existingItemModel())
            .lang("Broken Makeshift Core")
            .register();
    public static ItemEntry<Item> handheld_core = REGISTRATE
            .item("handheld_core", Item::new)
            .model(AssetLookup.existingItemModel())
            .lang("Repaired Makeshift Core")
            .register();
    public static ItemEntry<Item> energy_core = REGISTRATE
            .item("energy_core", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();


    static {
        REGISTRATE.setCreativeTab(FHTabs.FOODS);
    }

    // Foods and plants
    public static ItemEntry<Item> dried_wolfberries = REGISTRATE
            .item("dried_wolfberries", Item::new)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.food(FHFoodProperties.DRIED_WOLFBERRIES))
            .register();
    public static ItemEntry<Item> rye = REGISTRATE
            .item("rye", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> INULIN = REGISTRATE
            .item("inulin", Item::new)
            .register();
    public static ItemEntry<Item> frozen_seeds = REGISTRATE
            .item("frozen_seeds", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> rye_flour = REGISTRATE
            .item("rye_flour", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> raw_rye_bread = REGISTRATE
            .item("raw_rye_bread", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> random_seeds = REGISTRATE
            .item("random_seeds", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> rye_bread = REGISTRATE
            .item("rye_bread", Item::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .properties(p -> p.food(FHFoodProperties.RYE_BREAD))
            .register();
    public static ItemEntry<Item> black_bread = REGISTRATE
            .item("black_bread", Item::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.food(FHFoodProperties.BLACK_BREAD))
            .register();
    public static ItemEntry<FHSoupItem> vegetable_sawdust_soup = REGISTRATE
            .item("vegetable_sawdust_soup", FHSoupItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.stacksTo(1).food(FHFoodProperties.VEGETABLE_SAWDUST_SOUP))
            .register();
    public static ItemEntry<FHSoupItem> rye_sawdust_porridge = REGISTRATE
            .item("rye_sawdust_porridge", FHSoupItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.stacksTo(1).food(FHFoodProperties.RYE_SAWDUST_PORRIDGE))
            .register();
    public static ItemEntry<FHSoupItem> rye_porridge = REGISTRATE
            .item("rye_porridge", FHSoupItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.stacksTo(1).food(FHFoodProperties.RYE_SAWDUST_PORRIDGE))
            .register();
    public static ItemEntry<FHSoupItem> vegetable_soup = REGISTRATE
            .item("vegetable_soup", FHSoupItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.stacksTo(1).food(FHFoodProperties.VEGETABLE_SAWDUST_SOUP))
            .register();
    public static ItemEntry<CannedFoodItem> military_rations = REGISTRATE
            .item("military_rations", CannedFoodItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .tag(FHTags.Items.INSULATED_FOOD.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.food(new FoodProperties.Builder().nutrition(6).saturationMod(0.6f).build()))
            .register();
    public static ItemEntry<CannedFoodItem> compressed_biscuits_pack = REGISTRATE
            .item("compressed_biscuits_pack", CannedFoodItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .tag(FHTags.Items.DRY_FOOD.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.food(Foods.BREAD))
            .register();
    public static ItemEntry<CannedFoodItem> compressed_biscuits = REGISTRATE
            .item("compressed_biscuits", CannedFoodItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .tag(FHTags.Items.DRY_FOOD.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.food(Foods.BREAD))
            .register();
    public static ItemEntry<CannedFoodItem> packed_nuts = REGISTRATE
            .item("packed_nuts", CannedFoodItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .tag(FHTags.Items.DRY_FOOD.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.food((new FoodProperties.Builder()).nutrition(2).saturationMod(0.8F).build()))
            .register();
    public static ItemEntry<CannedFoodItem> dried_vegetables = REGISTRATE
            .item("dried_vegetables", CannedFoodItem::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .tag(FHTags.Items.DRY_FOOD.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.6F).build()))
            .register();
    public static ItemEntry<Item> chocolate = REGISTRATE
            .item("chocolate", Item::new)
            .tag(FHTags.Items.REFUGEE_NEEDS.tag)
            .tag(FHTags.Items.DRY_FOOD.tag)
            .model(AssetLookup.existingItemModel())
            .properties(p -> p.food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.8F).meat().fast().build()))
            .register();
    public static ItemEntry<Item> RAW_WHALE_MEAT = REGISTRATE
            .item("raw_whale_meat", Item::new)
            .tag(CPTags.Items.MEAT)
            .tag(CPTags.Items.MEATS)
            .properties(p -> p.food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.8F).meat().fast().build()))
            .register();
    public static ItemEntry<Item> COOKED_WHALE_MEAT = REGISTRATE
            .item("cooked_whale_meat", Item::new)
            .properties(p -> p.food((new FoodProperties.Builder()).nutrition(8).saturationMod(1.6F).meat().fast().build()))
            .register();

    static {
        REGISTRATE.setCreativeTab(FHTabs.TOOLS);
    }

    // Equipment and tools
    public static ItemEntry<CoalHandStove> hand_stove = REGISTRATE.item("hand_stove", CoalHandStove::new)
            .properties(p -> p.defaultDurability(10))
            .model(AssetLookup.existingItemModel())
            .lang("Copper Hand Stove")
            .register();
    public static ItemEntry<DebugItem> debug_item = REGISTRATE
            .item("debug_item", DebugItem::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> coal_stick = REGISTRATE
            .item("coal_stick", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> charcoal_stick = REGISTRATE
            .item("charcoal_stick", Item::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<ThermometerItem> mercury_body_thermometer = REGISTRATE
            .item("mercury_body_thermometer", ThermometerItem::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<SteamBottleItem> steam_bottle = REGISTRATE
            .item("steam_bottle", SteamBottleItem::new)
            .properties(p -> p.stacksTo(1))
            .model(AssetLookup.existingItemModel())
            .register();


    public static ItemEntry<FHBaseItem> buff_coat = REGISTRATE
            .item("buff_coat", p -> new FHBaseItem(createProps().defaultDurability(384)).setRepairItem(raw_hide.get()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseItem> gambeson = REGISTRATE
            .item("gambeson", p -> new FHBaseItem(createProps().defaultDurability(384)).setRepairItem(Items.WHITE_WOOL))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseItem> kelp_lining = REGISTRATE
            .item("kelp_lining", p -> new FHBaseItem(createProps().defaultDurability(256)).setRepairItem(Items.KELP))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> straw_lining = REGISTRATE
            .item("straw_lining", Item::new)
            .properties(p -> p.defaultDurability(256))
            .model(AssetLookup.existingItemModel())
            .register();

    
    public static ItemEntry<FHBaseArmorItem> hay_boots = REGISTRATE
            .item("hay_boots", p -> new FHBaseArmorItem(FHArmorMaterial.HAY, Type.BOOTS, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> hay_hat = REGISTRATE
            .item("hay_hat", p -> new FHBaseArmorItem(FHArmorMaterial.HAY, Type.HELMET, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> hay_jacket = REGISTRATE
            .item("hay_jacket", p -> new FHBaseArmorItem(FHArmorMaterial.HAY, Type.CHESTPLATE, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> hay_pants = REGISTRATE
            .item("hay_pants", p -> new FHBaseArmorItem(FHArmorMaterial.HAY, Type.LEGGINGS, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> wool_boots = REGISTRATE
            .item("wool_boots", p -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.BOOTS, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> wool_hat = REGISTRATE
            .item("wool_hat", p -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.HELMET, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> wool_jacket = REGISTRATE
            .item("wool_jacket", p -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.CHESTPLATE, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> wool_pants = REGISTRATE
            .item("wool_pants", p -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.LEGGINGS, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> hide_boots = REGISTRATE
            .item("hide_boots", p -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.BOOTS, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> hide_hat = REGISTRATE
            .item("hide_hat", p -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.HELMET, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> hide_jacket = REGISTRATE
            .item("hide_jacket", p -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.CHESTPLATE, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> hide_pants = REGISTRATE
            .item("hide_pants", p -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.LEGGINGS, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<FHBaseArmorItem> space_boots = REGISTRATE
            .item("spacesuit_boots", p -> new FHBaseArmorItem(FHArmorMaterial.SPACESUIT, Type.BOOTS, createProps()))
            .register();
    public static ItemEntry<FHBaseArmorItem> space_hat = REGISTRATE
            .item("spacesuit_helmet", p -> new FHBaseArmorItem(FHArmorMaterial.SPACESUIT, Type.HELMET, createProps()))
            .register();
    public static ItemEntry<FHBaseArmorItem> space_jacket = REGISTRATE
            .item("spacesuit_body", p -> new FHBaseArmorItem(FHArmorMaterial.SPACESUIT, Type.CHESTPLATE, createProps()))
            .register();
    public static ItemEntry<FHBaseArmorItem> space_pants = REGISTRATE
            .item("spacesuit_leg", p -> new FHBaseArmorItem(FHArmorMaterial.SPACESUIT, Type.LEGGINGS, createProps()))
            .register();
    public static ItemEntry<HeaterVestItem> heater_vest = REGISTRATE
            .item("heater_vest", HeaterVestItem::new)
            .properties(p -> p.stacksTo(1).setNoRepair())
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<ProspectorPick> copper_pro_pick = REGISTRATE
            .item("copper_pro_pick", p -> new ProspectorPick(1, createProps().defaultDurability(128)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<ProspectorPick> iron_pro_pick = REGISTRATE
            .item("iron_pro_pick", p -> new ProspectorPick(2, createProps().defaultDurability(192)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<ProspectorPick> steel_pro_pick = REGISTRATE
            .item("steel_pro_pick", p -> new ProspectorPick(3, createProps().defaultDurability(256)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<CoreSpade> copper_core_spade = REGISTRATE
            .item("copper_core_spade", p -> new CoreSpade(1, createProps().defaultDurability(96)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<CoreSpade> iron_core_spade = REGISTRATE
            .item("iron_core_spade", p -> new CoreSpade(2, createProps().defaultDurability(128)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<CoreSpade> steel_core_spade = REGISTRATE
            .item("steel_core_spade", p -> new CoreSpade(3, createProps().defaultDurability(160)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<GeologistsHammer> copper_geologists_hammer = REGISTRATE
            .item("copper_geologists_hammer", p -> new GeologistsHammer(1, createProps().defaultDurability(96)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<GeologistsHammer> iron_geologists_hammer = REGISTRATE
            .item("iron_geologists_hammer", p -> new GeologistsHammer(2, createProps().defaultDurability(128)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<GeologistsHammer> steel_geologists_hammer = REGISTRATE
            .item("steel_geologists_hammer", p -> new GeologistsHammer(3, createProps().defaultDurability(160)))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<SoilThermometer> soil_thermometer = REGISTRATE
            .item("soil_thermometer", SoilThermometer::new)
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<HeatDebugItem> heat_debugger = REGISTRATE
            .item("heat_debugger", p -> new HeatDebugItem())
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<MushroomBed> red_mushroombed = REGISTRATE
            .item("straw_briquette_red_mushroom", p -> new MushroomBed(Items.RED_MUSHROOM, createProps().defaultDurability(4800)))
            .model(AssetLookup.existingItemModel())
            .lang("Red Fungus Bed")
            .register();
    public static ItemEntry<MushroomBed> brown_mushroombed = REGISTRATE
            .item("straw_briquette_brown_mushroom", p -> new MushroomBed(Items.BROWN_MUSHROOM, createProps().defaultDurability(4800)))
            .model(AssetLookup.existingItemModel())
            .lang("Brown Fungus Bed")
            .register();
    public static ItemEntry<CeramicBucket> ceramic_bucket = REGISTRATE
            .item("ceramic_bucket", CeramicBucket::new)
            .properties(p -> p.stacksTo(1))
            .model(AssetLookup.existingItemModel())
            .register();

    public static ItemEntry<FHBaseArmorItem> weatherHelmet = REGISTRATE
            .item("weather_helmet", p -> new FHBaseArmorItem(FHArmorMaterial.WEATHER, Type.HELMET, createProps()))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> weatherRadar = REGISTRATE
            .item("weather_radar", Item::new)
            .properties(p -> p.stacksTo(1))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<Item> temperatureProbe = REGISTRATE
            .item("temperature_probe", Item::new)
            .properties(p -> p.stacksTo(1))
            .model(AssetLookup.existingItemModel())
            .register();

    // Thermos
    public static ItemEntry<ThermosItem> thermos = REGISTRATE
        .item("thermos", p -> new ThermosItem(1500, true))
        .model(AssetLookup.existingItemModel())
        .tag(FHTags.Items.INSULATED_FOOD.tag)
        .tag(FHTags.Items.THERMOS.tag)
        .lang("Thermos")
        .register();
    public static ItemEntry<ThermosItem> advanced_thermos = REGISTRATE.item("advanced_thermos", p -> new ThermosItem(3000, true))
            .model(AssetLookup.existingItemModel())
            .tag(FHTags.Items.INSULATED_FOOD.tag)
            .tag(FHTags.Items.THERMOS.tag)
            .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "item.frostedheart.thermos"))
            .lang("Advanced Thermos")
            .register();

    // colour.getName() is all form xxx_xxx
    // make it like Xxx[SPACE]Xxx[SPACE]
    public static String fromIdToDisplay(String color) {
        String[] split = color.split("_");
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            sb.append(s.substring(0, 1).toUpperCase()).append(s.substring(1)).append(" ");
        }
        return sb.toString();
    }

    public static final DyedItemList<ThermosItem> allthermos = new DyedItemList<>(color -> {
        return REGISTRATE.item(color + "_thermos", p -> new ThermosItem(1500, false))
                .model((ctx, prov) -> prov.generated(ctx, FHMain.rl("item/" + "flask_i/insulated_flask_i_pouch_" + color)))
                .tag(FHTags.Items.INSULATED_FOOD.tag)
                .tag(FHTags.Items.COLORED_THERMOS.tag)
                .tag(FHTags.Items.THERMOS.tag)
                .lang(fromIdToDisplay(color.getName()) + "Thermos")
                .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "item.frostedheart.thermos"))
                .register();
    });

    public static final DyedItemList<ThermosItem> alladvthermos = new DyedItemList<>(color -> {
        return REGISTRATE.item(color + "_advanced_thermos", p -> new ThermosItem(3000, false))
                .model((ctx, prov) -> prov.generated(ctx, FHMain.rl("item/" + "flask_ii/insulated_flask_ii_pouch_" + color)))
                .tag(FHTags.Items.INSULATED_FOOD.tag)
                .tag(FHTags.Items.COLORED_ADVANCED_THERMOS.tag)
                .tag(FHTags.Items.THERMOS.tag)
                .lang(fromIdToDisplay(color.getName()) + "Advanced Thermos")
                .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "item.frostedheart.thermos"))
                .register();
    });

    // Equipment
    public static final ItemEntry<PowderedSnowWalkable> SNOWSHOES =
            REGISTRATE.item("snowshoes", p -> new PowderedSnowWalkable(ArmorMaterials.LEATHER, Type.BOOTS, new Item.Properties().stacksTo(1)))
                    .tag(FHTags.Items.POWDERED_SNOW_WALKABLE.tag)
                    .register();
    public static final ItemEntry<FHBaseArmorItem> ICE_SKATES =
            REGISTRATE.item("ice_skates", p -> new FHBaseArmorItem(ArmorMaterials.LEATHER, Type.BOOTS, new Item.Properties().stacksTo(1)))
                    .register();

    // Clothes
  /*  public static final ItemEntry<FHBaseClothesItem> DEBUG_CLOTH = REGISTRATE
            .item("debug_cloth", n -> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 0.5f, 100.0f, PlayerTemperatureData.BodyPart.TORSO))
            .register();

    public static final ItemEntry<FHBaseClothesItem> REMOVE_ALL = REGISTRATE
            .item("debug_remove_all_cloth", n-> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 0.0f, 0.0f, PlayerTemperatureData.BodyPart.REMOVEALL))
            .register();*/

    /*public static final Map<String, ItemEntry<FHBaseClothesItem>> FH_CLOTHES = new HashMap<>();
    static {
        // Define item properties for each type of clothing material
        // {windproof, insulation}
        Map<String, Float[]> materials = Map.of(
                "hay", new Float[]{0.2f, 200.0f},
                "hide", new Float[]{1.0f, 300.0f},
                "cotton", new Float[]{0.3f, 400.0f},
                "wool", new Float[]{0.5f, 500.0f},
                "down", new Float[]{0.7f, 600.0f}
        );

        // Define body parts
        Map<String, PlayerTemperatureData.BodyPart> bodyParts = Map.of(
                "jacket", PlayerTemperatureData.BodyPart.TORSO,
                "pants", PlayerTemperatureData.BodyPart.LEGS,
                "boots", PlayerTemperatureData.BodyPart.FEET,
                "hat", PlayerTemperatureData.BodyPart.HEAD,
                "glove", PlayerTemperatureData.BodyPart.HANDS
        );

        // Loop through materials and body parts to register items
        materials.forEach((materialName, properties) -> {
            bodyParts.forEach((bodyPartName, bodyPart) -> {
                String itemName = materialName + "_" + bodyPartName;
                FH_CLOTHES.put(itemName, REGISTRATE
                        .item(itemName, n -> new FHBaseClothesItem(
                                new Item.Properties().stacksTo(1),
                                properties[0],
                                properties[1],
                                bodyPart
                        ))
                        .model(AssetLookup.existingItemModel())
                        .register());
            });
        });
    }*/

    // Tools
    public static final ItemEntry<KnifeItem> MAKESHIFT_KNIFE =
            REGISTRATE.item("makeshift_knife", p -> new KnifeItem(FHToolMaterials.FLINT, 1, -1.5F, new Item.Properties()))
                    .tag(ItemTags.SWORDS)
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<PickaxeItem> MAKESHIFT_PICKAXE =
            REGISTRATE.item("makeshift_pickaxe", p -> new PickaxeItem(FHToolMaterials.FLINT, 1, -2.8F, new Item.Properties()))
                    .tag(ItemTags.PICKAXES)
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<AxeItem> MAKESHIFT_AXE =
            REGISTRATE.item("makeshift_axe", p -> new AxeItem(FHToolMaterials.FLINT, 4.0F, -3.2F, new Item.Properties()))
                    .tag(ItemTags.AXES)
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<ShovelItem> MAKESHIFT_SHOVEL =
            REGISTRATE.item("makeshift_shovel", p -> new ShovelItem(FHToolMaterials.FLINT, 1.5F, -3.0F, new Item.Properties()))
                    .tag(ItemTags.SHOVELS)
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<HoeItem> MAKESHIFT_HOE =
            REGISTRATE.item("makeshift_hoe", p -> new HoeItem(FHToolMaterials.FLINT, 0, -3.0F, new Item.Properties()))
                    .tag(ItemTags.HOES)
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<KnifeItem> BRONZE_KNIFE =
            REGISTRATE.item("bronze_knife", p -> new KnifeItem(FHToolMaterials.BRONZE, 1, -1.5F, new Item.Properties()))
                    .tag(ItemTags.SWORDS, forgeItemTag("knifes"), forgeItemTag("knifes/bronze"))
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<PickaxeItem> BRONZE_PICKAXE =
            REGISTRATE.item("bronze_pickaxe", p -> new PickaxeItem(FHToolMaterials.BRONZE, 1, -2.8F, new Item.Properties()))
                    .tag(ItemTags.PICKAXES, forgeItemTag("pickaxes"), forgeItemTag("pickaxes/bronze"))
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<AxeItem> BRONZE_AXE =
            REGISTRATE.item("bronze_axe", p -> new AxeItem(FHToolMaterials.BRONZE, 4.0F, -3.2F, new Item.Properties()))
                    .tag(ItemTags.AXES, forgeItemTag("axes"), forgeItemTag("axes/bronze"))
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<ShovelItem> BRONZE_SHOVEL =
            REGISTRATE.item("bronze_shovel", p -> new ShovelItem(FHToolMaterials.BRONZE, 1.5F, -3.0F, new Item.Properties()))
                    .tag(ItemTags.SHOVELS, forgeItemTag("shovels"), forgeItemTag("shovels/bronze"))
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<HoeItem> BRONZE_HOE =
            REGISTRATE.item("bronze_hoe", p -> new HoeItem(FHToolMaterials.BRONZE, 0, -3.0F, new Item.Properties()))
                    .tag(ItemTags.HOES, forgeItemTag("hoes"), forgeItemTag("hoes/bronze"))
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<SwordItem> BRONZE_SWORD =
            REGISTRATE.item("bronze_sword", p -> new SwordItem(FHToolMaterials.BRONZE, 3, -2.4F, new Item.Properties()))
                    .tag(ItemTags.SWORDS, forgeItemTag("swords"), forgeItemTag("swords/bronze"))
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<PickaxeItem> STONE_HAMMER =
            REGISTRATE.item("stone_hammer", p -> new PickaxeItem(FHToolMaterials.FLINT, 1, -2.8F, new Item.Properties()))
                    .tag(ItemTags.PICKAXES, forgeItemTag("hammers"), forgeItemTag("hammers/stone"))
                    .model(FHAssetsUtils.handheld())
                    .register();
    public static final ItemEntry<PickaxeItem> BRONZE_HAMMER =
            REGISTRATE.item("bronze_hammer", p -> new PickaxeItem(FHToolMaterials.BRONZE, 1, -2.8F, new Item.Properties()))
                    .tag(ItemTags.PICKAXES, forgeItemTag("hammers"), forgeItemTag("hammers/bronze"))
                    .model(FHAssetsUtils.handheld())
                    .register();

    public static ItemEntry<FluidBottleItem> fluid_bottle = REGISTRATE
            .item("fluid_bottle", FluidBottleItem::new)
            .properties(p -> p.stacksTo(16))
            .model(AssetLookup.existingItemModel())
            .register();
    public static ItemEntry<WoodenCupItem> wooden_cup = REGISTRATE
            .item("wooden_cup", p -> new WoodenCupItem(new Item.Properties(), 250)
            )
            .model(AssetLookup.existingItemModel())
            .lang("Wooden Cup")
            .register();

    public static ItemEntry<WoodenCupItem> wooden_cup_drink = REGISTRATE
            .item("wooden_cup_drink", p -> new WoodenCupItem(new Item.Properties().stacksTo(1), 250))
            .model(AssetLookup.existingItemModel())
            .lang("Wooden Cup With Drink")
            .removeTab(FHTabs.TOOLS.getKey())
            
            .register();
    public static ItemEntry<LeatherWaterBagItem> LEATHER_WATER_BAG = REGISTRATE
            .item("leather_water_bag", p -> new LeatherWaterBagItem(new Item.Properties().stacksTo(1).setNoRepair(), 1000))
            .model(AssetLookup.existingItemModel())
            .lang("Leather Water Bag")
            .register();
    public static ItemEntry<IronBottleItem> IRON_BOTTLE = REGISTRATE
            .item("iron_bottle", p -> new IronBottleItem(new Item.Properties().stacksTo(1).setNoRepair(), 1000))
            .model(AssetLookup.existingItemModel())
            .lang("Iron Bottle")
            .register();

}