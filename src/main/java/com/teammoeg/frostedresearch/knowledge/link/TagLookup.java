/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.link;

import com.teammoeg.frostedresearch.knowledge.model.Observation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 标签查询由服务端当前注册表提供，测试和独立用途可提供自己的实现。
 */
@FunctionalInterface
public interface TagLookup {
    boolean contains(Observation.Type type, String field, ResourceLocation value, ResourceLocation tag);

    TagLookup NONE = (type, field, value, tag) -> false;
    TagLookup SERVER = (type, field, value, tag) -> {
        if (field.equals("biome")) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;
            var registry = server.registryAccess().registryOrThrow(Registries.BIOME);
            return registry.getHolder(net.minecraft.resources.ResourceKey.create(Registries.BIOME, value))
                    .map(holder -> holder.is(TagKey.create(Registries.BIOME, tag))).orElse(false);
        }
        if (!field.equals("object")) return false;
        return switch (type) {
            case BLOCK -> ForgeRegistries.BLOCKS.containsKey(value) && ForgeRegistries.BLOCKS.tags() != null
                    && ForgeRegistries.BLOCKS.tags().getTag(TagKey.create(Registries.BLOCK, tag)).contains(ForgeRegistries.BLOCKS.getValue(value));
            case ENTITY ->
                    ForgeRegistries.ENTITY_TYPES.containsKey(value) && ForgeRegistries.ENTITY_TYPES.tags() != null
                            && ForgeRegistries.ENTITY_TYPES.tags().getTag(TagKey.create(Registries.ENTITY_TYPE, tag)).contains(ForgeRegistries.ENTITY_TYPES.getValue(value));
            case ITEM -> ForgeRegistries.ITEMS.containsKey(value) && ForgeRegistries.ITEMS.tags() != null
                    && ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, tag)).contains(ForgeRegistries.ITEMS.getValue(value));
        };
    };
}
