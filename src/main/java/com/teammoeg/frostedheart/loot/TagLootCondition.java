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

package com.teammoeg.frostedheart.loot;

import javax.annotation.Nonnull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

public class TagLootCondition implements LootItemCondition {
    public static class Serializer implements Serializer<TagLootCondition> {

        @Nonnull
        @Override
        public TagLootCondition deserialize(JsonObject jsonObject, JsonDeserializationContext context) {
            Tags.IOptionalNamedTag<Block> optional = BlockTags.createOptional(new ResourceLocation(GsonHelper.getAsString(jsonObject, "tag")));
            return new TagLootCondition(optional);
        }

        @Override
        public void serialize(JsonObject jsonObject, TagLootCondition matchTagCondition, JsonSerializationContext serializationContext) {
            jsonObject.addProperty("tag", matchTagCondition.tag.getName().toString());
        }
    }
    public static LootItemConditionType TYPE;

    private Tags.IOptionalNamedTag<Block> tag;

    public TagLootCondition(Tags.IOptionalNamedTag<Block> tag) {
        this.tag = tag;
    }

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }

    @Override
    public boolean test(LootContext t) {
        if (t.hasParam(LootContextParams.ORIGIN)) {
            Vec3 v = t.getParamOrNull(LootContextParams.ORIGIN);
            BlockPos bp = new BlockPos(v.x, v.y, v.z);
            Level w = t.getLevel();
            BlockState bs = w.getBlockState(bp);

            return bs != null && tag.contains(bs.getBlock());
        }
        return false;
    }
}
