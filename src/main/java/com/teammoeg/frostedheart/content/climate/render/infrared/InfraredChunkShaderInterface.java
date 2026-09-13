/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.render.infrared;

import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniform;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import org.joml.Vector3ic;
import org.lwjgl.opengl.GL20C;

final class InfraredChunkShaderInterface extends ChunkShaderInterface {
    private final InfraredChunkRenderer renderer;
    private final GlUniformInt captureEnabled, temperatureSampler;
    private final Int3Uniform regionOffset;

    InfraredChunkShaderInterface(ShaderBindingContext binding, ChunkShaderOptions options, InfraredChunkRenderer renderer) {
        super(binding, options);
        this.renderer = renderer;
        captureEnabled = binding.bindUniform("fhCaptureEnabled", GlUniformInt::new);
        temperatureSampler = binding.bindUniform("fhTemperatureTexture", GlUniformInt::new);
        regionOffset = binding.bindUniform("fhRegionToTextureOrigin", Int3Uniform::new);
    }

    @Override public void setupState() {
        super.setupState();
        temperatureSampler.setInt(2); // Distinct from the original block and light samplers, even without data.
        captureEnabled.setInt(renderer.beginCapture() ? 1 : 0);
    }

    @Override public void setRegionOffset(float x, float y, float z) {
        super.setRegionOffset(x, y, z);
        CameraTransform camera = renderer.camera();
        // Invert the backend's integer-minus-fraction translation, using its actual quantized fraction.
        regionOffset.setInts(Math.round(x + camera.fracX) + camera.intX - InfraredViewRenderer.frameOriginX(),
                Math.round(y + camera.fracY) + camera.intY - InfraredViewRenderer.frameOriginY(),
                Math.round(z + camera.fracZ) + camera.intZ - InfraredViewRenderer.frameOriginZ());
    }

    private static final class Int3Uniform extends GlUniform<Vector3ic> {
        Int3Uniform(int index) { super(index); }
        void setInts(int x, int y, int z) { GL20C.glUniform3i(index, x, y, z); }
        @Override public void set(Vector3ic value) { setInts(value.x(), value.y(), value.z()); }
    }
}
