package com.teammoeg.frostedheart.research;

import blusunrize.immersiveengineering.api.EnumMetals;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.common.blocks.IEBlocks;
import blusunrize.immersiveengineering.common.items.IEItems;
import com.cannolicatfish.rankine.init.RankineBlocks;
import com.cannolicatfish.rankine.init.RankineItems;
import com.simibubi.create.AllItems;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.research.clues.CustomClue;
import com.teammoeg.frostedheart.research.effects.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.List;

/**
 * Store some constant research instances
 */
public class Researches {


    public static void init() {
        FHResearch.prepareReload();
        File folder = FMLPaths.CONFIGDIR.get().toFile();
        File rf = new File(folder, "fhresearches");
        ResearchCategories.init();

        FHResearch.loadAll();
        FHResearch.finishReload();
        //FHResearch.saveAll();
    }

    public static void initFromPacket(List<Research> rs) {
        FHResearch.prepareReload();
        ResearchCategories.init();
        FHResearch.readAll(rs);
        FHResearch.finishReload();
        //FHResearch.saveAll();
    }

    public static void createDefaultResearches() {
        FHResearch.prepareReload();
        CustomClue ROOT_CLUE = new CustomClue("rootclue", 1.0F);

        Research GEN_T1 = new Research("generator_t1", ResearchCategory.RESCUE,
                FHItems.energy_core);
        Research GEN_T2 = new Research("generator_t2", ResearchCategory.RESCUE,
                FHMultiblocks.generator_t2);
        Research GEN_T3 = new Research("generator_t3", ResearchCategory.RESCUE,
                FHItems.energy_core);
        Research GEN_T4 = new Research("generator_t4", ResearchCategory.RESCUE,
                FHItems.energy_core);

        Research COAL_HAND_STOVE = new Research("coal_hand_stove", ResearchCategory.LIVING,
                FHItems.hand_stove);

        Research SNOW_BOOTS = new Research("snow_boots", ResearchCategory.EXPLORATION,
                RankineItems.SNOWSHOES.get());
        COAL_HAND_STOVE.attachRequiredItem(IngredientWithSize.of(new ItemStack(FHItems.copper_core_spade)),
                IngredientWithSize.of(new ItemStack(FHItems.copper_geologists_hammer)),
                IngredientWithSize.of(new ItemStack(FHItems.copper_pro_pick)),
                IngredientWithSize.of(new ItemStack(FHItems.iron_core_spade)),
                IngredientWithSize.of(new ItemStack(FHItems.iron_geologists_hammer)),
                IngredientWithSize.of(new ItemStack(FHItems.iron_pro_pick)),
                IngredientWithSize.of(new ItemStack(FHItems.steel_core_spade)),
                IngredientWithSize.of(new ItemStack(FHItems.steel_geologists_hammer)),
                IngredientWithSize.of(new ItemStack(FHItems.steel_pro_pick)));
        COAL_HAND_STOVE.attachEffect(new EffectItemReward(new ItemStack(FHItems.hand_stove)));
        FHResearch.researches.register(COAL_HAND_STOVE);

        SNOW_BOOTS.attachEffect(new EffectItemReward(new ItemStack(RankineItems.SNOWSHOES.get())));
        FHResearch.researches.register(SNOW_BOOTS);

        FHResearch.researches.register(new Research("mechanics", ResearchCategory.ARS, AllItems.GOGGLES.get()));
        FHResearch.researches
                .register(new Research("steam_properties", ResearchCategory.ARS, FHItems.steam_bottle));
        FHResearch.researches
                .register(new Research("steam_cannon", ResearchCategory.ARS, AllItems.POTATO_CANNON.get(),
                        FHResearch.getResearch("mechanics"), FHResearch.getResearch("steam_properties")));
        FHResearch.researches
                .register(new Research("sulfuric_acid", ResearchCategory.PRODUCTION, RankineItems.SULFUR.get()));
        FHResearch.researches.register(new Research("aluminum_extraction", ResearchCategory.PRODUCTION,
                RankineItems.ALUMINUM_INGOT.get(), FHResearch.getResearch("sulfuric_acid")));
        FHResearch.researches.register(new Research("mechanic", ResearchCategory.PRODUCTION,
                IEBlocks.MetalDecoration.engineeringLight.asItem(), FHResearch.getResearch("aluminum_extraction")));
        FHResearch.researches.register(
                new Research("steel", ResearchCategory.PRODUCTION, IEItems.Metals.ingots.get(EnumMetals.STEEL)));
        GEN_T1.attachRequiredItem(IngredientWithSize.of(new ItemStack(FHItems.energy_core)),
                IngredientWithSize.of(new ItemStack(FHBlocks.generator_brick.asItem(), 2)));
        GEN_T1.attachEffect(new EffectItemReward(new ItemStack(FHItems.energy_core)),
                new EffectBuilding(FHMultiblocks.GENERATOR, FHMultiblocks.generator),
                new EffectCrafting(FHBlocks.generator_core_t1.asItem()),
                new EffectCrafting(FHBlocks.generator_amplifier_r1.asItem()),
                new EffectUse(FHBlocks.generator_core_t1, FHBlocks.generator_amplifier_r1),
                new EffectStats("Generator Burning Efficiency", 25));
        GEN_T1.attachClue(ROOT_CLUE);
        FHResearch.researches.register(GEN_T1);

        GEN_T2.setParents(GEN_T1.getSupplier(), FHResearch.getResearch("mechanic"));
        GEN_T2.attachRequiredItem(IngredientWithSize.of(new ItemStack(FHItems.energy_core)),
                IngredientWithSize.of(new ItemStack(RankineBlocks.INVAR_BLOCK.get(), 2)));
        GEN_T2.attachEffect(new EffectItemReward(new ItemStack(FHItems.energy_core)),
                new EffectBuilding(FHMultiblocks.GENERATOR_T2, FHMultiblocks.generator_t2));

        GEN_T3.setParents(FHResearch.getResearch("generator_t2"));
        GEN_T4.setParents(FHResearch.getResearch("generator_t3"));
        FHResearch.researches.register(GEN_T2);
        FHResearch.researches.register(GEN_T3);
        FHResearch.researches.register(GEN_T4);

        FHResearch.researches.register(new Research("radiator", ResearchCategory.LIVING,
                FHMultiblocks.radiator.asItem(), GEN_T2.getSupplier()));

        FHResearch.researches.register(
                new Research("charger", ResearchCategory.LIVING, FHBlocks.charger.asItem(), GEN_T2.getSupplier()));
        FHResearch.saveAll();
        FHResearch.prepareReload();
        FHResearch.loadAll();
        FHResearch.finishReload();
    }

}
