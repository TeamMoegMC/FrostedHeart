/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.render.infrared;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.content.climate.render.InfraredViewRenderer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.DefaultChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import me.jellysquid.mods.sodium.client.render.chunk.shader.*;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import static org.lwjgl.opengl.GL33C.*;

/** Extends program selection; the original renderer still performs all geometry submission. */
public final class InfraredChunkRenderer extends DefaultChunkRenderer {
    private final OwnedChunkVertexType ownedType;
    private final Map<ChunkShaderOptions, Programs> capturePrograms = new HashMap<>();
    private CameraTransform camera;
    private boolean captureForPass, captureBound;
    private int previousFramebuffer, previousTexture;

    public InfraredChunkRenderer(RenderDevice device, OwnedChunkVertexType type) {
        super(device, type);
        ownedType = type;
    }

    @Override public void render(ChunkRenderMatrices matrices, CommandList commands,
            ChunkRenderListIterable lists, TerrainRenderPass pass, CameraTransform camera) {
        this.camera = camera;
        try {
            super.render(matrices, commands, lists, pass, camera);
            // Embeddium draws SOLID then CUTOUT together, before entities and block entities.
            if (captureBound && pass == DefaultTerrainRenderPasses.CUTOUT)
                InfraredViewRenderer.captureTerrainDepth();
        } finally {
            if (captureBound) {
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousFramebuffer);
                int active = glGetInteger(GL_ACTIVE_TEXTURE);
                RenderSystem.activeTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_3D, previousTexture);
                RenderSystem.activeTexture(active);
            }
            captureBound = captureForPass = false;
            this.camera = null;
        }
    }

    @Override protected GlProgram<ChunkShaderInterface> compileProgram(ChunkShaderOptions options) {
        Programs programs = capturePrograms.get(options);
        if (programs == null) {
            ChunkShaderOptions base = new ChunkShaderOptions(options.fog(), options.pass(), ownedType.base());
            programs = new Programs(base);
            capturePrograms.put(options, programs);
        }
        // Called after the original pass setup, which may bind a framebuffer of its own.
        boolean mainView = InfraredViewRenderer.visitTerrainPass();
        captureForPass = mainView && isSurfacePass(options.pass());
        if (!captureForPass) return super.compileProgram(programs.base);
        if (programs.capture == null) programs.capture = createCaptureProgram(programs.base);
        return programs.capture;
    }

    private static boolean isSurfacePass(TerrainRenderPass pass) {
        return pass == DefaultTerrainRenderPasses.SOLID || pass == DefaultTerrainRenderPasses.CUTOUT;
    }

    boolean beginCapture() {
        if (!captureForPass || !InfraredViewRenderer.prepareSurfaceCapture()) return false;
        previousFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int active = glGetInteger(GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL_TEXTURE2);
        previousTexture = glGetInteger(GL_TEXTURE_BINDING_3D);
        glBindTexture(GL_TEXTURE_3D, InfraredViewRenderer.frameTemperatureTexture());
        RenderSystem.activeTexture(active);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, InfraredViewRenderer.surfaceCaptureFramebuffer());
        captureBound = true;
        return InfraredViewRenderer.frameTemperatureTexture() != 0;
    }

    CameraTransform camera() { return camera; }

    private GlProgram<ChunkShaderInterface> createCaptureProgram(ChunkShaderOptions base) {
        ResourceLocation vertexName = new ResourceLocation("sodium", "blocks/block_layer_opaque.vsh");
        ResourceLocation fragmentName = new ResourceLocation("sodium", "blocks/block_layer_opaque.fsh");
        String capture = ShaderLoader.getShaderSource(new ResourceLocation("frostedheart", "infrared_capture.glsl"));
        String vertexSource = InfraredCaptureShader.vertex(ShaderLoader.getShaderSource(vertexName), capture);
        String fragmentSource = InfraredCaptureShader.fragment(ShaderLoader.getShaderSource(fragmentName));
        GlShader vertex = new GlShader(ShaderType.VERTEX, vertexName, ShaderParser.parseShader(vertexSource, base.constants()));
        GlShader fragment = null;
        try {
            fragment = new GlShader(ShaderType.FRAGMENT, fragmentName, ShaderParser.parseShader(fragmentSource, base.constants()));
            return GlProgram.builder(new ResourceLocation("frostedheart", "infrared_capture"))
                    .attachShader(vertex).attachShader(fragment)
                    .bindAttribute("a_PosId", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID)
                    .bindAttribute("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
                    .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
                    .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
                    .bindAttribute("fhOwner", OwnedChunkVertexType.OWNER_ATTRIBUTE)
                    .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                    .bindFragmentData("fhSurfaceTemperature", 1)
                    .link(binding -> new InfraredChunkShaderInterface(binding, base, this));
        } finally {
            vertex.delete();
            if (fragment != null) fragment.delete();
        }
    }

    @Override public void delete(CommandList commands) {
        for (Programs programs : capturePrograms.values())
            if (programs.capture != null) programs.capture.delete();
        capturePrograms.clear();
        super.delete(commands); // The parent owns normal programs and the original geometry resources.
    }

    private static final class Programs {
        final ChunkShaderOptions base;
        GlProgram<ChunkShaderInterface> capture;

        Programs(ChunkShaderOptions base) { this.base = base; }
    }
}
