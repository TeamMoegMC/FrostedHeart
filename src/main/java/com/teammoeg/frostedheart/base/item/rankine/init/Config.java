package com.teammoeg.frostedheart.base.item.rankine.init;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mod.EventBusSubscriber
public class Config {

    static Predicate<Object> ELEMENT_VALIDATOR = o -> o instanceof String;
    public static Predicate<Object> DoubleValidator = o -> o instanceof Double;

    public static class Tools {
        public final ForgeConfigSpec.BooleanValue DISABLE_WOODEN_HAMMER;
        public final ForgeConfigSpec.BooleanValue DISABLE_STONE_HAMMER;
        public final ForgeConfigSpec.BooleanValue DISABLE_WOODEN_SWORD;
        public final ForgeConfigSpec.BooleanValue DISABLE_WOODEN_AXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_WOODEN_SHOVEL;
        public final ForgeConfigSpec.BooleanValue DISABLE_WOODEN_PICKAXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_WOODEN_HOE;
        public final ForgeConfigSpec.BooleanValue DISABLE_STONE_SWORD;
        public final ForgeConfigSpec.BooleanValue DISABLE_STONE_AXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_STONE_SHOVEL;
        public final ForgeConfigSpec.BooleanValue DISABLE_STONE_PICKAXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_STONE_HOE;
        public final ForgeConfigSpec.BooleanValue DISABLE_IRON_SWORD;
        public final ForgeConfigSpec.BooleanValue DISABLE_IRON_AXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_IRON_SHOVEL;
        public final ForgeConfigSpec.BooleanValue DISABLE_IRON_PICKAXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_IRON_HOE;
        public final ForgeConfigSpec.BooleanValue DISABLE_GOLDEN_SWORD;
        public final ForgeConfigSpec.BooleanValue DISABLE_GOLDEN_AXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_GOLDEN_SHOVEL;
        public final ForgeConfigSpec.BooleanValue DISABLE_GOLDEN_PICKAXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_GOLDEN_HOE;
        public final ForgeConfigSpec.BooleanValue DISABLE_DIAMOND_SWORD;
        public final ForgeConfigSpec.BooleanValue DISABLE_DIAMOND_AXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_DIAMOND_SHOVEL;
        public final ForgeConfigSpec.BooleanValue DISABLE_DIAMOND_PICKAXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_DIAMOND_HOE;
        public final ForgeConfigSpec.BooleanValue DISABLE_NETHERITE_SWORD;
        public final ForgeConfigSpec.BooleanValue DISABLE_NETHERITE_AXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_NETHERITE_SHOVEL;
        public final ForgeConfigSpec.BooleanValue DISABLE_NETHERITE_PICKAXE;
        public final ForgeConfigSpec.BooleanValue DISABLE_NETHERITE_HOE;

        public final ForgeConfigSpec.BooleanValue DISABLE_COMPASS;
        public final ForgeConfigSpec.BooleanValue DISABLE_CLOCK;
        public final ForgeConfigSpec.BooleanValue DISABLE_ALTIMETER;
        public final ForgeConfigSpec.BooleanValue DISABLE_THERMOMETER;
        public final ForgeConfigSpec.BooleanValue DISABLE_PHOTOMETER;
        public final ForgeConfigSpec.BooleanValue DISABLE_SPEEDOMETER;
        public final ForgeConfigSpec.BooleanValue DISABLE_BIOMETER;
        public final ForgeConfigSpec.BooleanValue DISABLE_MAGNETOMETER;


        public final ForgeConfigSpec.IntValue PROSPECTING_STICK_RANGE;
        public final ForgeConfigSpec.IntValue MAGNETOMETER_RANGE;
        public final ForgeConfigSpec.IntValue ORE_DETECTOR_RANGE;
        public final ForgeConfigSpec.BooleanValue ROCK_DRILL;
        public final ForgeConfigSpec.DoubleValue SAMPLE_CHANCE;

        public final ForgeConfigSpec.BooleanValue ALLOY_PICKAXE_BONUS;
        public final ForgeConfigSpec.BooleanValue ALLOY_AXE_BONUS;
        public final ForgeConfigSpec.BooleanValue ALLOY_HOE_BONUS;
        public final ForgeConfigSpec.BooleanValue ALLOY_SWORD_BONUS;
        public final ForgeConfigSpec.BooleanValue ALLOY_SHOVEL_BONUS;

        public Tools(ForgeConfigSpec.Builder b) {
            b.comment("Settings for tools").push("tools");

            b.comment("Rankine Tools").push("rankineTools");

                ALLOY_PICKAXE_BONUS = b.comment("Enable the bonus feature of the Alloy Pickaxe, which causes nuggets to drop from mining certain ores using Attack Damage and Attack Speed.")
                    .define("enableAlloyPickaxeBonus", true);
                ALLOY_AXE_BONUS = b.comment("Enable the bonus feature of the Alloy Axe, which allows harvesting an entire tree at a time.")
                        .define("enableAlloyAxeBonus", true);
                ALLOY_HOE_BONUS = b.comment("Enable the bonus feature of the Alloy Hoe, which allows replanting of crops by right-clicking and foraging.")
                        .define("enableAlloyHoeBonus", true);
                ALLOY_SWORD_BONUS = b.comment("Enable the bonus feature of the Alloy Sword, which causes a bleed effect based on Mining Speed.")
                        .define("enableAlloySwordBonus", true);
                ALLOY_SHOVEL_BONUS = b.comment("Enable the bonus feature of the Alloy Shovel, which causes nuggets to drop from mining certain ores.")
                        .define("enableAlloyShovelBonus", true);

                DISABLE_WOODEN_HAMMER = b.comment("Disable the use of the wooden hammer (still allows crafting for other recipes). This is enabled by default for progression.")
                        .define("disableWoodenHammer", false);
                DISABLE_STONE_HAMMER = b.comment("Disable the use of the stone hammer (still allows crafting for other recipes). This is enabled by default for progression.")
                        .define("disableStoneHammer", false);
                DISABLE_COMPASS = b.comment("Disable status bar message from compass.")
                        .define("disableCompass",false);
                DISABLE_CLOCK = b.comment("Disable status bar message from clock.")
                        .define("disableClock",false);
                DISABLE_ALTIMETER = b.comment("Disable status bar message from altimeter.")
                        .define("disableAltimeter",false);
                DISABLE_PHOTOMETER = b.comment("Disable status bar message from photometer.")
                        .define("disablePhotmeter",false);
                DISABLE_SPEEDOMETER = b.comment("Disable status bar message from speedometer.")
                        .define("disableSpeedometer",false);
                DISABLE_THERMOMETER = b.comment("Disable status bar message from thermometer.")
                        .define("disableThermometer",false);
                DISABLE_BIOMETER = b.comment("Disable status bar message from biometer.")
                        .define("disableBiometer",false);
                DISABLE_MAGNETOMETER = b.comment("Disable status bar message from magnetometer.")
                        .define("disableMagnetometer",false);
                PROSPECTING_STICK_RANGE = b.comment("Number of blocks away that the Prospecting Stick can detect ore.")
                        .defineInRange("prospectingStickRange", 6, 0, 64);
                SAMPLE_CHANCE = b.comment("Chance for the Prospecting Stick to return a sample. The chance for a sample to be returned from a stone cobble is half that.")
                        .defineInRange("cobbleSampleChance", 0.8D, 0.0D, 1.0D);
                MAGNETOMETER_RANGE = b.comment("Number of blocks away that the Magnetometer can detect ore. Square radius.")
                        .defineInRange("magnetometerRange", 6, 0, 16);
                ORE_DETECTOR_RANGE = b.comment("Number of blocks away that the Ore Detector can detect ore. Square radius.")
                        .defineInRange("oreDetectorRange", 7, 0, 16);
                ROCK_DRILL = b.comment("Enable the use of the rock drill.")
                        .define("rockDrill",true);
            b.pop();

            b.comment("Vanilla Tools").push("vanillaTools");
            b.comment("Wooden Tools").push("woodenTools");
            DISABLE_WOODEN_SWORD = b.comment("Disable the use of the wooden sword (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableWoodenSword", false);
            DISABLE_WOODEN_AXE = b.comment("Disable the use of the wooden axe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableWoodenAxe", false);
            DISABLE_WOODEN_SHOVEL = b.comment("Disable the use of the wooden shovel (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableWoodenShovel", false);
            DISABLE_WOODEN_PICKAXE = b.comment("Disable the use of the wooden pickaxe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableWoodenPickaxe", false);
            DISABLE_WOODEN_HOE = b.comment("Disable the use of the wooden hoe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableWoodenHoe", false);
            b.pop();
            b.comment("Stone Tools").push("stoneTools");
            DISABLE_STONE_SWORD = b.comment("Disable the use of the stone sword (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableStoneSword", false);
            DISABLE_STONE_AXE = b.comment("Disable the use of the stone axe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableStoneAxe", false);
            DISABLE_STONE_SHOVEL = b.comment("Disable the use of the stone shovel (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableStoneShovel", false);
            DISABLE_STONE_PICKAXE = b.comment("Disable the use of the stone pickaxe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableStonePickaxe", false);
            DISABLE_STONE_HOE = b.comment("Disable the use of the stone hoe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableStoneHoe", false);
            b.pop();
            b.comment("Iron Tools").push("ironTools");
            DISABLE_IRON_SWORD = b.comment("Disable the use of the iron sword (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableIronSword", false);
            DISABLE_IRON_AXE = b.comment("Disable the use of the iron axe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableIronAxe", false);
            DISABLE_IRON_SHOVEL = b.comment("Disable the use of the iron shovel (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableIronShovel", false);
            DISABLE_IRON_PICKAXE = b.comment("Disable the use of the iron pickaxe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableIronPickaxe", false);
            DISABLE_IRON_HOE = b.comment("Disable the use of the iron hoe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableIronHoe", false);
            b.pop();
            b.comment("Gold Tools").push("goldTools");
            DISABLE_GOLDEN_SWORD = b.comment("Disable the use of the gold sword (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableGoldSword", false);
            DISABLE_GOLDEN_AXE = b.comment("Disable the use of the gold axe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableGoldAxe", false);
            DISABLE_GOLDEN_SHOVEL = b.comment("Disable the use of the gold shovel (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableGoldShovel", false);
            DISABLE_GOLDEN_PICKAXE = b.comment("Disable the use of the gold pickaxe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableGoldPickaxe", false);
            DISABLE_GOLDEN_HOE = b.comment("Disable the use of the gold hoe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableGoldHoe", false);
            b.pop();
            b.comment("Diamond Tools").push("diamondTools");
            DISABLE_DIAMOND_SWORD = b.comment("Disable the use of the diamond sword (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableDiamondSword", false);
            DISABLE_DIAMOND_AXE = b.comment("Disable the use of the diamond axe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableDiamondAxe", false);
            DISABLE_DIAMOND_SHOVEL = b.comment("Disable the use of the diamond shovel (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableDiamondShovel", false);
            DISABLE_DIAMOND_PICKAXE = b.comment("Disable the use of the diamond pickaxe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableDiamondPickaxe", false);
            DISABLE_DIAMOND_HOE = b.comment("Disable the use of the diamond hoe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableDiamondHoe", false);
            b.pop();
            b.comment("Netherite Tools").push("netheriteTools");
            DISABLE_NETHERITE_SWORD = b.comment("Disable the use of the netherite sword (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableNetheriteSword", false);
            DISABLE_NETHERITE_AXE = b.comment("Disable the use of the netherite axe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableNetheriteAxe", false);
            DISABLE_NETHERITE_SHOVEL = b.comment("Disable the use of the netherite shovel (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableNetheriteShovel", false);
            DISABLE_NETHERITE_PICKAXE = b.comment("Disable the use of the netherite pickaxe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableNetheritePickaxe", false);
            DISABLE_NETHERITE_HOE = b.comment("Disable the use of the netherite hoe (still allows crafting for other recipes). This is disabled by default for progression.")
                    .define("disableNetheriteHoe", false);
            b.pop();
            b.pop();
            b.pop();
        }
    }


