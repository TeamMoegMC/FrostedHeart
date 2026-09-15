/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.client.embeddium;

import com.teammoeg.frostedheart.content.climate.render.infrared.OwnedChunkVertexType;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DefaultChunkRenderer.class, remap = false)
public abstract class InfraredVertexBindingsMixin {
    @Inject(method = "getBindingsForType", at = @At("HEAD"), cancellable = true)
    private void fh$ownedAttributes(CallbackInfoReturnable<GlVertexAttributeBinding[]> cir) {
        if (((ShaderChunkRenderer) (Object) this).getVertexType() instanceof OwnedChunkVertexType owned)
            cir.setReturnValue(owned.bindings());
    }
}
