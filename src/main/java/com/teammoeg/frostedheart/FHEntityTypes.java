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

package com.teammoeg.frostedheart;

import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import com.teammoeg.frostedheart.content.utility.SpearEntity;
import com.teammoeg.frostedheart.world.entities.CuriosityEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FHEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FHMain.MODID);

    public static final RegistryObject<EntityType<SpearEntity>> FLINT_SPEAR = ENTITY_TYPES.register("flint_spear",
            () -> EntityType.Builder.<SpearEntity>of(SpearEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .build(new ResourceLocation(FHMain.MODID, "flint_spear").toString()));

    public static final RegistryObject<EntityType<SpearEntity>> ALLOY_SPEAR = ENTITY_TYPES.register("alloy_spear",
            () -> EntityType.Builder.<SpearEntity>of(SpearEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .build(new ResourceLocation(FHMain.MODID, "alloy_spear").toString()));

    public static final RegistryObject<EntityType<WanderingRefugee>> WANDERING_REFUGEE = ENTITY_TYPES.register("wandering_refugee",
            () -> EntityType.Builder.of(WanderingRefugee::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(new ResourceLocation(FHMain.MODID, "wandering_refugee").toString()));

    public static final RegistryObject<EntityType<CuriosityEntity>> CURIOSITY = ENTITY_TYPES.register("curiosity_entity",
            () -> EntityType.Builder.of(CuriosityEntity::new, MobCategory.CREATURE)
                    .sized(1.0f, 1.0f)
                    .build(new ResourceLocation(FHMain.MODID, "curiosity_entity").toString())
    );
}
