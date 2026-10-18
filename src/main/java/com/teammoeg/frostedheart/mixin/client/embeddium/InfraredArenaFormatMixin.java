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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.client.embeddium;

import com.teammoeg.frostedheart.content.climate.render.infrared.OwnedChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The backend separately chooses GPU arena stride; compact already has the same 20-byte stride. */
@Mixin(value = RenderRegion.DeviceResources.class, remap = false)
public abstract class InfraredArenaFormatMixin {
    @Redirect(method = "<init>", at = @At(value = "FIELD", target =
            "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkMeshFormats;VANILLA_LIKE:"
            + "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;"))
    private ChunkVertexType fh$preciseArenaFormat() { return OwnedChunkVertexType.HIGH_PRECISION; }
}
