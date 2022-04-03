package com.teammoeg.frostedheart.research;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.cannolicatfish.rankine.init.RankineItems;
import com.simibubi.create.AllItems;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.research.effects.EffectBuilding;
import com.teammoeg.frostedheart.research.effects.EffectItemReward;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * Store some constant research instances
 */
public class Researches {

    public static final Research GEN_T1 = new Research("generator_t1", ResearchCategories.RESCUE,  FHContent.FHItems.energy_core);

    public static void init() {
        FHResearch.researches.register(new Research("coal_hand_stove", ResearchCategories.LIVING, FHContent.FHItems.hand_stove));
        FHResearch.researches.register(new Research("snow_boots", ResearchCategories.EXPLORATION, RankineItems.SNOWSHOES.get()));
        FHResearch.researches.register(new Research("mechanics", ResearchCategories.ARS, AllItems.GOGGLES.get()));
        FHResearch.researches.register(new Research("steam_properties", ResearchCategories.ARS, FHContent.FHItems.steam_bottle));
        FHResearch.researches.register(new Research("steam_cannon", ResearchCategories.ARS, AllItems.POTATO_CANNON.get(),
                FHResearch.getResearch("mechanics"), FHResearch.getResearch("steam_properties")));
        FHResearch.researches.register(new Research("sulfuric_acid", ResearchCategories.PRODUCTION, RankineItems.SULFUR.get()));
        FHResearch.researches.register(new Research("aluminum_extraction", ResearchCategories.PRODUCTION, RankineItems.ALUMINUM_INGOT.get(),
                FHResearch.getResearch("sulfuric_acid")));

        GEN_T1.attachRequiredItem(
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.energy_core)),
                IngredientWithSize.of(new ItemStack(FHContent.FHBlocks.generator_brick.asItem(), 2)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.copper_core_spade)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.copper_geologists_hammer)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.copper_pro_pick)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.iron_core_spade)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.iron_geologists_hammer)),
                IngredientWithSize.of(new ItemStack(FHContent.FHItems.iron_pro_pick))
                );
        GEN_T1.attachEffect(
                new EffectItemReward(new ItemStack(FHContent.FHItems.energy_core)),
                new EffectBuilding(FHContent.FHMultiblocks.GENERATOR)
        );
        FHResearch.researches.register(GEN_T1);

        FHResearch.researches.register(new Research("generator_t2", ResearchCategories.RESCUE, FHResearch.getResearch("generator_t1")));
        FHResearch.researches.register(new Research("generator_t3", ResearchCategories.RESCUE, FHResearch.getResearch("generator_t2")));
        FHResearch.researches.register(new Research("generator_t4", ResearchCategories.RESCUE, FHResearch.getResearch("generator_t3")));
        FHResearch.indexResearches();
    }

}
