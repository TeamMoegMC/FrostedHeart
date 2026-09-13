/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.client.embeddium;

import com.teammoeg.frostedheart.content.climate.render.infrared.BlockOwnerScope;
import com.teammoeg.frostedheart.content.climate.render.infrared.OwnedChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public abstract class InfraredBlockScopeMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE", target =
            "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel("
            + "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;"
            + "Lme/jellysquid/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;)V"))
    private void fh$ownedModel(BlockRenderer renderer, BlockRenderContext context, ChunkBuildBuffers buffers) {
        if (!(buffers.getVertexType() instanceof OwnedChunkVertexType)) {
            renderer.renderModel(context, buffers);
            return;
        }
        var pos = context.pos();
        int previous = BlockOwnerScope.enter(pos.getX(), pos.getY(), pos.getZ());
        try {
            renderer.renderModel(context, buffers);
        } finally {
            BlockOwnerScope.restore(previous);
        }
    }
}
