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

import com.simibubi.create.AllTags;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.teammoeg.frostedheart.base.item.*;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.foods.CannedFoodItem;
import com.teammoeg.frostedheart.content.foods.FHSoupItem;
import com.teammoeg.frostedheart.content.ore.FHSnowballItem;
import com.teammoeg.frostedheart.content.research.blocks.FHBasePen;
import com.teammoeg.frostedheart.content.research.blocks.FHReusablePen;
import com.teammoeg.frostedheart.content.research.blocks.RubbingTool;
import com.teammoeg.frostedheart.content.steamenergy.debug.HeatDebugItem;
import com.teammoeg.frostedheart.content.utility.*;
import com.teammoeg.frostedheart.content.utility.handstoves.CoalHandStove;
import com.teammoeg.frostedheart.content.utility.heatervest.HeaterVestItem;
import com.teammoeg.frostedheart.content.utility.oredetect.CoreSpade;
import com.teammoeg.frostedheart.content.utility.oredetect.GeologistsHammer;
import com.teammoeg.frostedheart.content.utility.oredetect.ProspectorPick;
import com.teammoeg.frostedheart.content.water.item.FluidBottleItem;
import com.teammoeg.frostedheart.content.water.item.IronBottleItem;
import com.teammoeg.frostedheart.content.water.item.LeatherWaterBagItem;
import com.teammoeg.frostedheart.content.water.item.WoodenCupItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.*;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.teammoeg.frostedheart.FHMain.REGISTRATE;
import static com.teammoeg.frostedheart.FHTags.forgeItemTag;

/**
 * All items.
 */
public class FHItems {

    static {
        REGISTRATE.setCreativeTab(FHTabs.BASE_TAB);
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FHMain.MODID);

    // helper method: use FHBaseItem as the item class
    public static RegistryObject<Item> register(String name) {
        return register(name, n -> new FHBaseItem(createProps()));
    }

    static <T extends Item> RegistryObject<T> register(String name, Function<String, T> supplier) {
        return ITEMS.register(name, () -> {
            //item.setRegistryName(FHMain.MODID, name);
            return supplier.apply(name);
        });
    }

