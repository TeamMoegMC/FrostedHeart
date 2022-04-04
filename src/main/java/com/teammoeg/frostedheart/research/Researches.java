package com.teammoeg.frostedheart.research;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.cannolicatfish.rankine.init.RankineBlocks;
import com.cannolicatfish.rankine.init.RankineItems;
import com.simibubi.create.AllItems;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.research.clues.CustomClue;
import com.teammoeg.frostedheart.research.effects.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;

/**
 * Store some constant research instances
 */
public class Researches {

    public static final CustomClue ROOT_CLUE = new CustomClue("rootclue", 1.0F);

    public static final Research GEN_T1 = new Research("generator_t1", ResearchCategories.RESCUE,  FHContent.FHItems.energy_core);
    public static final Research GEN_T2 = new Research("generator_t2", ResearchCategories.RESCUE,  FHContent.FHItems.energy_core);
    public static final Research GEN_T3 = new Research("generator_t3", ResearchCategories.RESCUE,  FHContent.FHItems.energy_core);
    public static final Research GEN_T4 = new Research("generator_t4", ResearchCategories.RESCUE,  FHContent.FHItems.energy_core);

    public static final Research COAL_HAND_STOVE = new Research("coal_hand_stove", ResearchCategories.LIVING, FHContent.FHItems.hand_stove);

    public static final Research SNOW_BOOTS = new Research("snow_boots", ResearchCategories.EXPLORATION, RankineItems.SNOWSHOES.get());


    public static void init() {
        COAL_HAND_STOVE.attachRequiredItem(
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.copper_core_spade)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.copper_geologists_hammer)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.copper_pro_pick)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.iron_core_spade)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.iron_geologists_hammer)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.iron_pro_pick)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.steel_core_spade)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.steel_geologists_hammer)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.steel_pro_pick))
        );
        COAL_HAND_STOVE.attachEffect(
                new EffectItemReward(new ItemStack(FHContent.FHItems.hand_stove))
        );
        FHResearch.researches.register(COAL_HAND_STOVE);

        SNOW_BOOTS.attachEffect(
                new EffectItemReward(new ItemStack(RankineItems.SNOWSHOES.get()))
        );
        FHResearch.researches.register(SNOW_BOOTS);

        FHResearch.researches.register(new Research("mechanics", ResearchCategories.ARS, AllItems.GOGGLES.get()));
        FHResearch.researches.register(new Research("steam_properties", ResearchCategories.ARS, FHContent.FHItems.steam_bottle));
        FHResearch.researches.register(new Research("steam_cannon", ResearchCategories.ARS, AllItems.POTATO_CANNON.get(),
                FHResearch.getResearch("mechanics"), FHResearch.getResearch("steam_properties")));
        FHResearch.researches.register(new Research("sulfuric_acid", ResearchCategories.PRODUCTION, RankineItems.SULFUR.get()));
        FHResearch.researches.register(new Research("aluminum_extraction", ResearchCategories.PRODUCTION, RankineItems.ALUMINUM_INGOT.get(),
                FHResearch.getResearch("sulfuric_acid")));

        GEN_T1.attachRequiredItem(
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.energy_core)),
                IngredientWithSize.of(new ItemStack(FHContent.FHBlocks.generator_brick.asItem(), 2))
                );
        GEN_T1.attachEffect(
                new EffectItemReward(new ItemStack(FHContent.FHItems.energy_core)),
                new EffectBuilding(FHContent.FHMultiblocks.GENERATOR, FHContent.FHMultiblocks.generator),
                new EffectCrafting(FHContent.FHBlocks.generator_core_t1.asItem(), FHContent.FHBlocks.generator_amplifier_r1.asItem()),
                new EffectUse(FHContent.FHBlocks.generator_core_t1, FHContent.FHBlocks.generator_amplifier_r1),
                new EffectStats("Generator Burning Efficiency +25%")
        );
        GEN_T1.attachClue(() -> ROOT_CLUE);
        FHResearch.researches.register(GEN_T1);
        GEN_T2.setParents(FHResearch.getResearch("generator_t1"));
        GEN_T2.attachRequiredItem(
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.energy_core)),
                IngredientWithSize.of(new ItemStack(RankineBlocks.INVAR_BLOCK.get(), 2))
        );
        GEN_T2.attachEffect(
                new EffectItemReward(new ItemStack(FHContent.FHItems.energy_core)),
                new EffectBuilding(FHContent.FHMultiblocks.GENERATOR_T2, FHContent.FHMultiblocks.generator_t2)
        );
        GEN_T3.setParents(FHResearch.getResearch("generator_t2"));
        GEN_T4.setParents(FHResearch.getResearch("generator_t3"));
        FHResearch.researches.register(GEN_T2);
        FHResearch.researches.register(GEN_T3);
        FHResearch.researches.register(GEN_T4);

        FHResearch.indexResearches();
    }

}
