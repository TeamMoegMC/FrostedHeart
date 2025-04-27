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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import com.teammoeg.frostedheart.content.utility.seld.ContainerHolderEntity;
import com.teammoeg.frostedheart.content.utility.seld.SledEntity;
import com.teammoeg.frostedheart.content.world.entities.CuriosityEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FHEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FHMain.MODID);

    public static final RegistryObject<EntityType<WanderingRefugee>> WANDERING_REFUGEE = ENTITY_TYPES.register("wandering_refugee",
            () -> EntityType.Builder.<WanderingRefugee>of(WanderingRefugee::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).clientTrackingRange(10)
                    .build(new ResourceLocation(FHMain.MODID, "wandering_refugee").toString()));

    public static final RegistryObject<EntityType<CuriosityEntity>> CURIOSITY = ENTITY_TYPES.register("curiosity_entity",
            () -> EntityType.Builder.<CuriosityEntity>of(CuriosityEntity::new, MobCategory.CREATURE)
                    .sized(1.0f, 1.0f)
                    .build(new ResourceLocation(FHMain.MODID, "curiosity_entity").toString())
    );
    public static final RegistryObject<EntityType<SledEntity>> SLED = ENTITY_TYPES.register("sled_entity",
            () -> EntityType.Builder.<SledEntity>of(SledEntity::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F).clientTrackingRange(10)
                    .build(new ResourceLocation(FHMain.MODID, "sled_entity").toString())
    );
    public static final RegistryObject<EntityType<ContainerHolderEntity>> CONTAINER_ENTITY = ENTITY_TYPES.register("container_entity",
            () -> EntityType.Builder.<ContainerHolderEntity>of(ContainerHolderEntity::new, MobCategory.MISC)
                    .sized(0.75f, 0.75f).clientTrackingRange(8)
                    .build(new ResourceLocation(FHMain.MODID, "container_entity").toString())
    );
}
