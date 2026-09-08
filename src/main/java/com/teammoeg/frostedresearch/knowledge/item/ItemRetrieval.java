package com.teammoeg.frostedresearch.knowledge.item;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitions;
import net.minecraft.nbt.CompoundTag;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.model.ObservationValue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * 检索只读取显式声明的附加字段，不复制数量与任意 NBT。
 */
public final class ItemRetrieval {
    private static final Map<ResourceLocation, Map<String, Function<ItemStack, String>>> FIELDS = new LinkedHashMap<>();

    private ItemRetrieval() {
    }

    /**
     * Register named item.* snapshot fields during common setup; no mutable ItemStack is retained.
     */
    public static void registerField(ResourceLocation item, String field, Function<ItemStack, String> collector) {
        FIELDS.computeIfAbsent(item, ignored -> new LinkedHashMap<>()).put(field, collector);
    }

    public static KnowledgeElement observe(ServerPlayer player, ItemStack original) {
        if (original.isEmpty() || ResearchNotes.isNote(original))
            throw new IllegalArgumentException("Retrieve a physical item, not a research note");
        ItemStack sample = original.copyWithCount(1);
        ResourceLocation item = BuiltInRegistries.ITEM.getKey(sample.getItem());
        Map<String, ObservationValue> values = new LinkedHashMap<>();
        if (sample.getItem() instanceof BlockItem block)
            values.put("item_block", ObservationValue.known(BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString()));
        FIELDS.getOrDefault(item, Map.of()).forEach((field, collector) -> values.put("item." + field, ObservationValue.known(collector.apply(sample.copy()))));
        CompoundTag itemData = new CompoundTag();
        KnowledgeDefinitions.current().itemRetrievalRules().values().stream().filter(rule -> rule.matches(sample))
                .forEach(rule -> itemData.merge(rule.copyAllowedData(sample)));
        for (String key : itemData.getAllKeys())
            values.put("item." + key, ObservationValue.known(itemData.get(key).toString()));
        Observation.Source source = new Observation.Source("item_retrieval", Optional.of(player.getUUID()), Optional.empty(), Optional.empty(), Optional.empty());
        return KnowledgeElement.observation(new Observation(UUID.randomUUID(), Observation.Type.ITEM, item, values, source, itemData.isEmpty() ? Optional.empty() : Optional.of(itemData)));
    }
}