    public static class General {
        public final ForgeConfigSpec.BooleanValue MOVEMENT_MODIFIERS;
        public final ForgeConfigSpec.BooleanValue MOVEMENT_MODIFIERS_FOV;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_SAND;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_GRASS_PATH;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_BRICKS;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_ROMAN_CONCRETE;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_DIRT;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_POLISHED_STONE;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_WOODEN;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_CONCRETE;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_SNOW;
        public final ForgeConfigSpec.DoubleValue MOVEMENT_MUD;

        public final ForgeConfigSpec.BooleanValue SLUICING_COOLDOWN;
        public final ForgeConfigSpec.BooleanValue CROWBAR_FROM_ABOVE;
        public final ForgeConfigSpec.BooleanValue PLAYER_PRYING_ENCHANTMENT;

        public final ForgeConfigSpec.DoubleValue NUGGET_CHANCE;
        public final ForgeConfigSpec.IntValue NUGGET_DISTANCE;




        public final ForgeConfigSpec.IntValue MAX_TREE;
        public final ForgeConfigSpec.BooleanValue TREE_CHOPPING;
        public final ForgeConfigSpec.DoubleValue TREE_CHOP_SPEED;
        public final ForgeConfigSpec.DoubleValue LEAF_LITTER_GEN;
        public final ForgeConfigSpec.DoubleValue LEAF_LITTER_GEN_TREES;
        public final ForgeConfigSpec.DoubleValue SAPLING_GROW;

        public final ForgeConfigSpec.IntValue PATH_CREATION_TIME;
        public final ForgeConfigSpec.BooleanValue PATH_CREATION;
        public final ForgeConfigSpec.BooleanValue PUMICE_SOAP;
        public final ForgeConfigSpec.BooleanValue FLINT_FIRE;
        public final ForgeConfigSpec.BooleanValue STUMP_CREATION;
        public final ForgeConfigSpec.BooleanValue STRIPPABLES_STICKS;
        public final ForgeConfigSpec.DoubleValue FLINT_FIRE_CHANCE;
        public final ForgeConfigSpec.DoubleValue FLINT_DROP_CHANCE;
        public final ForgeConfigSpec.DoubleValue GRASS_GROW_CHANCE;
        public final ForgeConfigSpec.DoubleValue PODZOL_GROW_CHANCE;
        public final ForgeConfigSpec.IntValue LEAF_LITTER_GROWTH;
        public final ForgeConfigSpec.BooleanValue MANDATORY_AXE;
        public final ForgeConfigSpec.BooleanValue REFRESH_ALLOYS;
        public final ForgeConfigSpec.BooleanValue STARTING_BOOK;
        public final ForgeConfigSpec.BooleanValue DISABLE_WATER;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> FUEL_VALUES_LIST;
        public final ForgeConfigSpec.BooleanValue LIGHTNING_CONVERSION;
        public final ForgeConfigSpec.BooleanValue PENDANT_CURSE;
        public final ForgeConfigSpec.BooleanValue VILLAGER_TRADES;
        public final ForgeConfigSpec.BooleanValue WANDERING_TRADE_SPECIAL;
        public final ForgeConfigSpec.DoubleValue ROCK_GENERATOR_REMOVAL_CHANCE;
        public final ForgeConfigSpec.DoubleValue GLOWSTONE_GAS_CHANCE;
        public final ForgeConfigSpec.BooleanValue IGNEOUS_COBBLE_GEN;
        public final ForgeConfigSpec.BooleanValue METAMORPHIC_STONE_GEN;
        public final ForgeConfigSpec.DoubleValue GLOBAL_BREAK_EXHAUSTION;
        public final ForgeConfigSpec.DoubleValue CHEESE_AGE_CHANCE;
        public final ForgeConfigSpec.DoubleValue ICE_BREAK;
        public final ForgeConfigSpec.DoubleValue GEODE_CHANCE;
        public final ForgeConfigSpec.DoubleValue FUMAROLE_DEPOSIT_RATE;
        public final ForgeConfigSpec.IntValue HERBICIDE_RANGE;
        public final ForgeConfigSpec.IntValue ICEMELT_RANGE;
        public final ForgeConfigSpec.IntValue TRAMPOLINE_SIZE;
        public final ForgeConfigSpec.IntValue FIRE_EXTINGUISHER_RANGE;
        public final ForgeConfigSpec.IntValue FORCE_BREAK;

        public final ForgeConfigSpec.DoubleValue TOTEM_PROMISING_CHANCE;


        public General(ForgeConfigSpec.Builder b) {
            b.comment("Settings for general mechanics").push("general");

                b.comment("Miscellaneous").push("misc");
                    PATH_CREATION_TIME = b.comment("The 1 in X chance for grass blocks to convert to path blocks while a player is on them.")
                            .defineInRange("pathCreationTime", 8, 1, Integer.MAX_VALUE);
                    PATH_CREATION = b.comment("If enabled, walking on grass blocks, mycelium and podzol has a chance to create a path block underfoot.")
                            .define("pathCreation",true);
                    PUMICE_SOAP = b.comment("If enabled, pumice soap can repair mossy/cracked stone bricks and polish stones.")
                            .define("pumiceSoapEnabled",true);
                    STRIPPABLES_STICKS = b.comment("If enabled, sticks will drop from logs when stripped (30% chance).")
                            .define("strippablesSticks",true);
                    HERBICIDE_RANGE = b.comment("The radius at which herbicide will kill plants.")
                            .defineInRange("herbicideRange", 8, 0, 16);
                    ICEMELT_RANGE = b.comment("The radius at which Ice Melt will melt snow and ice.")
                            .defineInRange("iceMeltRange", 3, 0, 16);
                    DISABLE_WATER = b.comment("No more infinite water")
                            .define("disableWater",true);
                    LIGHTNING_CONVERSION = b.comment("Lightning strikes creating fulgurite and glasses")
                            .define("enableLightningConversion",true);
                    FUEL_VALUES_LIST = b.comment("List of blocks and their respective burn time. Works with tags.")
                            .defineList("fuelValues", List.of("#forge:rods/wooden|50","#minecraft:saplings|100","#minecraft:wooden_doors|200","#minecraft:wooden_trapdoors|300","#minecraft:wooden_fence_gates|400","#minecraft:wooden_fences|150","#minecraft:wooden_pressure_plates|200","#minecraft:wooden_stairs|75","#minecraft:wooden_slabs|50","#minecraft:wooden_buttons|100","#minecraft:planks|100","#minecraft:oak_logs|520","#minecraft:acacia_logs|500","#minecraft:birch_logs|450","#minecraft:spruce_logs|410","#minecraft:jungle_logs|450","#minecraft:dark_oak_logs|520","#rankine:magnolia_logs|450","#rankine:balsam_fir_logs|390","#rankine:eastern_hemlock_logs|440","#rankine:juniper_logs|480","#rankine:black_birch_logs|470","#rankine:yellow_birch_logs|490","#rankine:pinyon_pine_logs|520","#rankine:maple_logs|500","#rankine:cedar_logs|410","#rankine:black_walnut_logs|470","#rankine:cedar_logs|410","#rankine:coconut_palm_logs|450","#rankine:sharinga_logs|450","#rankine:cork_oak_logs|480","#rankine:erythrina_logs|550","#rankine:cinnamon_logs|500","#rankine:charred_logs|400","#rankine:petrified_chorus_logs|450","#rankine:hollow_logs|100","#forge:sulfur|400","#forge:storage_blocks/sulfur|4000","minecraft:charcoal|800"), o -> o instanceof String);
                    FIRE_EXTINGUISHER_RANGE = b.comment("The range of the fire extinguisher.")
                            .defineInRange("fireExtinguisherRange", 16, 0, 64);
                    TRAMPOLINE_SIZE = b.comment("The maximum size of a trampoline. Jump factor depends on size. Set to 0 to have a fixed jump factor of 1.3 which is just enough to have the player gain height over time.")
                            .defineInRange("trampolineSize", 289, 0, 961);
                    CHEESE_AGE_CHANCE = b.comment("Chance for unaged cheese to age in a random tick.")
                            .defineInRange("cheeseAgeChance", 0.1D, 0.0D, 1.0D);
                    GEODE_CHANCE = b.comment("Chance for a geode to be found in stone.")
                            .defineInRange("geodeChance", 0.0005D, 0.0D, 1.0D);
                    GLOWSTONE_GAS_CHANCE = b.comment("Chance for a glowstone to spawn gas block when broken. The chance is for the Nether and the End is 5x more likely.")
                            .defineInRange("glowstoneGasChance", 0.1D, 0.0D, 1.0D);
                    STARTING_BOOK = b.comment("Enables the Rankine Journal (a guide to the mod, requires Patchouli)")
                            .define("startingBook",true);
                    REFRESH_ALLOYS = b.comment("If enabled, alloy-related content in the player's inventory will always refresh on world join. Useful for modifying element recipes and quickly determining changes.")
                            .define("refreshAlloys",false);
                    PENDANT_CURSE = b.comment("Causes Pendants to spawn in with Curse of Vanishing.")
                            .define("pendantCurse",true);
                    MANDATORY_AXE = b.comment("Makes axes required to harvest logs.")
                            .define("axesOnly",false);
                    SLUICING_COOLDOWN = b.comment("Enables cooldown on items used for sluicing recipes.")
                            .define("sluicingCooldown",true);
                    CROWBAR_FROM_ABOVE = b.comment("Allows crowbars to move blocks below where the player is standing.")
                            .define("crowbarFromAbove",true);
                    FUMAROLE_DEPOSIT_RATE = b.comment("Chance for a fumarole to convert blocks into fumarole deposits.")
                            .defineInRange("fumaroleDepositChance", 0.1D, 0.00D, 1.00D);
                    FLINT_DROP_CHANCE = b.comment("Chance for a stone block to drop a flint")
                            .defineInRange("flintDropChance", 0.15D, 0.00D, 1.00D);
                    GRASS_GROW_CHANCE = b.comment("Chance for a grass block to grow something on a random tick")
                            .defineInRange("grassGrowChance", 0.001D, 0.00D, 1.00D);
                    LEAF_LITTER_GROWTH = b.comment("Chance on random tick for a leaf litters to age. Higher values slow decay.")
                            .defineInRange("leafLitterDecay", 2, 1, Integer.MAX_VALUE);
                    PODZOL_GROW_CHANCE = b.comment("Chance for a podzol block to grow on grass")
                            .defineInRange("podzolGrowChance", 0.0005D, 0.00D, 1.00D);
                    ROCK_GENERATOR_REMOVAL_CHANCE = b.comment("Chance for a mineral block to be removed from any rock generator process.")
                            .defineInRange("rockGenRemovalChance", 0.01D, 0.00D, 1.00D);
                    IGNEOUS_COBBLE_GEN = b.comment("Change the output of a cobblestone generator and basalt generator to intrusive and extrusive igneous rocks respectively.")
                            .define("igneousGen",true);
                    METAMORPHIC_STONE_GEN = b.comment("Change the output of a stone generator from stone to metamorphic rocks.")
                            .define("igneousGen",true);
                    VILLAGER_TRADES = b.comment("Adds trades for Project Rankine to Villagers and the Wandering Trader.")
                            .define("villageTrades",true);
                    WANDERING_TRADE_SPECIAL = b.comment("Adds a trade to the Wandering Trader for a random tool which is not restricted by alloy constraints. May be unbalanced due to complete randomness.")
                            .define("wanderingSpecial",true);
                    GLOBAL_BREAK_EXHAUSTION = b.comment("Amount of additional exhaustion when breaking a block.")
                            .defineInRange("breakExhaustion", 0.00D, 0.00D, 1.00D);
                    ICE_BREAK = b.comment("Chance for ice to break when walking on it.")
                            .defineInRange("iceBreak", 0.002D, 0.0D, 1.0D);
                    FLINT_FIRE = b.comment("Enable the lighting of fires and certain machines using two flint.")
                            .define("flintFire",true);
                    FLINT_FIRE_CHANCE = b.comment("Chance for flint to be consumed when lighting a fire.")
                            .defineInRange("flintFireChance", 0.30D, 0.00D, 1.00D);
                b.pop();

                b.comment("Tree Configs").push("treeConfigs");
                    TREE_CHOPPING = b.comment("Enable full tree chopping using #rankine:tree_choppers")
                            .define("treeChopping",true);
                    MAX_TREE = b.comment("Maximum blocks to be considered a tree. Set to 0 to disable tree capitation.")
                        .defineInRange("maxTree", 256, 0, 1024);
                    TREE_CHOP_SPEED = b.comment("Speed factor for chopping trees after size is accounted for.")
                            .defineInRange("treeChopSpeedFactor", 1.0D, 0.0D, 5.0D);
                    LEAF_LITTER_GEN = b.comment("Chance for leaves to drop leaf litter on a random tick")
                            .defineInRange("leafLitterChance", 0.005D, 0.0D, 1.0D);
                    LEAF_LITTER_GEN_TREES = b.comment("Chance for leaves to drop leaf litter on break from chopping")
                            .defineInRange("leafLitterChanceChop", 0.1D, 0.0D, 1.0D);
                    STUMP_CREATION = b.comment("Creates stumps when tree chopping.")
                            .define("createStumps",true);
                    FORCE_BREAK = b.comment("The range to force break leaves.")
                            .defineInRange("forceBreakRange", 3, 0, 10);
                    SAPLING_GROW = b.comment("Chance for a sapling to grow. Affects bonemeal.")
                            .defineInRange("saplingGrowChance", 0.9D, 0.0D, 1.0D);
                b.pop();

                b.comment("Movement speed modifiers").push("movementModifiers");
                    MOVEMENT_MODIFIERS = b.comment("Set to false to disable movement speed modifiers.")
                            .define("movementModifiersEnabled",true);
                    MOVEMENT_MODIFIERS_FOV = b.comment("When set to true, attempts to disable the FOV changes of walking slower on movement modifier blocks. The FOV effects setting can also be used to disable this (at 0%), and if used this variable should be set to false.")
                            .define("movementModifiersFOVReset",false);
                    MOVEMENT_SAND = b.comment("Movement speed modifier for walking on Sand blocks.")
                            .defineInRange("movementSand", -0.02D, -1.0D, 1.0D);
                    MOVEMENT_BRICKS = b.comment("Movement speed modifier for walking on Brick / Stone Bricks and variants.")
                            .defineInRange("movementBricks", 0.02D, -1.0D, 1.0D);
                    MOVEMENT_GRASS_PATH = b.comment("Movement speed modifier for walking on Grass Paths.")
                            .defineInRange("movementGrassPath", 0.00D, -1.0D, 1.0D);
                    MOVEMENT_ROMAN_CONCRETE = b.comment("Movement speed modifier for walking on Roman Cooncrete.")
                            .defineInRange("movementRomanConcrete", 0.07D, -1.0D, 1.0D);
                    MOVEMENT_DIRT = b.comment("Movement speed modifier for walking on Dirt / Grass blocks.")
                            .defineInRange("movementDirt", -0.01D, -1.0D, 1.0D);
                    MOVEMENT_POLISHED_STONE = b.comment("Movement speed modifier for walking on Polished Stone blocks.")
                            .defineInRange("movementPolishedStone", 0.00D, -1.0D, 1.0D);
                    MOVEMENT_WOODEN = b.comment("Movement speed modifier for walking on Planks and wooden variants.")
                            .defineInRange("movementWooden", 0.00D, -1.0D, 1.0D);
                    MOVEMENT_CONCRETE = b.comment("Movement speed modifier for walking on Concrete / Cement")
                            .defineInRange("movementConcrete", 0.02D, -1.0D, 1.0D);
                    MOVEMENT_SNOW = b.comment("Movement speed modifier for walking on Snow.")
                            .defineInRange("movementSnow", -0.02D, -1.0D, 1.0D);
                    MOVEMENT_MUD = b.comment("Movement speed modifier for walking on Mud.")
                            .defineInRange("movementMud", -0.03D, -1.0D, 1.0D);
                b.pop();

                b.comment("Ore Detection").push("oreDetection");
                    NUGGET_CHANCE = b.comment("Chance for a block in #rankine:nugget_stones to drop a nugget of a nearby ore.")
                            .defineInRange("nuggetChance", 0.04D, 0.00D, 1.00D);
                    NUGGET_DISTANCE = b.comment("Distance from an ore block in which nuggets have a chance to drop from blocks.")
                            .defineInRange("nuggetRange", 4, 1, 16);
                b.pop();

                b.comment("Enchantments").push("enchantments");
                    PLAYER_PRYING_ENCHANTMENT = b.comment("Enables the Prying enchantment to work on players (when hit by crowbar, chance to drop held item).")
                            .define("playerPryingEnchantment",true);
                b.pop();

                b.comment("Totems").push("totems");
                    TOTEM_PROMISING_CHANCE = b.comment("Chance for an extra block to be dropped when using the Totem of Promising")
                            .defineInRange("totemOfPromisingChance",0.15D, 0.00D, 1.00D);
                b.pop();


            b.pop();
        }
    }

