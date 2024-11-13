package com.teammoeg.frostedheart.base.item.rankine.init;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.item.rankine.entities.SpearEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RankineEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FHMain.MODID);

    public static final RegistryObject<EntityType<SpearEntity>> ALLOY_SPEAR = ENTITY_TYPES.register("alloy_spear",
            () -> EntityType.Builder.<SpearEntity>of(SpearEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .build(new ResourceLocation(FHMain.MODID, "alloy_spear").toString()));

}
