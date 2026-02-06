/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedresearch.data;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.codec.CompressDifferCodec;

import lombok.ToString;
import net.minecraft.nbt.CompoundTag;

@ToString
public class ClueData {
    public static final Codec<ClueData> FULL_CODEC = RecordCodecBuilder.create(t -> t.group(
            Codec.BOOL.fieldOf("completed").forGetter(o -> o.completed),
            CompoundTag.CODEC.optionalFieldOf("data").forGetter(o -> Optional.ofNullable(o.data))).apply(t, ClueData::new));
    public static final Codec<ClueData> CODEC = new CompressDifferCodec<>(FULL_CODEC,RecordCodecBuilder.create(t -> t.group(
            Codec.BOOL.fieldOf("completed").forGetter(o -> o.completed)).apply(t, ClueData::new)));
    boolean completed;
    CompoundTag data;

    public ClueData() {
        super();
    }

    public ClueData(boolean completed, Optional<CompoundTag> data) {
        super();
        this.completed = completed;
        this.data = data.orElse(null);
    }

    public ClueData(boolean completed) {
        super();
        this.completed = completed;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public CompoundTag getData() {
        return data;
    }

    public void setData(CompoundTag data) {
        this.data = data;
    }

}