    public static class Alloys {
        public final ForgeConfigSpec.IntValue ALLOY_MODIFIERS_MAX;

        public final ForgeConfigSpec.BooleanValue ALLOY_CORROSION;
        public final ForgeConfigSpec.IntValue ALLOY_CORROSION_AMT;
        public final ForgeConfigSpec.BooleanValue ALLOY_HEAT;
        public final ForgeConfigSpec.IntValue ALLOY_HEAT_AMT;
        public final ForgeConfigSpec.BooleanValue ALLOY_TOUGHNESS;
        public final ForgeConfigSpec.DoubleValue ALLOY_WEAR_MINING_AMT;
        public final ForgeConfigSpec.DoubleValue ALLOY_WEAR_DAMAGE_AMT;

        public final ForgeConfigSpec.IntValue ALLOY_BONUS_DURABILITY;
        public final ForgeConfigSpec.DoubleValue ALLOY_BONUS_MINING_SPEED;
        public final ForgeConfigSpec.IntValue ALLOY_BONUS_HL;
        public final ForgeConfigSpec.IntValue ALLOY_BONUS_ENCHANTABILITY;
        public final ForgeConfigSpec.DoubleValue ALLOY_BONUS_DAMAGE;
        public final ForgeConfigSpec.DoubleValue ALLOY_BONUS_ATTACK_SPEED;
        public final ForgeConfigSpec.DoubleValue ALLOY_BONUS_CORR_RESIST;
        public final ForgeConfigSpec.DoubleValue ALLOY_BONUS_HEAT_RESIST;
        public final ForgeConfigSpec.DoubleValue ALLOY_BONUS_TOUGHNESS;

        public Alloys(ForgeConfigSpec.Builder b) {
            b.comment("Settings for alloys and alloy tools").push("alloys");
                ALLOY_MODIFIERS_MAX = b.comment("Sets the maximum number of unique modifiers that can be applied at the smithing table.")
                        .defineInRange("alloyModifiersMax", 1, 0, 5);
                ALLOY_CORROSION = b.comment("Enables the corrosion negative modifier for alloy tools (chance to consume extra points of durability in water and rain)")
                        .define("alloyCorrosion",true);
                ALLOY_CORROSION_AMT = b.comment("If enabled, modifies the amount of durability damage taken in wet environments.")
                        .defineInRange("alloyCorrosionDmgAmount", 1, 1, 10);
                ALLOY_HEAT = b.comment("Enables the heat negative modifier for alloy tools (chance to consume extra points of durability in hot environments and lava)")
                        .define("alloyHeat",true);
                ALLOY_HEAT_AMT = b.comment("If enabled, modifies the amount of durability damage taken in hot environments.")
                        .defineInRange("alloyHeatDmgAmount", 1, 1, 10);
                ALLOY_TOUGHNESS = b.comment("Enables the toughness negative modifier for alloy tools (chance to consume/resist loss of an extra point of durability)")
                        .define("alloyToughness",true);
                ALLOY_WEAR_MINING_AMT = b.comment("Modifies the severity of the wear effect on mining speed (ex. 0.25 means mining speed will be reduced to 75% of the original value as durability is lost)")
                        .defineInRange("alloyWearMiningAmount", 0.25D, 0.00D, 0.99D);
                ALLOY_WEAR_DAMAGE_AMT = b.comment("Modifies the severity of the wear effect on damage (ex. 0.25 means damage will be reduced to 75% of the original value as durability is lost)")
                        .defineInRange("alloyWearDamageAmount", 0.25D, 0.00D, 0.99D);
                b.pop();
                b.comment("Custom Alloy Tool Properties").push("alloy");
                ALLOY_BONUS_DURABILITY = b.comment("Adds bonus durability for the custom alloy tools.")
                        .defineInRange("alloyBonusDurability", 0, 0, 10000);
                ALLOY_BONUS_MINING_SPEED = b.comment("Adds bonus mining speed for the custom alloy tools.")
                        .defineInRange("alloyBonusMiningSpeed", 0.0D, 0D, 20D);
                ALLOY_BONUS_HL = b.comment("Adds bonus to harvest level for the custom alloy tools.")
                        .defineInRange("alloyBonusHL", 0, 0, 10);
                ALLOY_BONUS_ENCHANTABILITY = b.comment("Adds bonus enchantability for the custom alloy tools.")
                        .defineInRange("alloyBonusEnchantability", 0, 0, 40);
                ALLOY_BONUS_ATTACK_SPEED = b.comment("Adds bonus attack speed for the custom alloy tools.")
                        .defineInRange("alloyBonusAttackSpeed", 0.0D, 0D, 4D);
                ALLOY_BONUS_DAMAGE = b.comment("Adds bonus damage for the custom alloy tools.")
                        .defineInRange("alloyBonusDamage", 0.0D, 0D, 20D);
                ALLOY_BONUS_CORR_RESIST = b.comment("Adds bonus corrosion resistance for the custom alloy tools.")
                        .defineInRange("alloyBonusCorrResist", 0.0D, 0D, 1D);
                ALLOY_BONUS_HEAT_RESIST = b.comment("Adds bonus heat resistance for the custom alloy tools.")
                        .defineInRange("alloyBonusHeatResist", 0.0D, 0D, 1D);
                ALLOY_BONUS_TOUGHNESS = b.comment("Adds bonus toughness for the custom alloy tools.")
                        .defineInRange("alloyBonusToughness", 0D, -1D, 1D);
                b.pop();

        }
    }

    public static class Machines {

        public final ForgeConfigSpec.IntValue GROUND_TAP_SPEED;
        public final ForgeConfigSpec.IntValue CHARCOAL_PIT_SPEED;
        public final ForgeConfigSpec.IntValue CHARCOAL_PIT_RADIUS;
        public final ForgeConfigSpec.IntValue CHARCOAL_PIT_HEIGHT;
        public final ForgeConfigSpec.IntValue FLOOD_GATE_RANGE;
        public final ForgeConfigSpec.IntValue ELECTROMAGNET_RANGE;
        public final ForgeConfigSpec.IntValue MAGNET_RANGE;
        public final ForgeConfigSpec.BooleanValue ELECTROMAGNET_MATERIAL_REQ;
        public final ForgeConfigSpec.BooleanValue EVAPORATION_TOWER_MAINTENANCE;
        public final ForgeConfigSpec.IntValue GAS_BOTTLER_SPEED;

        public final ForgeConfigSpec.IntValue FUSION_FURNACE_POWER;
        public final ForgeConfigSpec.IntValue INDUCTION_FURNACE_POWER;
        public final ForgeConfigSpec.IntValue AIR_DISTILLATION_SPEED;