    static Properties createProps() {
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

    // Equipment
    public static final ItemEntry<FHBaseArmorItem> SNOWSHOES =
            REGISTRATE.item("snowshoes", p -> new FHBaseArmorItem(ArmorMaterials.LEATHER, Type.BOOTS, new Item.Properties().stacksTo(1)))
                    .register();
    public static final ItemEntry<FHBaseArmorItem> ICE_SKATES =
            REGISTRATE.item("ice_skates", p -> new FHBaseArmorItem(ArmorMaterials.LEATHER, Type.BOOTS, new Item.Properties().stacksTo(1)))
                    .register();

    // Clothes
    public static final ItemEntry<FHBaseClothesItem> DEBUG_CLOTH = REGISTRATE.item("debug_cloth", n -> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 0.5f, 100.0f, PlayerTemperatureData.BodyPart.BODY)).register();
    public static final ItemEntry<FHBaseClothesItem> HAY_CLOTH= REGISTRATE.item("hay_cloth", n -> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 0.2f, 200.0f, PlayerTemperatureData.BodyPart.BODY)).register();
    public static final ItemEntry<FHBaseClothesItem> HIDE_CLOTH = REGISTRATE.item("hide_cloth", n -> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 1.0f, 300.0f, PlayerTemperatureData.BodyPart.BODY)).register();
    public static final ItemEntry<FHBaseClothesItem> COTTON_CLOTH = REGISTRATE.item("cotton_cloth", n -> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 0.3f, 400.0f, PlayerTemperatureData.BodyPart.BODY)).register();
    public static final ItemEntry<FHBaseClothesItem> WOOL_CLOTH = REGISTRATE.item("wool_cloth", n -> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 0.5f, 500.0f, PlayerTemperatureData.BodyPart.BODY)).register();
    public static final ItemEntry<FHBaseClothesItem> DOWN_CLOTH = REGISTRATE.item("down_cloth", n -> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 0.7f, 600.0f, PlayerTemperatureData.BodyPart.BODY)).register();
    public static final ItemEntry<FHBaseClothesItem> REMOVE_ALL = REGISTRATE.item("debug_remove_all_cloth", n-> new FHBaseClothesItem(new Item.Properties().stacksTo(1), 0.0f, 0.0f, PlayerTemperatureData.BodyPart.REMOVEALL)).register();


    // Tools
    public static final ItemEntry<KnifeItem> MAKESHIFT_KNIFE =
            REGISTRATE.item("makeshift_knife", p -> new KnifeItem(FHToolMaterials.FLINT, 1, -1.5F, new Item.Properties()))
                    .tag(ItemTags.SWORDS)
                    .register();
    public static final ItemEntry<PickaxeItem> MAKESHIFT_PICKAXE =
            REGISTRATE.item("makeshift_pickaxe", p -> new PickaxeItem(FHToolMaterials.FLINT, 1, -2.8F, new Item.Properties()))
                    .tag(ItemTags.PICKAXES)
                    .register();
    public static final ItemEntry<AxeItem> MAKESHIFT_AXE =
            REGISTRATE.item("makeshift_axe", p -> new AxeItem(FHToolMaterials.FLINT, 4.0F, -3.2F, new Item.Properties()))
                    .tag(ItemTags.AXES)
                    .register();
    public static final ItemEntry<ShovelItem> MAKESHIFT_SHOVEL =
            REGISTRATE.item("makeshift_shovel", p -> new ShovelItem(FHToolMaterials.FLINT, 1.5F, -3.0F, new Item.Properties()))
                    .tag(ItemTags.SHOVELS)
                    .register();
    public static final ItemEntry<HoeItem> MAKESHIFT_HOE =
            REGISTRATE.item("makeshift_hoe", p -> new HoeItem(FHToolMaterials.FLINT, 0, -3.0F, new Item.Properties()))
                    .tag(ItemTags.HOES)
                    .register();
    public static final ItemEntry<SpearItem> MAKESHIFT_SPEAR =
            REGISTRATE.item("makeshift_spear", p -> new SpearItem(FHToolMaterials.FLINT, 2, -2.9F, new ResourceLocation("frostedheart:textures/item/entity/makeshift_spear.png"),new Item.Properties()))
                    .tag(forgeItemTag("spears"), forgeItemTag("spears/flint"))
                    .register();
    public static final ItemEntry<KnifeItem> BRONZE_KNIFE =
            REGISTRATE.item("bronze_knife", p -> new KnifeItem(FHToolMaterials.ALLOY, 1, -1.5F, new Item.Properties()))
                    .tag(ItemTags.SWORDS, forgeItemTag("knifes"), forgeItemTag("knifes/bronze"))
                    .register();
    public static final ItemEntry<PickaxeItem> BRONZE_PICKAXE =
            REGISTRATE.item("bronze_pickaxe", p -> new PickaxeItem(FHToolMaterials.ALLOY, 1, -2.8F, new Item.Properties()))
                    .tag(ItemTags.PICKAXES, forgeItemTag("pickaxes"), forgeItemTag("pickaxes/bronze"))
                    .register();
    public static final ItemEntry<AxeItem> BRONZE_AXE =
            REGISTRATE.item("bronze_axe", p -> new AxeItem(FHToolMaterials.ALLOY, 4.0F, -3.2F, new Item.Properties()))
                    .tag(ItemTags.AXES, forgeItemTag("axes"), forgeItemTag("axes/bronze"))
                    .register();
    public static final ItemEntry<ShovelItem> BRONZE_SHOVEL =
            REGISTRATE.item("bronze_shovel", p -> new ShovelItem(FHToolMaterials.ALLOY, 1.5F, -3.0F, new Item.Properties()))
                    .tag(ItemTags.SHOVELS, forgeItemTag("shovels"), forgeItemTag("shovels/bronze"))
                    .register();
    public static final ItemEntry<HoeItem> BRONZE_HOE =
            REGISTRATE.item("bronze_hoe", p -> new HoeItem(FHToolMaterials.ALLOY, 0, -3.0F, new Item.Properties()))
                    .tag(ItemTags.HOES, forgeItemTag("hoes"), forgeItemTag("hoes/bronze"))
                    .register();
    public static final ItemEntry<SpearItem> BRONZE_SPEAR =
            REGISTRATE.item("bronze_spear", p -> new SpearItem(FHToolMaterials.ALLOY, 2, -2.9F, new ResourceLocation("frostedheart:textures/item/entity/bronze_spear.png"), new Item.Properties()))
                    .tag(forgeItemTag("spears"), forgeItemTag("spears/bronze"))
                    .register();
    public static final ItemEntry<SwordItem> BRONZE_SWORD =
            REGISTRATE.item("bronze_sword", p -> new SwordItem(FHToolMaterials.ALLOY, 3, -2.4F, new Item.Properties()))
                    .tag(ItemTags.SWORDS, forgeItemTag("swords"), forgeItemTag("swords/bronze"))
                    .register();
    public static final ItemEntry<PickaxeItem> STONE_HAMMER =
            REGISTRATE.item("stone_hammer", p -> new PickaxeItem(FHToolMaterials.FLINT, 1, -2.8F, new Item.Properties()))
                    .tag(ItemTags.PICKAXES, forgeItemTag("hammers"), forgeItemTag("hammers/stone"))
                    .register();
    public static final ItemEntry<PickaxeItem> BRONZE_HAMMER =
            REGISTRATE.item("bronze_hammer", p -> new PickaxeItem(FHToolMaterials.ALLOY, 1, -2.8F, new Item.Properties()))
                    .tag(ItemTags.PICKAXES, forgeItemTag("hammers"), forgeItemTag("hammers/bronze"))
                    .register();

    static {
        REGISTRATE.setCreativeTab(FHTabs.MATERIALS_TAB);
    }

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
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_COPPER_ORE =
            REGISTRATE.item("condensed_ball_copper_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_GOLD_ORE =
            REGISTRATE.item("condensed_ball_gold_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_ZINC_ORE =
            REGISTRATE.item("condensed_ball_zinc_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_SILVER_ORE =
            REGISTRATE.item("condensed_ball_silver_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_TIN_ORE =
            REGISTRATE.item("condensed_ball_tin_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_PYRITE_ORE =
            REGISTRATE.item("condensed_ball_pyrite_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_NICKEL_ORE =
            REGISTRATE.item("condensed_ball_nickel_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
                    .register();
    public static ItemEntry<FHSnowballItem> CONDENSED_BALL_LEAD_ORE =
            REGISTRATE.item("condensed_ball_lead_ore", FHSnowballItem::new)
                    .tag(FHTags.Items.CONDENSED_BALLS.tag)
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
    public static ItemEntry<Item> CRUSHED_RAW_SILVER =
            taggedIngredient("crushed_raw_silver",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/silver"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);
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
    public static ItemEntry<Item> CRUSHED_RAW_NICKEL =
            taggedIngredient("crushed_raw_nickel",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/nickel"),
                    AllTags.AllItemTags.CRUSHED_RAW_MATERIALS.tag);
    public static ItemEntry<Item> CRUSHED_RAW_LEAD =
            taggedIngredient("crushed_raw_lead",
                    forgeItemTag("crushed_raw_materials"),
                    forgeItemTag("crushed_raw_materials/lead"),
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
    public static ItemEntry<Item> RAW_SILVER =
                taggedIngredient("raw_silver",
                        forgeItemTag("raw_materials/silver"),
                        forgeItemTag("raw_materials"));
    public static ItemEntry<Item> RAW_TIN =
            taggedIngredient("raw_tin",
                    forgeItemTag("raw_materials/tin"),
                    forgeItemTag("raw_materials"));
    public static ItemEntry<Item> RAW_PYRITE =
            taggedIngredient("raw_pyrite",
                    forgeItemTag("raw_materials/pyrite"),
                    forgeItemTag("raw_materials"),
                    FHTags.Items.IGNITION_METAL.tag);
    public static ItemEntry<Item> RAW_NICKEL =
            taggedIngredient("raw_nickel",
                    forgeItemTag("raw_materials/nickel"),
                    forgeItemTag("raw_materials"));
    public static ItemEntry<Item> RAW_LEAD =
            taggedIngredient("raw_lead",
                    forgeItemTag("raw_materials/lead"),
                    forgeItemTag("raw_materials"));
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
    public static ItemEntry<Item> RUSTED_IRON_INGOT =
            taggedIngredient("rusted_iron_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/rusted_iron"),
                    FHTags.Items.IGNITION_METAL.tag
            );
    public static ItemEntry<Item> RUSTED_COPPER_INGOT =
            taggedIngredient("rusted_copper_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/rusted_copper")
            );
    public static ItemEntry<Item> GRAY_TIN_INGOT =
            taggedIngredient("gray_tin_ingot",
                    forgeItemTag("ingots"),
                    forgeItemTag("ingots/gray_tin")
            );

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

    static {
        REGISTRATE.setCreativeTab(FHTabs.BASE_TAB);
    }

    // OLD FORGE LIKE REGISTRY - TRY NOT USE THIS, USE REGISTRATE like above

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
    public static RegistryObject<Item> wool_pants = register("wool_pants", n -> new FHBaseArmorItem(FHArmorMaterial.WOOL, Type.LEGGINGS, createProps()));
    public static RegistryObject<Item> hide_boots = register("hide_boots", n -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.BOOTS, createProps()));
    public static RegistryObject<Item> hide_hat = register("hide_hat", n -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.HELMET, createProps()));
    public static RegistryObject<Item> hide_jacket = register("hide_jacket", n -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.CHESTPLATE, createProps()));
    public static RegistryObject<Item> hide_pants = register("hide_pants", n -> new FHBaseArmorItem(FHArmorMaterial.HIDE, Type.LEGGINGS, createProps()));
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

    //WaterSource section
    public final static RegistryObject<Item> fluid_bottle = register("fluid_bottle", (s) -> new FluidBottleItem(new Item.Properties().stacksTo(16)));
    public final static RegistryObject<Item> wooden_cup = register("wooden_cup", (s) -> new WoodenCupItem(new Item.Properties(), 250) {@Override public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundTag nbt) {return super.initCapabilities(new ItemStack(wooden_cup_drink.get()), nbt);}});
    public final static RegistryObject<Item> wooden_cup_drink = register("wooden_cup_drink", (s) -> new WoodenCupItem(new Item.Properties().stacksTo(1), 250));
    public final static RegistryObject<Item> LEATHER_WATER_BAG = register("leather_water_bag", (s) -> new LeatherWaterBagItem(new Item.Properties().stacksTo(1).setNoRepair(), 1500));
    public final static RegistryObject<Item> IRON_BOTTLE = register("iron_bottle", (s) -> new IronBottleItem(new Item.Properties().stacksTo(1).setNoRepair(), 1500));
}