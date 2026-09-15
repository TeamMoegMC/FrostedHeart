/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.client.embeddium;

import com.teammoeg.frostedheart.content.climate.render.infrared.InfraredChunkRenderer;
import com.teammoeg.frostedheart.content.climate.render.infrared.OwnedChunkVertexType;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only supplies the missing creation seam; existing argument adapters run first. */
@Mixin(value = RenderSectionManager.class, remap = false, priority = 900)
public abstract class InfraredRendererFactoryMixin {
    @Redirect(method = "<init>", at = @At(value = "FIELD", target =
            "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkMeshFormats;COMPACT:"
            + "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;"))
    private ChunkVertexType fh$compactType() { return OwnedChunkVertexType.COMPACT; }

    @Redirect(method = "<init>", at = @At(value = "FIELD", target =
            "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkMeshFormats;VANILLA_LIKE:"
            + "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;"))
    private ChunkVertexType fh$preciseType() { return OwnedChunkVertexType.HIGH_PRECISION; }

    @Redirect(method = "<init>", at = @At(value = "NEW", target =
            "(Lme/jellysquid/mods/sodium/client/gl/device/RenderDevice;"
            + "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexType;)"
            + "Lme/jellysquid/mods/sodium/client/render/chunk/DefaultChunkRenderer;"))
    private DefaultChunkRenderer fh$renderer(RenderDevice device, ChunkVertexType type) {
        return type instanceof OwnedChunkVertexType owned
                ? new InfraredChunkRenderer(device, owned) : new DefaultChunkRenderer(device, type);
    }
}