        public Machines(ForgeConfigSpec.Builder b) {
            b.comment("Settings for machines").push("machines");
                AIR_DISTILLATION_SPEED = b.comment("Processing speed of the air distillation tower")
                        .defineInRange("airDistillationSpeed", 100, 10, Integer.MAX_VALUE);
                GROUND_TAP_SPEED = b.comment("The number of ticks it takes the Ground Tap to process")
                        .defineInRange("groundTapSpeed", 600, 0, Integer.MAX_VALUE);
                GAS_BOTTLER_SPEED = b.comment("The number of ticks it takes the Gas Bottler to process")
                        .defineInRange("gasBottlerSpeed", 60, 0, Integer.MAX_VALUE);
                CHARCOAL_PIT_RADIUS = b.comment("Maximum radius the charcoal pit can convert logs.")
                        .defineInRange("charcoalPitRadius", 7, 3, 15);
                CHARCOAL_PIT_SPEED = b.comment("The number of ticks it takes the Charcoal Pit to process. There is some randomization.")
                        .defineInRange("charcoalPitSpeed", 3600, 1, Integer.MAX_VALUE);
                CHARCOAL_PIT_HEIGHT = b.comment("Maximum height a charcoal pile can be")
                        .defineInRange("charcoalPitHeight", 5, 1, 10);
                MAGNET_RANGE = b.comment("Range for the Simple Magnet. The Alnico and Rare Earth versions scale at x2 and x3 respectively.")
                        .defineInRange("magnetRange",4,1,8);
                ELECTROMAGNET_RANGE = b.comment("Range for the Simple Electromagnet. The Alnico and Rare Earth versions scale at x2 and x3 respectively.")
                        .defineInRange("electromagnetRange",5,1,10);
                FLOOD_GATE_RANGE = b.comment("Maximum number of blocks the Flood Gate will search for when placing fluid. Set to 0 to disable this ability.")
                        .defineInRange("floodGateRange",128,0,Integer.MAX_VALUE);
                ELECTROMAGNET_MATERIAL_REQ = b.comment("Require the material of the block to be Material.IRON in order for the electromagnet to pull the block. If disabled, it will pick up any block as long as it is not a FluidBlock, Tile Entity, or in the rankine:magnet_banned tag (these blocks are also banned if this value is true).")
                        .define("electromagnetMaterialReq",true);
                INDUCTION_FURNACE_POWER = b.comment("Defines the power requirement for one process in the induction furnace.")
                        .defineInRange("inductionFurnacePower", 16, 0, 10000);
                FUSION_FURNACE_POWER = b.comment("Defines the power requirement for one process in the fusion furnace.")
                        .defineInRange("fusionFurnacePower", 2, 0, 10000);
                EVAPORATION_TOWER_MAINTENANCE = b.comment("If enabled, sheetmetal from the evaporation tower will occasionally break.")
                        .define("evaporationTowerMaintenance",true);
            b.pop();
        }
    }

    public static class Gases {
        public final ForgeConfigSpec.BooleanValue GAS_MOVEMENT;
        public final ForgeConfigSpec.BooleanValue ENABLE_GAS_VENTS;
        public final ForgeConfigSpec.BooleanValue GAS_AFFECT_UNDEAD;
        public final ForgeConfigSpec.BooleanValue GAS_DISSIPATION;


        public Gases(ForgeConfigSpec.Builder b) {
            b.comment("Settings for Gases.").push("gases");
            GAS_MOVEMENT = b.comment("If enabled, gases will move on random tick and dissipate at or above y-level 95 (EXPERIMENTAL).")
                    .define("gasMovement", true);
            GAS_AFFECT_UNDEAD = b.comment("If enabled, gas effects will work against undead mobs.")
                    .define("gasAffectUndead", true);
            ENABLE_GAS_VENTS = b.comment("Enables blocks which emit gases on random tick.")
                    .define("enableGasVents", true);
            GAS_DISSIPATION = b.comment("Enables gas blocks to have a chance to remove themselves on random tick (based on the gas).")
                    .define("enableGasDissipation", true);

            b.pop();
        }
    }

    public static class HardMode {
        public final ForgeConfigSpec.BooleanValue WATER_REACTIVE;
        public final ForgeConfigSpec.BooleanValue RADIOACTIVE;


        public HardMode(ForgeConfigSpec.Builder b) {
            b.comment("Settings for Hard Mode mechanics (HIGHLY EXPERIMENTAL).").push("hardMode");
            WATER_REACTIVE = b.comment("If enabled, certain elements will react with water. Generally creates an explosion.")
                    .define("elementWaterReactive", false);
            RADIOACTIVE = b.comment("If enabled, certain elements will be radioactive which applies a radiation potion effect that causes damage over time.")
                    .define("elementRadioactive", false);
            b.pop();
        }
    }

    public static class Worldgen {
        public final ForgeConfigSpec.IntValue BEDROCK_LAYERS;
        public final ForgeConfigSpec.BooleanValue DISABLE_VANILLA_FEATURES;
        public final ForgeConfigSpec.BooleanValue RANKINE_FLORA;
        public final ForgeConfigSpec.BooleanValue RANKINE_TREES;
        public final ForgeConfigSpec.BooleanValue MUSHROOMS;
        public final ForgeConfigSpec.BooleanValue FALLEN_LOGS;
        public final ForgeConfigSpec.BooleanValue COBBLES_GEN;
        public final ForgeConfigSpec.IntValue FUMAROLE_GEN;
        public final ForgeConfigSpec.BooleanValue FIRE_CLAY_GEN;
        public final ForgeConfigSpec.BooleanValue WHITE_SAND_GEN;
        public final ForgeConfigSpec.BooleanValue BLACK_SAND_GEN;
        public final ForgeConfigSpec.BooleanValue RETRO_GEN;
        public final ForgeConfigSpec.BooleanValue SOIL_GEN;
        public final ForgeConfigSpec.BooleanValue ANTIMATTER_GEN;
        public final ForgeConfigSpec.BooleanValue REPLACE_VANILLA_ORES;
        public final ForgeConfigSpec.BooleanValue END_METEORITE_GEN;
        public final ForgeConfigSpec.BooleanValue METEORITE_GEN;
        public final ForgeConfigSpec.DoubleValue END_METEORITE_CHANCE;
        public final ForgeConfigSpec.DoubleValue BIG_METEORITE_CHANCE;
        public final ForgeConfigSpec.IntValue METEORITE_SIZE;
        public final ForgeConfigSpec.IntValue METEORITE_CHANCE;
        public final ForgeConfigSpec.IntValue NETHERRACK_LAYER_THICKNESS;
        public final ForgeConfigSpec.IntValue SOUL_SANDSTONE_LAYER_THICKNESS;

        //public final ForgeConfigSpec.BooleanValue COLUMN_GEN;
        //public final ForgeConfigSpec.DoubleValue COLUMN_CHANCE;
        //public final ForgeConfigSpec.DoubleValue COLUMN_FREQUENCY;
        public final ForgeConfigSpec.IntValue LAYER_GEN;
        public final ForgeConfigSpec.DoubleValue LAYER_BEND;
        public final ForgeConfigSpec.IntValue LAYER_THICKNESS;
        public final ForgeConfigSpec.IntValue NOISE_SCALE;
        public final ForgeConfigSpec.IntValue SOIL_NOISE_SCALE;

        public final ForgeConfigSpec.BooleanValue INTRUSION_GEN;
        public final ForgeConfigSpec.IntValue OVERWORLD_INTRUSION_RADIUS;
        public final ForgeConfigSpec.DoubleValue OVERWORLD_INTRUSION_SHRINK;
        public final ForgeConfigSpec.DoubleValue OVERWORLD_INTRUSION_SHIFT;
        public final ForgeConfigSpec.DoubleValue INTRUSION_CINNABAR_ORE;
        public final ForgeConfigSpec.IntValue NETHER_INTRUSION_RADIUS;
        public final ForgeConfigSpec.DoubleValue NETHER_INTRUSION_SHRINK;
        public final ForgeConfigSpec.DoubleValue NETHER_INTRUSION_SHIFT;

        public Worldgen(ForgeConfigSpec.Builder b) {
            b.comment("Here are miscellaneous worldgen options.").push("worldgen");
            BEDROCK_LAYERS = b.comment("The number of flat bedrock layers to generate. Set to 0 to disable.")
                    .defineInRange("flatBedrockLayers", 0, 0, 5);
            SOIL_NOISE_SCALE = b.comment("This determines how mixed the two types of soil are per biome. Larger numbers mean larger patches.")
                    .defineInRange("soilNoiseScale", 60, 1, Integer.MAX_VALUE);
            ANTIMATTER_GEN = b.comment("Generate antimatter in the End.")
                    .define("antimatterGen",true);
            SOIL_GEN = b.comment("Generate soil varieties.")
                    .define("soilGen",true);
            FIRE_CLAY_GEN = b.comment("Generate fire clay under coal veins")
                    .define("fireClayGen",true);
            RETRO_GEN = b.comment("Enable the retrogen of chunks for Rankine generation. This controls soils, grasses, gravels, sands, and matching ores to the stone layer.")
                    .define("retroGen",true);
            REPLACE_VANILLA_ORES = b.comment("If enabled, replaces vanilla ores with the Rankine counterparts (mostly for texture purposes). Results may vary due to the order of feature placements.")
                    .define("replaceVanillaOres",true);
            DISABLE_VANILLA_FEATURES = b.comment("Disable vanilla features in the overworld. Works by replacing the listed blocks in #rankine:vanilla_override with stones")
                    .define("disableVanillaOres",true);
            RANKINE_FLORA = b.comment("Enable/Disable Project Rankine flowers and berry bushes in world.")
                    .define("generateFlora",true);
            RANKINE_TREES = b.comment("Enable/Disable Project Rankine trees in world.")
                    .define("generateTrees",true);
            MUSHROOMS = b.comment("Enable/Disable Project Rankine mushrooms in world.")
                    .define("generateMushrooms",true);
            FALLEN_LOGS = b.comment("Enable Project Rankine fallen log features in world.")
                    .define("generateMushrooms",true);
            COBBLES_GEN = b.comment("Enable/Disable Project Rankine cobbles in world.")
                    .define("generateCobbles",true);
            FUMAROLE_GEN = b.comment("Average number of chuncks to generate a fumarole in. Set to 0 to disable.")
                    .defineInRange("fumaroleGenerationChance", 20, 0, Integer.MAX_VALUE);
            WHITE_SAND_GEN = b.comment("Enables the generation of white sand disks in beaches.")
                    .define("generateWhiteSand",true);
            BLACK_SAND_GEN = b.comment("Enables the generation of black sand disks in the Nether.")
                    .define("generateBlackSand",true);
            END_METEORITE_GEN = b.comment("Enable to generate meteorites in the end.")
                    .define("endMeteoriteGen",true);
            END_METEORITE_CHANCE = b.comment("The chance for an end meteroite.")
                    .defineInRange("endMeteoriteChance", 0.03, 0.00, 1.00);
            METEORITE_GEN = b.comment("Enable to generate meteorites in the overworld.")
                    .define("meteoriteGen",true);
            METEORITE_SIZE = b.comment("Size parameter for meteorites. Higher number is bigger.")
                    .defineInRange("meteoriteSize", 1, 0, 5);
            METEORITE_CHANCE = b.comment("The chance a meteroite will spawn in the Overworld. Higher numbers increase rarity.")
                    .defineInRange("meteoriteChance", 75, 0, Integer.MAX_VALUE);
            SOUL_SANDSTONE_LAYER_THICKNESS = b.comment("The number of blocks that Soul Sandstone will generate up or down from Soul Sand.")
                    .defineInRange("soulSandstoneLayerThickness", 3, 0, Integer.MAX_VALUE);
            BIG_METEORITE_CHANCE = b.comment("The chance a meteroite will be big.")
                    .defineInRange("meteoriteBigChance", 0.25, 0.00, 1.00);
            b.pop();

            b.comment("Settings for stone layering").push("layers");
            LAYER_GEN = b.comment("Determines how stone layers generate. 0 means disabled, 1 means they replace minecraft:stone, 2 means they replace #minecraft:base_stone_overworld, 3 means they replace any non-Rankine stone in #minecraft:base_stone_overworld")
                    .defineInRange("layerGenType", 3, 0, 3);
            LAYER_BEND = b.comment("Determines the vertical spread of stone layers. 1.0 is flat, closer to 0.0 is more extreme, 0.0 will crash.")
                    .defineInRange("layerWidth", 0.05D, 0.0D, 1.0D);
            LAYER_THICKNESS = b.comment("")
                    .defineInRange("layerThickness", 23, 1, Integer.MAX_VALUE);
            NOISE_SCALE = b.comment("This determines how wide stone layers generate. Smaller values means it will look more like bedrock. Default value is 125.")
                    .defineInRange("noiseScale", 100, 1, Integer.MAX_VALUE);
            NETHERRACK_LAYER_THICKNESS = b.comment("The number of layers of netherrack to keep on top of stones in Warped and Crimson Forests.")
                    .defineInRange("netherrackLayerThickness", 3, 0, Integer.MAX_VALUE);

            // NOISE_OFFSET = b.comment("This determines how close the overlap of noise layers is. A value of 0 means all layers are shaped identically.")
            //        .defineInRange("noiseOffset", 0, 0, Integer.MAX_VALUE);
            b.pop();

            /*
            b.comment("Settings for stone columns").push("columns");
            COLUMN_GEN = b.comment("Enables the generation of stone columns.")
                    .define("generateColumns",true);
            COLUMN_CHANCE = b.comment("Determines the chance per x,z coordinate for columns to generate.")
                    .defineInRange("columnChance", 0.15D, 0.0D, 1.0D);
            COLUMN_FREQUENCY = b.comment("The chance for columns to generate as full columns instead of stalactites.")
                    .defineInRange("columnFrequency", 0.1D, 0.0D, 1.0D);
            b.pop();

             */

            b.comment("Settings for intrusions").push("intrusions");
            INTRUSION_GEN = b.comment("Enables the generation of intrusions.")
                    .define("generateIntrusions",true);
            OVERWORLD_INTRUSION_RADIUS = b.comment("Size of an intrusion")
                    .defineInRange("overworldIntrusionRadius", 2, 0, 8);
            OVERWORLD_INTRUSION_SHRINK = b.comment("Chance for an overworld intrusion to shrink as it goes up. Values closer to 0 result in longer intrusions")
                    .defineInRange("overworldIntrusionShrink", 0.05D, 0.0D, 1.0D);
            OVERWORLD_INTRUSION_SHIFT = b.comment("Chance for an overworld intrusion to shift as it goes up. Values closer to 0 result in straighter intrusions")
                    .defineInRange("overworldIntrusionShift", 0.08D, 0.0D, 1.0D);
            INTRUSION_CINNABAR_ORE = b.comment("Chance for an overworld intrusion to contain cinnabar ore. Separate from the ore defined in the WorldGen settings.")
                    .defineInRange("intrusionCinnabar", 0.065D, 0.0D, 1.0D);

            NETHER_INTRUSION_RADIUS = b.comment("Maximum radius of an intrusion")
                    .defineInRange("netherIntrusionRadius", 2, 0, 8);
            NETHER_INTRUSION_SHRINK = b.comment("Chance for an nether intrusion to shift as it goes up. Values closer to 0 result in straighter intrusions")
                    .defineInRange("netherIntrusionShrink", 0.02D, 0.0D, 1.0D);
            NETHER_INTRUSION_SHIFT = b.comment("Chance for an overworld intrusion to shift as it goes up. Values closer to 0 result in straighter intrusions")
                    .defineInRange("netherIntrusionShift", 0.10D, 0.0D, 1.0D);
            b.pop();
        }
    }

    public static class BiomeGen {
        public final ForgeConfigSpec.ConfigValue<List<? extends List<Object>>> BIOME_SETTINGS;
        private static final List<List<Object>> biomeSettings = new ArrayList<>();

        public final ForgeConfigSpec.ConfigValue<List<? extends List<Object>>> ORE_SETTINGS;
        private static final List<List<Object>> oreSettings = new ArrayList<>();

        public BiomeGen(ForgeConfigSpec.Builder b) {
            if (TwilightForest.isInstalled()) {
                biomeSettings.add(List.of("twilightforest:oak_savannah",
                        List.of("rankine:silty_loam_grass_block","rankine:silty_loam","rankine:silty_clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:norite|20|rankine:magnetite_ore|0.08","rankine:norite|5|rankine:chromite_ore|0.08","rankine:granodiorite|10|rankine:magnetite_ore|0.08","rankine:granodiorite|5|rankine:wolframite_ore|0.08"),
                        List.of("rankine:rose_marble","rankine:rhyolite","rankine:graywacke"),
                        List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "rankine:light_gravel",
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:swamp",
                        List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:humus_grass_block","rankine:humus","rankine:clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:shonkinite|20|rankine:magnetite_ore|0.08","rankine:shonkinite|5|rankine:plumbago_ore|0.06","rankine:gray_granite|10|rankine:cassiterite_ore|0.08","rankine:gray_granite|10|rankine:malachite_ore|0.08"),
                        List.of("rankine:phonolite","rankine:phyllite","rankine:mudstone"),
                        List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:stinging_nettle|1"),
                        "rankine:dark_gravel",
                        "minecraft:air",
                        "minecraft:air",
                        "rankine:pointed_feric_dripstone"));
                biomeSettings.add(List.of("twilightforest:fire_swamp",
                        List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:humus_grass_block","rankine:humus","rankine:clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:shonkinite|20|rankine:magnetite_ore|0.08","rankine:shonkinite|5|rankine:plumbago_ore|0.06","rankine:gray_granite|10|rankine:cassiterite_ore|0.08","rankine:gray_granite|10|rankine:malachite_ore|0.08"),
                        List.of("rankine:purple_porphyry","rankine:komatiite","rankine:gray_granite"),
                        List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:stinging_nettle|1"),
                        "rankine:dark_gravel",
                        "minecraft:air",
                        "minecraft:air",
                        "rankine:pointed_feric_dripstone"));
                biomeSettings.add(List.of("twilightforest:dense_forest",
                        List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","minecraft:granite|10|rankine:cassiterite_ore|0.08","minecraft:granite|15|rankine:malachite_ore|0.08","rankine:granodiorite|15|rankine:magnetite_ore|0.08","rankine:granodiorite|5|rankine:wolframite_ore|0.06"),
                        List.of("rankine:black_dacite","rankine:black_marble","rankine:anorthosite"),
                        List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:forest",
                        List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","minecraft:granite|10|rankine:cassiterite_ore|0.08","minecraft:granite|15|rankine:malachite_ore|0.08","rankine:granodiorite|15|rankine:magnetite_ore|0.08","rankine:granodiorite|5|rankine:wolframite_ore|0.06"),
                        List.of("rankine:comendite","rankine:soapstone","rankine:limestone"),
                        List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:dark_forest",
                        List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:diabase|10|rankine:baddeleyite_ore|0.08","rankine:pegmatite|5|rankine:coltan_ore|0.06","rankine:pegmatite|5|rankine:beryl_ore|0.06","rankine:pegmatite|5|rankine:uraninite_ore|0.06","rankine:pegmatite|10|rankine:petalite_ore|0.08"),
                        List.of("rankine:blueschist","rankine:gabbro","rankine:tholeiitic_basalt"),
                        List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "rankine:dark_gravel",
                        "rankine:black_sand",
                        "rankine:black_sandstone",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:dark_forest_center",
                        List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:diabase|10|rankine:baddeleyite_ore|0.08","rankine:pegmatite|5|rankine:coltan_ore|0.06","rankine:pegmatite|5|rankine:beryl_ore|0.06","rankine:pegmatite|5|rankine:uraninite_ore|0.06","rankine:pegmatite|10|rankine:petalite_ore|0.08"),
                        List.of("rankine:blueschist","rankine:gabbro","rankine:tholeiitic_basalt"),
                        List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "rankine:dark_gravel",
                        "rankine:black_sand",
                        "rankine:black_sandstone",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:firefly_forest",
                        List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","minecraft:granite|10|rankine:cassiterite_ore|0.08","minecraft:granite|15|rankine:malachite_ore|0.08","rankine:granodiorite|15|rankine:magnetite_ore|0.08","rankine:granodiorite|5|rankine:wolframite_ore|0.1"),
                        List.of("minecraft:deepslate","rankine:nepheline_syenite","rankine:gray_marble"),
                        List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:firefly_forest",
                        List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","minecraft:granite|10|rankine:cassiterite_ore|0.08","minecraft:granite|15|rankine:malachite_ore|0.08","rankine:granodiorite|15|rankine:magnetite_ore|0.08","rankine:granodiorite|5|rankine:wolframite_ore|0.1"),
                        List.of("minecraft:ringwoodine","rankine:wadsleyone","rankine:greenschist"),
                        List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:stream",
                        List.of("rankine:alluvium","rankine:alluvium","rankine:alluvium","rankine:silty_clay_loam_grass_block","rankine:silty_clay_loam","rankine:silty_clay"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06"),
                        List.of("rankine:slate","rankine:anorthosite","rankine:chalk"),
                        List.of("rankine:short_grass|20","minecraft:grass|50","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "rankine:silt",
                        "minecraft:air",
                        "rankine:pointed_boracitic_dripstone"));
                biomeSettings.add(List.of("twilightforest:lake",
                        List.of("rankine:alluvium","rankine:alluvium","rankine:alluvium","rankine:silty_clay_loam_grass_block","rankine:silty_clay_loam","rankine:silty_clay"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06"),
                        List.of("rankine:slate","rankine:anorthosite","rankine:chalk"),
                        List.of("rankine:short_grass|20","minecraft:grass|50","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "rankine:silt",
                        "minecraft:air",
                        "rankine:pointed_boracitic_dripstone"));
                biomeSettings.add(List.of("twilightforest:snowy_forest",
                        List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:gray_granite|15|rankine:cassiterite_ore|0.08","rankine:gray_granite|15|rankine:malachite_ore|0.08","minecraft:diorite|15|rankine:magnetite_ore|0.08"),
                        List.of("rankine:mica_schist","rankine:white_marble","rankine:dolostone"),
                        List.of("rankine:short_grass|70","minecraft:fern|10","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:glacier",
                        List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:gray_granite|15|rankine:cassiterite_ore|0.08","rankine:gray_granite|15|rankine:malachite_ore|0.08","minecraft:diorite|15|rankine:magnetite_ore|0.08"),
                        List.of("rankine:mica_schist","rankine:white_marble","rankine:dolostone"),
                        List.of("rankine:short_grass|70","minecraft:fern|10","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:mushroom_forest",
                        List.of("rankine:humus_grass_block","rankine:humus","rankine:clay_loam","rankine:clay_loam_grass_block","rankine:clay_loam","rankine:clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:diabase|10|rankine:baddeleyite_ore|0.08","rankine:red_porphyry|10|rankine:porphyry_copper|0.1","rankine:pegmatite|5|rankine:coltan_ore|0.06","rankine:pegmatite|5|rankine:beryl_ore|0.06","rankine:pegmatite|5|rankine:uraninite_ore|0.06","rankine:pegmatite|10|rankine:petalite_ore|0.08"),
                        List.of("rankine:wehrlite","rankine:troctolite","rankine:marlstone"),
                        List.of("rankine:short_grass|70","minecraft:red_mushroom|10","minecraft:brown_mushroom|10","rankine:crimson_clover|10"),
                        "rankine:light_gravel",
                        "minecraft:air",
                        "minecraft:air",
                        "rankine:pointed_halitic_dripstone"));
                biomeSettings.add(List.of("twilightforest:dense_mushroom_forest",
                        List.of("rankine:humus_grass_block","rankine:humus","rankine:clay_loam","rankine:clay_loam_grass_block","rankine:clay_loam","rankine:clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:diabase|10|rankine:baddeleyite_ore|0.08","rankine:red_porphyry|10|rankine:porphyry_copper|0.1","rankine:pegmatite|5|rankine:coltan_ore|0.06","rankine:pegmatite|5|rankine:beryl_ore|0.06","rankine:pegmatite|5|rankine:uraninite_ore|0.06","rankine:pegmatite|10|rankine:petalite_ore|0.08"),
                        List.of("rankine:wehrlite","rankine:troctolite","rankine:marlstone"),
                        List.of("rankine:short_grass|70","minecraft:red_mushroom|10","minecraft:brown_mushroom|10","rankine:crimson_clover|10"),
                        "rankine:light_gravel",
                        "minecraft:air",
                        "minecraft:air",
                        "rankine:pointed_halitic_dripstone"));
                biomeSettings.add(List.of("twilightforest:clearing",
                        List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:sandy_clay_loam_grass_block","rankine:sandy_clay_loam","rankine:clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","minecraft:granite|15|rankine:cassiterite_ore|0.08","minecraft:granite|10|rankine:malachite_ore|0.08","rankine:granodiorite|15|rankine:magnetite_ore|0.08","rankine:granodiorite|5|rankine:wolframite_ore|0.06"),
                        List.of("rankine:red_dacite","rankine:rhyolite","rankine:siltstone"),
                        List.of("rankine:short_grass|70","rankine:crimson_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air"));
                biomeSettings.add(List.of("twilightforest:spooky_forest",
                        List.of("rankine:humus_grass_block","rankine:humus","rankine:clay_loam","rankine:clay_loam_grass_block","rankine:clay_loam","rankine:clay_loam"),
                        List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:diabase|10|rankine:baddeleyite_ore|0.08","rankine:red_porphyry|10|rankine:porphyry_copper|0.1","rankine:pegmatite|5|rankine:coltan_ore|0.06","rankine:pegmatite|5|rankine:beryl_ore|0.06","rankine:pegmatite|5|rankine:uraninite_ore|0.06","rankine:pegmatite|10|rankine:petalite_ore|0.08"),
                        List.of("rankine:episyenite","rankine:rose_marble","rankine:arkose"),
                        List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "rankine:light_gravel",
                        "minecraft:air",
                        "minecraft:air",
                        "rankine:pointed_halitic_dripstone"));
                biomeSettings.add(List.of("twilightforest:thornlands",
                        List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|15|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:shonkinite|20|rankine:beryl_ore|0.08","minecraft:diorite|5|rankine:ilmenite_ore|0.08","minecraft:diorite|10|rankine:magnetite_ore|0.08","rankine:gray_granite|10|rankine:cassiterite_ore|0.08","rankine:gray_granite|5|rankine:malachite_ore|0.08"),
                        List.of("rankine:serpentinite","rankine:mariposite","rankine:gneiss","rankine:hornblende_andesite"),
                        List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "rankine:pointed_zirconic_dripstone"));
                biomeSettings.add(List.of("twilightforest:highlands",
                        List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|15|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:shonkinite|20|rankine:beryl_ore|0.08","minecraft:diorite|5|rankine:ilmenite_ore|0.08","minecraft:diorite|10|rankine:magnetite_ore|0.08","rankine:gray_granite|10|rankine:cassiterite_ore|0.08","rankine:gray_granite|5|rankine:malachite_ore|0.08"),
                        List.of("rankine:serpentinite","rankine:mariposite","rankine:gneiss","rankine:hornblende_andesite"),
                        List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "rankine:pointed_zirconic_dripstone"));
                biomeSettings.add(List.of("twilightforest:final_plateau",
                        List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                        List.of("minecraft:air|15|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.06","rankine:shonkinite|20|rankine:beryl_ore|0.08","minecraft:diorite|5|rankine:ilmenite_ore|0.08","minecraft:diorite|10|rankine:magnetite_ore|0.08","rankine:gray_granite|10|rankine:cassiterite_ore|0.08","rankine:gray_granite|5|rankine:malachite_ore|0.08"),
                        List.of("rankine:serpentinite","rankine:mariposite","rankine:gneiss","rankine:hornblende_andesite"),
                        List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                        "minecraft:air",
                        "minecraft:air",
                        "minecraft:air",
                        "rankine:pointed_zirconic_dripstone"));
            }
            biomeSettings.add(List.of("minecraft:soul_sand_valley",
                    List.of(),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:scoria|1|rankine:scoria|0.0","rankine:pumice|1|rankine:pumice|0.0"),
                    List.of("rankine:blueschist","rankine:blueschist","rankine:blueschist","rankine:blueschist","rankine:honeystone","rankine:honeystone","rankine:honeystone","rankine:wehrlite"),
                    List.of(),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of("minecraft:basalt_deltas",
                    List.of(),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:scoria|1|rankine:scoria|0.0","rankine:pumice|1|rankine:pumice|0.0"),
                    List.of("minecraft:blackstone","minecraft:blackstone","minecraft:blackstone","minecraft:blackstone","minecraft:basalt","minecraft:basalt","minecraft:basalt","rankine:dunite"),
                    List.of(),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of("minecraft:crimson_forest",
                    List.of(),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:scoria|1|rankine:scoria|0.0","rankine:pumice|1|rankine:pumice|0.0"),
                    List.of("rankine:purple_porphyry","rankine:purple_porphyry","rankine:purple_porphyry","rankine:purple_porphyry","rankine:komatiite","rankine:komatiite","rankine:komatiite","rankine:red_porphyry"),
                    List.of(),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of("minecraft:warped_forest",
                    List.of(),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:scoria|1|rankine:scoria|0.0","rankine:pumice|1|rankine:pumice|0.0"),
                    List.of("rankine:lherzolite","rankine:lherzolite","rankine:lherzolite","rankine:lherzolite","rankine:pyroxenite","rankine:pyroxenite","rankine:pyroxenite","rankine:harzburgite"),
                    List.of(),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of("minecraft:nether_wastes",
                    List.of(),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:scoria|1|rankine:scoria|0.0","rankine:pumice|1|rankine:pumice|0.0"),
                    List.of("minecraft:netherrack"),
                    List.of(),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            /*biomeSettings.add(List.of(BiomeTags.IS_NETHER.getName(),
                    List.of(),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:scoria|1|rankine:scoria|0.0","rankine:pumice|1|rankine:pumice|0.0"),
                    List.of("minecraft:netherrack"),
                    List.of(),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of("minecraft:stony_shore",
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:sandy_clay_loam_grass_block","rankine:sandy_clay_loam","rankine:sandy_clay"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:diabase|30|rankine:baddeleyite_ore|0.04","rankine:red_porphyry|10|rankine:porphyry_copper|0.04"),
                    List.of("rankine:wadsleyone","rankine:troctolite","rankine:gabbro","rankine:tholeiitic_basalt","rankine:chalk","rankine:slate","rankine:shale"),
                    List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:stinging_nettle|1"),
                    "rankine:light_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_boracitic_dripstone"));
            biomeSettings.add(List.of("minecraft:grove",
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:shonkinite|20|rankine:beryl_ore|0.04","minecraft:diorite|5|rankine:ilmenite_ore|0.04","minecraft:diorite|10|rankine:magnetite_ore|0.04","rankine:gray_granite|10|rankine:cassiterite_ore|0.04","rankine:gray_granite|5|rankine:malachite_ore|0.04"),
                    List.of("rankine:ringwoodine","rankine:serpentinite","rankine:greenschist","rankine:mariposite","rankine:gray_marble","rankine:dolostone","rankine:gneiss","rankine:anorthosite","rankine:hornblende_andesite"),
                    List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_zirconic_dripstone"));
            biomeSettings.add(List.of("minecraft:windswept_forest",
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:shonkinite|20|rankine:beryl_ore|0.04","minecraft:diorite|5|rankine:ilmenite_ore|0.04","minecraft:diorite|10|rankine:magnetite_ore|0.04","rankine:gray_granite|10|rankine:cassiterite_ore|0.04","rankine:gray_granite|5|rankine:malachite_ore|0.04"),
                    List.of("rankine:ringwoodine","rankine:serpentinite","rankine:greenschist","rankine:mariposite","rankine:gray_marble","rankine:dolostone","rankine:gneiss","rankine:anorthosite","rankine:hornblende_andesite"),
                    List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_zirconic_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.THEEND.getName(),
                    List.of(),
                    List.of(),
                    List.of("minecraft:end_stone"),
                    List.of(),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of(Biome.BiomeCategory.NONE.getName(),
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:sandy_clay_loam_grass_block","rankine:sandy_clay_loam","rankine:sandy_clay"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:diabase|30|rankine:baddeleyite_ore|0.04","rankine:red_porphyry|10|rankine:porphyry_copper|0.04"),
                    List.of("rankine:wadsleyone","rankine:troctolite","rankine:gabbro","rankine:slate","rankine:shale","rankine:tholeiitic_basalt"),
                    List.of("rankine:short_grass|70","minecraft:grass|10"),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of(Biome.BiomeCategory.MUSHROOM.getName(),
                    List.of("rankine:humus_grass_block","rankine:humus","rankine:clay_loam","rankine:clay_loam_grass_block","rankine:clay_loam","rankine:clay_loam"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:diabase|30|rankine:baddeleyite_ore|0.04","rankine:red_porphyry|10|rankine:porphyry_copper|0.04","rankine:pegmatite|1|rankine:coltan_ore|0.03","rankine:pegmatite|1|rankine:beryl_ore|0.03","rankine:pegmatite|1|rankine:uraninite_ore|0.03","rankine:pegmatite|5|rankine:petalite_ore|0.04"),
                    List.of("rankine:wadsleyone","rankine:troctolite","rankine:gabbro","rankine:tholeiitic_basalt","rankine:marlstone"),
                    List.of("rankine:short_grass|70","minecraft:red_mushroom|10","minecraft:brown_mushroom|10","rankine:crimson_clover|10"),
                    "rankine:light_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_halitic_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.OCEAN.getName(),
                    List.of("rankine:sandy_clay_loam_grass_block","rankine:sandy_clay_loam","rankine:sandy_clay","rankine:silty_clay_loam_grass_block","rankine:silty_clay_loam","rankine:silty_clay"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:diabase|30|rankine:baddeleyite_ore|0.04","rankine:red_porphyry|10|rankine:porphyry_copper|0.04"),
                    List.of("rankine:wadsleyone","rankine:troctolite","rankine:gabbro","rankine:tholeiitic_basalt"),
                    List.of("rankine:short_grass|70","minecraft:grass|10"),
                    "rankine:light_gravel",
                    "rankine:silt",
                    "minecraft:air",
                    "rankine:pointed_halitic_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.BEACH.getName(),
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:sandy_clay_loam_grass_block","rankine:sandy_clay_loam","rankine:sandy_clay"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:syenite|30|rankine:baddeleyite_ore|0.04","rankine:nepheline_syenite|10|rankine:magnetite|0.03"),
                    List.of("rankine:wadsleyone","rankine:troctolite","rankine:gabbro","rankine:tholeiitic_basalt","rankine:chalk","rankine:shale","rankine:marlstone"),
                    List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:stinging_nettle|1"),
                    "rankine:light_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_boracitic_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.RIVER.getName(),
                    List.of("rankine:alluvium","rankine:alluvium","rankine:alluvium","rankine:silty_clay_loam_grass_block","rankine:silty_clay_loam","rankine:silty_clay"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|5|rankine:kimberlitic_diamond_ore|0.03"),
                    List.of("rankine:post_perovskite","rankine:troctolite","rankine:gabbro","rankine:tholeiitic_basalt","rankine:chalk","rankine:shale","rankine:shale","rankine:anorthosite"),
                    List.of("rankine:short_grass|20","minecraft:grass|50","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "rankine:silt",
                    "minecraft:air",
                    "rankine:pointed_boracitic_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.SAVANNA.getName(),
                    List.of("rankine:silty_loam_grass_block","rankine:silty_loam","rankine:silty_clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:norite|20|rankine:magnetite_ore|0.04","rankine:norite|5|rankine:chromite_ore|0.04","rankine:granodiorite|10|rankine:magnetite_ore|0.04","rankine:granodiorite|5|rankine:wolframite_ore|0.04"),
                    List.of("rankine:bridgmanham","rankine:rose_marble","rankine:red_dacite","rankine:rhyolite","rankine:quartzite","rankine:siltstone","rankine:graywacke"),
                    List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "rankine:light_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of(Biome.BiomeCategory.DESERT.getName(),
                    List.of("rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:loamy_sand","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:loamy_sand"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:red_porphyry|20|rankine:porphyry_copper|0.05","rankine:red_porphyry|10|rankine:native_gold_ore|0.04","rankine:red_porphyry|3|rankine:molybdenum_ore|0.03","rankine:pegmatite|2|rankine:baddeleyite_ore|0.03","rankine:pegmatite|1|rankine:coltan_ore|0.03","rankine:pegmatite|1|rankine:beryl_ore|0.03","rankine:pegmatite|1|rankine:uraninite_ore|0.03","rankine:pegmatite|5|rankine:petalite_ore|0.04"),
                    List.of("rankine:bridgmanham","rankine:rose_marble","rankine:red_dacite","rankine:rhyolite","rankine:quartzite","rankine:siltstone","rankine:itacolumite"),
                    List.of("rankine:short_grass|30","minecraft:dead_bush|10","rankine:stinging_nettle|1"),
                    "rankine:light_gravel",
                    "rankine:desert_sand",
                    "rankine:desert_sandstone",
                    "rankine:pointed_nitric_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.EXTREME_HILLS.getName(),
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:shonkinite|20|rankine:beryl_ore|0.04","minecraft:diorite|5|rankine:ilmenite_ore|0.04","minecraft:diorite|10|rankine:magnetite_ore|0.04","rankine:gray_granite|10|rankine:cassiterite_ore|0.04","rankine:gray_granite|5|rankine:malachite_ore|0.04"),
                    List.of("rankine:ringwoodine","rankine:serpentinite","rankine:greenschist","rankine:mariposite","rankine:gray_marble","rankine:dolostone","rankine:gneiss","rankine:anorthosite","rankine:hornblende_andesite"),
                    List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_zirconic_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.MOUNTAIN.getName(),
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                    List.of("minecraft:air|20|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:shonkinite|20|rankine:beryl_ore|0.04","minecraft:diorite|5|rankine:ilmenite_ore|0.04","minecraft:diorite|10|rankine:magnetite_ore|0.04","rankine:gray_granite|10|rankine:cassiterite_ore|0.04","rankine:gray_granite|5|rankine:malachite_ore|0.04"),
                    List.of("rankine:ringwoodine","rankine:serpentinite","rankine:greenschist","rankine:mariposite","rankine:gray_marble","rankine:dolostone","rankine:gneiss","rankine:anorthosite","rankine:hornblende_andesite"),
                    List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_zirconic_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.FOREST.getName(),
                    List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","minecraft:granite|10|rankine:cassiterite_ore|0.04","minecraft:granite|15|rankine:malachite_ore|0.04","rankine:granodiorite|15|rankine:magnetite_ore|0.04","rankine:granodiorite|5|rankine:wolframite_ore|0.03"),
                    List.of("minecraft:deepslate","rankine:black_dacite","rankine:comendite","rankine:black_marble","rankine:soapstone","rankine:anorthosite","rankine:limestone"),
                    List.of("rankine:short_grass|70","rankine:yellow_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of(Biome.BiomeCategory.TAIGA.getName(),
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:loamy_sand_grass_block","rankine:loamy_sand","rankine:sandy_clay_loam"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:gray_granite|15|rankine:cassiterite_ore|0.04","rankine:gray_granite|15|rankine:malachite_ore|0.04","minecraft:diorite|15|rankine:magnetite_ore|0.04"),
                    List.of("rankine:sommanite","rankine:black_dacite","rankine:comendite","rankine:white_marble","rankine:soapstone","rankine:anorthosite","rankine:dolostone"),
                    List.of("rankine:short_grass|70","minecraft:fern|10","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of(Biome.BiomeCategory.PLAINS.getName(),
                    List.of("rankine:sandy_loam_grass_block","rankine:sandy_loam","rankine:sandy_clay_loam","rankine:sandy_clay_loam_grass_block","rankine:sandy_clay_loam","rankine:clay_loam"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","minecraft:granite|15|rankine:cassiterite_ore|0.04","minecraft:granite|10|rankine:malachite_ore|0.04","rankine:granodiorite|15|rankine:magnetite_ore|0.04","rankine:granodiorite|5|rankine:wolframite_ore|0.03"),
                    List.of("rankine:bridgmanham","rankine:red_dacite","rankine:rhyolite","rankine:gray_marble","rankine:anorthosite","rankine:limestone","rankine:siltstone"),
                    List.of("rankine:short_grass|70","rankine:crimson_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of(Biome.BiomeCategory.ICY.getName(),
                    List.of("rankine:silty_loam_grass_block","rankine:silty_loam","rankine:permafrost","rankine:silty_clay_loam_grass_block","rankine:silty_clay_loam","rankine:permafrost"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","minecraft:diorite|5|rankine:plumbago_ore|0.03","minecraft:diorite|5|rankine:ilmenite_ore|0.04","minecraft:diorite|10|rankine:magnetite_ore|0.04","rankine:gray_granite|10|rankine:cassiterite_ore|0.04","rankine:pegmatite|2|rankine:baddeleyite_ore|0.03","rankine:pegmatite|1|rankine:coltan_ore|0.03","rankine:pegmatite|1|rankine:beryl_ore|0.03","rankine:pegmatite|1|rankine:uraninite_ore|0.03","rankine:pegmatite|10|rankine:petalite_ore|0.04"),
                    List.of("rankine:whiteschist","rankine:white_marble","rankine:comendite","rankine:mica_schist","rankine:phyllite","minecraft:andesite","rankine:chalk"),
                    List.of("rankine:short_grass|70","minecraft:fern|10","rankine:red_clover|10","rankine:white_clover|10","rankine:stinging_nettle|1"),
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air",
                    "minecraft:air"));
            biomeSettings.add(List.of(Biome.BiomeCategory.JUNGLE.getName(),
                    List.of("rankine:humus_grass_block","rankine:humus","rankine:laterite","rankine:clay_loam_grass_block","rankine:clay_loam","rankine:laterite"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:norite|20|rankine:magnetite_ore|0.04","rankine:norite|5|rankine:chromite_ore|0.04","rankine:pegmatite|4|rankine:baddeleyite_ore|0.03","rankine:pegmatite|2|rankine:coltan_ore|0.03","rankine:pegmatite|2|rankine:beryl_ore|0.03","rankine:pegmatite|2|rankine:uraninite_ore|0.03","rankine:pegmatite|15|rankine:petalite_ore|0.04"),
                    List.of("minecraft:deepslate","rankine:eclogite","rankine:gneiss","rankine:mica_schist","rankine:phyllite","rankine:slate","rankine:mudstone"),
                    List.of("rankine:short_grass|70","rankine:crimson_clover|10","rankine:yellow_clover|10","rankine:stinging_nettle|1"),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_magnesitic_dripstone"));
            biomeSettings.add(List.of(Biome.BiomeCategory.SWAMP.getName(),
                    List.of("rankine:loam_grass_block","rankine:loam","rankine:clay_loam","rankine:humus_grass_block","rankine:humus","rankine:clay_loam"),
                    List.of("minecraft:air|30|minecraft:air|0.0","rankine:kimberlite|10|rankine:kimberlitic_diamond_ore|0.03","rankine:shonkinite|20|rankine:magnetite_ore|0.04","rankine:shonkinite|5|rankine:plumbago_ore|0.03","rankine:gray_granite|10|rankine:cassiterite_ore|0.04","rankine:gray_granite|10|rankine:malachite_ore|0.04"),
                    List.of("rankine:post_perovskite","rankine:eclogite","rankine:gneiss","rankine:mica_schist","rankine:phyllite","rankine:slate","rankine:mudstone"),
                    List.of("rankine:short_grass|70","rankine:red_clover|10","rankine:stinging_nettle|1"),
                    "rankine:dark_gravel",
                    "minecraft:air",
                    "minecraft:air",
                    "rankine:pointed_feric_dripstone"));*/


            b.comment("Biome Feature Settings").push("biomeGen");
            BIOME_SETTINGS = b.comment("Custom generations per biome or biome category. The defaults are created with biome categories for the overworld. Specific biomes can be used and should be put first in the list.",
                    "Syntax: [[List1], [List2], [List3], ...]",
                    "   [ListX]: [Biome, [Soils], [Intrusions], [Layers], [Vegetation], Gravel, Sand, Sandstone, Pointed Dripstone]",
                    "   Biome: biome resource location or category (ex: \"minecraft:nether_wastes\" or \"jungle\")",
                    "   [Soils]: O1, A1, B1, O2, A2, B2",
                    "       O1: resource location of the primary block to replace grass (ex: \"rankine:loam_grass_block\")",
                    "       A1: resource location of the primary block to replace dirt (ex: \"rankine:loam\")",
                    "       B1: resource location of the primary block to generate under dirt (ex: \"rankine:loam\")",
                    "       O2: resource location of the secondary block to replace grass (ex: \"rankine:loam_grass_block\")",
                    "       A2: resource location of the secondary block to replace dirt (ex: \"rankine:loam\")",
                    "       B2: resource location of the secondary block to generate under dirt (ex: \"rankine:loam\")",
                    "   [Intrusions]: [Block|Weight|Ore|Chance]",
                    "       Block: resource location of block to generate as an intrusion (ex: \"rankine:pegmatite\". Use \"minecraft:air\" to not generate an intrusion)",
                    "       Weight: weight of the intrusion to generate (ex: \"5\")",
                    "       Ore: resource location of an block to generate in an intrusion (ex: \"rankine:magnetite_ore\")",
                    "       Chance: chance for an ore block to replace an intrusion block (ex: \"0.03\")",
                    "   [Layers]: Rock1, Rock2, Rock3, ...",
                    "       RockX: resource locations of the blocks to use in stone layers. From bottom to top.",
                    "   [Vegetation]: [Block|Weight]",
                    "       Block: resource location of block to grow above grassy soils (ex: \"rankine:stinging_nettle\")",
                    "       Weight: weight of the block to generate (ex: \"2\")"
            ).defineList("biomeSettings", biomeSettings, (p) -> p instanceof List);
            b.pop();



///fill ~ ~ ~ ~30 ~-30 ~30 air replace #rankine:world_strip
            //lower natives
            oreSettings.add(List.of("rankine:stibnite_ore", List.of("overworld"), "default", "uniform", 50, 85, 4, 1.0D, 11, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_tin_ore", List.of("overworld"), "default", "uniform", 50, 85, 4, 1.0D, 14, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_lead_ore", List.of("overworld"), "default", "uniform", 50, 85, 4, 1.0D, 10, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_silver_ore", List.of("overworld"), "default", "uniform", 50, 85, 4, 1.0D, 8, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_bismuth_ore", List.of("overworld"), "default", "uniform", 50, 85, 4, 1.0D, 8, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:lignite_ore", List.of("overworld"), "default", "uniform", 50, 85, 15, 0.8D, 4, 1.0, 0.0D));
            //upper natives
            oreSettings.add(List.of("rankine:stibnite_ore", List.of("overworld"), "default", "triangle", 50, 250, 5, 1.0D, 22, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_tin_ore", List.of("overworld"), "default", "triangle", 50, 250, 5, 1.0D, 28, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_lead_ore", List.of("overworld"), "default", "triangle", 50, 250, 5, 1.0D, 20, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_silver_ore", List.of("overworld"), "default", "triangle", 50, 250, 5, 1.0D, 16, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_bismuth_ore", List.of("overworld"), "default", "triangle", 50, 250, 5, 1.0D, 16, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:lignite_ore", List.of("overworld"), "default", "triangle", 50, 250, 10, 0.8D, 30, 1.0, 0.0D));

            oreSettings.add(List.of("rankine:native_gold_ore", List.of("overworld"), "default", "triangle", -63, 63, 6, 1.0D, 12, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_gold_ore", List.of("mesa"), "default", "triangle", 20, 120, 6, 1.0D, 12, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:pyrite_ore", List.of("overworld"), "default", "triangle", -40, 80, 4, 1.0D, 10, 1.0, 0.0D));

            oreSettings.add(List.of("rankine:subbituminous_ore", List.of("overworld"), "sphere", "uniform", 20, 60, 3, 0.5D, 1, 0.5, 0.0D));
            oreSettings.add(List.of("rankine:bituminous_ore", List.of("overworld"), "sphere", "uniform", -30, 30, 3, 0.5D, 1, 0.3, 0.0D));
            oreSettings.add(List.of("rankine:anthracite_ore", List.of("overworld"), "sphere", "uniform", -64, -20, 3, 0.5D, 1, 0.2, 0.0D));


            oreSettings.add(List.of("rankine:hematite_ore", List.of("overworld"), "sphere", "uniform", -10, 80, 3, 0.4D, 2, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:hematite_ore", List.of("overworld"), "default", "triangle", 64, 384, 3, 1.0D, 50, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:chalcocite_ore", List.of("overworld"), "sphere", "uniform", 0, 96, 3, 0.6D, 1, 0.4, 0.0D));
            oreSettings.add(List.of("rankine:chalcocite_ore", List.of("overworld"), "default", "triangle", 0, 96, 3, 1.0D, 30, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:magnesite_ore", List.of("overworld"), "sphere", "uniform", -30, 20, 3, 0.3D, 1, 0.5, 0.0D));
            oreSettings.add(List.of("rankine:magnesite_ore", List.of("overworld"), "default", "triangle", -64, 30, 3, 1.0D, 25, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:pyrolusite_ore", List.of("overworld"), "sphere", "uniform", -30, 10, 4, 0.3D, 1, 0.2D, 0.0D));
            oreSettings.add(List.of("rankine:pyrolusite_ore", List.of("overworld"), "default", "triangle", -96, 10, 3, 1.0D, 25, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:pentlandite_ore", List.of("ocean","beach","mushroom","desert","mesa"), "sphere", "uniform", -30, 20, 3, 0.3D, 1, 0.5, 0.0D));
            oreSettings.add(List.of("rankine:pentlandite_ore", List.of("overworld"), "default", "triangle", -40, 40, 3, 1.0D, 20, 1.0, 0.0D));

            oreSettings.add(List.of("rankine:sphalerite_ore", List.of("desert","mesa","savanna","plains"), "sphere", "uniform", 10, 60, 3, 0.6D, 1, 0.3, 0.0D));
            oreSettings.add(List.of("rankine:sphalerite_ore", List.of("overworld"), "default", "triangle", 0, 128, 3, 1.0D, 8, 1.0, 0.0D));

            oreSettings.add(List.of("rankine:cryolite_ore", List.of("extreme_hills","mountain","taiga","icy"), "sphere", "uniform", -60, -30, 3, 0.6D, 1, 0.3, 0.0D));
            oreSettings.add(List.of("rankine:bauxite_ore", List.of("desert","mesa","savanna","jungle","swamp","plains","forest","taiga"), "sphere", "uniform", 10, 40, 3, 0.2D, 1, 0.3, 0.0D));
            oreSettings.add(List.of("rankine:celestine_ore", List.of("jungle","swamp","plains","forest","taiga"), "sphere", "uniform", 10, 40, 3, 0.6D, 1, 0.1, 0.0D));

            oreSettings.add(List.of("rankine:cobaltite_ore", List.of("overworld"), "sphere", "uniform", -60, -35, 3, 0.2D, 1, 0.1, 0.0D));
            oreSettings.add(List.of("rankine:galena_ore", List.of("overworld"), "sphere", "uniform", -30, 10, 4, 0.2D, 1, 0.2D, 0.0D));
            oreSettings.add(List.of("rankine:acanthite_ore", List.of("overworld"), "sphere", "uniform", -30, 10, 4, 0.2D, 1, 0.2D, 0.0D));
            oreSettings.add(List.of("rankine:plumbago_ore", List.of("overworld"), "default", "triangle", -60, -20, 7, 1.0D, 3, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:lazurite_ore", List.of("overworld"), "sphere", "uniform", 0, 40, 3, 0.2D, 1, 1.0, 0.0D));

            //Rare ores
            oreSettings.add(List.of("rankine:sperrylite_ore", List.of("overworld"), "default", "triangle", -84, -40, 8, 1.0D, 1, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:ilmenite_ore", List.of("overworld"), "default", "triangle", -84, -40, 6, 1.0D, 1, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:wolframite_ore", List.of("overworld"), "default", "triangle", -84, -40, 6, 1.0D, 1, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:uraninite_ore", List.of("overworld"), "default", "triangle", -84, -40, 6, 1.0D, 1, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:molybdenite_ore", List.of("overworld"), "default", "triangle", -84, -40, 6, 1.0D, 1, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:coltan_ore", List.of("overworld"), "default", "triangle", -84, -40, 6, 1.0D, 1, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:chromite_ore", List.of("overworld"), "default", "triangle", -84, -40, 6, 1.0D, 1, 1.0, 0.0D));

            oreSettings.add(List.of("rankine:banded_iron_formation", List.of("overworld"), "disk", "uniform", 30, 70, 4, 0.6D, 1, 0.4, 0.0D));
            oreSettings.add(List.of("rankine:bog_iron", List.of("swamp","jungle"), "disk", "uniform", 30, 70, 4, 0.3D, 1, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:ironstone", List.of("desert","savanna","mesa"), "disk", "uniform", 30, 70, 4, 0.3D, 1, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:kaolin", List.of("swamp","jungle","mushroom"), "disk", "uniform", 40, 70, 4, 0.7D, 1, 0.4, 0.0D));
            oreSettings.add(List.of("rankine:kaolin", List.of("overworld"), "disk", "uniform", 40, 60, 3, 0.6D, 1, 0.7, 0.0D));
            oreSettings.add(List.of("rankine:sylvinite", List.of("beach","ocean","desert"), "default", "triangle", 20, 50, 3, 1.0D, 15, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:phosphorite", List.of("beach","ocean","desert"), "disk", "uniform", 30, 50, 4, 0.6D, 1, 0.4, 0.0D));
            oreSettings.add(List.of("rankine:phosphorite", List.of("extreme_hills","mountain"), "disk", "uniform", 70, 100, 4, 0.6D, 1, 0.5, 0.0D));
            oreSettings.add(List.of("rankine:snowflake_obsidian", List.of("overworld"), "disk", "uniform", -60, -45, 4, 1.0D, 1, 0.4, 0.0D));
            oreSettings.add(List.of("rankine:blood_obsidian", List.of("overworld"), "disk", "uniform", -60, -45, 4, 1.0D, 1, 0.4, 0.0D));

            //oreSettings.add(List.of("rankine:basaltic_tuff", List.of("ocean","beach","mushroom","none"), "disk", "uniform",20, 50, 6, 1.0D, 1, 0.4, 0.0D));
            //oreSettings.add(List.of("rankine:andesitic_tuff", List.of("extreme_hills"), "disk", "uniform",70, 100, 6, 1.0D, 1, 0.4, 0.0D));
            //oreSettings.add(List.of("rankine:rhyolitic_tuff", List.of("savanna","mesa","desert","plains"), "disk", "uniform",10, 40, 6, 1.0D, 1, 0.4, 0.0D));
            oreSettings.add(List.of("rankine:kimberlitic_tuff", List.of("all"), "disk", "uniform",-60, -45, 4, 1.0D, 1, 0.4, 0.0D));
            oreSettings.add(List.of("rankine:komatiitic_tuff", List.of("minecraft:nether_wastes"), "disk", "uniform",10, 30, 6, 1.0D, 1, 0.4, 0.0D));

            //Nether ores
            oreSettings.add(List.of("rankine:native_sulfur_ore", List.of("nether"), "default", "triangle", -30, 130, 5, 1.0D, 20, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_arsenic_ore", List.of("nether"), "default", "triangle", -30, 130, 5, 1.0D, 20, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:magnetite_ore", List.of("nether"), "sphere", "uniform",60, 120, 3, 0.6D, 1, 0.1, 0.0D));
            oreSettings.add(List.of("rankine:cobaltite_ore", List.of("nether"), "sphere", "uniform",60, 120, 3, 0.6D, 1, 0.1, 0.0D));
            oreSettings.add(List.of("rankine:wolframite_ore", List.of("nether"), "sphere", "uniform",60, 120, 3, 0.6D, 1, 0.1, 0.0D));
            oreSettings.add(List.of("rankine:ilmenite_ore", List.of("nether"), "sphere", "uniform",60, 120, 3, 0.6D, 1, 0.1, 0.0D));
            oreSettings.add(List.of("rankine:sperrylite_ore", List.of("nether"), "sphere", "uniform",90, 120, 3, 0.6D, 1, 0.1, 0.0D));
            oreSettings.add(List.of("rankine:coltan_ore", List.of("nether"), "sphere", "uniform",10, 40, 3, 0.5D, 1, 0.2, 0.0D));
            oreSettings.add(List.of("rankine:monazite_ore", List.of("nether"), "sphere", "uniform",10, 40, 3, 0.5D, 1, 0.2, 0.0D));
            oreSettings.add(List.of("rankine:interspinifex_ore", List.of("minecraft:crimson_forest"), "default", "triangle", 30, 90, 6, 1.0D, 15, 1.0, 0.0D));

            //End ores
            oreSettings.add(List.of("rankine:native_gallium_ore", List.of("the_end"), "default", "triangle",-50, 70, 4, 1.0D, 15, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_indium_ore", List.of("the_end"), "default", "triangle",-50, 70, 4, 1.0D, 15, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_selenium_ore", List.of("the_end"), "default", "triangle",-50, 70, 4, 1.0D, 15, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:native_tellurium_ore", List.of("the_end"), "default", "triangle",-50, 70, 4, 1.0D, 15, 1.0, 0.0D));
            oreSettings.add(List.of("rankine:molybdenite_ore", List.of("the_end"), "sphere", "uniform",10, 40, 3, 0.5D, 1, 0.3, 0.0D));
            oreSettings.add(List.of("rankine:uraninite_ore", List.of("the_end"), "sphere", "uniform",10, 40, 3, 0.5D, 1, 0.3, 0.0D));
            oreSettings.add(List.of("rankine:xenotime_ore", List.of("the_end"), "sphere", "uniform",10, 40, 3, 0.5D, 1, 0.3, 0.0D));
            oreSettings.add(List.of("rankine:greenockite_ore", List.of("the_end"), "sphere", "uniform",10, 40, 3, 0.5D, 1, 0.3, 0.0D));










            b.comment("Ore Feature Settings").push("oreGen");
            ORE_SETTINGS = b.comment("Ore Settings",
                    "[OreGen]: [Ore, [Biomes], Type, Min Height, Max Height, Size, Density, Count, spawnChance, discardChance]",
                    "   Ore: resource loacation of the block to generate",
                    "   [Biomes]: String list of biome resource locations to generate in. Use \"overworld\" to generate in all biomes. Can use biome categories by using the category name: ex \"ocean\".",
                    "   VeinType: String type of vein to generate. Options include \"default\" (works like vanilla veins), \"sphere\" (generates veins more radially, like an explosion), \"disk\" (like the sphere except flatter)",
                    "   DistributionType: String type of distribution to generate. Options include \"uniform\" (evenly distributes ore between the two heights), \"triangle\" (concentrates ores in the middle of the two heights)",
                    "   Min Height: Int to generate",
                    "   Max Height: Int to generate",
                    "   Size: Int to generate",
                    "   Density: Double to determine the density of the ore vein",
                    "   Count: Int to generate",
                    "   spawnChance: Double to ",
                    "   discardChance: Double to "
            ).defineList("oreSettings", oreSettings, (p) -> p instanceof List);
            b.pop();

        }
    }

    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final General GENERAL;
    public static final Tools TOOLS;
    public static final Machines MACHINES;
    public static final Alloys ALLOYS;
    public static final HardMode HARD_MODE;
    public static final Gases GASES;
    public static final Worldgen WORLDGEN;
    public static final BiomeGen BIOME_GEN;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        GENERAL = new General(BUILDER);
        TOOLS = new Tools(BUILDER);
        MACHINES = new Machines(BUILDER);
        ALLOYS = new Alloys(BUILDER);
        GASES = new Gases(BUILDER);
        WORLDGEN = new Worldgen(BUILDER);
        BIOME_GEN = new BiomeGen(BUILDER);
        HARD_MODE = new HardMode(BUILDER);

        COMMON_CONFIG = BUILDER.build();
    }


}
